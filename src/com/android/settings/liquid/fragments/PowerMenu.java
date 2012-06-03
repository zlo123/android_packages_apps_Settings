/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.liquid.fragments;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import com.android.settings.R;

public class PowerMenu extends PreferenceFragment {

    private static final String PREF_AIRPLANE = "show_airplane";
    private static final String PREF_EASTEREGG = "show_easteregg";
    private static final String PREF_FLASHLIGHT = "show_flashlight";
    private static final String PREF_HIDENAVBAR = "show_hidenavbar";
    private static final String PREF_POWERSAVER = "show_powersaver";
    private static final String PREF_PROFILES = "show_profiles";
    private static final String PREF_SCREENSHOT = "show_screenshot";

    CheckBoxPreference mShowAirplane;
    CheckBoxPreference mShowEasteregg;
    CheckBoxPreference mShowFlashlight;
    CheckBoxPreference mShowHidenavbar;
    CheckBoxPreference mShowPowersaver;
    CheckBoxPreference mShowProfiles;
    CheckBoxPreference mShowScreenshot;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.prefs_powermenu);

        mShowAirplane = (CheckBoxPreference) findPreference(PREF_AIRPLANE);
        mShowAirplane.setChecked(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.POWER_DIALOG_SHOW_AIRPLANE, 0) == 1);

        mShowEasteregg = (CheckBoxPreference) findPreference(PREF_EASTEREGG);
        mShowEasteregg.setChecked(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.POWER_DIALOG_SHOW_EASTEREGG, 0) == 1);

        mShowFlashlight = (CheckBoxPreference) findPreference(PREF_FLASHLIGHT);
        mShowFlashlight.setChecked(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.POWER_DIALOG_SHOW_FLASHLIGHT, 0) == 1);

        mShowHidenavbar = (CheckBoxPreference) findPreference(PREF_HIDENAVBAR);
        mShowHidenavbar.setChecked(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.POWER_DIALOG_SHOW_HIDENAVBAR, 1) == 1);

        mShowPowersaver = (CheckBoxPreference) findPreference(PREF_POWERSAVER);
        mShowPowersaver.setChecked(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.POWER_DIALOG_SHOW_POWERSAVER, 0) == 1);

        mShowProfiles = (CheckBoxPreference) findPreference(PREF_PROFILES);
        mShowProfiles.setChecked(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.POWER_DIALOG_SHOW_PROFILES, 1) == 1);

        mShowScreenshot = (CheckBoxPreference) findPreference(PREF_SCREENSHOT);
        mShowScreenshot.setChecked(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.POWER_DIALOG_SHOW_SCREENSHOT, 1) == 1);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, 
        Preference preference) {
        if (preference == mShowAirplane) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.POWER_DIALOG_SHOW_AIRPLANE,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
            return true;
        } else if (preference == mShowEasteregg) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.POWER_DIALOG_SHOW_EASTEREGG,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
            return true;
        } else if (preference == mShowFlashlight) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.POWER_DIALOG_SHOW_FLASHLIGHT,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
            return true;
        } else if (preference == mShowHidenavbar) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.POWER_DIALOG_SHOW_HIDENAVBAR,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
            return true;
        } else if (preference == mShowPowersaver) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.POWER_DIALOG_SHOW_POWERSAVER,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
            return true;
        } else if (preference == mShowProfiles) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.POWER_DIALOG_SHOW_PROFILES,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
            return true;
        } else if (preference == mShowScreenshot) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.POWER_DIALOG_SHOW_SCREENSHOT,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}

