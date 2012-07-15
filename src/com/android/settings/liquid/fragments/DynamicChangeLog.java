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

package com.android.settings.liquid.fragments;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Gravity;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeSet;

import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.ResponseHandler;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DynamicChangeLog extends SettingsPreferenceFragment {
    private static final boolean DEBUG = true;
    private static final boolean JSON_SPEW = false;
    private static final String TAG = "DynamicChangeLog";

    // example of commit list from parser
    // https://api.github.com/repos/LiquidSmoothROMs/android_frameworks_base/commits?page=1
    // example of repo list from parser
    // https://api.github.com/orgs/LiquidSmoothROMs/repos

    // github json api addresses
    private static final String GITHUB_JSON = "https://api.github.com/";
    private static final String ORGANIZATION = "LiquidSmoothROMs/";
    private static final String REPO_URL = GITHUB_JSON + "orgs/" + ORGANIZATION + "repos";
    private static final String REPOS_PARSER = GITHUB_JSON + "repos/show/"
            + ORGANIZATION; // returns a list of our projects
    private static final String COMMITS_PAGE = "commits?page="; //later... + PAGE_NUMBER (30 returns by default)
    private static final String COMMITS_REQUEST_FORMAT = GITHUB_JSON + "repos/" + ORGANIZATION + "%s/" + COMMITS_PAGE + "%s";

    // classwide constants
    private static final String PREF_CAT = "version_preference_screens"; //TODO use a more generic blank screen with category
    private static final int DEFAULT_FLING_SPEED = 60;

    // classwide static variables
    private static String BRANCH;
    private static boolean ARE_IN_PROJECT_PATH;
    private static boolean REMOVE_AUTHOR_LAYOUT;
    private static boolean REMOVE_COMMITTER_LAYOUT;
    private static String AUTHOR_GRAVATAR_URL;
    private static String COMMITTER_GRAVATAR_URL;
    private static String COMMIT_AUTHOR;
    private static String COMMIT_COMMITTER;
    private static String COMMIT_MESSAGE;
    private static String COMMIT_DATE;
    private static String COMMIT_SHA;
    private static String COMMIT_URL;
    private static String PROJECT;
    private static Date LAST_UPDATE;

    // Dialogs (1001+)
    private static final int COMMIT_INFO_DIALOG = 1001;
    private static final int SORT_COMMITS = 1002;

    // Menu item ids (101+)
    private static final int MENU_ID_PACKAGES = 101;
    private static final int MENU_ID_COMMITLOG = 102;

    // classwide objects
    Context mContext;
    Handler mHandler;
    // static in case we need it in another thread
    PreferenceCategory mCategory;
    SharedPreferences mSharedPrefs; //to hold default branches

    // timers
    long mStartTime;
    long mStopTime;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        // initialize our classwide objects
        mContext = getActivity().getApplicationContext();
        mHandler = new Handler();
        mSharedPrefs = mContext.getSharedPreferences("dynamic_changelogs", Context.MODE_PRIVATE);

        // disabled to I start the menu items
        setHasOptionsMenu(true);

        // set defaults
        ARE_IN_PROJECT_PATH = true;

        // load blank screen & set initial title
        addPreferencesFromResource(R.xml.open_recovery); //we can hijack this for our needs
        mCategory = (PreferenceCategory) findPreference(PREF_CAT);
        mCategory.setOrderingAsAdded(false);
        mCategory.setTitle(getString(R.string.initial_changelog_category_title));

        // network communication must be done async
        // and be sure we don't waste bandwidth on silly rotation
        // if first run then the Bundle state will be null
        if (state == null) {
            // populate the initial screen with list of projects
            new DisplayProjectsList().execute();

        }
    }

    // this is the only method called right before every display of the menu
    // here we choose what dynamic content to display for the menu
    public void onPrepareOptionsMenu(Menu menu) {
        // remove old menu items
        menu.clear();

        // cant change branch if we are not viewing a project folder's commits
        if (ARE_IN_PROJECT_PATH)
            menu.add(0, MENU_ID_COMMITLOG, 0, getString(R.string.changelog_menu_commitlog_title));
        else 
            menu.add(0, MENU_ID_PACKAGES, 0, getString(R.string.changelog_menu_projects_title));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // pass the method on we don't need it our work was done in onPrepareOptionsMenu(Menu)
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case MENU_ID_PACKAGES:
                new DisplayProjectsList().execute();
                // reset menu tracker variable
                ARE_IN_PROJECT_PATH = true;
                return true;
            case MENU_ID_COMMITLOG:
                new generateCommitlog().execute();
                ARE_IN_PROJECT_PATH = false;
                return true;

            // This should never happen but just in case let the system handle the return
            default:
                return super.onContextItemSelected(item);
        }
    }

    private class DisplayProjectsList extends AsyncTask<Void, Void, Void> {
        public DisplayProjectsList() {
        }

        // can use UI thread here
        protected void onPreExecute() {
            // start with a clean view, always
            mCategory.removeAll();
            mCategory.setTitle(getString(R.string.loading_projects));
        }

        // automatically done on worker thread (separate from UI thread)
        protected Void doInBackground(Void... unused) {
            try {
                // network comms are not simple and require a few components
                // the client is the main construct
                HttpClient httpClient = new DefaultHttpClient();

                Log.i(TAG, "attempting to connect to: " + REPO_URL);

                // get requests the actual website
                HttpGet requestWebsite = new HttpGet(REPO_URL);
                // construct that handles recieving web streams to strings
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                // hold the response in a JSONArray
                JSONArray repoProjectsArray = new JSONArray(httpClient.execute(requestWebsite, responseHandler));
        
                // debugging
                if (DEBUG) Log.d(TAG, "repoProjectsArray.length() is: " + repoProjectsArray.length());

                // scroll through each item in array (projects in repo organization)
                for (int i = 0; i < repoProjectsArray.length(); i++) {
                    // make a new PreferenceScreen to 
                    PreferenceScreen mProject = getPreferenceManager().createPreferenceScreen(mContext);
                    // make an object of each repo
                    JSONObject projectsObject = (JSONObject) repoProjectsArray.get(i);

                    // extract info about each project
                    final String projectName = projectsObject.getString("name");
                    final String projectHtmlUrl = projectsObject.getString("html_url");
                    final String projectDescription = projectsObject.getString("description");
                    final int githubProjectId = projectsObject.getInt("id");

                    // apply info to our preference screen
                    mProject.setKey(githubProjectId + "");
                    if (projectDescription.contains("") || projectDescription == null) {
                        mProject.setTitle(projectName);
                        mProject.setSummary(projectDescription);
                    } else {
                        mProject.setTitle(projectDescription);
                        mProject.setSummary(projectName);
                    }

                    mProject.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference p) {
                            GetCommitList listFirstCommits = new GetCommitList();
                            listFirstCommits.PAGE_ = 1; // we start at the most recent commits
                            listFirstCommits.PROJECT_ = projectName;
                            listFirstCommits.execute();
                            return true;
                        }
                    });

                    mCategory.addPreference(mProject);
                }
            } catch (JSONException je) {
                if (DEBUG) Log.e(TAG, "Bad json interaction...", je);
            } catch (IOException ioe) {
                if (DEBUG) Log.e(TAG, "IOException...", ioe);
            } catch (NullPointerException ne) {
                if (DEBUG) Log.e(TAG, "NullPointer...", ne); //we may need to catch in the for(){} block
            }
            return null;
        }

        // can use UI thread here
        protected void onPostExecute(Void unused) {
            mCategory.setTitle(getString(R.string.org_projects));
        }
    }

    private class GetCommitList extends AsyncTask<Void, Void, Void> {
        // inner class constants
        final String DEFAULT_BRANCH = "ics";

        // inner class variables; populated before calling .execute(); if no BRANCH_ we assume ics
        int PAGE_ = -1;
        String BRANCH_;
        String PROJECT_;

        public GetCommitList() {
        }

        protected void onPreExecute() {
            // show commit after we load next set
            mCategory.setTitle(getString(R.string.loading_commits));
            if (PAGE_ <= 1)
                mCategory.removeAll();
        }

        protected Void doInBackground(Void... unused) {
            // so we don't acidentally crash the ui
            if (PROJECT_ == null || PAGE_ == -1) //int is not a valid null type
                return null;

            // TODO: deal with branches later
            String requestCommits = String.format(COMMITS_REQUEST_FORMAT, PROJECT_, PAGE_);

            if (BRANCH_ == null) BRANCH_ = DEFAULT_BRANCH;
            try {
                HttpClient httpClient = new DefaultHttpClient();

                Log.i(TAG, "attempting to connect to: " + requestCommits);
                HttpGet requestWebsite = new HttpGet(requestCommits);
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                JSONArray projectCommitsArray = new JSONArray(httpClient.execute(requestWebsite, responseHandler));
        
                // debugging
                if (DEBUG) Log.d(TAG, "projectCommitsArray.length() is: " + projectCommitsArray.length());
                if (JSON_SPEW) Log.d(TAG, "projectCommitsArray.toString() is: " + projectCommitsArray.toString());

                for (int i = 0; i < projectCommitsArray.length(); i++) {
                    PreferenceScreen mCommit = getPreferenceManager().createPreferenceScreen(mContext);
                    // make an object of each commit
                    JSONObject projectsObject = (JSONObject) projectCommitsArray.get(i);

                    // some fields are just plain strings we can parse
                    final String commitSsh = projectsObject.getString("sha"); // for setKey

                    final String commitWebPath = projectsObject.getString("url"); // JSON commit path

                    // author could possible be null so use a try block to prevent failures
                    // (merges have committers not authors, authors exist for the parent commits)
                    try {
                        // this is slightly different as we have many values for fields
                        // therefor each of these fields will be an object to itself (for each commit)
                        // author; committer; parents and commit
                        JSONObject authorObject = (JSONObject) projectsObject.getJSONObject("author");
                        JSONObject commitObject = (JSONObject) projectsObject.getJSONObject("commit");
                        if (JSON_SPEW) Log.d(TAG, "authorObject: " + authorObject.toString());

                        // pull needed info from our new objects (for each commit)
                        final String authorName = authorObject.getString("login"); // github screen name
                        final String authorAvatar = authorObject.getString("avatar_url"); // author's avatar url
                        final String commitMessage = commitObject.getString("message"); // commit message

                        // to grab the date we need to make a new object from
                        // the commit object and collect info from there
                        final JSONObject authorObject_ =
                                (JSONObject) commitObject.getJSONObject("author");
                        final JSONObject committerObject =
                                (JSONObject) projectsObject.getJSONObject("committer");
                        final String commitDate = authorObject_.getString("date"); // commit date
                        final String committerAvatar = committerObject.getString("avatar_url");
                        final String committerName = committerObject.getString("login");

                        // apply info to our preference screen
                        mCommit.setKey(commitSsh + "");
                        mCommit.setTitle(commitMessage);
                        mCommit.setSummary(authorName);

                        mCommit.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(Preference p) {
                                AUTHOR_GRAVATAR_URL = authorAvatar;
                                COMMITTER_GRAVATAR_URL = committerAvatar;
                                COMMIT_COMMITTER = committerName;
                                PROJECT = PROJECT_;
                                COMMIT_URL = commitWebPath;
                                COMMIT_AUTHOR = authorName;
                                COMMIT_MESSAGE = commitMessage;
                                COMMIT_DATE = commitDate;
                                COMMIT_SHA = commitSsh + "";
                                showDialog(COMMIT_INFO_DIALOG);
                                return true;
                            }
                        });

                        mCategory.addPreference(mCommit);

                    } catch (JSONException je) {
                        // no author found for commit
                        if (DEBUG) Log.d(TAG, "encountered a null value", je);
                    }
                }
                // append next 30 commits onClick()
                final PreferenceScreen mNext = getPreferenceManager().createPreferenceScreen(mContext);
                mNext.setTitle(getString(R.string.next_commits_page_title));
                mNext.setSummary(getString(R.string.next_commits_page_summary));
                mNext.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference p) {
                        GetCommitList nextList = new GetCommitList();
                        nextList.PAGE_ = PAGE_ + 1; // next page of commits (30)
                        nextList.PROJECT_ = PROJECT_; // stay in same project folder
                        nextList.execute();
                        mCategory.removePreference(mNext); // don't keep in list after we click
                        return true;
                    }
                });
                // avoid adding if we don't have commits, prob network fail :-/
                if (mCategory.getPreferenceCount() > 1)
                    mCategory.addPreference(mNext);
            } catch (JSONException je) {
                if (DEBUG) Log.e(TAG, "Bad json interaction...", je);
            } catch (IOException ioe) {
                if (DEBUG) Log.e(TAG, "IOException...", ioe);
            } catch (NullPointerException ne) {
                if (DEBUG) Log.e(TAG, "NullPointer...", ne);
            }
            return null;
        }

        protected void onPostExecute(Void unused) {
            mCategory.setTitle(getString(R.string.commits_title));
        }
    }

    private class GetCommitArray extends AsyncTask<Void, Void, Void> {
        boolean LAST_PROJECT = false;
        int PAGE_;
        String PROJECT_;
        int DATE_;
        boolean foundTheEnd;

        protected void onPreExecute() {
            if (PAGE_ < 1)
                PAGE_ = 1;

            foundTheEnd = false;
        }

        protected Void doInBackground(Void... noused) {
            String requestCommits = String.format(COMMITS_REQUEST_FORMAT, PROJECT_, PAGE_);
            try {
                HttpClient httpClient = new DefaultHttpClient();
                HttpGet requestWebsite = new HttpGet(requestCommits);
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                JSONArray projectCommitsArray = null;
                try {
                    projectCommitsArray = new JSONArray(httpClient.execute(requestWebsite, responseHandler));
                } catch (HttpResponseException httpError) {
                    foundTheEnd = true;
                    return null;
                }
        
                // debugging
                if (DEBUG) Log.d(TAG, "projectCommitsArray.length() is: " + projectCommitsArray.length());
                if (JSON_SPEW) Log.d(TAG, "projectCommitsArray.toString() is: " + projectCommitsArray.toString());

                for (int i = 0; i < projectCommitsArray.length(); i++) {
                    final JSONObject projectsObject =
                            (JSONObject) projectCommitsArray.get(i);
                    final PreferenceScreen gitCommitPref = getPreferenceManager()
                            .createPreferenceScreen(mContext);

                    // some fields are just plain strings we can parse
                    final String commitSha = projectsObject.getString("sha"); // for setKey

                    final String commitWebPath = projectsObject.getString("url"); // JSON commit path

                    // author could possible be null so use a try block to prevent failures
                    // (merges have committers not authors, authors exist for the parent commits)
                    try {
                        // this is slightly different as we have many values for fields
                        // therefor each of these fields will be an object to itself (for each commit)
                        // author; committer; parents and commit
                        JSONObject commitObject = (JSONObject) projectsObject.getJSONObject("commit");

                        JSONObject authorObject;
                        try {
                            authorObject = (JSONObject) projectsObject.getJSONObject("author");
                            if (JSON_SPEW) Log.d(TAG, "authorObject: " + authorObject.toString());
                            REMOVE_AUTHOR_LAYOUT = false;
                        } catch (JSONException je) {
                            // here is our problem we need to repopulate these if they are null
                            try {
                                JSONObject commitObjectAuthor = (JSONObject) commitObject
                                        .getJSONObject("author");
                                authorObject = new JSONObject();
                                // try to repopulate null array
                                authorObject.put("login", commitObjectAuthor.getString("name"));
                                authorObject.put("avatar_url", "VOID");
                            } catch (JSONException jse) {
                                // we are out of alternatives so give up
                                authorObject = new JSONObject();
                                // try to repopulate null array
                                authorObject.put("login", getString(R.string.unknown_author));
                                authorObject.put("avatar_url", "VOID");
                                REMOVE_AUTHOR_LAYOUT = true;
                            }
                        }

                        JSONObject committerObject;
                        try {
                            committerObject = (JSONObject) projectsObject.getJSONObject("committer");
                            REMOVE_COMMITTER_LAYOUT = false;
                        } catch (JSONException je_) {
                            try {
                                JSONObject commitObjectCommitter = (JSONObject) commitObject
                                        .getJSONObject("author");
                                // try to repopulate null array
                                committerObject = new JSONObject();
                                committerObject.put("login", commitObjectCommitter.getString("name"));
                                committerObject.put("avatar_url", "VOID");
                            } catch (JSONException jse_) {
                                // give up
                                committerObject = new JSONObject();
                                committerObject.put("login", getString(R.string.unknown_committer));
                                committerObject.put("avatar_url", "VOID");
                                REMOVE_COMMITTER_LAYOUT = true;
                            }
                        }

                        // pull needed info from our new objects (for each commit)
                        final String authorName = authorObject.getString("login"); // github screen name
                        final String authorAvatar = authorObject.getString("avatar_url"); // author's avatar url
                        final String commitMessage = commitObject.getString("message"); // commit message

                        // to grab the date we need to make a new object from
                        // the commit object and collect info from there
                        final JSONObject authorObject_ =
                                (JSONObject) commitObject.getJSONObject("author");
                        final String commitDate = authorObject_.getString("date"); // commit date
                        final String committerAvatar = committerObject.getString("avatar_url");
                        final String committerName = committerObject.getString("login");

                        gitCommitPref.setKey(commitDate);
                        // format the string for our date comparison inputted format: 2012-05-20T13:31:35-07:00
                        TimeZone utc = TimeZone.getTimeZone("UTC");
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        sdf.setTimeZone(utc);
                        GregorianCalendar cal = new GregorianCalendar(utc);
                        cal.setTime(sdf.parse(commitDate.substring(0, 10)));
                        if (DEBUG) Log.d(TAG, "commit date: " + cal.getTime());
                        Date timeStamp = cal.getTime();
                        if (timeStamp.after(LAST_UPDATE)) {
                            foundTheEnd = false;
                            gitCommitPref.setTitle(commitMessage);
                            gitCommitPref.setSummary(PROJECT_);
                            gitCommitPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                                @Override
                                public boolean onPreferenceClick(Preference p) {
                                    PROJECT = PROJECT_;
                                    COMMIT_URL = commitWebPath;
                                    COMMIT_AUTHOR = authorName;
                                    COMMIT_MESSAGE = commitMessage;
                                    COMMIT_DATE = gitCommitPref.getKey();
                                    COMMIT_COMMITTER = committerName;
                                    COMMIT_SHA = commitSha;
                                    AUTHOR_GRAVATAR_URL = authorAvatar;
                                    COMMITTER_GRAVATAR_URL = committerAvatar;
                                    showDialog(COMMIT_INFO_DIALOG);
                                    return true;
                                }
                            });
                            if (alreadyFoundCommit(gitCommitPref)) {
                                foundTheEnd = true;
                                break;
                            } else {
                                mCategory.addPreference(gitCommitPref);
                            }
                        } else {
                            // before our time
                            foundTheEnd = true;
                            break;
                        }
                    } catch (ParseException pe) {
                        if (DEBUG) Log.e(TAG, "failed to create calendar", pe);
                    } catch (JSONException je) {
                        if (DEBUG) Log.d(TAG, "bad json interaction while on webpage: " + requestCommits);
                        if (DEBUG) je.printStackTrace();
                    } catch (NullPointerException ne) {
                        if (DEBUG) Log.e(TAG, "found null value", ne);
                    }
                }
            } catch (JSONException je) {
                if (DEBUG) Log.e(TAG, "Bad JSON interaction while making master array", je);
            } catch (IOException ioe) {
                if (DEBUG) Log.e(TAG, "IOException...", ioe);
            } catch (NullPointerException ne) {
                if (DEBUG) Log.e(TAG, "NullPointer...", ne);
            }
            return null;
        }

        protected void onPostExecute(Void unused) {
            Log.i(TAG, "project:" + PROJECT_ + " page:" + PAGE_ + " last_project:" + LAST_PROJECT);
            if (!foundTheEnd) {
                GetCommitArray getMoreCommits = new GetCommitArray();
                getMoreCommits.PAGE_ = PAGE_ + 1;
                getMoreCommits.PROJECT_ = PROJECT_;
                getMoreCommits.execute();
            }

            if (DEBUG) Log.d(TAG, "show sort commits? " + LAST_PROJECT);
            if (LAST_PROJECT) {
                showDialog(SORT_COMMITS);
            }
        }
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        // send String[] (url[0]) return Bitmap
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected void onPreExecute() {
            bmImage.setVisibility(View.GONE);
        }

        protected Bitmap doInBackground(String... urls) {
            String avatarUrl = urls[0];
            if (DEBUG) Log.d(TAG, "downloading: " + avatarUrl);
            Bitmap mAvatar = null;
            try {
                InputStream in = new URL(avatarUrl).openStream();
                mAvatar = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e(TAG, "failed to download avatar", e);
            }
            return mAvatar;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
            bmImage.setVisibility(View.VISIBLE);
        }
    }

    private class generateCommitlog extends AsyncTask<Void, Void, Void> {
        public generateCommitlog() {
            mCategory.removeAll();
        }

        protected Void doInBackground(Void... no) {
           try {
                mStartTime = System.currentTimeMillis();
                // first we need the date of the latest update on goo.im
                String search = "http://goo.im/json2&action=search&query=teamliquid";
                HttpClient httpClient = new DefaultHttpClient();
                Log.i(TAG, "attempting to connect to: " + search);
                HttpGet requestWebsite = new HttpGet(search);
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                // we could link these commands into one array but
                // it becomes complex quickly so one step at a time
                JSONObject ourGooimObject = new JSONObject(httpClient.execute(requestWebsite, responseHandler));
                JSONArray ourList = ourGooimObject.getJSONArray("search_result");
                if (DEBUG) Log.d(TAG, "length of search result:" + ourList.length());
                // we don't really need the whole list we just need the first one
                JSONObject latestUpdate = ourList.getJSONObject(0);
                long unixDate = latestUpdate.getLong("modified");
                Date timeStamp = new Date((long) unixDate * 1000);
                // give this date to the main thread so we can compare in other threads
                LAST_UPDATE = timeStamp;
                Log.i(TAG, "Latest update was{ unix:" + unixDate + "	formatted:"
                        + timeStamp.toString() + " }");

                // now we know when the last update was so lets try
                // and find all the commits since that date we parse
                // from our github org account
                HttpClient http = new DefaultHttpClient();
                Log.i(TAG, "attempting to connect to: " + REPO_URL);
                HttpGet github = new HttpGet(REPO_URL);
                ResponseHandler<String> strings = new BasicResponseHandler();
                JSONArray ourGooimArray = new JSONArray(http.execute(github, strings));
                for (int i = 0; ourGooimArray.length() > i; i++) {
                    JSONObject commitObj = (JSONObject) ourGooimArray.get(i);
                    GetCommitArray getCommits = new GetCommitArray();
                    getCommits.PAGE_ = 1;
                    getCommits.PROJECT_ = commitObj.getString("name");
                    if (DEBUG) Log.d(TAG, "total repos:" + ourGooimArray.length() +
                            " current index:" + i);
                    getCommits.LAST_PROJECT = (ourGooimArray.length() == i + 1) ? true : false;
                    getCommits.execute();
                }
            } catch (JSONException je) {
                if (DEBUG) Log.e(TAG, "Bad json interaction...", je);
            } catch (IOException ioe) {
                if (DEBUG) Log.e(TAG, "IOException...", ioe);
            } catch (NullPointerException ne) {
                if (DEBUG) Log.e(TAG, "NullPointer...", ne);
            }
            return null;
        }

        protected void onPostExecute(Void unused) {
            mStopTime = System.currentTimeMillis();
            if (DEBUG) Log.d(TAG, "found: " + mCategory.getPreferenceCount() +
                "commits in: " + (mStopTime - mStartTime)/1000 + "s");
        }
    }

    private boolean alreadyFoundCommit(Preference pref) {
        boolean same = false;
        for (int i = 0; mCategory.getPreferenceCount() > i; i++) {
            Preference mCatPref = mCategory.getPreference(i);
            same = (pref.compareTo(mCatPref) == 0);
            if (same) break;
        }
        return same;
    }

    private void sortScreen(boolean chronologicalOrder) {
        if (chronologicalOrder) {
            ArrayList<Preference> foundCommits = new ArrayList<Preference>();
            ArrayList<String> orderDates = new ArrayList<String>();
            for (int i = 0; mCategory.getPreferenceCount() > i; i++) {
                Preference p = mCategory.getPreference(i);
                foundCommits.add(p);
                String stamp = p.getKey();
                // format the date using unix code for accutate sorting
                Calendar commitTimeStamp = GregorianCalendar.getInstance();
                // how substrings are derived (example format, account for 0 offset)
                // 2012-05-27T22:16:56-07:00
                // 012345678901234567890
                // 0         1         2
                try {
                    final int year = Integer.parseInt(stamp.substring(0, 4));
                    final int month = Integer.parseInt(stamp.substring(5, 7));
                    final int day = Integer.parseInt(stamp.substring(8, 10));
                    final int hours = Integer.parseInt(stamp.substring(11, 13));
                    final int mins = Integer.parseInt(stamp.substring(14, 16));
                    final int secs = Integer.parseInt(stamp.substring(17, 19));
                    commitTimeStamp.set(year, month, day, hours, mins, secs);
                    long unix = commitTimeStamp.getTimeInMillis();
                    if (DEBUG) Log.d(TAG, String.format("adding value: %d", unix));
                        orderDates.add(String.format("%d", unix));
                } catch (NumberFormatException badDate) {
                    badDate.printStackTrace();
                }
            }
            if (DEBUG) Log.d(TAG, "presorted orderDates.toString() {" + orderDates.toString() + "}");

            // use clone to find positions of numbers before sorting we
            // use that number to index where our preference *SHOULD* be
            ArrayList<String> clone = new ArrayList<String>(orderDates);
            ArrayList<Preference> orderedCommits = new ArrayList<Preference>(foundCommits);

            Collections.sort(orderDates);
            Collections.reverse(orderDates);
            // these are in the correct order (SHIT) hence a SUPER ANNOYING TODO: FIX SORTING!!!
            if (DEBUG) Log.d(TAG, "postsorted orderDates.toString() {" + orderDates.toString() + "}");

            // clear the screen and repopulate with ordered commits
            mCategory.removeAll();
            mCategory.setOrderingAsAdded(true);
            if (DEBUG) Log.d(TAG, "population of mCategory should be zero... mCategory.getPreferenceCount(): "
                    + mCategory.getPreferenceCount());
            ArrayList<Preference> commitsOrdered = new ArrayList<Preference>();
            for (int i = 0; orderDates.size() > i; i++) {
                int prevPosition = clone.lastIndexOf(orderDates.get(i));
                Preference pref = foundCommits.get(prevPosition);
                // since we are scrolling through the ordered times
                // we can just add as we see them and not worry about
                // size or compacity of the ArrayLists
                if (DEBUG) Log.d(TAG, "Moving from " + prevPosition + " to mCategory[index] " + i);

                // move to correct place
                orderedCommits.set(i, pref);
            }

            // apply order
            for (int i = 0; orderedCommits.size() > i; i++) {
                mCategory.addPreference(orderedCommits.get(i));
            }

            if (DEBUG) {
                Log.d(TAG, "foundCommits.size():" + foundCommits.size() + "\nclone.size():" +
                        clone.size() + "\norderDates.size():" + orderDates.size());
            }
        // we don't want to sort so just ensure we are in alphabetical order
        } else {
            mCategory.setOrderingAsAdded(false);
        }
    }

    public Dialog onCreateDialog(final int id) {
        switch (id) {
            default:
            case COMMIT_INFO_DIALOG:
                // get service and inflate our dialog
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View commitExtendedInfoLayout = inflater.inflate(R.layout.extended_commit_info_layout, null);

                // references for our objects
                ScrollView scroller = (ScrollView) commitExtendedInfoLayout.findViewById
                        (R.id.extended_commit_info_layout_scrollview);
                // so we scroll smoothly if commit message is large
                scroller.setSmoothScrollingEnabled(true);
                scroller.fling(DEFAULT_FLING_SPEED);

                // gain object references
                LinearLayout authorContainer = (LinearLayout) commitExtendedInfoLayout.findViewById
                        (R.id.author_container);
                LinearLayout committerContainer = (LinearLayout) commitExtendedInfoLayout.findViewById
                        (R.id.committer_container);
                ImageView authorAvatar = (ImageView) commitExtendedInfoLayout.findViewById
                        (R.id.author_avatar);
                ImageView committerAvatar = (ImageView) commitExtendedInfoLayout.findViewById
                        (R.id.committer_avatar);
                TextView author_header = (TextView) commitExtendedInfoLayout.findViewById
                        (R.id.author_header);
                TextView committer_header = (TextView) commitExtendedInfoLayout.findViewById
                        (R.id.committer_header);
                TextView author_tv = (TextView) commitExtendedInfoLayout.findViewById
                        (R.id.commit_author);
                TextView committer_tv = (TextView) commitExtendedInfoLayout.findViewById
                        (R.id.commit_committer);
                TextView message_tv = (TextView) commitExtendedInfoLayout.findViewById
                        (R.id.commit_message);
                TextView date_tv = (TextView) commitExtendedInfoLayout.findViewById
                        (R.id.commit_date);
                TextView sha_tv = (TextView) commitExtendedInfoLayout.findViewById
                        (R.id.commit_sha);

                // remove any LinearLayouts we don't have values for
                if (REMOVE_AUTHOR_LAYOUT) {
                    authorContainer.setVisibility(View.GONE);
                    author_header.setVisibility(View.GONE);
                    // reset our watcher
                    REMOVE_AUTHOR_LAYOUT = false;
                } else {
                    // since we have this value we don't hide and we load our images 
                    // this way we don't waste bandwidth loading legacy values
                    if (AUTHOR_GRAVATAR_URL != null)
                        new DownloadImageTask(authorAvatar).execute(AUTHOR_GRAVATAR_URL);
                    else
                        // this is important because if we have null value
                        // it won't fail till too late for us to use responsibly (sp? ...fuck it I don't care)
                        REMOVE_AUTHOR_LAYOUT = true;
                }

                if (REMOVE_COMMITTER_LAYOUT) {
                    committerContainer.setVisibility(View.GONE);
                    committer_header.setVisibility(View.GONE);
                    // reset our watcher
                    REMOVE_COMMITTER_LAYOUT = false;
                } else {
                    // try to populate the image from gravatar
                    // @link http://stackoverflow.com/a/9288544
                    if (COMMITTER_GRAVATAR_URL != null)
                         new DownloadImageTask(committerAvatar).execute(COMMITTER_GRAVATAR_URL);
                    else
                         REMOVE_COMMITTER_LAYOUT = true;
                }

                // setText for TextViews
                author_tv.setText(COMMIT_AUTHOR);
                committer_tv.setText(COMMIT_COMMITTER);
                message_tv.setText(COMMIT_MESSAGE);
                date_tv.setText(COMMIT_DATE);

                // we split the sha-1 hash into two strings because
                // it looks horrible by default display and smaller
                // size text is hard to read
                int halfHashLength = COMMIT_SHA.length() / 2;
                StringBuilder splitHash = new StringBuilder();
                splitHash.append(COMMIT_SHA.substring(0, halfHashLength));
                splitHash.append("-\n"); // to seperate the strings
                splitHash.append(COMMIT_SHA.substring(halfHashLength));
                splitHash.trimToSize();
                // set the text from our StringBuilder
                sha_tv.setText(splitHash.toString());

                // make a builder to helps construct our dialog
                final AlertDialog.Builder commitInfo = new AlertDialog.Builder(getActivity());
                commitInfo.setTitle(getString(R.string.commit_extended_info_title));
                commitInfo.setView(commitExtendedInfoLayout);

                // the order we place the buttons in is important
                // standard is:			| CANCEL | OK |
                // per our needs we use:	| CLOSE | WEBVIEW |
                commitInfo.setNegativeButton(getString(R.string.button_close), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int button) {
                        // just let the dialog go
                    }
                });

                commitInfo.setPositiveButton(getString(R.string.button_webview), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int button) {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        String webviewUrl = "https://github.com/" + ORGANIZATION + PROJECT + "/commit/" + COMMIT_SHA;
                        i.setData(Uri.parse(webviewUrl));
                        startActivity(i);
                    }
                });

                AlertDialog ad_commit = commitInfo.create();
                ad_commit.show();
                return ad_commit;

            case SORT_COMMITS:
                mStopTime = System.currentTimeMillis();

                LayoutInflater mLayoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View layout = mLayoutInflater.inflate(R.layout.sort_commits_dialog, null);
                ImageView octocat = (ImageView) layout.findViewById(R.id.github);
                octocat.setImageResource(R.drawable.octocat_class_act);
                TextView timeAndCount = (TextView) layout.findViewById(R.id.time_and_count);
                TextView message = (TextView) layout.findViewById(R.id.message);
                timeAndCount.setText(String.format(getString(R.string.toast_commitlog_info),
                        mCategory.getPreferenceCount(), (mStopTime - mStartTime) / 1000));
                message.setText(getString(R.string.sort_commits_message));

                if (DEBUG) Log.d(TAG, "found: " + mCategory.getPreferenceCount() +
                        "commits in: " + (mStopTime - mStartTime)/1000 + "s");
                if (DEBUG) Log.d(TAG, "Should we sort commits?");
                final AlertDialog.Builder sortDialog = new AlertDialog.Builder(getActivity());
                sortDialog.setTitle(getString(R.string.sort_commits_title));
                sortDialog.setView(layout);
                sortDialog.setNegativeButton(getString(R.string.button_donot_sort), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface di, int button) {
                        sortScreen(false);
                    }
                });
                sortDialog.setPositiveButton(getString(R.string.button_sort), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface di, int button) {
                        sortScreen(true);
                    }
                });
                AlertDialog ad_sort = sortDialog.create();
                ad_sort.show();
                return ad_sort;
        }
    }
}
