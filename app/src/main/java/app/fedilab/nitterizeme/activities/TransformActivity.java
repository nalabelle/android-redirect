package app.fedilab.nitterizeme.activities;
/* Copyright 2020 Thomas Schneider
 *
 * This file is a part of UntrackMe
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * UntrackMe is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with UntrackMe; if not,
 * see <http://www.gnu.org/licenses>. */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.fedilab.nitterizeme.R;
import app.fedilab.nitterizeme.helpers.Utils;

import static app.fedilab.nitterizeme.activities.CheckAppActivity.bibliogram_instances;
import static app.fedilab.nitterizeme.activities.CheckAppActivity.instagram_domains;
import static app.fedilab.nitterizeme.activities.CheckAppActivity.invidious_instances;
import static app.fedilab.nitterizeme.activities.CheckAppActivity.nitter_instances;
import static app.fedilab.nitterizeme.activities.CheckAppActivity.shortener_domains;
import static app.fedilab.nitterizeme.activities.CheckAppActivity.twitter_domains;
import static app.fedilab.nitterizeme.activities.CheckAppActivity.youtube_domains;
import static app.fedilab.nitterizeme.activities.MainActivity.SET_BIBLIOGRAM_ENABLED;
import static app.fedilab.nitterizeme.activities.MainActivity.SET_INVIDIOUS_ENABLED;
import static app.fedilab.nitterizeme.activities.MainActivity.SET_NITTER_ENABLED;
import static app.fedilab.nitterizeme.helpers.Utils.KILL_ACTIVITY;
import static app.fedilab.nitterizeme.helpers.Utils.URL_APP_PICKER;
import static app.fedilab.nitterizeme.helpers.Utils.ampExtract;
import static app.fedilab.nitterizeme.helpers.Utils.bibliogramAccountPattern;
import static app.fedilab.nitterizeme.helpers.Utils.bibliogramPostPattern;
import static app.fedilab.nitterizeme.helpers.Utils.maps;
import static app.fedilab.nitterizeme.helpers.Utils.nitterPattern;
import static app.fedilab.nitterizeme.helpers.Utils.remove_tracking_param;
import static app.fedilab.nitterizeme.helpers.Utils.transformUrl;
import static app.fedilab.nitterizeme.helpers.Utils.youtubePattern;


public class TransformActivity extends Activity {


