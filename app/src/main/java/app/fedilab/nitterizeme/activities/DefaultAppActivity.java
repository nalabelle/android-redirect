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


import android.content.pm.PackageInfo;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import app.fedilab.nitterizeme.R;
import app.fedilab.nitterizeme.adapters.DefaultAppAdapter;
import app.fedilab.nitterizeme.entities.DefaultApp;
import app.fedilab.nitterizeme.helpers.Utils;
import app.fedilab.nitterizeme.sqlite.DefaultAppDAO;
import app.fedilab.nitterizeme.sqlite.Sqlite;


public class DefaultAppActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default_app);


        setTitle(R.string.default_apps);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        RecyclerView list_apps = findViewById(R.id.list_apps);
        final LinearLayoutManager mLayoutManager;
        mLayoutManager = new LinearLayoutManager(DefaultAppActivity.this);
        list_apps.setLayoutManager(mLayoutManager);
        list_apps.setNestedScrollingEnabled(false);

        ArrayList<DefaultApp> appInfos = getAppInfo();
        DefaultAppAdapter defaultAppAdapter = new DefaultAppAdapter(appInfos);
        list_apps.setAdapter(defaultAppAdapter);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * Get default app set inside the application for opening links
     *
     * @return ArrayList<DefaultApp>
     */
    private ArrayList<DefaultApp> getAppInfo() {
        ArrayList<DefaultApp> appInfos = new ArrayList<>();
        SQLiteDatabase db = Sqlite.getInstance(getApplicationContext(), Sqlite.DB_NAME, null, Sqlite.DB_VERSION).open();
        ArrayList<String> packageName = new DefaultAppDAO(DefaultAppActivity.this, db).getDefault();
        if (packageName != null) {
            for (String p : packageName) {
                DefaultApp defaultApp = new DefaultApp();
                PackageInfo packageInfo = Utils.getPackageInfo(this, p);
                if (packageInfo != null) {
                    defaultApp.setApplicationInfo(packageInfo.applicationInfo);
                    appInfos.add(defaultApp);
                }
            }
        }
        return appInfos;
    }

}
