package com.github.axet.hourlyreminder.animations;

import android.annotation.TargetApi;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.widget.AbsListView;
import android.widget.ListView;

import com.github.axet.androidlibrary.animations.MarginAnimation;
import com.github.axet.hourlyreminder.R;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AlarmAnimation extends MarginAnimation {
    ListView list;

    View convertView;
    View bottom;
    View bottom_f;
    View bottom_s;
    View compact;
    View compact_f;
    View compact_s;

    boolean partial;
    Handler handler;

    // true if this animation was started simultaneously with expand animation.
    boolean collapse_multi = false;

    // if we have two concurrent animations on the same listview
    // the only one 'expand' should have control of showChild function.
    static AlarmAnimation atomicExpander;

    public static void apply(final ListView list, final View v, final boolean expand, boolean animate) {
        apply(new LateCreator() {
            @Override
            public MarginAnimation create() {
                AlarmAnimation a = new AlarmAnimation(list, v, expand);
                if (expand)
                    atomicExpander = a;
                return a;
            }
        }, v, expand, animate);
    }

    public AlarmAnimation(ListView list, View v, boolean expand) {
        super(v.findViewById(R.id.alarm_detailed), expand);

        handler = new Handler();

        this.convertView = v;
        this.list = list;

        bottom = v.findViewById(R.id.alarm_bottom);
        bottom_f = v.findViewById(R.id.alarm_bottom_first);
        bottom_s = v.findViewById(R.id.alarm_bottom_second);
        compact = v.findViewById(R.id.alarm_compact);
        compact_f = v.findViewById(R.id.alarm_compact_first);
        compact_s = v.findViewById(R.id.alarm_compact_second);
    }

    public void init() {
        super.init();

        bottom.setVisibility(View.VISIBLE);
        bottom_s.setVisibility(expand ? View.INVISIBLE : View.VISIBLE);

        compact.setVisibility(View.VISIBLE);
        compact_s.setVisibility(expand ? View.VISIBLE : View.INVISIBLE);

        {
            final int paddedTop = list.getListPaddingTop();
            final int paddedBottom = list.getHeight() - list.getListPaddingTop() - list.getListPaddingBottom();

            partial = false;

            partial |= convertView.getTop() < paddedTop;
            partial |= convertView.getBottom() > paddedBottom;
        }
    }

    @Override
    public void calc(final float i, Transformation t) {
        super.calc(i, t);

        float ii = expand ? i : 1 - i;

        compact_f.setAlpha(1 - ii);
        compact_s.setRotation(180 * ii);
        bottom_f.setAlpha(ii);
        bottom_s.setRotation(-180 + 180 * ii);

        // ViewGroup will crash on null pointer without this post pone.
        // seems like some views are removed by RecyvingView when they
        // gone off screen.
        if (Build.VERSION.SDK_INT >= 19) {
            collapse_multi |= !expand && atomicExpander != null && !atomicExpander.hasEnded();
            if (collapse_multi) {
                // do not showChild;
            } else {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        showChild(i);
                    }
                });
            }
        }
    }

    @TargetApi(19)
    void showChild(float i) {
        final int paddedTop = list.getListPaddingTop();
        final int paddedBottom = list.getHeight() - list.getListPaddingTop() - list.getListPaddingBottom();

        if (convertView.getTop() < paddedTop) {
            int off = convertView.getTop() - paddedTop;
            if (partial)
                off = (int) (off * i);
            list.scrollListBy(off);
        }

        if (convertView.getBottom() > paddedBottom) {
            int off = convertView.getBottom() - paddedBottom;
            if (partial)
                off = (int) (off * i);
            list.scrollListBy(off);
        }
    }

    @Override
    public void restore() {
        super.restore();

        bottom_f.setAlpha(1);
        bottom_s.setRotation(0);
        compact_f.setAlpha(1);
        compact_s.setRotation(0);
    }

    @Override
    public void end() {
        super.end();

        bottom_s.setVisibility(View.VISIBLE);
        compact_s.setVisibility(View.VISIBLE);
        view.setVisibility(expand ? View.VISIBLE : View.GONE);
        compact.setVisibility(expand ? View.GONE : View.VISIBLE);
        bottom.setVisibility(expand ? View.VISIBLE : View.GONE);
    }
}
