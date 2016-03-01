package com.github.axet.hourlyreminder;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
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
import android.widget.Toast;

import org.w3c.dom.Text;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class AlarmsFragment extends Fragment {

    AlarmsAdapter adapter;

    public static class AlarmsAdapter implements ListAdapter, AbsListView.OnScrollListener, PreferenceManager.OnActivityResultListener {
        ArrayList<DataSetObserver> listeners = new ArrayList<>();
        List<Alarm> alarms = new ArrayList<>();
        int selected = -1;
        int scrollState;

        static final int TYPE_NORMAL = 0;
        static final int TYPE_DETAIL = 1;

        static final int[] ALL = {TYPE_NORMAL, TYPE_DETAIL};

        int layout_id = R.layout.alarm;

        Context context;

        Alarm fragmentRequest;
        Fragment fragment;

        public AlarmsAdapter(Context context, Fragment fragment) {
            this.context = context;
            this.fragment = fragment;

            load();
        }

        void load() {
            alarms = ((HourlyApplication) context.getApplicationContext()).getAlarms();
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
                                    ((HourlyApplication) context.getApplicationContext()).saveAlarms();
                                    select(-1);
                                }
                            }
                        };
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setMessage("Are you sure?").setPositiveButton("Yes", dialogClickListener)
                                .setNegativeButton("No", dialogClickListener).show();
                    }
                });

                View ringtoneButton = convertView.findViewById(R.id.alarm_ringtone_button);
                ringtoneButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        fragmentRequest = a;
                        Uri uri = null;
                        if (!a.ringtoneValue.isEmpty()) {
                            uri = Uri.parse(a.ringtoneValue);
                        }

                        fragment.startActivityForResult(new Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Tone")
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) uri), 0);
                    }
                });
                View ringtoneBrowse = convertView.findViewById(R.id.alarm_ringtone_browse);
                ringtoneBrowse.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        fragmentRequest = a;

                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("*/*");
                        intent.addCategory(Intent.CATEGORY_OPENABLE);

                        fragment.startActivityForResult(
                                Intent.createChooser(intent, "Select a File to Upload"),
                                1);
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

            String title = "";
            if (!a.ringtoneValue.isEmpty()) {
                Ringtone rt = RingtoneManager.getRingtone(context, Uri.parse(a.ringtoneValue));
                title = rt.getTitle(context);
            }

            ringtoneValue.setText(title.isEmpty() ? "Default Ringtone" : title);
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
            ((HourlyApplication) context.getApplicationContext()).saveAlarms();
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
            ((HourlyApplication) context.getApplicationContext()).saveAlarms();
            changed();
        }

        void changed() {
            for (DataSetObserver l : listeners) {
                l.onChanged();
            }
        }

        @Override
        public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
            if (fragmentRequest == null)
                return false;

            if (resultCode != Activity.RESULT_OK) {
                fragmentRequest = null;
                return true;
            }

            if (requestCode == 0) {
                Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                if (uri != null) {
                    fragmentRequest.ringtoneValue = uri.toString();
                } else {
                    fragmentRequest.ringtoneValue = "";
                }
                save(fragmentRequest);
                fragmentRequest = null;
                changed();
                return true;
            }

            if (requestCode == 1) {
                Uri uri = data.getData();
            }

            return false;
        }
    }

    public AlarmsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = new AlarmsAdapter(getActivity(), this);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        adapter.onActivityResult(requestCode, resultCode, data);
    }
}
