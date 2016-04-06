package com.github.axet.hourlyreminder.fragments;

import android.Manifest;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.design.widget.FloatingActionButton;
import android.support.v13.app.FragmentCompat;
import android.support.v14.preference.PreferenceFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.Preference;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.support.v7.widget.ContentFrameLayout;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.github.axet.androidlibrary.widgets.SeekBarPreference;
import com.github.axet.androidlibrary.widgets.SeekBarPreferenceDialogFragment;
import com.github.axet.hourlyreminder.R;
import com.github.axet.hourlyreminder.app.HourlyApplication;
import com.github.axet.hourlyreminder.app.Sound;
import com.github.axet.hourlyreminder.basics.Reminder;
import com.github.axet.hourlyreminder.layouts.HoursDialogFragment;

public class SettingsFragment extends PreferenceFragment implements PreferenceFragment.OnPreferenceDisplayDialogCallback {
    Sound sound;

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
        addPreferencesFromResource(R.xml.pref_settings);
        setHasOptionsMenu(true);

        sound = new Sound(getActivity());

        // 23 SDK requires to be Alarm to be percice on time
        if (Build.VERSION.SDK_INT < 23)
            getPreferenceScreen().removePreference(findPreference(HourlyApplication.PREFERENCE_ALARM));

        RemindersFragment.bindPreferenceSummaryToValue(findPreference(HourlyApplication.PREFERENCE_VOLUME));

        if (DateFormat.is24HourFormat(getActivity())) {
            getPreferenceScreen().removePreference(findPreference(HourlyApplication.PREFERENCE_SPEAK_AMPM));
        }

        RemindersFragment.bindPreferenceSummaryToValue(findPreference(HourlyApplication.PREFERENCE_THEME));

        findPreference(HourlyApplication.PREFERENCE_CALLSILENCE).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if (!permitted(PERMISSIONS)) {
                    permitted();
                    return false;
                }
                return true;
            }
        });
    }

    void setPhone() {
        SwitchPreferenceCompat s = (SwitchPreferenceCompat) findPreference(HourlyApplication.PREFERENCE_CALLSILENCE);
        s.setChecked(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case 1:
                if (permitted(PERMISSIONS))
                    setPhone();
                else
                    Toast.makeText(getActivity(), "Not permitted", Toast.LENGTH_SHORT).show();
        }
    }

    public static final String[] PERMISSIONS = new String[]{Manifest.permission.READ_PHONE_STATE};

    boolean permitted(String[] ss) {
        for (String s : ss) {
            if (ContextCompat.checkSelfPermission(getActivity(), s) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    boolean permitted() {
        for (String s : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(getActivity(), s) != PackageManager.PERMISSION_GRANTED) {
                FragmentCompat.requestPermissions(this, PERMISSIONS, 1);
                return false;
            }
        }
        return true;
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
                    sound.soundReminder(System.currentTimeMillis());
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
