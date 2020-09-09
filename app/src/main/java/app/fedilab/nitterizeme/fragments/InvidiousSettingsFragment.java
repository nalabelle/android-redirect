package app.fedilab.nitterizeme.fragments;
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

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;

import app.fedilab.nitterizeme.R;

public class InvidiousSettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_invidious, rootKey);
        SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
        String volume_mode = prefs.getString(getString(R.string.invidious_volume_mode), "0");
        assert volume_mode != null;
        if (volume_mode.compareTo("0") == 0 || volume_mode.compareTo("-1") == 0) {
            PreferenceCategory player_parameters = findPreference(getString(R.string.invidious_category_player_parameters));
            SeekBarPreference volume_pref = findPreference(getString(R.string.invidious_volume_value));
            assert volume_pref != null;
            assert player_parameters != null;
            player_parameters.removePreference(volume_pref);
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if (key.compareTo(getString(R.string.invidious_volume_mode)) == 0) {
            String volume_mode = sharedPreferences.getString(getString(R.string.invidious_volume_mode), "0");
            SeekBarPreference volume_pref = findPreference(getString(R.string.invidious_volume_value));
            PreferenceCategory player_parameters = findPreference(getString(R.string.invidious_category_player_parameters));
            assert volume_mode != null;
            if (volume_mode.compareTo("0") == 0 || volume_mode.compareTo("-1") == 0) {
                assert player_parameters != null;
                if (volume_pref != null) {
                    player_parameters.removePreference(volume_pref);
                }
            } else {
                FragmentActivity activity = getActivity();
                assert activity != null;
                SeekBarPreference volume_pref_new = new SeekBarPreference(activity);
                volume_pref_new.setTitle(R.string.invidious_volume_mode_value);
                volume_pref_new.setSummary(R.string.invidious_volume_mode_value_indication);
                volume_pref_new.setKey(getString(R.string.invidious_volume_value));
                volume_pref_new.setMax(100);
                volume_pref_new.setDefaultValue(sharedPreferences.getInt(getString(R.string.invidious_volume_value), 60));
                volume_pref_new.setIconSpaceReserved(false);
                volume_pref_new.setShowSeekBarValue(true);
                assert player_parameters != null;
                if (volume_pref != null) {
                    player_parameters.removePreference(volume_pref);
                }
                player_parameters.addPreference(volume_pref_new);
            }
        }
    }
}
