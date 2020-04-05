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
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.io.IOException;

public class WebviewPlayerActivity extends AppCompatActivity {

    private String videoUrl;
    private WebView webView;
    private RelativeLayout loader;
    private BroadcastReceiver receive_data;
    private FrameLayout webview_container;

    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String url = null;
        Bundle b = getIntent().getExtras();
        if (b != null) {
            url = b.getString("url", null);
        }
        if( url == null){
            finish();
        }
        setContentView(R.layout.activity_webview_player);


        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        final ViewGroup videoLayout = findViewById(R.id.videoLayout);
        webView = findViewById(R.id.webview);
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
                String streaming_url = b.getString("streaming_url", null);
                if (streaming_url != null ) {
                    webView.stopLoading();
                    webView.loadUrl(streaming_url);
                    loader.setVisibility(View.GONE);
                    webview_container.setVisibility(View.VISIBLE);
                }
            }
        };
        webView.setWebChromeClient(playerChromeClient);
        LocalBroadcastManager.getInstance(WebviewPlayerActivity.this).registerReceiver(receive_data, new IntentFilter(Utils.RECEIVE_STREAMING_URL));

        String finalUrl = url;
        AsyncTask.execute(() -> {
            try {
                Document document = Jsoup
                        .connect(finalUrl).ignoreContentType(true).get();

                Element video = document.select("video").first();
                if( video != null ){
                    Element source = video.select("source").first();
                    if( source != null ) {
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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onPause(){
        super.onPause();
        if( webView != null ){
            webView.onPause();
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        if( webView != null ){
            webView.onResume();
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if (receive_data != null)
            LocalBroadcastManager.getInstance(WebviewPlayerActivity.this).unregisterReceiver(receive_data);
    }
}
