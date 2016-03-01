package com.github.axet.hourlyreminder;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class HourlyApplication extends Application {
    TextToSpeech tts;
    Handler handler;
    List<Alarm> alarms;

    public void loadAlarms() {
        alarms = new ArrayList<>();

        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int c = shared.getInt("Alarm_Count", 0);
        if (c == 0) {
            alarms.add(new Alarm());
        }

        for (int i = 0; i < c; i++) {
            String prefix = "Alarm_" + i + "_";
            Alarm a = new Alarm();
            a.time = shared.getLong(prefix + "Time", 0);
            a.enable = shared.getBoolean(prefix + "Enable", false);
            a.weekdays = shared.getBoolean(prefix + "WeekDays", false);
            a.setWeekDays(shared.getStringSet(prefix + "WeekDays_Values", null));
            a.ringtone = shared.getBoolean(prefix + "Ringtone", false);
            a.ringtoneValue = shared.getString(prefix + "Ringtone_Values", "");
            a.beep = shared.getBoolean(prefix + "Beep", false);
            a.speech = shared.getBoolean(prefix + "Speech", false);
            alarms.add(a);
        }
    }

    public List<Alarm> getAlarms() {
        return alarms;
    }

    public void saveAlarms() {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor edit = shared.edit();
        edit.putInt("Alarm_Count", alarms.size());
        for (int i = 0; i < alarms.size(); i++) {
            Alarm a = alarms.get(i);
            String prefix = "Alarm_" + i + "_";
            edit.putLong(prefix + "Time", a.time);
            edit.putBoolean(prefix + "Enable", a.enable);
            edit.putBoolean(prefix + "WeekDays", a.weekdays);
            edit.putStringSet(prefix + "WeekDays_Values", a.getWeekDays());
            edit.putBoolean(prefix + "Ringtone", a.ringtone);
            edit.putString(prefix + "Ringtone_Value", a.ringtoneValue);
            edit.putBoolean(prefix + "Beep", a.beep);
            edit.putBoolean(prefix + "Speech", a.speech);
        }
        edit.commit();
    }

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
                if (shared.getBoolean("alarm", true)) {
                    if (Build.VERSION.SDK_INT >= 21) {
                        alarm.setAlarmClock(new AlarmManager.AlarmClockInfo(calendar.getTimeInMillis(), pe), pe);
                    } else {
                        alarm.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pe);
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= 23) {
                        alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pe);
                    } else {
                        alarm.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pe);
                    }
                }
            } else {
                alarm.cancel(pe);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        loadAlarms();

        handler = new Handler();
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.US);

                    if (Build.VERSION.SDK_INT >= 21) {
                        tts.setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build());
                    }
                }
            }
        });
    }

    // https://gist.github.com/slightfoot/6330866
    private AudioTrack generateTone(double freqHz, int durationMs) {
        int count = (int) (44100.0 * 2.0 * (durationMs / 1000.0)) & ~1;
        short[] samples = new short[count];
        for (int i = 0; i < count; i += 2) {
            short sample = (short) (Math.sin(2 * Math.PI * i / (44100.0 / freqHz)) * 0x7FFF);
            samples[i + 0] = sample;
            samples[i + 1] = sample;
        }
        AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                count * (Short.SIZE / 8), AudioTrack.MODE_STATIC);
        track.write(samples, 0, count);
        return track;
    }

    public void soundAlarm() {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if (shared.getBoolean("beep", false)) {
            int delay = 100;

            AudioTrack track = generateTone(900, delay);

            if (Build.VERSION.SDK_INT < 21) {
                track.setStereoVolume(getVolume(), getVolume());
            } else {
                track.setVolume(getVolume());
            }

            track.play();

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    playSound();
                }
            }, delay * 2);
        } else {
            playSound();
        }
    }

    float getVolume() {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        float v = (float) (Math.pow(shared.getFloat("volume", 1f), 3));

        return v;
    }

    void playSound() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int min = Calendar.getInstance().get(Calendar.MINUTE);

        String speak;

        if (min != 0) {
            if (min < 10) {
                speak = String.format("Time is %d o %d.", hour, min);
            } else {
                speak = String.format("Time is %d %02d.", hour, min);
            }
        } else
            speak = String.format("%d o'clock", hour);

        Toast.makeText(getApplicationContext(), speak, Toast.LENGTH_SHORT).show();

        if (Build.VERSION.SDK_INT >= 21) {
            Bundle params = new Bundle();
            params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, getVolume());

            tts.speak(speak, TextToSpeech.QUEUE_FLUSH, params, UUID.randomUUID().toString());
        } else {
            HashMap<String, String> params = new HashMap<>();
            params.put(TextToSpeech.Engine.KEY_PARAM_VOLUME, Float.toString(getVolume()));
            tts.speak(speak, TextToSpeech.QUEUE_FLUSH, params);
        }
    }

}
