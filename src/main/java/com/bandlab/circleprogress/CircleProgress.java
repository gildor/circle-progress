package com.bandlab.circleprogress;


import android.animation.Animator;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

/**
 * Created by vorobievilya on 17/03/15.
 */
public class CircleProgress implements Animatable {
    public static final float UNIT_ANGLE = 360f / 100f;
    public static final Interpolator END_INTERPOLATOR = new LinearInterpolator();
    private static final Interpolator DEFAULT_ROTATION_INTERPOLATOR = new LinearInterpolator();
    private static final Interpolator DEFAULT_SWEEP_INTERPOLATOR = new DecelerateInterpolator();
    private static final int ROTATION_ANIMATOR_DURATION = 2000;
    private static final int SWEEP_ANIMATOR_DURATION = 600;
    private static final int END_ANIMATOR_DURATION = 200;
    private final static float SWEEP_SPEED = 1;
    private final static float ROTATION_SPEED = 1;
    private final Paint currentProgressPaint;
    private Paint restProgress;
    private int donutRadius;
    private float сurrentRotationAngle;
    private float сurrentRotationAngleOffset;
    private float сurrentSweepAngle;
    private boolean mRunning;
    private boolean modeAppearing;
    private float сurrentEndRatio;
    private ValueAnimator mSweepAppearingAnimator;
    private ValueAnimator mSweepDisappearingAnimator;
    private ValueAnimator mRotationAnimator;
    private ValueAnimator mEndAnimator;
    private Interpolator mAngleInterpolator;
    private Interpolator mSweepInterpolator;
    private int mMinSweepAngle = 20;
    private int mMaxSweepAngle = 300;
    private boolean mFirstSweepAnimation;
    private OnInvalidateListener listener;
    private RectF oval;
    private Paint donutPaint;
    private boolean isShowRestProgress = true;

    public CircleProgress() {

        mSweepInterpolator = DEFAULT_SWEEP_INTERPOLATOR;
        mAngleInterpolator = DEFAULT_ROTATION_INTERPOLATOR;

        currentProgressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        currentProgressPaint.setColor(Color.RED);

        restProgress = new Paint(Paint.ANTI_ALIAS_FLAG);
        restProgress.setColor(Color.WHITE);

        oval = new RectF();

        setupAnimations();
    }

    public void setDonutParams(int donutRadius, int donutColor) {
        this.donutRadius = donutRadius;
        if (donutRadius > 0) {
            currentProgressPaint.setColor(donutColor);
            donutPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            donutPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        } else {
            donutPaint = null;
        }
    }

    public void setShowRestProgress(boolean isShow) {
        isShowRestProgress = isShow;
    }

    public void drawInfiniteProgress(Canvas canvas, Point point, int radius) {
        float startAngle = сurrentRotationAngle - сurrentRotationAngleOffset;
        float sweepAngle = сurrentSweepAngle;
        if (!modeAppearing) {
            startAngle = startAngle + (360 - sweepAngle);
        }
        startAngle %= 360;
        if (сurrentEndRatio < 1f) {
            float newSweepAngle = sweepAngle * сurrentEndRatio;
            startAngle = (startAngle + (sweepAngle - newSweepAngle)) % 360;
            sweepAngle = newSweepAngle;
        }
        drawArc(canvas, point, radius, startAngle, sweepAngle);

    }

    private void drawArc(Canvas canvas, Point point, int radius, float startAngle, float sweepAngle) {
        if (isShowRestProgress) {
            canvas.drawCircle(point.x, point.y, radius + donutRadius, restProgress);
        }

        oval.set(point.x - radius, point.y - radius, point.x + radius, point.y + radius);
        canvas.drawArc(oval, startAngle, sweepAngle, true, currentProgressPaint);

        if (donutPaint != null) {
            oval.set(
                    point.x - radius + donutRadius,
                    point.y - radius + donutRadius,
                    point.x + radius - donutRadius,
                    point.y + radius - donutRadius
            );
            canvas.drawOval(oval, donutPaint);
        }
    }