    private Thread thread;
    private ArrayList<String> notShortnedURLDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedpreferences = getSharedPreferences(MainActivity.APP_PREFS, Context.MODE_PRIVATE);
        Intent intent = getIntent();
        if (intent != null && intent.getStringExtra("nitterizeme") != null) {
            finish();
            return;
        }
        Intent stopMainActivity = new Intent(KILL_ACTIVITY);
        sendBroadcast(stopMainActivity);
        notShortnedURLDialog = new ArrayList<>();
        assert intent != null;
        //Dealing with URLs
        if (Objects.requireNonNull(intent.getAction()).equals(Intent.ACTION_VIEW)) {
            String url = Objects.requireNonNull(intent.getData()).toString();
            URL url_;
            String host = null;
            try {
                url_ = new URL(url);
                host = url_.getHost();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            //Shortened URLs
            if (Arrays.asList(shortener_domains).contains(host)) {

                AlertDialog.Builder unshortenAlertBuilder = new AlertDialog.Builder(TransformActivity.this, R.style.AppThemeDialog);
                unshortenAlertBuilder.setTitle(R.string.shortened_detected);
                unshortenAlertBuilder.setOnDismissListener(dialog -> finish());
                View view = getLayoutInflater().inflate(R.layout.popup_unshorten, new LinearLayout(getApplicationContext()), false);
                unshortenAlertBuilder.setView(view);
                unshortenAlertBuilder.setIcon(R.mipmap.ic_launcher);
                unshortenAlertBuilder.setPositiveButton(R.string.open, (dialog, id) -> {
                    if (notShortnedURLDialog.size() > 0) {
                        Intent delegate = new Intent(Intent.ACTION_VIEW);
                        delegate.setData(Uri.parse(notShortnedURLDialog.get(notShortnedURLDialog.size() - 1)));
                        delegate.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        forwardToBrowser(delegate);
                    }
                    dialog.dismiss();
                    finish();
                });
                unshortenAlertBuilder.setNegativeButton(R.string.dismiss, (dialog, id) -> {
                    dialog.dismiss();
                    finish();
                });
                AlertDialog alertDialog = unshortenAlertBuilder.create();
                alertDialog.show();
                Button positiveButton = (alertDialog).getButton(AlertDialog.BUTTON_POSITIVE);
                positiveButton.setEnabled(false);
                thread = new Thread() {
                    @Override
                    public void run() {
                        notShortnedURLDialog = new ArrayList<>();
                        notShortnedURLDialog.add(url);
                        Utils.checkUrl(TransformActivity.this, notShortnedURLDialog);
                        Handler mainHandler = new Handler(Looper.getMainLooper());
                        Runnable myRunnable = () -> {
                            positiveButton.setEnabled(true);
                            StringBuilder message;
                            if (notShortnedURLDialog.size() <= 1) {
                                message = new StringBuilder(getString(R.string.the_app_failed_shortened));
                            } else {
                                message = new StringBuilder(getString(R.string.try_to_redirect, notShortnedURLDialog.get(0), notShortnedURLDialog.get(1)));
                                if (notShortnedURLDialog.size() > 2) {
                                    for (int i = 2; i < notShortnedURLDialog.size(); i++) {
                                        message.append("\n\n").append(getString(R.string.try_to_redirect_again, notShortnedURLDialog.get(i)));
                                    }
                                }
                            }
                            TextView indications = view.findViewById(R.id.indications);
                            RelativeLayout progress = view.findViewById(R.id.progress);
                            indications.setText(message.toString());
                            indications.setVisibility(View.VISIBLE);
                            progress.setVisibility(View.GONE);
                        };
                        mainHandler.post(myRunnable);
                    }
                };
                thread.start();
            }
            //Twitter URLs
            else if (Arrays.asList(twitter_domains).contains(host)) {
                boolean nitter_enabled = sharedpreferences.getBoolean(SET_NITTER_ENABLED, true);
                if (nitter_enabled) {
                    Intent delegate = new Intent(Intent.ACTION_VIEW);
                    String transformedURL = transformUrl(TransformActivity.this, url);
                    if (transformedURL != null) {
                        delegate.setData(Uri.parse(transformUrl(TransformActivity.this, url)));
                        delegate.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        forwardToBrowser(delegate);
                    } else {
                        forwardToBrowser(intent);
                    }
                } else {
                    forwardToBrowser(intent);
                }
            } //Instagram URLs
            else if (Arrays.asList(instagram_domains).contains(host)) {
                boolean bibliogram_enabled = sharedpreferences.getBoolean(SET_BIBLIOGRAM_ENABLED, true);
                if (bibliogram_enabled) {
                    Intent delegate = new Intent(Intent.ACTION_VIEW);
                    String transformedURL = transformUrl(TransformActivity.this, url);
                    if (transformedURL != null) {
                        delegate.setData(Uri.parse(transformUrl(TransformActivity.this, url)));
                        delegate.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        forwardToBrowser(delegate);
                    } else {
                        forwardToBrowser(intent);
                    }
                } else {
                    forwardToBrowser(intent);
                }
            }
            //Maps URLs (containing /maps/place like Google Maps links)
            else if (url.contains("/maps/place")) {
                boolean osm_enabled = sharedpreferences.getBoolean(MainActivity.SET_OSM_ENABLED, true);
                if (osm_enabled) {
                    Intent delegate = new Intent(Intent.ACTION_VIEW);
                    String transformedURL = transformUrl(TransformActivity.this, url);
                    if (transformedURL != null) {
                        delegate.setData(Uri.parse(transformUrl(TransformActivity.this, url)));
                        delegate.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        forwardToBrowser(delegate);
                    } else {
                        forwardToBrowser(intent);
                    }
                } else {
                    forwardToBrowser(intent);
                }
            }
            //AMP URLs (containing /amp/s/ like Google AMP links)
            else if (url.contains("/amp/s/")) {
                Intent delegate = new Intent(Intent.ACTION_VIEW);
                Matcher matcher = ampExtract.matcher(url);
                String transformedURL = null;
                while (matcher.find()) {
                    transformedURL = "https://" + matcher.group(1);
                }
                if (transformedURL != null) {
                    delegate.setData(Uri.parse(transformedURL));
                    delegate.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    forwardToBrowser(delegate);
                } else {
                    forwardToBrowser(intent);
                }
            }
            //YouTube URLs
            else if (Arrays.asList(youtube_domains).contains(host)) { //Youtube URL
                boolean invidious_enabled = sharedpreferences.getBoolean(SET_INVIDIOUS_ENABLED, true);
                if (invidious_enabled) {
                    Intent delegate = new Intent(Intent.ACTION_VIEW);
                    String transformedURL = transformUrl(TransformActivity.this, url);
                    if (transformedURL != null) {
                        delegate.setData(Uri.parse(transformUrl(TransformActivity.this, url)));
                        delegate.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        forwardToBrowser(delegate);
                    } else {
                        forwardToBrowser(intent);
                    }
                } else {
                    forwardToBrowser(intent);
                }
            }
            //Transform an Invidious URL from an instance to another one selected by the end user.
            else if (Arrays.asList(invidious_instances).contains(host)) {
                boolean invidious_enabled = sharedpreferences.getBoolean(SET_INVIDIOUS_ENABLED, true);
                if (invidious_enabled) {
                    String invidiousHost = sharedpreferences.getString(MainActivity.SET_INVIDIOUS_HOST, MainActivity.DEFAULT_INVIDIOUS_HOST).toLowerCase();
                    String transformedURL = url;
                    if (host != null && host.compareTo(invidiousHost) != 0) {
                        transformedURL = url.replace(host, invidiousHost);
                    }
                    intent.setData(Uri.parse(transformedURL));
                    forwardToBrowser(intent);
                } else {
                    forwardToBrowser(intent);
                }
            }
            //Transform a Nitter URL from an instance to another one selected by the end user.
            else if (Arrays.asList(nitter_instances).contains(host)) {
                boolean nitter_enabled = sharedpreferences.getBoolean(SET_NITTER_ENABLED, true);
                if (nitter_enabled) {
                    String nitterHost = sharedpreferences.getString(MainActivity.SET_NITTER_HOST, MainActivity.DEFAULT_NITTER_HOST).toLowerCase();
                    String transformedURL = url;
                    if (host != null && host.compareTo(nitterHost) != 0) {
                        transformedURL = url.replace(host, nitterHost);
                    }
                    intent.setData(Uri.parse(transformedURL));
                    forwardToBrowser(intent);
                } else {
                    forwardToBrowser(intent);
                }
            }
            //Transform a Bibliogram URL from an instance to another one selected by the end user.
            else if (Arrays.asList(bibliogram_instances).contains(host)) {
                boolean bibliogram_enabled = sharedpreferences.getBoolean(SET_BIBLIOGRAM_ENABLED, true);
                if (bibliogram_enabled) {
                    String bibliogramHost = sharedpreferences.getString(MainActivity.SET_BIBLIOGRAM_HOST, MainActivity.DEFAULT_BIBLIOGRAM_HOST).toLowerCase();
                    String transformedURL = url;
                    if (host != null && host.compareTo(bibliogramHost) != 0) {
                        transformedURL = url.replace(host, bibliogramHost);
                    }
                    intent.setData(Uri.parse(transformedURL));
                    forwardToBrowser(intent);
                } else {
                    forwardToBrowser(intent);
                }
            } else {
                String newUrl = remove_tracking_param(url);
                intent.setData(Uri.parse(newUrl));
                forwardToBrowser(intent);
            }

        }
        //It's a sharing intent
        else if (Objects.requireNonNull(intent.getAction()).equals(Intent.ACTION_SEND)) {
            share(intent.getStringExtra(Intent.EXTRA_TEXT));
        }
    }

