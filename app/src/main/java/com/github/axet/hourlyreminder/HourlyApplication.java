package com.github.axet.hourlyreminder;

import android.app.AlarmManager;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.github.axet.hourlyreminder.activities.MainActivity;
import com.github.axet.hourlyreminder.basics.Alarm;
import com.github.axet.hourlyreminder.basics.Reminder;
import com.github.axet.hourlyreminder.basics.Sound;
import com.github.axet.hourlyreminder.basics.Storage;
import com.github.axet.hourlyreminder.services.AlarmIntentService;
import com.github.axet.hourlyreminder.services.AlarmService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class HourlyApplication extends Application {
    public static final String CANCEL = "CANCEL";
    // upcoming noticiation alarm action. triggers notification upcoming.
    public static final String NOTIFICATION = "NOTIFICATION";

    // MainActivity action
    public static final String SHOW_ALARMS_PAGE = "SHOW_ALARMS_PAGE";

    public static final int NOTIFICATION_UPCOMING_ICON = 0;
    public static final int NOTIFICATION_ALARM_ICON = 1;

    List<Alarm> alarms;
    List<Reminder> reminders;

    public Sound sound;
    public Storage storage;

    public static String formatTime(long time) {
        SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return s.format(new Date(time));
    }

    @Override
    public void onCreate() {
        super.onCreate();

        sound = new Sound(this);
        storage = new Storage(this);

        loadAlarms();
        loadReminders();
    }

    public Sound Sound() {
        return sound;
    }

    public Storage Storage() {
        return storage;
    }

    public void activateAlarm(Alarm a) {
        startService(new Intent(this, AlarmService.class)
                .putExtra("time", a.time)
                .putExtra("beep", a.beep)
                .putExtra("speech", a.speech)
                .putExtra("ringtone", a.ringtone)
                .putExtra("ringtoneValue", a.ringtoneValue));
    }

    public void dismissActiveAlarm() {
        stopService(new Intent(this, AlarmService.class));
    }

    public List<Alarm> getAlarms() {
        return alarms;
    }

    public void loadAlarms() {
        alarms = new ArrayList<>();

        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int c = shared.getInt("Alarm_Count", 0);
        if (c == 0) {
            alarms.add(new Alarm(this));
        }

        for (int i = 0; i < c; i++) {
            String prefix = "Alarm_" + i + "_";
            Alarm a = new Alarm(this);
            a.time = shared.getLong(prefix + "Time", 0);
            a.enable = shared.getBoolean(prefix + "Enable", false);
            a.weekdays = shared.getBoolean(prefix + "WeekDays", false);
            a.setWeekDays(shared.getStringSet(prefix + "WeekDays_Values", null));
            a.ringtone = shared.getBoolean(prefix + "Ringtone", false);
            a.ringtoneValue = shared.getString(prefix + "Ringtone_Value", "");
            a.beep = shared.getBoolean(prefix + "Beep", false);
            a.speech = shared.getBoolean(prefix + "Speech", false);
            alarms.add(a);
        }
    }

    public void saveAlarms() {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor edit = shared.edit();
        edit.putInt("Alarm_Count", alarms.size());
        for (int i = 0; i < alarms.size(); i++) {
            Alarm a = alarms.get(i);
            String prefix = "Alarm_" + i + "_";
            edit.putLong(prefix + "Time", a.time);
            edit.putBoolean(prefix + "Enable", a.enable);
            edit.putBoolean(prefix + "WeekDays", a.weekdays);
            edit.putStringSet(prefix + "WeekDays_Values", a.getWeekDays());
            edit.putBoolean(prefix + "Ringtone", a.ringtone);
            edit.putString(prefix + "Ringtone_Value", a.ringtoneValue);
            edit.putBoolean(prefix + "Beep", a.beep);
            edit.putBoolean(prefix + "Speech", a.speech);
        }
        edit.commit();
    }

    // check if 'hour' is a enabled reminder
    public Reminder getReminder(long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);

        int ah = cal.get(Calendar.HOUR_OF_DAY);

        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> hours = shared.getStringSet("hours", new HashSet<String>());
        String h = String.format("%02d", ah);
        for (Reminder r : reminders) {
            if (r.getHour() == ah)
                return r;
        }
        return null;
    }

    public void loadReminders() {
        ArrayList<Reminder> list = new ArrayList<>();

        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> hours = shared.getStringSet("hours", new HashSet<String>());

        for (int i = 0; i < 24; i++) {
            String h = String.format("%02d", i);

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, i);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            if (hours.contains(h)) {
                Reminder r = new Reminder();
                r.time = cal.getTimeInMillis();
                list.add(r);
            }
        }

        this.reminders = list;
    }

    // create list for hour reminders. 'time' list for reminder (hronological order)
    public TreeSet<Long> generateReminders(Calendar cur) {
        TreeSet<Long> alarms = new TreeSet<>();

        int hour = cur.get(Calendar.HOUR_OF_DAY);

        for (Reminder r : reminders) {
            alarms.add(r.getAlarmTime(cur));
        }

        return alarms;
    }

    // create list for alarms. 'time' list for alarms (hronological order)
    public TreeSet<Long> generateAlarms(Calendar cur) {
        TreeSet<Long> alarms = new TreeSet<>();

        for (Alarm a : this.alarms) {
            if (!a.enable)
                continue;
            alarms.add(a.getAlarmTime(cur));
        }

        return alarms;
    }

    // cancel alarm 'time' by set it time for day+1 (same hour:min)
    public void tomorrow(long time) {
        Alarm a = getAlarm(time);
        if (a != null) {
            a.setTomorrow();
            updateAlerts();
        }

        Reminder r = getReminder(time);
        if (r != null) {
            r.setTomorrow();
            updateAlerts();
        }
    }

    // scan all alarms and hourly reminders and register alarm for next one.
    //
    //
    public void updateAlerts() {
        Context context = this;

        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);

        TreeSet<Long> alarms = new TreeSet<>();

        Calendar cur = Calendar.getInstance();

        // check hourly reminders
        if (shared.getBoolean("enabled", false)) {
            alarms.addAll(generateReminders(cur));
        }
        // check alarms
        alarms.addAll(generateAlarms(cur));

        Intent intent = new Intent(context, AlarmIntentService.class).setAction(HourlyApplication.class.getSimpleName());

        if (alarms.isEmpty()) {
            PendingIntent pe = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
            alarm.cancel(pe);
            updateNotificationAlarm(0);
        } else {
            long time = alarms.first();

            intent.putExtra("time", time);

            PendingIntent pe = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

            Log.d(HourlyApplication.class.getSimpleName(), "Current: " + formatTime(cur.getTimeInMillis()) + "; SetAlarm: " + formatTime(time));

            updateNotificationAlarm(time);

            if (shared.getBoolean("alarm", true)) {
                if (Build.VERSION.SDK_INT >= 21) {
                    alarm.setAlarmClock(new AlarmManager.AlarmClockInfo(time, pe), pe);
                } else {
                    alarm.set(AlarmManager.RTC_WAKEUP, time, pe);
                }
            } else {
                if (Build.VERSION.SDK_INT >= 23) {
                    alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pe);
                } else {
                    alarm.set(AlarmManager.RTC_WAKEUP, time, pe);
                }
            }
        }
    }

    // register notification_upcoming alarm for 'time' - 15min.
    //
    // service will call showNotificationUpcoming(time)
    //
    void updateNotificationAlarm(long time) {
        Intent intent = new Intent(this, AlarmIntentService.class).setAction(NOTIFICATION);
        intent.putExtra("time", time);
        PendingIntent pe = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

        AlarmManager alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

        alarm.cancel(pe);

        if (time == 0) {
            showNotificationUpcoming(0);
            return;
        } else {
            Calendar cur = Calendar.getInstance();

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(time);
            cal.add(Calendar.MINUTE, -15);

            if (cur.after(cal)) {
                // we already 15 before alarm, show notification_upcoming
                showNotificationUpcoming(time);
            } else {
                showNotificationUpcoming(0);
                // time to wait before show notification_upcoming
                time = cal.getTimeInMillis();

                if (Build.VERSION.SDK_INT >= 23) {
                    alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pe);
                } else {
                    alarm.set(AlarmManager.RTC_WAKEUP, time, pe);
                }
            }
        }
    }

    // show notification_upcoming. (about upcoming alarm)
    //
    // time - 0 cancel notifcation
    // time - upcoming alarm time, show text.
    public void showNotificationUpcoming(long time) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (time == 0) {
            notificationManager.cancel(NOTIFICATION_UPCOMING_ICON);
        } else {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(time);
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int min = c.get(Calendar.MINUTE);

            Intent intent = new Intent(this, AlarmIntentService.class).setAction(CANCEL);
            intent.putExtra("time", time);
            PendingIntent pe = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

            Intent maini = new Intent(this, MainActivity.class).setAction(SHOW_ALARMS_PAGE);
            maini.putExtra("time", time);
            PendingIntent main = PendingIntent.getActivity(this, 0, maini, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

            String text = String.format("%02d:%02d", hour, min);

            RemoteViews view = new RemoteViews(getPackageName(), R.layout.notification_upcoming);
            view.setOnClickPendingIntent(R.id.notification_cancel, pe);
            view.setTextViewText(R.id.notification_text, text);
            view.setOnClickPendingIntent(R.id.notification_base, main);

            Notification.Builder builder = new Notification.Builder(this)
                    .setOngoing(true)
                    .setContentTitle("Upcoming alarm")
                    .setContentText(text)
                    .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                    .setContent(view);

            if (Build.VERSION.SDK_INT >= 21)
                builder.setVisibility(Notification.VISIBILITY_PUBLIC);

            notificationManager.notify(NOTIFICATION_UPCOMING_ICON, builder.build());
        }
    }

    public Alarm getAlarm(long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);

        int ah = cal.get(Calendar.HOUR_OF_DAY);
        int am = cal.get(Calendar.MINUTE);

        for (Alarm a : alarms) {
            if (a.getHour() == ah && a.getMin() == am)
                return a;
        }
        return null;
    }

}
