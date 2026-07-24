package com.neo.keymonitor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import java.util.List;

public class MainActivity extends Activity {

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // או בניה דינמית בקוד

        prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE);

        // הפעלת שירות הרקע הראשי
        Intent serviceIntent = new Intent(this, ButtonMonitorService.class);
        startService(serviceIntent);

        // הגדרת מתג תפריט יישומים אחרונים (מקש #)
        CheckBox chkRecents = (CheckBox) findViewById(R.id.chk_recents);
        chkRecents.setChecked(prefs.getBoolean("enable_recents", true));
        chkRecents.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean("enable_recents", isChecked).apply();
            }
        });

        // הגדרת מתג עכבר וירטואלי (מקש 5)
        CheckBox chkMouse = (CheckBox) findViewById(R.id.chk_mouse);
        chkMouse.setChecked(prefs.getBoolean("enable_mouse", true));
        chkMouse.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean("enable_mouse", isChecked).apply();
            }
        });

        // טעינת רשימת האפליקציות
        loadAppList();
    }

    private void loadAppList() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        ListView listView = (ListView) findViewById(R.id.app_list_view);
        AppAdapter adapter = new AppAdapter(this, apps);
        listView.setAdapter(adapter);
    }

    // מתאם (Adapter) להצגת רשימת האפליקציות והבחירה עבור כל אחת
    private class AppAdapter extends ArrayAdapter<ApplicationInfo> {
        public AppAdapter(Context context, List<ApplicationInfo> apps) {
            super(context, 0, apps);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_app_setting, parent, false);
            }

            final ApplicationInfo app = getItem(position);
            PackageManager pm = getPackageManager();

            TextView appName = (TextView) convertView.findViewById(R.id.app_name);
            ImageView appIcon = (ImageView) convertView.findViewById(R.id.app_icon);
            RadioGroup statusGroup = (RadioGroup) convertView.findViewById(R.id.status_bar_group);

            appName.setText(app.loadLabel(pm));
            appIcon.setImageDrawable(app.loadIcon(pm));

            // קריאת מצב שמור עבור האפליקציה: 0 = לאפשר הסתרה, 1 = לא לאפשר (חסום), 2 = לשאול
            int currentMode = prefs.getInt("statusbar_" + app.packageName, 1); // ברירת מחדל: חסום

            if (currentMode == 0) statusGroup.check(R.id.radio_allow);
            else if (currentMode == 1) statusGroup.check(R.id.radio_block);
            else if (currentMode == 2) statusGroup.check(R.id.radio_ask);

            statusGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    int mode = 1;
                    if (checkedId == R.id.radio_allow) mode = 0;
                    else if (checkedId == R.id.radio_block) mode = 1;
                    else if (checkedId == R.id.radio_ask) mode = 2;

                    prefs.edit().putInt("statusbar_" + app.packageName, mode).apply();
                }
            });

            return convertView;
        }
    }
}