    @Override
    protected void onDestroy() {
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
        super.onDestroy();
    }

    /**
     * Forward the intent to a browser
     *
     * @param i original intent
     */
    private void forwardToBrowser(Intent i) {

        Intent app_picker = new Intent(TransformActivity.this, AppsPickerActivity.class);
        Bundle b = new Bundle();
        b.putString(URL_APP_PICKER, i.getDataString());
        app_picker.putExtras(b);
        startActivity(app_picker);
        finish();
    }


    /**
     * Transform URL inside the shared content without modifying the whole content
     *
     * @param extraText String the new extra text
     */
    private void share(String extraText) {
        SharedPreferences sharedpreferences = getSharedPreferences(MainActivity.APP_PREFS, Context.MODE_PRIVATE);

        String url = null;
        if (extraText != null) {
            Matcher matcher;
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT)
                matcher = Patterns.WEB_URL.matcher(extraText);
            else
                matcher = Utils.urlPattern.matcher(extraText);
            while (matcher.find()) {
                int matchStart = matcher.start(1);
                int matchEnd = matcher.end();
                if (matchStart < matchEnd && extraText.length() >= matchEnd) {
                    url = extraText.substring(matchStart, matchEnd);
                }
            }
        }
        URL url_;
        String host = null;
        try {
            url_ = new URL(url);
            host = url_.getHost();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        String newUrl = null;
        if (url == null) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, extraText);
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
            return;
        }

        if (Arrays.asList(twitter_domains).contains(host)) {
            boolean nitter_enabled = sharedpreferences.getBoolean(SET_NITTER_ENABLED, true);
            if (nitter_enabled) {

                String nitterHost = sharedpreferences.getString(MainActivity.SET_NITTER_HOST, MainActivity.DEFAULT_NITTER_HOST).toLowerCase();
                assert host != null;
                if (host.compareTo("pbs.twimg.com") == 0 || host.compareTo("pic.twitter.com") == 0) {
                    try {
                        newUrl = "https://" + nitterHost + "/pic/" + URLEncoder.encode(url, "utf-8");
                    } catch (UnsupportedEncodingException e) {
                        newUrl = "https://" + nitterHost + "/pic/" + url;
                    }
                } else if (url.contains("/search?")) {
                    newUrl = url.replace(host, nitterHost);
                } else {
                    Matcher matcher = nitterPattern.matcher(url);
                    while (matcher.find()) {
                        final String nitter_directory = matcher.group(2);
                        newUrl = "https://" + nitterHost + nitter_directory;
                    }
                }
            }
        } else if (Arrays.asList(instagram_domains).contains(host)) {
            boolean bibliogram_enabled = sharedpreferences.getBoolean(SET_BIBLIOGRAM_ENABLED, true);
            if (bibliogram_enabled) {
                Matcher matcher = bibliogramPostPattern.matcher(url);
                while (matcher.find()) {
                    final String bibliogram_directory = matcher.group(2);
                    String bibliogramHost = sharedpreferences.getString(MainActivity.SET_BIBLIOGRAM_HOST, MainActivity.DEFAULT_BIBLIOGRAM_HOST).toLowerCase();
                    newUrl = "https://" + bibliogramHost + bibliogram_directory;
                }
                matcher = bibliogramAccountPattern.matcher(url);
                while (matcher.find()) {
                    final String bibliogram_directory = matcher.group(2);
                    String bibliogramHost = sharedpreferences.getString(MainActivity.SET_BIBLIOGRAM_HOST, MainActivity.DEFAULT_BIBLIOGRAM_HOST).toLowerCase();
                    if (bibliogram_directory != null && bibliogram_directory.compareTo("privacy") != 0) {
                        newUrl = "https://" + bibliogramHost + "/u" + bibliogram_directory;
                    } else {
                        newUrl = "https://" + bibliogramHost + bibliogram_directory;
                    }
                }
            }
        } else if (url.contains("/maps/place/")) {
            boolean osm_enabled = sharedpreferences.getBoolean(MainActivity.SET_OSM_ENABLED, true);
            if (osm_enabled) {
                Matcher matcher = maps.matcher(url);
                while (matcher.find()) {
                    final String localization = matcher.group(1);
                    assert localization != null;
                    String[] data = localization.split(",");
                    if (data.length > 2) {
                        String zoom;
                        String[] details = data[2].split("\\.");
                        if (details.length > 0) {
                            zoom = details[0];
                        } else {
                            zoom = data[2];
                        }
                        String osmHost = sharedpreferences.getString(MainActivity.SET_OSM_HOST, MainActivity.DEFAULT_OSM_HOST).toLowerCase();
                        newUrl = "https://" + osmHost + "/#map=" + zoom + "/" + data[0] + "/" + data[1];
                    }
                }
            }
        } else if (url.contains("/amp/s/")) {
            Matcher matcher = ampExtract.matcher(url);
            while (matcher.find()) {
                newUrl = "https://" + matcher.group(1);
            }
        } else if (Arrays.asList(youtube_domains).contains(host)) { //Youtube URL
            boolean invidious_enabled = sharedpreferences.getBoolean(SET_INVIDIOUS_ENABLED, true);
            if (invidious_enabled) {
                Matcher matcher = youtubePattern.matcher(url);
                while (matcher.find()) {
                    final String youtubeId = matcher.group(3);
                    String invidiousHost = sharedpreferences.getString(MainActivity.SET_INVIDIOUS_HOST, MainActivity.DEFAULT_INVIDIOUS_HOST).toLowerCase();
                    if (Objects.requireNonNull(matcher.group(2)).compareTo("youtu.be") == 0) {
                        newUrl = "https://" + invidiousHost + "/watch?v=" + youtubeId + "&local=true";
                    } else {
                        newUrl = "https://" + invidiousHost + "/" + youtubeId + "&local=true";
                    }
                }
            }
        } else if (Arrays.asList(shortener_domains).contains(host)) {
            String finalUrl = url;
            String finalExtraText = extraText;
            Thread thread = new Thread() {
                @Override
                public void run() {
                    notShortnedURLDialog.add(finalUrl);
                    Utils.checkUrl(TransformActivity.this, notShortnedURLDialog);

                    URL url_;
                    String host = null;
                    try {
                        url_ = new URL(notShortnedURLDialog.get(notShortnedURLDialog.size() - 1));
                        host = url_.getHost();
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }

                    boolean nitter_enabled = sharedpreferences.getBoolean(SET_NITTER_ENABLED, true);
                    boolean invidious_enabled = sharedpreferences.getBoolean(SET_INVIDIOUS_ENABLED, true);
                    boolean osm_enabled = sharedpreferences.getBoolean(MainActivity.SET_OSM_ENABLED, true);
                    if (nitter_enabled && Arrays.asList(twitter_domains).contains(host)) {
                        Matcher matcher = nitterPattern.matcher(notShortnedURLDialog.get(notShortnedURLDialog.size() - 1));
                        String newUrlFinal = notShortnedURLDialog.get(notShortnedURLDialog.size() - 1);
                        while (matcher.find()) {
                            final String nitter_directory = matcher.group(2);
                            String nitterHost = sharedpreferences.getString(MainActivity.SET_NITTER_HOST, MainActivity.DEFAULT_NITTER_HOST).toLowerCase();
                            newUrlFinal = "https://" + nitterHost + nitter_directory;
                        }
                        String newExtraText = finalExtraText.replaceAll(Pattern.quote(finalUrl), Matcher.quoteReplacement(newUrlFinal));
                        Intent sendIntent = new Intent();
                        sendIntent.setAction(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_TEXT, newExtraText);
                        sendIntent.setType("text/plain");
                        startActivity(sendIntent);
                    } else if (invidious_enabled && Arrays.asList(youtube_domains).contains(host)) {
                        Matcher matcher = youtubePattern.matcher(notShortnedURLDialog.get(notShortnedURLDialog.size() - 1));
                        String newUrlFinal = notShortnedURLDialog.get(notShortnedURLDialog.size() - 1);
                        while (matcher.find()) {
                            final String youtubeId = matcher.group(3);
                            String invidiousHost = sharedpreferences.getString(MainActivity.SET_INVIDIOUS_HOST, MainActivity.DEFAULT_INVIDIOUS_HOST).toLowerCase();
                            if (Objects.requireNonNull(matcher.group(2)).compareTo("youtu.be") == 0) {
                                newUrlFinal = "https://" + invidiousHost + "/watch?v=" + youtubeId + "&local=true";
                            } else {
                                newUrlFinal = "https://" + invidiousHost + "/" + youtubeId + "&local=true";
                            }
                        }
                        String newExtraText = finalExtraText.replaceAll(Pattern.quote(finalUrl), Matcher.quoteReplacement(newUrlFinal));
                        Intent sendIntent = new Intent();
                        sendIntent.setAction(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_TEXT, newExtraText);
                        sendIntent.setType("text/plain");
                        startActivity(sendIntent);
                    } else if (osm_enabled && notShortnedURLDialog.get(notShortnedURLDialog.size() - 1).contains("/maps/place/")) {
                        String newUrlFinal = notShortnedURLDialog.get(notShortnedURLDialog.size() - 1);
                        Matcher matcher = maps.matcher(notShortnedURLDialog.get(notShortnedURLDialog.size() - 1));
                        while (matcher.find()) {
                            final String localization = matcher.group(1);
                            assert localization != null;
                            String[] data = localization.split(",");
                            if (data.length > 2) {
                                String zoom;
                                String[] details = data[2].split("\\.");
                                if (details.length > 0) {
                                    zoom = details[0];
                                } else {
                                    zoom = data[2];
                                }
                                String osmHost = sharedpreferences.getString(MainActivity.SET_OSM_HOST, MainActivity.DEFAULT_OSM_HOST).toLowerCase();
                                newUrlFinal = "https://" + osmHost + "/#map=" + zoom + "/" + data[0] + "/" + data[1];
                            }
                        }
                        String newExtraText = finalExtraText.replaceAll(Pattern.quote(finalUrl), Matcher.quoteReplacement(newUrlFinal));
                        Intent sendIntent = new Intent();
                        sendIntent.setAction(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_TEXT, newExtraText);
                        sendIntent.setType("text/plain");
                        startActivity(sendIntent);
                    } else {
                        String newExtraText = finalExtraText.replaceAll(Pattern.quote(finalUrl), Matcher.quoteReplacement(notShortnedURLDialog.get(notShortnedURLDialog.size() - 1)));
                        Intent sendIntent = new Intent();
                        sendIntent.setAction(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_TEXT, newExtraText);
                        sendIntent.setType("text/plain");
                        startActivity(sendIntent);
                    }
                }
            };
            thread.start();
            return;
        } else {
            newUrl = remove_tracking_param(url);
        }
        if (newUrl != null) {
            extraText = extraText.replaceAll(Pattern.quote(url), Matcher.quoteReplacement(newUrl));
        }
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, extraText);
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }


}
