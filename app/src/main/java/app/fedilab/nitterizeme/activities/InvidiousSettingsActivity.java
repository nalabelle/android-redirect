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

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import app.fedilab.nitterizeme.R;
import app.fedilab.nitterizeme.fragments.InvidiousSettingsFragment;

public class InvidiousSettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invidious_settings);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, new InvidiousSettingsFragment())
                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_invidious_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_help) {
            AlertDialog.Builder invidious_settings_helper = new AlertDialog.Builder(InvidiousSettingsActivity.this);
            View view = getLayoutInflater().inflate(R.layout.popup_invidious_settings_info, new LinearLayout(getApplicationContext()), false);
            invidious_settings_helper.setView(view);
            invidious_settings_helper.setTitle(R.string.invidious_help_title);
            invidious_settings_helper.setPositiveButton(R.string.close, (dialog, id) -> dialog.dismiss());
            AlertDialog alertDialog = invidious_settings_helper.create();
            alertDialog.show();
        }
        return super.onOptionsItemSelected(item);
    }
}
