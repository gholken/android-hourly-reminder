package com.github.axet.hourlyreminder.basics;

import java.util.Calendar;
import java.util.Date;

public class Reminder {
    public int hour;

    public long time;

    public boolean enabled;

    // move alarm to tomorrow
    public void setTomorrow() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.add(Calendar.DATE, 1);
        time = cal.getTimeInMillis();
    }

    public void setNext() {
        Calendar cur = Calendar.getInstance();

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        time = getAlarmTime(cal, cur);
    }

    public int getHour() {
        return hour;
    }

    public long getAlarmTime(Calendar cal, Calendar cur) {
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        if (cal.after(cur)) {
            return cal.getTimeInMillis();
        } else {
            int ch = cur.get(Calendar.HOUR_OF_DAY);
            int rh = cal.get(Calendar.HOUR_OF_DAY);

            if (rh <= ch) {
                // point in past, make it next day
                cal = Calendar.getInstance();
                cal.setTime(cur.getTime());
                cal.set(Calendar.HOUR_OF_DAY, rh);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);

                cal.add(Calendar.DATE, 1);
                return cal.getTimeInMillis();
            } else {
                cal = Calendar.getInstance();
                cal.setTime(cur.getTime());
                cal.set(Calendar.HOUR_OF_DAY, rh);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);

                return cal.getTimeInMillis();
            }
        }
    }
}
