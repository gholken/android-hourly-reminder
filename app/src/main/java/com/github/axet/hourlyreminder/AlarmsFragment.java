package com.github.axet.hourlyreminder;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.ContentFrameLayout;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class AlarmsFragment extends Fragment {

    AlarmsAdapter adapter;

    public static class AlarmsAdapter implements ListAdapter, AbsListView.OnScrollListener {
        ArrayList<DataSetObserver> listeners = new ArrayList<>();
        ArrayList<Alarm> alarms = new ArrayList<>();
        int selected = -1;
        int scrollState;

        static final int TYPE_NORMAL = 0;
        static final int TYPE_DETAIL = 1;

        static final int[] ALL = {TYPE_NORMAL, TYPE_DETAIL};

        int layout_id;

        Context context;

        public AlarmsAdapter(Context context) {
            this.context = context;
            layout_id = R.layout.alarm;

            load();
        }

        void load() {
            SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
            int c = shared.getInt("Alarm_Count", 0);
            if (c == 0) {
                alarms.add(new Alarm());
            }

            for (int i = 0; i < c; i++) {
                String prefix = "Alarm_" + i + "_";
                Alarm a = new Alarm();
                a.time = shared.getLong(prefix + "Time", 0);
                a.enable = shared.getBoolean(prefix + "Enable", false);
                a.weekdays = shared.getBoolean(prefix + "WeekDays", false);
                a.setWeekDays(shared.getStringSet(prefix + "WeekDays_Values", null));
                a.ringtone = shared.getBoolean(prefix + "Ringtone", false);
                a.ringtoneValue = shared.getString(prefix + "Ringtone_Values", "");
                a.beep = shared.getBoolean(prefix + "Beep", false);
                a.speech = shared.getBoolean(prefix + "Speech", false);
                alarms.add(a);
            }
        }

        void save() {
            SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor edit = shared.edit();
            edit.putInt("Alarm_Count", alarms.size());
            for (int i = 0; i < alarms.size(); i++) {
                Alarm a = alarms.get(i);
                String prefix = "Alarm_" + i + "_";
                edit.putLong(prefix + "Time", a.time);
                edit.putBoolean(prefix + "Enable", a.enable);
                edit.putBoolean(prefix + "WeekDays", a.weekdays);
                edit.putStringSet(prefix + "WeekDays_Values", a.getWeekDays());
                edit.putBoolean(prefix + "Ringtone", a.ringtone);
                edit.putString(prefix + "Ringtone_Value", a.ringtoneValue);
                edit.putBoolean(prefix + "Beep", a.beep);
                edit.putBoolean(prefix + "Speech", a.speech);
            }
            edit.commit();
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

        @Override
        public View getView(final int position, View convertView, final ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            if (convertView == null) {
                convertView = inflater.inflate(layout_id, parent, false);
                convertView.setTag(-1);
            }

            final Alarm a = alarms.get(position);

            if (selected == position) {
                //
                // fill detailed alarm
                //
                fillDetailed(convertView, a);

                if ((int) convertView.getTag() == TYPE_NORMAL && scrollState == SCROLL_STATE_IDLE) {
                    AlarmExpandAnimation e = new AlarmExpandAnimation(convertView);
                    convertView.startAnimation(e);
                } else {
                    convertView.findViewById(R.id.alarm_detailed).setVisibility(View.VISIBLE);
                    convertView.findViewById(R.id.alarm_bottom).setVisibility(View.VISIBLE);
                    convertView.findViewById(R.id.alarm_compact).setVisibility(View.GONE);
                }
                convertView.setTag(TYPE_DETAIL);

                final CheckBox alarmRepeat = (CheckBox) convertView.findViewById(R.id.alarm_week_days);
                final View alarmWeek = convertView.findViewById(R.id.alarm_week);

                alarmRepeat.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        a.weekdays = alarmRepeat.isChecked();
                        save(a);

                        if (alarmRepeat.isChecked()) {
                            MarginExpandAnimation e = new MarginExpandAnimation(parent, alarmWeek);
                            alarmWeek.startAnimation(e);
                        } else {
                            MarginCollapseAnimation e = new MarginCollapseAnimation(alarmWeek);
                            alarmWeek.startAnimation(e);
                        }
                    }
                });
                alarmWeek.setVisibility(alarmRepeat.isChecked() ? View.VISIBLE : View.GONE);

                final CheckBox alarmRingtone = (CheckBox) convertView.findViewById(R.id.alarm_ringtone);
                final View alarmRingtoneLayout = convertView.findViewById(R.id.alarm_ringtone_layout);

                alarmRingtone.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        a.ringtone = alarmRingtone.isChecked();
                        save(a);

                        if (alarmRingtone.isChecked()) {
                            MarginExpandAnimation e = new MarginExpandAnimation(parent, alarmRingtoneLayout);
                            alarmRingtoneLayout.startAnimation(e);
                        } else {
                            MarginCollapseAnimation e = new MarginCollapseAnimation(alarmRingtoneLayout);
                            alarmRingtoneLayout.startAnimation(e);
                        }
                    }
                });
                alarmRingtoneLayout.setVisibility(alarmRingtone.isChecked() ? View.VISIBLE : View.GONE);

                final View trash = convertView.findViewById(R.id.alarm_bottom_first);
                trash.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == DialogInterface.BUTTON_POSITIVE) {
                                    alarms.remove(a);
                                    save();
                                    select(-1);
                                }
                            }
                        };
                        AlertDialog.Builder builder = new AlertDialog.Builder(parent.getContext());
                        builder.setMessage("Are you sure?").setPositiveButton("Yes", dialogClickListener)
                                .setNegativeButton("No", dialogClickListener).show();
                    }
                });

                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        select(-1);
                    }
                });

                return convertView;
            } else {
                //
                // fill compact alarm
                //

                fillCompact(convertView, a);

                if ((int) convertView.getTag() == TYPE_DETAIL && scrollState == SCROLL_STATE_IDLE) {
                    AlarmCollapseAnimation e = new AlarmCollapseAnimation(convertView);
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
        }

        void fillCompact(View view, final Alarm a) {
            TextView time = (TextView) view.findViewById(R.id.alarm_time);
            time.setText(a.getTime());

            final Switch enable = (Switch) view.findViewById(R.id.alarm_enable);
            enable.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    a.enable = enable.isChecked();
                    save(a);
                }
            });
            enable.setChecked(a.enable);

            TextView days = (TextView) view.findViewById(R.id.alarm_compact_first);
            days.setText(a.getDays(context));
        }

        void fillDetailed(View view, final Alarm a) {
            final CheckBox weekdays = (CheckBox) view.findViewById(R.id.alarm_week_days);
            weekdays.setChecked(a.weekdays);
            LinearLayout weekdaysValues = (LinearLayout) view.findViewById(R.id.alarm_week);
            int week = 0;
            for (int i = 0; i < weekdaysValues.getChildCount(); i++) {
                final CheckBox child = (CheckBox) weekdaysValues.getChildAt(i);
                if (child instanceof CheckBox) {
                    final int w = week;
                    child.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            a.setWeek(w, child.isChecked());
                        }
                    });
                    child.setChecked(a.isWeek(week));
                    week++;
                }
            }
            final CheckBox ringtone = (CheckBox) view.findViewById(R.id.alarm_ringtone);
            ringtone.setChecked(a.ringtone);
            TextView ringtoneValue = (TextView) view.findViewById(R.id.alarm_ringtone_value);
            ringtoneValue.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //TODO add file choise dialog
                }
            });
            ringtoneValue.setText(a.ringtoneValue);
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
        }

        void select(int pos) {
            selected = pos;
            changed();
        }

        void save(Alarm a) {
            save();
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
            return getCount() == 0;
        }

        public void addAlarm() {
            alarms.add(new Alarm(System.currentTimeMillis()));
            save();
            changed();
        }

        void changed() {
            for (DataSetObserver l : listeners) {
                l.onChanged();
            }
        }
    }

    public AlarmsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = new AlarmsAdapter(getActivity().getApplicationContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_alarms, container, false);
        final ListView list = (ListView) rootView.findViewById(R.id.section_label);
        list.setAdapter(adapter);
        list.setOnScrollListener(adapter);
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
