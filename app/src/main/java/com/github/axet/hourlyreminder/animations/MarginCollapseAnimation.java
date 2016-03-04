package com.github.axet.hourlyreminder.animations;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;

public class MarginCollapseAnimation extends Animation implements Animation.AnimationListener {

    View detailed;
    ViewGroup.MarginLayoutParams detailedLp;
    int h;

    public MarginCollapseAnimation(View v) {
        setDuration(500);

        setAnimationListener(this);

        detailed = v;

        detailed.setVisibility(View.VISIBLE);

        detailedLp = (ViewGroup.MarginLayoutParams) detailed.getLayoutParams();
        h = detailed.getMeasuredHeight();

        detailedLp.topMargin = 0;
    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight) {
        super.initialize(width, h, parentWidth, parentHeight);
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        super.applyTransformation(interpolatedTime, t);

        detailedLp.topMargin = -(int) (h * interpolatedTime);

        detailed.requestLayout();
    }

    @Override
    public void onAnimationStart(Animation animation) {
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        detailedLp.topMargin = 0;
        detailed.setVisibility(View.GONE);

        detailed.requestLayout();
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
    }
}