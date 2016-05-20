package com.github.axet.hourlyreminder.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.DataSetObserver;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v13.app.FragmentCompat;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.github.axet.androidlibrary.animations.MarginAnimation;
import com.github.axet.androidlibrary.animations.RemoveItemAnimation;
import com.github.axet.androidlibrary.widgets.OpenFileDialog;
import com.github.axet.hourlyreminder.R;
import com.github.axet.hourlyreminder.animations.AlarmAnimation;
import com.github.axet.hourlyreminder.app.HourlyApplication;
import com.github.axet.hourlyreminder.app.Sound;
import com.github.axet.hourlyreminder.app.Storage;
import com.github.axet.hourlyreminder.basics.Alarm;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

public class AlarmsFragment extends Fragment implements ListAdapter, AbsListView.OnScrollListener, SharedPreferences.OnSharedPreferenceChangeListener {
    static final int TYPE_COLLAPSED = 0;
    static final int TYPE_EXPANDED = 1;
    static final int TYPE_DELETED = 2;

    final int[] ALL = {TYPE_COLLAPSED, TYPE_EXPANDED};

    Alarm fragmentRequestRingtone;

    ListView list;

    ArrayList<DataSetObserver> listeners = new ArrayList<>();
    List<Alarm> alarms = new ArrayList<>();
    long selected = -1;
    int scrollState;
    boolean boxAnimate;
    Handler handler;
    // preview ringtone
    boolean preview;
    Sound sound;
    Storage storage;

    int startweek = 0;

    TreeMap<Long, Integer> viewids = new TreeMap<>();

    public AlarmsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handler = new Handler();

        sound = new Sound(getActivity());
        sound.setVolume(1);

        storage = new Storage(getActivity());

        updateStartWeek();

        alarms = HourlyApplication.loadAlarms(getActivity());
        Collections.sort(alarms, new Alarm.CustomComparator());

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(this);

