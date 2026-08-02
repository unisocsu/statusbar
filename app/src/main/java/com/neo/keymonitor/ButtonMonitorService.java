package com.neo.keymonitor;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class ButtonMonitorService extends Service {

    private InputEventReader eventReader0;
    private InputEventReader eventReader1;
    private Thread readerThread0;
    private Thread readerThread1;

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundServiceKitKat();

        KeyActionHandler actionHandler = new KeyActionHandler(this);

        // 1. האזנה ל-event0 (sprd-keypad: מקש MENU ואירועי מקלדת נוספים) 📜
        eventReader0 = new InputEventReader("/dev/input/event0", actionHandler);
        readerThread0 = new Thread(eventReader0);
        readerThread0.start();

        // 2. האזנה ל-event1 (sprd-gpio-keys: מקש 5, מקשי GPIO ומספרים) 🖱️⚡
        eventReader1 = new InputEventReader("/dev/input/event1", actionHandler);
        readerThread1 = new Thread(eventReader1);
        readerThread1.start();
    }

    @SuppressWarnings("deprecation")
    private void startForegroundServiceKitKat() {
        Notification notification = new Notification();
        startForeground(1, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        // עצירת שני המאזינים והטרדים באופן נקי 🧹
        if (eventReader0 != null) eventReader0.stop();
        if (eventReader1 != null) eventReader1.stop();
        
        if (readerThread0 != null) readerThread0.interrupt();
        if (readerThread1 != null) readerThread1.interrupt();

        ShellExecutor.getInstance().close();
        super.onDestroy();
    }
}
