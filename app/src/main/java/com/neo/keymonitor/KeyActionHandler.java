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

    private static final String TAG = "KeyActionHandlerDebug";
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
        // 🔍 בדיקה ראשונית: האם שדור לחיצת מקש מגיע לכאן בכלל?
        Log.e(TAG, "Key event received! Code: " + code + ", Value: "  + value);

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
        Log.e(TAG, "Current foreground package detected: " + currentPackage);
        if (currentPackage == null || currentPackage.isEmpty()) {
            Log.e(TAG, "Foreground package is null or empty!");
            return;
        }

        try {
            String mouseList = Settings.Global.getString(context.getContentResolver(), "mouse_support_list");
            if (mouseList == null) {
                mouseList = "";
            }

            if (!mouseList.contains(currentPackage)) {
                String updatedList = mouseList + currentPackage + ",";
                Log.e(TAG, "Adding package to mouse_support_list: " + updatedList);
                ShellExecutor.getInstance().execute("settings put global mouse_support_list \"" + updatedList + "\"");
            } else {
                Log.e(TAG, "Package already exists in mouse_support_list.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in checkAndToggleMouseForCurrentApp: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getForegroundPackage() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "dumpsys window"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("mCurrentFocus") || line.contains("mFocusedApp")) {
                    Log.e(TAG, "Found focus line: " + line);
                    int index = line.indexOf("u0 ");
                    if (index != -1) {
                        String sub = line.substring(index + 3);
                        String pkgAndActivity = sub.split(" ")[0];
                        String pkg = pkgAndActivity.split("/")[0];
                        if (pkg != null && !pkg.isEmpty()) {
                            process.destroy();
                            return pkg.trim();
                        }
                    }
                }
            }
            process.destroy();
        } catch (Exception e) {
            Log.e(TAG, "Error executing dumpsys window via root: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
                if (taskInfo != null && !taskInfo.isEmpty()) {
                    ComponentName componentInfo = taskInfo.get(0).topActivity;
                    if (componentInfo != null) {
                        Log.e(TAG, "Fallback ActivityManager package: " + componentInfo.getPackageName());
                        return componentInfo.getPackageName();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in ActivityManager fallback: " + e.getMessage());
            e.printStackTrace();
        }

        Log.e(TAG, "Failed to detect foreground package!");
        return null;
    }
}
