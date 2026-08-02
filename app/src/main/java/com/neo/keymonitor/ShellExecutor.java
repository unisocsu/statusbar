package com.neo.keymonitor;

import java.io.DataOutputStream;
import java.io.IOException;

public class ShellExecutor {
    private static ShellExecutor instance;
    private Process suProcess;
    private DataOutputStream os;

    private ShellExecutor() {
        initShell();
    }

    public static synchronized ShellExecutor getInstance() {
        if (instance == null) {
            instance = new ShellExecutor();
        }
        return instance;
    }

    private void initShell() {
        try {
            suProcess = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(suProcess.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void execute(String command) {
        try {
            if (os == null) {
                initShell();
            }
            if (os != null) {
                os.writeBytes(command + "\n");
                os.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
            initShell(); // ניסיון התחברות מחדש במקרה של ניתוק 🔄
        }
    }

    public void close() {
        try {
            if (os != null) {
                os.writeBytes("exit\n");
                os.flush();
                os.close();
            }
            if (suProcess != null) {
                suProcess.destroy();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
