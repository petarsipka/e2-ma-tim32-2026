package com.example.slagalica.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.slagalica.R;

/**
 * Win-rate ring (Sunny pop .win-ring): a track circle with an accent arc swept
 * to {@code percent}, and the percentage drawn in the centre. Defaults to ~64dp.
 */
public class StatRingView extends View {

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcRect = new RectF();

    private int percent = 0;
    private float strokeWidth;

    public StatRingView(Context context) {
        this(context, null);
    }

    public StatRingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;
        strokeWidth = 7f * density;

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(strokeWidth);
        trackPaint.setColor(ContextCompat.getColor(context, R.color.track));

        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(strokeWidth);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);
        arcPaint.setColor(ContextCompat.getColor(context, R.color.accent));

        textPaint.setColor(ContextCompat.getColor(context, R.color.text));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(16f * density);
        textPaint.setTypeface(ResourcesCompat.getFont(context, R.font.baloo2));
    }

    /** Set the win percentage (0–100) and redraw. */
    public void setPercent(int percent) {
        this.percent = Math.max(0, Math.min(100, percent));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float pad = strokeWidth / 2f + 1f;
        arcRect.set(pad, pad, getWidth() - pad, getHeight() - pad);

        canvas.drawOval(arcRect, trackPaint);
        canvas.drawArc(arcRect, -90f, percent * 3.6f, false, arcPaint);

        float cy = getHeight() / 2f - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(percent + "%", getWidth() / 2f, cy, textPaint);
    }
}
