package com.github.axet.hourlyreminder.animations;

import android.graphics.Rect;
import android.os.Build;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ListView;

import com.github.axet.hourlyreminder.R;

public class AlarmAnimation extends MarginAnimation {
    ListView list;

    View convertView;
    View bottom;
    View bottom_f;
    View bottom_s;
    View compact;
    View compact_f;
    View compact_s;

    public static void apply(ListView list, View v, boolean expand, boolean animate) {
        Animation a = v.getAnimation();
        if (a != null && a instanceof AlarmAnimation) {
            v.clearAnimation();
            AlarmAnimation m = (AlarmAnimation) a;
            m.restore();
        }

        AlarmAnimation mm = new AlarmAnimation(list, v, expand);

        if (animate)
            v.startAnimation(mm);
        else
            mm.end();
    }

    public AlarmAnimation(ListView list, View v, boolean expand) {
        super(v.findViewById(R.id.alarm_detailed), expand);

        this.convertView = v;
        this.list = list;

        bottom = v.findViewById(R.id.alarm_bottom);
        bottom_f = v.findViewById(R.id.alarm_bottom_first);
        bottom_s = v.findViewById(R.id.alarm_bottom_second);
        compact = v.findViewById(R.id.alarm_compact);
        compact_f = v.findViewById(R.id.alarm_compact_first);
        compact_s = v.findViewById(R.id.alarm_compact_second);
    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight) {
        super.initialize(width, height, parentWidth, parentHeight);

        view.setVisibility(View.VISIBLE);

        bottom.setVisibility(View.VISIBLE);
        bottom_s.setVisibility(expand ? View.INVISIBLE : View.VISIBLE);

        compact.setVisibility(View.VISIBLE);
        compact_s.setVisibility(expand ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    void calc(float i) {
        super.calc(i);

        compact_f.setAlpha(1 - i);
        compact_s.setRotation(180 * i);
        bottom_f.setAlpha(i);
        bottom_s.setRotation(-180 + 180 * i);

        if (expand)
            showChild();
    }

    void showChild() {
        if (Build.VERSION.SDK_INT >= 19) {
            final Rect r = new Rect(0, 0, convertView.getWidth(), convertView.getHeight());
            list.getChildVisibleRect(convertView, r, null);
            int off = convertView.getHeight() - r.height();
            if (off > 0)
                list.scrollListBy(off);
        }
    }

    @Override
    void restore() {
        super.restore();

        compact_f.setAlpha(1);
        compact_s.setRotation(0);
        bottom_f.setAlpha(1);
        bottom_s.setRotation(0);
    }

    @Override
    void end() {
        super.end();

        bottom_s.setVisibility(View.VISIBLE);
        compact_s.setVisibility(View.VISIBLE);
        view.setVisibility(expand ? View.VISIBLE : View.GONE);
        compact.setVisibility(expand ? View.GONE : View.VISIBLE);
        bottom.setVisibility(expand ? View.VISIBLE : View.GONE);
    }
}
