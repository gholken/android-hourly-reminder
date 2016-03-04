package com.github.axet.hourlyreminder.animations;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import com.github.axet.hourlyreminder.R;

public class AlarmCollapseAnimation extends Animation implements Animation.AnimationListener {
    View detailed;
    View bottom;
    View bottom_f;
    View bottom_s;
    View compact;
    View compact_f;
    View compact_s;

    ViewGroup.MarginLayoutParams detailedLp;
    int h;

    public AlarmCollapseAnimation(View v) {
        setDuration(500);

        setAnimationListener(this);

        detailed = v.findViewById(R.id.alarm_detailed);
        bottom = v.findViewById(R.id.alarm_bottom);
        bottom_f = v.findViewById(R.id.alarm_bottom_first);
        bottom_s = v.findViewById(R.id.alarm_bottom_second);
        compact = v.findViewById(R.id.alarm_compact);
        compact_f = v.findViewById(R.id.alarm_compact_first);
        compact_s = v.findViewById(R.id.alarm_compact_second);

        compact_s.setVisibility(View.INVISIBLE);
        compact_s.setRotation(0);

        compact.setVisibility(View.VISIBLE);

        detailedLp = (ViewGroup.MarginLayoutParams) detailed.getLayoutParams();
        h = detailed.getMeasuredHeight();
    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight) {
        super.initialize(width, h, parentWidth, parentHeight);
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        super.applyTransformation(interpolatedTime, t);

        detailedLp.topMargin = -(int) (h * interpolatedTime);
        bottom_f.setAlpha(1.0f - interpolatedTime);
        bottom_s.setRotation(-180 * interpolatedTime);
        compact_f.setAlpha(interpolatedTime);

        detailed.requestLayout();
    }

    @Override
    public void onAnimationStart(Animation animation) {
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        detailedLp.topMargin = 0;
        bottom_f.setAlpha(1);
        bottom_s.setRotation(0);
        compact_f.setAlpha(1);

        detailed.setVisibility(View.GONE);
        bottom.setVisibility(View.GONE);
        compact_s.setVisibility(View.VISIBLE);

        detailed.requestLayout();
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
    }
}