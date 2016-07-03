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

        load(days);
    }

    public Reminder(Context context, Set days) {
        super(context);

        load(days);
   }

    // ["Mon", "Tru"] --> [Calendar.Monday, Calendar.Thusday]
    public void load(Set<String> days) {
        weekdaysCheck = true;
        // java Calendar.MONDAY... values
        weekDaysValues = new ArrayList<>();
        // convert 'days' -> 'weekDays'
        for (String d : days) {
            for (int i = 0; i < Week.DAYS.length; i++) {
                String day = context.getString(Week.DAYS[i]);
                if (day.equals(d)) {
                    weekDaysValues.add(Week.EVERYDAY[i]);
                }
            }
        }
    }

    public static String format(int hour) {
        return String.format("%02d", hour);
    }
}
