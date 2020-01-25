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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TransformActivity extends AppCompatActivity {



    final Pattern youtubePattern = Pattern.compile("(www\\.|m\\.)?(youtube\\.com|youtu\\.be|youtube-nocookie\\.com)/(((?!([\"'<])).)*)");
    final Pattern nitterPattern = Pattern.compile("(mobile\\.|www\\.)?twitter.com([\\w-/]+)");

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
                Matcher matcher = nitterPattern.matcher(url);
                while (matcher.find()) {
                    final String nitter_directory = matcher.group(2);
                    String nitterHost = sharedpreferences.getString(MainActivity.SET_NITTER_HOST, MainActivity.DEFAULT_NITTER_HOST).toLowerCase();
                    newUrl = "https://" + nitterHost + nitter_directory;
                }
            }else{ //Youtube URL
                Matcher matcher = youtubePattern.matcher(url);
                while (matcher.find()) {
                    final String youtubeId = matcher.group(3);
                    String invidiousHost = sharedpreferences.getString(MainActivity.SET_INVIDIOUS_HOST, MainActivity.DEFAULT_INVIDIOUS_HOST).toLowerCase();
                    if (matcher.group(2) != null && matcher.group(2).compareTo("youtu.be") == 0) {
                        newUrl = "https://" + invidiousHost + "/watch?v=" + youtubeId + "&local=true";
                    } else {
                        newUrl = "https://" + invidiousHost + "/" + youtubeId + "&local=true";
                    }
                }
            }
            Log.v(MainActivity.TAG,"newUrl: " + newUrl);
            Intent delegate = new Intent(Intent.ACTION_VIEW);
            delegate.setData(Uri.parse(newUrl));
            delegate.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (delegate.resolveActivity(getPackageManager()) != null) {
                startActivity(delegate);
            }
        }


    }


}
