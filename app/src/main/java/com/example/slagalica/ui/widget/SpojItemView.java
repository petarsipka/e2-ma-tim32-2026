package com.example.slagalica.ui.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.slagalica.R;

/**
 * One tile in a Spojnice column (Sunny pop .spoj-item). Owns its three looks —
 * neutral, selected and matched — so screens only call {@link #reset()},
 * {@link #markSelected()} or {@link #markMatched(int, int)} and never touch the
 * background/colour plumbing. Matched tiles are tinted with the pair colour and
 * show a small pair-number badge.
 */
public class SpojItemView extends FrameLayout {

    private final TextView label;
    private final TextView badge;

    public SpojItemView(Context context) {
        this(context, null);
    }

    public SpojItemView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.view_spoj_item, this, true);
        label = findViewById(R.id.spojText);
        badge = findViewById(R.id.spojBadge);
        setClickable(true);
        setFocusable(true);
        reset();
    }

    /** Set the displayed term. */
    public void setLabel(CharSequence text) {
        label.setText(text);
    }

    /** Neutral, unconnected look (.spoj-item). */
    public void reset() {
        setBackgroundResource(R.drawable.bg_spoj_item);
        setBackgroundTintList(null);
        label.setTextColor(ContextCompat.getColor(getContext(), R.color.text));
        badge.setVisibility(GONE);
    }

    /** Awaiting-its-pair look (.spoj-item.sel): dashed accent border. */
    public void markSelected() {
        setBackgroundResource(R.drawable.bg_spoj_item_sel);
        setBackgroundTintList(null);
        label.setTextColor(ContextCompat.getColor(getContext(), R.color.text));
        badge.setVisibility(GONE);
    }

    /** Connected look (.spoj-item.matched): solid pair colour + numbered badge. */
    public void markMatched(int color, int pairNumber) {
        setBackgroundResource(R.drawable.bg_spoj_item_matched);
        setBackgroundTintList(ColorStateList.valueOf(color));
        label.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
        badge.setBackgroundResource(R.drawable.bg_spoj_badge);
        badge.setText(String.valueOf(pairNumber));
        badge.setVisibility(VISIBLE);
    }

    /** Wrong, locked look (.wrong): red fill + ✕ badge; this choice can't be retried. */
    public void markWrong() {
        setBackgroundResource(R.drawable.bg_spoj_item_wrong);
        setBackgroundTintList(null);
        label.setTextColor(ContextCompat.getColor(getContext(), R.color.wrong));
        badge.setBackgroundResource(R.drawable.bg_spoj_badge_wrong);
        badge.setText("✕");
        badge.setVisibility(VISIBLE);
    }

    /** Hide/show the whole tile (e.g. once the game is over). */
    public void setShown(boolean shown) {
        setVisibility(shown ? VISIBLE : View.GONE);
    }
}
