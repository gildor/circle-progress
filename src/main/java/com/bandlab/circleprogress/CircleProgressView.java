package com.bandlab.circleprogress;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;

public class CircleProgressView extends View {

    public static final int MAX_PROGRESS = 100;
    private static final int DEFAULT_DONUT_RADIUS = 4;
    private final Point progressCenter = new Point(0, 0);
    private CircleProgress circleProgress;
    private int progress;
    private boolean isInfiniteProgress;

    public CircleProgressView(Context context) {
        super(context);
        initCircleProgress();
    }

    public CircleProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initCircleProgress();
        initStylesFromAttrs(attrs);
    }

    private void initCircleProgress() {
        circleProgress = new CircleProgress();
        circleProgress.setOnInvalidateListener(new CircleProgress.OnInvalidateListener() {
            @Override
            public void invalidate() {
                postInvalidateOnAnimation();
            }
        });
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    private void initStylesFromAttrs(AttributeSet attrs) {
        TypedArray attributes = getContext().obtainStyledAttributes(attrs, R.styleable.CircleProgressView);
        final int defaultDonutRadius = Math.round(DEFAULT_DONUT_RADIUS * getResources().getDisplayMetrics().density);
        int donutRadius = attributes.getDimensionPixelSize(R.styleable.CircleProgressView_donutRadius, defaultDonutRadius);
        int progressColor = attributes.getColor(R.styleable.CircleProgressView_progressColor, Color.RED);
        boolean showRestProgress = attributes.getBoolean(R.styleable.CircleProgressView_showRestProgress, false);

        circleProgress.setDonutParams(donutRadius, progressColor);
        circleProgress.setShowRestProgress(showRestProgress);

        attributes.recycle();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int x = getMeasuredWidth() / 2 - getPaddingTop() - getPaddingBottom();
        int y = getMeasuredHeight() / 2 - getPaddingLeft() - getPaddingRight();
        int radius = Math.min(x, y);

        progressCenter.set(x, y);
        if (isInfiniteProgress) {
            circleProgress.drawInfiniteProgress(canvas, progressCenter, radius);
        } else {
            circleProgress.drawProgress(
                    canvas,
                    progressCenter,
                    progress,
                    radius
            );
        }
    }

    public double getMax() {
        return MAX_PROGRESS;
    }

    public void setProgress(int progress) {
        circleProgress.stop();
        isInfiniteProgress = false;
        this.progress = progress;
        postInvalidateOnAnimation();
    }


    public void showInfiniteProgress() {
        circleProgress.start();
        isInfiniteProgress = true;
        progress = 0;
        postInvalidateOnAnimation();
    }

    public void clearProgress() {
        setProgress(0);
    }
}
