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


import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import app.fedilab.nitterizeme.activities.MainActivity;

import static android.content.Context.DOWNLOAD_SERVICE;
import static app.fedilab.nitterizeme.activities.CheckAppActivity.instagram_domains;
import static app.fedilab.nitterizeme.activities.CheckAppActivity.shortener_domains;
import static app.fedilab.nitterizeme.activities.CheckAppActivity.twitter_domains;
import static app.fedilab.nitterizeme.activities.CheckAppActivity.youtube_domains;
import static app.fedilab.nitterizeme.activities.MainActivity.SET_BIBLIOGRAM_ENABLED;
import static app.fedilab.nitterizeme.activities.MainActivity.SET_INVIDIOUS_ENABLED;
import static app.fedilab.nitterizeme.activities.MainActivity.SET_NITTER_ENABLED;

public class Utils {

    public static final String KILL_ACTIVITY = "kill_activity";
    public static final String URL_APP_PICKER = "url_app_picker";
    public static final Pattern youtubePattern = Pattern.compile("(www\\.|m\\.)?(youtube\\.com|youtu\\.be|youtube-nocookie\\.com)/(((?!([\"'<])).)*)");
    public static final Pattern nitterPattern = Pattern.compile("(mobile\\.|www\\.)?twitter.com([\\w-/]+)");
    public static final Pattern bibliogramPostPattern = Pattern.compile("(m\\.|www\\.)?instagram.com(/p/[\\w-/]+)");
    public static final Pattern bibliogramAccountPattern = Pattern.compile("(m\\.|www\\.)?instagram.com(((?!/p/).)+)");
    public static final Pattern maps = Pattern.compile("/maps/place/[^@]+@([\\d.,z]{3,}).*");
    public static final Pattern ampExtract = Pattern.compile("amp/s/(.*)");
    public static final String RECEIVE_STREAMING_URL = "receive_streaming_url";
    private static final Pattern extractPlace = Pattern.compile("/maps/place/(((?!/data).)*)");
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
            "[\\w]+"

    };
    private static String urlRegex = "(?i)\\b((?:[a-z][\\w-]+:(?:/{1,3}|[a-z0-9%])|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,10}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))";
    public static final Pattern urlPattern = Pattern.compile(
            urlRegex,
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    /**
     * Returns the unshortened URL
     *
     * @param urls ArrayList<String> URL to check
     */
    public static void checkUrl(Context context, ArrayList<String> urls) {
        URL url;
        String newURL = null;
        String comingURl;
        try {
            comingURl = urls.get(urls.size() - 1);

            if (comingURl.startsWith("http://")) {
                comingURl = comingURl.replace("http://", "https://");
            }
            url = new URL(comingURl);
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) url.openConnection();
            httpsURLConnection.setRequestProperty("http.keepAlive", "false");
            httpsURLConnection.setInstanceFollowRedirects(false);
            httpsURLConnection.setRequestMethod("HEAD");
            if (httpsURLConnection.getResponseCode() == 301) {
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
        try {
            url_ = new URL(url);
            host = url_.getHost();
        } catch (MalformedURLException e) {
            e.printStackTrace();
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
                return newUrl;
            } else {
                return url;
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
        return url;
    }

    /**
     * Get time for reaching a domain
     *
     * @param domain String domain name
     * @return long delay
     */
    public static long ping(String domain) {
        long timeDifference = -2;
        try {
            long beforeTime = System.currentTimeMillis();
            //noinspection ResultOfMethodCallIgnored
            InetAddress.getByName(domain).isReachable(10000);
            long afterTime = System.currentTimeMillis();
            timeDifference = afterTime - beforeTime;
        } catch (IOException ignored) {
        }
        return timeDifference;
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
        }
        if (url != null && url.endsWith("?")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }


    /**
     * Manage downloads with URLs
     *
     * @param context Context
     * @param url     String download url
     */
    public static void manageDownloadsNoPopup(final Context context, final String url) {

        final DownloadManager.Request request;
        try {
            request = new DownloadManager.Request(Uri.parse(url.trim()));
        } catch (Exception e) {
            return;
        }
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.ENGLISH);
            Date now = new Date();
            final String fileName = "UntrackMe_" + formatter.format(now) + ".mp4";
            request.allowScanningByMediaScanner();
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            DownloadManager dm = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
            assert dm != null;
            dm.enqueue(request);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }
}
