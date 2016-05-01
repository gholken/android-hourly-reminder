package com.github.axet.hourlyreminder.fragments;

import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceGroupAdapter;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.support.v7.widget.ContentFrameLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.github.axet.androidlibrary.widgets.ThemeUtils;
import com.github.axet.hourlyreminder.R;
import com.github.axet.hourlyreminder.app.HourlyApplication;
import com.github.axet.hourlyreminder.app.Sound;
import com.github.axet.hourlyreminder.layouts.HoursDialogFragment;
import com.github.axet.androidlibrary.widgets.SeekBarPreference;
import com.github.axet.androidlibrary.widgets.SeekBarPreferenceDialogFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class RemindersFragment extends PreferenceFragment implements PreferenceFragment.OnPreferenceDisplayDialogCallback, SharedPreferences.OnSharedPreferenceChangeListener  {

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

    //
    // support library 23.0.1 and api 23 failed with:
    //
    // https://code.google.com/p/android/issues/detail?id=85392#makechanges
    //
    // http://stackoverflow.com/questions/30336635
    //
    // To fix this, we need create our own PreferenceGroupAdapter
    //
    class PreferenceGroupAdapterFix extends PreferenceGroupAdapter {
        public PreferenceGroupAdapterFix(PreferenceGroup preferenceGroup) {
            super(preferenceGroup);
        }

        public void onBindViewHolder(PreferenceViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);

            // LinerLayoutManager.onLayoutChildren() call detach(), then fill() which cause:
            //
            // onBindViewHolder cause SwitchCompat.setCheck() call on currently detached view !!!
            // so no animation starts.
            // then called RecyclerView.attachViewToParent()
        }

        public void onViewAttachedToWindow(PreferenceViewHolder holder) {
            super.onViewAttachedToWindow(holder);
        }

        public void onViewDetachedFromWindow(PreferenceViewHolder holder) {
            super.onViewDetachedFromWindow(holder);
        }
    }

    // LinearLayoutManager llm;

    class LinearLayoutManagerFix extends LinearLayoutManager {
        public LinearLayoutManagerFix(Context context) {
            super(context);
        }

        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            super.onLayoutChildren(recycler, state);
        }

        @Override
        public void addView(View child) {
            if (child.getParent() != null)
                return;
            super.addView(child);
        }

        @Override
        public void addView(View child, int index) {
            if (child.getParent() != null)
                return;
            super.addView(child, index);
        }

        @Override
        public void addDisappearingView(View child) {
            if (child.getParent() != null)
                return;
            super.addDisappearingView(child);
        }

        @Override
        public void addDisappearingView(View child, int index) {
            if (child.getParent() != null)
                return;
            super.addDisappearingView(child, index);
        }
    }

    class RecyclerViewFix extends RecyclerView {
        public RecyclerViewFix(Context context) {
            super(context);
        }

        @Override
        protected void attachViewToParent(View child, int index, ViewGroup.LayoutParams params) {
            super.attachViewToParent(child, index, params);
        }

        @Override
        protected void detachViewFromParent(View child) {
            super.detachViewFromParent(child);
        }

        @Override
        protected void detachViewFromParent(int index) {
            super.detachViewFromParent(index);
        }
    }

//    @Override
//    public RecyclerView.LayoutManager onCreateLayoutManager() {
//        return llm = new LinearLayoutManagerFix(this.getActivity());
//    }

//    @Override
//    protected RecyclerView.Adapter onCreateAdapter(PreferenceScreen preferenceScreen) {
//        return new PreferenceGroupAdapterFix(preferenceScreen);
//    }

//    @Override
//    public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
//        //RecyclerView recyclerView = (RecyclerView)inflater.inflate(android.support.v14.preference.R.layout.preference_recyclerview, parent, false);
//        RecyclerView recyclerView = new RecyclerViewFix(getActivity());
//        recyclerView.setLayoutManager(this.onCreateLayoutManager());
//        return recyclerView;
//    }

    public static void bindPreferenceSummaryToValue(Preference preference) {
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
        addPreferencesFromResource(R.xml.pref_reminders);
        setHasOptionsMenu(true);

        sound = new Sound(getActivity());

        bindPreferenceSummaryToValue(findPreference(HourlyApplication.PREFERENCE_HOURS));

        {
            Preference p = findPreference(HourlyApplication.PREFERENCE_REPEAT);
            p.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    // it is only for 23 api phones and up. since only alarms can trigs often then 15 mins.
                    if (Build.VERSION.SDK_INT >= 23) {
                        int min = Integer.parseInt((String) o);
                        if (min < 15) {
                            SharedPreferences shared = android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences(getActivity());
                            boolean b = shared.getBoolean(HourlyApplication.PREFERENCE_ALARM, false);
                            if (!b) {
                                Toast.makeText(getActivity(), "Setting alarm type to 'alarm', check settings.", Toast.LENGTH_SHORT).show();
                                SharedPreferences.Editor edit = shared.edit();
                                edit.putBoolean(HourlyApplication.PREFERENCE_ALARM, true);
                                edit.commit();
                            }
                        }
                    }
                    return sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, o);
                }
            });
            sBindPreferenceSummaryToValueListener.onPreferenceChange(p,
                    PreferenceManager
                            .getDefaultSharedPreferences(p.getContext())
                            .getAll().get(p.getKey()));
        }

        findPreference(HourlyApplication.PREFERENCE_BEEP).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                Boolean beep = (Boolean) o;
                boolean speak = ((SwitchPreferenceCompat) findPreference(HourlyApplication.PREFERENCE_SPEAK)).isChecked();
                if (!beep && !speak) {
                    annonce(getActivity());
                }
                return true;
            }
        });
        findPreference(HourlyApplication.PREFERENCE_SPEAK).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                Boolean speak = (Boolean) o;
                boolean beep = ((SwitchPreferenceCompat) findPreference(HourlyApplication.PREFERENCE_BEEP)).isChecked();
                if (!beep && !speak) {
                    annonce(getActivity());
                }
                return true;
            }
        });

        SharedPreferences shared = android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences(getActivity());
        shared.registerOnSharedPreferenceChangeListener(this);
    }

    static void annonce(Context context) {
        SharedPreferences shared = android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences(context);
        boolean v = shared.getBoolean(HourlyApplication.PREFERENCE_VIBRATE, false);
        annonce(context, v);
    }

    static void annonce(Context context, boolean v) {
        if (v) {
            Toast.makeText(context, "Reminders set to vibrate only", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(context, "Reminders set to 'silence'", Toast.LENGTH_LONG).show();
        }
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

            RecyclerView v = getListView();
            v.setClipToPadding(false);
            v.setPadding(0, 0, 0, ThemeUtils.dp2px(getActivity(), 61) + dim);
        }

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        SharedPreferences shared = android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences(getActivity());
        shared.unregisterOnSharedPreferenceChangeListener(this);

        if (sound != null) {
            sound.close();
            sound = null;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals(HourlyApplication.PREFERENCE_REPEAT)) {
            Preference p = findPreference(HourlyApplication.PREFERENCE_REPEAT);
            sBindPreferenceSummaryToValueListener.onPreferenceChange(p,
                    PreferenceManager
                            .getDefaultSharedPreferences(p.getContext())
                            .getAll().get(p.getKey()));
            ((ListPreference) p).setValue(sharedPreferences.getString(HourlyApplication.PREFERENCE_REPEAT, "60"));
        }
    }
}
