package com.neo.keymonitor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

public class MainActivity extends Activity {

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE);

        // הפעלת שירות הניטור במידה ועדיין לא רץ ⚡
        Intent serviceIntent = new Intent(this, ButtonMonitorService.class);
        startService(serviceIntent);

        initViews();
    }

    private void initViews() {
        CheckBox chkWilon = findViewById(R.id.chk_wilon);
        CheckBox chkRecents = findViewById(R.id.chk_recents);
        CheckBox chkMouse = findViewById(R.id.chk_mouse);
        Button btnOpenAppList = findViewById(R.id.btn_open_app_list);

        // טעינת מצב שמור למתגים
        chkWilon.setChecked(prefs.getBoolean("enable_wilon", true));
        chkRecents.setChecked(prefs.getBoolean("enable_recents", true));
        chkMouse.setChecked(prefs.getBoolean("enable_mouse", true));

        // מאזינים לשינויים במתגים
        chkWilon.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean("enable_wilon", isChecked).apply();
            }
        });

        chkRecents.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean("enable_recents", isChecked).apply();
            }
        });

        chkMouse.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean("enable_mouse", isChecked).apply();
            }
        });

        // בלחיצה על הכפתור (או מקש אישור) - פתיחת מסך ניהול האפליקציות 🎯
        btnOpenAppList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AppListActivity.class);
                startActivity(intent);
            }
        });
    }
}
