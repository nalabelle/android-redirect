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
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import static app.fedilab.nitterizeme.MainActivity.APP_PREFS;
import static app.fedilab.nitterizeme.MainActivity.DEFAULT_BIBLIOGRAM_HOST;
import static app.fedilab.nitterizeme.MainActivity.DEFAULT_INVIDIOUS_HOST;
import static app.fedilab.nitterizeme.MainActivity.DEFAULT_NITTER_HOST;
import static app.fedilab.nitterizeme.MainActivity.SET_BIBLIOGRAM_HOST;
import static app.fedilab.nitterizeme.MainActivity.SET_INVIDIOUS_HOST;
import static app.fedilab.nitterizeme.MainActivity.SET_NITTER_HOST;


public class InstanceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popup_instance);
        new SearchInstances(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }


    static class SearchInstances extends AsyncTask<Void, Void, String> {

        private WeakReference<Activity> activityWeakReference;

        SearchInstances(Activity activity) {
            activityWeakReference = new WeakReference<>(activity);
        }

        @Override
        protected String doInBackground(Void... voids) {
            HttpsURLConnection httpsURLConnection;
            try {
                String instances_url = "https://fedilab.app/nitterizeme_instances/payload_2.json";
                URL url = new URL(instances_url);
                httpsURLConnection = (HttpsURLConnection) url.openConnection();
                httpsURLConnection.setConnectTimeout(10 * 1000);
                httpsURLConnection.setRequestProperty("http.keepAlive", "false");
                httpsURLConnection.setRequestProperty("Content-Type", "application/json");
                httpsURLConnection.setRequestProperty("Accept", "application/json");
                httpsURLConnection.setRequestMethod("GET");
                httpsURLConnection.setDefaultUseCaches(true);
                httpsURLConnection.setUseCaches(true);
                String response = null;
                if (httpsURLConnection.getResponseCode() >= 200 && httpsURLConnection.getResponseCode() < 400) {
                    java.util.Scanner s = new java.util.Scanner(httpsURLConnection.getInputStream()).useDelimiter("\\A");
                    response = s.hasNext() ? s.next() : "";
                }
                httpsURLConnection.getInputStream().close();
                return response;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            Activity activity = activityWeakReference.get();
            LinearLayout instance_container = activity.findViewById(R.id.instance_container);
            RelativeLayout loader = activity.findViewById(R.id.loader);
            RecyclerView invidious_instances = activity.findViewById(R.id.invidious_instances);
            RecyclerView nitter_instances = activity.findViewById(R.id.nitter_instances);
            RecyclerView bibliogram_instances = activity.findViewById(R.id.bibliogram_instances);
            Button latency_test = activity.findViewById(R.id.latency_test);
            Button close = activity.findViewById(R.id.close);
            if (result == null) {
                View parentLayout = activity.findViewById(android.R.id.content);
                Snackbar.make(parentLayout, R.string.error_message_internet, Snackbar.LENGTH_LONG).setAction(R.string.close, v -> activity.finish()).show();
                return;
            }
            try {
                JSONObject jsonObject = new JSONObject(result);
                JSONArray jsonArrayInvidious = jsonObject.getJSONArray("invidious");
                JSONArray jsonArrayNitter = jsonObject.getJSONArray("nitter");
                JSONArray jsonArrayBibliogram = jsonObject.getJSONArray("bibliogram");

                SharedPreferences sharedpreferences = activity.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE);
                String defaultInvidious = sharedpreferences.getString(SET_INVIDIOUS_HOST, DEFAULT_INVIDIOUS_HOST);
                String defaultNitter = sharedpreferences.getString(SET_NITTER_HOST, DEFAULT_NITTER_HOST);
                String defaultBibliogram = sharedpreferences.getString(SET_BIBLIOGRAM_HOST, DEFAULT_BIBLIOGRAM_HOST);

                List<Instance> invidiousInstances = new ArrayList<>();
                for (int i = 0; i < jsonArrayInvidious.length(); i++) {
                    Instance instance = new Instance();
                    String domain = jsonArrayInvidious.getJSONObject(i).getString("domain");
                    boolean cloudFlare = jsonArrayInvidious.getJSONObject(i).getBoolean("cloudflare");
                    String locale = jsonArrayInvidious.getJSONObject(i).getString("locale");
                    instance.setDomain(domain);
                    instance.setCloudflare(cloudFlare);
                    instance.setLocale(locale);
                    if (domain.compareTo(defaultInvidious) == 0) {
                        instance.setChecked(true);
                    }
                    instance.setType(Instance.instanceType.INVIDIOUS);
                    invidiousInstances.add(instance);
                }
                List<Instance> nitterInstances = new ArrayList<>();
                for (int i = 0; i < jsonArrayNitter.length(); i++) {
                    Instance instance = new Instance();
                    String domain = jsonArrayNitter.getJSONObject(i).getString("domain");
                    boolean cloudFlare = jsonArrayNitter.getJSONObject(i).getBoolean("cloudflare");
                    String locale = jsonArrayNitter.getJSONObject(i).getString("locale");
                    instance.setDomain(domain);
                    instance.setCloudflare(cloudFlare);
                    instance.setLocale(locale);
                    if (domain.compareTo(defaultNitter) == 0) {
                        instance.setChecked(true);
                    }
                    instance.setType(Instance.instanceType.NITTER);
                    nitterInstances.add(instance);
                }
                List<Instance> bibliogramInstances = new ArrayList<>();
                for (int i = 0; i < jsonArrayBibliogram.length(); i++) {
                    Instance instance = new Instance();
                    String domain = jsonArrayBibliogram.getJSONObject(i).getString("domain");
                    boolean cloudFlare = jsonArrayBibliogram.getJSONObject(i).getBoolean("cloudflare");
                    String locale = jsonArrayBibliogram.getJSONObject(i).getString("locale");
                    instance.setDomain(domain);
                    instance.setCloudflare(cloudFlare);
                    instance.setLocale(locale);
                    if (domain.compareTo(defaultBibliogram) == 0) {
                        instance.setChecked(true);
                    }
                    instance.setType(Instance.instanceType.BIBLIOGRAM);
                    bibliogramInstances.add(instance);
                }

                final LinearLayoutManager iLayoutManager = new LinearLayoutManager(activity);
                InstanceAdapter invidiousAdapter = new InstanceAdapter(invidiousInstances);
                invidious_instances.setAdapter(invidiousAdapter);
                invidious_instances.setLayoutManager(iLayoutManager);

                final LinearLayoutManager nLayoutManager = new LinearLayoutManager(activity);
                InstanceAdapter nitterAdapter = new InstanceAdapter(nitterInstances);
                nitter_instances.setAdapter(nitterAdapter);
                nitter_instances.setLayoutManager(nLayoutManager);

                final LinearLayoutManager bLayoutManager = new LinearLayoutManager(activity);
                InstanceAdapter bibliogramAdapter = new InstanceAdapter(bibliogramInstances);
                bibliogram_instances.setAdapter(bibliogramAdapter);
                bibliogram_instances.setLayoutManager(bLayoutManager);

                latency_test.setOnClickListener(
                        v -> {
                            invidiousAdapter.evalLatency();
                            nitterAdapter.evalLatency();
                            bibliogramAdapter.evalLatency();
                        }
                );

            } catch (JSONException e) {
                e.printStackTrace();
            }

            close.setOnClickListener(v -> activity.finish());


            instance_container.setVisibility(View.VISIBLE);
            loader.setVisibility(View.GONE);

        }

    }

}
