package com.github.axet.hourlyreminder.animations;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;

public class MarginAnimation extends Animation {

    View view;
    ViewGroup.MarginLayoutParams viewLp;
    ViewGroup.MarginLayoutParams viewLpOrig;
    int marginSlide;
    boolean expand;

    public static void apply(View v, boolean expand, boolean animate) {
        Animation a = v.getAnimation();
        if (a != null && a instanceof MarginAnimation) {
            MarginAnimation m = (MarginAnimation) a;

            long cur = AnimationUtils.currentAnimationTimeMillis();
            long past = cur - m.getStartTime() - m.getStartOffset();
            long left = m.getDuration() - past;
            long offset = cur - m.getStartTime() - left;

            if (animate) {
                if (m.hasEnded()) {
                    MarginAnimation mm = new MarginAnimation(v, expand);
                    v.startAnimation(mm);
                } else {
                    if (m.expand != expand) {
                        m.expand = expand;
                        m.setStartOffset(offset);
                    } else {
                        // keep rolling do nothing
                    }
                }
            } else {
                m.restore();
                m.cancel();
                MarginAnimation mm = new MarginAnimation(v, expand);
                mm.end();
            }
        } else {
            MarginAnimation mm = new MarginAnimation(v, expand);
            if (animate) {
                v.startAnimation(mm);
            } else {
                mm.end();
            }
        }
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
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.UNSPECIFIED));
        marginSlide = view.getMeasuredHeight() + viewLpOrig.bottomMargin;
    }

    void calc(float i) {
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

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        super.applyTransformation(interpolatedTime, t);

        if (interpolatedTime < 1) {
            float i = expand ? interpolatedTime : 1 - interpolatedTime;
            calc(i);
        } else {
            restore();
            end();
        }
    }
}