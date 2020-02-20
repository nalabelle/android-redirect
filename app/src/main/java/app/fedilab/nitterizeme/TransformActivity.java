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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static app.fedilab.nitterizeme.MainActivity.SET_BIBLIOGRAM_ENABLED;
import static app.fedilab.nitterizeme.MainActivity.SET_INVIDIOUS_ENABLED;
import static app.fedilab.nitterizeme.MainActivity.SET_NITTER_ENABLED;
import static app.fedilab.nitterizeme.MainActivity.instagram_domains;
import static app.fedilab.nitterizeme.MainActivity.shortener_domains;
import static app.fedilab.nitterizeme.MainActivity.twitter_domains;
import static app.fedilab.nitterizeme.MainActivity.youtube_domains;


public class TransformActivity extends Activity {


    final Pattern youtubePattern = Pattern.compile("(www\\.|m\\.)?(youtube\\.com|youtu\\.be|youtube-nocookie\\.com)/(((?!([\"'<])).)*)");
    final Pattern nitterPattern = Pattern.compile("(mobile\\.|www\\.)?twitter.com([\\w-/]+)");
    final Pattern bibliogramPattern = Pattern.compile("(m\\.|www\\.)?instagram.com([\\w-/]+)");
    final Pattern maps = Pattern.compile("/maps/place/[^@]+@([\\d.,z]{3,}).*");
    final Pattern extractPlace = Pattern.compile("/maps/place/(((?!/data).)*)");
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

