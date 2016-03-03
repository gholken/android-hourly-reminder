package com.github.axet.hourlyreminder.services;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

public class ScreenService extends Service {

    ScreenReceiver receiver = new ScreenReceiver();

    public ScreenService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(ScreenService.class.getSimpleName(), "ScreenService onCreate");

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        //filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(receiver, filter);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(ScreenService.class.getSimpleName(), "ScreenService onDestory");

        unregisterReceiver(receiver);
    }

}

