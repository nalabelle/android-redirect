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
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;


import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;


import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    @SuppressWarnings("unused")
    public static String TAG = "NitterizeMe";
    public  static  String SET_NITTER_HOST = "set_nitter_host";
    public  static  String DEFAULT_NITTER_HOST = "nitter.net";
    public  static  String SET_INVIDIOUS_HOST = "set_invidious_host";
    public  static  String DEFAULT_INVIDIOUS_HOST = "invidio.us";
    public  static  String SET_INVIDIOUS_ENABLED = "set_invidious_enabled";
    public  static  String SET_NITTER_ENABLED = "set_nitter_enabled";
    public  static  String SET_OSM_ENABLED = "set_osm_enabled";
    public  static  String SET_OSM_HOST = "set_osm_host";
    public  static  String DEFAULT_OSM_HOST = "www.openstreetmap.org";
    public static final String APP_PREFS = "app_prefs";

    //Supported domains
    private String[] domains = {
            "twitter.com",
            "mobile.twitter.com",
            "www.twitter.com",
            "www.youtube.com",
            "youtube.com",
            "m.youtube.com",
            "youtu.be",
            "youtube-nocookie.com",
            "google.com/maps",
            "www.google.com/maps"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        SharedPreferences sharedpreferences = getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE);

        TextInputEditText nitter_instance = findViewById(R.id.nitter_instance);
        TextInputEditText invidious_instance = findViewById(R.id.invidious_instance);
        TextInputEditText osm_instance = findViewById(R.id.osm_instance);

        SwitchCompat enable_nitter = findViewById(R.id.enable_nitter);
        SwitchCompat enable_invidious = findViewById(R.id.enable_invidious);
        SwitchCompat enable_osm = findViewById(R.id.enable_osm);

        boolean nitter_enabled = sharedpreferences.getBoolean(SET_NITTER_ENABLED, true);
        boolean invidious_enabled = sharedpreferences.getBoolean(SET_INVIDIOUS_ENABLED, true);
        boolean osm_enabled = sharedpreferences.getBoolean(SET_OSM_ENABLED, true);

        enable_nitter.setChecked(nitter_enabled);
        enable_invidious.setChecked(invidious_enabled);
        enable_osm.setChecked(osm_enabled);

        Button button_save = findViewById(R.id.button_save);
        RecyclerView list_apps = findViewById(R.id.list_apps);
        String nitterHost = sharedpreferences.getString(SET_NITTER_HOST, null);
        String invidiousHost = sharedpreferences.getString(SET_INVIDIOUS_HOST, null);
        String osmHost = sharedpreferences.getString(SET_OSM_HOST, null);

        enable_invidious.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putBoolean(SET_INVIDIOUS_ENABLED, isChecked);
            editor.apply();
        });
        enable_nitter.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putBoolean(SET_NITTER_ENABLED, isChecked);
            editor.apply();
        });
        enable_osm.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putBoolean(SET_OSM_ENABLED, isChecked);
            editor.apply();
        });

        if(nitterHost!=null) {
            nitter_instance.setText(nitterHost);
        }
        if(invidiousHost!=null) {
            invidious_instance.setText(invidiousHost);
        }
        if(osmHost!=null) {
            osm_instance.setText(osmHost);
        }
        button_save.setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedpreferences.edit();
            if (nitter_instance.getText() != null && nitter_instance.getText().toString().trim().length() > 0) {
                editor.putString(SET_NITTER_HOST, nitter_instance.getText().toString().toLowerCase().trim());
            } else {
                editor.putString(SET_NITTER_HOST, null);
            }
            if (invidious_instance.getText() != null && invidious_instance.getText().toString().trim().length() > 0) {
                editor.putString(SET_INVIDIOUS_HOST, invidious_instance.getText().toString().toLowerCase().trim());
            } else {
                editor.putString(SET_INVIDIOUS_HOST, null);
            }
            if (osm_instance.getText() != null && osm_instance.getText().toString().trim().length() > 0) {
                editor.putString(SET_OSM_HOST, osm_instance.getText().toString().toLowerCase().trim());
            } else {
                editor.putString(SET_OSM_HOST, null);
            }
            editor.apply();
            View parentLayout = findViewById(android.R.id.content);
            Snackbar.make(parentLayout, R.string.instances_saved, Snackbar.LENGTH_LONG).show();
        });

        ArrayList<AppInfo> appInfos = new ArrayList<>();
        for(String domain: domains) {
            AppInfo appInfo = new AppInfo();
            appInfo.setDomain(domain);
            appInfo.setApplicationInfo(getDefaultApp("https://"+domain));
            appInfos.add(appInfo);
        }

        AppInfoAdapter appInfoAdapter = new AppInfoAdapter(appInfos);
        list_apps.setAdapter(appInfoAdapter);
        final LinearLayoutManager mLayoutManager;
        mLayoutManager = new LinearLayoutManager(MainActivity.this);
        list_apps.setLayoutManager(mLayoutManager);
        list_apps.setNestedScrollingEnabled(false);
    }


    /**
     * Allow to get info about application that opens the link by default
     * @param url String url for test
     * @return ApplicationInfo info about the application
     */
    ApplicationInfo getDefaultApp(String url) {
        final Intent browserIntent = new Intent(Intent.ACTION_VIEW);
        browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        browserIntent.setData(Uri.parse(url));
        final ResolveInfo defaultResolution = getPackageManager().resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY);
        if (defaultResolution != null) {
            final ActivityInfo activity = defaultResolution.activityInfo;
            if (!activity.name.equals("com.android.internal.app.ResolverActivity") && !activity.packageName.equals("com.huawei.android.internal.app")) {
                return activity.applicationInfo;
            }
        }
        return null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {
            Intent intent = new Intent(MainActivity.this, AboutActivity.class);
            startActivity(intent);
            return true;
        }else if(id == android.R.id.home){
            finish();
        }

        return super.onOptionsItemSelected(item);
    }
}
