package com.github.axet.hourlyreminder.basics;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Reminder {
    public int hour;
    public int minute;

    public long time;

    public boolean enabled;

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
