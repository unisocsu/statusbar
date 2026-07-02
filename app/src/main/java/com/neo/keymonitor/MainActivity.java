package com.neo.keymonitor;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // יצירת ממשק בסיסי ישירות מהקוד לטובת פשטות ותאימות
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(20, 20, 20, 20);

        TextView title = new TextView(this);
        title.setText("מנטר מקשים - אנדרואיד 4.4");
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        Button startButton = new Button(this);
        startButton.setText("הפעל שירות מנטר ברקע");
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent serviceIntent = new Intent(MainActivity.this, ButtonMonitorService.class);
                startService(serviceIntent);
                finish(); // סוגר את המסך ומשאיר רק את השירות פעיל ברקע
            }
        });
        
        layout.addView(startButton);
        setContentView(layout);
    }
}
