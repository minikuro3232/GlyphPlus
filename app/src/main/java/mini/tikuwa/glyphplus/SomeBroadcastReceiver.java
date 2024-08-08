package mini.tikuwa.glyphplus;

import static mini.tikuwa.glyphplus.BackgroundService.mGM;
import static mini.tikuwa.glyphplus.BackgroundService.lastfunction;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class SomeBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // サービスを開始
            Intent serviceIntent = new Intent(context, BackgroundService.class);
            context.startService(serviceIntent);
        } else if (Intent.ACTION_POWER_CONNECTED.equals(intent.getAction())) {
            Log.d("ChargingReceiver", "充電が開始されました");
            BackgroundService.isPowerCharge = true;

            if (lastfunction != 3) {
                BackgroundService.lastScaledPosition = 0;
            }
        } else if (Intent.ACTION_POWER_DISCONNECTED.equals(intent.getAction())) {
            Log.d("ChargingReceiver", "充電が終了しました");
            if (lastfunction == 3) {
                mGM.turnOff();
            } else {
                BackgroundService.lastScaledPosition = 0;
            }
            BackgroundService.isPowerCharge = false;
        }
        Log.d("AllBroadcastReceiver", "Action: " + intent.getAction());
    }
}

