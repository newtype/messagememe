package com.github.messageme;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

/**
 * Code to interact with SMS content provider.
 *
 * Created by keith on 12/16/13.
 */
public class SmsDatabase {
    public static final Uri INBOX_CONTENT_URI = Uri.parse("content://sms/inbox");
    private ContentResolver cr;

    public SmsDatabase(ContentResolver resolver) {
        cr = resolver;
    }

    List<String> getUnread(String phoneNumber) {
        ArrayList<String> messages = new ArrayList<String>();

        final String SMS_ADDRESS = "address";
        final String SMS_THREAD_ID = "thread_id";
        final String SMS_DATE = "date";
        final String SMS_BODY = "body";
        final String SMS_READ = "read";

        final String WHERE_CONDITION = SMS_READ + " = 0 AND " + SMS_ADDRESS + " = ?";
        final String SORT_ORDER = "date ASC";

        Cursor cursor = cr.query(INBOX_CONTENT_URI,
                new String[] { "_id", SMS_ADDRESS, SMS_DATE, SMS_BODY },
                WHERE_CONDITION,
                new String[] { phoneNumber },
                SORT_ORDER);

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
}
