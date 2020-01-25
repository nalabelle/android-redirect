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
import android.os.Bundle;


import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    @SuppressWarnings("unused")
    public static String TAG = "NitterizeMe";
    public  static  String SET_NITTER_HOST = "set_nitter_host";
    public  static  String DEFAULT_NITTER_HOST = "nitter.net";
    public  static  String SET_INVIDIOUS_HOST = "set_invidious_host";
    public  static  String DEFAULT_INVIDIOUS_HOST = "invidio.us";
    public static final String APP_PREFS = "app_prefs";

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
        Button button_save = findViewById(R.id.button_save);

        String nitterHost = sharedpreferences.getString(SET_NITTER_HOST, null);
        String invidiousHost = sharedpreferences.getString(SET_INVIDIOUS_HOST, null);
        if(nitterHost!=null) {
            nitter_instance.setText(nitterHost);
        }
        if(invidiousHost!=null) {
            invidious_instance.setText(invidiousHost);
        }
        button_save.setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedpreferences.edit();
            if (nitter_instance.getText() != null && nitter_instance.getText().toString().trim().length() > 0) {
                editor.putString(SET_NITTER_HOST, nitter_instance.getText().toString().toLowerCase().trim());
            } else {
                editor.putString(SET_NITTER_HOST, null);
            }
            editor.apply();
            if (invidious_instance.getText() != null && invidious_instance.getText().toString().trim().length() > 0) {
                editor.putString(SET_INVIDIOUS_HOST, invidious_instance.getText().toString().toLowerCase().trim());
            } else {
                editor.putString(SET_INVIDIOUS_HOST, null);
            }
            editor.apply();
            View parentLayout = findViewById(android.R.id.content);
            Snackbar.make(parentLayout, R.string.instances_saved, Snackbar.LENGTH_LONG).show();
        });
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
