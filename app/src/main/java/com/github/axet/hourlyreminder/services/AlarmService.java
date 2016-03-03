package com.github.axet.hourlyreminder.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.RemoteViews;

import com.github.axet.hourlyreminder.HourlyApplication;
import com.github.axet.hourlyreminder.R;
import com.github.axet.hourlyreminder.activities.AlarmActivity;
import com.github.axet.hourlyreminder.activities.MainActivity;
import com.github.axet.hourlyreminder.basics.Sound;

import java.util.Calendar;

public class AlarmService extends Service {
    // alarm activity action. close it.
    public static final String CLOSE_ACTIVITY = AlarmService.class.getCanonicalName() + ".CLOSE_ACTIVITY";

    // show activity broadcast
    public static final String SHOW_ACTIVITY = AlarmService.class.getCanonicalName() + ".SHOW_ACTIVITY";

    // minutes
    public static final int ALARM_AUTO_OFF = 15;

    public class ScreenReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(ScreenReceiver.class.getSimpleName(), "ScreenReceiver " + intent.getAction());

            Intent i = new Intent(context, AlarmActivity.class);
            long time = intent.getLongExtra("time", 0);
            i.putExtra("time", time);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }

    ScreenReceiver receiver = new ScreenReceiver();
    Binder binder = new Binder();
    Sound sound;
    Handler handle = new Handler();
    Runnable cancel;

    public AlarmService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(AlarmService.class.getSimpleName(), "AlarmService onCreate");

        sound = new Sound(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        //filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(SHOW_ACTIVITY);
        registerReceiver(receiver, filter);

        final long time = intent.getLongExtra("time", 0);
        final boolean beep = intent.getBooleanExtra("beep", false);
        final boolean speech = intent.getBooleanExtra("speech", false);
        final boolean ringtone = intent.getBooleanExtra("ringtone", false);
        final String ringtoneValue = intent.getStringExtra("ringtoneValue");

        if (!alive(time)) {
            stopSelf();
            showNotificationMissed(time);
            return START_NOT_STICKY;
        }

        showNotificationAlarm(time);

        if (beep) {
            sound.playBeep(new Runnable() {
                               @Override
                               public void run() {
                                   if (speech) {
                                       sound.playSpeech(new Runnable() {
                                           @Override
                                           public void run() {
                                               if (ringtone) {
                                                   playRingtone(Uri.parse(ringtoneValue));
                                               }
                                           }
                                       });
                                   } else if (ringtone) {
                                       playRingtone(Uri.parse(ringtoneValue));
                                   }
                               }
                           }
            );
        } else if (speech) {
            sound.playSpeech(new Runnable() {
                @Override
                public void run() {
                    playRingtone(Uri.parse(ringtoneValue));
                }
            });
        } else if (ringtone) {
            playRingtone(Uri.parse(ringtoneValue));
        }

        showAlarmActivity(time);

        return super.onStartCommand(intent, flags, startId);
    }

    boolean alive(final long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        cal.add(Calendar.MINUTE, ALARM_AUTO_OFF);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Calendar cur = Calendar.getInstance();

        boolean b = cal.after(cur);

        if (b) {
            cancel = new Runnable() {
                @Override
                public void run() {
                    if (!alive(time)) {
                        stopSelf();
                        showNotificationMissed(time);
                    }
                }
            };
            handle.postDelayed(cancel, 1000 * 60);
        }

        return b;
    }

    public void showAlarmActivity(long time) {
        Intent intent = new Intent(this, AlarmActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("time", time);
        startActivity(intent);
    }

    void playRingtone(Uri uri) {
        sound.playRingtone(uri);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public class Binder extends android.os.Binder {
        public AlarmService getService() {
            return AlarmService.this;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(AlarmService.class.getSimpleName(), "onDestory");

        if (sound != null) {
            sound.close();
            sound = null;
        }

        showNotificationAlarm(0);

        unregisterReceiver(receiver);

        Intent intent = new Intent(this, AlarmActivity.class);
        intent.setAction(CLOSE_ACTIVITY);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

        if (cancel != null) {
            handle.removeCallbacks(cancel);
            cancel = null;
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

            Intent maini = new Intent(this, MainActivity.class).setAction(HourlyApplication.SHOW_ALARMS_PAGE);
            maini.putExtra("time", time);
            PendingIntent main = PendingIntent.getActivity(this, 0, maini, PendingIntent.FLAG_UPDATE_CURRENT);

            String text = String.format("Alarm %02d:%02d dismissed after %d mins", hour, min, ALARM_AUTO_OFF);

            RemoteViews view = new RemoteViews(getPackageName(), R.layout.notification_missed);
            view.setTextViewText(R.id.notification_text, text);
            view.setOnClickPendingIntent(R.id.notification_base, main);

            Notification.Builder builder = new Notification.Builder(this)
                    .setContentTitle("Alarm")
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
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (time == 0) {
            notificationManager.cancel(HourlyApplication.NOTIFICATION_ALARM_ICON);
        } else {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(time);
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int min = c.get(Calendar.MINUTE);

            Intent intent = new Intent().setAction(HourlyApplication.DISMISS);
            intent.putExtra("time", time);
            PendingIntent pe = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

            Intent maini = new Intent().setAction(SHOW_ACTIVITY);
            maini.putExtra("time", time);
            PendingIntent main = PendingIntent.getBroadcast(this, 0, maini, PendingIntent.FLAG_UPDATE_CURRENT);

            String text = String.format("%02d:%02d", hour, min);

            RemoteViews view = new RemoteViews(getPackageName(), R.layout.notification_alarm);
            view.setOnClickPendingIntent(R.id.notification_cancel, pe);
            view.setTextViewText(R.id.notification_text, text);
            view.setOnClickPendingIntent(R.id.notification_base, main);

            Notification.Builder builder = new Notification.Builder(this)
                    .setOngoing(true)
                    .setContentTitle("Alarm")
                    .setContentText(text)
                    .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                    .setContent(view);

            if (Build.VERSION.SDK_INT >= 21)
                builder.setVisibility(Notification.VISIBILITY_PUBLIC);

            notificationManager.notify(HourlyApplication.NOTIFICATION_ALARM_ICON, builder.build());
        }
    }
}

