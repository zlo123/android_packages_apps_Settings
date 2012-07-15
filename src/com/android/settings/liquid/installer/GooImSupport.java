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

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.liquid.SettingsPreferenceFragment;
import com.android.settings.util.Crypto;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Date;

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

public class GooImSupport extends SettingsPreferenceFragment {

    private static final boolean DEBUG = true;
    /* !!! SECURITY WARNING: ALWAYS SHIP CIPHER_DEBUG=false !!! */
    private static final boolean CIPHER_DEBUG = false;
    private static final String TAG = "LC : GooImSupport";
    private static final String DEVICE_NAME = android.os.Build.DEVICE;

    public static final String LIQUID_JSON_PARSER = "http://goo.im/json2&path=/devs/teamliquid/"
            + (DEVICE_NAME.contains("toro") ? "vzw" : "gsm");
    public static final String JSON_PARSER = "http://goo.im/json2&path=/devs&ro_board=toro";
    private static final String FORMATED_JSON_PATH = "http://goo.im/json2&path=%s&ro_board=toro";
    private static final String PREF_VERSIONS = "version_preference_screens";
    private static final String ERROR = "error";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_GOOIM_SUPPORTER_HASHCODE = "hashcode";
    private static String PARSED_WEBSITE;
    private static String STATIC_LOCATION;

    //Dialogs
    private static final int WEB_VIEW = 101;
    private static final int GOOIM_SUPPORTER_DIALOG = 102;

    Context mContext;
    PreferenceCategory mVersionViews;
    Handler mHandler;
    Runnable mReadWebsite;
    SharedPreferences mSharedPrefs;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        mContext = getActivity().getApplicationContext();
        mHandler = new Handler();
        mSharedPrefs = mContext.getSharedPreferences("gooim", Context.MODE_PRIVATE);

        addPreferencesFromResource(R.xml.open_recovery);
        mVersionViews = (PreferenceCategory) findPreference(PREF_VERSIONS);
        Log.d(TAG, "Device name: " + android.os.Build.DEVICE);
        setHasOptionsMenu(true);

        // else if rotated while on another dev's
        // product list we reload our products page
        if (state == null) {
            GetAvailableVersions listPop = new GetAvailableVersions();
            listPop.PARSER = LIQUID_JSON_PARSER;
            listPop.execute();
        }

