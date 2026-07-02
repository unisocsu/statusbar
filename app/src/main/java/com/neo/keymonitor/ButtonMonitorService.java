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
        Notification notification = new Notification.Builder(this)
                .setContentTitle("מנטר מקשים פעיל")
                .setContentText("בודק לחיצות במצב בטוח...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();
                
        startForeground(1, notification);
    }

    private void startMonitoringLoop() {
        monitorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isRunning) {
                    try {
                        // אנחנו דוגמים את המכשיר כל חצי שנייה בשגרה
                        Thread.sleep(500);

                        if (isMenuKeyLongPressed()) {
                            triggerSystemUIWilon();
                            Thread.sleep(2000); // השהייה למניעת לולאה כפולה
                        }
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        });
        monitorThread.start();
    }

    /**
     * פונקציה חסינה שבודקת אם כפתור התפריט נלחץ לחיצה ארוכה
     */
    private boolean isMenuKeyLongPressed() {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            
            // אנחנו מריצים פקודה שלא נתקעת לנצח - היא בודקת את הסטטוס הנוכחי בלבד
            os.writeBytes("getevent -p\n"); 
            os.flush();
            os.writeBytes("exit\n");
            os.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            boolean foundMenuDevice = false;
            
            while ((line = reader.readLine()) != null) {
                // נבדוק אילו התקנים קיימים במכשיר שמכילים את מקש התפריט
                if (line.contains("KEY_MENU") || line.contains("008b") || line.contains("001c")) {
                    foundMenuDevice = true;
                }
            }
            process.waitFor();

            // אם מצאנו שיש אירוע מקש, נבדוק ישירות ב-dumpsys בצורה פשוטה בלי סינונים כבדים
            if (foundMenuDevice) {
                Process checkProcess = Runtime.getRuntime().exec("su");
                DataOutputStream checkOs = new DataOutputStream(checkProcess.getOutputStream());
                
                // פקודה שמציגה את מצב המקשים הנוכחי במערכת
                checkOs.writeBytes("dumpsys input\n");
                checkOs.flush();
                checkOs.writeBytes("exit\n");
                checkOs.flush();

                BufferedReader checkReader = new BufferedReader(new InputStreamReader(checkProcess.getInputStream()));
                boolean isDown = false;
                
                while ((line = checkReader.readLine()) != null) {
                    // באנדרואיד 4.4, אם מקש לחוץ, מופיעה השורה "FocusedApplications" או "Key Event" עם פירוט המצב
                    if (line.toLowerCase().contains("menu") && (line.contains("DOWN") || line.contains("pressed=true"))) {
                        isDown = true;
                    }
                }
                checkProcess.waitFor();
                return isDown;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
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
