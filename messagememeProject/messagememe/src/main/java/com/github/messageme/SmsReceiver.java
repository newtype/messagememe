package com.github.messageme;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import com.github.messageme.com.github.messageme.interfaces.NotificationIdManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Handles SMS received intents.
 *
 * Created by ryan on 11/9/13.
 */
public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsReceiver";
    private static final String AUTO_RESPONSE_INTENT = "com.github.messageme.AUTO_RESPONSE";
    private static final String DESTINATION_ADDRESS = "destinationAddress";
    private static final String BODY = "body";
    private static final int PENDING_POSITIVE = 0;
    private static final int PENDING_NEGATIVE = 1;
    private static final String NOTIFICATION_ID = "notificationId";
    private static final int PENDING_TIME = 2;
    private static final boolean LOG_SMS_ONLY = false;
    private static final String NOTIFICATION_MESSAGE_SEPARATOR = "   ";

    private final NotificationIdManager idManager = new StaticVarNotificationIdManager();

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
        Log.v(TAG, "handleIncomingSms");

        final Bundle bundle = intent.getExtras();
        try {
            if (bundle != null) {

                final Object[] pdusObj = (Object[]) bundle.get("pdus");

                // In what situations will this iterate more than once?
                // Is there the chance of being spammy?
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

        if (LOG_SMS_ONLY) {
            Toast.makeText(context, "Fake send to " + destination, Toast.LENGTH_LONG).show();
        }
        else {
            // TODO: Set pending intents for success/fail and alert the user on failure
            // and maybe don't write to sent messages if it failed to send
            SmsManager.getDefault().sendTextMessage(destination, null, body, null, null);

            SmsDatabase database = new SmsDatabase(context.getContentResolver());
            database.markRead(destination);
            database.writeSentMessage(destination, body);
        }

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

        if (contactName == null) {
            Log.v(TAG, "No contact name for " + phoneNumber + ", not showing popup");
            return;
        }
        else {
            Log.v(TAG, "Received message from " + contactName + " (" + phoneNumber + "): " + smsMessage);
        }

        SmsDatabase database = new SmsDatabase(context.getContentResolver());
        List<String> unreadMessages = database.getUnread(phoneNumber);

        int currentNotificationId = idManager.getId(phoneNumber);

        // TODO: This code should load the quick response configuration from somewhere and build more programatically
        Intent positiveReplyIntent = buildQuickResponseIntent(context, phoneNumber, context.getString(R.string.response_yes), currentNotificationId);
        PendingIntent positivePending = PendingIntent.getBroadcast(context, PENDING_POSITIVE, positiveReplyIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
        
        Intent timeReplyIntent = buildQuickResponseIntent(context, phoneNumber, context.getString(R.string.response_time), currentNotificationId);
        PendingIntent timePending = PendingIntent.getBroadcast(context, PENDING_TIME, timeReplyIntent, Intent.FLAG_ACTIVITY_NEW_TASK);

        Intent negativeReplyIntent = buildQuickResponseIntent(context, phoneNumber, context.getString(R.string.response_no), currentNotificationId);
        PendingIntent negativePending = PendingIntent.getBroadcast(context, PENDING_NEGATIVE, negativeReplyIntent, Intent.FLAG_ACTIVITY_NEW_TASK);

        // TODO: addAction is Jelly Bean and above.  Switch to NotificationCompat or require JB API level.
        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(contactName)
                .setContentText(buildNotificationText(unreadMessages, smsMessage))
                .addAction(android.R.drawable.ic_media_play, context.getString(R.string.response_yes), positivePending)
                .addAction(android.R.drawable.ic_menu_recent_history, context.getString(R.string.response_time), timePending)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, context.getString(R.string.response_no), negativePending);

        Bitmap contactPhoto = getContactPhoto(context, getContactId(context.getContentResolver(), phoneNumber));
        if (contactPhoto != null) {
            builder.setLargeIcon(contactPhoto);
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(currentNotificationId, builder.build());
    }

    /**
     * Build the "long" text for the notification from the smsMessage plus any unread messages.
     * @param unreadMessages Any unread messages from the same sender, typically empty.  Assumed non-null.
     * @param smsMessage The body of the active SMS message.
     * @return notification text
     */
    private CharSequence buildNotificationText(List<String> unreadMessages, String smsMessage) {
        StringBuilder builder = new StringBuilder();

        for (String unreadMessage : unreadMessages) {
            builder.append(unreadMessage);
            builder.append(NOTIFICATION_MESSAGE_SEPARATOR);
        }

        builder.append(smsMessage);

        // TODO: Test this code.
        // 1) I couldn't seem to trigger it
        // 2) Due to the buttons, the text is only a single line in the layout, so 255 is too much.
        if (builder.length() > 255) {
            Log.d(TAG, "Trimming notification body to size");
            int lastSpace = builder.lastIndexOf(" ", 255);
            builder.delete(lastSpace, builder.length());
            builder.append(" ... (" + (unreadMessages.size() + 1) + ")");
        }

        return builder;
    }

    private String getContactNameFromPhoneNumber(Context context, String phoneNumber) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        // Query the filter URI
        String[] projection = new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME};
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);

        if (cursor == null) {
            return null;
        }

        try {
            if (cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
                String contactName = cursor.getString(index);
                return contactName;
            }
        }
        finally {
            cursor.close();
        }

        return null;
    }

    private long getContactId(ContentResolver resolver, String phoneNumber) {
        final Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        final Cursor cursor = resolver.query(uri, new String[]{ContactsContract.Contacts.PHOTO_ID, ContactsContract.Contacts._ID}, null, null, null);
        long thumbnailId = 0;

        try {
            if (cursor.moveToFirst()) {
                thumbnailId = cursor.getLong(cursor.getColumnIndex(ContactsContract.Contacts._ID));
            }
        }
        finally {
            cursor.close();
        }

        return thumbnailId;
    }

    private Bitmap getContactPhoto(Context context, long contactId) {
        if (contactId == 0) {
            return null;
        }

        ContentResolver resolver = context.getContentResolver();

        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
        InputStream photoInput = ContactsContract.Contacts.openContactPhotoInputStream(resolver, contactUri, true);

        if (photoInput == null) {
            return null;
        }

        Bitmap thumbnail = BitmapFactory.decodeStream(photoInput);
        try {
            photoInput.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to close contact photo input stream", e);
        }

        return scaleBitmap(thumbnail, context.getResources().getDimension(android.R.dimen.notification_large_icon_width), context.getResources().getDimension(android.R.dimen.notification_large_icon_height));
    }

    /**
     * Scale the bitmap up or down to match the target width and height.
     * Scales along the dimension of least change.
     */
    private Bitmap scaleBitmap(Bitmap bitmap, float targetWidth, float targetHeight) {
        // compute larger scale factor
        float scaleX = targetWidth / bitmap.getWidth();
        float scaleY = targetHeight / bitmap.getHeight();

        float scale = scaleX;
        if (scaleY > scale)
            scale = scaleY;

        // TODO: Rounding might be more appropriate
        int scaledHeight = (int) (scale * bitmap.getHeight());
        int scaledWidth = (int) (scale * bitmap.getWidth());

        // scale; the ImageView will crop it
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, false);
        return scaledBitmap;
    }

    private Intent buildQuickResponseIntent(Context context, String destinationAddress, String body, int notificationId) {
        Intent quickResponseIntent = new Intent(AUTO_RESPONSE_INTENT);
        quickResponseIntent.putExtra(DESTINATION_ADDRESS, destinationAddress);
        quickResponseIntent.putExtra(BODY, body);
        quickResponseIntent.putExtra(NOTIFICATION_ID, notificationId);
        return quickResponseIntent;
    }

}
