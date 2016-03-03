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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v13.app.FragmentCompat;
import android.support.v4.content.ContextCompat;
import android.transition.TransitionManager;
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

import com.github.axet.hourlyreminder.HourlyApplication;
import com.github.axet.hourlyreminder.R;
import com.github.axet.hourlyreminder.animations.AlarmCollapseAnimation;
import com.github.axet.hourlyreminder.animations.AlarmExpandAnimation;
import com.github.axet.hourlyreminder.animations.MarginCollapseAnimation;
import com.github.axet.hourlyreminder.animations.MarginExpandAnimation;
import com.github.axet.hourlyreminder.basics.Alarm;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AlarmsFragment extends Fragment implements ListAdapter, AbsListView.OnScrollListener, SharedPreferences.OnSharedPreferenceChangeListener {
    static final int TYPE_COLLAPSED = 0;
    static final int TYPE_EXPANDED = 1;

    final int[] ALL = {TYPE_COLLAPSED, TYPE_EXPANDED};

    Alarm fragmentRequestRingtone;

    ArrayList<DataSetObserver> listeners = new ArrayList<>();
    List<Alarm> alarms = new ArrayList<>();
    int selected = -1;
    int scrollState;
    Handler handler;
    // preview ringtone
    MediaPlayer preview;

    public AlarmsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handler = new Handler();

        alarms = ((HourlyApplication) getActivity().getApplicationContext()).getAlarms();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_alarms, container, false);
        final ListView list = (ListView) rootView.findViewById(R.id.section_label);
        list.setAdapter(this);
        list.setOnScrollListener(this);
        FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 19)
                    TransitionManager.beginDelayedTransition(list);
                addAlarm();
            }
        });

        return rootView;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                changed();
            }
        });
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
                fragmentRequestRingtone.ringtoneValue = Alarm.DEFAULT_RING;
            }
            save(fragmentRequestRingtone);
            fragmentRequestRingtone = null;
            return;
        }

        if (requestCode == 1) {
            Uri uri = data.getData();
            if (uri != null) {
                File f = ((HourlyApplication) getActivity().getApplicationContext()).Storage().storeRingtone(uri);
                fragmentRequestRingtone.ringtoneValue = f.getAbsolutePath();
            } else {
                fragmentRequestRingtone.ringtoneValue = Alarm.DEFAULT_RING;
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
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    static boolean checkboxAnimate(CheckBox checkbox, View view) {
        boolean animate;
        if (checkbox.isChecked()) {
            animate = view.getVisibility() != View.VISIBLE;
        } else {
            animate = view.getVisibility() == View.VISIBLE;
        }

        if (!animate) {
            view.setVisibility(checkbox.isChecked() ? View.VISIBLE : View.GONE);
            view.clearAnimation();
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            lp.topMargin = 0;
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

        final Alarm a = alarms.get(position);

        final View alarmRingtonePlay = convertView.findViewById(R.id.alarm_ringtone_play);
        alarmRingtonePlay.clearAnimation();

        if (selected == position) {
            fillDetailed(parent, convertView, a);

            final CheckBox weekdays = (CheckBox) convertView.findViewById(R.id.alarm_week_days);
            final LinearLayout weekdaysValues = (LinearLayout) convertView.findViewById(R.id.alarm_week);
            final View alarmRingtoneLayout = convertView.findViewById(R.id.alarm_ringtone_layout);
            final CheckBox alarmRingtone = (CheckBox) convertView.findViewById(R.id.alarm_ringtone);

            if (scrollState != SCROLL_STATE_IDLE) {
                convertView.findViewById(R.id.alarm_detailed).setVisibility(View.VISIBLE);
                convertView.findViewById(R.id.alarm_bottom).setVisibility(View.VISIBLE);
                convertView.findViewById(R.id.alarm_compact).setVisibility(View.GONE);

                weekdaysValues.setVisibility(weekdays.isChecked() ? View.VISIBLE : View.GONE);
                alarmRingtoneLayout.setVisibility(alarmRingtone.isChecked() ? View.VISIBLE : View.GONE);
            } else {
                if ((int) convertView.getTag() == TYPE_COLLAPSED) {
                    weekdaysValues.setVisibility(weekdays.isChecked() ? View.VISIBLE : View.GONE);
                    alarmRingtoneLayout.setVisibility(alarmRingtone.isChecked() ? View.VISIBLE : View.GONE);

                    AlarmExpandAnimation e = new AlarmExpandAnimation(convertView);
                    convertView.startAnimation(e);
                } else {
                    if (checkboxAnimate(weekdays, weekdaysValues)) {
                        if (weekdays.isChecked()) {
                            MarginExpandAnimation e = new MarginExpandAnimation(parent, weekdaysValues);
                            weekdaysValues.startAnimation(e);
                        } else {
                            MarginCollapseAnimation e = new MarginCollapseAnimation(weekdaysValues);
                            weekdaysValues.startAnimation(e);
                        }
                    }

                    if (checkboxAnimate(alarmRingtone, alarmRingtoneLayout)) {
                        alarmRingtone.setTag(alarmRingtone.isChecked());
                        if (alarmRingtone.isChecked()) {
                            MarginExpandAnimation e = new MarginExpandAnimation(parent, alarmRingtoneLayout);
                            alarmRingtoneLayout.startAnimation(e);
                        } else {
                            MarginCollapseAnimation e = new MarginCollapseAnimation(alarmRingtoneLayout);
                            alarmRingtoneLayout.startAnimation(e);
                        }
                    }
                }
            }

            convertView.setTag(TYPE_EXPANDED);

            return convertView;
        } else {
            fillCompact(convertView, a, position);

            if ((int) convertView.getTag() == TYPE_EXPANDED && scrollState == SCROLL_STATE_IDLE) {
                AlarmCollapseAnimation e = new AlarmCollapseAnimation(convertView);
                convertView.startAnimation(e);
            } else {
                convertView.findViewById(R.id.alarm_detailed).setVisibility(View.GONE);
                convertView.findViewById(R.id.alarm_bottom).setVisibility(View.GONE);
                convertView.findViewById(R.id.alarm_compact).setVisibility(View.VISIBLE);
            }
            convertView.setTag(TYPE_COLLAPSED);

            return convertView;
        }
    }

    void select(int pos) {
        // stop sound preview when detailed view closed.
        if (preview != null) {
            preview.release();
            preview = null;
        }
        selected = pos;
        changed();
    }

    void save(Alarm a) {
        ((HourlyApplication) getActivity().getApplicationContext()).saveAlarms();
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

    public void addAlarm() {
        alarms.add(new Alarm(getActivity(), System.currentTimeMillis()));
        ((HourlyApplication) getActivity().getApplicationContext()).saveAlarms();
    }

    public void remove(Alarm a) {
        alarms.remove(a);
        ((HourlyApplication) getActivity().getApplicationContext()).saveAlarms();
    }

    void changed() {
        for (DataSetObserver l : listeners) {
            l.onChanged();
        }
    }

    void fillDetailed(final View parent, View view, final Alarm a) {
        final CheckBox weekdays = (CheckBox) view.findViewById(R.id.alarm_week_days);
        LinearLayout weekdaysValues = (LinearLayout) view.findViewById(R.id.alarm_week);
        for (int i = 0; i < weekdaysValues.getChildCount(); i++) {
            final CheckBox child = (CheckBox) weekdaysValues.getChildAt(i);
            if (child instanceof CheckBox) {
                child.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        a.setWeek(a.parseTag(child.getTag()), child.isChecked());
                        if (a.noDays()) {
                            a.weekdays = false;
                        }
                        save(a);
                    }
                });
                child.setChecked(a.isWeek(a.parseTag(child.getTag())));
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
            String title = "";
            File f = new File(a.ringtoneValue);
            if (f.exists()) {
                title = f.getName();
            } else {
                Ringtone rt = RingtoneManager.getRingtone(getActivity(), Uri.parse(a.ringtoneValue));
                title = rt.getTitle(getActivity());
                rt.stop();
            }
            ringtoneValue.setText(title.isEmpty() ? Alarm.DEFAULT_RING : title);
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
        final View alarmRingtoneLayout = view.findViewById(R.id.alarm_ringtone_layout);
        final View alarmRingtonePlay = view.findViewById(R.id.alarm_ringtone_play);

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
                if (preview != null) {
                    alarmRingtonePlay.clearAnimation();
                    preview.release();
                    preview = null;
                    return;
                }
                if (a.ringtoneValue.isEmpty())
                    return;
                Uri uri = Uri.parse(a.ringtoneValue);

                preview = ((HourlyApplication) getActivity().getApplicationContext()).Sound().playOnce(uri);
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
                            remove(a);
                            select(-1);
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
        time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerDialog d = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        a.setTime(hourOfDay, minute);
                        time.setText(a.getTimeString());
                        save(a);
                    }
                }, a.getHour(), a.getMin(), true);
                d.show();
            }
        });

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                select(-1);
            }
        });
    }

    void selectFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        startActivityForResult(
                Intent.createChooser(intent, "Select a File"),
                1);
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

    void fillCompact(final View view, final Alarm a, final int position) {
        TextView time = (TextView) view.findViewById(R.id.alarm_time);
        time.setText(a.getTimeString());
        time.setClickable(false);

        final Switch enable = (Switch) view.findViewById(R.id.alarm_enable);
        enable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                a.setEnable(enable.isChecked());
                save(a);
            }
        });
        enable.setChecked(a.getEnable());

        TextView days = (TextView) view.findViewById(R.id.alarm_compact_first);
        days.setText(a.getDays());

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                select(position);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.unregisterOnSharedPreferenceChangeListener(this);

        if (preview != null) {
            preview.release();
            preview = null;
        }
    }
}
