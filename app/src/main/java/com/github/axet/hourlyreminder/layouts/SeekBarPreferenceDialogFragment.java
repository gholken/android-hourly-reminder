package com.github.axet.hourlyreminder.layouts;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.support.annotation.NonNull;
import android.support.v14.preference.PreferenceDialogFragment;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class SeekBarPreferenceDialogFragment extends PreferenceDialogFragment {
    private static final String SAVE_STATE_VALUES = "SeekBarPreferenceDialogFragment.values";
    private static final String SAVE_STATE_CHANGED = "SeekBarPreferenceDialogFragment.changed";
    private static final String SAVE_STATE_ENTRIES = "SeekBarPreferenceDialogFragment.entries";
    private static final String SAVE_STATE_ENTRY_VALUES = "SeekBarPreferenceDialogFragment.entryValues";
    private boolean mPreferenceChanged;

    float value;

    public SeekBarPreferenceDialogFragment() {
    }

    public static SeekBarPreferenceDialogFragment newInstance(String key) {
        SeekBarPreferenceDialogFragment fragment = new SeekBarPreferenceDialogFragment();
        Bundle b = new Bundle(1);
        b.putString("key", key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            value = savedInstanceState.getFloat("value");
            mPreferenceChanged = savedInstanceState.getBoolean("changed");
        } else {
            SeekBarPreference preference = (SeekBarPreference) getPreference();
            value = preference.getValue();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putFloat("value", value);
        outState.putBoolean("changed", mPreferenceChanged);
    }

    int getThemeColor(int id) {
        TypedValue typedValue = new TypedValue();
        Context context = getActivity();
        Resources.Theme theme = context.getTheme();
        if (theme.resolveAttribute(id, typedValue, true)) {
            if (Build.VERSION.SDK_INT >= 23)
                return context.getResources().getColor(typedValue.resourceId, theme);
            else
                return context.getResources().getColor(typedValue.resourceId);
        } else {
            return Color.TRANSPARENT;
        }
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        Context context = builder.getContext();

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams lp;

        SeekBar seekBar = null;

        lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        seekBar = new SeekBar(context);
        layout.addView(seekBar, lp);

        lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        final TextView valueText = new TextView(context);
        valueText.setText("" + value);
        valueText.setTextColor(getThemeColor(android.R.attr.textColorSecondary));
        layout.addView(valueText, lp);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int newValue, boolean fromUser) {
                mPreferenceChanged = true;
                value = newValue / 100f;
                valueText.setText(String.valueOf((int) (value * 100)) + " %");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        seekBar.setKeyProgressIncrement(1);
        seekBar.setMax(100);
        seekBar.setProgress((int) (value * 100));

        builder.setView(layout);
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        SeekBarPreference preference = (SeekBarPreference) getPreference();
        if (positiveResult && this.mPreferenceChanged) {
            if (preference.callChangeListener(value)) {
                preference.setValue(value);
            }
        }

        this.mPreferenceChanged = false;
    }
}
