package com.github.axet.hourlyreminder.basics;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spanned;
import android.text.format.DateFormat;

import com.github.axet.hourlyreminder.R;
import com.github.axet.hourlyreminder.app.HourlyApplication;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class Alarm {
    public final static Uri DEFAULT_RING = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

    // days <-> java index converter
    //
    // keep EVERYDAY order
    public final static int[] DAYS = new int[]{R.string.WEEK_MON, R.string.WEEK_TUE, R.string.WEEK_WED, R.string.WEEK_THU, R.string.WEEK_FRI, R.string.WEEK_SAT, R.string.WEEK_SUN};

    public final static Integer[] EVERYDAY = new Integer[]{Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY};

    public final static Integer[] WEEKDAY = new Integer[]{Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY};

    public final static Integer[] WEEKEND = new Integer[]{Calendar.SATURDAY, Calendar.SUNDAY};

    protected Context context;

    // unique id
    public long id;
    // actual alarm go time.
    //
    // (may be incorrect if user moved from one time zone to anoter)
    public long time;
    // hour alarm set for
    public int hour;
    // min alarm set for
    public int min;
    // enabled?
    public boolean enable;
    // alarm on selected weekdays only
    public boolean weekdays;
    // weekdays values
    public List<Integer> weekdaysValues;
    // alarm with ringtone?
    public boolean ringtone;
    // uri or file
    public String ringtoneValue;
    // beep?
    public boolean beep;
    // speech time?
    public boolean speech;

    public static class CustomComparator implements Comparator<Alarm> {
        @Override
        public int compare(Alarm o1, Alarm o2) {
            int c = new Integer(o1.hour).compareTo(o2.hour);
            if (c != 0)
                return c;
            return new Integer(o1.min).compareTo(o2.min);
        }
    }

    public Alarm(Alarm copy) {
        context = copy.context;
        id = copy.id;
        time = copy.time;
        hour = copy.hour;
        min = copy.min;
        enable = copy.enable;
        weekdays = copy.weekdays;
        weekdaysValues = new ArrayList<Integer>(copy.weekdaysValues);
        ringtone = copy.ringtone;
        ringtoneValue = copy.ringtoneValue;
        beep = copy.beep;
        speech = copy.speech;
    }

    public Alarm(Context context) {
        this.id = System.currentTimeMillis();

        this.context = context;

        enable = false;
        weekdays = true;
        weekdaysValues = new ArrayList<Integer>(Arrays.asList(EVERYDAY));
        ringtone = false;
        beep = false;
        speech = true;
        ringtoneValue = DEFAULT_RING.toString();

        setTime(9, 0);
    }

    public Alarm(Context context, long time) {
        this(context);

        this.enable = true;
        this.beep = true;
        this.weekdays = false;
        this.ringtone = true;


        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int min = cal.get(Calendar.MINUTE);

        setTime(hour, min);
    }

    // keep proper order week days
    //
    // should take ordering values from settings (sun or mon first).
    public List<Integer> order(List<Integer> list) {
        ArrayList<Integer> l = new ArrayList<>();
        for (int i = 0; i < EVERYDAY.length; i++) {
            int w = EVERYDAY[i];
            if (list.contains(w))
                l.add(w);
        }
        return l;
    }

    public String parseConst(int c) {
        for (int i = 0; i < EVERYDAY.length; i++) {
            if (EVERYDAY[i] == c) {
                return context.getString(DAYS[i]);
            }
        }
        throw new RuntimeException("wrong day");
    }

    public int parseTag(Object o) {
        Integer s = (Integer) o;
        for (int i = 0; i < DAYS.length; i++) {
            if (s == DAYS[i])
                return EVERYDAY[i];
        }

        throw new RuntimeException("bad week");
    }

    public static String format(long time) {
        SimpleDateFormat f = new SimpleDateFormat("HH:mm");
        return f.format(new Date(time));
    }

    public static String format(Context context, long time) {
        if (DateFormat.is24HourFormat(context)) {
            SimpleDateFormat f = new SimpleDateFormat("HH:mm");
            return f.format(new Date(time));
        } else {
            SimpleDateFormat f = new SimpleDateFormat("h:mm a");
            return f.format(new Date(time));
        }
    }

    public String format() {
        return format(context, time);
    }

    public long getTime() {
        return time;
    }

    public void setTime(long l) {
        this.time = l;
    }

    // set today alarm
    public void setTime(int hour, int min) {
        this.hour = hour;
        this.min = min;
        setNext();
    }

    public void setEnable(boolean e) {
        this.enable = e;
        if (e)
            setNext();
    }

    public boolean getEnable() {
        return enable;
    }

    public int getHour() {
        return hour;
    }

    public int getMin() {
        return min;
    }

    public Set<String> getWeekDaysProperty() {
        TreeSet<String> set = new TreeSet<>();
        for (Integer w : weekdaysValues) {
            set.add(w.toString());
        }
        return set;
    }

    public void setWeekDaysProperty(Set<String> set) {
        ArrayList w = new ArrayList<>();
        for (String s : set) {
            w.add(Integer.parseInt(s));
        }
        weekdaysValues = w;
    }

    public void setWeekDays(Integer[] set) {
        ArrayList w = new ArrayList<>();
        for (Integer s : set) {
            w.add(s);
        }
        weekdaysValues = w;
    }

    public void setWeek(int week, boolean b) {
        weekdaysValues.remove(new Integer(week));
        if (b) {
            weekdaysValues.add(week);
        }
        if (noDays()) {
            weekdays = false;
        }
        setNext();
    }

    // check if 'week' in weekdays when alarm goes off
    public boolean isWeek(int week) {
        for (Integer i : weekdaysValues) {
            if (i == week)
                return true;
        }
        return false;
    }

    // check if all 7 days are enabled (mon-sun)
    public boolean isEveryday() {
        for (Integer i : EVERYDAY) {
            if (!isWeek(i))
                return false;
        }
        return true;
    }

    public boolean noDays() {
        return weekdaysValues.isEmpty();
    }

    public void setEveryday() {
        for (int w : EVERYDAY) {
            weekdaysValues.add(w);
        }
    }


    // check if all 5 days are enabled (mon-fri)
    public boolean isWeekdays() {
        for (Integer i : WEEKDAY) {
            if (!isWeek(i))
                return false;
        }
        // check all weekend days are disabled
        for (Integer i : WEEKEND) {
            if (isWeek(i))
                return false;
        }
        return true;
    }

    // check if all 2 week days are enabled (sat, sun)
    public boolean isWeekend() {
        for (Integer i : WEEKEND) {
            if (!isWeek(i))
                return false;
        }
        // check all weekdays are disabled
        for (Integer i : WEEKDAY) {
            if (isWeek(i))
                return false;
        }
        return true;
    }

    public String getDays() {
        if (weekdays) {
            if (isEveryday()) {
                return "Everyday";
            }
            if (isWeekdays()) {
                return "Weekdays";
            }
            if (isWeekend()) {
                return "Weekend";
            }
            String str = "";
            for (Integer i : order(weekdaysValues)) {
                if (!str.isEmpty())
                    str += ", ";
                str += parseConst(i);
            }
            if (str.isEmpty())
                str = "No days selected"; // wrong, should not be allowed by UI
            return str;
        } else {
            if (isToday()) {
                return "Today";
            } else {
                return "Tomorrow";
            }
        }
    }

    // move alarm to the next day (tomorrow)
    //
    // (including weekdays checks)
    public void setTomorrow() {
        Calendar cur = Calendar.getInstance();

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, min);
        cal.add(Calendar.DATE, 1);

        time = getAlarmTime(cal, cur);
    }

    // set alarm to go off next possible time
    //
    // today or tomorrow (including weekday checks)
    public void setNext() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, min);

        Calendar cur = Calendar.getInstance();

        time = getAlarmTime(cal, cur);
    }

    // If alarm time > current time == tomorrow. Or compare hours.
    public boolean isToday() {
        Calendar cur = Calendar.getInstance();

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);

        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
        return fmt.format(cur.getTime()).equals(fmt.format(cal.getTime()));
    }

    public boolean isTomorrow() {
        Calendar cur = Calendar.getInstance();
        cur.add(Calendar.DATE, 1);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);

        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
        return fmt.format(cur.getTime()).equals(fmt.format(cal.getTime()));
    }

    public Calendar rollWeek(Calendar cal) {
        long init = cal.getTimeInMillis();

        // check if alarm is active for current weekday. skip all disabled weekdays.
        int week = cal.get(Calendar.DAY_OF_WEEK);
        int i;
        for (i = 0; i < 7; i++) {
            // check week enabled?
            if (isWeek(week))
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

    // get time for Alarm Manager
    public long getAlarmTime(Calendar cal, Calendar cur) {
        if (weekdays) {
            cal = rollWeek(cal);
        }

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
                return cal.getTimeInMillis();
            }
        }
    }
}
