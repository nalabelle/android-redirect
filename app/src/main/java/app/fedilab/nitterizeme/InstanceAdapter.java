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
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import static app.fedilab.nitterizeme.MainActivity.APP_PREFS;
import static app.fedilab.nitterizeme.MainActivity.DEFAULT_BIBLIOGRAM_HOST;
import static app.fedilab.nitterizeme.MainActivity.DEFAULT_INVIDIOUS_HOST;
import static app.fedilab.nitterizeme.MainActivity.DEFAULT_NITTER_HOST;
import static app.fedilab.nitterizeme.MainActivity.SET_BIBLIOGRAM_HOST;
import static app.fedilab.nitterizeme.MainActivity.SET_INVIDIOUS_HOST;
import static app.fedilab.nitterizeme.MainActivity.SET_NITTER_HOST;

public class InstanceAdapter extends RecyclerView.Adapter {

    private List<Instance> instances;

    InstanceAdapter(List<Instance> instances) {
        this.instances = instances;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int position) {
        Context context = parent.getContext();
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        return new ViewHolder(layoutInflater.inflate(R.layout.drawer_instance, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {

        ViewHolder holder = (ViewHolder) viewHolder;
        Instance instance = instances.get(i);
        Context context = viewHolder.itemView.getContext();
        SharedPreferences sharedpreferences = context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE);
        //Reset checked instances by type when tipping

        String defaultInvidious = sharedpreferences.getString(SET_INVIDIOUS_HOST, DEFAULT_INVIDIOUS_HOST);
        String defaultNitter = sharedpreferences.getString(SET_NITTER_HOST, DEFAULT_NITTER_HOST);
        String defaultBibliogram = sharedpreferences.getString(SET_BIBLIOGRAM_HOST, DEFAULT_BIBLIOGRAM_HOST);


        if (instance.getLatency() == -1){
            holder.latency.setVisibility(View.GONE);
            holder.progress.setVisibility(View.GONE);
        }else if(instance.getLatency() == 0 ){
            holder.latency.setVisibility(View.GONE);
            holder.progress.setVisibility(View.VISIBLE);
        }else{
            holder.latency.setVisibility(View.VISIBLE);
            holder.progress.setVisibility(View.GONE);
        }

        switch (instance.getType()){
            case INVIDIOUS:
                if( instance.getDomain().compareTo(defaultInvidious) == 0 ){
                    holder.checkbox_instance.setChecked(true);
                }
                break;
            case NITTER:
                if( instance.getDomain().compareTo(defaultNitter) == 0 ){
                    holder.checkbox_instance.setChecked(true);
                }
                break;
            case BIBLIOGRAM:
                if( instance.getDomain().compareTo(defaultBibliogram) == 0 ){
                    holder.checkbox_instance.setChecked(true);
                }
                break;
        }

        holder.checkbox_instance.setOnCheckedChangeListener((buttonView, isChecked) -> {
            for(Instance _ins: instances){
                if(instance.getType() == _ins.getType() && instance.getDomain().compareTo(_ins.getDomain()) != 0 ){
                    _ins.setChecked(false);
                }
            }
            SharedPreferences.Editor editor = sharedpreferences.edit();
            switch (instance.getType()){
                case INVIDIOUS:
                    if( isChecked) {
                        editor.putString(SET_INVIDIOUS_HOST, instance.getDomain().trim());
                    }else{
                        editor.putString(SET_INVIDIOUS_HOST, null);
                    }
                    editor.apply();
                    break;
                case NITTER:
                    if( isChecked) {
                        editor.putString(SET_NITTER_HOST, instance.getDomain().trim());
                    }else{
                        editor.putString(SET_NITTER_HOST, null);
                    }
                    editor.apply();
                    break;
                case BIBLIOGRAM:
                    if( isChecked) {
                        editor.putString(SET_BIBLIOGRAM_HOST, instance.getDomain().trim());
                    }else{
                        editor.putString(SET_BIBLIOGRAM_HOST, null);
                    }
                    editor.apply();
                    break;
            }
        });

    }

    void evalLatency(){
        for(Instance instance: instances){
            instance.setLatency(0);
            Thread thread = new Thread() {
                @Override
                public void run() {
                    long ping = Utils.ping(instance.getDomain());
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    Runnable myRunnable = () -> {
                        if( ping > 0 ) {
                            instance.setLatency(ping);
                        }else{
                            instance.setLatency(R.string.error);
                        }
                        notifyDataSetChanged();
                    };
                    mainHandler.post(myRunnable);
                }
            };
            thread.start();
        }
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return instances.size();
    }


    class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkbox_instance;
        TextView latency;
        ProgressBar progress;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            checkbox_instance = itemView.findViewById(R.id.checkbox_instance);
            latency = itemView.findViewById(R.id.latency);
            progress = itemView.findViewById(R.id.progress);
        }
    }


}