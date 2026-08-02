package com.neo.keymonitor;

import android.content.Context;
import android.content.SharedPreferences;

public class PoundKeyHandler {
    private static long startTime = 0;

    public static void handle(Context context, int value) {
        SharedPreferences prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE);

        if (value == 1) {
            startTime = System.currentTimeMillis();
        } else if (value == 0 && startTime != 0) {
            if (System.currentTimeMillis() - startTime >= 1500) {
                if (prefs.getBoolean("enable_recents", true)) {
                    // הפעלת פקודת הקלט הישירה לפתיחת האפליקציות האחרונות ⚡
                    ShellExecutor.getInstance().execute("input keyevent 187");
                }
            }
            startTime = 0;
        }
    }
}
