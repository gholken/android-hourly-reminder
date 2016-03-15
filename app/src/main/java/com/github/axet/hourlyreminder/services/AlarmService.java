package com.github.axet.hourlyreminder.services;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.github.axet.hourlyreminder.R;
import com.github.axet.hourlyreminder.activities.MainActivity;
import com.github.axet.hourlyreminder.app.HourlyApplication;
import com.github.axet.hourlyreminder.app.Sound;
import com.github.axet.hourlyreminder.basics.Alarm;
import com.github.axet.hourlyreminder.basics.Reminder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

/**
 * System Alarm Manager notifes this service to create/stop alarms.
 * <p/>
 * All Alarm notifications clicks routed to this service.
 */
public class AlarmService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String TAG = AlarmService.class.getSimpleName();

    // upcoming noticiation alarm action. triggers notification upcoming.
    public static final String REGISTER = AlarmService.class.getCanonicalName() + ".REGISTER";
    // upcoming noticiation alarm action. triggers notification upcoming.
    public static final String NOTIFICATION = AlarmService.class.getCanonicalName() + ".NOTIFICATION";
    // cancel alarm
    public static final String CANCEL = HourlyApplication.class.getCanonicalName() + ".CANCEL";
    // alarm broadcast, trigs sounds
    public static final String ALARM = HourlyApplication.class.getCanonicalName() + ".ALARM";
    // dismiss current alarm action
    public static final String DISMISS = HourlyApplication.class.getCanonicalName() + ".DISMISS";

    public static void start(Context context) {
        Intent intent = new Intent(context, AlarmService.class);
        intent.setAction(REGISTER);
        context.startService(intent);
    }

    Sound sound;
    List<Alarm> alarms;
    List<Reminder> reminders;

    public AlarmService() {
        super();
    }

    public static String formatTime(long time) {
        SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return s.format(new Date(time));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        sound = new Sound(this);
        alarms = HourlyApplication.loadAlarms(this);
        reminders = HourlyApplication.loadReminders(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        if (sound != null) {
            sound.close();
            sound = null;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            Log.d(TAG, "onStartCommand " + action);
            if (action != null) {
                long time = intent.getLongExtra("time", 0);
                if (action.equals(NOTIFICATION)) {
                    showNotificationUpcoming(time);
                } else if (action.equals(CANCEL)) {
                    tomorrow(time);
                } else if (action.equals(DISMISS)) {
                    FireAlarmService.dismissActiveAlarm(this);
                } else if (action.equals(ALARM)) {
                    soundAlarm(time);
                } else if (action.equals(REGISTER)) {
                    registerNextAlarm();
                }
            }
        } else {
            Log.d(TAG, "onStartCommand restart");
        }

        return super.onStartCommand(intent, flags, startId);
    }

    // create list for hour reminders. 'time' list for reminder (hronological order)
    public TreeSet<Long> generateReminders(Calendar cur) {
        TreeSet<Long> alarms = new TreeSet<>();

        for (Reminder r : reminders) {
            if (r.enabled)
                alarms.add(r.time);
        }

        return alarms;
    }

    // create list for alarms. 'time' list for alarms (hronological order)
    public TreeSet<Long> generateAlarms(Calendar cur) {
        TreeSet<Long> alarms = new TreeSet<>();

        for (Alarm a : this.alarms) {
            if (a.enable)
                alarms.add(a.time);
        }

        return alarms;
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

    // check if 'hour' is a enabled reminder
    public Reminder getReminder(long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);

        int rh = cal.get(Calendar.HOUR_OF_DAY);
        int rm = cal.get(Calendar.MINUTE);

        if (rm != 0)
            return null;

        String h = Reminder.format(rh);
        for (Reminder r : reminders) {
            if (r.getHour() == rh)
                return r;
        }
        return null;
    }

    // cancel alarm 'time' by set it time for day+1 (same hour:min)
    public void tomorrow(long time) {
        Alarm a = getAlarm(time);
        if (a != null && a.enable && a.isToday()) {
            if (a.weekdays) {
                // be safe for another timezone. if we moved we better call setNext()
                a.setTomorrow();
            } else {
                a.setEnable(false);
            }
            HourlyApplication.toastAlarmSet(this, a);
            HourlyApplication.saveAlarms(this, alarms);
        }

        Reminder r = getReminder(time);
        if (r != null && r.enabled && r.isToday()) {
            r.setTomorrow();
        }

        registerNextAlarm();
    }

    // register alarm event for next one.
    //
    // scan all alarms and hourly reminders and register net one
    //
    public void registerNextAlarm() {
        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);

        TreeSet<Long> alarms = new TreeSet<>();

        Calendar cur = Calendar.getInstance();

        // check hourly reminders
        if (shared.getBoolean(HourlyApplication.PREFERENCE_ENABLED, false)) {
            alarms.addAll(generateReminders(cur));
        }
        // check alarms
        alarms.addAll(generateAlarms(cur));

        Intent intent = new Intent(this, AlarmService.class).setAction(ALARM);

        if (alarms.isEmpty()) {
            PendingIntent pe = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
            alarm.cancel(pe);
            updateNotificationUpcomingAlarm(0);
        } else {
            long time = alarms.first();

            intent.putExtra("time", time);

            PendingIntent pe = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

            Log.d(HourlyApplication.class.getSimpleName(), "Current: " + formatTime(cur.getTimeInMillis()) + "; SetAlarm: " + formatTime(time));

            updateNotificationUpcomingAlarm(time);

            if (shared.getBoolean(HourlyApplication.PREFERENCE_ALARM, true)) {
                if (Build.VERSION.SDK_INT >= 21) {
                    alarm.setAlarmClock(new AlarmManager.AlarmClockInfo(time, pe), pe);
                } else if (Build.VERSION.SDK_INT >= 19) {
                    alarm.setExact(AlarmManager.RTC_WAKEUP, time, pe);
                } else {
                    alarm.set(AlarmManager.RTC_WAKEUP, time, pe);
                }
            } else {
                if (Build.VERSION.SDK_INT >= 23) {
                    alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pe);
                } else if (Build.VERSION.SDK_INT >= 19) {
                    alarm.setExact(AlarmManager.RTC_WAKEUP, time, pe);
                } else {
                    alarm.set(AlarmManager.RTC_WAKEUP, time, pe);
                }
            }
        }
    }

    // register notification_upcoming alarm event for 'time' - 15min.
    //
    // service will call showNotificationUpcoming(time)
    //
    void updateNotificationUpcomingAlarm(long time) {
        AlarmManager alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

        PendingIntent pe = PendingIntent.getService(this, 0,
                new Intent(this, AlarmService.class).setAction(NOTIFICATION).putExtra("time", time),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

        alarm.cancel(pe);

        if (time == 0) {
            showNotificationUpcoming(0);
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
                } else if (Build.VERSION.SDK_INT >= 19) {
                    alarm.setExact(AlarmManager.RTC_WAKEUP, time, pe);
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
            notificationManager.cancel(HourlyApplication.NOTIFICATION_UPCOMING_ICON);
        } else {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(time);
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int min = c.get(Calendar.MINUTE);

            PendingIntent button = PendingIntent.getService(this, 0,
                    new Intent(this, AlarmService.class).setAction(CANCEL).putExtra("time", time),
                    PendingIntent.FLAG_UPDATE_CURRENT);

            PendingIntent main = PendingIntent.getActivity(this, 0,
                    new Intent(this, MainActivity.class).setAction(MainActivity.SHOW_ALARMS_PAGE).putExtra("time", time),
                    PendingIntent.FLAG_UPDATE_CURRENT);

            String subject = "Upcoming alarm";
            String text = Alarm.format(hour, min);

            RemoteViews view = new RemoteViews(getPackageName(), R.layout.notification_upcoming);
            view.setOnClickPendingIntent(R.id.notification_cancel, button);
            view.setTextViewText(R.id.notification_text, text);
            view.setOnClickPendingIntent(R.id.notification_base, main);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setOngoing(true)
                    .setContentTitle(subject)
                    .setContentText(text)
                    .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                    .setContent(view);

            if (Build.VERSION.SDK_INT >= 21)
                builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            notificationManager.notify(HourlyApplication.NOTIFICATION_UPCOMING_ICON, builder.build());
        }
    }

    // alarm come from service call (System Alarm Manager) for specified time
    //
    // we have to check what 'alarms' do we have at specified time (can be reminder + alarm)
    // and act propertly.
    public void soundAlarm(final long time) {
        // find hourly reminder + alarm = combine proper sound notification_upcoming (can be merge beep, speech, ringtone)
        //
        // then sound alarm or hourly reminder

        Alarm a = getAlarm(time);
        if (a != null && a.enable) {
            Log.d(TAG, "Sound Alarm " + a.format());
            Alarm old = new Alarm(a);
            if (!a.weekdays) {
                // disable alarm after it goes off for non rcuring alarms (!a.weekdays)
                a.setEnable(false);
            } else {
                // calling setNext is more safe. if this alarm have to fire today we will reset it
                // to the same time. if it is already past today's time (as we expect) then it will
                // be set for tomorrow.
                //
                // also safe if we moved to another timezone.
                a.setNext();
            }
            HourlyApplication.saveAlarms(this, alarms);

            FireAlarmService.activateAlarm(this, old);
            registerNextAlarm();
            return;
        }

        Reminder reminder = getReminder(time);
        if (reminder != null && reminder.enabled) {
            final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);
            if (shared.getBoolean(HourlyApplication.PREFERENCE_BEEP, false)) {
                sound.playBeep(new Runnable() {
                    @Override
                    public void run() {
                        sound.playSpeech(time, null);
                    }
                });
            } else {
                sound.playSpeech(time, null);
            }
            // calling setNext is more safe. if this alarm have to fire today we will reset it
            // to the same time. if it is already past today's time (as we expect) then it will
            // be set for tomorrow.
            //
            // also safe if we moved to another timezone.
            reminder.setNext();

            registerNextAlarm();
            return;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG, "onSharedPreferenceChanged " + key);
        if (key.startsWith(HourlyApplication.PREFERENCE_ALARMS_PREFIX)) {
            alarms = HourlyApplication.loadAlarms(this);
            registerNextAlarm();
        }

        // reset reminders on special events
        if (key.equals(HourlyApplication.PREFERENCE_ENABLED)) {
            reminders = HourlyApplication.loadReminders(this);
            registerNextAlarm();
        }
        if (key.equals(HourlyApplication.PREFERENCE_HOURS)) {
            reminders = HourlyApplication.loadReminders(this);
            registerNextAlarm();
        }
        if (key.equals(HourlyApplication.PREFERENCE_ALARM)) {
            registerNextAlarm();
        }
    }
}
