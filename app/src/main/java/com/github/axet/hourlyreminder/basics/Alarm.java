package com.github.axet.hourlyreminder.basics;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.RingtoneManager;
import android.net.Uri;
import android.text.format.DateFormat;

import com.github.axet.hourlyreminder.app.HourlyApplication;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public class Alarm extends Week {
    public final static Uri DEFAULT_RING = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

    // unique id
    public long id;
    // enabled?
    public boolean enable;
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
            int c = new Integer(o1.getHour()).compareTo(o2.getHour());
            if (c != 0)
                return c;
            return new Integer(o1.getMin()).compareTo(o2.getMin());
        }
    }

    public Alarm(Alarm copy) {
        super(copy.context);

        id = copy.id;
        time = copy.time;
        enable = copy.enable;
        weekdaysCheck = copy.weekdaysCheck;
        weekDaysValues = new ArrayList<>(copy.weekDaysValues);
        ringtone = copy.ringtone;
        ringtoneValue = copy.ringtoneValue;
        beep = copy.beep;
        speech = copy.speech;
    }

    public Alarm(Context context) {
        super(context);

        this.id = System.currentTimeMillis();

        enable = false;
        weekdaysCheck = true;
        weekDaysValues = new ArrayList<>(Arrays.asList(Week.EVERYDAY));
        ringtone = false;
        beep = false;
        speech = true;
        ringtoneValue = DEFAULT_RING.toString();

        setTime(9, 0);
    }

    public Alarm(Context context, String json) {
        this(context);
        try {
            JSONObject o = new JSONObject(json);
            Alarm a = this;
            a.id = o.getLong("id");
            a.time = o.getLong("time");
            a.enable = o.getBoolean("enable");
            a.weekdaysCheck = o.getBoolean("weekdays");
            a.setWeekDaysProperty(o.getJSONArray("weekdays_values"));
            a.ringtone = o.getBoolean("ringtone");
            a.ringtoneValue = o.getString("ringtone_value");
            a.beep = o.getBoolean("beep");
            a.speech = o.getBoolean("speech");
        }catch(JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public Alarm(Context context, long time) {
        this(context);

        this.enable = true;
        this.beep = true;
        this.weekdaysCheck = false;
        this.ringtone = true;


        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int min = cal.get(Calendar.MINUTE);

        setTime(hour, min);
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
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(time);
            int hour = cal.get(Calendar.HOUR_OF_DAY);

            Resources res = context.getResources();
            Configuration conf = res.getConfiguration();
            Locale locale = conf.locale;

            SimpleDateFormat f = new SimpleDateFormat("h:mm");
            return f.format(new Date(time)) + " " + HourlyApplication.getHourString(context, locale, hour);
        }
    }

    public String format() {
        return format(context, time);
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
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        return hour;
    }

    public int getMin() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        int min = cal.get(Calendar.MINUTE);
        return min;
    }

    public String save() {
        try {
            Alarm a = this;
            JSONObject o = new JSONObject();
            o.put("id", a.id);
            o.put("time", a.time);
            o.put("enable", a.enable);
            o.put("weekdays", a.weekdaysCheck);
            o.put("weekdays_values", new JSONArray(a.getWeekDaysProperty()));
            o.put("ringtone", a.ringtone);
            o.put("ringtone_value", a.ringtoneValue);
            o.put("beep", a.beep);
            o.put("speech", a.speech);
            return o.toString();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

}
