package com.github.axet.hourlyreminder.animations;

import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ListView;

import com.github.axet.hourlyreminder.R;

public class AlarmExpandAnimation extends Animation implements Animation.AnimationListener {

    ListView list;
    View view;

    View detailed;
    View bottom;
    View bottom_f;
    View bottom_s;
    View compact;
    View compact_f;
    View compact_s;

    ViewGroup.MarginLayoutParams detailedLp;
    int h;

    public AlarmExpandAnimation(View v, ListView list) {
        this.list = list;
        this.view = v;

        setDuration(500);

        setAnimationListener(this);

        detailed = v.findViewById(R.id.alarm_detailed);
        bottom = v.findViewById(R.id.alarm_bottom);
        bottom_f = v.findViewById(R.id.alarm_bottom_first);
        bottom_s = v.findViewById(R.id.alarm_bottom_second);
        compact = v.findViewById(R.id.alarm_compact);
        compact_f = v.findViewById(R.id.alarm_compact_first);
        compact_s = v.findViewById(R.id.alarm_compact_second);

        detailed.setVisibility(View.VISIBLE);
        bottom.setVisibility(View.VISIBLE);

        bottom_s.setVisibility(View.INVISIBLE);
        bottom_s.setRotation(0);

        detailed.measure(View.MeasureSpec.makeMeasureSpec(v.getWidth(), View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(v.getHeight(), View.MeasureSpec.UNSPECIFIED));
        detailedLp = (ViewGroup.MarginLayoutParams) detailed.getLayoutParams();
        h = detailed.getMeasuredHeight();

        detailedLp.topMargin = -h;
    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight) {
        super.initialize(width, h, parentWidth, parentHeight);
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        super.applyTransformation(interpolatedTime, t);

        detailedLp.topMargin = -h + (int) (h * interpolatedTime);
        compact_f.setAlpha(1 - interpolatedTime);
        compact_s.setRotation(180 * interpolatedTime);
        bottom_f.setAlpha(interpolatedTime);

        showChild();

        detailed.requestLayout();
    }

    void showChild() {
        if (Build.VERSION.SDK_INT >= 19) {
            final Rect r = new Rect(0, 0, view.getWidth(), view.getHeight());
            list.getChildVisibleRect(view, r, null);
            int off = view.getHeight() - r.height();
            if (off > 0)
                list.scrollListBy(off);
        }
    }

    @Override
    public void onAnimationStart(Animation animation) {
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        detailedLp.topMargin = 0;
        compact_f.setAlpha(1);
        compact_s.setRotation(0);
        bottom_f.setAlpha(1);

        bottom_s.setVisibility(View.VISIBLE);
        compact.setVisibility(View.GONE);

        showChild();

        detailed.requestLayout();
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
    }
}
