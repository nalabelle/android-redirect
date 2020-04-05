package app.fedilab.nitterizeme;
/* Copyright 2020 Thomas Schneider
 *
 * This file is a part of NitterizeMe
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * NitterizeMe is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with NitterizeMe; if not,
 * see <http://www.gnu.org/licenses>. */

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;

public class WebviewPlayerActivity extends AppCompatActivity {

    private static final int EXTERNAL_STORAGE_REQUEST_CODE = 84;
    private String videoUrl;
    private WebView webView;
    private RelativeLayout loader;
    private BroadcastReceiver receive_data;
    private FrameLayout webview_container;
    private String streaming_url;
    private String initialUrl;

    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initialUrl = null;
        Bundle b = getIntent().getExtras();
        if (b != null) {
            initialUrl = b.getString("url", null);
        }
        if (initialUrl == null) {
            finish();
        }

        setContentView(R.layout.activity_webview_player);


        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        final ViewGroup videoLayout = findViewById(R.id.videoLayout);
        webView = findViewById(R.id.webview);
        webView.setBackgroundColor(Color.TRANSPARENT);
        loader = findViewById(R.id.loader);
        webview_container = findViewById(R.id.webview_container);
        webView.getSettings().setJavaScriptEnabled(true);

        PlayerChromeClient playerChromeClient = new PlayerChromeClient(WebviewPlayerActivity.this, webView, webview_container, videoLayout);
        playerChromeClient.setOnToggledFullscreen(fullscreen -> {
            if (fullscreen) {
                videoLayout.setVisibility(View.VISIBLE);
                WindowManager.LayoutParams attrs = getWindow().getAttributes();
                attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
                attrs.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
                getWindow().setAttributes(attrs);
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
            } else {
                WindowManager.LayoutParams attrs = getWindow().getAttributes();
                attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
                attrs.flags &= ~WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
                getWindow().setAttributes(attrs);
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                videoLayout.setVisibility(View.GONE);
            }
        });

        receive_data = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle b = intent.getExtras();
                assert b != null;
                streaming_url = b.getString("streaming_url", null);
                if (streaming_url != null) {
                    webView.stopLoading();
                    webView.loadUrl(streaming_url);
                    loader.setVisibility(View.GONE);
                    webview_container.setVisibility(View.VISIBLE);
                }
            }
        };
        webView.setWebChromeClient(playerChromeClient);
        LocalBroadcastManager.getInstance(WebviewPlayerActivity.this).registerReceiver(receive_data, new IntentFilter(Utils.RECEIVE_STREAMING_URL));

        AsyncTask.execute(() -> {
            try {
                Document document = Jsoup
                        .connect(initialUrl).ignoreContentType(true).get();

                Element video = document.select("video").first();
                if (video != null) {
                    Element source = video.select("source").first();
                    if (source != null) {
                        videoUrl = source.absUrl("src");
                        runOnUiThread(() -> webView.loadUrl(videoUrl));
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_player, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_share && initialUrl != null) {


            AsyncTask.execute(() -> {
                try {
                    Document document = Jsoup
                            .connect(initialUrl).ignoreContentType(true).get();

                    Element metaTitle = document.select("meta[property=\"og:title\"]").first();
                    String title = metaTitle.attr("content");
                    Element metaMedia = document.select("meta[property=\"og:video:url\"]").first();
                    String media = metaMedia.attr("content");
                    Element metaDescription = document.select("meta[property=\"og:description\"]").first();
                    String description = metaDescription.attr("content");
                    runOnUiThread(() -> {
                        Intent sendIntent = new Intent(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_SUBJECT, title);
                        sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(media));
                        sendIntent.putExtra(Intent.EXTRA_TEXT, description + "\n\n" + initialUrl);
                        sendIntent.setType("text/plain");
                        startActivity(Intent.createChooser(sendIntent, getString(R.string.share_with)));
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }

            });
            return true;
        } else if (id == R.id.action_download && streaming_url != null) {
            if (Build.VERSION.SDK_INT >= 23) {
                if (ContextCompat.checkSelfPermission(WebviewPlayerActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(WebviewPlayerActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(WebviewPlayerActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, EXTERNAL_STORAGE_REQUEST_CODE);
                } else {
                    Utils.manageDownloadsNoPopup(WebviewPlayerActivity.this, streaming_url);
                }
            } else {
                Utils.manageDownloadsNoPopup(WebviewPlayerActivity.this, streaming_url);
            }
            return true;
        } else if (id == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (receive_data != null) {
            LocalBroadcastManager.getInstance(WebviewPlayerActivity.this).unregisterReceiver(receive_data);
        }
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
    }
}
