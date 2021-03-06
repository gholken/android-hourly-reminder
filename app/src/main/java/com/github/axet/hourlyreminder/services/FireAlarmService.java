package com.github.axet.hourlyreminder.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.github.axet.hourlyreminder.R;
import com.github.axet.hourlyreminder.activities.AlarmActivity;
import com.github.axet.hourlyreminder.activities.MainActivity;
import com.github.axet.hourlyreminder.app.HourlyApplication;
import com.github.axet.hourlyreminder.app.Sound;
import com.github.axet.hourlyreminder.basics.Alarm;

import java.util.Calendar;

public class FireAlarmService extends Service {
    public static final String TAG = FireAlarmService.class.getSimpleName();

    public static final String FIRE_ALARM = FireAlarmService.class.getCanonicalName() + ".FIRE_ALARM";

    // alarm activity action. close it.
    public static final String CLOSE_ACTIVITY = FireAlarmService.class.getCanonicalName() + ".CLOSE_ACTIVITY";

    // notification click -> show activity broadcast
    public static final String SHOW_ACTIVITY = FireAlarmService.class.getCanonicalName() + ".SHOW_ACTIVITY";

    // minutes
    public static final int ALARM_AUTO_OFF = 15;

    FireAlarmReceiver receiver = new FireAlarmReceiver();
    Sound sound;
    Handler handle = new Handler();
    Runnable alive;
    boolean alarmActivity = false;
    Sound.Silenced silenced = Sound.Silenced.NONE;

    PhoneStateChangeListener pscl;

    class PhoneStateChangeListener extends PhoneStateListener {
        public boolean wasRinging;

        @Override
        public void onCallStateChanged(int s, String incomingNumber) {
            switch (s) {
                // incoming call ringing
                case TelephonyManager.CALL_STATE_RINGING:
                    wasRinging = true;
                    break;
                // answered
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    wasRinging = true;
                    // stop current alarm
                    if (sound != null) {
                        sound.playerClose();
                    }
                    break;
                // switch to idle state: no call, no ringing
                case TelephonyManager.CALL_STATE_IDLE:
                    wasRinging = false;
                    break;
            }
        }
    }

