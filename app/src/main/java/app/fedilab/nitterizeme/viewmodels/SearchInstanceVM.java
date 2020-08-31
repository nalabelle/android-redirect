package app.fedilab.nitterizeme.viewmodels;
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
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import app.fedilab.nitterizeme.entities.Instance;

import static app.fedilab.nitterizeme.activities.MainActivity.APP_PREFS;
import static app.fedilab.nitterizeme.activities.MainActivity.DEFAULT_BIBLIOGRAM_HOST;
import static app.fedilab.nitterizeme.activities.MainActivity.DEFAULT_INVIDIOUS_HOST;
import static app.fedilab.nitterizeme.activities.MainActivity.DEFAULT_NITTER_HOST;
import static app.fedilab.nitterizeme.activities.MainActivity.SET_BIBLIOGRAM_HOST;
import static app.fedilab.nitterizeme.activities.MainActivity.SET_INVIDIOUS_HOST;
import static app.fedilab.nitterizeme.activities.MainActivity.SET_NITTER_HOST;

public class SearchInstanceVM extends ViewModel {
    private MutableLiveData<List<Instance>> instancesMLD;
    private WeakReference<Activity> activityWeakReference;

    public LiveData<List<Instance>> getInstances(WeakReference<Activity> activityWeakReference) {
        if (instancesMLD == null) {
            instancesMLD = new MutableLiveData<>();
            this.activityWeakReference = activityWeakReference;
            loadInstances();
        }
        return instancesMLD;
    }

    private void loadInstances() {
        HttpsURLConnection httpsURLConnection;
        try {
            String instances_url = "https://fedilab.app/untrackme_instances/payload_2.json";
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
            SharedPreferences sharedpreferences = activityWeakReference.get().getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE);
            String defaultInvidious = sharedpreferences.getString(SET_INVIDIOUS_HOST, DEFAULT_INVIDIOUS_HOST);
            String defaultNitter = sharedpreferences.getString(SET_NITTER_HOST, DEFAULT_NITTER_HOST);
            String defaultBibliogram = sharedpreferences.getString(SET_BIBLIOGRAM_HOST, DEFAULT_BIBLIOGRAM_HOST);
            ArrayList<Instance> instances = new ArrayList<>();
            if( response != null) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    JSONArray jsonArrayInvidious = jsonObject.getJSONArray("invidious");
                    JSONArray jsonArrayNitter = jsonObject.getJSONArray("nitter");
                    JSONArray jsonArrayBibliogram = jsonObject.getJSONArray("bibliogram");

                    for (int i = 0; i < jsonArrayInvidious.length(); i++) {
                        Instance instance = new Instance();
                        String domain = jsonArrayInvidious.getJSONObject(i).getString("domain");
                        boolean cloudFlare = jsonArrayInvidious.getJSONObject(i).getBoolean("cloudflare");
                        String locale = jsonArrayInvidious.getJSONObject(i).getString("locale");
                        instance.setDomain(domain);
                        instance.setCloudflare(cloudFlare);
                        instance.setLocale(locale);
                        instance.setType(Instance.instanceType.INVIDIOUS);
                        if (defaultInvidious != null && domain.compareTo(defaultInvidious) == 0) {
                            instance.setChecked(true);
                        }
                        instances.add(instance);
                    }
                    for (int i = 0; i < jsonArrayNitter.length(); i++) {
                        Instance instance = new Instance();
                        String domain = jsonArrayNitter.getJSONObject(i).getString("domain");
                        boolean cloudFlare = jsonArrayNitter.getJSONObject(i).getBoolean("cloudflare");
                        String locale = jsonArrayNitter.getJSONObject(i).getString("locale");
                        instance.setDomain(domain);
                        instance.setCloudflare(cloudFlare);
                        instance.setLocale(locale);
                        instance.setType(Instance.instanceType.NITTER);
                        if (defaultNitter != null && domain.compareTo(defaultNitter) == 0) {
                            instance.setChecked(true);
                        }
                        instances.add(instance);
                    }
                    for (int i = 0; i < jsonArrayBibliogram.length(); i++) {
                        Instance instance = new Instance();
                        String domain = jsonArrayBibliogram.getJSONObject(i).getString("domain");
                        boolean cloudFlare = jsonArrayBibliogram.getJSONObject(i).getBoolean("cloudflare");
                        String locale = jsonArrayBibliogram.getJSONObject(i).getString("locale");
                        instance.setDomain(domain);
                        instance.setCloudflare(cloudFlare);
                        instance.setLocale(locale);
                        instance.setType(Instance.instanceType.BIBLIOGRAM);
                        if (defaultBibliogram != null && domain.compareTo(defaultBibliogram) == 0) {
                            instance.setChecked(true);
                        }
                        instances.add(instance);
                    }
                    instancesMLD.setValue(instances);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
