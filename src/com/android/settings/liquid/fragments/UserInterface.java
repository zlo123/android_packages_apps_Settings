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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.Spannable;
import android.util.Log;
import android.widget.EditText;

import com.android.settings.R;
import com.android.settings.liquid.SettingsPreferenceFragment;
import com.android.settings.util.CMDProcessor;
import com.android.settings.util.Helpers;

public class UserInterface extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String PREF_CRT_ON = "crt_on";
    private static final String PREF_CRT_OFF = "crt_off";
    private static final String PREF_CUSTOM_CARRIER_LABEL = "custom_carrier_label";
    private static final String PREF_IME_SWITCHER = "ime_switcher";
    private static final String PREF_ENABLE_VOLUME_OPTIONS = "enable_volume_options";
    private static final String PREF_LONGPRESS_APP_TASKER = "longpress_app_tasker";
    private static final String PREF_ROTATION_ANIMATION = "rotation_animation_delay";
    private static final String PREF_180 = "rotate_180";
    private static final String PREF_DISABLE_SCREENSHOT_SOUND = "screenshot_sound";
    private static final String PREF_DISABLE_BOOT_AUDIO = "disable_bootaudio";
    private static final String PREF_RECENT_APP_SWITCHER = "recent_app_switcher";
    private static final String PREF_HOME_LONGPRESS = "long_press_home";
    private static final String DISABLE_BOOTANIMATION_PREF = "disable_bootanimation";

    CheckBoxPreference mAllow180Rotation;
    ListPreference mAnimationRotationDelay;
    CheckBoxPreference mCrtOffAnimation;
    CheckBoxPreference mCrtOnAnimation;
    Preference mCustomLabel;
    CheckBoxPreference mDisableBootAnimation;
    CheckBoxPreference mDisableBootAudio;
    CheckBoxPreference mDisableBugMailer;
    CheckBoxPreference mShowImeSwitcher;
    CheckBoxPreference mEnableVolumeOptions;
    CheckBoxPreference mLongPressAppTasker;
    CheckBoxPreference mDisableScreenshotSound;
    ListPreference mRecentAppSwitcher;
    ListPreference mHomeLongpress;
    String mCustomLabelText = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.prefs_ui);
        PreferenceScreen prefs = getPreferenceScreen();

        mCrtOffAnimation = (CheckBoxPreference) findPreference(PREF_CRT_OFF);
        mCrtOffAnimation.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.CRT_OFF_ANIMATION, 1) == 1);

        mCrtOnAnimation = (CheckBoxPreference) findPreference(PREF_CRT_ON);
        mCrtOnAnimation.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.CRT_ON_ANIMATION, 0) == 1);

        mShowImeSwitcher = (CheckBoxPreference) findPreference(PREF_IME_SWITCHER);
        mShowImeSwitcher.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.SHOW_STATUSBAR_IME_SWITCHER, 0) == 1);

        mEnableVolumeOptions = (CheckBoxPreference) findPreference(PREF_ENABLE_VOLUME_OPTIONS);
        mEnableVolumeOptions.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.ENABLE_VOLUME_OPTIONS, 0) == 1);

        mCustomLabel = findPreference(PREF_CUSTOM_CARRIER_LABEL);
        updateCustomLabelTextSummary();

        mAnimationRotationDelay = (ListPreference) findPreference(PREF_ROTATION_ANIMATION);
        mAnimationRotationDelay.setOnPreferenceChangeListener(this);
        mAnimationRotationDelay.setValue(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.ACCELEROMETER_ROTATION_SETTLE_TIME, 200) + "");

        mAllow180Rotation = (CheckBoxPreference) findPreference(PREF_180);
        mAllow180Rotation.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION_ANGLES, (1 | 2 | 8)) == (1 | 2 | 4 | 8));

        mRecentAppSwitcher = (ListPreference) findPreference(PREF_RECENT_APP_SWITCHER);
        mRecentAppSwitcher.setOnPreferenceChangeListener(this);
        mRecentAppSwitcher.setValue(Integer.toString(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.RECENT_APP_SWITCHER,
                0)));

        mDisableBootAnimation = (CheckBoxPreference) findPreference(
                DISABLE_BOOTANIMATION_PREF);
        mDisableBootAnimation.setChecked(!new File(
                "/system/media/bootanimation.zip").exists());

        mDisableBugMailer = (CheckBoxPreference) findPreference("disable_bugmailer");
        mDisableBugMailer.setChecked(!new File("/system/bin/bugmailer.sh").exists());

        mDisableScreenshotSound = (CheckBoxPreference) findPreference(PREF_DISABLE_SCREENSHOT_SOUND);
        mDisableScreenshotSound.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.SCREENSHOT_CAMERA_SOUND, 0) == 1);

        //TODO: summarys in ics shouldn't be dynamic; only exception is dialog input events
        // summary should be true if checked and false if unchecked
        if (mDisableBootAnimation.isChecked())
            mDisableBootAnimation.setSummary(R.string.disable_bootanimation_summary);

        if (!getResources().getBoolean(com.android.internal.R.bool.config_enableCrtAnimations)) {
            prefs.removePreference((PreferenceGroup) findPreference("crt"));
        } else {
            // can't get this working in ICS just yet
            ((PreferenceGroup) findPreference("crt")).removePreference(mCrtOnAnimation);
        }

        mDisableBootAudio = (CheckBoxPreference) findPreference(PREF_DISABLE_BOOT_AUDIO);

        mHomeLongpress = (ListPreference) findPreference(PREF_HOME_LONGPRESS);
        mHomeLongpress.setOnPreferenceChangeListener(this);
        mHomeLongpress.setValue(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.NAVIGATION_BAR_HOME_LONGPRESS, -1) + "");

        if (!hasHardwareButtons) {
            ((PreferenceGroup) findPreference("mics")).removePreference(mHomeLongpress);
        }

        // update summeries that should be dynamic
        updateListPrefs();
    }

    private void updateCustomLabelTextSummary() {
        mCustomLabelText = Settings.System.getString(getActivity().getContentResolver(),
                Settings.System.CUSTOM_CARRIER_LABEL);
        if (mCustomLabelText == null)
            mCustomLabel.setSummary(getString(R.string.custom_carrier_warning));
        else
            mCustomLabel.setSummary(mCustomLabelText);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mDisableBootAnimation) {
            boolean checked = ((CheckBoxPreference) preference).isChecked();
            if (checked) {
                Helpers.getMount("rw");
                new CMDProcessor().su
                        .runWaitFor("mv /system/media/bootanimation.zip /system/media/bootanimation.liquid");
                Helpers.getMount("ro");
                preference.setSummary(R.string.disable_bootanimation_summary);
            } else {
                Helpers.getMount("rw");
                new CMDProcessor().su
                        .runWaitFor("mv /system/media/bootanimation.liquid /system/media/bootanimation.zip");
                Helpers.getMount("ro");
            }
            return true;
        } else if (preference == mCrtOffAnimation) {
            boolean checked = ((CheckBoxPreference) preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.CRT_OFF_ANIMATION, checked ? 1 : 0);
            return true;
        } else if (preference == mCrtOnAnimation) {
            boolean checked = ((CheckBoxPreference) preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.CRT_ON_ANIMATION, checked ? 1 : 0);
            return true;
        } else if (preference == mShowImeSwitcher) {
            boolean checked = ((CheckBoxPreference) preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SHOW_STATUSBAR_IME_SWITCHER, checked ? 1 : 0);
            return true;
        } else if (preference == mEnableVolumeOptions) {
            boolean checked = ((CheckBoxPreference) preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.ENABLE_VOLUME_OPTIONS, checked ? 1 : 0);
            return true;
        } else if (preference == mCustomLabel) {
            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
            alert.setTitle("Custom Carrier Label");
            alert.setMessage("Please enter a new one!");

            // Set an EditText view to get user input
            final EditText input = new EditText(getActivity());
            input.setText(mCustomLabelText != null ? mCustomLabelText : "");
            alert.setView(input);

            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String value = ((Spannable) input.getText()).toString();
                    Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.CUSTOM_CARRIER_LABEL, value);
                    updateCustomLabelTextSummary();
                }
            });

            // if the frameworks see "default" then our TextView is View.GONE and
            // we show the system default carrier label controled by CarrierLabel.java
            alert.setNeutralButton("Default", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String vzw = "default";
                    Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.CUSTOM_CARRIER_LABEL, vzw);
                    updateCustomLabelTextSummary();
                }
            });

            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Canceled.
                }
            });

            alert.show();
        } else if (preference == mAllow180Rotation) {
            boolean checked = ((CheckBoxPreference) preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION_ANGLES, checked ? (1 | 2 | 4 | 8)
                            : (1 | 2 | 8));
            return true;
        } else if (preference == mDisableBootAudio) {
            if (mDisableBootAudio.isChecked()) {
                Helpers.getMount("rw");
                new CMDProcessor().su.runWaitFor("mv /system/media/boot_audio.mp3 /system/media/boot_audio.unicorn");
                Helpers.getMount("ro");
            } else {
                Helpers.getMount("rw");
                new CMDProcessor().su.runWaitFor("mv /system/media/boot_audio.unicorn /system/media/boot_audio.mp3");
                Helpers.getMount("ro");
            }
        } else if (preference == mDisableBugMailer) {
            boolean checked = ((CheckBoxPreference) preference).isChecked();
            if (checked) {
                Helpers.getMount("rw");
                new CMDProcessor().su
                        .runWaitFor("mv /system/bin/bugmailer.sh /system/bin/bugmailer.sh.liquid");
                Helpers.getMount("ro");
            } else {
                Helpers.getMount("rw");
                new CMDProcessor().su
                        .runWaitFor("mv /system/bin/bugmailer.sh.liquid /system/bin/bugmailer.sh");
                Helpers.getMount("ro");
            }
        } else if (preference == mDisableScreenshotSound) {
            boolean checked = ((CheckBoxPreference) preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SCREENSHOT_CAMERA_SOUND, checked ? 1 : 0);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean handled = false;
        if (preference == mAnimationRotationDelay) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION_SETTLE_TIME,
                    Integer.parseInt((String) newValue));
            handled = true;
        } else if (preference == mRecentAppSwitcher) {
            int val = Integer.parseInt((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.RECENT_APP_SWITCHER, val);
            Helpers.restartSystemUI();
            return true;
        } else if (preference == mHomeLongpress) {
            Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.NAVIGATION_BAR_HOME_LONGPRESS,
                Integer.parseInt((String) newValue));
            return true;
        }

        //update our dynamic values and return if we handled
        updateListPrefs();
        return handled;
    }

    public static void addButton(Context context, String key) {
        ArrayList<String> enabledToggles = Navbar
                .getButtonsStringArray(context);
        enabledToggles.add(key);
        Navbar.setButtonsFromStringArray(context, enabledToggles);
    }

    public static void removeButton(Context context, String key) {
        ArrayList<String> enabledToggles = Navbar
                .getButtonsStringArray(context);
        enabledToggles.remove(key);
        Navbar.setButtonsFromStringArray(context, enabledToggles);
    }

    private void updateListPrefs() {
        int mRotate = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION_SETTLE_TIME, 200);
        mAnimationRotationDelay.setSummary(String.format("Current: %s", mRotate));

        File audioFile = new File("/system/media/boot_audio.unicorn");
        mDisableBootAudio.setChecked(audioFile.isFile());
    }
}