        if (CIPHER_DEBUG) runCipherTest("some_constant_value", "TestString");
    }

    private void runCipherTest(String seed, String val) {
        try {
            Log.d(TAG, "encrypting:" + val + " with seed:" + seed);
            String encrpted_string = Crypto.encrypt(seed, val);
            Log.d(TAG, "ENCRIPTED STRING: " + encrpted_string);
            String unencrpted_string = Crypto.decrypt(seed, encrpted_string);
            Log.d(TAG, "DECRIPTED STRING: " + unencrpted_string);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.other_gooim_devs, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.other_gooim_devs:
                getFolder(JSON_PARSER);
                return true;
            case R.id.gooim_supporters:
                showDialog(GOOIM_SUPPORTER_DIALOG);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void getFolder(String s_) {
        GetDevList getDev = new GetDevList();
        // fail safe some times our string doesnt
        // make it into the async for some reason
        STATIC_LOCATION = s_;
        getDev.http = s_;
        getDev.execute();
    }

    private class GetAvailableVersions extends AsyncTask<Void, Void, Void> {
        String PARSER;

        // called when we create the AsyncTask object
        public GetAvailableVersions() {
        }

        // can use UI thread here
        protected void onPreExecute() {
            // start with a clean view, always
            mVersionViews.removeAll();
        }

        // automatically done on worker thread (separate from UI thread)
        protected Void doInBackground(Void... urls) {
            if (PARSER == null) {
                Log.e(TAG, "website path was null");
                return null;
            }

            if (DEBUG) Log.d(TAG, "addressing website " + PARSER);

            try {
                HttpClient httpClient = new DefaultHttpClient();
                HttpGet request = new HttpGet(PARSER);
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                JSONObject jsObject = new JSONObject(httpClient.execute(request, responseHandler));
                JSONArray jsArray = new JSONArray(jsObject.getString("list"));
                if (DEBUG) Log.d(TAG, "JSONArray.length() is: " + jsArray.length());
                for (int i = 0; i < jsArray.length(); i++) {
                    PreferenceScreen mVersionPresent = getPreferenceManager().createPreferenceScreen(mContext);
                    // parse strings from JSONObject
                    JSONObject JSONObject = (JSONObject) jsArray.get(i);
                    final String JSONfilename = JSONObject.getString("filename");
                    final String JSONid = JSONObject.getString("id");
                    final String JSONpath = JSONObject.getString("path");
                    final String JSONmd5 = JSONObject.getString("md5");
                    final String JSONtype = JSONObject.getString("type");
                    final String JSONshort_url = JSONObject.getString("short_url");
                    final String JSONdownloads = JSONObject.getString("downloads");

                    // debug
                    String log_formatter = "filename:{%s}	id:{%s}	path:{%s}	md5:{%s}	type:{%s}	short_url:{%s}";
                    if (DEBUG) Log.d(TAG, String.format(log_formatter, JSONfilename, JSONid, JSONpath, JSONmd5, JSONtype, JSONshort_url));

                    mVersionPresent.setKey(JSONid);
                    // TODO we should prob pull a version from this for the title
                    mVersionPresent.setTitle(JSONfilename);
                    mVersionPresent.setSummary(JSONtype);
                    mVersionPresent.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference p) {
                            Toast.makeText(mContext, "This file has been downloaded " + JSONdownloads + " + times",
                                    Toast.LENGTH_LONG).show();
                            PARSED_WEBSITE = "http://goo.im" + JSONpath;
                            showDialog(WEB_VIEW);
                            return true;
                        }
                    });
                    mVersionViews.addPreference(mVersionPresent);
                }
            } catch (JSONException e) {
                if (DEBUG) e.printStackTrace();
            } catch (IOException ioe) {
                if (DEBUG) ioe.printStackTrace();
            }
            return null;
        }

        // can use UI thread here
        protected void onPostExecute(Void unused) {
        }
    }

    private class GetDevList extends AsyncTask<Void, Void, Void> {
        public String http;
        String format_web_address;

        // can use UI thread here
        protected void onPreExecute() {
            mVersionViews.removeAll();

            if (http == null && STATIC_LOCATION != null) {
                http = STATIC_LOCATION;
                STATIC_LOCATION = null;
            }

            format_web_address = String.format("http://goo.im/json2&path=%s&ro_board=%s",
                http, android.os.Build.DEVICE);
        }

        // automatically done on worker thread (separate from UI thread)
        protected Void doInBackground(Void... urls) {
            // we user seperate try blocks for folders and files
            try {
                HttpClient httpClient = new DefaultHttpClient();
                HttpGet request = new HttpGet(http.contains("http") ? http : format_web_address);
                if (DEBUG) Log.d(TAG, "using website: " + (http.contains("http") ? http : format_web_address));
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                JSONObject jsObject = new JSONObject(httpClient.execute(request, responseHandler));
                JSONArray jsArray = new JSONArray(jsObject.getString("list"));
                if (DEBUG) Log.d(TAG, "JSONArray.length() is: " + jsArray.length());
                for (int i = 0; i < jsArray.length(); i++) {

                    PreferenceScreen mDevsFolder = getPreferenceManager().createPreferenceScreen(mContext);
                    final JSONObject obj_ = (JSONObject) jsArray.get(i);
                    // parse strings from JSONObject
                    try {
                        final String folder = obj_.getString("folder");
                        mDevsFolder.setTitle(folder);
                        mDevsFolder.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(Preference p) {
                                // move to the next folder
                                if (DEBUG) Log.d(TAG, "Sending http=" + folder);
                                getFolder(folder);
                                return true;
                            }
                        });

                        if (!http.contains(folder)) {
                            // we don't want to add the same folder we are currently viewing
                            if (DEBUG) Log.d(TAG, "not adding the folder we are viewing");
                            mVersionViews.addPreference(mDevsFolder);
                        }
                    } catch (JSONException je) {
                        // we didn't find a folder maybe we have files?
                        if (DEBUG) je.printStackTrace();
                    }

                    // seperate try block so we don't fail if we have folders and files
                    try {
                        PreferenceScreen mDevsFiles = getPreferenceManager().createPreferenceScreen(mContext);
                        final String JSONfilename = obj_.getString("filename");
                        final String JSONid = obj_.getString("id");
                        final String JSONpath = obj_.getString("path");
                        final String JSONmd5 = obj_.getString("md5");
                        final String JSONdownloads = obj_.getString("downloads");
                        final String JSONtype = obj_.getString("type"); // unused right now
                        final String JSONshort_url = obj_.getString("short_url"); // unused right now

                        // date fromatting test
                        long unixDate = obj_.getLong("modified");
                        Date date = new Date((long) unixDate * 1000);
                        Log.i(TAG, "Latest update was{ unix:" + unixDate + "	formatted:" + date.toString() + " }");

                        mDevsFiles.setKey(JSONid);
                        // TODO we should prob pull a version from this for the title
                        mDevsFiles.setTitle(JSONfilename);
                        mDevsFiles.setSummary(JSONtype);
                        mDevsFiles.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(Preference p) {
                                Toast.makeText(mContext, "This file has been downloaded " + JSONdownloads + " + times",
                                    Toast.LENGTH_LONG).show();
                                PARSED_WEBSITE = "http://goo.im" + JSONpath;
                                showDialog(WEB_VIEW);
                                return true;
                            }
                        });
                        mVersionViews.addPreference(mDevsFiles);
                    } catch (JSONException je) {
                        // if we don't find file info just skip this part
                    }
                }
            } catch (JSONException e) {
                if (DEBUG) e.printStackTrace();
            } catch (IOException ioe) {
                if (DEBUG) ioe.printStackTrace();
            }
            return null;
        }

        // can use UI thread here
        protected void onPostExecute(Void unused) {
            mVersionViews.setTitle(getString(R.string.dev_list));
        }
    }

    private class GetGooImSupporterHash extends AsyncTask<Void, Void, Void> {
        private final String INVALID_LOGIN = "INVALID_LOGIN";
        String USERNAME;
        String PASSWORD;
        String HASHCODE;

        // called when we create the AsyncTask object
        public GetGooImSupporterHash() {
        }

        // can use UI thread here
        protected void onPreExecute() {
        }

        // automatically done on worker thread (separate from UI thread)
        protected Void doInBackground(Void... urls) {
            if (USERNAME == null) {
                Log.e(TAG, "website path was null");
                return null;
            }

            String PASS_THE_SALT = "http://goo.im/salt&username=" + USERNAME.trim() + "&password=" + PASSWORD.trim();

            try {
                HttpClient httpClient = new DefaultHttpClient();
                if (CIPHER_DEBUG) Log.d(TAG, "addressing website {" + PASS_THE_SALT + "}");
                HttpGet request = new HttpGet(PASS_THE_SALT);
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                // request new hashcode
                String hashcode = httpClient.execute(request, responseHandler);
                if (DEBUG) Log.d(TAG, "GooIm supporter hash: " + hashcode);
                if (INVALID_LOGIN.contains(hashcode) || hashcode == null) return null;
                HASHCODE = hashcode;
            } catch (IOException ioe) {
                if (DEBUG) ioe.printStackTrace();
            }
            return null;
        }

        // can use UI thread here
        protected void onPostExecute(Void unused) {
            // since SharedPreferences isn't thread safe we save
            // on the UI thread so we can use the committed data
            // while this class is already loaded
            SharedPreferences.Editor prefs = mSharedPrefs.edit();
            prefs.putString(KEY_GOOIM_SUPPORTER_HASHCODE, HASHCODE);
            prefs.putString(KEY_USERNAME, USERNAME);
            if (CIPHER_DEBUG) Log.d(TAG, "Unencryped USERNAME: " + USERNAME + " PASSWORD: " + PASSWORD);
            // storing passwords in plain text is BAD PROGRAMMING!!!
            // we encrypt to store and decrypt in onCreate to use
            String pass_crypto = null;
            try {
                // Yes, username is an insecure key however
                // this is open source so any key is insecure
                // so using the USERNAME WILL stop casual
                // spying eyes, but if .gov wants to decrypt your
                // goo.im password they will not have problems, fyi.
                pass_crypto = Crypto.encrypt(USERNAME, PASSWORD);
            } catch (Exception e) {
                if (CIPHER_DEBUG) e.printStackTrace();
            }
            if (pass_crypto == null) return;
            if (CIPHER_DEBUG) Log.d(TAG, "USERNAME: " + USERNAME + " Encryped PASSWORD: " + pass_crypto);
            prefs.putString(KEY_PASSWORD, pass_crypto);
            prefs.commit();
        }
    }

    private String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            if (DEBUG) e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                if (DEBUG) e.printStackTrace();
            }
        }
            return sb.toString();
    }

    public Dialog onCreateDialog(final int id) {
        switch (id) {
            default:
            case WEB_VIEW:
                String mAddress;
                boolean mUseHash = false;

                String hash = mSharedPrefs.getString(KEY_GOOIM_SUPPORTER_HASHCODE, null);
                if (hash != null) {
                    mAddress = PARSED_WEBSITE + "&hash=" + hash;
                    mUseHash = true;
                } else {
                    mAddress = PARSED_WEBSITE;
                    mUseHash = false;
                }

                AlertDialog.Builder mDownloadFile = new AlertDialog.Builder(getActivity());
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View webview_layout = inflater.inflate(R.layout.webview_dialog, null);

                final WebView mWebView = (WebView) webview_layout.findViewById(R.id.webview1);

                mWebView.getSettings().setJavaScriptEnabled(true);
                mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
                mWebView.getSettings().setSupportMultipleWindows(false);

                // passed website
                if (DEBUG) Log.d(TAG, "addressing website: " + mAddress);

                mDownloadFile.setView(webview_layout);

                if (mAddress != null) mWebView.loadUrl(mAddress);
                PARSED_WEBSITE = null;
                final AlertDialog ad_0 = mDownloadFile.create();
                ad_0.show();

                // we remove the dialog that called the webview
                // there is no public method to kill webviews
                // so user must be exit on their own
                Handler mKillDialogs = new Handler();
                Runnable mReleaseDialog = new Runnable() {
                    public void run() {
                        mWebView.destroy();
                        ad_0.dismiss();
                    }
                };
                Runnable mReleaseWebView = new Runnable() {
                    public void run() {
                        mWebView.destroy();
                    }
                };

                if (mUseHash) {
                    /* user shouldn't have delay so drop dialogs quicker */
                    mKillDialogs.postDelayed(mReleaseWebView, 8 * 1000);
                    mKillDialogs.postDelayed(mReleaseDialog, 5 * 1000);
                } else {
                    mKillDialogs.postDelayed(mReleaseWebView, 14 * 1000);
                    mKillDialogs.postDelayed(mReleaseDialog, 9 * 1000);
                }
                return ad_0;
            case GOOIM_SUPPORTER_DIALOG:
                LayoutInflater inflater_ = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View passwordFields = inflater_.inflate(R.layout.save_theme_dialog, null);
                final AlertDialog.Builder passDialog = new AlertDialog.Builder(getActivity());
                passDialog.setView(passwordFields);

                // since we are hijacking the save_theme_Dialog
                // we need to reset the TextViews
                final TextView username_textview = (TextView) passwordFields.findViewById(R.id.title_textview_id);
                final TextView password_textview = (TextView) passwordFields.findViewById(R.id.summary_textview_id);
                final EditText username = (EditText) passwordFields.findViewById(R.id.title_input_edittext);
                final EditText password = (EditText) passwordFields.findViewById(R.id.summary_input_edittext);

                // set password mode
                password.setTransformationMethod(new android.text.method.PasswordTransformationMethod().getInstance());

                // inform the user we use a cipher to store their password
                Toast.makeText(mContext, getString(R.string.inform_user_about_cipher),
                            Toast.LENGTH_LONG).show();

                // set textviews
                username_textview.setText(getString(R.string.username));
                password_textview.setText(getString(R.string.password));

                // set previously saved info
                Runnable getSharedPreferences = new Runnable() {
                    public void run() {
                        String stored_username = mSharedPrefs.getString(KEY_USERNAME, null);
                        String stored_password = mSharedPrefs.getString(KEY_PASSWORD, null);
                        if (stored_username != null && stored_username != null) {
                            try {
                                Log.d(TAG, "found values {username:" + stored_username +
                                        " encrypted_password:" + stored_password + "}");
                                String decrypted_pw = Crypto.decrypt(stored_username, stored_password);
                                username.setText(stored_username);
                                password.setText(decrypted_pw);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                };
                mHandler.post(getSharedPreferences);

                passDialog.setNegativeButton(getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // nothing
                    }
                });

                passDialog.setPositiveButton(getString(R.string.button_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        GetGooImSupporterHash getHash = new GetGooImSupporterHash();
                        getHash.USERNAME = username.getText().toString().trim();
                        getHash.PASSWORD = password.getText().toString().trim();
                        getHash.execute();
                    }
                });

                AlertDialog ad_gooim = passDialog.create();
                ad_gooim.show();

                final Button getHash = (Button) ad_gooim.getButton(AlertDialog.BUTTON_POSITIVE);
                getHash.setEnabled(false);
                username.addTextChangedListener(new TextWatcher() {
                    public void afterTextChanged(Editable e) {
                    }
                    public void beforeTextChanged(CharSequence cs, int start, int count, int after) {
                    }
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        int snLength = username.getText().length();
                        int psLength = password.getText().length();
                        if (snLength > 0 && psLength > 0) {
                            getHash.setEnabled(true);
                        } else {
                            getHash.setEnabled(false);
                        }
                    }
                });

                password.addTextChangedListener(new TextWatcher() {
                    public void afterTextChanged(Editable e) {
                    }
                    public void beforeTextChanged(CharSequence cs, int start, int count, int after) {
                    }
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        int snLength = username.getText().length();
                        int psLength = password.getText().length();
                        if (snLength > 0 && psLength > 0) {
                            getHash.setEnabled(true);
                        } else {
                            getHash.setEnabled(false);
                        }
                    }
                });
                return ad_gooim;
        }
    }
}
