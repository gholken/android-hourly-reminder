package com.github.axet.hourlyreminder.fragments;

import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.widget.ContentFrameLayout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.github.axet.hourlyreminder.R;
import com.github.axet.hourlyreminder.app.HourlyApplication;
import com.github.axet.hourlyreminder.app.Sound;
import com.github.axet.hourlyreminder.layouts.HoursDialogFragment;
import com.github.axet.hourlyreminder.layouts.SeekBarPreference;
import com.github.axet.hourlyreminder.layouts.SeekBarPreferenceDialogFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class GeneralPreferenceFragment extends PreferenceFragment implements PreferenceFragment.OnPreferenceDisplayDialogCallback {

    Sound sound;

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference.getKey().equals(HourlyApplication.PREFERENCE_HOURS)) {
                List sortedList = new ArrayList((Set) value);
                preference.setSummary(HourlyApplication.getHoursString(preference.getContext(), sortedList));
            } else if (preference instanceof SeekBarPreference) {
                float f = (Float) value;
                preference.setSummary((int) (f * 100) + "%");
            } else if (preference instanceof android.support.v14.preference.MultiSelectListPreference) {
                List sortedList = new ArrayList((Set) value);
                Collections.sort(sortedList);

                preference.setSummary(sortedList.toString());
            } else if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);
            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getAll().get(preference.getKey()));
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
    }

    @Override
    public Fragment getCallbackFragment() {
        return this;
    }

    @Override
    public boolean onPreferenceDisplayDialog(PreferenceFragment preferenceFragment, Preference preference) {
        if (preference instanceof SeekBarPreference) {
            SeekBarPreferenceDialogFragment f = SeekBarPreferenceDialogFragment.newInstance(preference.getKey());
            ((DialogFragment) f).setTargetFragment(this, 0);
            ((DialogFragment) f).show(this.getFragmentManager(), "android.support.v14.preference.PreferenceFragment.DIALOG");
            return true;
        }

        if (preference.getKey().equals(HourlyApplication.PREFERENCE_HOURS)) {
            HoursDialogFragment f = HoursDialogFragment.newInstance(preference.getKey());
            ((DialogFragment) f).setTargetFragment(this, 0);
            ((DialogFragment) f).show(this.getFragmentManager(), "android.support.v14.preference.PreferenceFragment.DIALOG");
            return true;
        }

        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_general);
        setHasOptionsMenu(true);

        sound = new Sound(getActivity());

        // 23 SDK requires to be Alarm to be percice on time
        if (Build.VERSION.SDK_INT < 23)
            getPreferenceScreen().removePreference(findPreference(HourlyApplication.PREFERENCE_ALARM));

        bindPreferenceSummaryToValue(findPreference(HourlyApplication.PREFERENCE_HOURS));
        bindPreferenceSummaryToValue(findPreference(HourlyApplication.PREFERENCE_VOLUME));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        {
            final Context context = inflater.getContext();
            ViewGroup layout = (ViewGroup) view.findViewById(R.id.list_container);
            FloatingActionButton f = new FloatingActionButton(context);
            f.setImageResource(R.drawable.play);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ContentFrameLayout.LayoutParams.WRAP_CONTENT, ContentFrameLayout.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.BOTTOM | Gravity.RIGHT;
            int dim = (int) getResources().getDimension(R.dimen.fab_margin);
            lp.setMargins(dim, dim, dim, dim);
            f.setLayoutParams(lp);
            layout.addView(f);

            f.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sound.soundAlarm(System.currentTimeMillis());
                }
            });
        }

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (sound != null) {
            sound.close();
            sound = null;
        }
    }
}