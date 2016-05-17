package com.github.axet.hourlyreminder.widgets;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Environment;
import android.os.Parcelable;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;

import com.github.axet.androidlibrary.widgets.OpenFileDialog;
import com.github.axet.hourlyreminder.app.HourlyApplication;

public class CustomSoundListPreference extends ListPreference {
    public CustomSoundListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CustomSoundListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomSoundListPreference(Context context) {
        this(context, null);
    }

    public String getDefault() {
        return Environment.getExternalStorageDirectory().getPath();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
    }

    public void update(String value) {
        Preference ringtone = getPreferenceManager().findPreference(HourlyApplication.PREFERENCE_RINGTONE);
        Preference sound = getPreferenceManager().findPreference(HourlyApplication.PREFERENCE_SOUND);

        sound.setVisible(value.equals("sound"));
        ringtone.setVisible(value.equals("ringtone"));
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
    }
}
