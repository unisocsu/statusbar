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
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class ButtonMonitorService extends Service {
    private Thread monitorThread;
    private volatile boolean isRunning = true;
    private Process eventProcess;
    private Process commandShellProcess;
    private DataOutputStream commandShellStream;
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        startForegroundServiceKitKat();
        initCommandShell(); // פתיחת Shell רוט יחיד וקבוע ⚡
        startMonitoringLoop();
    }

    @SuppressWarnings("deprecation")
    private void startForegroundServiceKitKat() {
        Notification notification = new Notification();
        startForeground(1, notification);
    }

    // יצירת צינור תקשורת קבוע לפקודות Shell 🚀
    private void initCommandShell() {
        try {
            commandShellProcess = Runtime.getRuntime().exec("su");
            commandShellStream = new DataOutputStream(commandShellProcess.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startMonitoringLoop() {
        monitorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // הקצאת זיכרון יחידה מחוץ ללולאה - מונע עומס על ה-RAM וה-GC 🧹
                byte[] buffer = new byte[16];

                while (isRunning) {
                    DataInputStream dis = null;
                    try {
                        eventProcess = Runtime.getRuntime().exec("su");
                        DataOutputStream os = new DataOutputStream(eventProcess.getOutputStream());
                        os.writeBytes("cat /dev/input/event0\n");
                        os.flush();

                        dis = new DataInputStream(eventProcess.getInputStream());

                        long startTimeMenu = 0;
                        long startTimePound = 0;
                        long startTimeFive = 0;

                        while (isRunning) {
                            dis.readFully(buffer); // קריאה חסכונית בלבד

                            int type = ((buffer[9] & 0xFF) << 8) | (buffer[8] & 0xFF);
                            
                            // סינון מהיר: אם זה לא אירוע מקש, מדלגים מיד ⚡
                            if (type != 1) continue;

                            int code = ((buffer[11] & 0xFF) << 8) | (buffer[10] & 0xFF);
                            int value = ((buffer[15] & 0xFF) << 24) | ((buffer[14] & 0xFF) << 16) 
                                      | ((buffer[13] & 0xFF) << 8) | (buffer[12] & 0xFF);

                            // 1. מקש Menu (קוד 139) 📜 -> וילון הסטטוס באר
                            if (code == 139) {
                                if (value == 1) {
                                    startTimeMenu = System.currentTimeMillis();
                                } else if (value == 0 && startTimeMenu != 0) {
                                    if (System.currentTimeMillis() - startTimeMenu >= 1000) {
                                        if (prefs.getBoolean("enable_wilon", true)) {
                                            runSuCommandFast("service call statusbar 1");
                                        }
                                    }
                                    startTimeMenu = 0;
                                }
                            }
                            // 2. מקש # (קוד 228) 📱 -> אפליקציות אחרונות
                            else if (code == 228) {
                                if (value == 1) {
                                    startTimePound = System.currentTimeMillis();
                                } else if (value == 0 && startTimePound != 0) {
                                    if (System.currentTimeMillis() - startTimePound >= 1500) {
                                        if (prefs.getBoolean("enable_recents", true)) {
                                            runSuCommandFast("service call statusbar 2");
                                        }
                                    }
                                    startTimePound = 0;
                                }
                            }
                            // 3. מקש 5 (קוד 6) 🖱️ -> עכבר וירטואלי
                            else if (code == 6) {
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
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (dis != null) dis.close();
                            if (eventProcess != null) eventProcess.destroy();
                        } catch (Exception e) {}
                    }

                    if (isRunning) {
                        try { Thread.sleep(1000); } catch (InterruptedException e) { break; }
                    }
                }
            }
        });
        monitorThread.start();
    }

    // הרצה מיידית ללא יצירת תהליך חדש 🏎️💨
    private synchronized void runSuCommandFast(String command) {
        try {
            if (commandShellStream != null) {
                commandShellStream.writeBytes(command + "\n");
                commandShellStream.flush();
            } else {
                initCommandShell();
                if (commandShellStream != null) {
                    commandShellStream.writeBytes(command + "\n");
                    commandShellStream.flush();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            initCommandShell();
        }
    }

    private void checkAndToggleMouseForCurrentApp() {
        String currentPackage = getForegroundPackage();
        if (currentPackage == null) return;

        try {
            String mouseList = Settings.Global.getString(getContentResolver(), "mouse_support_list");
            if (mouseList != null && mouseList.contains(currentPackage)) {
                runSuCommandFast("settings put global mouse_mode_enabled 1");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getForegroundPackage() {
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
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

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        if (eventProcess != null) eventProcess.destroy();
        if (commandShellProcess != null) commandShellProcess.destroy();
        if (monitorThread != null) monitorThread.interrupt();
        super.onDestroy();
    }
}
