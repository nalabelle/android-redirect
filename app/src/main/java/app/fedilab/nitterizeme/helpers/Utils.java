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
import android.net.Uri;
import android.os.Environment;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import static android.content.Context.DOWNLOAD_SERVICE;
import static app.fedilab.nitterizeme.activities.CheckAppActivity.shortener_domains;

public class Utils {

    public static final String RECEIVE_STREAMING_URL = "receive_streaming_url";

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
    public static void checkUrl(ArrayList<String> urls) {
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
                            urls.add(newURL);
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
                        checkUrl(urls);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
    private static String remove_tracking_param(String url) {
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