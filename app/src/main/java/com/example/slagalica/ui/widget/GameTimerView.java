package com.example.slagalica.ui.widget;

import android.content.Context;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.view.Gravity;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.slagalica.R;

import java.util.Locale;

/**
 * Reusable countdown timer ring (Sunny pop .timer). Drop into any in-game layout
 * (typically 58dp × 58dp) and drive it with {@link #start(long)} /
 * {@link #start(long, Runnable)}; the view owns the ring look, the ticking and the
 * [m]m:ss formatting, so each screen only supplies the duration.
 */
public class GameTimerView extends AppCompatTextView {

    private CountDownTimer timer;

    public GameTimerView(Context context) {
        super(context);
        init();
    }

    public GameTimerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GameTimerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setBackgroundResource(R.drawable.bg_timer_ring);
        setGravity(Gravity.CENTER);
        setIncludeFontPadding(false);
        setTypeface(ResourcesCompat.getFont(getContext(), R.font.baloo2));
        setTextColor(ContextCompat.getColor(getContext(), R.color.text));
        if (getText() == null || getText().length() == 0) setText("0:00");
    }

    /** Start a countdown of the given duration with no completion callback. */
    public void start(long durationMillis) {
        start(durationMillis, null);
    }

    /** Start a countdown; {@code onFinish} runs once the timer reaches zero. */
    public void start(long durationMillis, @Nullable Runnable onFinish) {
        cancel();
        setText(format(durationMillis - 1));
        timer = new CountDownTimer(durationMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                setText(format(millisUntilFinished));
            }

            @Override
            public void onFinish() {
                setText("0:00");
                if (onFinish != null) onFinish.run();
            }
        }.start();
    }

    /** Stop the countdown without firing the completion callback. */
    public void cancel() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    /** Display 0:00 (e.g. once a question has been answered). */
    public void showZero() {
        setText("0:00");
    }

    /** [m]m:ss — minute digit(s) without a leading zero, seconds always two digits.
     *  Rounds up so the final second reads 0:01 rather than 0:00. */
    private static String format(long millisUntilFinished) {
        long totalSeconds = millisUntilFinished / 1000 + 1;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return minutes + ":" + String.format(Locale.getDefault(), "%02d", seconds);
    }
}
