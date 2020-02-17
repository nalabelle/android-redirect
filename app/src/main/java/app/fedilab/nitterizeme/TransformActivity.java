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
import android.os.Parcelable;
import android.util.Patterns;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static app.fedilab.nitterizeme.MainActivity.SET_INVIDIOUS_ENABLED;
import static app.fedilab.nitterizeme.MainActivity.SET_NITTER_ENABLED;


public class TransformActivity extends Activity {



    final Pattern youtubePattern = Pattern.compile("(www\\.|m\\.)?(youtube\\.com|youtu\\.be|youtube-nocookie\\.com)/(((?!([\"'<])).)*)");
    final Pattern nitterPattern = Pattern.compile("(mobile\\.|www\\.)?twitter.com([\\w-/]+)");
    final Pattern maps = Pattern.compile("/maps/place/[^@]+@([\\d.,z]{3,}).*");
    final Pattern extractPlace  = Pattern.compile("/maps/place/(((?!/data).)*)");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedpreferences = getSharedPreferences(MainActivity.APP_PREFS, Context.MODE_PRIVATE);
        Intent intent = getIntent();
        if( intent != null && intent.getStringExtra("nitterizeme") != null ) {
            finish();
            return;
        }
        assert intent != null;
        if( Objects.requireNonNull(intent.getAction()).equals(Intent.ACTION_VIEW)){
            String action = intent.getAction();
            String  url = Objects.requireNonNull(intent.getData()).toString();
            //Twitter URLs
            if( url.contains("t.co")) {
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        String notShortnedURL = Utils.checkUrl(url);
                        if( notShortnedURL == null) {
                            notShortnedURL = url;
                        }
                        boolean nitter_enabled = sharedpreferences.getBoolean(SET_NITTER_ENABLED, true);
                        if(nitter_enabled) {
                            String newUrlFinal = notShortnedURL;
                            Matcher matcher = nitterPattern.matcher(notShortnedURL);
                            while (matcher.find()) {
                                final String nitter_directory = matcher.group(2);
                                String nitterHost = sharedpreferences.getString(MainActivity.SET_NITTER_HOST, MainActivity.DEFAULT_NITTER_HOST).toLowerCase();
                                newUrlFinal = "https://" + nitterHost + nitter_directory;
                            }
                            Intent delegate = new Intent(action);
                            delegate.setData(Uri.parse(newUrlFinal));
                            delegate.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            if (delegate.resolveActivity(getPackageManager()) != null) {
                                startActivity(delegate);
                            }
                        } else {
                            forwardToBrowser(intent, action);
                        }
                    }
                };
                thread.start();
            } else if( url.contains("twitter")) {
                boolean nitter_enabled = sharedpreferences.getBoolean(SET_NITTER_ENABLED, true);
                if(nitter_enabled) {
                    Intent delegate = new Intent(action);
                    String transformedURL = transformUrl(url);
                    if( transformedURL != null) {
                        delegate.setData(Uri.parse(transformUrl(url)));
                        delegate.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        if (delegate.resolveActivity(getPackageManager()) != null) {
                            startActivity(delegate);
                        }
                    }else{
                        forwardToBrowser(intent, action);
                    }
                } else {
                    forwardToBrowser(intent, action);
                }
            }else if( url.contains("/maps/place")) {
                boolean osm_enabled = sharedpreferences.getBoolean(MainActivity.SET_OSM_ENABLED, true);
                if(osm_enabled) {
                    Intent delegate = new Intent(action);
                    String transformedURL = transformUrl(url);
                    if( transformedURL != null) {
                        delegate.setData(Uri.parse(transformUrl(url)));
                        delegate.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        if (delegate.resolveActivity(getPackageManager()) != null) {
                            startActivity(delegate);
                        }
                    }else {
                        forwardToBrowser(intent, action);
                    }
                } else {
                    forwardToBrowser(intent, action);
                }
            }else{ //Youtube URL
                boolean invidious_enabled = sharedpreferences.getBoolean(SET_INVIDIOUS_ENABLED, true);
                if( invidious_enabled) {
                    Intent delegate = new Intent(action);
                    String transformedURL = transformUrl(url);
                    if( transformedURL != null) {
                        delegate.setData(Uri.parse(transformUrl(url)));
                        delegate.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        if (delegate.resolveActivity(getPackageManager()) != null) {
                            startActivity(delegate);
                        }
                    }else {
                        forwardToBrowser(intent, action);
                    }
                }else{
                    forwardToBrowser(intent, action);
                }
            }

        }
        else  if( Objects.requireNonNull(intent.getAction()).equals(Intent.ACTION_SEND)){
            share(intent.getStringExtra(Intent.EXTRA_TEXT));
        }
    }




    private void forwardToBrowser(Intent i, String action) {
        Intent intent = new Intent();
        intent.setAction(action);
        intent.setDataAndType(i.getData(), i.getType());
        List<ResolveInfo> activities = getPackageManager().queryIntentActivities(intent, 0);
        ArrayList<Intent> targetIntents = new ArrayList<>();
        String thisPackageName = getApplicationContext().getPackageName();
        for (ResolveInfo currentInfo : activities) {
            String packageName = currentInfo.activityInfo.packageName;
            if (!thisPackageName.equals(packageName)) {
                Intent targetIntent = new Intent(action);
                targetIntent.setDataAndType(intent.getData(), intent.getType());
                targetIntent.setPackage(intent.getPackage());
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


    private String transformUrl(String url) {
        SharedPreferences sharedpreferences = getSharedPreferences(MainActivity.APP_PREFS, Context.MODE_PRIVATE);
        String newUrl = null;
        if( url.contains("twitter")) {
            boolean nitter_enabled = sharedpreferences.getBoolean(SET_NITTER_ENABLED, true);
            if(nitter_enabled) {
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
        }else if( url.contains("/maps/place")) {
            boolean osm_enabled = sharedpreferences.getBoolean(MainActivity.SET_OSM_ENABLED, true);
            if(osm_enabled) {
                Matcher matcher = maps.matcher(url);
                while (matcher.find()) {
                    final String localization = matcher.group(1);
                    assert localization != null;
                    String[] data = localization.split(",");
                    if( data.length > 2 ){
                        String zoom;
                        String[] details = data[2].split("\\.");
                        if( details.length > 0 ) {
                            zoom = details[0];
                        }else {
                            zoom = data[2];
                        }

                        String osmHost = sharedpreferences.getString(MainActivity.SET_OSM_HOST, MainActivity.DEFAULT_OSM_HOST).toLowerCase();
                        boolean geo_uri_enabled = sharedpreferences.getBoolean(MainActivity.SET_GEO_URIS, false);
                        if(! geo_uri_enabled) {
                            newUrl = "https://" + osmHost + "/#map=" + zoom + "/" + data[0] + "/" + data[1];
                        }else{
                            newUrl =  "geo:0,0?q="+data[0]+","+data[1]+",z="+zoom;
                        }
                    }
                }
                if( newUrl == null && url.contains("/data=")) {
                    matcher = extractPlace.matcher(url);
                    while (matcher.find()) {
                        final String search = matcher.group(1);
                        newUrl =  "geo:0,0?q="+search;
                    }
                }
                return newUrl;
            } else {
                return url;
            }
        }else{ //Youtube URL
            boolean invidious_enabled = sharedpreferences.getBoolean(SET_INVIDIOUS_ENABLED, true);
            if( invidious_enabled) {
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
            }else{
                return url;
            }
        }
    }


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

        String newUrl = null;
        if( url == null){
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, extraText);
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
            return;
        }

        if( url.contains("twitter")) {
            boolean nitter_enabled = sharedpreferences.getBoolean(SET_NITTER_ENABLED, true);
            if(nitter_enabled) {
                Matcher matcher = nitterPattern.matcher(url);
                while (matcher.find()) {
                    final String nitter_directory = matcher.group(2);
                    String nitterHost = sharedpreferences.getString(MainActivity.SET_NITTER_HOST, MainActivity.DEFAULT_NITTER_HOST).toLowerCase();
                    newUrl = "https://" + nitterHost + nitter_directory;
                }
            }
        }else if( url.contains("/maps/place/")) {
            boolean osm_enabled = sharedpreferences.getBoolean(MainActivity.SET_OSM_ENABLED, true);
            if(osm_enabled) {
                Matcher matcher = maps.matcher(url);
                while (matcher.find()) {
                    final String localization = matcher.group(1);
                    assert localization != null;
                    String[] data = localization.split(",");
                    if( data.length > 2 ){
                        String zoom;
                        String[] details = data[2].split("\\.");
                        if( details.length > 0 ) {
                            zoom = details[0];
                        }else {
                            zoom = data[2];
                        }
                        String osmHost = sharedpreferences.getString(MainActivity.SET_OSM_HOST, MainActivity.DEFAULT_OSM_HOST).toLowerCase();
                        newUrl = "https://"+osmHost+"/#map="+zoom+"/"+data[0]+"/"+data[1];
                    }
                }
            }
        }else{ //Youtube URL
            boolean invidious_enabled = sharedpreferences.getBoolean(SET_INVIDIOUS_ENABLED, true);
            if( invidious_enabled) {
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
        } if( url.contains("t.co")) {
            String finalUrl = url;
            String finalExtraText = extraText;
            Thread thread = new Thread() {
                @Override
                public void run() {
                    String notShortnedURL = Utils.checkUrl(finalUrl);
                    if( notShortnedURL == null) {
                        notShortnedURL = finalUrl;
                    }
                    boolean nitter_enabled = sharedpreferences.getBoolean(SET_NITTER_ENABLED, true);
                    if(nitter_enabled) {
                        String newUrlFinal = notShortnedURL;
                        Matcher matcher = nitterPattern.matcher(notShortnedURL);
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
                    }
                }
            };
            thread.start();
            return;
        }
        if( newUrl != null) {
            extraText = extraText.replaceAll(Pattern.quote(url), Matcher.quoteReplacement(newUrl));
        }
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, extraText);
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }
}
