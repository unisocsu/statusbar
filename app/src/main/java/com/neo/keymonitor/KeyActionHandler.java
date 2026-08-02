package com.neo.keymonitor;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import java.util.List;

public class KeyActionHandler implements InputEventReader.OnKeyEventListener {

    private final Context context;
    private final SharedPreferences prefs;

    private long startTimeMenu = 0;
    private long startTimePound = 0;
    private long startTimeFive = 0;

    public KeyActionHandler(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences("app_settings", Context.MODE_PRIVATE);
    }

    @Override
    public void onKeyEvent(int code, int value) {
        switch (code) {
            case 139: // מקש Menu 📜
                handleMenuKey(value);
                break;
            case 228: // מקש # 📱
                handlePoundKey(value);
                break;
            case 6:   // מקש 5 🖱️
                handleFiveKey(value);
                break;
        }
    }

    private void handleMenuKey(int value) {
        if (value == 1) {
            startTimeMenu = System.currentTimeMillis();
        } else if (value == 0 && startTimeMenu != 0) {
            if (System.currentTimeMillis() - startTimeMenu >= 1000) {
                if (prefs.getBoolean("enable_wilon", true)) {
                    ShellExecutor.getInstance().execute("service call statusbar 1");
                }
            }
            startTimeMenu = 0;
        }
    }

    private void handlePoundKey(int value) {
        if (value == 1) {
            startTimePound = System.currentTimeMillis();
        } else if (value == 0 && startTimePound != 0) {
            if (System.currentTimeMillis() - startTimePound >= 1500) {
                if (prefs.getBoolean("enable_recents", true)) {
                    ShellExecutor.getInstance().execute("service call statusbar 2");
                }
            }
            startTimePound = 0;
        }
    }

    private void handleFiveKey(int value) {
        if (value == 1) {
            startTimeFive = System.currentTimeMillis();
        } else if (value == 0 && startTimeFive != 0) {
            if (System.currentTimeMillis() - startTimeFive >= 1000) {
                if (prefs.getBoolean("enable_mouse", true)) {
                    checkAndToggleMouseForCurrentApp();
                }
            }
            startTimeFive = 0;
        }
    }

    private void checkAndToggleMouseForCurrentApp() {
        String currentPackage = getForegroundPackage();
        if (currentPackage == null) return;

        try {
            String mouseList = Settings.Global.getString(context.getContentResolver(), "mouse_support_list");
            if (mouseList != null && mouseList.contains(currentPackage)) {
                ShellExecutor.getInstance().execute("settings put global mouse_mode_enabled 1");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getForegroundPackage() {
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
            if (taskInfo != null && !taskInfo.isEmpty()) {
                ComponentName componentInfo = taskInfo.get(0).topActivity;
                return componentInfo.getPackageName();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
