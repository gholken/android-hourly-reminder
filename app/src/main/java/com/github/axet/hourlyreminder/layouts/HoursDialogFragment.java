package com.github.axet.hourlyreminder.layouts;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v14.preference.MultiSelectListPreference;
import android.support.v14.preference.PreferenceDialogFragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

import com.github.axet.hourlyreminder.R;
import com.github.axet.hourlyreminder.basics.Reminder;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class HoursDialogFragment extends PreferenceDialogFragment {
    private static final String SAVE_STATE_VALUES = "SeekBarPreferenceDialogFragment.values";
    private static final String SAVE_STATE_CHANGED = "SeekBarPreferenceDialogFragment.changed";
    private static final String SAVE_STATE_ENTRIES = "SeekBarPreferenceDialogFragment.entries";
    private static final String SAVE_STATE_ENTRY_VALUES = "SeekBarPreferenceDialogFragment.entryValues";
    private boolean mPreferenceChanged;
    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;


    int[] ids = new int[]{
            R.id.hours_00,
            R.id.hours_01,
            R.id.hours_02,
            R.id.hours_03,
            R.id.hours_04,
            R.id.hours_05,
            R.id.hours_06,
            R.id.hours_07,
            R.id.hours_08,
            R.id.hours_09,
            R.id.hours_10,
            R.id.hours_11,
            R.id.hours_12,
            R.id.hours_13,
            R.id.hours_14,
            R.id.hours_15,
            R.id.hours_16,
            R.id.hours_17,
            R.id.hours_18,
            R.id.hours_19,
            R.id.hours_20,
            R.id.hours_21,
            R.id.hours_22,
            R.id.hours_23,
    };

    Set<String> values;

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

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        Context context = builder.getContext();

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View view = inflater.inflate(R.layout.hours, null, false);

        MultiSelectListPreference preference = (MultiSelectListPreference) getPreference();
        Set<String> values = preference.getValues();

        for (int i = 0; i < 24; i++) {
            CheckBox c = (CheckBox) view.findViewById(ids[i]);
            String h = Reminder.format(i);
            boolean b = values.contains(h);
            c.setChecked(b);

            c.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    changed(view);
                }
            });
        }

        builder.setView(view);
    }

    void changed(View view) {
        mPreferenceChanged = true;

        Set<String> s = new TreeSet<>();
        for (int i = 0; i < 24; i++) {
            CheckBox c = (CheckBox) view.findViewById(ids[i]);
            String h = Reminder.format(i);
            if(c.isChecked()) {
                s.add(h);
            }
        }
        values = s;
    }


    @Override
    public void onDialogClosed(boolean positiveResult) {
        MultiSelectListPreference preference = (MultiSelectListPreference) getPreference();
        if (positiveResult && this.mPreferenceChanged) {
            if (preference.callChangeListener(values)) {
                preference.setValues(values);
            }
        }

        this.mPreferenceChanged = false;
    }
}
