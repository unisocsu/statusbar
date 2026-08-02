package com.neo.keymonitor;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public class InputEventReader implements Runnable {

    public interface OnKeyEventListener {
        void onKeyEvent(int code, int value);
    }

    private final String devicePath;
    private final OnKeyEventListener listener;
    private volatile boolean isRunning = true;
    private Process eventProcess;

    public InputEventReader(String devicePath, OnKeyEventListener listener) {
        this.devicePath = devicePath;
        this.listener = listener;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[16]; // הקצאה יחידה של זיכרון למניעת עומס 🧹

        while (isRunning) {
            DataInputStream dis = null;
            try {
                eventProcess = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(eventProcess.getOutputStream());
                os.writeBytes("cat " + devicePath + "\n");
                os.flush();

                dis = new DataInputStream(eventProcess.getInputStream());

                while (isRunning) {
                    dis.readFully(buffer);

                    int type = ((buffer[9] & 0xFF) << 8) | (buffer[8] & 0xFF);
                    
                    // סינון מהיר: רק אירועי מקשים (EV_KEY = 1) ⚡
                    if (type != 1) continue;

                    int code = ((buffer[11] & 0xFF) << 8) | (buffer[10] & 0xFF);
                    int value = ((buffer[15] & 0xFF) << 24) | ((buffer[14] & 0xFF) << 16) 
                              | ((buffer[13] & 0xFF) << 8) | (buffer[12] & 0xFF);

                    if (listener != null) {
                        listener.onKeyEvent(code, value);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (dis != null) dis.close();
                    if (eventProcess != null) eventProcess.destroy();
                } catch (Exception e) {}
            }

            if (isRunning) {
                try { Thread.sleep(1000); } catch (InterruptedException e) { break; }
            }
        }
    }

    public void stop() {
        isRunning = false;
        if (eventProcess != null) {
            eventProcess.destroy();
        }
    }
}
