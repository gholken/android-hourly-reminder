package com.github.axet.hourlyreminder;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

public class SquareLinerarLayout extends LinearLayout {

    public SquareLinerarLayout(Context context) {
        super(context);
    }

    public SquareLinerarLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public SquareLinerarLayout(Context context, AttributeSet attrs) {
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
            int s = MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY);
            child.measure(s, s);
        }
    }
}
