package com.github.axet.hourlyreminder;

import android.app.IntentService;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class AlarmService extends IntentService {

    public AlarmService() {
        super("AlarmService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onHandleIntent(Intent workIntent) {
        long time = workIntent.getLongExtra("time", 0);
        if (workIntent.getAction() == HourlyApplication.NOTIFICATION) {
            ((HourlyApplication) getApplication()).showNotification(time);
        } else if (workIntent.getAction() == HourlyApplication.CANCEL) {
            ((HourlyApplication) getApplication()).tomorrow(time);
        } else {
            Log.d(HourlyApplication.class.getSimpleName(), "AlarmService: " + HourlyApplication.formatTime(time));
            ((HourlyApplication) getApplication()).updateAlerts();

            ((HourlyApplication) getApplication()).soundAlarm(time);
        }
    }
}

