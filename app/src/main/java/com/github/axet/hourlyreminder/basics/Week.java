package com.github.axet.hourlyreminder.basics;

import android.content.Context;

import com.github.axet.hourlyreminder.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class Week {
    // days <-> java index converter
    //
    // keep EVERYDAY order
    public final static int[] DAYS = new int[]{R.string.WEEK_MON, R.string.WEEK_TUE, R.string.WEEK_WED, R.string.WEEK_THU, R.string.WEEK_FRI, R.string.WEEK_SAT, R.string.WEEK_SUN};

    public final static String[] DAYS_VALUES = new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    public final static Integer[] EVERYDAY = new Integer[]{Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY};

    public final static Integer[] WEEKDAY = new Integer[]{Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY};

    public final static Integer[] WEEKEND = new Integer[]{Calendar.SATURDAY, Calendar.SUNDAY};

    public Context context;

    // alarm on selected weekdays only
    public boolean weekdaysCheck;
    // weekday values
    public List<Integer> weekDaysValues;
    // actual alarm go time.
    //
    // (may be incorrect if user moved from one time zone to anoter)
    public long time;

    public Week(Context context) {
        this.context = context;
    }

    // keep proper order week days
    //
    // should take ordering values from settings (sun or mon first).
    public static List<Integer> order(List<Integer> list) {
        ArrayList<Integer> l = new ArrayList<>();
        for (int i = 0; i < EVERYDAY.length; i++) {
            int w = EVERYDAY[i];
            if (list.contains(w))
                l.add(w);
        }
        return l;
    }

    public static String parseConst(Context context, int c) {
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

    public Set<String> getWeekDaysProperty() {
        TreeSet<String> set = new TreeSet<>();
        for (Integer w : weekDaysValues) {
            set.add(w.toString());
        }
        return set;
    }

    // "Thu" -> (int)Calendar.Thursday
    int parseTag(String d) {
        for (int i = 0; i < Week.DAYS.length; i++) {
            String day = DAYS_VALUES[i];
            if (day.equals(d)) {
                return Week.EVERYDAY[i];
            }
        }
        throw new RuntimeException("unknown day");
    }

    public void setWeekDaysProperty(Set<String> set) {
        ArrayList w = new ArrayList<>();
        for (String s : set) {
            int i;

            try {
                i = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                i = parseTag(s);
            }

            w.add(i);
        }
        weekDaysValues = w;
    }

    public void setWeekDaysValues(Integer[] set) {
        ArrayList w = new ArrayList<>();
        for (Integer s : set) {
            w.add(s);
        }
        weekDaysValues = w;
    }

    public boolean noDays() {
        return weekDaysValues.isEmpty();
    }

    public void setEveryday() {
        for (int w : Week.EVERYDAY) {
            weekDaysValues.add(w);
        }
    }

    public boolean isWeek(int week) {
        return weekDaysValues.contains(week);
    }

    // check if all 7 days are enabled (mon-sun)
    public boolean isEveryday(List<Integer> weekDays) {
        for (Integer i : Week.EVERYDAY) {
            if (!isWeek(i))
                return false;
        }
        return true;
    }

    // check if all 5 days are enabled (mon-fri)
    public boolean isWeekdays(List<Integer> weekDays) {
        for (Integer i : Week.WEEKDAY) {
            if (!isWeek(i))
                return false;
        }
        // check all weekend days are disabled
        for (Integer i : Week.WEEKEND) {
            if (isWeek(i))
                return false;
        }
        return true;
    }

    // check if all 2 week days are enabled (sat, sun)
    public boolean isWeekend(List<Integer> weekDays) {
        for (Integer i : Week.WEEKEND) {
            if (!isWeek(i))
                return false;
        }
        // check all weekdays are disabled
        for (Integer i : Week.WEEKDAY) {
            if (isWeek(i))
                return false;
        }
        return true;
    }

    public String getDays() {
        if (isEveryday(weekDaysValues)) {
            return context.getString(R.string.Everyday);
        }
        if (isWeekdays(weekDaysValues)) {
            return context.getString(R.string.Weekdays);
        }
        if (isWeekend(weekDaysValues)) {
            return context.getString(R.string.Weekend);
        }
        String str = "";
        for (Integer i : Week.order(weekDaysValues)) {
            if (!str.isEmpty())
                str += ", ";
            str += Week.parseConst(context, i);
        }
        if (str.isEmpty())
            str = "No days selected"; // wrong, should not be allowed by UI
        return str;
    }

    public Calendar rollWeek(Calendar cal) {
        long init = cal.getTimeInMillis();

        // check if alarm is active for current weekday. skip all disabled weekdays.
        int week = cal.get(Calendar.DAY_OF_WEEK);
        int i;
        for (i = 0; i < EVERYDAY.length; i++) {
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

    public void setWeek(int week, boolean b) {
        weekDaysValues.remove(new Integer(week));
        if (b) {
            weekDaysValues.add(week);
        }
        if (noDays()) {
            weekdaysCheck = false;
        }
        setNext();
    }

    public long getTime() {
        return time;
    }

    public void setTime(long l) {
        this.time = l;
    }

    // set today alarm
    public void setTime(int hour, int min) {
        Calendar cur = Calendar.getInstance();

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, min);

        this.time = cal.getTimeInMillis();

        time = getAlarmTime(cal, cur);
    }

    // move alarm to the next day (tomorrow)
    //
    // (including weekdays checks)
    public void setTomorrow() {
        Calendar cur = Calendar.getInstance();

        Calendar m = Calendar.getInstance();
        m.setTimeInMillis(time);
        int hour = m.get(Calendar.HOUR_OF_DAY);
        int min = m.get(Calendar.MINUTE);

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
        Calendar cur = Calendar.getInstance();

        Calendar m = Calendar.getInstance();
        m.setTimeInMillis(time);
        int hour = m.get(Calendar.HOUR_OF_DAY);
        int min = m.get(Calendar.MINUTE);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, min);

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

    // get time for Alarm Manager
    public long getAlarmTime(Calendar cal, Calendar cur) {
        if (weekdaysCheck) {
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