    public class FireAlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(FireAlarmReceiver.class.getSimpleName(), "FireAlarmReceiver " + intent.getAction());

            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                long time = intent.getLongExtra("time", 0);
                showAlarmActivity(time, silenced);
            }
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                // do nothing. do not annoy user. he will see alarm screen on next screen on event.
            }
            if (intent.getAction().equals(SHOW_ACTIVITY)) {
                long time = intent.getLongExtra("time", 0);
                showAlarmActivity(time, silenced);
            }
        }
    }

    public static void activateAlarm(Context context, Alarm a) {
        context.startService(new Intent(context, FireAlarmService.class)
                .setAction(FIRE_ALARM)
                .putExtra("time", a.time)
                .putExtra("beep", a.beep)
                .putExtra("speech", a.speech)
                .putExtra("ringtone", a.ringtone)
                .putExtra("ringtoneValue", a.ringtoneValue));
    }

    public static void dismissActiveAlarm(Context context) {
        context.stopService(new Intent(context, FireAlarmService.class));
        final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = shared.edit();
        edit.remove(HourlyApplication.PREFERENCE_ACTIVE_ALARM);
        edit.commit();
    }

    public FireAlarmService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        sound = new Sound(this);
    }

    public Alarm getAlarm(Intent intent) {
        Alarm a = new Alarm(this);

        a.time = intent.getLongExtra("time", 0);
        a.beep = intent.getBooleanExtra("beep", false);
        a.speech = intent.getBooleanExtra("speech", false);
        a.ringtone = intent.getBooleanExtra("ringtone", false);
        a.ringtoneValue = intent.getStringExtra("ringtoneValue");

        return a;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(SHOW_ACTIVITY);
        registerReceiver(receiver, filter);

        final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);

        if (shared.getBoolean(HourlyApplication.PREFERENCE_CALLSILENCE, false)) {
            pscl = new PhoneStateChangeListener();
            TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
            tm.listen(pscl, PhoneStateListener.LISTEN_CALL_STATE);
        }

        Alarm a;

        if (intent == null) {
            Log.d(TAG, "onStartCommand restart");
            String json = shared.getString(HourlyApplication.PREFERENCE_ACTIVE_ALARM, "");
            if (json.isEmpty())
                return START_NOT_STICKY;

            a = new Alarm(this, json);
        } else {
            a = getAlarm(intent);

            SharedPreferences.Editor editor = shared.edit();
            editor.putString(HourlyApplication.PREFERENCE_ACTIVE_ALARM, a.save());
            editor.commit();
        }

        Log.d(TAG, "time=" + Alarm.format(a.time));

        if (!alive(a.time)) {
            stopSelf();
            showNotificationMissed(a.time);
            return START_NOT_STICKY;
        }

        showNotificationAlarm(a.time);

        // do we have silence alarm?
        silenced = sound.playAlarm(a);

        showAlarmActivity(a.time, silenced);

        return super.onStartCommand(intent, flags, startId);
    }

    boolean alive(final long time) {
        Calendar cur = Calendar.getInstance();

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        cal.add(Calendar.MINUTE, ALARM_AUTO_OFF);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        boolean b = cal.after(cur);

        if (b) {
            alive = new Runnable() {
                @Override
                public void run() {
                    if (!alive(time)) {
                        stopSelf();
                        showNotificationMissed(time);
                    }
                }
            };
            handle.postDelayed(alive, 1000 * 60);
        }

        return b;
    }

    public void showAlarmActivity(long time, Sound.Silenced silenced) {
        alarmActivity = true;
        AlarmActivity.showAlarmActivity(this, time, silenced);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public class Binder extends android.os.Binder {
        public FireAlarmService getService() {
            return FireAlarmService.this;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(FireAlarmService.class.getSimpleName(), "onDestory");

        final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);
        if (shared.getBoolean(HourlyApplication.PREFERENCE_VIBRATE, false)) {
            sound.vibrateStop();
        }

        if (sound != null) {
            sound.close();
            sound = null;
        }

        showNotificationAlarm(0);

        unregisterReceiver(receiver);

        if (pscl != null) {
            TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
            tm.listen(pscl, PhoneStateListener.LISTEN_NONE);
            pscl = null;
        }

        if (alarmActivity) {
            alarmActivity = false;
            Intent intent = new Intent(this, AlarmActivity.class);
            intent.setAction(CLOSE_ACTIVITY);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        if (alive != null) {
            handle.removeCallbacks(alive);
            alive = null;
        }
    }

    // show notification about missed alarm
    void showNotificationMissed(long time) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (time == 0) {
            notificationManager.cancel(HourlyApplication.NOTIFICATION_MISSED_ICON);
        } else {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(time);
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int min = c.get(Calendar.MINUTE);

            PendingIntent main = PendingIntent.getActivity(this, 0,
                    new Intent(this, MainActivity.class).setAction(MainActivity.SHOW_ALARMS_PAGE).putExtra("time", time),
                    PendingIntent.FLAG_UPDATE_CURRENT);

            String text = getString(R.string.AlarmMissedAfter, hour, min, ALARM_AUTO_OFF);

            RemoteViews view = new RemoteViews(getPackageName(), HourlyApplication.getTheme(getBaseContext(), R.layout.notification_alarm_light, R.layout.notification_alarm_dark));
            view.setOnClickPendingIntent(R.id.notification_base, main);
            view.setTextViewText(R.id.notification_subject, getString(R.string.AlarmMissed));
            view.setTextViewText(R.id.notification_text, text);
            view.setViewVisibility(R.id.notification_button, View.GONE);

            Notification.Builder builder = new Notification.Builder(this)
                    .setContentTitle(getString(R.string.Alarm))
                    .setContentText(text)
                    .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                    .setContent(view);

            if (Build.VERSION.SDK_INT >= 21)
                builder.setVisibility(Notification.VISIBILITY_PUBLIC);

            notificationManager.notify(HourlyApplication.NOTIFICATION_MISSED_ICON, builder.build());
        }
    }

    // alarm dismiss button
    public void showNotificationAlarm(long time) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean(HourlyApplication.PREFERENCE_NOTIFICATIONS, true))
            return;

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (time == 0) {
            notificationManager.cancel(HourlyApplication.NOTIFICATION_ALARM_ICON);
        } else {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(time);
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int min = c.get(Calendar.MINUTE);

            PendingIntent button = PendingIntent.getService(this, 0,
                    new Intent(this, AlarmService.class).setAction(AlarmService.DISMISS).putExtra("time", time),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

            PendingIntent main = PendingIntent.getBroadcast(this, 0,
                    new Intent(SHOW_ACTIVITY).putExtra("time", time),
                    PendingIntent.FLAG_UPDATE_CURRENT);

            String text = String.format("%02d:%02d", hour, min);

            RemoteViews view = new RemoteViews(getPackageName(), HourlyApplication.getTheme(getBaseContext(), R.layout.notification_alarm_light, R.layout.notification_alarm_dark));
            view.setOnClickPendingIntent(R.id.notification_base, main);
            view.setOnClickPendingIntent(R.id.notification_button, button);
            view.setTextViewText(R.id.notification_text, text);

            Notification.Builder builder = new Notification.Builder(this)
                    .setOngoing(true)
                    .setContentTitle(getString(R.string.Alarm))
                    .setContentText(text)
                    .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                    .setContent(view);

            if (Build.VERSION.SDK_INT >= 21)
                builder.setVisibility(Notification.VISIBILITY_PUBLIC);

            notificationManager.notify(HourlyApplication.NOTIFICATION_ALARM_ICON, builder.build());
        }
    }
}

