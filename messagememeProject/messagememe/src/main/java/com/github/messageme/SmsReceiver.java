package com.github.messageme;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by ryan on 11/9/13.
 */
public class SmsReceiver extends BroadcastReceiver{

    // Get the object of SmsManager
    final SmsManager sms = SmsManager.getDefault();

    @Override
    public void onReceive(Context context, Intent intent) {
    // Retrieves a map of extended data from the intent.
        final Bundle bundle = intent.getExtras();

        try {
            Log.e("SmsReceiver", "got called");
            if (bundle != null) {

                final Object[] pdusObj = (Object[]) bundle.get("pdus");

                for (int i = 0; i < pdusObj.length; i++) {

                    SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) pdusObj[i]);

                    showNotification(context, currentMessage);

                }
            }

        } catch (Exception e) {
            Log.e("SmsReceiver", "Exception smsReceiver" +e);
        }
    }

    private void showNotification(Context context, SmsMessage currentMessage) {
        String phoneNumber = currentMessage.getDisplayOriginatingAddress();

        String senderNum = phoneNumber;
        String smsMessage = currentMessage.getDisplayMessageBody();

        String contactName = getContactNameFromPhoneNumber(context, phoneNumber);

        Notification.Builder mBuilder =
                new Notification.Builder(context)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("Message from " + contactName)
                        .setContentText(smsMessage);

        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        int mId = 0;
        mNotificationManager.notify(mId, mBuilder.build());

    }

    private String getContactNameFromPhoneNumber(Context context, String phoneNumber) {
        // Android 2.0 and later
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        // Query the filter URI
        String[] projection = new String[]{ ContactsContract.PhoneLookup.DISPLAY_NAME };
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
        if (cursor.moveToFirst()) {
            int index = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
            return cursor.getString(index);
        }
        return null;
    }


}
