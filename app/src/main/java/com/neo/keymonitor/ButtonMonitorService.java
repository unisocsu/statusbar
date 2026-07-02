package com.neo.keymonitor;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
                                Thread.sleep(2000); // הגנה מהקפצות כפולות
                                continue;
                            }
                            Thread.sleep(250); // קצב מהיר ברגע שמזהים לחיצה
                        } else {
                            consecutiveHits = 0;
                            Thread.sleep(500); // קצב שגרה חסכוני
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
     * בדיקה חסכונית ויציבה של מצב הלחצן
     */
    private boolean checkCurrentKeyStatus() {
        // דרך א': ניסיון לקרוא ישירות מסטטוס המקלדת בלי לפתוח תהליך רוט יקר בכל פעם
        try {
            // נתיב נפוץ במעבדי Unisoc/Spreadtrum עבור כפתורים פיזיים
            File gpioKeyFile = new File("/sys/devices/platform/soc/soc:gpio_keys/keys_status");
            if (gpioKeyFile.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(gpioKeyFile));
                String status = br.readLine();
                br.close();
                // אם הקובץ מכיל אינדיקציה שהמקש לחוץ (למשל "1" או שם המקש)
                if (status != null && (status.contains("1") || status.toLowerCase().contains("menu"))) {
                    return true;
                }
            }
        } catch (Exception e) {
            // ממשיך לדרך ב' אם הקובץ הספציפי לא קיים בגרסת הקרנל הזו
        }

        // דרך ב': דגימה מהירה באמצעות פקודת מעטפת קלילה
        try {
            // במקום getevent -p, נשתמש ב-dumpsys input כדי לראות אם המקש לחוץ כרגע במערכת
            Process process = Runtime.getRuntime().exec("sh"); // "sh" רגיל מספיק לקריאת dumpsys ברוב המכשירים
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            
            os.writeBytes("dumpsys input | grep -A 10 \"Key Modifier\"\n");
            os.flush();
            os.writeBytes("exit\n");
            os.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            boolean downDetected = false;
            
            while ((line = reader.readLine()) != null) {
                // מחפשים עדות למקש תפריט לחוץ בסטייט הנוכחי של מנהל ה-Input
                if (line.contains("KEY_MENU") && (line.contains("DOWN") || line.contains("PRESSED"))) {
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
            
            // שליחת פקודת הפתיחה בצורה המאובטחת שבדקת ב-ADB
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
