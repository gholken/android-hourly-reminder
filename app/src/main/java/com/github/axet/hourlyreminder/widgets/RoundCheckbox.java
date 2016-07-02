package com.github.axet.hourlyreminder.widgets;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.AttributeSet;
import android.util.Property;
import android.view.Gravity;
import android.widget.CheckBox;

import com.github.axet.androidlibrary.R;
import com.github.axet.androidlibrary.widgets.StateDrawable;
import com.github.axet.androidlibrary.widgets.ThemeUtils;

// old phones does not allow to refer colors from xml shapes.
//
// better to create simple class. java always better then xml (note to google)
//
public class RoundCheckbox extends AppCompatCheckBox {

    public static int SECOND_BACKGROUND = 0x22222222;

    ObjectAnimator animator;

    float stateAnimator;

    Property<RoundCheckbox, Float> STATE_ANIMATOR = new Property<RoundCheckbox, Float>(Float.class, "stateAnimator") {
        @Override
        public Float get(RoundCheckbox object) {
            return object.stateAnimator;
        }

        @Override
        public void set(RoundCheckbox object, Float value) {
            object.stateAnimator = value;
            object.invalidate();
        }
    };

    public RoundCheckbox(Context context) {
        super(context);

        create();
    }

    public RoundCheckbox(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        create();
    }

    public RoundCheckbox(Context context, AttributeSet attrs) {
        super(context, attrs);

        create();
    }

    public void create() {
        /*
        <style name="RoundCheckBox">
            <item name="android:button">@android:color/transparent</item>
            <item name="android:textColor">@color/round_checkbox_color</item>
            <item name="android:textStyle">bold</item>
            <item name="android:background">@drawable/round_checkbox</item>
            <item name="android:gravity">center</item>
        </style>

        <?xml version="1.0" encoding="utf-8"?>
        <selector xmlns:android="http://schemas.android.com/apk/res/android">
            <item android:drawable="@drawable/round_checkbox_uncheck" android:state_checked="false" />
            <item android:drawable="@drawable/round_checkbox_check" android:state_checked="true" />
        </selector>

        <?xml version="1.0" encoding="UTF-8"?>
        <shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="oval">
            <solid android:color="@color/colorAccent" />
            <padding
                android:bottom="0dip"
                android:left="0dip"
                android:right="0dip"
                android:top="0dip" />
        </shape>

        <?xml version="1.0" encoding="UTF-8"?>
        <shape xmlns:android="http://schemas.android.com/apk/res/android"
            android:shape="oval">
            <solid android:color="#222222" />
            <padding
                android:bottom="0dip"
                android:left="0dip"
                android:right="0dip"
                android:top="0dip" />
        </shape>
        */

        ShapeDrawable checkbox_on = new ShapeDrawable(new OvalShape());
        PorterDuffColorFilter checkbox_on_filter = new PorterDuffColorFilter(ThemeUtils.getThemeColor(getContext(), R.attr.colorAccent), PorterDuff.Mode.SRC_ATOP);
        checkbox_on.setColorFilter(checkbox_on_filter);
        ShapeDrawable checkbox_off = new ShapeDrawable(new OvalShape());
        PorterDuffColorFilter checkbox_off_filter = new PorterDuffColorFilter(SECOND_BACKGROUND, PorterDuff.Mode.MULTIPLY);
        checkbox_off.setColorFilter(checkbox_off_filter);

        StateDrawable background = new StateDrawable();
        background.addState(new int[]{android.R.attr.state_checked}, checkbox_on, checkbox_on_filter);
        background.addState(new int[]{-android.R.attr.state_checked}, checkbox_off, checkbox_off_filter);

        background.setExitFadeDuration(500);
        background.setEnterFadeDuration(500);

        if (Build.VERSION.SDK_INT >= 16)
            setBackground(background);
        else
            setBackgroundDrawable(background);

        // reset padding set by previous background from constructor
        setPadding(0, 0, 0, 0);

        setButtonDrawable(android.R.color.transparent);

        setGravity(Gravity.CENTER);

        setTypeface(null, Typeface.BOLD);

        ColorStateList colors = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{-android.R.attr.state_checked},
                },
                new int[]{
                        Color.WHITE,
                        ThemeUtils.getThemeColor(getContext(), android.R.attr.textColorHint),
                });

        setTextColor(colors);
    }

    @Override
    public void setChecked(boolean checked) {
        super.setChecked(checked);

        animator = ObjectAnimator.ofFloat(this, STATE_ANIMATOR, 1f);
        animator.setDuration(500);
        if (Build.VERSION.SDK_INT >= 18)
            animator.setAutoCancel(true);
        animator.start();
    }

}
