package com.github.axet.hourlyreminder;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

public class EqualLinearLayout extends LinearLayout {

    int icount;

    public EqualLinearLayout(Context context) {
        super(context);
    }

    public EqualLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public EqualLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        icount = 0;

        for (int i = 0; i < getChildCount(); ++i) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE)
                continue;

            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) child.getLayoutParams();
            lp.gravity = Gravity.CENTER;
            icount++;
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // linearlayout width
        int llw = (r - l) - getPaddingLeft() - getPaddingRight();
        int llh = (b - t) - getPaddingTop() - getPaddingBottom();

        // child max width
        int cw = llw / icount;

        int ichild = 0;
        int ilast = getChildCount() - 1;

        for (int i = 0; i <= ilast; ++i) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE)
                continue;

            final int childWidth = child.getMeasuredWidth();
            final int childHeight = child.getMeasuredHeight();

            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) child.getLayoutParams();

            int cl, cr, ct, cb;

            ct = 0;
            if (lp.gravity == Gravity.CENTER) {
                ct = (llh - childHeight) / 2 + getPaddingTop();
            }
            cb = ct + childHeight;

            if (i == 0) {
                cl = 0;
            } else if (i == ilast) {
                cl = llw - childWidth - getPaddingRight() - lp.rightMargin;
            } else {
                cl = getPaddingLeft() + lp.leftMargin;
                cl += ichild * cw + (cw - childWidth) / 2;
            }
            cr = cl + childWidth;

            child.layout(cl, ct, cr, cb);
            ichild++;
        }
    }
}
