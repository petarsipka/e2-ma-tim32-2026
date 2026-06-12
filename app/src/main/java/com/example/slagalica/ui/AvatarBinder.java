package com.example.slagalica.ui;

import android.graphics.Bitmap;
import android.graphics.Outline;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.example.slagalica.data.AvatarRepository;
import com.example.slagalica.util.ImageUtils;

/**
 * Shared loading of the player avatar so every screen shows the same profile photo
 * (stored as Base64 in RTDB, see {@link AvatarRepository}) instead of initials or a
 * generic placeholder. Clips the target {@link ImageView} to a circle.
 */
public final class AvatarBinder {

    private static final AvatarRepository repo = new AvatarRepository();

    private AvatarBinder() {}

    /** Clip an ImageView's content to a circle (applied at draw time, after layout). */
    public static void circleClip(ImageView iv) {
        iv.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
        iv.setClipToOutline(true);
    }

    /**
     * Show {@code bitmap} in {@code photo} and hide {@code fallback}; or revert to the
     * fallback (initials view) when the bitmap is {@code null}. {@code fallback} may be
     * {@code null} for screens with no initials view.
     */
    public static void show(ImageView photo, @Nullable View fallback, @Nullable Bitmap bitmap) {
        if (bitmap != null) {
            photo.setImageBitmap(bitmap);
            photo.setVisibility(View.VISIBLE);
            if (fallback != null) fallback.setVisibility(View.INVISIBLE);
        } else {
            photo.setVisibility(View.GONE);
            if (fallback != null) fallback.setVisibility(View.VISIBLE);
        }
    }

    /** Load the current player's photo into {@code photo}, falling back to {@code fallback}. */
    public static void bindCurrentUser(ImageView photo, @Nullable View fallback) {
        circleClip(photo);
        repo.loadAvatar(b64 -> show(photo, fallback, ImageUtils.base64ToBitmap(b64)));
    }

    /** Load {@code uid}'s photo into {@code photo}, falling back to {@code fallback}. */
    public static void bindUser(@Nullable String uid, ImageView photo, @Nullable View fallback) {
        circleClip(photo);
        repo.loadAvatar(uid, b64 -> show(photo, fallback, ImageUtils.base64ToBitmap(b64)));
    }

    /**
     * Load the current player's photo into {@code photo}, but keep whatever placeholder
     * {@code photo} already shows when no photo is set (for headers with no initials view).
     */
    public static void bindCurrentUserOrPlaceholder(ImageView photo) {
        circleClip(photo);
        repo.loadAvatar(b64 -> {
            Bitmap bitmap = ImageUtils.base64ToBitmap(b64);
            if (bitmap != null) photo.setImageBitmap(bitmap);
        });
    }
}
