package org.wordpress.android.ui.chat;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.wordpress.android.BuildConfig;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;


public class SendMessage extends AsyncTask<Void, Void, String> {

    private Context ctx;
    private String sender;
    private String recipient;
    private String message;

    public SendMessage(Context ctx, String _sender, String _recipient, String _message){
        this.ctx = ctx;
        this.sender = _sender;
        this.recipient = _recipient;
        this.message = _message;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }


    @Override
    protected String doInBackground(Void... arg0) {
        URI url = null;
        try {
            url = new URI(BuildConfig.API_URL + "public/send_message/" + sender + "/" + recipient + "/" + message);
            Log.d("url'", BuildConfig.API_URL + "public/send_message/" + "/" + sender + "/" + recipient + "/" + message);
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet request = new HttpGet();
        request.setURI(url);
        try {
            httpclient.execute(request);
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }


    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        Toast.makeText(ctx, "Message sent!", Toast.LENGTH_SHORT).show();
    }
}