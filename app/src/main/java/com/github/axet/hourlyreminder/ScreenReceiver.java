package com.github.axet.hourlyreminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ScreenReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(ScreenReceiver.class.getSimpleName(), "ScreenReciver");

        Alarm a = ((HourlyApplication) context.getApplicationContext()).getActiveAlarm();
        if (a != null) {
            ((HourlyApplication) context.getApplicationContext()).showAlarmActivity();
        }
    }
}
