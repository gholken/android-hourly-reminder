package com.github.axet.hourlyreminder.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v13.app.FragmentCompat;
import android.support.v14.preference.PreferenceFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceGroupAdapter;
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.support.v7.widget.ContentFrameLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.github.axet.androidlibrary.widgets.FilePathPreference;
import com.github.axet.androidlibrary.widgets.OpenFileDialog;
import com.github.axet.androidlibrary.widgets.RingtonePreference;
import com.github.axet.androidlibrary.widgets.ThemeUtils;
import com.github.axet.hourlyreminder.R;
import com.github.axet.hourlyreminder.app.HourlyApplication;
import com.github.axet.hourlyreminder.app.Sound;
import com.github.axet.hourlyreminder.basics.Reminder;
import com.github.axet.hourlyreminder.widgets.DaysDialogFragment;
import com.github.axet.hourlyreminder.widgets.HoursDialogFragment;
import com.github.axet.androidlibrary.widgets.SeekBarPreference;
import com.github.axet.androidlibrary.widgets.SeekBarPreferenceDialogFragment;
import com.github.axet.hourlyreminder.widgets.CustomSoundListPreference;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class RemindersFragment extends PreferenceFragment implements PreferenceFragment.OnPreferenceDisplayDialogCallback, SharedPreferences.OnSharedPreferenceChangeListener {

    public final static Uri DEFAULT_NOTIFICATION = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

    Sound sound;

    static String getTitle(Context context, String t) {
        String s = HourlyApplication.getTitle(context, t);
        if (s == null)
            s = "None";
        return s;
    }

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference.getKey().equals(HourlyApplication.PREFERENCE_RINGTONE)) {
                preference.setSummary(getTitle(preference.getContext(), stringValue));
                return true;
            }

            if (preference.getKey().equals(HourlyApplication.PREFERENCE_CUSTOM_SOUND)) {
                CustomSoundListPreference pp = (CustomSoundListPreference) preference;
                pp.update(stringValue);
                // keep update List type pref
                // return true;
            }

            if (preference.getKey().equals(HourlyApplication.PREFERENCE_HOURS)) {
                List sortedList = new ArrayList((Set) value);
                preference.setSummary(HourlyApplication.getHoursString(preference.getContext(), sortedList));
                return true;
            }

            if (preference.getKey().equals(HourlyApplication.PREFERENCE_DAYS)) {
                Reminder r = new Reminder(preference.getContext(), (Set) value);
                preference.setSummary(r.getDays());
                return true;
            }

            if (preference instanceof FilePathPreference) {
                if (stringValue.isEmpty())
                    stringValue = preference.getContext().getString(R.string.not_selected);
                preference.setSummary(stringValue);
                return true;
            }

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
    public boolean onPreferenceDisplayDialog(PreferenceFragment preferenceFragment, final Preference preference) {
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

        if (preference.getKey().equals(HourlyApplication.PREFERENCE_DAYS)) {
            DaysDialogFragment f = DaysDialogFragment.newInstance(preference.getKey());
            ((DialogFragment) f).setTargetFragment(this, 0);
            ((DialogFragment) f).show(this.getFragmentManager(), "android.support.v14.preference.PreferenceFragment.DIALOG");
            return true;
        }

        if (preference.getKey().equals(HourlyApplication.PREFERENCE_SOUND)) {
            if (permitted(1))
                selectFile();
            return true;
        }

        if (preference.getKey().equals(HourlyApplication.PREFERENCE_RINGTONE)) {
            if (permitted(2))
                selectRingtone();
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

        bindPreferenceSummaryToValue(findPreference(HourlyApplication.PREFERENCE_DAYS));

        bindPreferenceSummaryToValue(findPreference(HourlyApplication.PREFERENCE_SOUND));

        bindPreferenceSummaryToValue(findPreference(HourlyApplication.PREFERENCE_CUSTOM_SOUND));

        bindPreferenceSummaryToValue(findPreference(HourlyApplication.PREFERENCE_RINGTONE));

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
                                Toast.makeText(getActivity(), R.string.alarm_type_alarm, Toast.LENGTH_SHORT).show();
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
            Toast.makeText(context, R.string.reminders_vibrate, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(context, R.string.reminders_silence, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        {
            final Context context = inflater.getContext();
            ViewGroup layout = (ViewGroup) view.findViewById(R.id.list_container);

            int dim = (int) getResources().getDimension(R.dimen.fab_margin);

            int pad = ThemeUtils.dp2px(context, 10);
            int top = (int) getResources().getDimension(R.dimen.appbar_padding_top);
            if (Build.VERSION.SDK_INT >= 17) { // so, it bugged only on 16
                pad = 0;
                top = 0;
            }
            RecyclerView v = getListView();
            v.setClipToPadding(false);
            v.setPadding(pad, top, pad, pad + ThemeUtils.dp2px(getActivity(), 61) + dim);

            FloatingActionButton f = new FloatingActionButton(context);
            f.setImageResource(R.drawable.play);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ContentFrameLayout.LayoutParams.WRAP_CONTENT, ContentFrameLayout.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.BOTTOM | Gravity.RIGHT;
            lp.setMargins(dim, dim, dim, dim);
            f.setLayoutParams(lp);
            layout.addView(f);

            f.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (sound.playerClose()) {
                        return;
                    }
                    sound.soundReminder(System.currentTimeMillis());
                }
            });
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (requestCode == 0) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            EditTextPreference edit = (EditTextPreference) findPreference(HourlyApplication.PREFERENCE_RINGTONE);
            String text = "";

            if (uri != null) {
                text = uri.toString();
            }

            edit.setText(text);
            edit.getOnPreferenceChangeListener().onPreferenceChange(edit, text);
            return;
        }
    }

    void selectFile() {
        final FilePathPreference pp = (FilePathPreference) findPreference(HourlyApplication.PREFERENCE_SOUND);

        final OpenFileDialog f = new OpenFileDialog(getActivity());

        String path = pp.getText();

        if (path == null || path.isEmpty()) {
            String def = Environment.getExternalStorageDirectory().getPath();
            SharedPreferences shared = android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences(getActivity());
            path = shared.getString(HourlyApplication.PREFERENCE_LAST_PATH, def);
        }

        f.setReadonly(true);
        f.setCurrentPath(new File(path));
        f.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                File ff = f.getCurrentPath();

                SharedPreferences shared = android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences(getActivity());
                shared.edit().putString(HourlyApplication.PREFERENCE_LAST_PATH, ff.getParent()).commit();

                String fileName = ff.getPath();
                if (pp.callChangeListener(fileName)) {
                    pp.setText(fileName);
                }
            }
        });
        f.show();
    }

    void selectRingtone() {
        // W/MediaPlayer: Couldn't open file on client side; trying server side:
        // java.lang.SecurityException: Permission Denial: reading com.android.providers.media.MediaProvider uri content://media/external/audio/media/17722
        // from pid=697, uid=10204
        // requires android.permission.READ_EXTERNAL_STORAGE, or grantUriPermission()
        //
        // context.grantUriPermission("com.android.providers.media.MediaProvider", Uri.parse("content://media/external/images/media"), Intent.FLAG_GRANT_READ_URI_PERMISSION);

        RingtonePreference pp = (RingtonePreference) findPreference(HourlyApplication.PREFERENCE_RINGTONE);
        Uri uri = null;
        if (!pp.getText().isEmpty()) {
            uri = Uri.parse(pp.getText());
        }
        startActivityForResult(new Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getActivity().getString(R.string.Reminder))
                .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, uri), 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case 1:
                if (permitted(permissions))
                    selectFile();
                else
                    Toast.makeText(getActivity(), R.string.NotPermitted, Toast.LENGTH_SHORT).show();
                break;
            case 2:
                if (permitted(permissions))
                    selectRingtone();
                else
                    Toast.makeText(getActivity(), R.string.NotPermitted, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    public static final String[] PERMISSIONS = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};

    boolean permitted(String[] ss) {
        for (String s : ss) {
            if (ContextCompat.checkSelfPermission(getActivity(), s) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    boolean permitted(int code) {
        for (String s : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(getActivity(), s) != PackageManager.PERMISSION_GRANTED) {
                FragmentCompat.requestPermissions(this, PERMISSIONS, code);
                return false;
            }
        }
        return true;
    }
}
