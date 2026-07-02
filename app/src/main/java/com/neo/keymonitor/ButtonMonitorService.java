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

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundServiceKitKat();
        startMonitoringLoop();
    }

    @SuppressWarnings("deprecation")
    private void startForegroundServiceKitKat() {
        // בניית נוטיפיקציה בשיטה הישנה התואמת לאנדרואיד 4.4 ומטה
        Notification notification = new Notification.Builder(this)
                .setContentTitle("מנטר מקשים פעיל")
                .setContentText("סורק לחצן תפריט באנדרואיד 4.4")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();
                
        startForeground(1, notification);
    }

    private void startMonitoringLoop() {
        monitorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int consecutiveHits = 0;

                while (isRunning) {
                    try {
                        boolean isKeyDown = checkCurrentKeyStatus();

                        if (isKeyDown) {
                            consecutiveHits++;
                            if (consecutiveHits >= 4) {
                                triggerSystemUIWilon();
                                consecutiveHits = 0;
                                Thread.sleep(2000); 
                                continue;
                            }
                            Thread.sleep(250); 
                        } else {
                            consecutiveHits = 0;
                            Thread.sleep(500); 
                        }
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        });
        monitorThread.start();
    }

    private boolean checkCurrentKeyStatus() {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            
            os.writeBytes("getevent -p /dev/input/event0\n");
            os.flush();
            os.writeBytes("exit\n");
            os.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            boolean downDetected = false;
            
            while ((line = reader.readLine()) != null) {
                if (line.contains("KEY_MENU") && (line.contains("DOWN") || line.contains("1"))) {
                    downDetected = true;
                }
            }
            process.waitFor();
            return downDetected;
        } catch (Exception e) {
            return false;
        }
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
