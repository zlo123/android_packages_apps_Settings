/*
 * Copyright (C) 2012 The LiquidSmoothROMs Project
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
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.liquid.SettingsPreferenceFragment;
import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class StatusBarSignal extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    ListPreference mDbmStyletyle;
    ListPreference mWifiStyle;
    CheckBoxPreference mHideSignal;
    CheckBoxPreference mSixbarSignal;
    ColorPickerPreference mColorPicker;
    ColorPickerPreference mWifiColorPicker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.prefs_statusbar_signal);

        mDbmStyletyle = (ListPreference) findPreference("signal_style");
        mDbmStyletyle.setOnPreferenceChangeListener(this);
        mDbmStyletyle.setValue(Integer.toString(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.STATUSBAR_SIGNAL_TEXT, 0)));

        mColorPicker = (ColorPickerPreference) findPreference("signal_color");
        mColorPicker.setOnPreferenceChangeListener(this);

        mHideSignal = (CheckBoxPreference) findPreference("hide_signal");
        mHideSignal.setChecked(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.STATUSBAR_HIDE_SIGNAL_BARS, 0) != 0);

        mSixbarSignal = (CheckBoxPreference) findPreference("sixbar_signal");
        mSixbarSignal.setChecked(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.STATUSBAR_SIXBAR_SIGNAL, 0) == 1);

        mWifiStyle = (ListPreference) findPreference("wifi_signal_style");
        mWifiStyle.setOnPreferenceChangeListener(this);
        mWifiStyle.setValue(Integer.toString(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.STATUSBAR_WIFI_SIGNAL_TEXT, 0)));

        mWifiColorPicker = (ColorPickerPreference) findPreference("wifi_signal_color");
        mWifiColorPicker.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mHideSignal) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_HIDE_SIGNAL_BARS,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
            return true;
        }
        if (preference == mSixbarSignal) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_SIXBAR_SIGNAL,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mDbmStyletyle) {
            int val = Integer.parseInt((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_SIGNAL_TEXT, val);
            return true;
        } else if (preference == mColorPicker) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_SIGNAL_TEXT_COLOR, intHex);
            return true;
        } else if (preference == mWifiStyle) {
            int val = Integer.parseInt((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_WIFI_SIGNAL_TEXT, val);
            return true;
        } else if (preference == mWifiColorPicker) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_WIFI_SIGNAL_TEXT_COLOR, intHex);
            return true;
        }
        return false;
    }
}
