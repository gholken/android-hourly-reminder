package com.github.axet.hourlyreminder;

import android.content.Context;
import android.content.DialogInterface;

import android.content.res.TypedArray;

import android.preference.DialogPreference;

import android.util.AttributeSet;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;

import android.view.ViewGroup;
import android.widget.ActionMenuView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class VolumeDialogPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener {
    // Layout widgets.
    private SeekBar seekBar = null;
    private TextView valueText = null;

    private float value = 0;

    /**
     * The VolumeDialogPreference constructor.
     *
     * @param context of this preference.
     * @param attrs   custom xml attributes.
     */
    public VolumeDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * {@inheritDoc}
     */
    protected View onCreateDialogView() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams lp;

        lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        seekBar = new SeekBar(getContext());
        layout.addView(seekBar, lp);

        lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        valueText = new TextView(getContext());
        layout.addView(valueText, lp);

        seekBar.setOnSeekBarChangeListener(this);
        seekBar.setKeyProgressIncrement(1);
        seekBar.setMax(100);
        seekBar.setProgress((int) (value * 100));

        return layout;
    }

    /**
     * {@inheritDoc}
     */
    public void onProgressChanged(SeekBar seek, int newValue,
                                  boolean fromTouch) {
        value = newValue / 100f;
        valueText.setText(String.valueOf((int) (value * 100)) + " %");
    }

    /**
     * {@inheritDoc}
     */
    public void onStartTrackingTouch(SeekBar seek) {
    }

    /**
     * {@inheritDoc}
     */
    public void onStopTrackingTouch(SeekBar seek) {
    }

    /**
     * {@inheritDoc}
     */
    public void onClick(DialogInterface dialog, int which) {
        // if the positive button is clicked, we persist the value.
        if (which == DialogInterface.BUTTON_POSITIVE) {
            if (shouldPersist()) {
                persistFloat(value);
                callChangeListener(value);
            }
        }

        super.onClick(dialog, which);
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
