package com.neo.keymonitor;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.DataOutputStream;

public class ButtonMonitorService extends Service {
    private Thread monitorThread;
    private boolean isRunning = true;

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
                // נתיב זמני בטוח בתוך הזיכרון של המכשיר
                String logPath = getFilesDir().getAbsolutePath() + "/ev.log";
                File logFile = new File(logPath);

                // ניקוי שאריות אם קיימות
                if (logFile.exists()) { logFile.delete(); }

                try {
                    // הפעלת getevent ברקע שכותב ישירות לקובץ הלוג
                    Process p = Runtime.getRuntime().exec("su");
                    DataOutputStream os = new DataOutputStream(p.getOutputStream());
                    os.writeBytes("getevent -l /dev/input/event0 > " + logPath + " &\n");
                    os.flush();
                    os.writeBytes("exit\n");
                    os.flush();
                    p.waitFor();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                long startTime = 0;

                while (isRunning) {
                    try {
                        // דגימה קלה כל 200 מילישניות - לא מעמיס על המעבד
                        Thread.sleep(2000); 

                        if (!logFile.exists() || logFile.length() == 0) {
                            continue;
                        }

                        // קריאת המצב מתוך קובץ הלוג הזמני
                        BufferedReader br = new BufferedReader(new FileReader(logFile));
                        String line;
                        
                        while ((line = br.readLine()) != null) {
                            if (line.contains("KEY_MENU")) {
                                if (line.contains("DOWN")) {
                                    startTime = System.currentTimeMillis();
                                } else if (line.contains("UP")) {
                                    if (startTime != 0) {
                                        long duration = System.currentTimeMillis() - startTime;
                                        // לחיצה ארוכה של מעל שנייה
                                        if (duration >= 1000) {
                                            triggerSystemUIWilon();
                                        }
                                        startTime = 0;
                                    }
                                }
                            }
                        }
                        br.close();

                        // נקה את הקובץ מדי פעם שלא יגדל יותר מדי בזיכרון
                        if (logFile.length() > 50000) {
                            Runtime.getRuntime().exec("su -c '> " + logPath + "'").waitFor();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // עצירה מסודרת וניקוי
                try {
                    Runtime.getRuntime().exec("su -c 'pkill getevent'").waitFor();
                    if (logFile.exists()) { logFile.delete(); }
                } catch (Exception e) {}
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
