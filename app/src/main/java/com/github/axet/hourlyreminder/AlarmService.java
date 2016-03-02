package com.github.axet.hourlyreminder;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import android.widget.HorizontalScrollView;

public class AlarmService extends IntentService {

    public AlarmService() {
        super("AlarmService");
    }

    @Override
    public void onHandleIntent(Intent workIntent) {
        long time = workIntent.getLongExtra("time", 0);

        Log.d(HourlyApplication.class.getSimpleName(), "AlarmService: " + HourlyApplication.formatTime(time));
        ((HourlyApplication) getApplication()).updateAlerts();

        ((HourlyApplication) getApplication()).soundAlarm(time);
    }
}
