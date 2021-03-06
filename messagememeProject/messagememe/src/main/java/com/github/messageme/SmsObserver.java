package com.github.messageme;

import android.app.NotificationManager;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.util.Log;

import com.github.messageme.interfaces.NotificationIdManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Observer/listener for changes to the SMS inbox.
 *
 * Created by keith on 12/18/13.
 */
public class SmsObserver extends ContentObserver {
    private static final String TAG = "SmsObserver";
    private final Context context;
    private final Handler handler;

    /**
     * Note: This is package private so that SmsDatabase can use this idManager to query active notifications
     */
    final NotificationIdManager idManager;

    public SmsObserver(Handler handler, Context context, NotificationIdManager idManager) {
        super(handler);
        this.handler = handler;
        this.context = context;
        this.idManager = idManager;
    }

    @Override
    public void onChange(boolean selfChange) {
        Log.v(TAG, "onChange(" + selfChange + ")");
        Iterable<String> phoneNumbers = getActiveNotificationPhoneNumbers();
        HashMap<String,int[]> unreadCounts = new SmsDatabase(context.getContentResolver()).getUnreadCounts(phoneNumbers);

        for (Map.Entry<String, int[]> pair : unreadCounts.entrySet()) {
            Log.v(TAG, "\t" + pair.getKey() + ": " + pair.getValue()[0] + " unread messages");
            if (pair.getValue()[0] == 0) {
                dismissNotification(pair.getKey());
            }
        }
    }

    /**
     * Dismiss the Android notification and remove the notification ID from
     * the notification ID manager.
     * @param phoneNumber the phone number of the contact
     */
    private void dismissNotification(String phoneNumber) {
        Log.v(TAG, "dismissNotification(" + phoneNumber + ")");

        // clear the notification (if there is one)
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(idManager.getId(phoneNumber, false));

        // clear the tracking
        idManager.removeNotificationPhoneNumber(phoneNumber);
    }

    public Set<String> getActiveNotificationPhoneNumbers() {
        Log.v(TAG, "getActiveNotificationPhoneNumbers()");
        return idManager.getActiveNotificationPhoneNumbers();
    }


}
