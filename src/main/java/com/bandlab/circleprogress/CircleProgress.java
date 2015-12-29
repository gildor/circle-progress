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
import android.support.annotation.NonNull;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

public class CircleProgress implements Animatable {
    public static final int FULL_ROTATION_ANGLE = 360;
    public static final float UNIT_ANGLE = FULL_ROTATION_ANGLE / 100f;
    public static final Interpolator END_INTERPOLATOR = new LinearInterpolator();
    private static final Interpolator DEFAULT_ROTATION_INTERPOLATOR = new LinearInterpolator();
    private static final Interpolator DEFAULT_SWEEP_INTERPOLATOR = new DecelerateInterpolator();
    private static final int ROTATION_ANIMATOR_DURATION = 2000;
    private static final int SWEEP_ANIMATOR_DURATION = 600;
    private static final int END_ANIMATOR_DURATION = 200;
    private static final float SWEEP_SPEED = 1;
    private static final float ROTATION_SPEED = 1;
    public static final int QUART_OF_ROTATION_ANGLE = 90;
    private final Paint currentProgressPaint;
    private final Paint restProgress;
    private int donutRadius;
    private float currentRotationAngle;
    private float currentRotationAngleOffset;
    private float currentSweepAngle;
    private boolean running;
    private boolean modeAppearing;
    private float currentEndRatio;
    private ValueAnimator sweepAppearingAnimator;
    private ValueAnimator sweepDisappearingAnimator;
    private ValueAnimator rotationAnimator;
    private ValueAnimator endAnimator;
    private final Interpolator angleInterpolator;
    private final Interpolator sweepInterpolator;
    private static final int MIN_SWEEP_ANGLE = 20;
    private static final int MAX_SWEEP_ANGLE = 300;
    private boolean firstSweepAnimation;
    private OnInvalidateListener listener;
    private final RectF oval;
    private Paint donutPaint;
    private boolean isShowRestProgress = true;