    private void setupAnimations() {
        mRotationAnimator = ValueAnimator.ofFloat(0f, 360f);
        mRotationAnimator.setInterpolator(mAngleInterpolator);
        mRotationAnimator.setDuration((long) (ROTATION_ANIMATOR_DURATION / ROTATION_SPEED));
        mRotationAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float angle = getAnimatedFraction(animation) * 360f;
                setCurrentRotationAngle(angle);
            }
        });
        mRotationAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mRotationAnimator.setRepeatMode(ValueAnimator.RESTART);

        mSweepAppearingAnimator = ValueAnimator.ofFloat(mMinSweepAngle, mMaxSweepAngle);
        mSweepAppearingAnimator.setInterpolator(mSweepInterpolator);
        mSweepAppearingAnimator.setDuration((long) (SWEEP_ANIMATOR_DURATION / SWEEP_SPEED));
        mSweepAppearingAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animatedFraction = getAnimatedFraction(animation);
                float angle;
                if (mFirstSweepAnimation) {
                    angle = animatedFraction * mMaxSweepAngle;
                } else {
                    angle = mMinSweepAngle + animatedFraction * (mMaxSweepAngle - mMinSweepAngle);
                }
                setCurrentSweepAngle(angle);
            }
        });
        mSweepAppearingAnimator.addListener(new Animator.AnimatorListener() {
            boolean cancelled = false;

            @Override
            public void onAnimationStart(Animator animation) {
                cancelled = false;
                modeAppearing = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!cancelled) {
                    mFirstSweepAnimation = false;
                    setDisappearing();
                    mSweepDisappearingAnimator.start();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                cancelled = true;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });

        mSweepDisappearingAnimator = ValueAnimator.ofFloat(mMaxSweepAngle, mMinSweepAngle);
        mSweepDisappearingAnimator.setInterpolator(mSweepInterpolator);
        mSweepDisappearingAnimator.setDuration((long) (SWEEP_ANIMATOR_DURATION / SWEEP_SPEED));
        mSweepDisappearingAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animatedFraction = getAnimatedFraction(animation);
                setCurrentSweepAngle(mMaxSweepAngle - animatedFraction * (mMaxSweepAngle - mMinSweepAngle));
            }
        });
        mSweepDisappearingAnimator.addListener(new Animator.AnimatorListener() {
            boolean cancelled;

            @Override
            public void onAnimationStart(Animator animation) {
                cancelled = false;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!cancelled) {
                    setAppearing();
                    mSweepAppearingAnimator.start();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                cancelled = true;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        mEndAnimator = ValueAnimator.ofFloat(1f, 0f);
        mEndAnimator.setInterpolator(END_INTERPOLATOR);
        mEndAnimator.setDuration(END_ANIMATOR_DURATION);
        mEndAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setEndRatio(1f - getAnimatedFraction(animation));

            }
        });
        mEndAnimator.addListener(new Animator.AnimatorListener() {
            private boolean cancelled;

            @Override
            public void onAnimationStart(Animator animation) {
                cancelled = false;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                setEndRatio(0f);
                if (!cancelled) stop();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                cancelled = true;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    private void stopAnimators() {
        mRotationAnimator.cancel();
        mSweepAppearingAnimator.cancel();
        mSweepDisappearingAnimator.cancel();
        mEndAnimator.cancel();
    }

    @Override
    public boolean isRunning() {
        return mRunning;
    }

    public void setCurrentRotationAngle(float currentRotationAngle) {
        this.сurrentRotationAngle = currentRotationAngle;
        invalidateSelf();
    }

    public void setCurrentSweepAngle(float currentSweepAngle) {
        this.сurrentSweepAngle = currentSweepAngle;
        invalidateSelf();
    }

    private void setEndRatio(float ratio) {
        this.сurrentEndRatio = ratio;
        invalidateSelf();
    }


    public void drawProgress(Canvas canvas, Point point, int progress, int radius) {
        drawArc(canvas, point, radius, -90, UNIT_ANGLE * progress);
    }

    public void setOnInvalidateListener(OnInvalidateListener listener) {
        this.listener = listener;
    }

    @Override
    public void start() {
        if (isRunning()) {
            return;
        }
        mRunning = true;
        reinitValues();
        mRotationAnimator.start();
        mSweepAppearingAnimator.start();
        invalidateSelf();
    }

    private void invalidateSelf() {
        if (listener != null) {
            listener.invalidate();
        }
    }

    private void reinitValues() {
        mFirstSweepAnimation = true;
        сurrentEndRatio = 1f;
    }

    @Override
    public void stop() {
        if (!isRunning()) {
            return;
        }
        mRunning = false;
        stopAnimators();
        invalidateSelf();
    }

    private void setAppearing() {
        modeAppearing = true;
        сurrentRotationAngleOffset += mMinSweepAngle;
    }

    private void setDisappearing() {
        modeAppearing = false;
        сurrentRotationAngleOffset = сurrentRotationAngleOffset + (360 - mMaxSweepAngle);
    }


    float getAnimatedFraction(ValueAnimator animator) {
        float fraction = animator.getDuration() > 0 ? ((float) animator.getCurrentPlayTime()) / animator.getDuration() : 1f;

        fraction %= 1f;
        fraction = Math.min(fraction, 1f);
        fraction = animator.getInterpolator().getInterpolation(fraction);
        return fraction;
    }

    public interface OnInvalidateListener {
        void invalidate();
    }
}
