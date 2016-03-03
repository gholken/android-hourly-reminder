package com.github.axet.hourlyreminder.layouts;

import android.content.Context;
import android.content.DialogInterface;

import android.content.SharedPreferences;
import android.content.res.TypedArray;

import android.os.Bundle;
import android.support.v4.content.SharedPreferencesCompat;
import android.support.v7.preference.DialogPreference;
import android.util.AttributeSet;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;

import android.view.ViewGroup;
import android.widget.ActionMenuView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Set;

public class SeekBarPreference extends DialogPreference {
    private SeekBar seekBar = null;
    private TextView valueText = null;

    private float value = 0;

    /**
     * The SeekBarPreference constructor.
     *
     * @param context of this preference.
     * @param attrs   custom xml attributes.
     */
    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setValue(float f) {
        this.value = f;

        if (this.shouldPersist()) {
            SharedPreferences.Editor editor = this.getPreferenceManager().getSharedPreferences().edit();
            editor.putFloat(this.getKey(), value);
            SharedPreferencesCompat.EditorCompat.getInstance().apply(editor);
        }
    }

    public float getValue() {
        return value;
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            // Restore existing state
            value = this.getPersistedFloat(1);
        } else {
            // Set default state from the XML attribute
            value = (Float) defaultValue;
            persistFloat(value);
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getFloat(index, 0);
    }
}