    public CircleProgress() {

        sweepInterpolator = DEFAULT_SWEEP_INTERPOLATOR;
        angleInterpolator = DEFAULT_ROTATION_INTERPOLATOR;

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
        float startAngle = currentRotationAngle - currentRotationAngleOffset;
        float sweepAngle = currentSweepAngle;
        if (!modeAppearing) {
            startAngle = startAngle + (FULL_ROTATION_ANGLE - sweepAngle);
        }
        startAngle %= FULL_ROTATION_ANGLE;
        if (currentEndRatio < 1f) {
            float newSweepAngle = sweepAngle * currentEndRatio;
            startAngle = (startAngle + (sweepAngle - newSweepAngle)) % FULL_ROTATION_ANGLE;
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
        rotationAnimator = ValueAnimator.ofFloat(0f, FULL_ROTATION_ANGLE);
        rotationAnimator.setInterpolator(angleInterpolator);
        rotationAnimator.setDuration((long) (ROTATION_ANIMATOR_DURATION / ROTATION_SPEED));
        rotationAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float angle = getAnimatedFraction(animation) * FULL_ROTATION_ANGLE;
                setCurrentRotationAngle(angle);
            }
        });
        rotationAnimator.setRepeatCount(ValueAnimator.INFINITE);
        rotationAnimator.setRepeatMode(ValueAnimator.RESTART);

        sweepAppearingAnimator = ValueAnimator.ofFloat(MIN_SWEEP_ANGLE, MAX_SWEEP_ANGLE);
        sweepAppearingAnimator.setInterpolator(sweepInterpolator);
        sweepAppearingAnimator.setDuration((long) (SWEEP_ANIMATOR_DURATION / SWEEP_SPEED));
        sweepAppearingAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animatedFraction = getAnimatedFraction(animation);
                float angle;
                if (firstSweepAnimation) {
                    angle = animatedFraction * MAX_SWEEP_ANGLE;
                } else {
                    angle = MIN_SWEEP_ANGLE + animatedFraction * (MAX_SWEEP_ANGLE - MIN_SWEEP_ANGLE);
                }
                setCurrentSweepAngle(angle);
            }
        });
        sweepAppearingAnimator.addListener(getSweepAppearingListener());

        sweepDisappearingAnimator = ValueAnimator.ofFloat(MAX_SWEEP_ANGLE, MIN_SWEEP_ANGLE);
        sweepDisappearingAnimator.setInterpolator(sweepInterpolator);
        sweepDisappearingAnimator.setDuration((long) (SWEEP_ANIMATOR_DURATION / SWEEP_SPEED));
        sweepDisappearingAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animatedFraction = getAnimatedFraction(animation);
                setCurrentSweepAngle(
                        MAX_SWEEP_ANGLE - animatedFraction * (MAX_SWEEP_ANGLE - MIN_SWEEP_ANGLE));
            }
        });
        sweepDisappearingAnimator.addListener(getSweepDisappearingListener());
        endAnimator = ValueAnimator.ofFloat(1f, 0f);
        endAnimator.setInterpolator(END_INTERPOLATOR);
        endAnimator.setDuration(END_ANIMATOR_DURATION);
        endAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setEndRatio(1f - getAnimatedFraction(animation));

            }
        });
        endAnimator.addListener(getEndAnimatorListener());
    }

    @NonNull
    private Animator.AnimatorListener getSweepAppearingListener() {
        return new Animator.AnimatorListener() {
            boolean cancelled;

            @Override
            public void onAnimationStart(Animator animation) {
                cancelled = false;
                modeAppearing = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!cancelled) {
                    firstSweepAnimation = false;
                    setDisappearing();
                    sweepDisappearingAnimator.start();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                cancelled = true;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                //No implementation
            }
        };
    }

    @NonNull
    private Animator.AnimatorListener getSweepDisappearingListener() {
        return new Animator.AnimatorListener() {
            boolean cancelled;

            @Override
            public void onAnimationStart(Animator animation) {
                cancelled = false;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!cancelled) {
                    setAppearing();
                    sweepAppearingAnimator.start();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                cancelled = true;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                //No implementation
            }
        };
    }

    @NonNull
    private Animator.AnimatorListener getEndAnimatorListener() {
        return new Animator.AnimatorListener() {
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
                //No implementation
            }
        };
    }

    private void stopAnimators() {
        rotationAnimator.cancel();
        sweepAppearingAnimator.cancel();
        sweepDisappearingAnimator.cancel();
        endAnimator.cancel();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    public void setCurrentRotationAngle(float currentRotationAngle) {
        this.currentRotationAngle = currentRotationAngle;
        invalidateSelf();
    }

    public void setCurrentSweepAngle(float currentSweepAngle) {
        this.currentSweepAngle = currentSweepAngle;
        invalidateSelf();
    }

    private void setEndRatio(float ratio) {
        this.currentEndRatio = ratio;
        invalidateSelf();
    }

    public void drawProgress(Canvas canvas, Point point, int progress, int radius) {
        drawArc(canvas, point, radius, -QUART_OF_ROTATION_ANGLE, UNIT_ANGLE * progress);
    }

    public void setOnInvalidateListener(OnInvalidateListener listener) {
        this.listener = listener;
    }

    @Override
    public void start() {
        if (isRunning()) {
            return;
        }
        running = true;
        reinitValues();
        rotationAnimator.start();
        sweepAppearingAnimator.start();
        invalidateSelf();
    }

    private void invalidateSelf() {
        if (listener != null) {
            listener.invalidate();
        }
    }

    private void reinitValues() {
        firstSweepAnimation = true;
        currentEndRatio = 1f;
    }

    @Override
    public void stop() {
        if (!isRunning()) {
            return;
        }
        running = false;
        stopAnimators();
        invalidateSelf();
    }

    private void setAppearing() {
        modeAppearing = true;
        currentRotationAngleOffset += MIN_SWEEP_ANGLE;
    }

    private void setDisappearing() {
        modeAppearing = false;
        currentRotationAngleOffset =
                currentRotationAngleOffset + (FULL_ROTATION_ANGLE - MAX_SWEEP_ANGLE);
    }

    float getAnimatedFraction(ValueAnimator animator) {
        float fraction = animator.getDuration() > 0 ?
                ((float) animator.getCurrentPlayTime()) / animator.getDuration() : 1f;

        fraction %= 1f;
        fraction = Math.min(fraction, 1f);
        fraction = animator.getInterpolator().getInterpolation(fraction);
        return fraction;
    }

    public interface OnInvalidateListener {
        void invalidate();
    }
}
