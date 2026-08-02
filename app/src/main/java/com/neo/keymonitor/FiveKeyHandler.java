package com.neo.keymonitor;

import android.content.Context;
import android.content.SharedPreferences;

public class FiveKeyHandler {
    private static long startTime = 0;

    public static void handle(Context context, int value) {
        SharedPreferences prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE);

        if (value == 1) {
            startTime = System.currentTimeMillis();
        } else if (value == 0 && startTime != 0) {
            if (System.currentTimeMillis() - startTime >= 1000) {
                if (prefs.getBoolean("enable_mouse", true)) {
                    // הפעלת תהליך זיהוי החבילה ועדכון ההגדרות ⚡
                    String currentPackage = ForegroundPackageDetector.getForegroundPackage(context);
                    if (currentPackage != null && !currentPackage.isEmpty()) {
                        MouseSettingsManager.toggleMouseSupport(context, currentPackage);
                    }
                }
            }
            startTime = 0;
        }
    }
}
