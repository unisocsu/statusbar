package com.neo.keymonitor;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class ButtonMonitorService extends Service {

    private InputEventReader eventReader;
    private Thread readerThread;

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundServiceKitKat();

        // יצירת הרכיבים והפרדת רשויות מלאה 🚀
        KeyActionHandler actionHandler = new KeyActionHandler(this);
        eventReader = new InputEventReader("/dev/input/event0", actionHandler);

        readerThread = new Thread(eventReader);
        readerThread.start();
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
        if (eventReader != null) {
            eventReader.stop();
        }
        if (readerThread != null) {
            readerThread.interrupt();
        }
        ShellExecutor.getInstance().close();
        super.onDestroy();
    }
}
