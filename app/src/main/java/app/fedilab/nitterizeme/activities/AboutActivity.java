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

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import app.fedilab.nitterizeme.BuildConfig;
import app.fedilab.nitterizeme.R;


public class AboutActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        TextView about_version = findViewById(R.id.about_version);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            about_version.setText(getResources().getString(R.string.about_vesrion, version));
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        setTitle(R.string.about_the_app);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        //Developer click for Mastodon account
        TextView developer_mastodon = findViewById(R.id.developer_mastodon);
        SpannableString content = new SpannableString(developer_mastodon.getText().toString());
        content.setSpan(new ForegroundColorSpan(ContextCompat.getColor(AboutActivity.this, R.color.colorAccent)), 0, content.length(), 0);
        developer_mastodon.setText(content);
        developer_mastodon.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://toot.fedilab.app/@fedilab"));
            startActivity(browserIntent);
        });

        //App Name:
        TextView app_name = findViewById(R.id.app_name);
        if (BuildConfig.fullLinks) {
            app_name.setText(R.string.app_name);
        } else {
            app_name.setText(R.string.app_name_lite);
        }

        //Developer Github
        TextView github = findViewById(R.id.github);
        content = new SpannableString(github.getText().toString());
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        github.setText(content);
        github.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/stom79"));
            startActivity(browserIntent);
        });

        //Developer Framagit
        TextView framagit = findViewById(R.id.framagit);
        content = new SpannableString(framagit.getText().toString());
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        framagit.setText(content);
        framagit.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://framagit.org/tom79"));
            startActivity(browserIntent);
        });

        //Developer Codeberg
        TextView codeberg = findViewById(R.id.codeberg);
        content = new SpannableString(codeberg.getText().toString());
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        codeberg.setText(content);
        codeberg.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://codeberg.org/tom79"));
            startActivity(browserIntent);
        });

        //Developer donation
        Button donatePaypal = findViewById(R.id.donate_paypal);
        donatePaypal.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.me/Mastalab"));
            startActivity(browserIntent);
        });
        Button donateLiberapay = findViewById(R.id.donate_liberapay);
        donateLiberapay.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://liberapay.com/tom79/donate"));
            startActivity(browserIntent);
        });

        Button how_to = findViewById(R.id.how_to);
        how_to.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://fedilab.app/wiki/untrackme/"));
            startActivity(browserIntent);
        });

        TextView license = findViewById(R.id.license);
        content = new SpannableString(license.getText().toString());
        content.setSpan(new ForegroundColorSpan(ContextCompat.getColor(AboutActivity.this, R.color.colorAccent)), 0, content.length(), 0);
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        license.setText(content);
        license.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.gnu.org/licenses/quick-guide-gplv3.fr.html"));
            startActivity(browserIntent);
        });
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
