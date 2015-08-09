package org.wordpress.android;

import android.content.Context;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;

import org.wordpress.android.ui.chat.ChatActivity;
import org.wordpress.android.ui.chat.Message;
import org.wordpress.android.ui.comments.CommentsActivity;
import org.wordpress.android.ui.main.RipotiMainActivity;
import org.wordpress.android.ui.posts.StoryBoard;
import org.wordpress.android.util.DeviceUtils;

public class GCMIntentService extends GCMBaseIntentService {

    private static final String TAG = "GCMIntentService";

    private WordPress aController = null;

    private int messageType;
    private String message;
    private String assignmentID;
    private String assignmentDeadline;
    private String user;

    public GCMIntentService() {
        // Call extended class Constructor GCMBaseIntentService
        super(BuildConfig.GCM_ID);
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
            //should have assignmentID
            assignmentID = intent.getExtras().getString("assignmentID");
            assignmentDeadline = "" + intent.getExtras().getString("assignmentDeadline");
            if(assignmentDeadline.equals("") || assignmentDeadline.equals("null")){
                assignmentDeadline = "Open ended";
            }else{
                assignmentDeadline = "Due on " + assignmentDeadline;
            }
            generateAdvancedAssignmentNotification();
        }else if(intent.hasExtra("feedback")){
            messageType = 1;
            message = intent.getExtras().getString("feedback");
            user = intent.getExtras().getString("author");
            generateFeedbackNotification();
        }else if(intent.hasExtra("chat")){
            messageType = 2;
            message = intent.getExtras().getString("chat");
            user = intent.getExtras().getString("user");
            generateChatNotification();
        }

        aController.displayMessageOnScreen(context, message);
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
        //generateNotification(context, message);
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

    public Intent imageIntent(){
        Intent intent = new Intent(this, StoryBoard.class);
        intent.putExtra("quick-media", DeviceUtils.getInstance().hasCamera(getApplicationContext())
                ? Constants.QUICK_POST_PHOTO_CAMERA
                : Constants.QUICK_POST_PHOTO_LIBRARY);
        intent.putExtra("isNew", true);
        intent.putExtra("shouldFinish", false);
        intent.putExtra("assignment_id", Integer.parseInt(assignmentID));

        return intent;
    }
    public Intent videoIntent(){
        Intent intent = new Intent(this, StoryBoard.class);
        intent.putExtra("quick-media", DeviceUtils.getInstance().hasCamera(getApplicationContext())
                ? Constants.QUICK_POST_VIDEO_CAMERA
                : Constants.QUICK_POST_PHOTO_LIBRARY);
        intent.putExtra("isNew", true);
        intent.putExtra("shouldFinish", false);
        intent.putExtra("assignment_id", Integer.parseInt(assignmentID));

        return intent;
    }
    public Intent audioIntent(){
        Intent intent = new Intent(this, StoryBoard.class);
        intent.putExtra("quick-media", Constants.QUICK_POST_AUDIO_MIC);
        intent.putExtra("isNew", true);
        intent.putExtra("shouldFinish", false);
        intent.putExtra("assignment_id", Integer.parseInt(assignmentID));

        return intent;
    }

    private void generateAdvancedAssignmentNotification(){
            PendingIntent imageIntent = PendingIntent.getActivity(this, 0, imageIntent(), 0);
            PendingIntent videoIntent = PendingIntent.getActivity(this, 0, videoIntent(), 0);
            PendingIntent audioIntent = PendingIntent.getActivity(this, 0, audioIntent(), 0);

            Notification notif = new Notification.Builder(getApplicationContext())
                    .setContentTitle("New Assignment" )
                    .setContentText(getResources().getString(R.string.expand_to_view))
                    .setSmallIcon(R.drawable.noticon_alert_big)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ab_icon_edit))
                    .setStyle(new Notification.InboxStyle()
                            .addLine(message)
                            .setBigContentTitle("New Assignment")
                            .setSummaryText(assignmentDeadline))
                    .setPriority(2)
                    .addAction(R.mipmap.ic_camera_white, "", imageIntent)
                    .addAction(R.mipmap.ic_video_white, "", videoIntent)
                    .addAction(R.mipmap.ic_audio_white, "", audioIntent)
                    .build();
            NotificationManager notificationManager = (NotificationManager)getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(55, notif);
    }

    private void generateChatNotification() {
        //insert to db
        Message chat = new Message();
        chat.setMessage(message.trim());
        chat.setIsMine("2");
        WordPress.wpDB.addMessage(chat);

        //create intent
        Intent chatIntent1 = new Intent(getApplicationContext(), ChatActivity.class);
        PendingIntent chatIntent = PendingIntent.getActivity(this, 0, chatIntent1, 0);

        //build notification
        Notification notif = new Notification.Builder(getApplicationContext())
                .setContentTitle(user)
                .setContentText(message)
                .setSmallIcon(R.drawable.me_icon_support)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.me_icon_support))
                .setStyle(new Notification.InboxStyle()
                        .addLine(message)
                        .setBigContentTitle(user)/*
                        .setSummaryText(assignmentDeadline)*/)
                .setPriority(2)
                .addAction(R.mipmap.ic_reply, "Reply", chatIntent)
                .build();
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(56, notif);
    }
    private void generateFeedbackNotification() {
        //create intent
        Intent feedbackIntent1 = new Intent(getApplicationContext(), CommentsActivity.class);
        PendingIntent feedbackIntent = PendingIntent.getActivity(this, 0, feedbackIntent1, 0);

        //build notification
        Notification notif = new Notification.Builder(getApplicationContext())
                .setContentTitle(user)
                .setContentText(message)
                        .setSmallIcon(R.drawable.my_site_icon_comments)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.my_site_icon_comments))
                .setStyle(new Notification.InboxStyle()
                        .addLine(message)
                        .setBigContentTitle(user))
                .setPriority(2)
                .addAction(R.mipmap.ic_reply, "Reply", feedbackIntent)
                .build();
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(57, notif);
    }

    private void generateNotification(Context context) {
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