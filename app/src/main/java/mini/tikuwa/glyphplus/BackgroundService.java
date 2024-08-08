
package mini.tikuwa.glyphplus;

import static mini.tikuwa.glyphplus.GlyphPlus.isNotificationAccessGranted;
import static mini.tikuwa.glyphplus.GlyphPlus.loaddata;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.Nullable;

import com.nothing.ketchum.Common;
import com.nothing.ketchum.Glyph;
import com.nothing.ketchum.GlyphException;
import com.nothing.ketchum.GlyphFrame;
import com.nothing.ketchum.GlyphManager;

import java.util.List;

public class BackgroundService extends Service {
    public static boolean isPowerCharge = false;
    public static boolean isTimer = false;
    public static boolean isLight = false;
    public static double TimerGlyph = 0;
    private static final String CHANNEL_ID = "MusicForegroundServiceChannel-GlyphPlus";
    private static final int NOTIFICATION_ID = 123;
    private static final int PAUSE_TIMEOUT = 6;// ６count
    private static final int UPDATE_INTERVAL = 500; // 0.5秒

    private static Handler handler;
    private static Runnable updateRunnable;

    private MediaSessionManager mediaSessionManager;

    private long lastPlaybackPosition = -1;
    private int stopcount = 6;
    private boolean isPaused = true;
    public static int lastScaledPosition = 0;
    private boolean lastIsPower = false;
    public static int lastfunction = 0;

    public static GlyphManager mGM = null;
    private GlyphManager.Callback mCallback = null;
    private SomeBroadcastReceiver chargingReceiver;
    private static Context context;
    private int position;
    private int duration;

    @Override
    public void onCreate() {
        super.onCreate();
        running = true;

        context = this;
        if (GlyphPlus.context == null) {
            GlyphPlus.context = this;
        }

        toggleTile(true);

        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        handler = new Handler();

        BatteryManager batteryManager = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
        int status = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS);
        isPowerCharge = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        chargingReceiver = new SomeBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(chargingReceiver, filter);

        if (isNotificationAccessGranted()) {
            List<MediaController> controllers = mediaSessionManager.getActiveSessions(new ComponentName(this, MediaSessionNotificationListenerService.class));
            if (!controllers.isEmpty()) {
                MediaController controller = controllers.get(0);
                if (controller != null && controller.getMetadata() != null) {
                    long position = controller.getPlaybackState().getPosition();
                    lastPlaybackPosition = position;
                }
            }
        }

        mGM = GlyphManager.getInstance(context);
        mGM.init(new GlyphManager.Callback() {
            @Override
            public void onServiceConnected(ComponentName componentName) {
                if (Common.is20111()) mGM.register(Common.DEVICE_20111);
                if (Common.is22111()) mGM.register(Common.DEVICE_22111);
                if (Common.is23111()) mGM.register(Common.DEVICE_23111);
                try {
                    mGM.openSession();
                } catch (Exception e) {
                    Log.e("GlyphPlus", "Error opening Glyph session", e);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                try {
                    mGM.closeSession();
                } catch (GlyphException e) {
                    e.printStackTrace();
                }
            }
        });

        updateRunnable = new Runnable() {
            @Override
            public void run() {
                retrieveMusicInfo();
                displayGlyphInfo();
                handler.postDelayed(this, UPDATE_INTERVAL);
            }
        };

        handler.post(updateRunnable);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateRunnable);
        mGM.turnOff();

        if (chargingReceiver != null) {
            unregisterReceiver(chargingReceiver);
        }

