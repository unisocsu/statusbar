package com.neo.keymonitor;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import java.io.DataInputStream;
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
        // יצירת נוטיפיקציה ריקה לחלוטין - עוקף את תצוגת הווילון באנדרואיד 4.4
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
                        
                        os.write("cat /dev/input/event0\n".getBytes());
                        os.flush();

                        dis = new DataInputStream(suProcess.getInputStream());
                        byte[] buffer = new byte[16];

                        long startTime = 0;

                        while (isRunning) {
                            dis.readFully(buffer);

                            int type = ((buffer[9] & 0xFF) << 8) | (buffer[8] & 0xFF);
                            int code = ((buffer[11] & 0xFF) << 8) | (buffer[10] & 0xFF);
                            int value = ((buffer[15] & 0xFF) << 24) | ((buffer[14] & 0xFF) << 16) 
                                      | ((buffer[13] & 0xFF) << 8) | (buffer[12] & 0xFF);

                            if (type == 1 && code == 139) {
                                if (value == 1) {
                                    startTime = System.currentTimeMillis();
                                } else if (value == 0) {
                                    if (startTime != 0) {
                                        long duration = System.currentTimeMillis() - startTime;
                                        if (duration >= 1000) {
                                            triggerSystemUIWilon();
                                        }
                                        startTime = 0;
                                    }
                                }
                            }
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
        if (suProcess != null) suProcess.destroy();
        if (monitorThread != null) monitorThread.interrupt();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
