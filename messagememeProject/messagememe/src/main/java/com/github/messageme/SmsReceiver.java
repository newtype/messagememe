package com.github.messageme;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

/**
 * Created by ryan on 11/9/13.
 */
public class SmsReceiver extends BroadcastReceiver {

    public static final String TAG = "SmsReceiver";
    private static final String AUTO_RESPONSE_INTENT = "com.github.messageme.AUTO_RESPONSE";
    private static final String DESTINATION_ADDRESS = "destinationAddress";
    private static final String BODY = "body";
    private static final int PENDING_POSITIVE = 0;
    private static final int PENDING_NEGATIVE = 1;
    private static final String NOTIFICATION_ID = "notificationId";
    private static int currentNotificationId = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        
        if (AUTO_RESPONSE_INTENT.equals(intent.getAction())) {
            handleSendIntent(context, intent);
        }
        else {
            handleIncomingSms(context, intent);
        }
    }

    private void handleIncomingSms(Context context, Intent intent) {
        final Bundle bundle = intent.getExtras();

        try {
            Log.v(TAG, "handleIncomingSms");
            if (bundle != null) {

                final Object[] pdusObj = (Object[]) bundle.get("pdus");

                for (int i = 0; i < pdusObj.length; i++) {

                    SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) pdusObj[i]);

                    showNotification(context, currentMessage);

                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in getting message", e);
        }
    }

    private void handleSendIntent(Context context, Intent intent) {
        Log.v(TAG, "handleSendIntent");

        String destination = intent.getStringExtra(DESTINATION_ADDRESS);
        String body = intent.getStringExtra(BODY);

        Log.v(TAG, "Send \"" + body + "\" to " + destination);

        // send a text message without an intent to notify that it's been sent
        //sms.sendTextMessage(destination, null, body, null, null);
        Toast.makeText(context, "Fake send to " + destination, Toast.LENGTH_LONG).show();

        // clear the notification
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(intent.getIntExtra(NOTIFICATION_ID, -1));
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void showNotification(Context context, SmsMessage currentMessage) {
        String phoneNumber = currentMessage.getDisplayOriginatingAddress();

        String smsMessage = currentMessage.getDisplayMessageBody();

        String contactName = getContactNameFromPhoneNumber(context, phoneNumber);

        Intent positiveReplyIntent = buildQuickResponseIntent(context, phoneNumber, "Sure", currentNotificationId);
        PendingIntent positivePending = PendingIntent.getBroadcast(context, PENDING_POSITIVE, positiveReplyIntent, Intent.FLAG_ACTIVITY_NEW_TASK);

        Intent negativeReplyIntent = buildQuickResponseIntent(context, phoneNumber, "Nah", currentNotificationId);
        PendingIntent negativePending = PendingIntent.getBroadcast(context, PENDING_NEGATIVE, negativeReplyIntent, Intent.FLAG_ACTIVITY_NEW_TASK);

        // TODO: addAction is Jelly Bean and above.  Switch to NotificationCompat or require JB API level.
        Notification.Builder mBuilder = new Notification.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Message from " + contactName)
                .setContentText(smsMessage)
                .addAction(R.drawable.check, "Sure", positivePending)
                .addAction(R.drawable.x, "Nah", negativePending);

        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(currentNotificationId, mBuilder.build());

        currentNotificationId++;
    }

    private String getContactNameFromPhoneNumber(Context context, String phoneNumber) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        // Query the filter URI
        String[] projection = new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME};
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
        if (cursor.moveToFirst()) {
            int index = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
            return cursor.getString(index);
        }

        return null;
    }

    private Intent buildQuickResponseIntent(Context context, String destinationAddress, String body, int notificationId) {
        Intent quickResponseIntent = new Intent(AUTO_RESPONSE_INTENT);
        quickResponseIntent.putExtra(DESTINATION_ADDRESS, destinationAddress);
        quickResponseIntent.putExtra(BODY, body);
        quickResponseIntent.putExtra(NOTIFICATION_ID, currentNotificationId);
        return quickResponseIntent;
    }

}
