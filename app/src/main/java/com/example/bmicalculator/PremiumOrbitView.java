package com.example.bmicalculator;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

public class PremiumOrbitView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF orbitBounds = new RectF();
    private ValueAnimator animator;
    private float phase = 0f;
    private int readiness = 50;

    public PremiumOrbitView(Context context) {
        super(context);
        init();
    }

    public PremiumOrbitView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PremiumOrbitView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        glowPaint.setMaskFilter(new BlurMaskFilter(dp(18), BlurMaskFilter.Blur.NORMAL));
    }

    public void setReadiness(int readiness) {
        this.readiness = Math.max(0, Math.min(100, readiness));
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopAnimation();
        super.onDetachedFromWindow();
    }

    private void startAnimation() {
        if (animator != null && animator.isStarted()) {
            return;
        }
        animator = ValueAnimator.ofFloat(0f, 360f);
        animator.setDuration(9000L);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            phase = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    private void stopAnimation() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();
        float cx = width * 0.5f;
        float cy = height * 0.52f;
        float radius = Math.min(width, height) * 0.28f;
        float intensity = 0.65f + (readiness / 100f) * 0.35f;

        drawStage(canvas, width, height);
        drawOrbit(canvas, cx, cy, radius * 1.42f, -14f, Color.argb(155, 221, 247, 250));
        drawOrbit(canvas, cx, cy, radius * 1.08f, 22f, Color.argb(175, 255, 205, 129));
        drawParticles(canvas, cx, cy, radius, intensity);
        drawSphere(canvas, cx, cy, radius, intensity);
    }

    private void drawStage(Canvas canvas, float width, float height) {
        paint.setShader(new LinearGradient(
                0f,
                0f,
                width,
                height,
                new int[]{Color.rgb(8, 17, 32), Color.rgb(11, 92, 108), Color.rgb(255, 122, 89)},
                new float[]{0f, 0.68f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawRoundRect(0f, 0f, width, height, dp(26), dp(26), paint);
        paint.setShader(null);
    }

    private void drawSphere(Canvas canvas, float cx, float cy, float radius, float intensity) {
        glowPaint.setColor(Color.argb(Math.round(120 * intensity), 27, 178, 200));
        canvas.drawCircle(cx, cy, radius * 1.22f, glowPaint);

        paint.setShader(new RadialGradient(
                cx - radius * 0.38f,
                cy - radius * 0.48f,
                radius * 1.35f,
                new int[]{
                        Color.rgb(255, 255, 255),
                        Color.rgb(143, 232, 221),
                        Color.rgb(18, 112, 136),
                        Color.rgb(9, 20, 34)
                },
                new float[]{0f, 0.23f, 0.68f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, radius, paint);
        paint.setShader(null);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2));
        paint.setColor(Color.argb(110, 255, 255, 255));
        canvas.drawCircle(cx - radius * 0.08f, cy - radius * 0.08f, radius * 0.78f, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawOrbit(Canvas canvas, float cx, float cy, float radius, float rotation, int color) {
        canvas.save();
        canvas.rotate(rotation + phase * 0.24f, cx, cy);
        orbitBounds.set(cx - radius, cy - radius * 0.34f, cx + radius, cy + radius * 0.34f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2));
        paint.setColor(color);
        canvas.drawOval(orbitBounds, paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.restore();
    }

    private void drawParticles(Canvas canvas, float cx, float cy, float radius, float intensity) {
        for (int i = 0; i < 5; i++) {
            float angle = (float) Math.toRadians(phase + i * 72f);
            float orbitX = (float) Math.cos(angle) * radius * (1.35f + i * 0.03f);
            float orbitY = (float) Math.sin(angle) * radius * 0.46f;
            int alpha = Math.round((120 + (i * 18)) * intensity);
            paint.setColor(Color.argb(Math.min(235, alpha), i % 2 == 0 ? 255 : 27, i % 2 == 0 ? 205 : 178, i % 2 == 0 ? 129 : 200));
            canvas.drawCircle(cx + orbitX, cy + orbitY, dp(i == 0 ? 5 : 4), paint);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
