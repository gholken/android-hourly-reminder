package com.github.axet.hourlyreminder.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.github.axet.androidlibrary.widgets.ThemeUtils;
import com.github.axet.hourlyreminder.R;

public class BeepView extends ViewGroup {
    public static final String TAG = BeepView.class.getSimpleName();

    // pitch delimiter length in px
    int pitchGraph;
    // pitch length in px
    int pitchWidth;
    // pitch length in pn + pitch delimiter length in px
    int pitchSize;

    float coeff = 1f;

    CoordsView coords;
    GraphView graph;

    Handler handler;

    public class CoordsView extends View {
        Paint paint;
        Paint paintMark;
        Path arrow;
        RectF arrowRect;
        Paint textPaint;

        String textX;
        Rect textXRect;
        String textY;
        Rect textYRect;

        float markSize;
        float y1; // y=1 mark
        float x2pi; // x=pi mark

        float midX;
        float midY;

        public CoordsView(Context context) {
            this(context, null);
        }

        public CoordsView(Context context, AttributeSet attrs) {
            this(context, attrs, 0);
        }

        public CoordsView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);

            markSize = ThemeUtils.dp2px(getContext(), 3);

            paint = new Paint();
            paint.setColor(getThemeColor(R.attr.colorPrimary));
            paint.setStrokeWidth(pitchWidth);

            paintMark = new Paint();
            paintMark.setColor(getThemeColor(R.attr.colorPrimaryDark));
            paintMark.setStrokeWidth(pitchGraph);

            textPaint = new Paint();
            textPaint.setColor(getThemeColor(R.attr.colorPrimary));
            textPaint.setTextSize(50f);
            textPaint.setAntiAlias(true);

            int p7 = ThemeUtils.dp2px(getContext(), 7);
            int p3 = ThemeUtils.dp2px(getContext(), 3);

            arrow = new Path();
            arrow.moveTo(0, 0);
            arrow.lineTo(-p3, p7);
            arrow.lineTo(p3, p7);
            arrow.close();

            arrowRect = new RectF();
            arrow.computeBounds(arrowRect, true);

            textX = "x";
            textY = "y";
            textXRect = new Rect();
            textYRect = new Rect();
            textPaint.getTextBounds(textX, 0, textX.length(), textXRect);
            textPaint.getTextBounds(textY, 0, textY.length(), textYRect);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int w = MeasureSpec.getSize(widthMeasureSpec);
            int h = getPaddingTop();
            h += w;

            setMeasuredDimension(w, h);

            midX = w / 2f;
            midY = h / 2;

            y1 = midY * 0.8f;
            x2pi = midX * 0.8f;
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            super.onLayout(changed, left, top, right, bottom);
        }

        double rad(float x) {
            return (coords.midX - x) / coords.x2pi * 2 * Math.PI;
        }

        @Override
        public void onDraw(Canvas canvas) {
            // hor
            canvas.drawLine(0, midY, getWidth(), midY, paint);
            // arrow
            canvas.save();
            canvas.translate(getWidth(), midY);
            canvas.rotate(90);
            canvas.drawPath(arrow, paint);
            canvas.restore();
            // x
            canvas.drawText(textX, getWidth() - textXRect.width(), midY + arrowRect.width() - textXRect.top, textPaint);
            // marks
            float stepX = x2pi / 8f;
            for (float x = 0; x < getWidth(); x += stepX) {
                float s = markSize / 2f;
                int r = (int) (rad(x) * 100);
                if (r % 314 == 0) { // every 180° or PI (3.14)
                    s = markSize;
                }
                s += pitchWidth;
                canvas.drawLine(x, midX - s, x, midX + s, paintMark);
            }

            // vert
            canvas.drawLine(midX, 0, midX, getHeight(), paint);
            // arrow
            canvas.save();
            canvas.translate(midX, 0);
            canvas.drawPath(arrow, paint);
            canvas.restore();
            // y
            canvas.drawText(textY, midX + arrowRect.width(), -textYRect.top, textPaint);
            // marks
            canvas.drawLine(midX - markSize, midY - y1, midX + markSize, midY - y1, paintMark);
            canvas.drawLine(midX - markSize, midY + y1, midX + markSize, midY + y1, paintMark);
        }
    }

    public class GraphView extends View {
        Paint paint;

        public GraphView(Context context) {
            this(context, null);
        }

        public GraphView(Context context, AttributeSet attrs) {
            this(context, attrs, 0);
        }

        public GraphView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);

            paint = new Paint();
            paint.setColor(getThemeColor(R.attr.colorAccent));
            paint.setStrokeWidth(pitchGraph);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int w = MeasureSpec.getSize(widthMeasureSpec);
            int h = getPaddingTop();
            h += w;

            setMeasuredDimension(w, h);
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            super.onLayout(changed, left, top, right, bottom);
        }

        double func(double x) {
            return Math.sin(x * coeff);
        }

        float funcX(float x) {
            double sx = coords.rad(x);
            double sy = func(sx);
            float y = (float) (sy * coords.y1);
            y += coords.midY;
            return y;
        }

        @Override
        public void onDraw(Canvas canvas) {
            float lx = 0;
            float ly = funcX(lx);
            for (float x = 0; x < getWidth(); x++) {
                float y = funcX(x);
                canvas.drawLine(lx, ly, x, y, paint);
                lx = x;
                ly = y;
            }
        }
    }

    public BeepView(Context context) {
        this(context, null);
    }

    public BeepView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BeepView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        create();
    }

    void create() {
        handler = new Handler();

        pitchGraph = ThemeUtils.dp2px(getContext(), 1);
        pitchWidth = ThemeUtils.dp2px(getContext(), 2);
        pitchSize = pitchWidth + pitchGraph;

        coords = new CoordsView(getContext());
        addView(coords);

        graph = new GraphView(getContext());
        addView(graph);

        if (isInEditMode()) {
        }
    }

    public int getThemeColor(int id) {
        return ThemeUtils.getThemeColor(getContext(), id);
    }

    public void draw() {
        coords.invalidate();
        graph.invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int ww = getMeasuredWidth() - getPaddingRight() - getPaddingLeft();
        int hh = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();

        coords.measure(MeasureSpec.makeMeasureSpec(ww, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(hh, MeasureSpec.AT_MOST));

        ww = coords.getMeasuredWidth() + getPaddingRight() + getPaddingLeft();
        hh = coords.getMeasuredHeight() + getPaddingTop() + getPaddingBottom();

        graph.measure(MeasureSpec.makeMeasureSpec(ww, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(hh, MeasureSpec.AT_MOST));

        setMeasuredDimension(ww, hh);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        coords.layout(getPaddingLeft(), getPaddingTop(),
                getPaddingLeft() + coords.getMeasuredWidth(), getPaddingTop() + coords.getMeasuredHeight());
        graph.layout(getPaddingLeft(), getPaddingTop(),
                getPaddingLeft() + coords.getMeasuredWidth(), getPaddingTop() + coords.getMeasuredHeight());
    }

    public void setCoeff(float f) {
        coeff = f;
        draw();
    }
}