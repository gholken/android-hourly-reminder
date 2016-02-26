package com.github.axet.hourlyreminder;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class AlarmService extends IntentService {

    public AlarmService() {
        super("AlarmService");
    }

    @Override
    public void onHandleIntent(Intent workIntent) {
        Log.d(AlarmService.class.getSimpleName(), "Alarm: " + workIntent);
        ((HourlyApplication) getApplication()).updateAlerts(getApplicationContext());

        ((HourlyApplication) getApplication()).soundAlarm();
    }
}
