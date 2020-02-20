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

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import static app.fedilab.nitterizeme.MainActivity.shortener_domains;

class Utils {

    static final Pattern urlPattern = Pattern.compile(
            "(?i)\\b((?:[a-z][\\w-]+:(?:/{1,3}|[a-z0-9%])|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,10}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);


    /**
     * Returns the unshortened URL
     *
     * @param urls ArrayList<String> URL to check
     */
    static void checkUrl(ArrayList<String> urls) {
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
                            urls.add(matcher.group(1));
                            newURL = matcher.group(1);
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
     * @param domain String domain name
     * @return long delay
     */
    static long ping(String domain){
        long timeDifference = -2;
        try {
            long beforeTime = System.currentTimeMillis();
            //noinspection ResultOfMethodCallIgnored
            InetAddress.getByName(domain).isReachable(10000);
            long afterTime = System.currentTimeMillis();
            timeDifference = afterTime - beforeTime;
        } catch (IOException ignored) {}
        return timeDifference;
    }
}
