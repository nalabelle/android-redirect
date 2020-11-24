package app.fedilab.nitterizeme.helpers;
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
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import app.fedilab.nitterizeme.R;
import app.fedilab.nitterizeme.activities.MainActivity;

import static app.fedilab.nitterizeme.activities.MainActivity.SET_BIBLIOGRAM_ENABLED;
import static app.fedilab.nitterizeme.activities.MainActivity.SET_INVIDIOUS_ENABLED;
import static app.fedilab.nitterizeme.activities.MainActivity.SET_NITTER_ENABLED;
import static app.fedilab.nitterizeme.data.Domains.bibliogram_instances;
import static app.fedilab.nitterizeme.data.Domains.instagram_domains;
import static app.fedilab.nitterizeme.data.Domains.invidious_instances;
import static app.fedilab.nitterizeme.data.Domains.nitter_instances;
import static app.fedilab.nitterizeme.data.Domains.outlook_safe_domain;
import static app.fedilab.nitterizeme.data.Domains.shortener_domains;
import static app.fedilab.nitterizeme.data.Domains.twitter_domains;
import static app.fedilab.nitterizeme.data.Domains.youtube_domains;

public class Utils {

    public static final String KILL_ACTIVITY = "kill_activity";

    public static final Pattern youtubePattern = Pattern.compile("(www\\.|m\\.)?(youtube\\.com|youtu\\.be|youtube-nocookie\\.com)/(((?!([\"'<])).)*)");
    public static final Pattern nitterPattern = Pattern.compile("(mobile\\.|www\\.)?twitter.com([\\w-/]+)");
    public static final Pattern bibliogramPostPattern = Pattern.compile("(m\\.|www\\.)?instagram.com(/p/[\\w-/]+)");

    public static final Pattern bibliogramAccountPattern = Pattern.compile("(m\\.|www\\.)?instagram.com(((?!/p/).)+)");
    public static final Pattern maps = Pattern.compile("/maps/place/([^@]+@)?([\\d.,z]+).*");
    public static final Pattern ampExtract = Pattern.compile("amp/s/(.*)");

    public static final Pattern outlookRedirect = Pattern.compile("(.*)safelinks\\.protection\\.outlook\\.com/?[?]?((?!url).)*url=([^&]+)");
    private static final Pattern extractPlace = Pattern.compile("/maps/place/(((?!/data).)*)");
    private static final Pattern googleRedirect = Pattern.compile("https?://(www\\.)?google(\\.\\w{2,})?(\\.\\w{2,})/url\\?q=(.*)");
    private static final String[] G_TRACKING = {
            "sourceid",
            "aqs",
            "client",
            "source",
            "ust",
            "usg"
    };

    private static final String[] UTM_PARAMS = {
            "utm_\\w+",
            "ga_source",
            "ga_medium",
            "ga_term",
            "ga_content",
            "ga_campaign",
            "ga_place",
            "yclid",
            "_openstat",
            "fb_action_ids",
            "fb_action_types",
            "fb_source",
            "fb_ref",
            "fbclid",
            "action_object_map",
            "action_type_map",
            "action_ref_map",
            "gs_l",
            "mkt_tok",
            "hmb_campaign",
            "hmb_medium",
            "hmb_source",
            "[\\?|&]ref[\\_]?",
            "amp[_#\\w]+",
            "click"
    };


