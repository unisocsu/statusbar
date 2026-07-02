package com.neo.keymonitor;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class ButtonMonitorService extends Service {
    private Thread monitorThread;
    private boolean isRunning = true;
    private Process suProcess;

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundServiceKitKat();
        startMonitoringLoop();
    }

    @SuppressWarnings("deprecation")
    private void startForegroundServiceKitKat() {
        Notification notification = new Notification.Builder(this)
                .setContentTitle("מנטר מקשים פעיל")
                .setContentText("מאזין ללחצן תפריט בזמן אמת")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();
                
        startForeground(1, notification);
    }

    private void startMonitoringLoop() {
        monitorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isRunning) {
                    BufferedReader reader = null;
                    OutputStream os = null;
                    try {
                        // הפעלת תהליך su בצורה יציבה ומבוקרת
                        suProcess = Runtime.getRuntime().exec("su");
                        os = suProcess.getOutputStream();
                        
                        // שליחת הפקודה המדויקת שבדקת ועבדה ב-ADB
                        String command = "getevent -l /dev/input/event0\n";
                        os.write(command.getBytes("UTF-8"));
                        os.flush();

                        reader = new BufferedReader(new InputStreamReader(suProcess.getInputStream(), "UTF-8"));
                        String line;
                        long startTime = 0;

                        // לולאת קריאה ישירה מהזרם
                        while (isRunning && (line = reader.readLine()) != null) {
                            // זיהוי לחיצה (DOWN)
                            if (line.contains("KEY_MENU") && line.contains("DOWN")) {
                                startTime = System.currentTimeMillis();
                            } 
                            // זיהוי שחרור (UP)
                            else if (line.contains("KEY_MENU") && line.contains("UP")) {
                                if (startTime != 0) {
                                    long duration = System.currentTimeMillis() - startTime;
                                    // בדיקה אם הלחיצה נמשכה לפחות שנייה אחת
                                    if (duration >= 1000) {
                                        triggerSystemUIWilon();
                                    }
                                    startTime = 0;
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        // ניקוי וסגירה לפני ניסיון הפעלה מחדש במקרה של ניתוק
                        try {
                            if (os != null) os.close();
                            if (reader != null) reader.close();
                            if (suProcess != null) suProcess.destroy();
                        } catch (Exception e) {}
                    }

                    // השהייה קצרה למניעת לולאה אינסופית מהירה במקרה של כשל ברוט
                    try { Thread.sleep(2000); } catch (InterruptedException e) { break; }
                }
            }
        });
        monitorThread.start();
    }

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

    @Override
    public void onDestroy() {
        isRunning = false;
        if (suProcess != null) {
            suProcess.destroy();
        }
        if (monitorThread != null) {
            monitorThread.interrupt();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
