package com.neo.keymonitor;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.provider.Settings;
import java.io.DataInputStream;
import java.io.OutputStream;
import java.util.List;

public class ButtonMonitorService extends Service {
    private Thread monitorThread;
    private boolean isRunning = true;
    private Process suProcess;
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        startForegroundServiceKitKat();
        startMonitoringLoop();
    }

    // הפעלה שקטה ללא הודעה בווילון ההתראות באנדרואיד 4.4 🤫
    private void startForegroundServiceKitKat() {
        Notification notification = new Notification();
        startForeground(1, notification);
    }

    private void startMonitoringLoop() {
        monitorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isRunning) {
                    DataInputStream dis = null;
                    try {
                        suProcess = Runtime.getRuntime().exec("su");
                        OutputStream os = suProcess.getOutputStream();
                        
                        // קריאת אירועי הדרייבר הבינארי ללא תיווך ⚡
                        os.write("cat /dev/input/event0\n".getBytes());
                        os.flush();

                        dis = new DataInputStream(suProcess.getInputStream());
                        byte[] buffer = new byte[16];

                        long startTimeMenu = 0;
                        long startTimePound = 0;
                        long startTimeFive = 0;

                        while (isRunning) {
                            dis.readFully(buffer);

                            int type = ((buffer[9] & 0xFF) << 8) | (buffer[8] & 0xFF);
                            int code = ((buffer[11] & 0xFF) << 8) | (buffer[10] & 0xFF);
                            int value = ((buffer[15] & 0xFF) << 24) | ((buffer[14] & 0xFF) << 16) 
                                      | ((buffer[13] & 0xFF) << 8) | (buffer[12] & 0xFF);

                            // 1. ניטור מקש Menu (קוד 139) 📜 - פתיחת וילון הסטטוס באר
                            if (type == 1 && code == 139) {
                                if (value == 1) {
                                    startTimeMenu = System.currentTimeMillis();
                                } else if (value == 0 && startTimeMenu != 0) {
                                    if (System.currentTimeMillis() - startTimeMenu >= 1000) {
                                        triggerSystemUIWilon();
                                    }
                                    startTimeMenu = 0;
                                }
                            }

                            // 2. ניטור מקש # (קוד 228) 📱 - פתיחת תפריט אפליקציות אחרונות (Recents)
                            if (type == 1 && code == 228) {
                                if (value == 1) {
                                    startTimePound = System.currentTimeMillis();
                                } else if (value == 0 && startTimePound != 0) {
                                    if (System.currentTimeMillis() - startTimePound >= 1500) {
                                        if (prefs.getBoolean("enable_recents", true)) {
                                            openRecentsMenu();
                                        }
                                    }
                                    startTimePound = 0;
                                }
                            }

                            // 3. ניטור מקש 5 (קוד 6) 🖱️ - הפעלת עכבר וירטואלי
                            if (type == 1 && code == 6) {
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

                            // 4. אכיפת נעילת שורת הסטטוס 🔒 - בהתאם למצב האפליקציה שבקדמה
                            enforceStatusBarPolicy();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (dis != null) dis.close();
                            if (suProcess != null) suProcess.destroy();
                        } catch (Exception e) {}
                    }

                    if (isRunning) {
                        try { Thread.sleep(1500); } catch (InterruptedException e) { break; }
                    }
                }
            }
        });
        monitorThread.start();
    }

    // פתיחת וילון ההתראות (Status Bar) 📜
    private void triggerSystemUIWilon() {
        try {
            Process actionProcess = Runtime.getRuntime().exec("su");
            OutputStream os = actionProcess.getOutputStream();
            os.write("service call statusbar 1\n".getBytes());
            os.flush();
            os.write("exit\n".getBytes());
            os.flush();
            actionProcess.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // פתיחת תפריט אפליקציות אחרונות (Recents) 📱
    private void openRecentsMenu() {
        try {
            Process actionProcess = Runtime.getRuntime().exec("su");
            OutputStream os = actionProcess.getOutputStream();
            os.write("service call statusbar 2\n".getBytes());
            os.flush();
            os.write("exit\n".getBytes());
            os.flush();
            actionProcess.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // בדיקת האפליקציה שבקדמה והפעלת עכבר וירטואלי לפי mouse_support_list 🖱️
    private void checkAndToggleMouseForCurrentApp() {
        String currentPackage = getForegroundPackage();
        if (currentPackage == null) return;

        try {
            String mouseList = Settings.Global.getString(getContentResolver(), "mouse_support_list");
            if (mouseList != null && mouseList.contains(currentPackage)) {
                Runtime.getRuntime().exec("su -c settings put global mouse_mode_enabled 1");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // אכיפת מדיניות ה-Status Bar 🔒
    private void enforceStatusBarPolicy() {
        String currentPackage = getForegroundPackage();
        if (currentPackage == null) return;

        int mode = prefs.getInt("statusbar_" + currentPackage, 1); // 0=Allow, 1=Block, 2=Ask

        if (mode == 1) { // חסום הסתרה - השאר את שורת הסטטוס גלויה תמיד
            try {
                Runtime.getRuntime().exec("su -c settings put global policy_control null");
            } catch (Exception e) {}
        }
    }

    // זיהוי שם החבילה (Package Name) של האפליקציה הפעילה כרגע על המסך 🔍
    private String getForegroundPackage() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
        if (taskInfo != null && !taskInfo.isEmpty()) {
            ComponentName componentInfo = taskInfo.get(0).topActivity;
            return componentInfo.getPackageName();
        }
        return null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        if (suProcess != null) suProcess.destroy();
        if (monitorThread != null) monitorThread.interrupt();
        super.onDestroy();
    }
}
