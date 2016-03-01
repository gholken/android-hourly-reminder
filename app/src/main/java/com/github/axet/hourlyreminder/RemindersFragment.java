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

public class RemindersFragment extends AlarmsFragment {

    public static class RemindersAdapter extends AlarmsAdapter {
        public RemindersAdapter() {
            layout_id = R.layout.reminder;
        }
    }

    public RemindersFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = new RemindersAdapter();
    }

}
