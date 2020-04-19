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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.GridView;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import app.fedilab.nitterizeme.R;
import app.fedilab.nitterizeme.adapters.AppPickerAdapter;
import app.fedilab.nitterizeme.entities.AppPicker;

import static app.fedilab.nitterizeme.activities.CheckAppActivity.invidious_instances;
import static app.fedilab.nitterizeme.helpers.Utils.KILL_ACTIVITY;
import static app.fedilab.nitterizeme.helpers.Utils.URL_APP_PICKER;
import static app.fedilab.nitterizeme.helpers.Utils.getPackageInfo;
import static app.fedilab.nitterizeme.helpers.Utils.isAppInstalled;


public class AppsPickerActivity extends Activity {


    private String url;
    private String appToUse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pickup_app);
        SharedPreferences sharedpreferences = getSharedPreferences(MainActivity.APP_PREFS, Context.MODE_PRIVATE);
        if (getIntent() == null) {
            finish();
        }
        Bundle b = getIntent().getExtras();
        if (b == null) {
            finish();
        }
        if (b != null) {
            url = b.getString(URL_APP_PICKER, null);
        }
        if (url == null) {
            finish();
        }
        //At this point we are sure that url is not null
        Intent stopMainActivity = new Intent(KILL_ACTIVITY);
        sendBroadcast(stopMainActivity);

        Intent delegate = new Intent(Intent.ACTION_VIEW);
        delegate.setDataAndType(Uri.parse(url), "text/html");
        delegate.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);


        List<ResolveInfo> activities;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            activities = getPackageManager().queryIntentActivities(delegate, PackageManager.MATCH_ALL);
        } else {
            activities = getPackageManager().queryIntentActivities(delegate, 0);
        }


        RelativeLayout blank = findViewById(R.id.blank);
        blank.setOnClickListener(v -> finish());

        String thisPackageName = getApplicationContext().getPackageName();
        ArrayList<String> packages = new ArrayList<>();
        List<AppPicker> appPickers = new ArrayList<>();
        int i = 0;
        for (ResolveInfo currentInfo : activities) {
            String packageName = currentInfo.activityInfo.packageName;
            if (!thisPackageName.equals(packageName) && !packages.contains(packageName)) {
                AppPicker appPicker = new AppPicker();
                appPicker.setIcon(currentInfo.activityInfo.loadIcon(getPackageManager()));
                appPicker.setName(String.valueOf(currentInfo.loadLabel(getPackageManager())));
                appPicker.setPackageName(packageName);
                if (i == 0) {
                    appPicker.setSelected(true);
                    appToUse = packageName;
                }
                appPickers.add(appPicker);
                packages.add(packageName);
            }
            i++;
        }
        if (isAppInstalled(AppsPickerActivity.this, "org.schabi.newpipe") && Arrays.asList(invidious_instances).contains(Objects.requireNonNull(Uri.parse(url)).getHost()) && !packages.contains("org.schabi.newpipe")) {
            PackageInfo packageInfo = getPackageInfo(AppsPickerActivity.this, "org.schabi.newpipe");
            if (packageInfo != null) {
                AppPicker appPicker = new AppPicker();
                appPicker.setIcon(packageInfo.applicationInfo.loadIcon(getPackageManager()));
                appPicker.setName(String.valueOf(packageInfo.applicationInfo.loadLabel(getPackageManager())));
                appPicker.setPackageName(packageInfo.applicationInfo.packageName);
                appPickers.add(appPicker);
            }
        }
        GridView gridView = findViewById(R.id.app_list);
        AppPickerAdapter appPickerAdapter = new AppPickerAdapter(appPickers);
        gridView.setAdapter(appPickerAdapter);
        gridView.setNumColumns(3);
        gridView.setOnItemClickListener((parent, view1, position, id) -> {
            for (AppPicker ap : appPickers) {
                ap.setSelected(false);
            }
            appPickers.get(position).setSelected(true);
            appToUse = appPickers.get(position).getPackageName();
            appPickerAdapter.notifyDataSetChanged();
        });


        Button always = findViewById(R.id.always);
        Button once = findViewById(R.id.once);

        always.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.setPackage(appToUse);
            startActivity(intent);
            finish();
        });

        once.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.setPackage(appToUse);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }

}
