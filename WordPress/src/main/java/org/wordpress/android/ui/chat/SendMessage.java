package org.wordpress.android.ui.chat;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.accounts.helpers.APIFunctions;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;


public class SendMessage extends AsyncTask<Void, Void, String> {

    private Context ctx;
    public SendMessage(Context ctx){
        this.ctx = ctx;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(Void... arg0) {
        APIFunctions userFunction = new APIFunctions();
        String username = WordPress.getCurrentBlog().getUsername();

        JSONObject json = userFunction.sendMessage(username);
        String result="";
        try {
            result  = json.getString("result");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if(result.equals("OK")){
            Toast.makeText(ctx, "Message sent!", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(ctx, "Message failed!", Toast.LENGTH_SHORT).show();
        }

    }
}