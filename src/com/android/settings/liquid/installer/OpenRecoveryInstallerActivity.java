/*
 * Copyright (C) 2012 The LiquidSmoothROMs Project
 * author JBirdVegas@gmail.com 2012
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

package com.android.settings.liquid.installer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.liquid.SettingsPreferenceFragment;
import com.android.settings.util.CMDProcessor;
import com.android.settings.widget.Md5Preference;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.IOException;
import java.lang.StringBuilder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.ResponseHandler;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OpenRecoveryScriptSupport extends Activity {

    private static final String TAG = "LC : OpenRecoveryScriptSupport";
    private static final boolean DEBUG = true;

    private static final int INSTALL_PROMPT = 1001;
    private static final String SCRIPT_PATH = "/cache/recovery/openrecoveryscript";
    private static final String LIQUID_PATH = "/sdcard/LiquidControl/";
    private static final String SHARED_WIPECACHE = "prev_wipe_cache";
    private static final String SHARED_WIPEDALVIK = "prev_wipe_dalvik";
    private static final String SHARED_WIPEDATA = "prev_wipe_data";
    private static final String SHARED_BACKUP = "prev_backup";
    private static final String SHARED_BACKUP_COMPRESSION = "prev_backup_compression";
    private static final String LINE_RETURN = "\n";
    private final CMDProcessor cmd = new CMDProcessor();
    private static String ZIP_PATH = null;
    private static final String LIQUID_JSON_PARSER = com.android.settings.liquid.installer.GooImSupport.LIQUID_JSON_PARSER;
    private static final String JSON_PARSER = com.android.settings.liquid.installer.GooImSupport.JSON_PARSER;

    Context mContext;
    Handler mHandler;
    Intent mIntent;
    PowerManager mPowerManager;
    PreferenceScreen mPreferenceScreen;
    SharedPreferences mSP;

    // Preferences
    Md5Preference mMd5;
    Preference mFileInfo;
    Preference mExecute;

    // install options
    CheckBoxPreference mWipeCache;
    CheckBoxPreference mWipeDalvik;
    CheckBoxPreference mWipeData;
    CheckBoxPreference mBackup;
    CheckBoxPreference mBackupCompression;

    @Override
    public void onCreate(Bundle liquid) {
        super.onCreate(liquid);
        mContext = getActivity().getApplicationContext();

        // initialize the worker thread handler
        mHandler = new Handler();

        // initialize the PowerManager
        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);

        // capture absolute file path
        mIntent = getActivity().getIntent();
        if (mIntent != null) {
            Log.d(TAG, "Intent found: " + mIntent);
            final Uri mUri = mIntent.getData();
            if (mUri != null) {
                ZIP_PATH = mUri.getEncodedPath();
                if (DEBUG) Log.d(TAG, "Intent data found: " + mUri + "	Path encoded: " + ZIP_PATH);
            }
        }

        addPreferencesFromResource(R.xml.open_recovery_script_support);
        findPrefs();

        Runnable getSharedPreferences = new Runnable() {
            public void run() {
                mSP = mContext.getSharedPreferences("previous_install_config", Context.MODE_PRIVATE);
                mWipeCache.setChecked(mSP.getBoolean(SHARED_WIPECACHE, true));
                mWipeData.setChecked(mSP.getBoolean(SHARED_WIPEDATA, true));
                mWipeDalvik.setChecked(mSP.getBoolean(SHARED_WIPEDALVIK, true));
                mBackup.setChecked(mSP.getBoolean(SHARED_BACKUP, true));
                mBackupCompression.setChecked(mSP.getBoolean(SHARED_BACKUP_COMPRESSION, true));

                if (mBackup.isChecked()) mBackupCompression.setEnabled(true);
                else mBackupCompression.setEnabled(false);
            }
        };

        // make the worker thread get us some info
        loadFileInfo();
        mHandler.post(getSharedPreferences);
    }

    private void findPrefs() {
        // get views
        mPreferenceScreen = (PreferenceScreen) findPreference("ors_support_preference_screen");
        mWipeCache = (CheckBoxPreference) findPreference("wipe_cache_checkbox");
        mWipeData = (CheckBoxPreference) findPreference("wipe_data_checkbox");
        mWipeDalvik = (CheckBoxPreference) findPreference("wipe_dalvik_checkbox");
        mBackup = (CheckBoxPreference) findPreference("backup_checkbox");
        mBackupCompression = (CheckBoxPreference) findPreference("backup_compression_checkbox");
        mFileInfo = (Preference) findPreference("fileinfo");
        mMd5 = (Md5Preference) findPreference("md5_preference");
        mExecute = (Preference) findPreference("execute");
    }

    private void loadFileInfo() {
        Runnable getFileInfo = new Runnable() {
            public void run() {
                try {
                    File mZip = new File(ZIP_PATH);
                    String mbs = Long.toString(mZip.length() /  1024) + " MB";
                    if (DEBUG) Log.d(TAG, String.format("file: %s size: %s", mZip.getAbsolutePath(), mbs));
                    mFileInfo.setTitle("File: " + mZip.getName());
                    mFileInfo.setSummary("Path: " + mZip.getAbsolutePath());
                    Log.d(TAG, "filepath found: adding md5 preference and begining md5 calculation");
                    mPreferenceScreen.addPreference(mMd5);
                    updateMD5();
                    mPreferenceScreen.addPreference(mExecute);
                } catch (NullPointerException npe) {
                    String note = getString(R.string.click_here_to_find_zips);
                    mFileInfo.setTitle(note);
                    mFileInfo.setSummary("");
                    Log.d(TAG, "filepath was null: hiding md5 preference");
                    mPreferenceScreen.removePreference(mMd5);
                    mPreferenceScreen.removePreference(mExecute);
                    if (DEBUG) npe.printStackTrace();
                }
            }
        };
        mHandler.post(getFileInfo);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mFileInfo) {
            Intent pickZIP = new Intent(mContext, com.android.settings.util.FilePicker.class);
            pickZIP.putExtra("zip", true);
            startActivityForResult(pickZIP, 1);
            return true;
        } else if (preference == mWipeCache) {
            if (!mWipeCache.isChecked())
                    Toast.makeText(mContext, getString(R.string.warn_about_dirty_flash),
                            Toast.LENGTH_SHORT).show();
            return true;
        } else if (preference == mWipeData) {
            if (!mWipeData.isChecked())
                    Toast.makeText(mContext, getString(R.string.warn_about_dirty_flash),
                            Toast.LENGTH_SHORT).show();
            return true;
        } else if (preference == mWipeDalvik) {
            if (!mWipeDalvik.isChecked())
                    Toast.makeText(mContext, getString(R.string.warn_about_dirty_flash),
                            Toast.LENGTH_SHORT).show();
            return true;
        } else if (preference == mBackup) {
            if (!mBackup.isChecked()) {
                Toast.makeText(mContext, getString(R.string.warn_about_no_backup),
                        Toast.LENGTH_SHORT).show();
                mBackupCompression.setEnabled(false);
            } else {
                mBackupCompression.setEnabled(true);
            }
            return true;
        } else if (preference == mMd5) {
            updateMD5();
            return true;
        } else if (preference == mExecute) {
            WriteScript task = new WriteScript();
            task.filePath_ = ZIP_PATH;
            task.wipeData_ = mWipeData.isChecked();
            task.wipeCache_ = mWipeCache.isChecked();
            task.wipeDalvik_ = mWipeDalvik.isChecked();
            task.backup_ = mBackup.isChecked();
            task.backupCompression_ = mBackupCompression.isChecked();

            SharedPreferences.Editor prefs = mSP.edit();
            prefs.putBoolean(SHARED_WIPEDATA, mWipeData.isChecked());
            prefs.putBoolean(SHARED_WIPECACHE, mWipeCache.isChecked());
            prefs.putBoolean(SHARED_WIPEDALVIK, mWipeDalvik.isChecked());
            prefs.putBoolean(SHARED_BACKUP, mBackup.isChecked());
            prefs.putBoolean(SHARED_BACKUP_COMPRESSION, mBackupCompression.isChecked());
            prefs.commit();

            task.execute();
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // we don't need to worry about what the
        // requestCode was because we only have one intent
        try {
            ZIP_PATH = data.getStringExtra("open_filepath");
            loadFileInfo();
        } catch (NullPointerException ne) {
            // user backed out of file picker
        }
    }
    private void updateMD5() {
        if (ZIP_PATH != null) {
            CalculateMd5 md5_ = new CalculateMd5();
            md5_.fPath = ZIP_PATH;
            md5_.execute();
        }
    }

    private class WriteScript extends AsyncTask<Void, Void, Void> {
        StringBuilder script = new StringBuilder();
        String filePath_ = null;
        String script_to_be_written_ = null;
        Boolean success_ = false;
        Boolean wipeData_ = false;
        Boolean wipeCache_ = false;
        Boolean wipeDalvik_ = false;
        Boolean backup_ = false;
        Boolean backupCompression_ = false;

        // can use UI thread here
        protected void onPreExecute() {
            // shouldn't happen but you never know...
            if (filePath_ == null) return;
            if (DEBUG) Log.d(TAG, "onPreExecute prepare for worker thread");
            if (backup_) {
                script.append("backup SDCB");
                if (backupCompression_) script.append("O");
                script.append(" " + LIQUID_PATH + LINE_RETURN);
            }
            if (wipeData_) script.append("wipe data" + LINE_RETURN);
            if (wipeCache_) script.append("wipe dalvik" + LINE_RETURN);
            if (wipeDalvik_) script.append("wipe dalvik" + LINE_RETURN);
            script.append("install " + filePath_ + LINE_RETURN);
            script_to_be_written_ = script.toString();
        }

        // automatically done on worker thread (separate from UI thread)
        protected Void doInBackground(Void... urls) {
            if (DEBUG) Log.d(TAG, "worker thread is writing script:"
                    + LINE_RETURN + script_to_be_written_);
            // all we need to do is write the file
            // but not on the UI thread
            String format_output = "echo %s > " + SCRIPT_PATH;
            File orss_ = new File(SCRIPT_PATH);
            File parent_orss_ = new File(orss_.getParent());
            FileWriter out = null;
            BufferedWriter bw = null;
            try {
                if (!orss_.exists()) orss_.createNewFile();
                out = new FileWriter(SCRIPT_PATH);
                bw = new BufferedWriter(out);
                try {
                    bw.append(script_to_be_written_);
                    success_ = true;
                } finally {
                    if (bw != null) bw.close();
                }
            } catch (IOException ioe) {
                success_ = false;
                if (DEBUG) ioe.printStackTrace();
            }
            if (DEBUG) {
                Log.d(TAG, "Script info: path {" + SCRIPT_PATH + "}");
                Log.d(TAG, "	isFile:" + orss_.isFile());
                Log.d(TAG, "	canWrite:" + orss_.canWrite());
                Log.d(TAG, "parentDir {" + parent_orss_.getAbsolutePath()
                        + "}	canWrite:" + parent_orss_.canWrite());
            }
            return null;
        }

        // can use UI thread here
        protected void onPostExecute(Void yourMom) {
            if (DEBUG) Log.d(TAG, "onPostExecute finished with worker thread");
            if (success_) {
                Toast.makeText(mContext, getString(R.string.filewrite_success),
                        Toast.LENGTH_SHORT).show();
                // reboot with the intent of going into recovery
                mPowerManager.reboot("recovery");
            } else {
                Toast.makeText(mContext, getString(R.string.filewrite_fail),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private class CalculateMd5 extends AsyncTask<Void, Void, Void> {
        String fPath = null;
        String newMd5 = null;

        protected void onPreExecute() {
            mMd5.setTitle("");
            mMd5.setSummary(getString(R.string.generating_md5));
        }

        protected Void doInBackground(Void... urls) {
            if (fPath == null) return null;
            MessageDigest complete;
            BufferedInputStream fis = null;
            File file_ = new File(fPath);
            try {
                complete = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException noDice) {
                noDice.printStackTrace(); //major error we should display stack trace
                return null;
            }
            try {
                fis =  new BufferedInputStream(new FileInputStream(file_));
                byte[] buffer = new byte[8192];
                int numRead;
                while ((numRead = fis.read(buffer)) > 0) {
                    complete.update(buffer, 0, numRead);
                }

                fis.close();
            } catch (IOException ioe) {
                // FileInputStream failed to close properly
                if (DEBUG) ioe.printStackTrace();
            }

            String result = "";
            byte[] b = complete.digest();
            for (int i=0; i < b.length; i++) {
                result += Integer.toString((b[i] & 0xff ) + 0x100, 16).substring(1);
            }
            newMd5 = result;
            return null;
        }

        protected void onPostExecute(Void yourMom) {
            if (DEBUG) Log.d(TAG, "Calculated md5 checksum: " + newMd5);
            mMd5.setTitle(getString(R.string.md5_title));
            mMd5.setSummary(newMd5);
            new CheckMD5vsGooIm().execute();
        }
    }

    private class CheckMD5vsGooIm extends AsyncTask<Void, Void, Void> {
        private String result;
        private HttpResponse response;
        private String JSONfilename;
        private String JSONmd5;
        private String localChecksum;
        private String gooimChecksum;
        boolean foundit = false;
        // called when we create the AsyncTask object
        public CheckMD5vsGooIm() {
            if (DEBUG) Log.d(TAG, "AsyncTask CheckMD5vsGooIm Object created");
        }

        // can use UI thread here
        protected void onPreExecute() {
            if (DEBUG) Log.d(TAG, "onPreExecute");
            mMd5.setTitle(getString(R.string.verify_md5_with_gooim));
            localChecksum = mMd5.getSummary().toString();
        }

        // automatically done on worker thread (separate from UI thread)
        protected Void doInBackground(Void... urls) {
            if (DEBUG) Log.d(TAG, "...trolling goo.im for md5 matches");
            result = "";
            try {
                HttpClient ourDevHostFolder = new DefaultHttpClient();
                HttpGet devFolderJSON = new HttpGet(LIQUID_JSON_PARSER);
                ResponseHandler<String> dev_responseHandler = new BasicResponseHandler();
                JSONObject jsObject = new JSONObject(ourDevHostFolder.execute
                        (devFolderJSON, dev_responseHandler));
                JSONArray jsArray = new JSONArray(jsObject.getString("list"));
                if (DEBUG) Log.d(TAG, "JSONArray.length() is: " + jsArray.length());
                for (int i = 0; i < jsArray.length(); i++) {

                    // parse strings from JSONObject
                    JSONObject JSONObject = (JSONObject) jsArray.get(i);
                    JSONfilename = JSONObject.getString("filename");
                    JSONmd5 = JSONObject.getString("md5");

                    // debug
                    String log_formatter = "(%d/%d) filename:{%s}	goo.im_md5:{%s}";
                    if (DEBUG) Log.d(TAG, String.format(log_formatter, i + 1, jsArray.length(), JSONfilename, JSONmd5));
                    String l = new String(localChecksum);
                    String g = new String(JSONmd5);
                    if (l.contains(g)) {
                        Log.d(TAG, "We have a winner MD5 Checksum matches! Verified: "
                                + JSONfilename);
                        foundit = true;
                        gooimChecksum = JSONmd5;
                        return null;
                    }                    
                }

                // ^ we check our lists then check other
                // developers work for md5 matches :( users cheating on us, lol
                // get the dev list
                HttpClient devHostFolder = new DefaultHttpClient();
                HttpGet ourFolderJSON = new HttpGet(JSON_PARSER);
                ResponseHandler<String> liquid_responseHandler = new BasicResponseHandler();
                JSONObject dev_jsObject = new JSONObject(devHostFolder.execute
                        (ourFolderJSON, liquid_responseHandler));
                JSONArray dev_jsArray = new JSONArray(dev_jsObject.getString("list"));
                if (DEBUG) Log.d(TAG, "JSONArray.length() is: " + dev_jsArray.length()
                        + "	toString(): " + dev_jsArray.toString());
                for (int i = 0; i < dev_jsArray.length(); i++) {
                    JSONObject dev_obj = (JSONObject) dev_jsArray.get(i);
                    try {
                        trollGooIm(dev_obj.getString("folder"));
                    } catch (JSONException n) {
                        n.printStackTrace();
                    }
                }
            } catch (JSONException e) {
                if (DEBUG) e.printStackTrace();
            } catch (IOException ioe) {
                if (DEBUG) ioe.printStackTrace();
            }
            return null;
        }

        private void trollGooIm(String devPath) {
            Log.d(TAG, "trollGooIm(" + devPath + ")");
            String folder = "";
            boolean files_ = false;
            String filesPresentMd5_ = null;
            String nameOfFile = null;
            String formatWebAddress = String.format("http://goo.im/json2&path=%s&ro_board=%s",
                    devPath, android.os.Build.DEVICE);
            try {
                // it wasn't one of ours so lets troll goo.im and see id we can find it
                HttpClient deviceDevHostFolder = new DefaultHttpClient();
                HttpGet deviceFolderJSON = new HttpGet(formatWebAddress);
                ResponseHandler<String> device_responseHandler = new BasicResponseHandler();

                JSONObject device_jsObject = new JSONObject(deviceDevHostFolder.execute
                        (deviceFolderJSON, device_responseHandler));
                JSONArray device_jsArray = new JSONArray(device_jsObject.getString("list"));
                String check;
                for (int i = 0; device_jsArray.length() > i; i++) {
                    boolean moreToCheck = false;
                    // if we find a value don't check the rest
                    if (foundit) return;
                    try {
                        JSONObject obj = (JSONObject) device_jsArray.get(i);

                        String checkPlease = new String(devPath);
                        folder = new String(obj.getString("folder"));

                        // check if files are present
                        try {
                            filesPresentMd5_ = new String(obj.getString("md5"));
                            nameOfFile = new String(obj.getString("filename"));

                            String l0 = new String(localChecksum);
                            String g0 = new String(filesPresentMd5_);
                            if (l0.contains(g0)) {
                                Log.d(TAG, "We have a winner MD5 Checksum matches! Verified: "
                                        + nameOfFile);
                                foundit = true;
                                return;
                            } else {
                                // we use i+1 to offset arrays using 0 as first slot
                                String output_fmt = "(%d/%d) filename:{%s}	goo.im_md5:{%s}";
                                if (DEBUG) Log.d(TAG, String.format(output_fmt, i + 1, device_jsArray.length(),
                                        nameOfFile, filesPresentMd5_));
                                files_ = true;
                            }
                        } catch (JSONException js) {
                            // no md5 value in JSONObject
                        } catch (Exception e) {
                            if (DEBUG) e.printStackTrace();
                        }
                    } catch (JSONException je) {
                        // finding folder failed
                        if (DEBUG) je.printStackTrace();
                    }
                }
            } catch (Exception e) {
                if (DEBUG) e.printStackTrace();
            }
            // we didn't find matching md5 try again with next folder
            if (folder != null && files_ == false) trollGooIm(folder);
        }

        // can use UI thread here
        protected void onPostExecute(Void stopLookingAtMeSwan) {
            if (DEBUG) Log.d(TAG, "finished trolling Goo.Im");
            mMd5.isMatch(foundit);
            mMd5.setTitle(foundit ? R.string.verify_md5_pass : R.string.verify_md5_fail);
        }
    }
}
