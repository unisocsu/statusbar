package com.neo.keymonitor;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

public class MouseSettingsManager {
    private static final String TAG = "MouseSettingsManager";

    public static void toggleMouseSupport(Context context, String packageName) {
        try {
            String mouseList = Settings.Global.getString(context.getContentResolver(), "mouse_support_list");
            if (mouseList == null) {
                mouseList = "";
            }

            if (mouseList.contains(packageName)) {
                // כיבוי - הסרת החבילה מהרשימה ❌
                String updatedList = mouseList.replace(packageName + ",", "");
                Log.e(TAG, "Turning OFF mouse for: " + packageName);
                ShellExecutor.getInstance().execute("settings put global mouse_support_list \"" + updatedList + "\"");
            } else {
                // הפעלה - הוספת החבילה לרשימה ✅
                String updatedList = mouseList + packageName + ",";
                Log.e(TAG, "Turning ON mouse for: " + packageName);
                ShellExecutor.getInstance().execute("settings put global mouse_support_list \"" + updatedList + "\"");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling mouse support: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
