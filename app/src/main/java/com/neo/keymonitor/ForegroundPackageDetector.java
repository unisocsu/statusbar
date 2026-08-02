package com.neo.keymonitor;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

public class ForegroundPackageDetector {
    private static final String TAG = "ForegroundDetector";

    public static String getForegroundPackage(Context context) {
        // 1. שליפה דרך dumpsys window ⚡
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "dumpsys window"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("mCurrentFocus") || line.contains("mFocusedApp")) {
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
            Log.e(TAG, "Error executing dumpsys window: " + e.getMessage());
            e.printStackTrace();
        }

        // 2. גיבוי דרך ActivityManager 🛡️
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
            Log.e(TAG, "Error in ActivityManager fallback: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }
}
