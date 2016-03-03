package com.github.axet.hourlyreminder.layouts;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v14.preference.PreferenceDialogFragment;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class SeekBarPreferenceDialogFragment extends PreferenceDialogFragment implements SeekBar.OnSeekBarChangeListener {
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
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        Context context = getActivity().getApplicationContext();

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams lp;

        lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        seekBar = new SeekBar(context);
        layout.addView(seekBar, lp);

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(android.R.attr.cacheColorHint, typedValue, true);
        int color = typedValue.data;

        lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        valueText = new TextView(context);
        valueText.setTextColor(0x55000000|color);
        layout.addView(valueText, lp);

        SeekBarPreference preference = (SeekBarPreference)getPreference();
        value = preference.getValue();

        seekBar.setOnSeekBarChangeListener(this);
        seekBar.setKeyProgressIncrement(1);
        seekBar.setMax(100);
        seekBar.setProgress((int) (value * 100));

        builder.setView(layout);
    }

    public void onProgressChanged(SeekBar seek, int newValue,
                                  boolean fromTouch) {
        mPreferenceChanged = true;
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

    @Override
    public void onDialogClosed(boolean positiveResult) {
        SeekBarPreference preference = (SeekBarPreference)getPreference();
        if (positiveResult && this.mPreferenceChanged) {
            if (preference.callChangeListener(value)) {
                preference.setValue(value);
            }
        }

        this.mPreferenceChanged = false;
    }
}
