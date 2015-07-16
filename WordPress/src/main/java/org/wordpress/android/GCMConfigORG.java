package org.wordpress.android;

public interface GCMConfigORG {


    // CONSTANTS
    static final String YOUR_SERVER_URL =  "http://192.168.21.89/GCM_code/gcm_server_files/register.php";
    // YOUR_SERVER_URL : Server url where you have placed your server files
    // Google project id
    static final String GOOGLE_SENDER_ID = "906832278927";  // Place here your Google project id

    /**
     * Tag used on log messages.
     */

    static final String TAG = "GCM Android Example";

    static final String DISPLAY_MESSAGE_ACTION =
            "com.androidexample.gcm.DISPLAY_MESSAGE";

    static final String EXTRA_MESSAGE = "message";


}