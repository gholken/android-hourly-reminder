package com.github.axet.hourlyreminder;

import android.content.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class Alarm {
    public long time;
    public boolean enable;
    public boolean weekdays;
    public List<Integer> weekdaysValues;
    public boolean ringtone;
    public String ringtoneValue;
    public boolean beep;
    public boolean speech;

    public Alarm() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 9);
        c.set(Calendar.MINUTE, 0);
        time = c.getTimeInMillis();

        enable = false;
        weekdays = true;
        weekdaysValues = new ArrayList<Integer>(Arrays.asList(new Integer[]{0, 1, 2, 3, 4, 5, 6}));
        ringtone = false;
        beep = false;
        speech = true;
        ringtoneValue = "Default Ringtone";
    }

    public Alarm(long time) {
        this();
        Calendar c = Calendar.getInstance();
        c.setTime(new Date(time));
        this.time = c.getTimeInMillis();
        this.enable = true;
    }

    public String getTime() {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date(time));

        int hour = c.get(Calendar.HOUR_OF_DAY);
        int min = c.get(Calendar.MINUTE);

        return String.format("%02d:%02d", hour, min);
    }

    public Set<String> getWeekDays() {
        TreeSet<String> set = new TreeSet<>();
        for (Integer w : weekdaysValues) {
            set.add(w.toString());
        }
        return set;
    }

    public void setWeekDays(Set<String> set) {
        ArrayList w = new ArrayList<>();
        for (String s : set) {
            w.add(Integer.parseInt(s));
        }
        weekdaysValues = w;
    }

    public void setWeek(int week, boolean b) {
        weekdaysValues.remove(new Integer(week));
        if (b)
            weekdaysValues.add(week);
    }

    public boolean isWeek(int week) {
        for (Integer i : weekdaysValues) {
            if (i == week)
                return true;
        }

        return false;
    }

    public String getDays(Context context) {
        if (weekdays) {
            String str = "";
            String[] ww = context.getResources().getStringArray(R.array.weekdays_short);
            for (Integer i : weekdaysValues) {
                if (!str.isEmpty())
                    str += ", ";
                str += ww[i];
            }
            if (str.isEmpty())
                str = "No days selected";
            return str;
        } else {
            if (isToday()) {
                return "Today";
            } else {
                return "Tomorrow";
            }
        }
    }

    public boolean isToday() {
        Calendar cal = Calendar.getInstance();

        cal.setTime(new Date(System.currentTimeMillis()));
        int ch = cal.get(Calendar.HOUR_OF_DAY);
        int cm = cal.get(Calendar.MINUTE);

        cal.setTime(new Date(time));
        int ah = cal.get(Calendar.HOUR_OF_DAY);
        int am = cal.get(Calendar.MINUTE);

        if (ah < ch || (ah == ch && am <= cm)) {
            return false;
        } else {
            return true;
        }
    }
}
