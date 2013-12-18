package com.github.messageme;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.github.messageme.com.github.messageme.interfaces.NotificationIdManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Code to interact with SMS content provider.
 *
 * Created by keith on 12/16/13.
 */
public class SmsDatabase implements com.github.messageme.com.github.messageme.interfaces.SmsDatabase {
    public static final Uri INBOX_CONTENT_URI = Uri.parse("content://sms/inbox");
    public static final Uri SENT_CONTENT_URI = Uri.parse("content://sms/sent");
    public static final String SMS_ADDRESS = "address";
    public static final String SMS_THREAD_ID = "thread_id";
    public static final String SMS_DATE = "date";
    public static final String SMS_BODY = "body";
    public static final String SMS_READ = "read";
    public static final String SMS_ID = "_id";

    private static final String TAG = "SmsDatabase";

    private ContentResolver cr;
    private SmsObserver observer;
    private Cursor observerCursor;

    public SmsDatabase(ContentResolver resolver) {
        cr = resolver;
    }

    @Override
    public void markRead(String phoneNumber) {
        Log.v(TAG, "Marking messages from " + phoneNumber + " as read");
        final String WHERE_CONDITION = SMS_READ + " = 0 AND " + SMS_ADDRESS + " = ?";

        ContentValues values = new ContentValues();
        values.put(SMS_READ, true);
        cr.update(INBOX_CONTENT_URI, values, WHERE_CONDITION, new String[] { phoneNumber });
    }

    @Override
    public List<String> getUnread(String phoneNumber) {
        ArrayList<String> messages = new ArrayList<String>();

        final String WHERE_CONDITION = SMS_READ + " = 0 AND " + SMS_ADDRESS + " = ?";
        final String SORT_ORDER = "date ASC";

        Cursor cursor = cr.query(INBOX_CONTENT_URI,
                new String[] { SMS_ID, SMS_ADDRESS, SMS_DATE, SMS_BODY },
                WHERE_CONDITION,
                new String[] { phoneNumber },
                SORT_ORDER);

        if (cursor == null) {
            return messages;
        }

        try {
            if (cursor.moveToFirst()) {
                int textColumn = cursor.getColumnIndex(SMS_BODY);

                do {
                    String text = cursor.getString(textColumn);
                    messages.add(text);
                } while (cursor.moveToNext());
            }
        }
        finally {
            cursor.close();
        }

        return messages;
    }

    @Override
    public void writeSentMessage(String phoneNumber, String messageBody) {
        ContentValues values = new ContentValues();
        values.put(SMS_ADDRESS, phoneNumber);
        values.put(SMS_BODY, messageBody);

        // TODO: set the thread id

        cr.insert(SENT_CONTENT_URI, values);
    }

    public HashMap<String, int[]> getUnreadCounts(Iterable<String> phoneNumbers) {
        final String WHERE_CONDITION = SMS_READ + " = 0";

        // TODO: This query probes all unread messages regardless of contact.
        Cursor cursor = cr.query(INBOX_CONTENT_URI,
                new String[] { SMS_ID, SMS_ADDRESS },
                WHERE_CONDITION,
                null,
                null);

        HashMap<String, int[]> unreadCounts = new HashMap<String, int[]>();
        for (String phoneNumber : phoneNumbers) {
            unreadCounts.put(phoneNumber, new int[] { 0 });
        }

        if (cursor == null) {
            return unreadCounts;
        }

        try {
            if (cursor.moveToFirst()) {
                final int PHONE_NUMBER_COL = cursor.getColumnIndex(SMS_ADDRESS);

                do {
                    String phone = cursor.getString(PHONE_NUMBER_COL);
                    int[] countWrapper = unreadCounts.get(phone);
                    if (countWrapper != null) {
                        countWrapper[0]++;
                    }
                    else {
                        countWrapper = new int[] { 1 };
                        unreadCounts.put(phone, countWrapper);
                    }
                } while (cursor.moveToNext());
            }
        }
        finally {
            cursor.close();
        }

        return unreadCounts;
    }

    public void checkCreateObserver(Context context, NotificationIdManager idManager) {
        Log.v(TAG, "checkCreateObserver");

        if (observer != null) {
            return;
        }

        observer = new SmsObserver(null, context, idManager);

        final String WHERE_CONDITION = SMS_READ + " = 0";
        observerCursor = cr.query(INBOX_CONTENT_URI,
                new String[] { SMS_ID, SMS_ADDRESS },
                WHERE_CONDITION,
                null,
                null);

        observerCursor.registerContentObserver(observer);
        Log.v(TAG, "\tregistered");
    }

    public void checkUnregisterObserver() {
        Log.v(TAG, "checkUnregisterObserver");
        if (observer == null) {
            return;
        }

        // if there are any active notifications still, don't unregister
        if (observer.getActiveNotificationPhoneNumbers().size() > 0) {
            Log.v(TAG, "\tthere are still active notifications, not unregistering");
            return;
        }

        // if there are no active notifications
        observerCursor.unregisterContentObserver(observer);
        observerCursor.close();
        Log.v(TAG, "\tunregistered!");
    }
}
