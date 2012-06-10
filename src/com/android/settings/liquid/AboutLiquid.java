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
package com.android.settings.liquid;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

import com.android.settings.R;

public class AboutLiquid extends PreferenceActivity {

    public static final String TAG = "About Liquid";

    private static final String LIQUID_WEBSITE_PREF = "liquid_website";
    private static final String LIQUID_SOURCE_PREF = "liquid_source";
    private static final String LIQUID_IRC_PREF = "liquid_irc";

    private Preference mSiteUrl;
    private Preference mSourceUrl;
    private Preference mIrcUrl;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.prefs_about);
        PreferenceScreen prefSet = getPreferenceScreen();

        mSiteUrl = prefSet.findPreference(LIQUID_WEBSITE_PREF);
        mSourceUrl = prefSet.findPreference(LIQUID_SOURCE_PREF);
        mIrcUrl = prefSet.findPreference(LIQUID_IRC_PREF);
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mSiteUrl) {
            launchUrl("http://liquidsmoothroms.com/");
            return true;
        } else if (preference == mSourceUrl) {
            launchUrl("http://github.com/LiquidSmoothROMs");
            return true;
        } else if (preference == mIrcUrl) {
            launchUrl("http://webchat.freenode.net/?channels=liquids");
            return true;
        }
        return false;
    }

    private void launchUrl(String url) {
        Uri uriUrl = Uri.parse(url);
        Intent mActivity = new Intent(Intent.ACTION_VIEW, uriUrl);
        startActivity(mActivity);
    }
}