        toggleTile(false);
        running = false;
    }

    private void retrieveMusicInfo() {
        if (isNotificationAccessGranted()) {
            List<MediaController> controllers = mediaSessionManager.getActiveSessions(new ComponentName(this, MediaSessionNotificationListenerService.class));
            if (!controllers.isEmpty()) {
                MediaController controller = controllers.get(0);
                if (controller != null && controller.getMetadata() != null) {
                    MediaMetadata metadata = controller.getMetadata();

                    duration = (int) metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
                    position = (int) controller.getPlaybackState().getPosition();

                    if (lastPlaybackPosition == position || !loaddata("PlayBackTime", false)) {
                        stopcount++;

                        // Log.i("BuckgroundService", stopcount + "/" + PAUSE_TIMEOUT);
                        if (stopcount >= PAUSE_TIMEOUT) {
                            isPaused = true;
                            stopcount = PAUSE_TIMEOUT - 1;
                        }

                        if (!loaddata("PlayBackTime", false)) {
                            isPaused = true;
                            stopcount = PAUSE_TIMEOUT - 1;
                        }
                    } else {
                        stopcount = 0;
                        isPaused = false;
                    }

                    lastPlaybackPosition = position;
                }
            }
        }
    }


    private void displayGlyphInfo() {
        if (isLight) {
            if (lastfunction != 5) {
                lastScaledPosition = 0;
            }
            if (Common.is23111()) {
                GlyphFrame.Builder builder = mGM.getGlyphFrameBuilder();
                if (builder == null) {
                    return;
                }

                builder.buildChannelA();
                builder.buildChannelB();
                builder.buildChannelC();

                GlyphFrame frame1 = builder.build();

                mGM.toggle(frame1);
            } else if (Common.is20111()) {
                GlyphFrame.Builder builder = mGM.getGlyphFrameBuilder();
                if (builder == null) {
                    return;
                }

                builder.buildChannelA();
                builder.buildChannelB();
                builder.buildChannelC();
                builder.buildChannelD();
                builder.buildChannelE();

                GlyphFrame frame1 = builder.build();

                mGM.toggle(frame1);
            } else if (Common.is22111()) {
                GlyphFrame.Builder builder = mGM.getGlyphFrameBuilder();
                if (builder == null) {
                    return;
                }

                builder.buildChannelA();
                builder.buildChannelB();
                builder.buildChannelC();
                builder.buildChannelD();
                builder.buildChannelE();

                GlyphFrame frame1 = builder.build();

                mGM.toggle(frame1);
            }

            lastfunction = 5;
        } else {
            if (isTimer && loaddata("Timer", false)) {
                if (lastfunction != 4) {
                    lastScaledPosition = 0;
                }
                if (Common.is23111()) {
                    GlyphFrame.Builder builder = mGM.getGlyphFrameBuilder();
                    if (builder == null) {
                        return;
                    }

                    int scaledPosition = (int) Math.ceil(TimerGlyph * 24);

                    if (scaledPosition > 24 || scaledPosition < 1 || scaledPosition == lastScaledPosition) {
                        return;
                    }

                    lastScaledPosition = scaledPosition;

                    for (int i = 0; i < scaledPosition; i++) {
                        builder.buildChannel(Glyph.Code_23111.C_1 + i);
                    }

                    if (isPowerCharge && loaddata("Battery", false)) {
                        builder.buildChannelB();
                    }

                    GlyphFrame frame1 = builder.build();

                    mGM.toggle(frame1);
                } else if (Common.is20111()) {
                    GlyphFrame.Builder builder = mGM.getGlyphFrameBuilder();
                    if (builder == null) {
                        return;
                    }

                    int scaledPosition = (int) Math.ceil(TimerGlyph * 7);

                    if (scaledPosition > 7 || scaledPosition < 1 || scaledPosition == lastScaledPosition) {
                        return;
                    }

                    lastScaledPosition = scaledPosition;

                    for (int i = 0; i < scaledPosition; i++) {
                        builder.buildChannel(Glyph.Code_20111.D1_1 + i);
                    }
                    if (isPowerCharge && loaddata("Battery", false)) {
                        builder.buildChannelB();
                    }

                    GlyphFrame frame1 = builder.build();

                    mGM.toggle(frame1);
                } else if (Common.is22111()) {
                    GlyphFrame.Builder builder = mGM.getGlyphFrameBuilder();
                    if (builder == null) {
                        return;
                    }

                    int scaledPosition = (int) Math.ceil(TimerGlyph * 15);

                    if (scaledPosition > 15 || scaledPosition < 1 || scaledPosition == lastScaledPosition) {
                        return;
                    }

                    lastScaledPosition = scaledPosition;

                    for (int i = 0; i < scaledPosition; i++) {
                        builder.buildChannel(Glyph.Code_22111.C1_1 + i);
                    }
                    if (isPowerCharge && loaddata("Battery", false)) {
                        builder.buildChannelB();
                    }

                    GlyphFrame frame1 = builder.build();

                    mGM.toggle(frame1);
                }

                lastfunction = 4;
            } else {
                if (isPaused) {
                    if (isPowerCharge && loaddata("Battery", false)) {
                        if (lastfunction != 3) {
                            lastScaledPosition = 0;
                        }
                        if (Common.is23111()) {
                            GlyphFrame.Builder builder = mGM.getGlyphFrameBuilder();
                            if (builder == null) {
                                return;
                            }

                            BatteryManager batteryManager = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
                            int batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

                            int scaledPosition = Math.round((batteryLevel / 100.0f) * 24);

                            if (scaledPosition > 24 || scaledPosition < 1 || scaledPosition == lastScaledPosition) {
                                return;
                            }

                            lastScaledPosition = scaledPosition;

                            for (int i = 0; i < scaledPosition; i++) {
                                builder.buildChannel(Glyph.Code_23111.C_1 + i).buildPeriod(UPDATE_INTERVAL * 2);
                            }

                            // フレームを構築し、色を設定
                            builder.buildChannelB().buildCycles(1)
                                    .buildPeriod(1000);

                            GlyphFrame frame1 = builder.build();

                            mGM.animate(frame1);
                        } else if (Common.is20111()) {
                            GlyphFrame.Builder builder = mGM.getGlyphFrameBuilder();
                            if (builder == null) {
                                return;
                            }

                            BatteryManager batteryManager = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
                            int batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

                            int scaledPosition = Math.round((batteryLevel / 100.0f) * 7);

                            if (scaledPosition > 7 || scaledPosition < 1 || scaledPosition == lastScaledPosition) {
                                return;
                            }

                            lastScaledPosition = scaledPosition;

                            for (int i = 0; i < scaledPosition; i++) {
                                builder.buildChannel(Glyph.Code_20111.D1_1 + i).buildPeriod(UPDATE_INTERVAL * 2);
                            }
                            builder.buildChannelB().buildCycles(1)
                                    .buildPeriod(1000);

                            GlyphFrame frame1 = builder.build();

                            mGM.toggle(frame1);
                        } else if (Common.is22111()) {
                            GlyphFrame.Builder builder = mGM.getGlyphFrameBuilder();
                            if (builder == null) {
                                return;
                            }

                            BatteryManager batteryManager = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
                            int batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

                            int scaledPosition = Math.round((batteryLevel / 100.0f) * 15);

                            if (scaledPosition > 15 || scaledPosition < 1 || scaledPosition == lastScaledPosition) {
                                return;
                            }

                            lastScaledPosition = scaledPosition;

                            for (int i = 0; i < scaledPosition; i++) {
                                builder.buildChannel(Glyph.Code_22111.C1_1 + i).buildPeriod(UPDATE_INTERVAL * 2);
                            }
                            builder.buildChannelB().buildCycles(1)
                                    .buildPeriod(1000);

                            GlyphFrame frame1 = builder.build();

                            mGM.toggle(frame1);
                        }

                        lastfunction = 3;
                    } else {
                        mGM.turnOff();
                        lastfunction = 1;
                    }
                } else if (loaddata("PlayBackTime", false)) {
                    if (lastfunction != 2) {
                        lastScaledPosition = 0;
                    }
                    if (Common.is23111()) {
                        GlyphFrame.Builder builder = mGM.getGlyphFrameBuilder();
                        if (builder == null) {
                            return;
                        }

                        double playbackRate = (double) position / duration;

                        int scaledPosition = (int) Math.ceil(playbackRate * 24);

                        if (lastIsPower == isPowerCharge) {
                            if (scaledPosition > 25 || scaledPosition < 1 || scaledPosition == lastScaledPosition) {
                                return;
                            }
                        }
                        lastIsPower = isPowerCharge;

                        lastScaledPosition = scaledPosition;

                        for (int i = 0; i < scaledPosition; i++) {
                            builder.buildChannel(Glyph.Code_23111.C_1 + i);
                        }

                        if (isPowerCharge  && loaddata("Battery", false)) {
                            builder.buildChannelB();
                        }

                        GlyphFrame frame1 = builder.build();

                        mGM.toggle(frame1);
                    } else if (Common.is20111()) {
                        GlyphFrame.Builder builder = mGM.getGlyphFrameBuilder();
                        if (builder == null) {
                            return;
                        }

                        double playbackRate = (double) position / duration;

                        int scaledPosition = (int) Math.ceil(playbackRate * 7);

                        if (lastIsPower == isPowerCharge) {
                            if (scaledPosition > 8 || scaledPosition < 1 || scaledPosition == lastScaledPosition) {
                                return;
                            }
                        }
                        lastIsPower = isPowerCharge;

                        lastScaledPosition = scaledPosition;

                        for (int i = 0; i < scaledPosition; i++) {
                            builder.buildChannel(Glyph.Code_20111.D1_1 + i);
                        }

                        if (isPowerCharge && loaddata("Battery", false)) {
                            builder.buildChannelB();
                        }

                        GlyphFrame frame1 = builder.buildPeriod(UPDATE_INTERVAL * 2).build();

                        mGM.toggle(frame1);
                    } else if (Common.is22111()) {
                        GlyphFrame.Builder builder = mGM.getGlyphFrameBuilder();
                        if (builder == null) {
                            return;
                        }

                        double playbackRate = (double) position / duration;

                        int scaledPosition = (int) Math.ceil(playbackRate * 15);


                        if (lastIsPower == isPowerCharge) {
                            if (scaledPosition > 16 || scaledPosition < 1 || scaledPosition == lastScaledPosition) {
                                return;
                            }
                        }
                        lastIsPower = isPowerCharge;

                        lastScaledPosition = scaledPosition;

                        for (int i = 0; i < scaledPosition; i++) {
                            builder.buildChannel(Glyph.Code_22111.C1_1 + i);
                        }

                        if (isPowerCharge && loaddata("Battery", false)) {
                            builder.buildChannelB();
                        }

                        GlyphFrame frame1 = builder.buildPeriod(UPDATE_INTERVAL * 2).build();

                        mGM.toggle(frame1);
                    }
                    lastfunction = 2;
                } else {
                    mGM.turnOff();
                    lastfunction = 1;
                }
            }
        }
    }

    public static void toggleTile(boolean enable) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("tile_active", enable);
        editor.apply();

        // タイルの状態を更新するためにタイルサービスを再起動
        Intent intent = new Intent(context, GlyphTileService.class);
        context.startService(intent);
    }

    private static boolean running = true;
    public static boolean isRunning() {
        return running;
    }

    public static void stop() {
        lastScaledPosition = 0;
        if (handler.hasCallbacks(updateRunnable)) {
            handler.removeCallbacks(updateRunnable);
        }
        running = false;
        if (mGM != null) {
            mGM.turnOff();
        }

        //GlyphLightTileService.updateTileState(false);
    }

    public static void start() {
        handler.post(updateRunnable);
        running = true;
        if (mGM == null) {
            mGM = GlyphManager.getInstance(context.getApplicationContext());
            mGM.init(new GlyphManager.Callback() {
                @Override
                public void onServiceConnected(ComponentName componentName) {
                    if (Common.is20111()) mGM.register(Common.DEVICE_20111);
                    if (Common.is22111()) mGM.register(Common.DEVICE_22111);
                    if (Common.is23111()) mGM.register(Common.DEVICE_23111);
                    try {
                        mGM.openSession();
                    } catch (Exception e) {
                        Log.e("GlyphPlus", "Error opening Glyph session", e);
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {
                    try {
                        mGM.closeSession();
                    } catch (GlyphException e) {
                        e.printStackTrace();
                    }
                }
            });

        }
        //GlyphLightTileService.updateTileState(true);
    }
}