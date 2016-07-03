package com.github.axet.hourlyreminder.basics;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;

import com.github.axet.hourlyreminder.app.HourlyApplication;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class Reminder extends Week {
    public boolean enabled;

    public Reminder(Context context) {
        super(context);

        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        // reminder days
        Set<String> days = shared.getStringSet(HourlyApplication.PREFERENCE_DAYS, new TreeSet<String>());

        weekdaysCheck = true;
        setWeekDaysProperty(days);
    }

    public Reminder(Context context, Set days) {
        super(context);

        weekdaysCheck = true;
        setWeekDaysProperty(days);
    }

    public static String format(int hour) {
        return String.format("%02d", hour);
    }
}
