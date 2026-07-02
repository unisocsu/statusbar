package com.neo.keymonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // בודק שהאירוע שהתקבל הוא אכן סיום עליית מערכת ההפעלה
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // מפעיל אוטומטית את שירות ניטור המקשים שכתבנו
            Intent serviceIntent = new Intent(context, ButtonMonitorService.class);
            context.startService(serviceIntent);
        }
    }
}
