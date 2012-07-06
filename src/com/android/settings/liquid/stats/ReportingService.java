package com.android.settings.liquid.stats;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.Settings;

public class ReportingService extends Service {
    protected static final String TAG = "LiquidStats";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        if (intent.getBooleanExtra("firstBoot", false)) {
            promptUser();
            Log.d(TAG, "Prompting user for opt-in.");
        } else {
            Log.d(TAG, "User has opted in -- reporting.");
            Thread thread = new Thread() {
                @Override
                public void run() {
                    report();
                }
            };
            thread.start();
        }
        return Service.START_REDELIVER_INTENT;
    }


    private void report() {
        String deviceId = Utilities.getUniqueID(getApplicationContext());
        String deviceName = Utilities.getDevice();
        String deviceVersion = Utilities.getModVersion();
        String KernelVersion = Utilities.getKernelVersion();
        String deviceCountry = Utilities.getCountryCode(getApplicationContext());
        String deviceCarrier = Utilities.getCarrier(getApplicationContext());
        String deviceCarrierId = Utilities.getCarrierId(getApplicationContext());

        Log.d(TAG, "SERVICE: Device ID=" + deviceId);
        Log.d(TAG, "SERVICE: Device Name=" + deviceName);
        Log.d(TAG, "SERVICE: Device Version=" + deviceVersion);
        Log.d(TAG, "SERVICE: Kernel Version=" + KernelVersion);
        Log.d(TAG, "SERVICE: Country=" + deviceCountry);
        Log.d(TAG, "SERVICE: Carrier=" + deviceCarrier);
        Log.d(TAG, "SERVICE: Carrier ID=" + deviceCarrierId);

        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://stats.liquidsmooth.org/submit");
        try {
            List<NameValuePair> kv = new ArrayList<NameValuePair>(5);
            kv.add(new BasicNameValuePair("device_hash", deviceId));
            kv.add(new BasicNameValuePair("device_name", deviceName));
            kv.add(new BasicNameValuePair("device_version", deviceVersion));
            kv.add(new BasicNameValuePair("kernel_version", KernelVersion));
            kv.add(new BasicNameValuePair("device_country", deviceCountry));
            kv.add(new BasicNameValuePair("device_carrier", deviceCarrier));
            kv.add(new BasicNameValuePair("device_carrier_id", deviceCarrierId));
            httppost.setEntity(new UrlEncodedFormEntity(kv));
            httpclient.execute(httppost);
            getSharedPreferences("LiquidStats", 0).edit().putLong(AnonymousStats.ANONYMOUS_LAST_CHECKED,
                    System.currentTimeMillis()).apply();
        } catch (Exception e) {
            Log.e(TAG, "Got Exception", e);
        }
        ReportingServiceManager.setAlarm(this);
        stopSelf();
    }

    private void promptUser() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent nI = new Intent();
        nI.setComponent(new ComponentName(getPackageName(),Settings.AnonymousStatsActivity.class.getName()));
        PendingIntent pI = PendingIntent.getActivity(this, 0, nI, 0);
        Notification.Builder builder = new Notification.Builder(this)
        .setSmallIcon(R.drawable.ic_lq_stats_notif)
        .setAutoCancel(true)
        .setTicker(getString(R.string.anonymous_statistics_title))
        .setContentIntent(pI)
        .setWhen(0)
        .setContentTitle(getString(R.string.anonymous_statistics_title))
        .setContentText(getString(R.string.anonymous_notification_desc));
        nm.notify(1, builder.getNotification());
    }
}
