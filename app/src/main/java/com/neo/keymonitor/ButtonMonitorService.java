package com.neo.keymonitor;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import java.io.InputStream;
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
                .setContentText("מאזין ללחצן פיזי...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();
                
        startForeground(1, notification);
    }

    private void startMonitoringLoop() {
        monitorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Process process = null;
                DataOutputStream os = null;
                InputStream is = null;

                while (isRunning) {
                    try {
                        // פותחים חיבור רוט אחד קבוע שיקשיב ללחיצות (חוסך סוללה ב-100%)
                        process = Runtime.getRuntime().exec("su");
                        os = new DataOutputStream(process.getOutputStream());
                        is = process.getInputStream();

                        // פקודת getevent רגילה שמציגה שינויים בזמן אמת ב-event0
                        os.writeBytes("getevent -q /dev/input/event0\n");
                        os.flush();

                        byte[] buffer = new byte[16]; // באנדרואיד ישן, כל אירוע getevent הוא באפר של 16 או 24 בתים
                        int bytesRead;
                        long lastDownTime = 0;

                        while (isRunning && (bytesRead = is.read(buffer)) != -1) {
                            // ברגע שמתקבל אירוע כלשהו מ-event0 (כלומר לחצת על הלחצן פיזית!)
                            // אנחנו בודקים אם עברה שנייה מהרגע שהתחלת ללחוץ
                            if (lastDownTime == 0) {
                                lastDownTime = System.currentTimeMillis();
                            } else {
                                long duration = System.currentTimeMillis() - lastDownTime;
                                // אם הלחיצה נמשכת מעל 1000 מילישניות (שנייה אחת)
                                if (duration >= 1000) {
                                    triggerSystemUIWilon();
                                    lastDownTime = 0; // איפוס
                                    Thread.sleep(2000); // הגנה מהקפצות
                                }
                            }
                        }

                    } catch (Exception e) {
                        // אם הערוץ נסגר, ננסה לפתוח אותו מחדש בלולאה הבאה
                        try { Thread.sleep(1000); } catch (InterruptedException ex) {}
                    } finally {
                        // ניקוי משאבים בטוח
                        try {
                            if (os != null) { os.writeBytes("exit\n"); os.flush(); }
                            if (process != null) { process.destroy(); }
                        } catch (Exception e) {}
                    }
                }
            }
        });
        monitorThread.start();
    }

    private void triggerSystemUIWilon() {
        try {
            Process suProcess = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());
            os.writeBytes("service call statusbar 1\n");
            os.flush();
            os.writeBytes("exit\n");
            os.flush();
            suProcess.waitFor();
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
