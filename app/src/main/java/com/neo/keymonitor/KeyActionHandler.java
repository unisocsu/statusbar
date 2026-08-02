package com.neo.keymonitor;

import android.content.Context;
import android.util.Log;

public class KeyActionHandler implements InputEventReader.OnKeyEventListener {

    private static final String TAG = "KeyActionHandler";
    private final Context context;

    public KeyActionHandler(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void onKeyEvent(int code, int value) {
        Log.e(TAG, "Key received -> Code: " + code + ", Value: " + value);

        switch (code) {
            case 139: // מקש Menu (קוד 139) 📜
                MenuKeyHandler.handle(context, value);
                break;
            case 228: // מקש הסולם # (קוד 228) 📱
                PoundKeyHandler.handle(context, value);
                break;
            case 6:   // מקש 5 (קוד 6) 🖱️
                FiveKeyHandler.handle(context, value);
                break;
        }
    }
}
