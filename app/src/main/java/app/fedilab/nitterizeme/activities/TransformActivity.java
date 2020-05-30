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
import android.util.Patterns;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.fedilab.nitterizeme.BuildConfig;
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
import static app.fedilab.nitterizeme.helpers.Utils.ampExtract;
import static app.fedilab.nitterizeme.helpers.Utils.bibliogramAccountPattern;
import static app.fedilab.nitterizeme.helpers.Utils.bibliogramPostPattern;
import static app.fedilab.nitterizeme.helpers.Utils.forwardToBrowser;
import static app.fedilab.nitterizeme.helpers.Utils.manageShortened;
import static app.fedilab.nitterizeme.helpers.Utils.manageShortenedShare;
import static app.fedilab.nitterizeme.helpers.Utils.maps;
import static app.fedilab.nitterizeme.helpers.Utils.nitterPattern;
import static app.fedilab.nitterizeme.helpers.Utils.remove_tracking_param;
import static app.fedilab.nitterizeme.helpers.Utils.transformUrl;
import static app.fedilab.nitterizeme.helpers.Utils.youtubePattern;


public class TransformActivity extends Activity {


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
                manageShortened(TransformActivity.this, url);
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
                        if (BuildConfig.fullLinks) {
                            forwardToBrowser(TransformActivity.this, delegate);
                        } else {
                            if (delegate.resolveActivity(getPackageManager()) != null) {
                                startActivity(delegate);
                                finish();
                            }
                        }
                    } else {
                        forwardToBrowser(TransformActivity.this, intent);
                    }
                } else {
                    forwardToBrowser(TransformActivity.this, intent);
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
                        if (BuildConfig.fullLinks) {
                            forwardToBrowser(TransformActivity.this, delegate);
                        } else {
                            if (delegate.resolveActivity(getPackageManager()) != null) {
                                startActivity(delegate);
                                finish();
                            }
                        }
                    } else {
                        forwardToBrowser(TransformActivity.this, intent);
                    }
                } else {
                    forwardToBrowser(TransformActivity.this, intent);
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
                        if (BuildConfig.fullLinks) {
                            forwardToBrowser(TransformActivity.this, delegate);
                        } else {
                            if (delegate.resolveActivity(getPackageManager()) != null) {
                                startActivity(delegate);
                                finish();
                            }
                        }
                    } else {
                        forwardToBrowser(TransformActivity.this, intent);
                    }
                } else {
                    forwardToBrowser(TransformActivity.this, intent);
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
                    if (BuildConfig.fullLinks) {
                        forwardToBrowser(TransformActivity.this, delegate);
                    } else {
                        if (delegate.resolveActivity(getPackageManager()) != null) {
                            startActivity(delegate);
                            finish();
                        }
                    }
                } else {
                    forwardToBrowser(TransformActivity.this, intent);
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
                        if (BuildConfig.fullLinks) {
                            forwardToBrowser(TransformActivity.this, delegate);
                        } else {
                            if (delegate.resolveActivity(getPackageManager()) != null) {
                                startActivity(delegate);
                                finish();
                            }
                        }
                    } else {
                        forwardToBrowser(TransformActivity.this, intent);
                    }
                } else {
                    forwardToBrowser(TransformActivity.this, intent);
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
                    transformedURL = Utils.replaceInvidiousParams(TransformActivity.this, transformedURL);
                    intent.setData(Uri.parse(transformedURL));
                    forwardToBrowser(TransformActivity.this, intent);
                } else {
                    forwardToBrowser(TransformActivity.this, intent);
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
                    forwardToBrowser(TransformActivity.this, intent);
                } else {
                    forwardToBrowser(TransformActivity.this, intent);
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
                    forwardToBrowser(TransformActivity.this, intent);
                } else {
                    forwardToBrowser(TransformActivity.this, intent);
                }
            } else {
                String newUrl = remove_tracking_param(url);
                try {
                    url_ = new URL(newUrl);
                    host = url_.getHost();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                //Shortened URLs
                if (Arrays.asList(shortener_domains).contains(host)) {
                    manageShortened(TransformActivity.this, newUrl);
                } else {
                    intent.setData(Uri.parse(newUrl));
                    forwardToBrowser(TransformActivity.this, intent);
                }
            }

        }
        //It's a sharing intent
        else if (Objects.requireNonNull(intent.getAction()).equals(Intent.ACTION_SEND)) {
            share(intent.getStringExtra(Intent.EXTRA_TEXT));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
            forwardToBrowser(TransformActivity.this, sendIntent);
            return;
        }
        Uri url_r = Uri.parse(url);
        String scheme = url_r.getScheme();
        if (scheme == null) {
            scheme = "https://";
        } else {
            scheme += "://";
        }

        if (Arrays.asList(twitter_domains).contains(host)) {
            boolean nitter_enabled = sharedpreferences.getBoolean(SET_NITTER_ENABLED, true);
            if (nitter_enabled) {

                String nitterHost = sharedpreferences.getString(MainActivity.SET_NITTER_HOST, MainActivity.DEFAULT_NITTER_HOST).toLowerCase();
                assert host != null;
                if (host.compareTo("pbs.twimg.com") == 0 || host.compareTo("pic.twitter.com") == 0) {
                    try {
                        newUrl = scheme + nitterHost + "/pic/" + URLEncoder.encode(url, "utf-8");
                    } catch (UnsupportedEncodingException e) {
                        newUrl = scheme + nitterHost + "/pic/" + url;
                    }
                } else if (url.contains("/search?")) {
                    newUrl = url.replace(host, nitterHost);
                } else {
                    Matcher matcher = nitterPattern.matcher(url);
                    while (matcher.find()) {
                        final String nitter_directory = matcher.group(2);
                        newUrl = scheme + nitterHost + nitter_directory;
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
                    newUrl = scheme + bibliogramHost + bibliogram_directory;
                }
                matcher = bibliogramAccountPattern.matcher(url);
                while (matcher.find()) {
                    final String bibliogram_directory = matcher.group(2);
                    String bibliogramHost = sharedpreferences.getString(MainActivity.SET_BIBLIOGRAM_HOST, MainActivity.DEFAULT_BIBLIOGRAM_HOST).toLowerCase();
                    if (bibliogram_directory != null && bibliogram_directory.compareTo("privacy") != 0) {
                        newUrl = scheme + bibliogramHost + "/u" + bibliogram_directory;
                    } else {
                        newUrl = scheme + bibliogramHost + bibliogram_directory;
                    }
                }
            }
        } else if (url.contains("/maps/place/")) {
            boolean osm_enabled = sharedpreferences.getBoolean(MainActivity.SET_OSM_ENABLED, true);
            if (osm_enabled) {
                Matcher matcher = maps.matcher(url);
                while (matcher.find()) {
                    final String localization = matcher.group(2);
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
                        newUrl = scheme + osmHost + "/#map=" + zoom + "/" + data[0] + "/" + data[1];
                    }
                }
            }
        } else if (url.contains("/amp/s/")) {
            Matcher matcher = ampExtract.matcher(url);
            while (matcher.find()) {
                newUrl = scheme + matcher.group(1);
            }
        } else if (Arrays.asList(youtube_domains).contains(host)) { //Youtube URL
            boolean invidious_enabled = sharedpreferences.getBoolean(SET_INVIDIOUS_ENABLED, true);
            if (invidious_enabled) {
                Matcher matcher = youtubePattern.matcher(url);
                while (matcher.find()) {
                    final String youtubeId = matcher.group(3);
                    String invidiousHost = sharedpreferences.getString(MainActivity.SET_INVIDIOUS_HOST, MainActivity.DEFAULT_INVIDIOUS_HOST).toLowerCase();
                    if (Objects.requireNonNull(matcher.group(2)).compareTo("youtu.be") == 0) {
                        newUrl = scheme + invidiousHost + "/watch?v=" + youtubeId;
                    } else {
                        newUrl = scheme + invidiousHost + "/" + youtubeId;
                    }
                }
            }
        } else if (Arrays.asList(shortener_domains).contains(host)) {
            manageShortenedShare(TransformActivity.this, url, extraText, scheme);
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
        if (BuildConfig.fullLinks) {
            forwardToBrowser(TransformActivity.this, sendIntent);
        } else {
            startActivity(sendIntent);
            finish();
        }
    }

}
