package com.github.axet.hourlyreminder.widgets;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v14.preference.MultiSelectListPreference;
import android.support.v14.preference.PreferenceDialogFragment;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import com.github.axet.hourlyreminder.R;
import com.github.axet.hourlyreminder.app.HourlyApplication;
import com.github.axet.hourlyreminder.basics.Alarm;
import com.github.axet.hourlyreminder.basics.Reminder;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

public class DaysDialogFragment extends PreferenceDialogFragment {
    private boolean mPreferenceChanged;

    int startweek = 0;

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

    public DaysDialogFragment() {
    }

    public static DaysDialogFragment newInstance(String key) {
        DaysDialogFragment fragment = new DaysDialogFragment();
        Bundle b = new Bundle(1);
        b.putString("key", key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            values = new TreeSet<String>(Arrays.asList(savedInstanceState.getStringArray("values")));
            mPreferenceChanged = savedInstanceState.getBoolean("changed");
        } else {
            MultiSelectListPreference preference = (MultiSelectListPreference) getPreference();
            values = new TreeSet<>(preference.getValues());
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putStringArray("values", values.toArray(new String[]{}));
        outState.putBoolean("changed", mPreferenceChanged);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        Context context = builder.getContext();

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View view = inflater.inflate(R.layout.days, null, false);

        startweek = 0;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String s = prefs.getString(HourlyApplication.PREFERENCE_WEEKSTART, "");
        for (int i = 0; i < Alarm.DAYS.length; i++) {
            if (s.equals(getString(Alarm.DAYS[i]))) {
                startweek = i;
                break;
            }
        }

        LinearLayout weekdaysValues = (LinearLayout) view.findViewById(R.id.alarm_week);

        for (int i = 0; i < weekdaysValues.getChildCount(); i++) {
            final CheckBox child = (CheckBox) weekdaysValues.getChildAt(i);
            if (child instanceof CheckBox) {
                String v = getString(Alarm.DAYS[startweek]);
                child.setText(v.substring(0, 1));
                final int week = Alarm.EVERYDAY[startweek];

                child.setTag(v);

                child.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        changed(child);
                    }
                });
                boolean b = values.contains(v);
                child.setChecked(b);
                startweek++;
                if (startweek >= Alarm.DAYS.length)
                    startweek = 0;
            }
        }

        builder.setView(view);
    }

    void changed(CheckBox view) {
        mPreferenceChanged = true;

        String tag = (String) view.getTag();

        if (view.isChecked()) {
            values.add(tag);
        } else {
            values.remove(tag);
        }

        values = new TreeSet<>(values);
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
