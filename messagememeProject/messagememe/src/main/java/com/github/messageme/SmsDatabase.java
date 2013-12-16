package com.github.messageme;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
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
    private static final String TAG = "SmsDatabase";

    private ContentResolver cr;

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
                new String[] { "_id", SMS_ADDRESS, SMS_DATE, SMS_BODY},
                WHERE_CONDITION,
                new String[] { phoneNumber },
                SORT_ORDER);

        if (cursor == null) {
            return messages;
        }

        if (cursor.moveToFirst()) {
            int textColumn = cursor.getColumnIndex(SMS_BODY);

            do {
                String text = cursor.getString(textColumn);
                messages.add(text);
            } while (cursor.moveToNext());
        }
        cursor.close();

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
}
