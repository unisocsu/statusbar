package com.neo.keymonitor;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.DataOutputStream;

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
                .setContentText("מאזין ללחצן תפריט בגרסה יציבה")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();
                
        startForeground(1, notification);
    }

    private void startMonitoringLoop() {
        monitorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                BufferedReader reader = null;
                DataOutputStream os = null;

                while (isRunning) {
                    try {
                        // פתיחת תהליך רוט ממוקד ישירות על החומרה שלך (event0)
                        suProcess = Runtime.getRuntime().exec("su");
                        os = new DataOutputStream(suProcess.getOutputStream());
                        
                        // שימוש בדגל -l כדי לקבל טקסט קריא (כמו שבדקת ב-ADB ועבד!)
                        os.writeBytes("getevent -l /dev/input/event0\n");
                        os.flush();

                        reader = new BufferedReader(new InputStreamReader(suProcess.getInputStream()));
                        String line;
                        long startTime = 0;

                        // קריאה רציפה של הסטרים בזמן אמת בלולאה חסכונית (חוסך סוללה!)
                        while (isRunning && (line = reader.readLine()) != null) {
                            
                            // 1. זיהוי תחילת לחיצה
                            if (line.contains("KEY_MENU") && line.contains("DOWN")) {
                                startTime = System.currentTimeMillis();
                            } 
                            
                            // 2. זיהוי עזיבת המקש וחישוב הזמן
                            else if (line.contains("KEY_MENU") && line.contains("UP")) {
                                if (startTime != 0) {
                                    long duration = System.currentTimeMillis() - startTime;
                                    
                                    // אם הלחיצה נמשכה שנייה אחת או יותר
                                    if (duration >= 1000) {
                                        triggerSystemUIWilon();
                                    }
                                    startTime = 0; // איפוס לשלב הבא
                                }
                            }
                        }

                    } catch (Exception e) {
                        // הגנה מקריסות: אם הערוץ נסגר מסיבה כלשהי, נמתין שנייה ונפתח מחדש
                        try { Thread.sleep(1000); } catch (InterruptedException ex) {}
                    } finally {
                        // סגירת משאבים מסודרת במקרה של עצירה
                        try {
                            if (os != null) { os.writeBytes("exit\n"); os.flush(); }
                            if (suProcess != null) { suProcess.destroy(); }
                            if (reader != null) { reader.close(); }
                        } catch (Exception e) {}
                    }
                }
            }
        });
        monitorThread.start();
    }

    private void triggerSystemUIWilon() {
        try {
            Process actionProcess = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(actionProcess.getOutputStream());
            os.writeBytes("service call statusbar 1\n");
            os.flush();
            os.writeBytes("exit\n");
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
