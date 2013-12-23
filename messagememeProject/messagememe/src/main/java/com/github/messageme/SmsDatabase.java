package com.github.messageme;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.github.messageme.interfaces.NotificationIdManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Code to interact with SMS content provider.
 *
 * Created by keith on 12/16/13.
 */
public class SmsDatabase {
    public static final Uri INBOX_CONTENT_URI = Uri.parse("content://sms/inbox");
    public static final Uri SENT_CONTENT_URI = Uri.parse("content://sms/sent");
    public static final String SMS_ADDRESS = "address";
    public static final String SMS_THREAD_ID = "thread_id";
    public static final String SMS_DATE = "date";
    public static final String SMS_BODY = "body";
    public static final String SMS_READ = "read";
    public static final String SMS_ID = "_id";

    public static final String WHERE_UNREAD = SMS_READ + " = 0";
    public static final String WHERE_UNREAD_AND_ADDRESS = WHERE_UNREAD + " AND " + SMS_ADDRESS + " = ?";

    public static final String SORT_CHRONOLOGICAL = "date ASC";

    private static final String TAG = "SmsDatabase";

    private final ContentResolver cr;
    private SmsObserver observer;

    public SmsDatabase(ContentResolver resolver) {
        cr = resolver;
    }

    /**
     * Mark all messages from this contact as read.
     * @param phoneNumber phone number (not normalized)
     */
    public void markRead(String phoneNumber) {
        Log.v(TAG, "markRead(" + phoneNumber + ")");

        ContentValues values = new ContentValues();
        values.put(SMS_READ, true);
        cr.update(INBOX_CONTENT_URI, values, WHERE_UNREAD_AND_ADDRESS, new String[] { phoneNumber });
    }

    /**
     * Get all unread messages from this contact in
     * the order they were received.
     * @param phoneNumber phone number (not normalized)
     * @return list of the messages, in chronological order
     */
    public List<String> getUnread(String phoneNumber) {
        ArrayList<String> messages = new ArrayList<String>();

        Cursor cursor = cr.query(INBOX_CONTENT_URI,
                new String[] { SMS_ID, SMS_ADDRESS, SMS_DATE, SMS_BODY },
                WHERE_UNREAD_AND_ADDRESS,
                new String[] { phoneNumber },
                SORT_CHRONOLOGICAL);

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

    /**
     * Write this sent message to the SMS database.
     * @param phoneNumber recipient's phone number
     * @param messageBody the text
     */
    public void writeSentMessage(String phoneNumber, String messageBody) {
        ContentValues values = new ContentValues();
        values.put(SMS_ADDRESS, phoneNumber);
        values.put(SMS_BODY, messageBody);

        // TODO: set the thread id

        cr.insert(SENT_CONTENT_URI, values);
    }

    /**
     * Get the number of unread SMS messages for the listed phone numbers.
     * The values of the HashMap are singleton int arrays that store the count.
     * If there are unread messages for other contacts, this will count them as well.
     *
     * @param phoneNumbers list of phone numbers to look for
     * @return HashMap that maps phone numbers to counts, where counts are stored as singleton int arrays
     */
    public HashMap<String, int[]> getUnreadCounts(Iterable<String> phoneNumbers) {
        // Note: This query probes all unread messages regardless of contact.
        Cursor cursor = cr.query(INBOX_CONTENT_URI,
                new String[] { SMS_ID, SMS_ADDRESS },
                WHERE_UNREAD,
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

    /**
     * Create the SMS database observer if necessary.
     *
     * @param context Android context, used by SmsObserver
     * @param idManager
     */
    public void checkCreateObserver(Context context, NotificationIdManager idManager) {
        Log.v(TAG, "checkCreateObserver");

        if (observer != null) {
            Log.v(TAG, "\taborting, observer already exists");
            return;
        }

        // TODO: Handler should be set on current thread
        observer = new SmsObserver(null, context, idManager);

        cr.registerContentObserver(Uri.parse("content://sms"), true, observer);

        Log.v(TAG, "\tregistered");
    }

    /**
     * Unregister the SMS database observer if there are no active notifications.
     */
    public void checkUnregisterObserver() {
        Log.v(TAG, "checkUnregisterObserver");
        if (observer == null) {
            Log.v(TAG, "\texiting early, observer is already null");
            return;
        }

        // if there are any active notifications still, don't unregister
        if (observer.getActiveNotificationPhoneNumbers().size() > 0) {
            Log.v(TAG, "\tthere are still active notifications, not unregistering");
            return;
        }

        // if there are no active notifications
        observer = null;
        Log.v(TAG, "\tunregistered!");
    }
}
