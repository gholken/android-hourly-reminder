package com.github.axet.hourlyreminder.layouts;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class SquareLinearLayout extends LinearLayout {

    public SquareLinearLayout(Context context) {
        super(context);
    }

    public SquareLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public SquareLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count = 0;

        for (int i = 0; i < getChildCount(); ++i) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE)
                continue;

            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) child.getLayoutParams();
            lp.gravity = Gravity.CENTER;
            count++;
        }

        int w = MeasureSpec.getSize(widthMeasureSpec) / count;

        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY));

        for (int i = 0; i < count; ++i) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE)
                continue;
            MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
            int s = MeasureSpec.makeMeasureSpec(w - lp.leftMargin - lp.rightMargin, MeasureSpec.EXACTLY);
            int t = MeasureSpec.makeMeasureSpec(w - lp.topMargin - lp.bottomMargin, MeasureSpec.EXACTLY);
            child.measure(s, t);
        }
    }
}
