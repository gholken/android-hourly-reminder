package com.github.axet.hourlyreminder.app;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.SharedPreferencesCompat;
import android.support.v7.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.widget.Toast;

import com.github.axet.androidlibrary.widgets.ThemeUtils;
import com.github.axet.hourlyreminder.R;
import com.github.axet.hourlyreminder.basics.Alarm;
import com.github.axet.hourlyreminder.basics.Reminder;
import com.github.axet.hourlyreminder.basics.Week;
import com.github.axet.hourlyreminder.services.AlarmService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public class HourlyApplication extends Application {
    public static final int NOTIFICATION_UPCOMING_ICON = 0;
    public static final int NOTIFICATION_ALARM_ICON = 1;
    public static final int NOTIFICATION_MISSED_ICON = 2;

    public static final String PREFERENCE_VERSION = "version";

    public static final String PREFERENCE_ENABLED = "enabled";
    public static final String PREFERENCE_HOURS = "hours";
    public static final String PREFERENCE_DAYS = "weekdays";
    public static final String PREFERENCE_REPEAT = "repeat";
    public static final String PREFERENCE_ALARM = "alarm";
    public static final String PREFERENCE_ALARMS_PREFIX = "alarm_";

    public static final String PREFERENCE_BEEP_CUSTOM = "beep_custom";

    public static final String PREFERENCE_BEEP = "beep";
    public static final String PREFERENCE_CUSTOM_SOUND = "custom_sound";
    public static final String PREFERENCE_CUSTOM_SOUND_OFF = "off";
    public static final String PREFERENCE_RINGTONE = "ringtone";
    public static final String PREFERENCE_SOUND = "sound";
    public static final String PREFERENCE_SPEAK = "speak";

    public static final String PREFERENCE_VOLUME = "volume";
    public static final String PREFERENCE_INCREASE_VOLUME = "increasing_volume";
    public static final String PREFERENCE_NOTIFICATIONS = "notifications";

    public static final String PREFERENCE_THEME = "theme";
    public static final String PREFERENCE_SPEAK_AMPM = "speak_ampm";

    public static final String PREFERENCE_MUSICSILENCE = "musicsilence";
    public static final String PREFERENCE_CALLSILENCE = "callsilence";
    public static final String PREFERENCE_WEEKSTART = "weekstart";

    public static final String PREFERENCE_VIBRATE = "vibrate";

    public static final String PREFERENCE_LAST_PATH = "lastpath";

    public static final String PREFERENCE_LANGUAGE = "language";

    public static final String PREFERENCE_ACTIVE_ALARM= "active_alarm";


    static HashMap<Uri, String> titles = new HashMap<>();

    public static final int VERSION = 1;

    @Override
    public void onCreate() {
        super.onCreate();

        setTheme(getUserTheme());

        SharedPreferences defaultValueSp = getSharedPreferences("_has_set_default_values", 0);
        if (!defaultValueSp.getBoolean("_has_set_default_values", false)) {
            PreferenceManager.setDefaultValues(this, R.xml.pref_reminders, true);
            PreferenceManager.setDefaultValues(this, R.xml.pref_settings, true);
            SharedPreferences.Editor editor = defaultValueSp.edit().putBoolean("_has_set_default_values", true);
            SharedPreferencesCompat.EditorCompat.getInstance().apply(editor);
        }

        // version settings upgrade
        {
            SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);
            int ver = shared.getInt(PREFERENCE_VERSION, 0);
            SharedPreferences.Editor edit = shared.edit();
            switch (ver) {
            }
            edit.putInt(PREFERENCE_VERSION, VERSION);
            edit.commit();
        }
    }

    public static int getActionbarColor(Context context) {
        int colorId = getTheme(context, R.attr.colorPrimary, R.color.actionBarBackgroundDark);
        int color = ThemeUtils.getThemeColor(context, colorId);
        return color;
    }

    public static List<Alarm> loadAlarms(Context context) {
        ArrayList<Alarm> alarms = new ArrayList<>();

        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        int c = shared.getInt(PREFERENCE_ALARMS_PREFIX + "count", -1);
        if (c == -1) // <=1.4.4
            c = shared.getInt("Alarm_" + "Count", 0);
        if (c == 0) { // default alarms list
            Set<Long> ids = new TreeSet<>();

            Alarm a;
            a = new Alarm(context);
            a.setTime(9, 0);
            a.weekdaysCheck = true;
            a.speech = true;
            a.beep = true;
            a.ringtone = true;
            a.setWeekDaysValues(Week.WEEKDAY);
            alarms.add(a);
            while (ids.contains(a.id)) {
                a.id++;
            }
            ids.add(a.id);

            a = new Alarm(context);
            a.setTime(10, 0);
            a.weekdaysCheck = true;
            a.speech = true;
            a.beep = true;
            a.ringtone = true;
            a.setWeekDaysValues(Week.WEEKEND);
            alarms.add(a);
            while (ids.contains(a.id)) {
                a.id++;
            }
            ids.add(a.id);

            a = new Alarm(context);
            a.setTime(10, 30);
            a.weekdaysCheck = false;
            a.speech = true;
            a.beep = true;
            a.ringtone = true;
            alarms.add(a);
            while (ids.contains(a.id)) {
                a.id++;
            }
            ids.add(a.id);
        }

        Set<Long> ids = new TreeSet<>();

        for (int i = 0; i < c; i++) {
            try {
                String json = shared.getString(PREFERENCE_ALARMS_PREFIX + i, "");
                if (json.isEmpty()) { // <=1.4.4
                    JSONObject o = new JSONObject();
                    String prefix = "Alarm_" + i + "_";
                    o.put("id", shared.getLong(prefix + "Id", System.currentTimeMillis()));
                    o.put("time", shared.getLong(prefix + "Time", 0));
                    o.put("enable", shared.getBoolean(prefix + "Enable", false));
                    o.put("weekdays", shared.getBoolean(prefix + "WeekDays", false));
                    o.put("weekdays_values", new JSONArray(shared.getStringSet(prefix + "WeekDays_Values", null)));
                    o.put("ringtone", shared.getBoolean(prefix + "Ringtone", false));
                    o.put("ringtone_value", shared.getString(prefix + "Ringtone_Value", ""));
                    o.put("beep", shared.getBoolean(prefix + "Beep", false));
                    o.put("speech", shared.getBoolean(prefix + "Speech", false));
                    json = o.toString();
                }
                Alarm a = new Alarm(context, json);

                while (ids.contains(a.id)) {
                    a.id++;
                }
                ids.add(a.id);

                alarms.add(a);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        return alarms;
    }

    public static void saveAlarms(Context context, List<Alarm> alarms) {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = shared.edit();
        edit.putInt(PREFERENCE_ALARMS_PREFIX + "count", alarms.size());

        Set<Long> ids = new TreeSet<>();

        for (int i = 0; i < alarms.size(); i++) {
            Alarm a = alarms.get(i);

            while (ids.contains(a.id)) {
                a.id++;
            }
            ids.add(a.id);

            edit.putString(PREFERENCE_ALARMS_PREFIX + i, a.save());
        }
        edit.commit();

        AlarmService.start(context);
    }

    public static List<Reminder> loadReminders(Context context) {
        ArrayList<Reminder> list = new ArrayList<>();

        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);

        int repeat = Integer.parseInt(shared.getString(PREFERENCE_REPEAT, "60"));

        Set<String> hours = shared.getStringSet(PREFERENCE_HOURS, new HashSet<String>());

        for (int hour = 0; hour < 24; hour++) {
            String h = Reminder.format(hour);

            Reminder r = new Reminder(context);
            r.enabled = hours.contains(h);
            r.setTime(hour, 0);
            list.add(r);

            String next = Reminder.format(hour + 1);

            if (r.enabled && hours.contains(next)) {
                for (int m = repeat; m < 60; m += repeat) {
                    r = new Reminder(context);
                    r.enabled = true;
                    r.setTime(hour, m);
                    list.add(r);
                }
            }
        }

        return list;
    }

    public static void toastAlarmSet(Context context, Alarm a) {
        if (!a.enable) {
            Toast.makeText(context, context.getString(R.string.alarm_disabled), Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar cur = Calendar.getInstance();

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(a.time);

        long diff = cal.getTimeInMillis() - cur.getTimeInMillis();

        int diffSeconds = (int) (diff / 1000 % 60);
        int diffMinutes = (int) (diff / (60 * 1000) % 60);
        int diffHours = (int) (diff / (60 * 60 * 1000) % 24);
        int diffDays = (int) (diff / (24 * 60 * 60 * 1000));

        String str = "";

        if (diffDays > 0)
            str += " " + context.getResources().getQuantityString(R.plurals.days, diffDays, diffDays);

        if (diffHours > 0)
            str += " " + context.getResources().getQuantityString(R.plurals.hours, diffHours, diffHours);

        if (diffMinutes > 0)
            str += " " + context.getResources().getQuantityString(R.plurals.minutes, diffMinutes, diffMinutes);

        if (diffDays == 0 && diffHours == 0 && diffMinutes == 0 && diffSeconds > 0)
            str += " " + context.getResources().getQuantityString(R.plurals.seconds, diffSeconds, diffSeconds);

        Toast.makeText(context, context.getString(R.string.alarm_set_for, str), Toast.LENGTH_SHORT).show();
    }

    public static String getHoursString(Context context, List<String> hours) {
        boolean h24 = DateFormat.is24HourFormat(context);

        String[] AMPM = new String[]{
                "12", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11",
                "12", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11",
        };

        String AM = "am";
        String PM = "pm";

        String H = context.getString(R.string.hour_symbol);

        String str = "";

        Collections.sort(hours);

        int prev = -2;
        int count = 0;
        for (String s : hours) {
            int i = Integer.parseInt(s);
            int next = prev + count;
            if (i == next + 1) {
                count++;
            } else {
                if (count != 0) {
                    if (!h24) {
                        if (prev < 12 && next >= 12)
                            str += AM;
                    }

                    if (count == 1)
                        str += ",";
                    else
                        str += "-";

                    if (h24)
                        str += Reminder.format(next);
                    else
                        str += AMPM[next];

                    if (!h24) {
                        if (next < 12 && i >= 12)
                            str += AM;
                    }

                    if (h24)
                        str += "," + Reminder.format(i);
                    else
                        str += "," + AMPM[i];
                } else {
                    if (!h24) {
                        if (prev < 12 && i >= 12)
                            str += AM;
                    }
                    if (!str.isEmpty())
                        str += ",";
                    if (h24)
                        str += Reminder.format(i);
                    else
                        str += AMPM[i];
                }

                prev = i;
                count = 0;
            }
        }

        if (count != 0) {
            int next = prev + count;

            if (!h24) {
                if (prev < 12 && next >= 12)
                    str += AM;
            }

            str += "-";

            if (h24)
                str += Reminder.format(next) + H;
            else
                str += AMPM[next] + (next >= 12 ? PM : AM);
        } else if (prev != -2) {
            if (h24)
                str += H;
            else
                str += (prev >= 12 ? PM : AM);
        }

        return str;
    }

    public int getUserTheme() {
        return getTheme(this, R.style.AppThemeLight, R.style.AppThemeDark);
    }

    public static int getTheme(Context context, int light, int dark) {
        final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        String theme = shared.getString(HourlyApplication.PREFERENCE_THEME, "");
        if (theme.equals("Theme_Dark")) {
            return dark;
        } else {
            return light;
        }
    }

    public static String getTitle(Context context, String file) {
        if (file.isEmpty())
            return null;

        File f = new File(file);
        if (f.exists()) {
            return f.getName();
        }

        Uri uri = Uri.parse(file);

        String title = titles.get(uri);
        if (title != null)
            return title;
        Ringtone rt = RingtoneManager.getRingtone(context, uri);
        title = rt.getTitle(context);
        rt.stop();
        titles.put(uri, title);
        return title;
    }

    public static String getQuantityString(Context context, Locale locale, int id, int n, Object... formatArgs) {
        Resources res = context.getResources();
        Configuration conf = res.getConfiguration();
        Locale savedLocale = conf.locale;
        if (Build.VERSION.SDK_INT >= 17)
            conf.setLocale(locale);
        else
            conf.locale = locale;
        res.updateConfiguration(conf, null);

        String str = res.getQuantityString(id, n, formatArgs);

        if (Build.VERSION.SDK_INT >= 17)
            conf.setLocale(savedLocale);
        else
            conf.locale = savedLocale;
        res.updateConfiguration(conf, null);

        return str;
    }


    public static String getString(Context context, Locale locale, int id, Object... formatArgs) {
        Resources res = context.getResources();
        Configuration conf = res.getConfiguration();
        Locale savedLocale = conf.locale;
        if (Build.VERSION.SDK_INT >= 17)
            conf.setLocale(locale);
        else
            conf.locale = locale;
        res.updateConfiguration(conf, null);

        String str = res.getString(id, formatArgs);

        if (Build.VERSION.SDK_INT >= 17)
            conf.setLocale(savedLocale);
        else
            conf.locale = savedLocale;
        res.updateConfiguration(conf, null);

        return str;
    }

    public static String getQuantityString(Context context, int id, int n, Object... formatArgs) {
        Resources res = context.getResources();
        String str = res.getQuantityString(id, n, formatArgs);
        return str;
    }

    public static String getHourString(Context context, int hour) {
        Resources res = context.getResources();
        Configuration conf = res.getConfiguration();
        Locale locale = conf.locale;
        return getHourString(context, locale, hour);
    }

    public static String getHourString(Context context, Locale locale, int hour) {
        switch (hour) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
                return getString(context, locale, R.string.day_night);
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
                return getString(context, locale, R.string.day_am);
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
                return getString(context, locale, R.string.day_mid);
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
                return getString(context, locale, R.string.day_pm);
        }

        throw new RuntimeException("bad hour");
    }
}
