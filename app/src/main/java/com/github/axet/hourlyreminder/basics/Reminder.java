package com.github.axet.hourlyreminder.basics;

import java.util.Calendar;
import java.util.Date;

public class Reminder {
    // time when alarm start to be active. used to snooze upcoming today alarms.
    //
    // may point in past or future. if it points to the past - it is currently active.
    // if it points to tomorrow or more days - do not send it to Alarm Manager until it is active.
    //
    // holds current hour and minute as part of active time.
    public long time;

    // move alarm to tomorrow
    public void setTomorrow() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        int rh = cal.get(Calendar.HOUR_OF_DAY);

        cal.setTime(new Date());
        cal.add(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, rh);
        this.time = cal.getTimeInMillis();
    }

    public int getHour() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);

        int rh = cal.get(Calendar.HOUR_OF_DAY);
        return rh;
    }

    public long getAlarmTime(Calendar cur) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);

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
