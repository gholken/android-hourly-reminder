package com.github.axet.hourlyreminder.layouts;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v14.preference.PreferenceDialogFragment;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class HoursDialogFragment extends PreferenceDialogFragment {
    private static final String SAVE_STATE_VALUES = "SeekBarPreferenceDialogFragment.values";
    private static final String SAVE_STATE_CHANGED = "SeekBarPreferenceDialogFragment.changed";
    private static final String SAVE_STATE_ENTRIES = "SeekBarPreferenceDialogFragment.entries";
    private static final String SAVE_STATE_ENTRY_VALUES = "SeekBarPreferenceDialogFragment.entryValues";
    private boolean mPreferenceChanged;
    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;

    SeekBar seekBar = null;
    TextView valueText = null;
    float value;

    public HoursDialogFragment() {
    }

    public static HoursDialogFragment newInstance(String key) {
        HoursDialogFragment fragment = new HoursDialogFragment();
        Bundle b = new Bundle(1);
        b.putString("key", key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
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

        Context context = getActivity();

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams lp;

        lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        seekBar = new SeekBar(context);
        layout.addView(seekBar, lp);

        lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        valueText = new TextView(context);
        valueText.setTextColor(getThemeColor(android.R.attr.textColorSecondary));
        layout.addView(valueText, lp);

        SeekBarPreference preference = (SeekBarPreference) getPreference();
        value = preference.getValue();

        seekBar.setKeyProgressIncrement(1);
        seekBar.setMax(100);
        seekBar.setProgress((int) (value * 100));

        builder.setView(layout);
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
