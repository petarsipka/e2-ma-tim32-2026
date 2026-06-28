package com.example.slagalica.util;

import android.app.Activity;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SystemBars {
    public static void apply(Activity activity) {
        ViewCompat.setOnApplyWindowInsetsListener(
                activity.findViewById(android.R.id.content),
                (v, insets) -> {
                    int top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
                    int bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
                    v.setPadding(0, top, 0, bottom);
                    return WindowInsetsCompat.CONSUMED;
                }
        );
    }
}
