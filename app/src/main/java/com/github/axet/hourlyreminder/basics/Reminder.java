package com.github.axet.hourlyreminder.basics;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;

import com.github.axet.hourlyreminder.R;
import com.github.axet.hourlyreminder.app.HourlyApplication;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class Reminder {
    public int hour;
    public int minute;

    public long time;

    public boolean enabled;

    Context context;

    List<Integer> weekDays;

    public Reminder(Context context) {
        this.context = context;

        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        // reminder days
        Set<String> days = shared.getStringSet(HourlyApplication.PREFERENCE_DAYS, new TreeSet<String>());
        weekDays = load(context, days);
    }

    static List<Integer> load(Context context, Set<String> days) {
        // java Calendar.MONDAY... values
        List<Integer> weekDays = new ArrayList<>();
        // convert 'days' -> 'weekDays'
        for (String d : days) {
            for (int i = 0; i < Alarm.DAYS.length; i++) {
                String day = context.getString(Alarm.DAYS[i]);
                if (day.equals(d)) {
                    weekDays.add(Alarm.EVERYDAY[i]);
                }
            }
        }
        return weekDays;
    }

    public static String format(int hour) {
        return String.format("%02d", hour);
    }

    // move alarm to tomorrow
    public void setTomorrow() {
        Calendar cur = Calendar.getInstance();

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.add(Calendar.DATE, 1);

        time = getAlarmTime(cal, cur);
    }

    public boolean isToday() {
        Calendar cur = Calendar.getInstance();

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);

        cal.setTimeInMillis(getAlarmTime(cal, cur));

        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
        return fmt.format(cur.getTime()).equals(fmt.format(cal.getTime()));
    }

    public static boolean isWeek(List<Integer> weekDays, int week) {
        return weekDays.contains(week);
    }

    // check if all 7 days are enabled (mon-sun)
    public static boolean isEveryday(List<Integer> weekDays) {
        for (Integer i : Alarm.EVERYDAY) {
            if (!isWeek(weekDays, i))
                return false;
        }
        return true;
    }

    // check if all 5 days are enabled (mon-fri)
    public static boolean isWeekdays(List<Integer> weekDays) {
        for (Integer i : Alarm.WEEKDAY) {
            if (!isWeek(weekDays, i))
                return false;
        }
        // check all weekend days are disabled
        for (Integer i : Alarm.WEEKEND) {
            if (isWeek(weekDays, i))
                return false;
        }
        return true;
    }

    // check if all 2 week days are enabled (sat, sun)
    public static boolean isWeekend(List<Integer> weekDays) {
        for (Integer i : Alarm.WEEKEND) {
            if (!isWeek(weekDays, i))
                return false;
        }
        // check all weekdays are disabled
        for (Integer i : Alarm.WEEKDAY) {
            if (isWeek(weekDays, i))
                return false;
        }
        return true;
    }

    public static String getDays(Context context, Set<String> values) {
        List<Integer> weekDays = load(context, values);

        if (isEveryday(weekDays)) {
            return context.getString(R.string.Everyday);
        }
        if (isWeekdays(weekDays)) {
            return context.getString(R.string.Weekdays);
        }
        if (isWeekend(weekDays)) {
            return context.getString(R.string.Weekend);
        }
        String str = "";
        for (Integer i : Alarm.order(weekDays)) {
            if (!str.isEmpty())
                str += ", ";
            str += parseConst(context, i);
        }
        if (str.isEmpty())
            str = "No days selected"; // wrong, should not be allowed by UI
        return str;
    }

    public static String parseConst(Context context, int c) {
        for (int i = 0; i < Alarm.EVERYDAY.length; i++) {
            if (Alarm.EVERYDAY[i] == c) {
                return context.getString(Alarm.DAYS[i]);
            }
        }
        throw new RuntimeException("wrong day");
    }

    public void setNext() {
        Calendar cur = Calendar.getInstance();

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        time = getAlarmTime(cal, cur);
    }

    public int getHour() {
        return hour;
    }

    public Calendar rollWeek(Calendar cal) {
        long init = cal.getTimeInMillis();

        // check if alarm is active for current weekday. skip all disabled weekdays.
        int week = cal.get(Calendar.DAY_OF_WEEK);
        int i;
        for (i = 0; i < 7; i++) {
            // check week enabled?
            if (isWeek(weekDays, week))
                break;
            // no, skip a day.
            cal.add(Calendar.DATE, 1);
            week = cal.get(Calendar.DAY_OF_WEEK);
        }
        if (i == 7) {
            // no weekday enabled. reset. use initial time, as if here were no weekdays checkbox enabled
            cal.setTimeInMillis(init);
        }
        return cal;
    }

    public long getAlarmTime(Calendar cal, Calendar cur) {
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        if (cal.after(cur)) {
            // time is future? then it points for correct time.
            // change nothing, but seconds.
            return cal.getTimeInMillis();
        } else {
            int ch = cur.get(Calendar.HOUR_OF_DAY);
            int cm = cur.get(Calendar.MINUTE);

            int ah = cal.get(Calendar.HOUR_OF_DAY);
            int am = cal.get(Calendar.MINUTE);

            if ((ah < ch) || ((ah == ch) && (am <= cm))) {
                // if it too late to play, point to for tomorrow
                cal = Calendar.getInstance();
                cal.setTime(cur.getTime());
                cal.set(Calendar.HOUR_OF_DAY, ah);
                cal.set(Calendar.MINUTE, am);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);

                cal.add(Calendar.DATE, 1);
                cal = rollWeek(cal);
                return cal.getTimeInMillis();
            } else {
                // it is today alarm, fix day
                cal = Calendar.getInstance();
                cal.setTime(cur.getTime());
                cal.set(Calendar.HOUR_OF_DAY, ah);
                cal.set(Calendar.MINUTE, am);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                cal = rollWeek(cal);
                return cal.getTimeInMillis();
            }
        }
    }
}