        if (savedInstanceState != null) {
            selected = savedInstanceState.getLong("selected");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putLong("selected", selected);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_alarms, container, false);
        list = (ListView) rootView.findViewById(R.id.section_label);
        list.setAdapter(this);
        list.setOnScrollListener(this);
        FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Alarm a = new Alarm(getActivity(), System.currentTimeMillis());
                TimePickerDialog d = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
                    // onTimeSet called twice on old phones
                    //
                    // http://stackoverflow.com/questions/19452993
                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            addAlarm(a);
                            HourlyApplication.toastAlarmSet(getActivity(), a);
                        }
                    };

                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        a.setTime(hourOfDay, minute);
                        if (r != null) {
                            r.run();
                            r = null;
                        }
                    }
                }, a.getHour(), a.getMin(), DateFormat.is24HourFormat(getActivity()));
                d.show();
            }
        });

        if (selected > 0)
            list.smoothScrollToPosition(getPosition(selected));

        return rootView;
    }

    int getPosition(long id) {
        for (int i = 0; i < alarms.size(); i++) {
            if (alarms.get(i).id == id) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        boxAnimate = true;
        alarms = HourlyApplication.loadAlarms(getActivity());
        Collections.sort(alarms, new Alarm.CustomComparator());
        changed();

        if (key.equals(HourlyApplication.PREFERENCE_WEEKSTART))
            updateStartWeek();
    }

    void updateStartWeek() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        String s = prefs.getString(HourlyApplication.PREFERENCE_WEEKSTART, "");
        for (int i = 0; i < Alarm.DAYS.length; i++) {
            if (s.equals(getString(Alarm.DAYS[i]))) {
                startweek = i;
                break;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (fragmentRequestRingtone == null)
            return;

        if (resultCode != Activity.RESULT_OK) {
            fragmentRequestRingtone = null;
            return;
        }

        if (requestCode == 0) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null) {
                fragmentRequestRingtone.ringtoneValue = uri.toString();
            } else {
                fragmentRequestRingtone.ringtoneValue = Alarm.DEFAULT_RING.toString();
            }
            save(fragmentRequestRingtone);
            fragmentRequestRingtone = null;
            return;
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        this.scrollState = scrollState;
        boxAnimate = true;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
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
        return alarms.size();
    }

    @Override
    public Object getItem(int position) {
        return alarms.get(position);
    }

    @Override
    public long getItemId(int position) {
        return alarms.get(position).id;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    static boolean checkboxAnimate(CheckBox checkbox, View view) {
        boolean animate;
        Animation a = view.getAnimation();
        if (a != null && !a.hasEnded())
            return true;
        if (checkbox.isChecked()) {
            animate = view.getVisibility() != View.VISIBLE;
        } else {
            animate = view.getVisibility() == View.VISIBLE;
        }
        return animate;
    }


    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.alarm, parent, false);
            convertView.setTag(-1);
        }

        if ((int) convertView.getTag() == TYPE_DELETED) {
            RemoveItemAnimation.restore(convertView.findViewById(R.id.alarm_base));
            convertView.setTag(-1);
        }

        final Alarm a = alarms.get(position);

        final View alarmRingtonePlay = convertView.findViewById(R.id.alarm_ringtone_play);
        alarmRingtonePlay.clearAnimation();

        if (selected == a.id) {
            fillDetailed(convertView, a, boxAnimate);

            final CheckBox weekdays = (CheckBox) convertView.findViewById(R.id.alarm_week_days);
            final LinearLayout weekdaysValues = (LinearLayout) convertView.findViewById(R.id.alarm_week);
            final CheckBox alarmRingtone = (CheckBox) convertView.findViewById(R.id.alarm_ringtone);
            final View alarmRingtoneLayout = convertView.findViewById(R.id.alarm_ringtone_layout);

            AlarmAnimation.apply(list, convertView, true, scrollState == SCROLL_STATE_IDLE && (int) convertView.getTag() == TYPE_COLLAPSED);

            MarginAnimation.apply(weekdaysValues, weekdays.isChecked(), scrollState == SCROLL_STATE_IDLE &&
                    (int) convertView.getTag() == TYPE_EXPANDED &&
                    checkboxAnimate(weekdays, weekdaysValues));

            MarginAnimation.apply(alarmRingtoneLayout, alarmRingtone.isChecked(), scrollState == SCROLL_STATE_IDLE &&
                    (int) convertView.getTag() == TYPE_EXPANDED &&
                    checkboxAnimate(alarmRingtone, alarmRingtoneLayout));

            convertView.setTag(TYPE_EXPANDED);

            return convertView;
        } else {
            fillCompact(convertView, a, boxAnimate);

            AlarmAnimation.apply(list, convertView, false, scrollState == SCROLL_STATE_IDLE && (int) convertView.getTag() == TYPE_EXPANDED);

            convertView.setTag(TYPE_COLLAPSED);

            return convertView;
        }
    }

    void select(long id) {
        // stop sound preview when detailed view closed.
        if (preview) {
            sound.playerClose();
            preview = false;
        }
        selected = id;
        changed();
    }

    void save(Alarm a) {
        HourlyApplication.saveAlarms(getActivity(), alarms);
    }

    @Override
    public int getItemViewType(int position) {
        return TYPE_COLLAPSED;
    }

    @Override
    public int getViewTypeCount() {
        return ALL.length;
    }

    @Override
    public boolean isEmpty() {
        return getCount() == 0;
    }

    public void addAlarm(Alarm a) {
        alarms.add(a);
        Collections.sort(alarms, new Alarm.CustomComparator());
        select(a.id);
        int pos = alarms.indexOf(a);
        list.smoothScrollToPosition(pos);

        HourlyApplication.saveAlarms(getActivity(), alarms);

        boxAnimate = false;
    }

    public void remove(Alarm a) {
        alarms.remove(a);
        HourlyApplication.saveAlarms(getActivity(), alarms);

        boxAnimate = false;
    }

    void changed() {
        for (DataSetObserver l : listeners) {
            l.onChanged();
        }
    }

    void fillDetailed(final View view, final Alarm a, boolean animate) {
        final Switch enable = (Switch) view.findViewById(R.id.alarm_enable);
        enable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                a.setEnable(enable.isChecked());

                if (enable.isChecked())
                    HourlyApplication.toastAlarmSet(getActivity(), a);

                save(a);
            }
        });
        enable.setChecked(a.getEnable());
        if (!animate)
            enable.jumpDrawablesToCurrentState();

        final CheckBox weekdays = (CheckBox) view.findViewById(R.id.alarm_week_days);
        LinearLayout weekdaysValues = (LinearLayout) view.findViewById(R.id.alarm_week);

        for (int i = 0; i < weekdaysValues.getChildCount(); i++) {
            final CheckBox child = (CheckBox) weekdaysValues.getChildAt(i);
            if (child instanceof CheckBox) {
                child.setText(getString(Alarm.DAYS[startweek]).substring(0, 1));
                final int week = Alarm.EVERYDAY[startweek];

                child.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        long time = a.time;
                        a.setWeek(week, child.isChecked());
                        if (a.time != time && a.enable) {
                            HourlyApplication.toastAlarmSet(getActivity(), a);
                        }
                        save(a);
                    }
                });
                child.setChecked(a.isWeek(week));
                startweek++;
                if (startweek >= Alarm.DAYS.length)
                    startweek = 0;
            }
        }
        weekdays.setChecked(a.weekdays);
        weekdays.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                a.weekdays = weekdays.isChecked();
                if (a.weekdays && a.noDays()) {
                    a.setEveryday();
                }
                save(a);
            }
        });

        final CheckBox ringtone = (CheckBox) view.findViewById(R.id.alarm_ringtone);
        ringtone.setChecked(a.ringtone);
        if (ringtone.isChecked()) {
            TextView ringtoneValue = (TextView) view.findViewById(R.id.alarm_ringtone_value);
            String title = HourlyApplication.getTitle(getActivity(), a.ringtoneValue);
            if (title == null)
                title = HourlyApplication.getTitle(getActivity(), Alarm.DEFAULT_RING.toString());
            ringtoneValue.setText(title);
        }

        final CheckBox beep = (CheckBox) view.findViewById(R.id.alarm_beep);
        beep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                a.beep = beep.isChecked();
                save(a);
            }
        });
        beep.setChecked(a.beep);
        final CheckBox speech = (CheckBox) view.findViewById(R.id.alarm_speech);
        speech.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                a.speech = speech.isChecked();
                save(a);
            }
        });
        speech.setChecked(a.speech);

        final CheckBox alarmRingtone = (CheckBox) view.findViewById(R.id.alarm_ringtone);
        final View alarmRingtonePlay = view.findViewById(R.id.alarm_ringtone_play);

        if (preview) {
            alarmRingtonePlay.clearAnimation();
            sound.playerClose();
            preview = false;
        }

        alarmRingtone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                a.ringtone = alarmRingtone.isChecked();
                save(a);
            }
        });
        alarmRingtonePlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (preview) {
                    alarmRingtonePlay.clearAnimation();
                    sound.playerClose();
                    preview = false;
                    return;
                }

                if (a.ringtoneValue.isEmpty())
                    return;

                if (sound.silenced()) {
                    sound.silencedToast();
                    return;
                }

                preview = true;

                sound.playAlarm(a);

                Animation a = AnimationUtils.loadAnimation(getActivity(), R.anim.shake);
                alarmRingtonePlay.startAnimation(a);
            }
        });

        final View trash = view.findViewById(R.id.alarm_bottom_first);
        trash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            view.setTag(TYPE_DELETED);
                            // mark scroll as animating. because we about to remove item.
                            RemoveItemAnimation.apply(list, view.findViewById(R.id.alarm_base), new Runnable() {
                                @Override
                                public void run() {
                                    remove(a);
                                    select(-1);
                                }
                            });
                        }
                    }
                };
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage("Are you sure?").setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();
            }
        });

        View ringtoneButton = view.findViewById(R.id.alarm_ringtone_value);
        ringtoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fragmentRequestRingtone = a;
                Uri uri = null;
                if (!a.ringtoneValue.isEmpty()) {
                    uri = Uri.parse(a.ringtoneValue);
                }

                startActivityForResult(new Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm")
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) uri), 0);
            }
        });
        View ringtoneBrowse = view.findViewById(R.id.alarm_ringtone_browse);
        ringtoneBrowse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fragmentRequestRingtone = a;

                if (permitted())
                    selectFile();
            }
        });

        final TextView time = (TextView) view.findViewById(R.id.alarm_time);
        updateTime(view, a);
        time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerDialog d = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
                    // onTimeSet called twice on old phones
                    //
                    // http://stackoverflow.com/questions/19452993
                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            if (a.enable)
                                HourlyApplication.toastAlarmSet(getActivity(), a);
                            updateTime(view, a);
                            save(a);
                        }
                    };

                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        a.setTime(hourOfDay, minute);
                        if (r != null) {
                            r.run();
                            r = null;
                        }
                    }
                }, a.getHour(), a.getMin(), DateFormat.is24HourFormat(getActivity()));
                d.show();
            }
        });

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boxAnimate = true;

                select(-1);
            }
        });
    }

    void selectFile() {
        final OpenFileDialog f = new OpenFileDialog(getActivity());

        String path = fragmentRequestRingtone.ringtoneValue;

        if (path == null || path.isEmpty()) {
            path = Environment.getExternalStorageDirectory().getPath();
        }

        File sound = new File(path);

        while (!sound.exists()) {
            sound = sound.getParentFile();
            if (sound == null)
                sound = Environment.getExternalStorageDirectory();
        }

        f.setCurrentPath(sound);
        f.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                fragmentRequestRingtone.ringtoneValue = f.getCurrentPath().getAbsolutePath();
                save(fragmentRequestRingtone);
                fragmentRequestRingtone = null;
            }
        });
        f.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case 1:
                if (permitted(permissions))
                    selectFile();
                else
                    Toast.makeText(getActivity(), "Not permitted", Toast.LENGTH_SHORT).show();
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

    boolean permitted() {
        for (String s : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(getActivity(), s) != PackageManager.PERMISSION_GRANTED) {
                FragmentCompat.requestPermissions(this, PERMISSIONS, 1);
                return false;
            }
        }
        return true;
    }

    void fillCompact(final View view, final Alarm a, boolean animate) {
        TextView time = (TextView) view.findViewById(R.id.alarm_time);
        updateTime(view, a);
        time.setClickable(false);

        final Switch enable = (Switch) view.findViewById(R.id.alarm_enable);
        enable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                a.setEnable(enable.isChecked());

                if (enable.isChecked())
                    HourlyApplication.toastAlarmSet(getActivity(), a);

                save(a);
            }
        });
        enable.setChecked(a.getEnable());
        if (!animate)
            enable.jumpDrawablesToCurrentState();

        TextView days = (TextView) view.findViewById(R.id.alarm_compact_first);
        days.setText(a.getDays());

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boxAnimate = true;
                select(a.id);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.unregisterOnSharedPreferenceChangeListener(this);

        if (sound != null) {
            sound.close();
            sound = null;
        }
    }

    void updateTime(View view, Alarm a) {
        TextView time = (TextView) view.findViewById(R.id.alarm_time);
        View am = view.findViewById(R.id.alarm_am);
        View pm = view.findViewById(R.id.alarm_pm);

        if (DateFormat.is24HourFormat(getActivity())) {
            SimpleDateFormat f = new SimpleDateFormat("HH:mm");
            time.setText(f.format(new Date(a.time)));

            am.setVisibility(View.GONE);
            pm.setVisibility(View.GONE);
        } else {
            SimpleDateFormat f = new SimpleDateFormat("h:mm");
            time.setText(f.format(new Date(a.time)));

            am.setVisibility(a.getHour() >= 12 ? View.GONE : View.VISIBLE);
            pm.setVisibility(a.getHour() >= 12 ? View.VISIBLE : View.GONE);
        }
    }
}
