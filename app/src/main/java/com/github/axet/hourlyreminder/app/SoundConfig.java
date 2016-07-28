package com.github.axet.hourlyreminder.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import com.github.axet.hourlyreminder.R;
import com.github.axet.hourlyreminder.basics.Alarm;

import java.util.HashSet;
import java.util.Set;

public class SoundConfig {
    public static final String TAG = SoundConfig.class.getSimpleName();

    public final static Uri DEFAULT_ALARM = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

    public enum Silenced {
        NONE,
        VIBRATE, // vibrate instead of sound
        SETTINGS,
        CALL,
        MUSIC
    }

    Context context;
    Handler handler;

    Float volume;

    // AudioSystem.STREAM_ALARM AudioManager.STREAM_ALARM;
    final static int SOUND_CHANNEL = AudioAttributes.USAGE_ALARM;
    final static int SOUND_TYPE = AudioAttributes.CONTENT_TYPE_SONIFICATION;

    public SoundConfig(Context context) {
        this.context = context;
        this.handler = new Handler();
    }

    float getRingtoneVolume() {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        if (Integer.parseInt(shared.getString(HourlyApplication.PREFERENCE_INCREASE_VOLUME, "0")) > 0) {
            return 0;
        }

        return getVolume();
    }

    float getVolume() {
        if (volume != null)
            return volume;

        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        return (float) (Math.pow(shared.getFloat(HourlyApplication.PREFERENCE_VOLUME, 1f), 3));
    }

    public void setVolume(float f) {
        volume = f;
    }

}
