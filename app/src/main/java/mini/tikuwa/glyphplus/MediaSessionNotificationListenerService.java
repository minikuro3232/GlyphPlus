package mini.tikuwa.glyphplus;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.List;

public class MediaSessionNotificationListenerService extends NotificationListenerService {

    private static final String TAG = "TimerNotificationListener";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        Bundle extras = notification.extras;

        NotificationCompat.MessagingStyle messagingStyle = (NotificationCompat.MessagingStyle) extras.get(Notification.EXTRA_MESSAGING_PERSON);
        if (messagingStyle != null) {
            // メッセージングスタイルがある場合は、メッセージリストを取得
            List<NotificationCompat.MessagingStyle.Message> messages = messagingStyle.getMessages();
            for (NotificationCompat.MessagingStyle.Message message : messages) {
                Log.d("Notification", "Message: " + message.getText());
            }
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // 通知が削除されたときの処理
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        // リスナーが接続されたときの処理
    }
}
