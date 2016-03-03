package com.github.axet.hourlyreminder.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.RemoteViews;

import com.github.axet.hourlyreminder.HourlyApplication;
import com.github.axet.hourlyreminder.R;
import com.github.axet.hourlyreminder.activities.AlarmActivity;
import com.github.axet.hourlyreminder.activities.MainActivity;
import com.github.axet.hourlyreminder.basics.Alarm;
import com.github.axet.hourlyreminder.basics.Sound;

import java.util.Calendar;

public class AlarmService extends Service {
    // dismiss current alarm action
    public static final String DISMISS = "DISMISS";

    // main activity action
    public static final String CLOSE_ACTIVITY = "CLOSE_ACTIVITY";

    ScreenReceiver receiver = new ScreenReceiver();
    MediaPlayer player;
    private Binder binder = new Binder();
    Sound sound;

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
        super.onStartCommand(intent, flags, startId);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        //filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(receiver, filter);

        final long time = intent.getLongExtra("time", 0);
        final boolean beep = intent.getBooleanExtra("beep", false);
        final boolean speech = intent.getBooleanExtra("speech", false);
        final boolean ringtone = intent.getBooleanExtra("ringtone", false);
        final String ringtoneValue = intent.getStringExtra("ringtoneValue");

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

        return START_STICKY;
    }

    public void showAlarmActivity(long time) {
        Intent intent = new Intent(this, AlarmActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("time", time);
        startActivity(intent);
    }

    void playRingtone(Uri uri) {
        final HourlyApplication app = ((HourlyApplication) getApplicationContext());
        if (player != null)
            player.release();
        player = app.Sound().playRingtone(uri);
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

        if (player != null) {
            player.release();
            player = null;
        }

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

            Intent intent = new Intent(this, AlarmIntentService.class).setAction(DISMISS);
            intent.putExtra("time", time);
            PendingIntent pe = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

            Intent maini = new Intent(this, MainActivity.class).setAction(HourlyApplication.SHOW_ALARMS_PAGE);
            maini.putExtra("time", time);
            PendingIntent main = PendingIntent.getActivity(this, 0, maini, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

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

