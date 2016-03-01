package com.github.axet.hourlyreminder;

import android.app.Fragment;
import android.content.Context;
import android.database.DataSetObserver;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.CheckBox;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class AlarmsFragment extends Fragment {

    AlarmsAdapter adapter;

    public static class AlarmsAdapter implements ListAdapter, AbsListView.OnScrollListener {
        ArrayList<DataSetObserver> listeners = new ArrayList<>();
        int count;
        int selected = -1;
        int scrollState;

        static final int TYPE_NORMAL = 0;
        static final int TYPE_DETAIL = 1;

        static final int[] ALL = {TYPE_NORMAL, TYPE_DETAIL};

        int layout_id;

        public AlarmsAdapter() {
            layout_id = R.layout.alarm;
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
            LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            if (convertView == null) {
                convertView = inflater.inflate(layout_id, parent, false);
                convertView.setTag(-1);
            }

            if (selected == position) {
                //
                // fill detailed alarm
                //

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
