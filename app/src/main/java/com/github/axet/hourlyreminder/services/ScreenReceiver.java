package com.github.axet.hourlyreminder.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.github.axet.hourlyreminder.HourlyApplication;
import com.github.axet.hourlyreminder.basics.Alarm;

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
