package com.github.axet.hourlyreminder.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.github.axet.androidlibrary.widgets.ThemeUtils;
import com.github.axet.hourlyreminder.R;
import com.github.axet.hourlyreminder.app.HourlyApplication;
import com.github.axet.hourlyreminder.basics.Alarm;
import com.github.axet.hourlyreminder.services.FireAlarmService;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class AlarmActivity extends AppCompatActivity {

    Handler handler = new Handler();
    Runnable updateClock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(HourlyApplication.getTheme(this, R.style.AppThemeLight_NoActionBar, R.style.AppThemeDark_NoActionBar));

        layoutInit();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        Intent intent = getIntent();

        String action = intent.getAction();
        if (action != null && action.equals(FireAlarmService.CLOSE_ACTIVITY)) {
            finish();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // bug? it haven't been called
        getResources().updateConfiguration(newConfig, null);

        layoutInit();
    }

    void layoutInit() {
        setContentView(R.layout.activity_alarm);

        Intent intent = getIntent();

        long time = intent.getLongExtra("time", 0);
        TextView text = (TextView) findViewById(R.id.alarm_text);
        text.setText(Alarm.format(time));

        updateClock();

        View dismiss = findViewById(R.id.alarm_activity_button);
        dismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // handing startActivity with Intent.FLAG_ACTIVITY_NEW_TASK
        String action = intent.getAction();
        if (action != null && action.equals(FireAlarmService.CLOSE_ACTIVITY))
            finish();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    void updateClock() {
        TextView text = (TextView) findViewById(R.id.time);
        text.setText(Alarm.format(System.currentTimeMillis()));

        if (updateClock == null) {
            handler.removeCallbacks(updateClock);
        }
        updateClock = new Runnable() {
            @Override
            public void run() {
                updateClock();
            }
        };
        handler.postDelayed(updateClock, 1000);
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (updateClock != null) {
            handler.removeCallbacks(updateClock);
            updateClock = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateClock();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        FireAlarmService.dismissActiveAlarm(this);

        if (updateClock != null) {
            handler.removeCallbacks(updateClock);
            updateClock = null;
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(false);
    }
}