    private static final String urlRegex = "(?i)\\b((?:[a-z][\\w-]+:(?:/{1,3}|[a-z0-9%])|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,10}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))";
    public static final Pattern urlPattern = Pattern.compile(
            urlRegex,
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    /**
     * Returns the unshortened URL
     *
     * @param urls ArrayList<String> URL to check
     */
    private static void checkUrl(Context context, ArrayList<String> urls) {
        URL url;
        String newURL = null;
        String comingURl;
        try {
            comingURl = urls.get(urls.size() - 1);

            url = new URL(comingURl);
            if (comingURl.startsWith("https")) {
                HttpsURLConnection httpsURLConnection = (HttpsURLConnection) url.openConnection();
                httpsURLConnection.setRequestProperty("http.keepAlive", "false");
                httpsURLConnection.setInstanceFollowRedirects(false);
                httpsURLConnection.setRequestMethod("HEAD");
                if (httpsURLConnection.getResponseCode() == 301 || httpsURLConnection.getResponseCode() == 302) {
                    Map<String, List<String>> map = httpsURLConnection.getHeaderFields();
                    for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                        if (entry.toString().toLowerCase().startsWith("location")) {
                            Matcher matcher = urlPattern.matcher(entry.toString());
                            if (matcher.find()) {
                                newURL = remove_tracking_param(matcher.group(1));
                                urls.add(transformUrl(context, newURL));
                            }
                        }
                    }
                }
                httpsURLConnection.getInputStream().close();
            } else {
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestProperty("http.keepAlive", "false");
                httpURLConnection.setInstanceFollowRedirects(false);
                httpURLConnection.setRequestMethod("HEAD");
                if (httpURLConnection.getResponseCode() == 301) {
                    Map<String, List<String>> map = httpURLConnection.getHeaderFields();
                    for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                        if (entry.toString().toLowerCase().startsWith("location")) {
                            Matcher matcher = urlPattern.matcher(entry.toString());
                            if (matcher.find()) {
                                newURL = remove_tracking_param(matcher.group(1));
                                urls.add(transformUrl(context, newURL));
                            }
                        }
                    }
                }
                httpURLConnection.getInputStream().close();
            }
            if (newURL != null && newURL.compareTo(comingURl) != 0) {
                URL redirectURL = new URL(newURL);
                String host = redirectURL.getHost();
                String protocol = redirectURL.getProtocol();
                if (protocol != null && host != null) {
                    if (Arrays.asList(shortener_domains).contains(host)) {
                        checkUrl(context, urls);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Transform the URL to a Nitter, Invidious or OSM ones
     *
     * @param url String original URL
     * @return String transformed URL
     */
    public static String transformUrl(Context context, String url) {
        SharedPreferences sharedpreferences = context.getSharedPreferences(MainActivity.APP_PREFS, Context.MODE_PRIVATE);
        String newUrl = null;
        URL url_;
        String host = null;
        url = Utils.remove_tracking_param(url);
        try {
            url_ = new URL(url);
            host = url_.getHost();
        } catch (MalformedURLException e) {
            e.printStackTrace();
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
                String nitterHost = sharedpreferences.getString(MainActivity.SET_NITTER_HOST, MainActivity.DEFAULT_NITTER_HOST);
                assert nitterHost != null;
                nitterHost = nitterHost.toLowerCase();
                if (nitterHost.startsWith("http")) {
                    scheme = "";
                }
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
                if (newUrl != null && (newUrl.endsWith("tweets") || newUrl.endsWith("tweets/"))) {
                    newUrl = newUrl.replaceAll("/tweets/?", "");
                }
                return newUrl;
            } else {
                return url;
            }
        } else if (Arrays.asList(instagram_domains).contains(host)) {
            boolean bibliogram_enabled = sharedpreferences.getBoolean(SET_BIBLIOGRAM_ENABLED, true);
            if (bibliogram_enabled) {
                String bibliogramHost = sharedpreferences.getString(MainActivity.SET_BIBLIOGRAM_HOST, MainActivity.DEFAULT_BIBLIOGRAM_HOST);
                assert bibliogramHost != null;
                bibliogramHost = bibliogramHost.toLowerCase();
                if (bibliogramHost.startsWith("http")) {
                    scheme = "";
                }
                Matcher matcher = bibliogramPostPattern.matcher(url);
                while (matcher.find()) {
                    final String bibliogram_directory = matcher.group(2);

                    newUrl = scheme + bibliogramHost + bibliogram_directory;
                }
                matcher = bibliogramAccountPattern.matcher(url);
                while (matcher.find()) {
                    final String bibliogram_directory = matcher.group(2);
                    if (bibliogram_directory != null && bibliogram_directory.compareTo("privacy") != 0 && !bibliogram_directory.startsWith("/tv/") && !bibliogram_directory.startsWith("/reel/") && !bibliogram_directory.startsWith("/igtv/")) {
                        newUrl = scheme + bibliogramHost + "/u" + bibliogram_directory;
                    } else {
                        newUrl = scheme + bibliogramHost + bibliogram_directory;
                    }
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
                    final String localization = matcher.group(2);
                    assert localization != null;
                    String[] data = localization.split(",");
                    if (data.length >= 2) {
                        String zoom;
                        if (data.length > 2) {
                            String[] details = data[2].split("\\.");
                            if (details.length > 0) {
                                zoom = details[0];
                            } else {
                                zoom = data[2];
                            }
                        } else {
                            zoom = "16";
                        }
                        String osmHost = sharedpreferences.getString(MainActivity.SET_OSM_HOST, MainActivity.DEFAULT_OSM_HOST);
                        assert osmHost != null;
                        osmHost = osmHost.toLowerCase();
                        boolean geo_uri_enabled = sharedpreferences.getBoolean(MainActivity.SET_GEO_URIS, false);
                        if (!geo_uri_enabled) {
                            newUrl = scheme + osmHost + "/#map=" + zoom + "/" + data[0] + "/" + data[1];
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
        } else if (url.contains("/amp/s/")) {
            Matcher matcher = ampExtract.matcher(url);
            String transformedURL = url;
            while (matcher.find()) {
                transformedURL = "https://" + matcher.group(1);
            }
            return transformedURL;
        } else if (Arrays.asList(youtube_domains).contains(host)) { //Youtube URL
            boolean invidious_enabled = sharedpreferences.getBoolean(SET_INVIDIOUS_ENABLED, true);
            if (invidious_enabled) {
                String invidiousHost = sharedpreferences.getString(MainActivity.SET_INVIDIOUS_HOST, MainActivity.DEFAULT_INVIDIOUS_HOST);
                assert invidiousHost != null;
                invidiousHost = invidiousHost.toLowerCase();
                if (invidiousHost.startsWith("http")) {
                    scheme = "";
                }
                Matcher matcher = youtubePattern.matcher(url);
                while (matcher.find()) {
                    String youtubeId = matcher.group(3);

                    if (Objects.requireNonNull(matcher.group(2)).compareTo("youtu.be") == 0) {
                        if (youtubeId != null && youtubeId.contains("?t=")) {
                            youtubeId = youtubeId.replace("?t=", "&t=");
                        }
                        newUrl = scheme + invidiousHost + "/watch?v=" + youtubeId;
                    } else {
                        newUrl = scheme + invidiousHost + "/" + youtubeId;
                    }
                }
                return newUrl;
            } else {
                return url;
            }
        } else if (Arrays.asList(invidious_instances).contains(host)) {
            boolean invidious_enabled = sharedpreferences.getBoolean(SET_INVIDIOUS_ENABLED, true);
            newUrl = url;
            if (invidious_enabled) {
                String invidiousHost = sharedpreferences.getString(MainActivity.SET_INVIDIOUS_HOST, MainActivity.DEFAULT_INVIDIOUS_HOST);
                assert invidiousHost != null;
                invidiousHost = invidiousHost.toLowerCase();
                if (host != null && host.compareTo(invidiousHost) != 0) {
                    if (!invidiousHost.startsWith("http")) {
                        newUrl = url.replace(host, invidiousHost);
                    } else {
                        newUrl = url.replace("https://" + host, invidiousHost).replace("http://" + host, invidiousHost);
                    }
                }
            }
            return newUrl;
        }
        //Transform a Nitter URL from an instance to another one selected by the end user.
        else if (Arrays.asList(nitter_instances).contains(host)) {
            newUrl = url;
            boolean nitter_enabled = sharedpreferences.getBoolean(SET_NITTER_ENABLED, true);
            if (nitter_enabled) {
                String nitterHost = sharedpreferences.getString(MainActivity.SET_NITTER_HOST, MainActivity.DEFAULT_NITTER_HOST);
                assert nitterHost != null;
                nitterHost = nitterHost.toLowerCase();
                if (host != null && host.compareTo(nitterHost) != 0) {
                    if (!nitterHost.startsWith("http")) {
                        newUrl = url.replace(host, nitterHost);
                    } else {
                        newUrl = url.replace("https://" + host, nitterHost).replace("http://" + host, nitterHost);
                    }
                }
            }
            return newUrl;
        }
        //Transform a Bibliogram URL from an instance to another one selected by the end user.
        else if (Arrays.asList(bibliogram_instances).contains(host)) {
            newUrl = url;
            boolean bibliogram_enabled = sharedpreferences.getBoolean(SET_BIBLIOGRAM_ENABLED, true);
            if (bibliogram_enabled) {
                String bibliogramHost = sharedpreferences.getString(MainActivity.SET_BIBLIOGRAM_HOST, MainActivity.DEFAULT_BIBLIOGRAM_HOST);
                assert bibliogramHost != null;
                bibliogramHost = bibliogramHost.toLowerCase();
                if (host != null && host.compareTo(bibliogramHost) != 0) {
                    if (!bibliogramHost.startsWith("http")) {
                        newUrl = url.replace(host, bibliogramHost);
                    } else {
                        newUrl = url.replace("https://" + host, bibliogramHost).replace("http://" + host, bibliogramHost);
                    }
                }
            }
            return newUrl;
        } else if (host != null && host.contains(outlook_safe_domain)) {
            newUrl = url;
            Matcher matcher = outlookRedirect.matcher(url);
            if (matcher.find()) {
                String tmp_url = matcher.group(3);
                try {
                    newUrl = transformUrl(context, URLDecoder.decode(tmp_url, "UTF-8"));
                } catch (UnsupportedEncodingException ignored) {
                }
            }
            return newUrl;
        }
        return url;
    }

    /**
     * Clean URLs from utm parameters
     *
     * @param url String URL
     * @return cleaned URL String
     */
    public static String remove_tracking_param(String url) {
        if (url != null) {
            for (String utm : UTM_PARAMS) {
                url = url.replaceAll("&amp;" + utm + "=[0-9a-zA-Z._-]*", "");
                url = url.replaceAll("&" + utm + "=[0-9a-zA-Z._-]*", "");
                url = url.replaceAll("\\?" + utm + "=[0-9a-zA-Z._-]*", "?");
                url = url.replaceAll("/" + utm + "=" + urlRegex, "/");
                url = url.replaceAll("#" + utm + "=" + urlRegex, "");
            }
            try {
                Matcher matcher = googleRedirect.matcher(url);
                if (matcher.find()) {
                    url = matcher.group(4);
                }
                URL redirectURL = new URL(url);
                String host = redirectURL.getHost();
                if (host != null) {
                    for (String utm : G_TRACKING) {
                        assert url != null;
                        url = url.replaceAll("&amp;" + utm + "=[0-9a-zA-Z._-]*", "");
                        url = url.replaceAll("&" + utm + "=[0-9a-zA-Z._-]*", "");
                        url = url.replaceAll("\\?" + utm + "=[0-9a-zA-Z._-]*", "?");
                        url = url.replaceAll("/" + utm + "=" + urlRegex, "/");
                    }
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        if (url != null && url.endsWith("?")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }


    /**
     * Manage URLs when visiting a shortened URL
     *
     * @param context Context
     * @param url     String the shortened URL
     */
    public static void manageShortened(Context context, String url) {
        final ArrayList<String> notShortnedURLDialog = new ArrayList<>();
        AlertDialog.Builder unshortenAlertBuilder = new AlertDialog.Builder(context, R.style.AppThemeDialog);
        unshortenAlertBuilder.setTitle(R.string.shortened_detected);
        unshortenAlertBuilder.setOnDismissListener(dialog -> ((Activity) context).finish());
        View view = ((Activity) context).getLayoutInflater().inflate(R.layout.popup_unshorten, new LinearLayout(context), false);
        unshortenAlertBuilder.setView(view);
        unshortenAlertBuilder.setIcon(R.mipmap.ic_launcher);
        unshortenAlertBuilder.setPositiveButton(R.string.open, (dialog, id) -> {
            if (notShortnedURLDialog.size() > 0) {
                Intent delegate = new Intent(Intent.ACTION_VIEW);
                delegate.setData(Uri.parse(notShortnedURLDialog.get(notShortnedURLDialog.size() - 1)));
                delegate.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                forwardToBrowser(context, delegate);
            }
            dialog.dismiss();
            ((Activity) context).finish();
        });
        unshortenAlertBuilder.setNegativeButton(R.string.dismiss, (dialog, id) -> {
            dialog.dismiss();
            ((Activity) context).finish();
        });
        AlertDialog alertDialog = unshortenAlertBuilder.create();
        alertDialog.show();
        Button positiveButton = (alertDialog).getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setEnabled(false);
        Thread thread = new Thread() {
            @Override
            public void run() {
                notShortnedURLDialog.add(url);
                Utils.checkUrl(context, notShortnedURLDialog);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> {
                    positiveButton.setEnabled(true);
                    StringBuilder message;
                    if (notShortnedURLDialog.size() <= 1) {
                        message = new StringBuilder(context.getString(R.string.the_app_failed_shortened));
                    } else {
                        message = new StringBuilder(context.getString(R.string.try_to_redirect, notShortnedURLDialog.get(0), notShortnedURLDialog.get(1)));
                        if (notShortnedURLDialog.size() > 2) {
                            for (int i = 2; i < notShortnedURLDialog.size(); i++) {
                                message.append("\n\n").append(context.getString(R.string.try_to_redirect_again, notShortnedURLDialog.get(i)));
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

    /**
     * Manage URLs when trying to share a shortened URL
     *
     * @param context   Context
     * @param url       String coming URL
     * @param extraText String text when sharing content
     * @param scheme    String scheme of the URL
     */
    public static void manageShortenedShare(Context context, String url, String extraText, String scheme) {
        ArrayList<String> notShortnedURLDialog = new ArrayList<>();
        Thread thread = new Thread() {
            @Override
            public void run() {
                notShortnedURLDialog.add(url);
                Utils.checkUrl(context, notShortnedURLDialog);

                URL url_;
                String host = null;
                try {
                    url_ = new URL(notShortnedURLDialog.get(notShortnedURLDialog.size() - 1));
                    host = url_.getHost();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                SharedPreferences sharedpreferences = context.getSharedPreferences(MainActivity.APP_PREFS, Context.MODE_PRIVATE);
                boolean nitter_enabled = sharedpreferences.getBoolean(SET_NITTER_ENABLED, true);
                boolean invidious_enabled = sharedpreferences.getBoolean(SET_INVIDIOUS_ENABLED, true);
                boolean osm_enabled = sharedpreferences.getBoolean(MainActivity.SET_OSM_ENABLED, true);
                if (nitter_enabled && Arrays.asList(twitter_domains).contains(host)) {
                    Matcher matcher = nitterPattern.matcher(notShortnedURLDialog.get(notShortnedURLDialog.size() - 1));
                    String newUrlFinal = notShortnedURLDialog.get(notShortnedURLDialog.size() - 1);
                    while (matcher.find()) {
                        final String nitter_directory = matcher.group(2);
                        String nitterHost = sharedpreferences.getString(MainActivity.SET_NITTER_HOST, MainActivity.DEFAULT_NITTER_HOST);
                        assert nitterHost != null;
                        nitterHost = nitterHost.toLowerCase();
                        newUrlFinal = scheme + nitterHost + nitter_directory;
                    }
                    String newExtraText = extraText.replaceAll(Pattern.quote(url), Matcher.quoteReplacement(newUrlFinal));
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, newExtraText);
                    sendIntent.setType("text/plain");
                    forwardToBrowser(context, sendIntent);
                } else if (invidious_enabled && Arrays.asList(youtube_domains).contains(host)) {
                    Matcher matcher = youtubePattern.matcher(notShortnedURLDialog.get(notShortnedURLDialog.size() - 1));
                    String newUrlFinal = notShortnedURLDialog.get(notShortnedURLDialog.size() - 1);
                    while (matcher.find()) {
                        final String youtubeId = matcher.group(3);
                        String invidiousHost = sharedpreferences.getString(MainActivity.SET_INVIDIOUS_HOST, MainActivity.DEFAULT_INVIDIOUS_HOST);
                        assert invidiousHost != null;
                        invidiousHost = invidiousHost.toLowerCase();
                        if (Objects.requireNonNull(matcher.group(2)).compareTo("youtu.be") == 0) {
                            newUrlFinal = scheme + invidiousHost + "/watch?v=" + youtubeId;
                        } else {
                            newUrlFinal = scheme + invidiousHost + "/" + youtubeId;
                        }
                    }
                    String newExtraText = extraText.replaceAll(Pattern.quote(url), Matcher.quoteReplacement(newUrlFinal));
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, newExtraText);
                    sendIntent.setType("text/plain");
                    forwardToBrowser(context, sendIntent);
                } else if (osm_enabled && notShortnedURLDialog.get(notShortnedURLDialog.size() - 1).contains("/maps/place/")) {
                    String newUrlFinal = notShortnedURLDialog.get(notShortnedURLDialog.size() - 1);
                    Matcher matcher = maps.matcher(notShortnedURLDialog.get(notShortnedURLDialog.size() - 1));
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
                            String osmHost = sharedpreferences.getString(MainActivity.SET_OSM_HOST, MainActivity.DEFAULT_OSM_HOST);
                            assert osmHost != null;
                            osmHost = osmHost.toLowerCase();
                            newUrlFinal = scheme + osmHost + "/#map=" + zoom + "/" + data[0] + "/" + data[1];
                        }
                    }
                    String newExtraText = extraText.replaceAll(Pattern.quote(url), Matcher.quoteReplacement(newUrlFinal));
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, newExtraText);
                    sendIntent.setType("text/plain");
                    forwardToBrowser(context, sendIntent);
                } else {
                    String newExtraText = extraText.replaceAll(Pattern.quote(url), Matcher.quoteReplacement(notShortnedURLDialog.get(notShortnedURLDialog.size() - 1)));
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, newExtraText);
                    sendIntent.setType("text/plain");
                    forwardToBrowser(context, sendIntent);
                }
            }
        };
        thread.start();
    }

    /**
     * Forward the intent to a browser
     *
     * @param i original intent
     */
    public static void forwardToBrowser(Context context, Intent i) {
        Intent stopMainActivity = new Intent(KILL_ACTIVITY);
        i.putExtra("nitterizeme",true);
        context.sendBroadcast(stopMainActivity);
        context.startActivity(i);
        ((Activity) context).finish();
    }


    public static boolean isRouted(String url) {

        URL url_;
        String host = null;
        try {
            url_ = new URL(url);
            host = url_.getHost();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return Arrays.asList(twitter_domains).contains(host) || Arrays.asList(nitter_instances).contains(host)
                || Arrays.asList(instagram_domains).contains(host) || Arrays.asList(bibliogram_instances).contains(host)
                || url.contains("/maps/place") || url.contains("/amp/s/") || (host != null && host.contains(outlook_safe_domain))
                || Arrays.asList(youtube_domains).contains(host) || Arrays.asList(invidious_instances).contains(host);
    }

    public static boolean routerEnabledForHost(Context context, String url) {

        URL url_;
        String host = null;
        try {
            url_ = new URL(url);
            host = url_.getHost();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        SharedPreferences sharedpreferences = context.getSharedPreferences(MainActivity.APP_PREFS, Context.MODE_PRIVATE);
        if (Arrays.asList(twitter_domains).contains(host) || Arrays.asList(nitter_instances).contains(host)) {
            return sharedpreferences.getBoolean(SET_NITTER_ENABLED, true);
        } else if (Arrays.asList(instagram_domains).contains(host) || Arrays.asList(bibliogram_instances).contains(host)) {
            return sharedpreferences.getBoolean(SET_BIBLIOGRAM_ENABLED, true);
        } else if (url.contains("/maps/place")) {
            return sharedpreferences.getBoolean(MainActivity.SET_OSM_ENABLED, true);
        } else if (Arrays.asList(youtube_domains).contains(host) || Arrays.asList(invidious_instances).contains(host)) {
            return sharedpreferences.getBoolean(SET_INVIDIOUS_ENABLED, true);
        } else
            return url.contains("/amp/s/") || (host != null && host.contains(outlook_safe_domain));
    }


}
