package com.github.axet.hourlyreminder.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.github.axet.hourlyreminder.HourlyApplication;
import com.github.axet.hourlyreminder.activities.AlarmActivity;
import com.github.axet.hourlyreminder.basics.Alarm;

public class ScreenReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(ScreenReceiver.class.getSimpleName(), "ScreenReceiver");

        Intent i = new Intent(context, AlarmActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }

}
