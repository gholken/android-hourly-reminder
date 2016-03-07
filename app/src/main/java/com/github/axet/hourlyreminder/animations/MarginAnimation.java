package com.github.axet.hourlyreminder.animations;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;

public class MarginAnimation extends StepAnimation {

    View view;
    ViewGroup.MarginLayoutParams viewLp;
    ViewGroup.MarginLayoutParams viewLpOrig;
    int marginSlide;
    boolean expand;

    public static void apply(final View v, final boolean expand, boolean animate) {
        apply(new LateCreator() {
            @Override
            public MarginAnimation create() {
                return new MarginAnimation(v, expand);
            }
        }, v, expand, animate);
    }

    public MarginAnimation(View v, boolean expand) {
        this.expand = expand;

        setDuration(500);

        view = v;
        viewLp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        viewLpOrig = new ViewGroup.MarginLayoutParams(viewLp);
    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight) {
        super.initialize(width, height, parentWidth, parentHeight);
        view.setVisibility(View.VISIBLE);
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(Math.max(height, parentHeight), View.MeasureSpec.AT_MOST));
        marginSlide = view.getMeasuredHeight() + viewLpOrig.bottomMargin;
    }

    void calc(float i) {
        i = expand ? i : 1 - i;

        viewLp.topMargin = (int) (viewLpOrig.topMargin * i - marginSlide * (1 - i));
        view.requestLayout();
    }

    void restore() {
        viewLp.topMargin = viewLpOrig.topMargin;
    }

    void end() {
        view.setVisibility(expand ? View.VISIBLE : View.GONE);
        view.requestLayout();
    }

}