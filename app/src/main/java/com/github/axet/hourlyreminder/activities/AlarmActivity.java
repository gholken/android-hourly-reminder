package com.github.axet.hourlyreminder.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.github.axet.hourlyreminder.R;
import com.github.axet.hourlyreminder.app.HourlyApplication;
import com.github.axet.hourlyreminder.app.Sound;
import com.github.axet.hourlyreminder.services.FireAlarmService;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class AlarmActivity extends AppCompatActivity {

    Handler handler = new Handler();
    Runnable updateClock;

    public static void showAlarmActivity(Context context, long time, Sound.Silenced silenced) {
        Intent intent = new Intent(context, AlarmActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("time", time);
        intent.putExtra("silenced", silenced);
        context.startActivity(intent);
    }

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
        View alarm = findViewById(R.id.alarm);
        updateTime(alarm, time);

        updateClock();

        View dismiss = findViewById(R.id.alarm_activity_button);
        dismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Sound.Silenced silenced = (Sound.Silenced) intent.getSerializableExtra("silenced");
        TextView sil = (TextView) findViewById(R.id.alarm_silenced);
        sil.setVisibility(View.GONE);
        if (silenced != null && silenced != Sound.Silenced.NONE) {
            sil.setVisibility(View.VISIBLE);
            switch (silenced) {
                case VIBRATE:
                    sil.setText(R.string.SoundSilencedVibrate);
                    break;
                case CALL:
                    sil.setText(R.string.SoundSilencedCall);
                    break;
                case MUSIC:
                    sil.setText(R.string.SoundSilencedMusic);
                    break;
                case SETTINGS:
                    sil.setText(R.string.SoundSilencedSettings);
                    break;
            }
        }
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
        View time = findViewById(R.id.time);
        updateTime(time, System.currentTimeMillis());

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

    void updateTime(View view, long t) {
        TextView time = (TextView) view.findViewById(R.id.alarm_time);
        View am = view.findViewById(R.id.alarm_am);
        View pm = view.findViewById(R.id.alarm_pm);

        if (DateFormat.is24HourFormat(this)) {
            SimpleDateFormat f = new SimpleDateFormat("HH:mm");
            time.setText(f.format(new Date(t)));

            am.setVisibility(View.GONE);
            pm.setVisibility(View.GONE);
        } else {
            SimpleDateFormat f = new SimpleDateFormat("h:mm");
            time.setText(f.format(new Date(t)));

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(t);
            int hour = cal.get(Calendar.HOUR_OF_DAY);

            am.setVisibility(hour >= 12 ? View.GONE : View.VISIBLE);
            pm.setVisibility(hour >= 12 ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(false);
    }
}
