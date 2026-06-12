package com.example.slagalica.util;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.util.Base64;

import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Turns a picked image into a small square Base64 string and back, so a profile
 * avatar can live as a plain string in Realtime Database (no Firebase Storage).
 *
 * <p>The image is center-cropped to a square, downscaled to {@link #AVATAR_SIZE}px
 * and JPEG-compressed, which keeps the encoded string at a few tens of kilobytes.
 */
public final class ImageUtils {

    /** Stored avatars are square, this many pixels per side. */
    public static final int AVATAR_SIZE = 256;
    private static final int JPEG_QUALITY = 70;

    private ImageUtils() {}

    /**
     * Decode {@code uri}, center-crop to a square, downscale and JPEG-encode it as a
     * Base64 string. Runs decoding/compression synchronously, so call it off the main
     * thread. Returns {@code null} if the image cannot be read.
     */
    @Nullable
    public static String uriToBase64(ContentResolver resolver, Uri uri) {
        try {
            ImageDecoder.Source source = ImageDecoder.createSource(resolver, uri);
            Bitmap decoded = ImageDecoder.decodeBitmap(source,
                    (decoder, info, src) -> decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE));
            Bitmap square = cropToSquare(decoded);
            Bitmap scaled = Bitmap.createScaledBitmap(square, AVATAR_SIZE, AVATAR_SIZE, true);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out);
            return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /** Decode a Base64 avatar string back into a bitmap, or {@code null} if invalid. */
    @Nullable
    public static Bitmap base64ToBitmap(@Nullable String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        try {
            byte[] bytes = Base64.decode(base64, Base64.NO_WRAP);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Bitmap cropToSquare(Bitmap src) {
        int dim = Math.min(src.getWidth(), src.getHeight());
        int x = (src.getWidth() - dim) / 2;
        int y = (src.getHeight() - dim) / 2;
        return Bitmap.createBitmap(src, x, y, dim, dim);
    }
}