                AlertDialog.Builder unshortenAlertBuilder = new AlertDialog.Builder(TransformActivity.this);
                unshortenAlertBuilder.setTitle(R.string.shortened_detected);
                View view = getLayoutInflater().inflate(R.layout.popup_unshorten, new LinearLayout(getApplicationContext()), false);
                unshortenAlertBuilder.setView(view);
                unshortenAlertBuilder.setIcon(R.mipmap.ic_launcher);
                unshortenAlertBuilder.setPositiveButton(R.string.open, (dialog, id) -> {
                    if (notShortnedURLDialog.size() > 0) {
                        URL url_1;
                        String realHost = null;
                        try {
                            url_1 = new URL(notShortnedURLDialog.get(notShortnedURLDialog.size() - 1));
                            realHost = url_1.getHost();
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                        if (Arrays.asList(twitter_domains).contains(realHost)) {
                            boolean nitter_enabled = sharedpreferences.getBoolean(SET_NITTER_ENABLED, true);
                            if (nitter_enabled) {
                                Intent delegate = new Intent(Intent.ACTION_VIEW);
                                String transformedURL = transformUrl(url);
                                if (transformedURL != null) {
                                    delegate.setData(Uri.parse(transformUrl(url)));
                                    delegate.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    if (delegate.resolveActivity(getPackageManager()) != null) {
                                        startActivity(delegate);
                                    }
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
                                String transformedURL = transformUrl(url);
                                if (transformedURL != null) {
                                    delegate.setData(Uri.parse(transformUrl(url)));
                                    delegate.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    if (delegate.resolveActivity(getPackageManager()) != null) {
                                        startActivity(delegate);
                                    }
                                } else {
                                    forwardToBrowser(intent);
                                }
                            } else {
                                forwardToBrowser(intent);
                            }
                        }
                        //YouTube URLs
                        else if (Arrays.asList(youtube_domains).contains(realHost)) { //Youtube URL
                            boolean invidious_enabled = sharedpreferences.getBoolean(SET_INVIDIOUS_ENABLED, true);
                            if (invidious_enabled) {
                                Intent delegate = new Intent(Intent.ACTION_VIEW);
                                String transformedURL = transformUrl(url);
                                if (transformedURL != null) {
                                    delegate.setData(Uri.parse(transformUrl(url)));
                                    delegate.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    if (delegate.resolveActivity(getPackageManager()) != null) {
                                        startActivity(delegate);
                                    }
                                } else {
                                    forwardToBrowser(intent);
                                }
                            } else {
                                forwardToBrowser(intent);
                            }
                        } else {
                            Intent delegate = new Intent(Intent.ACTION_VIEW);
                            delegate.setData(Uri.parse(notShortnedURLDialog.get(notShortnedURLDialog.size() - 1)));
                            delegate.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            if (delegate.resolveActivity(getPackageManager()) != null) {
                                startActivity(delegate);
                            }
                        }
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
                        Utils.checkUrl(notShortnedURLDialog);
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
                    String transformedURL = transformUrl(url);
                    if (transformedURL != null) {
                        delegate.setData(Uri.parse(transformUrl(url)));
                        delegate.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        if (delegate.resolveActivity(getPackageManager()) != null) {
                            startActivity(delegate);
                        }
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
                    String transformedURL = transformUrl(url);
                    if (transformedURL != null) {
                        delegate.setData(Uri.parse(transformUrl(url)));
                        delegate.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        if (delegate.resolveActivity(getPackageManager()) != null) {
                            startActivity(delegate);
                        }
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
                    String transformedURL = transformUrl(url);
                    if (transformedURL != null) {
                        delegate.setData(Uri.parse(transformUrl(url)));
                        delegate.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        if (delegate.resolveActivity(getPackageManager()) != null) {
                            startActivity(delegate);
                        }
                    } else {
                        forwardToBrowser(intent);
                    }
                } else {
                    forwardToBrowser(intent);
                }
            }
            //YouTube URLs
            else if (Arrays.asList(youtube_domains).contains(host)) { //Youtube URL
                boolean invidious_enabled = sharedpreferences.getBoolean(SET_INVIDIOUS_ENABLED, true);
                if (invidious_enabled) {
                    Intent delegate = new Intent(Intent.ACTION_VIEW);
                    String transformedURL = transformUrl(url);
                    if (transformedURL != null) {
                        delegate.setData(Uri.parse(transformUrl(url)));
                        delegate.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        if (delegate.resolveActivity(getPackageManager()) != null) {
                            startActivity(delegate);
                        }
                    } else {
                        forwardToBrowser(intent);
                    }
                } else {
                    forwardToBrowser(intent);
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
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(i.getData(), i.getType());
        List<ResolveInfo> activities = getPackageManager().queryIntentActivities(intent, 0);
        ArrayList<Intent> targetIntents = new ArrayList<>();
        String thisPackageName = getApplicationContext().getPackageName();
        for (ResolveInfo currentInfo : activities) {
            String packageName = currentInfo.activityInfo.packageName;
            if (!thisPackageName.equals(packageName)) {
                Intent targetIntent = new Intent(Intent.ACTION_VIEW);
                targetIntent.setDataAndType(intent.getData(), intent.getType());
                targetIntent.setPackage(intent.getPackage());
                targetIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                targetIntent.setComponent(new ComponentName(packageName, currentInfo.activityInfo.name));
                targetIntents.add(targetIntent);
            }
        }
        if (targetIntents.size() > 0) {
            Intent chooserIntent = Intent.createChooser(targetIntents.remove(0), getString(R.string.open_with));
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetIntents.toArray(new Parcelable[]{}));
            startActivity(chooserIntent);
            finish();
        }
    }


    /**
     * Transform the URL to a Nitter, Invidious or OSM ones
     *
     * @param url String original URL
     * @return String transformed URL
     */
    private String transformUrl(String url) {
        SharedPreferences sharedpreferences = getSharedPreferences(MainActivity.APP_PREFS, Context.MODE_PRIVATE);
        String newUrl = null;

        URL url_;
        String host = null;
        try {
            url_ = new URL(url);
            host = url_.getHost();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        if (Arrays.asList(twitter_domains).contains(host)) {
            boolean nitter_enabled = sharedpreferences.getBoolean(SET_NITTER_ENABLED, true);
            if (nitter_enabled) {
                Matcher matcher = nitterPattern.matcher(url);
                while (matcher.find()) {
                    final String nitter_directory = matcher.group(2);
                    String nitterHost = sharedpreferences.getString(MainActivity.SET_NITTER_HOST, MainActivity.DEFAULT_NITTER_HOST).toLowerCase();
                    newUrl = "https://" + nitterHost + nitter_directory;
                }
                return newUrl;
            } else {
                return url;
            }
        } else if (Arrays.asList(instagram_domains).contains(host)) {
            boolean bibliogram_enabled = sharedpreferences.getBoolean(SET_BIBLIOGRAM_ENABLED, true);
            if (bibliogram_enabled) {
                Matcher matcher = bibliogramPattern.matcher(url);
                while (matcher.find()) {
                    final String bibliogram_directory = matcher.group(2);
                    String bibliogramHost = sharedpreferences.getString(MainActivity.SET_BIBLIOGRAM_HOST, MainActivity.DEFAULT_BIBLIOGRAM_HOST).toLowerCase();
                    newUrl = "https://" + bibliogramHost + bibliogram_directory;
                }
                return newUrl;
            } else {
                return url;
            }
        } else if (url.contains("/maps/place")) {
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
                        boolean geo_uri_enabled = sharedpreferences.getBoolean(MainActivity.SET_GEO_URIS, false);
                        if (!geo_uri_enabled) {
                            newUrl = "https://" + osmHost + "/#map=" + zoom + "/" + data[0] + "/" + data[1];
                        } else {
                            newUrl = "geo:0,0?q=" + data[0] + "," + data[1] + ",z=" + zoom;
                        }
                    }
                }
                if (newUrl == null && url.contains("/data=")) {
                    matcher = extractPlace.matcher(url);
                    while (matcher.find()) {
                        final String search = matcher.group(1);
                        newUrl = "geo:0,0?q=" + search;
                    }
                }
                return newUrl;
            } else {
                return url;
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
                return newUrl;
            } else {
                return url;
            }
        }
        return null;
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
                Matcher matcher = nitterPattern.matcher(url);
                while (matcher.find()) {
                    final String nitter_directory = matcher.group(2);
                    String nitterHost = sharedpreferences.getString(MainActivity.SET_NITTER_HOST, MainActivity.DEFAULT_NITTER_HOST).toLowerCase();
                    newUrl = "https://" + nitterHost + nitter_directory;
                }
            }
        } else if (Arrays.asList(instagram_domains).contains(host)) {
            boolean bibliogram_enabled = sharedpreferences.getBoolean(SET_BIBLIOGRAM_ENABLED, true);
            if (bibliogram_enabled) {
                Matcher matcher = bibliogramPattern.matcher(url);
                while (matcher.find()) {
                    final String bibliogram_directory = matcher.group(2);
                    String bibliogramHost = sharedpreferences.getString(MainActivity.SET_BIBLIOGRAM_HOST, MainActivity.DEFAULT_BIBLIOGRAM_HOST).toLowerCase();
                    newUrl = "https://" + bibliogramHost + bibliogram_directory;
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
                    Utils.checkUrl(notShortnedURLDialog);

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
