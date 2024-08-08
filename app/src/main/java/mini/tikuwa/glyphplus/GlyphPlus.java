package mini.tikuwa.glyphplus;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.nothing.ketchum.Common;
import com.nothing.ketchum.GlyphException;
import com.nothing.ketchum.GlyphFrame;
import com.nothing.ketchum.GlyphManager;

import java.util.List;
import java.util.Locale;

public class GlyphPlus extends AppCompatActivity {
    private MediaSessionManager mediaSessionManager;
    private static final int REQUEST_NOTIFICATION_ACCESS = 123;
    private static final int REQUEST_ACCESSIBILITY_ACCESS = 124;
    public static Context context;
    public static GlyphPlus glyphPlus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_start);

        Log.v("GlyphPlus", "started");

        // MediaSessionManagerのインスタンスを取得
        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);

        context = this;
        glyphPlus = this;

        if (!isServiceRunning(this, BackgroundService.class)) {
            Intent serviceIntent = new Intent(this, BackgroundService.class);
            startService(serviceIntent);
        }

        retrieveInfo();
    }

    // 通知アクセスの許可が必要かどうかをチェックするメソッド
    public static boolean isNotificationAccessGranted() {
        ComponentName cn = new ComponentName(context, BackgroundService.class);
        String flat = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");

        return flat != null && flat.contains(cn.flattenToString());
    }

    public static boolean isNotificationAccessGranteds() {
        ComponentName cn = new ComponentName(context, BackgroundService.class);
        String flat = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
        if (flat != null && flat.contains(cn.flattenToString())) {
            ComponentName cn2 = new ComponentName(context, MediaSessionNotificationListenerService.class);
            String flat2 = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
            return flat2 != null && flat2.contains(cn2.flattenToString());
        }

        return false;
    }

    public static boolean isAccessibilityServiceEnabled() {
        ComponentName cn = new ComponentName(context, TimerAccessibilityService.class);
        String flat = Settings.Secure.getString(context.getContentResolver(), "enabled_accessibility_services");

        return flat != null && flat.contains(cn.flattenToString());
    }

    // 通知アクセスの設定画面を開くためのメソッド
    private static void requestNotificationAccess() {
        Toast.makeText(context, R.string.permissions_notification_access_request, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        glyphPlus.startActivityForResult(intent, REQUEST_NOTIFICATION_ACCESS);
    }
    private static void requestAccessibilityAccess() {
        Toast.makeText(context, R.string.permissions_accessibility_request, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        glyphPlus.startActivityForResult(intent, REQUEST_ACCESSIBILITY_ACCESS);
    }

    // 設定画面から戻ってきた時の処理
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_NOTIFICATION_ACCESS) {
            if (isNotificationAccessGranted()) {
                Toast.makeText(this, R.string.permissions_notification_access_true, Toast.LENGTH_SHORT).show();
                savedata("PlayBackTime", true);
                retrieveInfo();
            } else {
                Toast.makeText(this, R.string.permissions_notification_access_false, Toast.LENGTH_SHORT).show();
                retrieveInfo();
            }
        } if (requestCode == REQUEST_ACCESSIBILITY_ACCESS) {
            if (isAccessibilityServiceEnabled()) {
                Toast.makeText(this, R.string.permissions_accessibility_true, Toast.LENGTH_SHORT).show();
                savedata("Timer", true);
                retrieveInfo();
            } else {
                Toast.makeText(this, R.string.permissions_accessibility_false, Toast.LENGTH_SHORT).show();
                retrieveInfo();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // 楽曲情報を取得するメソッド
    public static void retrieveInfo() {
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            // If not on the main thread, do nothing
            return;
        }

        if (!isNotificationAccessGranted()) {
            savedata("PlayBackTime", false);
        }

        if (!isAccessibilityServiceEnabled()) {
            savedata("Timer", false);
        }

        Switch glyphPlusEnableSwitch = glyphPlus.findViewById(R.id.gliyh_plus_enable_toggle_switch);
        Switch batteryCapacitySwitch = glyphPlus.findViewById(R.id.battery_capacity_toggle_switch);
        Switch playbackTimeSwitch = glyphPlus.findViewById(R.id.playback_time_toggle_switch);
        Switch timerSwitch = glyphPlus.findViewById(R.id.timer_toggle_switch);

        glyphPlusEnableSwitch.setChecked(BackgroundService.isRunning());
        batteryCapacitySwitch.setChecked(loaddata("Battery", false));
        playbackTimeSwitch.setChecked(loaddata("PlayBackTime", false));
        timerSwitch.setChecked(loaddata("Timer", false));

        glyphPlusEnableSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    BackgroundService.toggleTile(true);
                    BackgroundService.start();
                } else {
                    BackgroundService.toggleTile(false);
                    BackgroundService.stop();
                }
            }
        });

        batteryCapacitySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                savedata("Battery", isChecked);
            }
        });

        playbackTimeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isNotificationAccessGranted()) {
                    savedata("PlayBackTime", isChecked);
                } else {
                    if (isChecked == true) {
                        requestNotificationAccess();
                    }
                }
            }
        });

        timerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isAccessibilityServiceEnabled()) {
                    savedata("Timer", isChecked);
                } else {
                    if (isChecked == true) {
                        requestAccessibilityAccess();
                    }
                }
            }
        });
    }

    public static boolean loaddata(String key,boolean defValue) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("GlyphPlus",Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(key, defValue);
    }
    public static void savedata(String key,boolean Value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("GlyphPlus",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, Value);
        editor.apply();
    }

    // サービスが動いてるか確認
    public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

}
