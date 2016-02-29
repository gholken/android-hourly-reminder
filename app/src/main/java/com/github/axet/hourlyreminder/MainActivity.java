package com.github.axet.hourlyreminder;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.widget.ContentFrameLayout;

import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof SeekBarPreference) {
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

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                ((HourlyApplication) getApplicationContext()).soundAlarm();
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

        ((HourlyApplication) getApplicationContext()).updateAlerts(getApplicationContext());
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        HourlyApplication.updateAlerts(getApplicationContext());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class AlarmsAdapter implements ListAdapter {
        ArrayList<DataSetObserver> listeners = new ArrayList<>();
        int count;
        int selected = -1;

        static final int TYPE_NORMAL = 0;
        static final int TYPE_DETAIL = 1;

        static final int[] ALL = {TYPE_NORMAL, TYPE_DETAIL};

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
            listeners.add(observer);
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            listeners.remove(observer);
        }

        @Override
        public int getCount() {
            return count;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getView(final int position, View convertView, final ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(LAYOUT_INFLATER_SERVICE);

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.alarm, parent, false);
                convertView.setTag(-1);
            }

            if (selected == position) {
                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        select(-1);
                    }
                });

                if ((int) convertView.getTag() == TYPE_NORMAL) {
                    AlarmExpandAnimation e = new AlarmExpandAnimation(convertView);
                    convertView.clearAnimation();
                    convertView.startAnimation(e);
                } else {
                    convertView.findViewById(R.id.alarm_detailed).setVisibility(View.VISIBLE);
                    convertView.findViewById(R.id.alarm_bottom).setVisibility(View.VISIBLE);
                    convertView.findViewById(R.id.alarm_compact).setVisibility(View.GONE);
                }
                convertView.setTag(TYPE_DETAIL);

                return convertView;
            }

            if ((int) convertView.getTag() == TYPE_DETAIL) {
                AlarmCollapseAnimation e = new AlarmCollapseAnimation(convertView);
                convertView.clearAnimation();
                convertView.startAnimation(e);
            } else {
                convertView.findViewById(R.id.alarm_detailed).setVisibility(View.GONE);
                convertView.findViewById(R.id.alarm_bottom).setVisibility(View.GONE);
                convertView.findViewById(R.id.alarm_compact).setVisibility(View.VISIBLE);
            }
            convertView.setTag(TYPE_NORMAL);

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    select(position);
                }
            });

            return convertView;
        }

        void select(int pos) {
            selected = pos;
            changed();
        }

        @Override
        public int getItemViewType(int position) {
            return TYPE_NORMAL;
        }

        @Override
        public int getViewTypeCount() {
            return ALL.length;
        }

        @Override
        public boolean isEmpty() {
            return count == 0;
        }

        public void addAlarm() {
            count++;
            changed();
        }

        void changed() {
            for (DataSetObserver l : listeners) {
                l.onChanged();
            }
        }
    }

    public static class AlarmsFragment extends Fragment {
        AlarmsAdapter adapter;

        public AlarmsFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            adapter = new AlarmsAdapter();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_alarms, container, false);
            final ListView list = (ListView) rootView.findViewById(R.id.section_label);
            list.setAdapter(adapter);
            FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (Build.VERSION.SDK_INT >= 19)
                        TransitionManager.beginDelayedTransition(list);
                    adapter.addAlarm();
                }
            });
            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new GeneralPreferenceFragment();
            }
            // getItem is called to instantiate the fragment for the given page.
            // Return a AlarmsFragment (defined as a static inner class below).
            return new AlarmsFragment();
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Hourly Reminders";
                case 1:
                    return "Custom Alarms";
            }
            return null;
        }
    }

    public static class GeneralPreferenceFragment extends PreferenceFragment implements PreferenceFragment.OnPreferenceDisplayDialogCallback {
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

            return false;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            // 23 SDK requires to be Alarm to be percice on time
            if (Build.VERSION.SDK_INT < 23)
                getPreferenceScreen().removePreference(findPreference("alarm"));

            bindPreferenceSummaryToValue(findPreference("hours"));
            bindPreferenceSummaryToValue(findPreference("volume"));
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
                        ((HourlyApplication) context.getApplicationContext()).soundAlarm();
                    }
                });
            }

            return view;
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}
