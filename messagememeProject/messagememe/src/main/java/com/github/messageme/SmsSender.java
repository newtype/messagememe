package com.github.messageme;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.BrokenBarrierException;

/**
 * Created by ryan on 11/17/13.
 */
public class SmsSender extends BroadcastReceiver {

    private static final String TAG = "SmsSender";
    public static final String AUTO_RESPONSE_INTENT = "com.github.messageme.AUTO_RESPONSE";
    private SmsDatabase smsDatabase;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (AUTO_RESPONSE_INTENT.equals(intent.getAction())) {
            handleSendIntent(context, intent);
        }
        else {
            Log.e(TAG, "Received unknown intent: " + intent);
        }
    }

    public void handleSendIntent(Context context, Intent intent) {
        Log.v(TAG, "handleSendIntent");

        String destination = intent.getStringExtra(SmsReceiver.DESTINATION_ADDRESS);
        String body = intent.getStringExtra(SmsReceiver.BODY);

        Log.v(TAG, "Send \"" + body + "\" to " + destination);

        if (SmsReceiver.LOG_SMS_ONLY) {
            Toast.makeText(context, "Fake send to " + destination, Toast.LENGTH_LONG).show();
        }
        else {
            // TODO: Set pending intents for success/fail and alert the user on failure
            // and maybe don't write to sent messages if it failed to send
            SmsManager.getDefault().sendTextMessage(destination, null, body, null, null);

            if (smsDatabase == null) {
                smsDatabase = new SmsDatabase(context.getContentResolver());
            }
            smsDatabase.markRead(destination);
            smsDatabase.writeSentMessage(destination, body);
        }

        // clear the notification
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(intent.getIntExtra(SmsReceiver.NOTIFICATION_ID, -1));

        if (smsDatabase == null) {
            smsDatabase = new SmsDatabase(context.getContentResolver());
        }
        smsDatabase.checkUnregisterObserver();
    }
}
