/*
 * Copyright 2011 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tortel.authenticator.activity;

import android.app.Fragment;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MenuItem;

import com.tortel.authenticator.R;
import com.tortel.authenticator.timesync.AboutDialog;

/**
 * Top-level settings Activity.
 *
 */
public class SettingsActivity extends AppCompatActivity {
    private static final String KEY_ABOUT = "about_time";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_activity);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Fragment fragment = new MainSettingsFragment();
        getFragmentManager().beginTransaction().replace(R.id.content_frame, fragment).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Fragment that actually shows the settings
     */
    public static class MainSettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preferences);

            String packageVersion = "";
            try {
                packageVersion = getActivity().getPackageManager().getPackageInfo(
                        getActivity().getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException e) {
                // Ignore
            }

            findPreference("version").setTitle(
                    getActivity().getString(R.string.version_preference_title) + " " + packageVersion);
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            if(KEY_ABOUT.equals(preference.getKey())){
                AppCompatActivity activity = (AppCompatActivity) getActivity();
                AboutDialog dialog = new AboutDialog();
                dialog.show(activity.getSupportFragmentManager(), "about");
                return true;
            }
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
    }
}
