package com.rudra.myassistant;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.core.content.ContextCompat;

public class AnimatedVisualizer extends View {
    private Paint arcPaint;
    private Paint glowPaint;
    private int centerX, centerY;
    private float radius;
    private AnimatorSet animatorSet;
    private float rotation1 = 0f;
    private float rotation2 = 0f;
    private float rotation3 = 0f;
    private float rotation4 = 0f;
    private float scale = 1f;
    private float opacity = 0.9f;

    public AnimatedVisualizer(Context context) {
        super(context);
        init();
    }

    public AnimatedVisualizer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AnimatedVisualizer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        int purpleColor = ContextCompat.getColor(getContext(), R.color.purple_accent);

        // Arc paint
        arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setColor(purpleColor);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(2f);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        // Glow paint for center
        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setColor(purpleColor);
        glowPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2;
        centerY = h / 2;
        radius = Math.min(w, h) / 2f;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
    }

    private void startAnimation() {
        // Rotation animations
        ObjectAnimator rotate1 = ObjectAnimator.ofFloat(this, "rotation1", 0f, 360f);
        rotate1.setDuration(6000);
        rotate1.setInterpolator(new LinearInterpolator());
        rotate1.setRepeatCount(ValueAnimator.INFINITE);

        ObjectAnimator rotate2 = ObjectAnimator.ofFloat(this, "rotation2", 360f, 0f);
        rotate2.setDuration(8000);
        rotate2.setInterpolator(new LinearInterpolator());
        rotate2.setRepeatCount(ValueAnimator.INFINITE);

        ObjectAnimator rotate3 = ObjectAnimator.ofFloat(this, "rotation3", 0f, 360f);
        rotate3.setDuration(10000);
        rotate3.setInterpolator(new LinearInterpolator());
        rotate3.setRepeatCount(ValueAnimator.INFINITE);

        ObjectAnimator rotate4 = ObjectAnimator.ofFloat(this, "rotation4", 180f, 540f);
        rotate4.setDuration(12000);
        rotate4.setInterpolator(new LinearInterpolator());
        rotate4.setRepeatCount(ValueAnimator.INFINITE);

        // Scale animation
        ObjectAnimator scaleAnim = ObjectAnimator.ofFloat(this, "scale", 1f, 1.15f);
        scaleAnim.setDuration(2500);
        scaleAnim.setRepeatCount(ValueAnimator.INFINITE);
        scaleAnim.setRepeatMode(ValueAnimator.REVERSE);

        // Opacity animation
        ObjectAnimator opacityAnim = ObjectAnimator.ofFloat(this, "opacity", 0.7f, 1f);
        opacityAnim.setDuration(2000);
        opacityAnim.setRepeatCount(ValueAnimator.INFINITE);
        opacityAnim.setRepeatMode(ValueAnimator.REVERSE);

        animatorSet = new AnimatorSet();
        animatorSet.playTogether(rotate1, rotate2, rotate3, rotate4, scaleAnim, opacityAnim);
        animatorSet.start();
    }

    private void stopAnimation() {
        if (animatorSet != null) {
            animatorSet.cancel();
            animatorSet = null;
        }
    }

    // Getters and setters for animation properties
    public float getRotation1() {
        return rotation1;
    }

    public void setRotation1(float rotation1) {
        this.rotation1 = rotation1;
        invalidate();
    }

    public float getRotation2() {
        return rotation2;
    }

    public void setRotation2(float rotation2) {
        this.rotation2 = rotation2;
        invalidate();
    }

    public float getRotation3() {
        return rotation3;
    }

    public void setRotation3(float rotation3) {
        this.rotation3 = rotation3;
        invalidate();
    }

    public float getRotation4() {
        return rotation4;
    }

    public void setRotation4(float rotation4) {
        this.rotation4 = rotation4;
        invalidate();
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
        invalidate();
    }

    public float getOpacity() {
        return opacity;
    }

    public void setOpacity(float opacity) {
        this.opacity = opacity;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        canvas.translate(centerX, centerY);
        canvas.scale(scale, scale);
        canvas.translate(-centerX, -centerY);

        // Set opacity
        int alpha = (int) (255 * opacity);
        arcPaint.setAlpha(alpha);
        glowPaint.setAlpha((int) (alpha * 0.4f));

        // Draw outer arcs
        canvas.save();
        canvas.rotate(rotation1, centerX, centerY);
        drawArc(canvas, radius, 0, 180);
        drawArc(canvas, radius, 180, 360);
        canvas.restore();

        // Draw second layer
        canvas.save();
        canvas.rotate(rotation2, centerX, centerY);
        drawArc(canvas, radius * 0.85f, 45, 225);
        drawArc(canvas, radius * 0.85f, 225, 405);
        canvas.restore();

        // Draw third layer
        canvas.save();
        canvas.rotate(rotation3, centerX, centerY);
        drawArc(canvas, radius * 0.7f, 90, 270);
        drawArc(canvas, radius * 0.7f, 270, 450);
        canvas.restore();

        // Draw fourth layer
        canvas.save();
        canvas.rotate(rotation4, centerX, centerY);
        drawArc(canvas, radius * 0.55f, 135, 315);
        drawArc(canvas, radius * 0.55f, 315, 495);
        canvas.restore();

        // Draw center glow
        float centerRadius = radius * 0.35f;
        RadialGradient gradient = new RadialGradient(
                centerX, centerY, centerRadius,
                new int[]{0x669D4EDD, 0x009D4EDD},
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP
        );
        glowPaint.setShader(gradient);
        canvas.drawCircle(centerX, centerY, centerRadius, glowPaint);

        // Inner glow
        float innerRadius = radius * 0.2f;
        RadialGradient innerGradient = new RadialGradient(
                centerX, centerY, innerRadius,
                new int[]{0x999D4EDD, 0x009D4EDD},
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP
        );
        glowPaint.setShader(innerGradient);
        canvas.drawCircle(centerX, centerY, innerRadius, glowPaint);

        canvas.restore();
    }

    private void drawArc(Canvas canvas, float r, float startAngle, float sweepAngle) {
        Path path = new Path();
        path.addArc(
                centerX - r, centerY - r,
                centerX + r, centerY + r,
                startAngle, sweepAngle - startAngle
        );
        canvas.drawPath(path, arcPaint);
    }
}
