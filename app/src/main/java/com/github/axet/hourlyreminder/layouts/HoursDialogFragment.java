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
import android.text.format.DateFormat;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

import com.github.axet.hourlyreminder.R;
import com.github.axet.hourlyreminder.basics.Reminder;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class HoursDialogFragment extends PreferenceDialogFragment {
    private boolean mPreferenceChanged;

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

    String[] AMPM = new String[]{
            "12",
            "1",
            "2",
            "3",
            "4",
            "5",
            "6",
            "7",
            "8",
            "9",
            "10",
            "11",
            "12",
            "1",
            "2",
            "3",
            "4",
            "5",
            "6",
            "7",
            "8",
            "9",
            "10",
            "11",
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
        final View view = inflater.inflate(R.layout.hours, null, false);

        for (int i = 0; i < ids.length; i++) {
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
            if (!DateFormat.is24HourFormat(context)) {
                c.setText(AMPM[i]);
            }
        }

        View am = view.findViewById(R.id.hours_am);
        View pm = view.findViewById(R.id.hours_pm);

        if (DateFormat.is24HourFormat(context)) {
            am.setVisibility(View.GONE);
            pm.setVisibility(View.GONE);
        }else {
            am.setVisibility(View.VISIBLE);
            pm.setVisibility(View.VISIBLE);
        }

        builder.setView(view);
    }

    void changed(View view) {
        mPreferenceChanged = true;

        Set<String> s = new TreeSet<>();
        for (int i = 0; i < ids.length; i++) {
            CheckBox c = (CheckBox) view.findViewById(ids[i]);
            String h = Reminder.format(i);
            if (c.isChecked()) {
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
