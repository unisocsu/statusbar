package com.neo.keymonitor;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
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
        if (currentPackage == null || currentPackage.isEmpty()) return;

        try {
            // 1. קריאת הרשימה הקיימת 📋
            String mouseList = Settings.Global.getString(context.getContentResolver(), "mouse_support_list");
            if (mouseList == null) {
                mouseList = "";
            }

            // 2. בדיקה אם ה-Package כבר קיים, ואם לא – הוספה ושמירה 🛠️
            if (!mouseList.contains(currentPackage)) {
                String updatedList = mouseList + currentPackage + ",";
                
                // כתיבה ל-Settings.Global בעזרת الـ ShellExecutor הקיים בפרויקט ⚡
                ShellExecutor.getInstance().execute("settings put global mouse_support_list \"" + updatedList + "\"");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getForegroundPackage() {
        // 1. ניסיון שליפה מהיר דרך פקודת Root על החלון הפעיל ⚡
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "dumpsys window windows | grep -E 'mCurrentFocus|mFocusedApp'"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("/") && !line.contains("StatusBar")) {
                    String[] parts = line.split("u0 ");
                    if (parts.length > 1) {
                        String pkgAndActivity = parts[1].split(" ")[0];
                        String pkg = pkgAndActivity.split("/")[0];
                        if (pkg != null && !pkg.isEmpty()) {
                            return pkg.trim();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. גיבוי בטוח דרך ActivityManager 🛡️
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
                if (taskInfo != null && !taskInfo.isEmpty()) {
                    ComponentName componentInfo = taskInfo.get(0).topActivity;
                    if (componentInfo != null) {
                        return componentInfo.getPackageName();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
