/*
 * Copyright (C) 2012 RaymanFX (raymanfx@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.raymanfx.settings.extras;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import java.util.Formatter;
import com.raymanfx.settings.tools.RootChecker;
import com.raymanfx.settings.filepicker.FileChooser;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

//
// GAPPS download link (will improve this with a direct link to the package)
//

public class Gapps extends SettingsPreferenceFragment {

	// Defines for imported values
		Preference mGooUrl;
		Preference mRebootRecovery;
		Preference mFilePicker;

        @Override
	public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	addPreferencesFromResource(R.xml.gapps);

	mGooUrl = findPreference("gapps_goo");
	mRebootRecovery = findPreference("install_gapps_recovery");
	mFilePicker = findPreference("install_gapps_filepicker");

	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mGooUrl) {
            launchUrl("http://www.goo.im/gapps/");
        } else if (preference == mRebootRecovery) {
	    RootChecker.runRootCommand("reboot recovery");
        } else if (preference == mFilePicker) {
            /* WIP */
	    //startActivity(new Intent(this, FileChooser.class));
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void launchUrl(String url) {
        Uri uriUrl = Uri.parse(url);
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, uriUrl);
        startActivity(browserIntent);
    }

}