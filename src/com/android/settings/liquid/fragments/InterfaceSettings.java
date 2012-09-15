/*
 * Copyright (C) 2012 The CyanogenMod Project
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

import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.IWindowManager;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class InterfaceSettings extends SettingsPreferenceFragment {

    private static final String TAG = "InterfaceSettings";
    private static final String KEY_NOTIFICATION_DRAWER = "notification_drawer";
    private static final String KEY_NOTIFICATION_DRAWER_TABLET = "notification_drawer_tablet";
    private static final String KEY_HARDWARE_KEYS = "hardware_keys";
    private static final String KEY_POWER_MENU = "power_menu";
    private static final String KEY_STATUS_BAR = "status_bar";

    private PreferenceScreen mPhoneDrawer;
    private PreferenceScreen mTabletDrawer;
    private PreferenceScreen mHardwareKeys;
    private PreferenceScreen mPowerMenu;
    private PreferenceScreen mStatusBar;

    private final Configuration mCurConfig = new Configuration();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.interface_settings);

        mPhoneDrawer = (PreferenceScreen) findPreference(KEY_NOTIFICATION_DRAWER);
        mTabletDrawer = (PreferenceScreen) findPreference(KEY_NOTIFICATION_DRAWER_TABLET);
        mHardwareKeys = (PreferenceScreen) findPreference(KEY_HARDWARE_KEYS);
        mPowerMenu = (PreferenceScreen) findPreference(KEY_POWER_MENU);
        mStatusBar = (PreferenceScreen) findPreference(KEY_STATUS_BAR);

        if (Utils.isTablet(getActivity())) {
            if (mPhoneDrawer != null) {
                getPreferenceScreen().removePreference(mPhoneDrawer);
            }
        } else {
            if (mTabletDrawer != null) {
                getPreferenceScreen().removePreference(mTabletDrawer);
            }
        }

        IWindowManager windowManager = IWindowManager.Stub.asInterface(
                ServiceManager.getService(Context.WINDOW_SERVICE));
        try {
            Preference hardKeys = findPreference(KEY_HARDWARE_KEYS);
            if (hardKeys != null) {
                getPreferenceScreen().removePreference(hardKeys);
            }
        } catch (RemoteException e) {
        }
    }
}
