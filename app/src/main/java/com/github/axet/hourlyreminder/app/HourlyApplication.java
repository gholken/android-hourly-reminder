package com.github.axet.hourlyreminder.app;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.github.axet.hourlyreminder.R;
import com.github.axet.hourlyreminder.basics.Alarm;
import com.github.axet.hourlyreminder.basics.Reminder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class HourlyApplication extends Application {
    public static final int NOTIFICATION_UPCOMING_ICON = 0;
    public static final int NOTIFICATION_ALARM_ICON = 1;
    public static final int NOTIFICATION_MISSED_ICON = 2;

    public static final String PREFERENCE_ENABLED = "enabled";
    public static final String PREFERENCE_HOURS = "hours";
    public static final String PREFERENCE_ALARM = "alarm";
    public static final String PREFERENCE_ALARMS_PREFIX = "Alarm_";
    public static final String PREFERENCE_BEEP = "beep";
    public static final String PREFERENCE_VOLUME = "volume";

    public static List<Alarm> loadAlarms(Context context) {
        ArrayList<Alarm> alarms = new ArrayList<>();

        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        int c = shared.getInt(PREFERENCE_ALARMS_PREFIX + "Count", 0);
        if (c == 0) {
            Alarm a;
            a = new Alarm(context);
            a.setTime(9, 0);
            a.weekdays = true;
            a.speech = true;
            a.beep = true;
            a.ringtone = true;
            a.setWeekDays(Alarm.WEEKDAY);
            alarms.add(a);

            a = new Alarm(context);
            a.setTime(10, 0);
            a.weekdays = true;
            a.speech = true;
            a.beep = true;
            a.ringtone = true;
            a.setWeekDays(Alarm.WEEKEND);
            alarms.add(a);

            a = new Alarm(context);
            a.setTime(10, 30);
            a.weekdays = false;
            a.speech = true;
            a.beep = true;
            a.ringtone = true;
            alarms.add(a);
        }

        long id = 0;

        for (int i = 0; i < c; i++) {
            String prefix = PREFERENCE_ALARMS_PREFIX + i + "_";
            Alarm a = new Alarm(context);
            a.id = shared.getLong(prefix + "Id", System.currentTimeMillis());

            while (a.id == id) {
                a.id++;
            }
            id = a.id;

            a.time = shared.getLong(prefix + "Time", 0);

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(a.time);

            a.hour = shared.getInt(prefix + "Hour", cal.get(Calendar.HOUR_OF_DAY));
            a.min = shared.getInt(prefix + "Min", cal.get(Calendar.MINUTE));
            a.enable = shared.getBoolean(prefix + "Enable", false);
            a.weekdays = shared.getBoolean(prefix + "WeekDays", false);
            a.setWeekDaysProperty(shared.getStringSet(prefix + "WeekDays_Values", null));
            a.ringtone = shared.getBoolean(prefix + "Ringtone", false);
            a.ringtoneValue = shared.getString(prefix + "Ringtone_Value", "");
            a.beep = shared.getBoolean(prefix + "Beep", false);
            a.speech = shared.getBoolean(prefix + "Speech", false);
            alarms.add(a);
        }

        return alarms;
    }

    public static void saveAlarms(Context context, List<Alarm> alarms) {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = shared.edit();
        edit.putInt(PREFERENCE_ALARMS_PREFIX + "Count", alarms.size());

        long id = 0;

        for (int i = 0; i < alarms.size(); i++) {
            Alarm a = alarms.get(i);
            String prefix = PREFERENCE_ALARMS_PREFIX + i + "_";

            while (a.id == id) {
                a.id++;
            }
            id = a.id;

            edit.putLong(prefix + "Id", a.id);
            edit.putInt(prefix + "Hour", a.hour);
            edit.putInt(prefix + "Min", a.min);
            edit.putLong(prefix + "Time", a.time);
            edit.putBoolean(prefix + "Enable", a.enable);
            edit.putBoolean(prefix + "WeekDays", a.weekdays);
            edit.putStringSet(prefix + "WeekDays_Values", a.getWeekDaysProperty());
            edit.putBoolean(prefix + "Ringtone", a.ringtone);
            edit.putString(prefix + "Ringtone_Value", a.ringtoneValue);
            edit.putBoolean(prefix + "Beep", a.beep);
            edit.putBoolean(prefix + "Speech", a.speech);
        }
        edit.commit();
    }

    public static List<Reminder> loadReminders(Context context) {
        ArrayList<Reminder> list = new ArrayList<>();

        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> hours = shared.getStringSet(PREFERENCE_HOURS, new HashSet<String>());

        for (int i = 0; i < 24; i++) {
            String h = Reminder.format(i);

            Reminder r = new Reminder();
            r.hour = i;
            r.enabled = hours.contains(h);
            r.setNext();
            list.add(r);
        }

        return list;
    }

    public static void toastAlarmSet(Context context, Alarm a) {
        if (!a.enable) {
            Toast.makeText(context, context.getString(R.string.alarm_disabled), Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar cur = Calendar.getInstance();

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(a.time);

        long diff = cal.getTimeInMillis() - cur.getTimeInMillis();

        int diffSeconds = (int) (diff / 1000 % 60);
        int diffMinutes = (int) (diff / (60 * 1000) % 60);
        int diffHours = (int) (diff / (60 * 60 * 1000) % 24);
        int diffDays = (int) (diff / (24 * 60 * 60 * 1000));

        String str = "";

        if (diffDays > 0)
            str += " " + context.getResources().getQuantityString(R.plurals.days, diffDays, diffDays);

        if (diffHours > 0)
            str += " " + context.getResources().getQuantityString(R.plurals.hours, diffHours, diffHours);

        if (diffMinutes > 0)
            str += " " + context.getResources().getQuantityString(R.plurals.minutes, diffMinutes, diffMinutes);

        Toast.makeText(context, context.getString(R.string.alarm_set_for, str), Toast.LENGTH_SHORT).show();
    }

    public static String getHoursString(Context context, List<String> hours) {
        String str = "";

        Collections.sort(hours);

        int prev = -2;
        int count = 0;
        for (String s : hours) {
            int i = Integer.parseInt(s);
            if (i == prev + count + 1) {
                count++;
            } else {
                if (count != 0) {
                    if (count == 1)
                        str += ",";
                    else
                        str += "-";
                    str += Reminder.format(prev + count);
                    str += "," + s;
                } else {
                    if (!str.isEmpty())
                        str += ",";
                    str += s;
                }

                prev = i;
                count = 0;
            }
        }

        if (count != 0) {
            str += "-";
            str += Reminder.format(prev + count);
        }

        if (!str.isEmpty())
            str += context.getString(R.string.hour_symbol);

        return str;
    }

}
