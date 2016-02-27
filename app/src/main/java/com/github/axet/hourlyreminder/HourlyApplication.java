package com.github.axet.hourlyreminder;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class HourlyApplication extends Application {
    TextToSpeech tts;

    public static void updateAlerts(Context context) {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        boolean enabled = shared.getBoolean("enabled", false);
        Set<String> hours = shared.getStringSet("hours", new HashSet<String>());

        for (int i = 0; i < 24; i++) {
            String h = String.format("%02d", i);
            String a = SettingsActivity.class.getName() + "#" + h;

            final Intent intent = new Intent(context, AlarmService.class).setAction(a);
            intent.putExtra("hour", i);

            PendingIntent pe = PendingIntent.getService(context, i, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, i);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);

            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

            if (i <= hour)
                calendar.add(Calendar.DATE, 1);

            AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (enabled && hours.contains(h)) {
                Log.d(HourlyApplication.class.getSimpleName(), "Setting up alarm: " + calendar);
                alarm.setAlarmClock(new AlarmManager.AlarmClockInfo(calendar.getTimeInMillis(), pe), pe);
            } else {
                alarm.cancel(pe);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.US);
                }
            }
        });
    }

    public void soundAlarm() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int min = Calendar.getInstance().get(Calendar.MINUTE);
        String speak;

        if (min != 0)
            speak = String.format("Time is %d:%02d", hour, min);
        else
            speak = String.format("%d o'clock", hour);

        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if(shared.getBoolean("alarm", true)) {
            tts.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
        }

        Toast.makeText(getApplicationContext(), speak, Toast.LENGTH_SHORT).show();

        Bundle params = new Bundle();
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, shared.getFloat("volume", 1f));

        tts.speak(speak, TextToSpeech.QUEUE_FLUSH, params, UUID.randomUUID().toString());
    }
}
