package org.wordpress.android;

import android.content.Context;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;

import org.wordpress.android.ui.chat.ChatActivity;
import org.wordpress.android.ui.chat.Message;
import org.wordpress.android.ui.comments.CommentsActivity;
import org.wordpress.android.ui.main.RipotiMainActivity;

public class GCMIntentService extends GCMBaseIntentService {

    private static final String TAG = "GCMIntentService";

    private WordPress aController = null;

    private int messageType;
    private String message;

    public GCMIntentService() {
        // Call extended class Constructor GCMBaseIntentService
        super(GCMConfigORG.GOOGLE_SENDER_ID);
    }

    /**
     * Method called on device registered
     **/
    @Override
    protected void onRegistered(Context context, String registrationId) {

        //Get Global Controller Class object (see application tag in AndroidManifest.xml)
        if(aController == null)
            aController = (WordPress) getApplicationContext();

        Log.i(TAG, "Device registered: regId = " + registrationId);
        aController.displayMessageOnScreen(context, "Your device registred with GCM");
        aController.register(context, registrationId);
    }

    /**
     * Method called on device unregistred
     * */
    @Override
    protected void onUnregistered(Context context, String registrationId) {
        if(aController == null)
            aController = (WordPress) getApplicationContext();
        Log.i(TAG, "Device unregistered");
        aController.displayMessageOnScreen(context, getString(R.string.gcm_unregistered));
        aController.unregister(context, registrationId);
    }

    /**
     * Method called on Receiving a new message from GCM server
     * */
    @Override
    protected void onMessage(Context context, Intent intent) {

        if(aController == null)
            aController = (WordPress) getApplicationContext();

        Log.i(TAG, "Received message");

        //compose message depending on type
        if(intent.hasExtra("assignment")){
            messageType = 0;
            message = intent.getExtras().getString("assignment");
        }else if(intent.hasExtra("feedback")){
            messageType = 1;
            message = intent.getExtras().getString("feedback");
        }else if(intent.hasExtra("chat")){
            messageType = 2;
            message = intent.getExtras().getString("chat");
        }

        aController.displayMessageOnScreen(context, message);
        // notifies user
        generateNotification(context, message);
    }

    /**
     * Method called on receiving a deleted message
     * */
    @Override
    protected void onDeletedMessages(Context context, int total) {

        if(aController == null)
            aController = (WordPress) getApplicationContext();

        Log.i(TAG, "Received deleted messages notification");
        String message = getString(R.string.gcm_deleted, total);
        aController.displayMessageOnScreen(context, message);
        // notifies user
        generateNotification(context, message);
    }

    /**
     * Method called on Error
     * */
    @Override
    public void onError(Context context, String errorId) {

        if(aController == null)
            aController = (WordPress) getApplicationContext();

        Log.i(TAG, "Received error: " + errorId);
        aController.displayMessageOnScreen(context, getString(R.string.gcm_error, errorId));
    }

    @Override
    protected boolean onRecoverableError(Context context, String errorId) {

        if(aController == null)
            aController = (WordPress) getApplicationContext();

        // log message
        Log.i(TAG, "Received recoverable error: " + errorId);
        aController.displayMessageOnScreen(context, getString(R.string.gcm_recoverable_error,
                errorId));
        return super.onRecoverableError(context, errorId);
    }

    /**
     * Create a notification to inform the user that server has sent a message.
     */
    private void generateNotification(Context context, String message) {
        int icon = R.drawable.noticon_clock;
        long when = System.currentTimeMillis();
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(icon, message, when);

        String title = context.getString(R.string.app_name) + " | New ";

        Intent notificationIntent = null;

        if(messageType==0){
            title += "Assignment";
            notificationIntent = new Intent(context, RipotiMainActivity.class);

        }else if (messageType == 1){

            title += "Feedback";
            notificationIntent = new Intent(context, CommentsActivity.class);

        }else{
            //insert to db
            Message chat = new Message();
            chat.setMessage(message.trim());
            chat.setIsMine("2");
            WordPress.wpDB.addMessage(chat);

            title += "Chat";
            notificationIntent = new Intent(context, ChatActivity.class);
        }

        // set intent so it does not start a new activity
        Bundle bundle = new Bundle();
        bundle.putString("assignment_refresh", "1");
        notificationIntent.putExtras(bundle);

        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent =
                PendingIntent.getActivity(context, 0, notificationIntent, 0);
        notification.setLatestEventInfo(context, title, message, intent);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        // Play default notification sound
        notification.defaults |= Notification.DEFAULT_SOUND;

        //notification.sound = Uri.parse("android.resource://" + context.getPackageName() + "your_sound_file_name.mp3");

        // Vibrate if vibrate is enabled
        notification.defaults |= Notification.DEFAULT_VIBRATE;
        notificationManager.notify(messageType, notification);

    }

    public static void clearNotificationsMap() {
    }

    public static int PUSH_NOTIFICATION_ID=0;

    public boolean shouldCircularizeNoteIcon(String type) {
        return false;
    }
}