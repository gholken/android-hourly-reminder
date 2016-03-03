package com.github.axet.hourlyreminder.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.github.axet.hourlyreminder.HourlyApplication;
import com.github.axet.hourlyreminder.basics.Alarm;
import com.github.axet.hourlyreminder.basics.Reminder;
import com.github.axet.hourlyreminder.basics.Sound;

public class AlarmIntentService extends IntentService {

    Sound sound;

    public AlarmIntentService() {
        super(AlarmIntentService.class.getSimpleName());
    }

    @Override
    public void onCreate() {
        super.onCreate();

        sound = new Sound(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (sound != null) {
            sound.close();
            sound = null;
        }
    }

    @Override
    public void onHandleIntent(Intent workIntent) {
        long time = workIntent.getLongExtra("time", 0);

        HourlyApplication app = ((HourlyApplication) getApplication());

        if (workIntent.getAction() == HourlyApplication.NOTIFICATION) {
            app.showNotificationUpcoming(time);
        } else if (workIntent.getAction() == HourlyApplication.CANCEL) {
            app.tomorrow(time);
        } else if (workIntent.getAction() == AlarmService.DISMISS) {
            app.dismissActiveAlarm();
        } else {
            soundAlarm(time);
        }
    }


    // alarm come from service call (System Alarm Manager) for specified time
    //
    // we have to check what 'alarms' do we have at specified time (can be reminder + alarm)
    // and act propertly.
    public void soundAlarm(long time) {
        HourlyApplication app = ((HourlyApplication) getApplication());
        app.updateAlerts();

        // find hourly reminder + alarm = combine proper sound notification_upcoming (can be merge beep, speech, ringtone)
        //
        // then sound alarm or hourly reminder

        Alarm a = app.getAlarm(time);

        if (a != null && a.enable) {
            app.activateAlarm(a);
            return;
        }

        Reminder reminder = app.getReminder(time);

        if (reminder != null) {
            SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);
            if (shared.getBoolean("beep", false)) {
                sound.playBeep(new Runnable() {
                    @Override
                    public void run() {
                        sound.playSpeech(null);
                    }
                });
            } else {
                sound.playSpeech(null);
            }
            return;
        }
    }
}
