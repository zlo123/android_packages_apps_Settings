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

package com.android.setitngs.liquid.fragments;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Toast;

import com.android.settings.liquid.R;
import com.android.settings.util.Helpers;
import com.android.settings.liquid.SettingsPreferenceFragment;
import com.android.settings.widget.SeekBarPreference;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class StatusBarGeneral extends SettingsPreferenceFragment implements
                OnPreferenceChangeListener {

    private static final String TAG = "LiquidControl :StatusBarGeneral";
    private static final boolean DEBUG = false;
    private static final String PREF_SHOW_DATE = "show_date";
    private static final String PREF_DATE_FORMAT = "date_format";
    private static final String PREF_SETTINGS_BUTTON_BEHAVIOR = "settings_behavior";
    private static final String PREF_AUTO_HIDE_TOGGLES = "auto_hide_toggles";
    private static final String PREF_DATE_BEHAVIOR = "date_behavior";
    private static final String PREF_BRIGHTNESS_TOGGLE = "status_bar_brightness_toggle";
    private static final String PREF_SHOW_AOSP = "show_aosp_settings";
    private static final String PREF_SHOW_LIQUIDCONTROL = "show_liquid_control";
    private static final String PREF_ADB_ICON = "adb_icon";
    private static final String PREF_WINDOWSHADE_COLOR = "statusbar_windowshade_background_color";
    private static final String PREF_STATUSBAR_ALPHA = "statusbar_alpha";
    private static final String PREF_NOTIFICATION_COLOR = "notification_color";
    private static final String PREF_NOTIFICATION_ALPHA = "notification_alpha";
    private static final String PREF_STATUSBAR_UNEXPANDED_COLOR = "statusbar_unexpanded_color";
    private static final String PREF_STATUSBAR_HANDLE_ALPHA = "statusbar_handle_alpha";
    private static final String PREF_LAYOUT = "status_bar_layout";
    private static final String PREF_FONTSIZE = "status_bar_fontsize";
    private static String STATUSBAR_COLOR_SUMMARY_HOLDER;
    
    /* Notification Color/Alpha */
    private ColorPickerPreference mNotificationColor;
    private SeekBarPreference mNotificationAlpha;

    private static final String PREF_USER_BACKGROUND = "user_background";
    private static final String PREF_WINDOWSHADE_HANDLE = "windowshade_handle"; //TODO finish
    private static final int REQUEST_PICK_WALLPAPER = 199;
    private static final String USER_IMAGE_NAME = "windowshade_background.jpg";
    private static boolean USER_SUPPLIED_IMAGE;

    /* Default Color Schemes */
    private static final float STATUSBAR_EXPANDED_ALPHA_DEFAULT = 0.7f;
    private static final int STATUSBAR_EXPANDED_COLOR_DEFAULT = 0xFF000000;
    private static final float STATUSBAR_NOTIFICATION_ALPHA_DEFAULT = 0.8f;
    private static final int STATUSBAR_NOTIFICATION_COLOR_DEFAULT = 0xFF000000;
    private static final float STATUSBAR_HANDLE_ALPHA_DEFAULT = 0.85f;

    CheckBoxPreference mShowDate;
    ListPreference mDateFormat;
    CheckBoxPreference mShowAospSettings;
    CheckBoxPreference mDefaultSettingsButtonBehavior;
    CheckBoxPreference mAutoHideToggles;
    CheckBoxPreference mDateBehavior;
    CheckBoxPreference mStatusBarBrightnessToggle;
    CheckBoxPreference mShowLiquidControl;
    CheckBoxPreference mAdbIcon;
    ColorPickerPreference mWindowshadeBackground;
    SeekBarPreference mStatusbarAlpha;
    ColorPickerPreference mStatusbarUnexpandedColor;

    ListPreference mLayout;
    Preference mUserBackground;
    ListPreference mWindowshadeHandle;
    SeekBarPreference mStatusbarHandleAlpha;
    ListPreference mFontsize;
    NotificationManager mNoticeManager;
    Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity().getApplicationContext();
        addPreferencesFromResource(R.xml.prefs_statusbar_general);

        // Load preferences
        mShowDate = (CheckBoxPreference) findPreference(PREF_SHOW_DATE);
        mDateFormat = (ListPreference) findPreference(PREF_DATE_FORMAT);
        mDateFormat.setOnPreferenceChangeListener(this);
        mDefaultSettingsButtonBehavior = (CheckBoxPreference) findPreference(PREF_SETTINGS_BUTTON_BEHAVIOR);
        mAutoHideToggles = (CheckBoxPreference) findPreference(PREF_AUTO_HIDE_TOGGLES);
        mDateBehavior = (CheckBoxPreference) findPreference(PREF_DATE_BEHAVIOR);
        mStatusBarBrightnessToggle = (CheckBoxPreference) findPreference(PREF_BRIGHTNESS_TOGGLE);
        mShowAospSettings = (CheckBoxPreference) findPreference(PREF_SHOW_AOSP);
        mShowLiquidControl = (CheckBoxPreference) findPreference(PREF_SHOW_LIQUIDCONTROL);
        mAdbIcon = (CheckBoxPreference) findPreference(PREF_ADB_ICON);
        mWindowshadeBackground = (ColorPickerPreference) findPreference(PREF_WINDOWSHADE_COLOR);
        mWindowshadeBackground.setOnPreferenceChangeListener(this);
        mStatusbarAlpha = (SeekBarPreference) findPreference(PREF_STATUSBAR_ALPHA);
        mStatusbarAlpha.setOnPreferenceChangeListener(this);
        mStatusbarUnexpandedColor = (ColorPickerPreference) findPreference(PREF_STATUSBAR_UNEXPANDED_COLOR);
        mStatusbarUnexpandedColor.setOnPreferenceChangeListener(this);
        mNotificationColor = (ColorPickerPreference) findPreference(PREF_NOTIFICATION_COLOR);
        mNotificationColor.setOnPreferenceChangeListener(this);
        mLayout = (ListPreference) findPreference(PREF_LAYOUT);
        mLayout.setOnPreferenceChangeListener(this);
        mLayout.setValue(Integer.toString(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.STATUS_BAR_LAYOUT, 0)));
        mFontsize = (ListPreference) findPreference(PREF_FONTSIZE);
        mFontsize.setOnPreferenceChangeListener(this);
        mFontsize.setValue(Integer.toString(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.STATUSBAR_FONT_SIZE, 16)));

        mStatusbarHandleAlpha = (SeekBarPreference) findPreference(PREF_STATUSBAR_HANDLE_ALPHA);
        mStatusbarHandleAlpha.setOnPreferenceChangeListener(this);

        mUserBackground = (Preference) findPreference(PREF_USER_BACKGROUND);
        mWindowshadeHandle = (ListPreference) findPreference(PREF_WINDOWSHADE_HANDLE);
        mWindowshadeHandle.setOnPreferenceChangeListener(this);

        if (mTablet) {
            PreferenceScreen prefs = getPreferenceScreen();
            prefs.removePreference(mStatusBarBrightnessToggle);
            prefs.removePreference(mAutoHideToggles);
            prefs.removePreference(mDefaultSettingsButtonBehavior);
        }

        setHasOptionsMenu(true);
        updateSettings();

     }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.statusbar_general, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.reset:
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.STATUSBAR_WINDOWSHADE_USER_BACKGROUND, 0);
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.STATUSBAR_SHOW_DATE, 0);
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.STATUSBAR_REMOVE_AOSP_SETTINGS_LINK, 0);
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.STATUSBAR_REMOVE_LIQUIDCONTROL_LINK, 0);
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.STATUSBAR_DATE_FORMAT, 0);
                Settings.System.putFloat(getActivity().getContentResolver(),
                        Settings.System.STATUSBAR_EXPANDED_BOTTOM_ALPHA, STATUSBAR_EXPANDED_ALPHA_DEFAULT);
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.STATUSBAR_EXPANDED_BACKGROUND_COLOR, STATUSBAR_EXPANDED_COLOR_DEFAULT);
                Settings.System.putFloat(getActivity().getContentResolver(),
                        Settings.System.STATUSBAR_NOTIFICATION_ALPHA, STATUSBAR_NOTIFICATION_ALPHA_DEFAULT);
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.STATUSBAR_NOTIFICATION_COLOR, STATUSBAR_NOTIFICATION_COLOR_DEFAULT);
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.STATUSBAR_WINDOWSHADE_HANDLE_IMAGE, 0);
                Settings.System.putFloat(getActivity().getContentResolver(),
                        Settings.System.STATUSBAR_HANDLE_ALPHA, STATUSBAR_HANDLE_ALPHA_DEFAULT);

                updateSettings();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void updateSettings() {
        mShowDate.setChecked(Settings.System.getInt(mContext
                .getContentResolver(), Settings.System.STATUSBAR_SHOW_DATE, 0) == 1);
        mDefaultSettingsButtonBehavior.setChecked(Settings.System.getInt(mContext
                .getContentResolver(), Settings.System.STATUSBAR_SETTINGS_BEHAVIOR, 0) == 1);
        mAutoHideToggles.setChecked(Settings.System.getInt(mContext
                .getContentResolver(), Settings.System.STATUSBAR_QUICKTOGGLES_AUTOHIDE, 1) == 1);
        mDateBehavior.setChecked(Settings.System.getInt(mContext
                .getContentResolver(), Settings.System.STATUSBAR_DATE_BEHAVIOR, 0) == 1);
        mStatusBarBrightnessToggle.setChecked(Settings.System.getInt(mContext
                .getContentResolver(), Settings.System.STATUS_BAR_BRIGHTNESS_TOGGLE, 0) == 1);
        mShowAospSettings.setChecked(Settings.System.getInt(mContext
                .getContentResolver(), Settings.System.STATUSBAR_REMOVE_AOSP_SETTINGS_LINK, 0) == 1);
        mShowLiquidControl.setChecked(Settings.System.getInt(mContext
                .getContentResolver(), Settings.System.STATUSBAR_REMOVE_LIQUIDCONTROL_LINK, 0) == 1);
        mAdbIcon.setChecked(Settings.Secure.getInt(getActivity().getContentResolver(),
                Settings.Secure.ADB_ICON, 1) == 1);

        // update the Date format summary
        int dFormat = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUSBAR_DATE_FORMAT, 0);
        String date = null;
        String displayFormat = "Current: %s";
        switch (dFormat) {
            case 0:
                // default, February 14, 2012
                date = "Febuary 14, 2012";
            break;
            case 1:
                // Tuesday February 14, 2012
                date = "Tuesday February 14, 2012";
            break;
            case 2:
                // Tues February 14, 2012
                date = "Tues February 14, 2012";
            break;
            case 3:
                // Tuesday
                date = "Tuesday";
            break;
            case 4:
                // day 45 of 2012
                date = "day 45 of 2012";
            break;
            case 5:
                // Tues Feb 14
                date = "Tues Feb 14";
            break;
        }
        mDateFormat.setSummary(String.format(displayFormat, date));

        float expandedAlpha = Settings.System.getFloat(getActivity()
                .getContentResolver(), Settings.System.STATUSBAR_EXPANDED_BOTTOM_ALPHA, 1f);
        mStatusbarAlpha.setInitValue((int) (expandedAlpha * 100));
        mStatusbarAlpha.setSummary(String.format("%f", expandedAlpha * 100));

        float defaultAlpha = Settings.System.getFloat(getActivity()
                .getContentResolver(), Settings.System.STATUSBAR_NOTIFICATION_ALPHA, 0.8f);
        mNotificationAlpha = (SeekBarPreference) findPreference(PREF_NOTIFICATION_ALPHA);
        mNotificationAlpha.setInitValue((int) (defaultAlpha * 100));                 
        mNotificationAlpha.setOnPreferenceChangeListener(this);

        try {
            int expandedColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_EXPANDED_BACKGROUND_COLOR);
            // I'm blanking on a better way to setSummary
            String summary = String.format("%d", expandedColor);
            mWindowshadeBackground.setSummary(summary);
        } catch (SettingNotFoundException snfe) {
            // just let it go
        }
        
        try {
            int unexpandedColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_UNEXPANDED_COLOR);
            // I'm blanking on a better way to setSummary
            String summary = String.format("%d", unexpandedColor);
            mWindowshadeBackground.setSummary(summary);
        } catch (SettingNotFoundException snfe) {
            // just let it go
        }

        // TODO: update summary mWindowshadeHandle
        int handleImage = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.STATUSBAR_WINDOWSHADE_HANDLE_IMAGE, 0);
        String imageName = "default";
        switch (handleImage) {
            case 1:
                imageName = mContext.getString(R.string.windowshade_handle_liquid_1);
                break;
            case 0:
            default:
                imageName = mContext.getString(R.string.windowshade_handle_default);
        }
        mWindowshadeHandle.setSummary(imageName);

        // update statusbar handle alpha
        float handleAlpha = Settings.System.getFloat(getActivity()
                .getContentResolver(), Settings.System.STATUSBAR_HANDLE_ALPHA, STATUSBAR_HANDLE_ALPHA_DEFAULT);
        mStatusbarHandleAlpha.setInitValue((int) (handleAlpha * 100));
        mStatusbarHandleAlpha.setSummary(String.format("%f", handleAlpha * 100));
    }

    public boolean onPreferenceChange(Preference pref, Object newValue) {
        boolean success = false;

        if (pref == mDateFormat) {
            int val0 = Integer.parseInt((String) newValue);
            if (DEBUG) Log.d(TAG, "led on time new value: " + val0);
            success = Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_DATE_FORMAT, val0);
        } else if (pref == mStatusbarAlpha) {
            float val1 = Float.parseFloat((String) newValue);
            if (DEBUG) Log.d(TAG, "value:" + val1 / 100 + "    raw:" + val1);
            success = Settings.System.putFloat(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_EXPANDED_BOTTOM_ALPHA, val1 / 100);
        } else if (pref == mWindowshadeBackground) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String.valueOf(newValue)));
            pref.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            // first we must turn off the user background for this to show
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_WINDOWSHADE_USER_BACKGROUND, 0);
            success = Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_EXPANDED_BACKGROUND_COLOR, intHex);
            if (DEBUG) Log.d(TAG, String.format("new color hex value: %d", intHex));
        } else if (pref == mStatusbarUnexpandedColor) {
            String statusbar_hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            pref.setSummary(statusbar_hex);
            int intHex = ColorPickerPreference.convertToColorInt(statusbar_hex);
            success = Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_UNEXPANDED_COLOR, intHex);
            if (DEBUG) Log.d(TAG, "color value int:" + intHex);
        } else if (pref == mLayout) {
            int val = Integer.parseInt((String) newValue);
            success = Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_LAYOUT, val);
            Helpers.restartSystemUI();
        } else if (pref == mWindowshadeHandle) {
            int val = Integer.parseInt((String) newValue);
            success = Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_WINDOWSHADE_HANDLE_IMAGE, val);
        } else if (pref == mStatusbarHandleAlpha) {
            float handleValue = Float.parseFloat((String) newValue);
            if (DEBUG) Log.d(TAG, "value:" + handleValue / 100 + "    raw:" + handleValue);
            success = Settings.System.putFloat(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_HANDLE_ALPHA, handleValue / 100);
        } else if (pref == mFontsize) {
            int val = Integer.parseInt((String) newValue);
            success = Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_FONT_SIZE, val);
            Helpers.restartSystemUI();
        }  else if (pref == mNotificationColor) {
            String hexColor = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            pref.setSummary(hexColor);
            int color = ColorPickerPreference.convertToColorInt(hexColor);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUSBAR_NOTIFICATION_COLOR, color);
        } else if (pref == mNotificationAlpha) {
            float val = Float.parseFloat((String) newValue);
            Settings.System.putFloat(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_NOTIFICATION_ALPHA, val / 100);
            return true;
        }

        updateSettings();
        return success;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mDefaultSettingsButtonBehavior) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.STATUSBAR_SETTINGS_BEHAVIOR,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
            return true;
        } else if (preference == mAutoHideToggles) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.STATUSBAR_QUICKTOGGLES_AUTOHIDE,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
            return true;
        } else if (preference == mDateBehavior) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.STATUSBAR_DATE_BEHAVIOR,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
            return true;
        } else if (preference == mStatusBarBrightnessToggle) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.STATUS_BAR_BRIGHTNESS_TOGGLE,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
            return true;
        } else if (preference == mShowAospSettings) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.STATUSBAR_REMOVE_AOSP_SETTINGS_LINK,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
            return true;
        } else if (preference == mShowLiquidControl) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.STATUSBAR_REMOVE_LIQUIDCONTROL_LINK,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
            return true;
        } else if (preference == mAdbIcon) {
            boolean checked = ((CheckBoxPreference) preference).isChecked();
            Settings.Secure.putInt(getActivity().getContentResolver(),
                    Settings.Secure.ADB_ICON, checked ? 1 : 0);
            return true;
        } else if (preference == mShowDate) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.STATUSBAR_SHOW_DATE,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
            return true;
        } else if (preference == mUserBackground) {
            Display display = getActivity().getWindowManager().getDefaultDisplay();
            float screenSizeX = (float) display.getWidth();
            float screenSizeY = (float) display.getHeight();
            if (DEBUG) Log.d(TAG, "screenSizeX: " + screenSizeX + "	screenSizeY: " + screenSizeY);
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
            intent.setType("image/*");
            intent.putExtra("crop", "true");
            // aspect is 720x1038 ratio for cropper //TODO: is that accurate ratio?
            intent.putExtra("aspectX", 720);
            intent.putExtra("aspectY", 1038);
            // output will be size of screen
            intent.putExtra("outputX", screenSizeX);
            intent.putExtra("outputY", screenSizeY);
            intent.putExtra("scale", true);
            // draw a starting point square for the user
            intent.putExtra("spotlightX", screenSizeX);
            intent.putExtra("spotlightY", screenSizeY);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, getWindowshadeExternalUri());
            intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
            startActivityForResult(intent, REQUEST_PICK_WALLPAPER);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_PICK_WALLPAPER) {
                FileOutputStream userImageStream = null;
                try {
                    userImageStream = mContext.openFileOutput(USER_IMAGE_NAME, Context.MODE_WORLD_READABLE);
                } catch (FileNotFoundException e) {
                    return;
                }
                Uri selectedImageUri = getWindowshadeExternalUri();
                if (DEBUG) Log.d(TAG, "Selected image uri: " + selectedImageUri);
                Bitmap bitmap = BitmapFactory.decodeFile(selectedImageUri.getPath());
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, userImageStream);

                // force settings update
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.STATUSBAR_WINDOWSHADE_USER_BACKGROUND, 0);
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.STATUSBAR_WINDOWSHADE_USER_BACKGROUND, 1);
            }
        } else {
            // result was not ok disable user background then poke the useless setting to update
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_WINDOWSHADE_USER_BACKGROUND, 0);
            if (DEBUG) Log.d(TAG, "result was not ok resultCode: " + resultCode);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private Uri getWindowshadeExternalUri() {
        File dir = mContext.getExternalCacheDir();
        File userImage = new File(dir, USER_IMAGE_NAME);
        Log.d(TAG, "Statusbar background path: " + userImage.getAbsolutePath());
        return Uri.fromFile(userImage);
    }
}
