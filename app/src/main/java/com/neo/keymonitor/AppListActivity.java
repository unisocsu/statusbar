package com.neo.keymonitor;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class AppListActivity extends Activity {

    private ListView listView;
    private SharedPreferences prefs;

    public static class AppInfoModel {
        String appName;
        String packageName;
        Drawable icon;
        int statusSetting; // 0 = אפשר, 1 = חסום, 2 = שאל
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_list);

        prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        listView = findViewById(R.id.app_list_view);

        loadInstalledApps();
    }

    private void loadInstalledApps() {
        new AsyncTask<Void, Void, List<AppInfoModel>>() {
            @Override
            protected List<AppInfoModel> doInBackground(Void... voids) {
                PackageManager pm = getPackageManager();
                List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
                List<AppInfoModel> appList = new ArrayList<>();

                for (ApplicationInfo app : apps) {
                    // סינון אפליקציות משתמש בלבד 📦
                    if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                        AppInfoModel model = new AppInfoModel();
                        model.appName = app.loadLabel(pm).toString();
                        model.packageName = app.packageName;
                        model.icon = app.loadIcon(pm);
                        model.statusSetting = prefs.getInt("statusbar_" + app.packageName, 0);
                        appList.add(model);
                    }
                }
                return appList;
            }

            @Override
            protected void onPostExecute(List<AppInfoModel> result) {
                AppAdapter adapter = new AppAdapter(AppListActivity.this, result, prefs);
                listView.setAdapter(adapter);
                listView.requestFocus(); // פוקוס אוטומטי למקשים 🎯
            }
        }.execute();
    }

    private static class AppAdapter extends BaseAdapter {
        private final Context context;
        private final List<AppInfoModel> list;
        private final SharedPreferences prefs;

        public AppAdapter(Context context, List<AppInfoModel> list, SharedPreferences prefs) {
            this.context = context;
            this.list = list;
            this.prefs = prefs;
        }

        @Override
        public int getCount() { return list.size(); }

        @Override
        public Object getItem(int position) { return list.get(position); }

        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_app_setting, parent, false);
            }

            final AppInfoModel item = list.get(position);

            ImageView imgIcon = convertView.findViewById(R.id.app_icon);
            TextView txtName = convertView.findViewById(R.id.app_name);
            RadioGroup group = convertView.findViewById(R.id.status_bar_group);
            RadioButton radioAllow = convertView.findViewById(R.id.radio_allow);
            RadioButton radioBlock = convertView.findViewById(R.id.radio_block);
            RadioButton radioAsk = convertView.findViewById(R.id.radio_ask);

            imgIcon.setImageDrawable(item.icon);
            txtName.setText(item.appName);

            // הגדרת מצב ה-RadioButtons
            group.setOnCheckedChangeListener(null);
            if (item.statusSetting == 1) {
                radioBlock.setChecked(true);
            } else if (item.statusSetting == 2) {
                radioAsk.setChecked(true);
            } else {
                radioAllow.setChecked(true);
            }

            group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup g, int checkedId) {
                    int val = 0;
                    if (checkedId == R.id.radio_block) val = 1;
                    else if (checkedId == R.id.radio_ask) val = 2;

                    item.statusSetting = val;
                    prefs.edit().putInt("statusbar_" + item.packageName, val).apply();
                }
            });

            return convertView;
        }
    }
}
