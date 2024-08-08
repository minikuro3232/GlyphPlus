package mini.tikuwa.glyphplus;


import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimerAccessibilityService extends AccessibilityService {
    private static final Pattern TIME_PATTERN = Pattern.compile("(?:(\\d+)分)?\\s*(?:(\\d+)秒)?");
    private static final Pattern TIME_PATTERN_En = Pattern.compile("(?:(\\d+) minutes )?\\s*(?:(\\d+) seconds)?");
    private static final Pattern TIME_PATTERN2 = Pattern.compile("(\\d{2}):(\\d{2})");
    private static int MaxSecond = 0;
    private long pauseTime = 0;
    private long resumeTime = 0;
    private int addTime = 0;
    private static boolean stop = false;
    private static boolean pause = false;
    private static String TAG = "TimerAccessibility";
    private static Notification notification;
    private Handler handler = new Handler();
    private long lastTimerCallTime = System.currentTimeMillis();
    private Runnable resetMaxSecondRunnable = new Runnable() {
        @Override
        public void run() {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastTimerCallTime >= 2000) { // 2 seconds of inactivity
                if (MaxSecond != 0) {
                    if (!pause) {
                        MaxSecond = 0;
                        addTime = 0;
                        Log.d(TAG, "MaxSecond reset to 0 due to inactivity!");
                    }
                    BackgroundService.isTimer = false;
                    stop = true;
                    if (handler.hasCallbacks(NotificationCheckRunnable)) {
                        handler.removeCallbacks(NotificationCheckRunnable);
                    }
                    Log.d(TAG, "MaxSecond reset to 0 due to inactivity.");
                }
            }
            handler.postDelayed(this, 2000); // Check every 2 seconds
        }
    };

    private Runnable NotificationCheckRunnable = new Runnable() {
        @Override
        public void run() {
            if (notification != null) {
                RemoteViews contentView = notification.contentView;
                RemoteViews bigContentView = notification.bigContentView;

                // RemoteViewsの内容を取得
                extractRemoteViewsContent(contentView);
                extractRemoteViewsContent(bigContentView);
            }
            handler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onServiceConnected() {
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED | AccessibilityEvent.TYPE_VIEW_CLICKED;
        info.feedbackType = AccessibilityEvent.TYPES_ALL_MASK;
        info.notificationTimeout = 100;
        info.flags = AccessibilityServiceInfo.DEFAULT;
        setServiceInfo(info);
    }

    @SuppressLint("NewApi")
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        CharSequence contentDescription = event.getContentDescription() != null ? event.getContentDescription().toString() : "";

        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            if (event.getPackageName().equals("com.google.android.deskclock")) {
                if (contentDescription.length() != 0) {

                    if (contentDescription.toString().equals("開始") || contentDescription.toString().equals("Start")) {
                        MaxSecond = 0;
                    }

                    Matcher matcher = TIME_PATTERN.matcher(contentDescription.toString());
                    if (matcher.find()) {
                        int minutes = matcher.group(1) != null ? Integer.parseInt(matcher.group(1)) : 0;
                        int seconds = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;

                        if (!(minutes == 0 && seconds == 0)) {
                            timer(minutes, seconds);
                        }
                    }

                    matcher = TIME_PATTERN_En.matcher(contentDescription.toString());
                    if (matcher.find()) {
                        int minutes = matcher.group(1) != null ? Integer.parseInt(matcher.group(1)) : 0;
                        int seconds = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;

                        if (!(minutes == 0 && seconds == 0)) {
                            timer(minutes, seconds);
                        }
                    }
                }
                lastTimerCallTime = System.currentTimeMillis();
            }
        }

        if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            if (event.getPackageName().equals("com.google.android.deskclock")) {
                Parcelable parcelable = event.getParcelableData();
                if (parcelable instanceof Notification) {
                    notification = (Notification) parcelable;

                    pause = false;
                    addTime = 0;

                    RemoteViews contentView = notification.contentView;
                    RemoteViews bigContentView = notification.bigContentView;

                    // RemoteViewsの内容を取得
                    extractRemoteViewsContent(contentView);
                    extractRemoteViewsContent(bigContentView);

                    if (handler.hasCallbacks(NotificationCheckRunnable)) {
                        handler.removeCallbacks(NotificationCheckRunnable);
                    }
                    handler.postDelayed(NotificationCheckRunnable, 500);
                }
            }
        }

        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            if (event.getContentDescription() != null) {
                if (event.getPackageName().equals("com.google.android.deskclock")) {
                    Log.d(TAG, "View clicked: " + event.getContentDescription());
                    Log.d(TAG, "View clicked: " + event.getClassName() + ", " + event.getText());
                    if (event.getContentDescription().equals("一時停止") || event.getContentDescription().equals("Pause")) {
                        pauseTime = System.currentTimeMillis();
                        if (handler.hasCallbacks(NotificationCheckRunnable)) {
                            handler.removeCallbacks(NotificationCheckRunnable);
                        }
                        pause = true;
                    }
                    if (event.getContentDescription().equals("再開") || event.getContentDescription().equals("開始") || event.getContentDescription().equals("Start") || event.getContentDescription().equals("Resume")) {
                        resumeTime = System.currentTimeMillis();
                        BackgroundService.isTimer = true;

                        long elapsedTime = resumeTime - pauseTime;
                        Log.d(TAG, "Paused time: " + pauseTime);
                        Log.d(TAG, "Resumed time: " + resumeTime);
                        Log.d(TAG, "Elapsed time: " + elapsedTime + " milliseconds");

                        addTime += (int) (elapsedTime / 1000);

                        if (handler.hasCallbacks(NotificationCheckRunnable)) {
                            handler.removeCallbacks(NotificationCheckRunnable);
                        }
                        pause = false;
                        handler.postDelayed(NotificationCheckRunnable, 500);
                    }

                    if (event.getContentDescription().equals("削除") || event.getContentDescription().equals("Delete")) {
                        if (handler.hasCallbacks(NotificationCheckRunnable)) {
                            handler.removeCallbacks(NotificationCheckRunnable);
                        }
                        notification = null;
                    }
                }
            }
        }
    }

    private void timer(int minutes, int seconds) {
        int sec = minutes * 60 + seconds;
        if (MaxSecond < sec) {
            MaxSecond = sec;
            BackgroundService.isTimer = true;
            handler.removeCallbacks(resetMaxSecondRunnable);
            handler.postDelayed(resetMaxSecondRunnable, 2000);
        }

        if (sec == 1) {
            MaxSecond = 1;
            stop = false;
        }

        double playbackRate = (double) sec / MaxSecond;
        int scaledPosition = (int) Math.ceil(playbackRate * 24);

        BackgroundService.TimerGlyph = playbackRate;
        Log.d(TAG, "残り時間: " + minutes + " 分 " + seconds + " 秒 / 24:" + scaledPosition);

        lastTimerCallTime = System.currentTimeMillis(); // Update timer call time
    }

    private void extractRemoteViewsContent(RemoteViews remoteViews) {
        if (remoteViews != null) {
            try {
                Context context = getApplicationContext();
                FrameLayout frameLayout = new FrameLayout(context);
                View view = remoteViews.apply(context, frameLayout);
                logViewContent(view);
            } catch (Exception e) {
                Log.e("MyNotificationListener", "Error extracting RemoteViews content", e);
            }
        }
    }

    private void logViewContent(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                logViewContent(viewGroup.getChildAt(i));
            }
        } else if (view instanceof TextView) {
            TextView textView = (TextView) view;
            String text = textView.getText().toString();
            if (text.matches("\\d{2}:\\d{2}")) {
                Matcher matcher = TIME_PATTERN2.matcher(text);
                if (matcher.find()) {
                    int minutes = matcher.group(1) != null ? Integer.parseInt(matcher.group(1)) : 0;
                    int seconds = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;

                    seconds += addTime;

                    // 秒が60以上なら分に繰り上げ
                    if (seconds >= 60) {
                        minutes += seconds / 60;
                        seconds = seconds % 60;
                    }

                    timer(minutes, seconds);
                }
            }
        }
    }

    @Override
    public void onInterrupt() {
        // This method is called when the service is interrupted.
    }
}
