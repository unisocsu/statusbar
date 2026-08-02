package com.example.mousecontrol;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.widget.Toast;

public class MouseSettingsHelper {

    private static final String SETTING_KEY = "mouse_support_list";

    public static void toggleAppMouseSupport(Context context, String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return;
        }

        ContentResolver resolver = context.getContentResolver();
        String currentList = Settings.Global.getString(resolver, SETTING_KEY);

        if (currentList == null) {
            currentList = "";
        }

        // בדיקה אם האפליקציה כבר קיימת ברשימה
        if (!currentList.contains(packageName)) {
            String updatedList = currentList + packageName + ",";
            try {
                Settings.Global.putString(resolver, SETTING_KEY, updatedList);
                Toast.makeText(context, "🖱️ עכבר הופעל עבור: " + packageName, Toast.LENGTH_SHORT).show();
            } catch (SecurityException e) {
                Toast.makeText(context, "❌ שגיאה: חסרה הרשאת WRITE_SECURE_SETTINGS", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(context, "ℹ️ האפליקציה כבר מוגדרת ברשימה!", Toast.LENGTH_SHORT).show();
        }
    }
}
