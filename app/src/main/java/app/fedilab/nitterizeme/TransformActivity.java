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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;


import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static app.fedilab.nitterizeme.MainActivity.SET_INVIDIOUS_ENABLED;
import static app.fedilab.nitterizeme.MainActivity.SET_NITTER_ENABLED;


public class TransformActivity extends AppCompatActivity {



    final Pattern youtubePattern = Pattern.compile("(www\\.|m\\.)?(youtube\\.com|youtu\\.be|youtube-nocookie\\.com)/(((?!([\"'<])).)*)");
    final Pattern nitterPattern = Pattern.compile("(mobile\\.|www\\.)?twitter.com([\\w-/]+)");
    final Pattern googleMap = Pattern.compile("google\\.com/maps[^@]+@([\\d.,z]{3,}).*");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedpreferences = getSharedPreferences(MainActivity.APP_PREFS, Context.MODE_PRIVATE);
        Intent intent = getIntent();
        if( intent != null && intent.getData() != null){
            String url = intent.getData().toString();
            String newUrl = null;
            //Twitter URLs
            if( url.contains("twitter")) {
                boolean nitter_enabled = sharedpreferences.getBoolean(SET_NITTER_ENABLED, true);
                if(nitter_enabled) {
                    Matcher matcher = nitterPattern.matcher(url);
                    while (matcher.find()) {
                        final String nitter_directory = matcher.group(2);
                        String nitterHost = sharedpreferences.getString(MainActivity.SET_NITTER_HOST, MainActivity.DEFAULT_NITTER_HOST).toLowerCase();
                        newUrl = "https://" + nitterHost + nitter_directory;
                    }
                    Intent delegate = new Intent(Intent.ACTION_VIEW);
                    delegate.setData(Uri.parse(newUrl));
                    delegate.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    if (delegate.resolveActivity(getPackageManager()) != null) {
                        startActivity(delegate);
                    }
                } else {
                    forwardToBrowser(intent);
                }
            }else if( url.contains("google")) {
                boolean osm_enabled = sharedpreferences.getBoolean(MainActivity.SET_OSM_ENABLED, true);
                if(osm_enabled) {
                    Matcher matcher = googleMap.matcher(url);
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
                    Intent delegate = new Intent(Intent.ACTION_VIEW);
                    delegate.setData(Uri.parse(newUrl));
                    delegate.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    if (delegate.resolveActivity(getPackageManager()) != null) {
                        startActivity(delegate);
                    }
                } else {
                    forwardToBrowser(intent);
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
                    Intent delegate = new Intent(Intent.ACTION_VIEW);
                    delegate.setData(Uri.parse(newUrl));
                    delegate.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    if (delegate.resolveActivity(getPackageManager()) != null) {
                        startActivity(delegate);
                    }
                }else{
                    forwardToBrowser(intent);
                }
            }

        }
    }



    private void forwardToBrowser(Intent i) {
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(i.getData(), i.getType());
        List<ResolveInfo> activities = getPackageManager().queryIntentActivities(intent, 0);
        ArrayList<Intent> targetIntents = new ArrayList<>();
        String thisPackageName = getApplicationContext().getPackageName();
        for (ResolveInfo currentInfo : activities) {
            String packageName = currentInfo.activityInfo.packageName;
            if (!thisPackageName.equals(packageName)) {
                Intent targetIntent = new Intent(android.content.Intent.ACTION_VIEW);
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
}
