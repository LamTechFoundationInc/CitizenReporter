package org.wordpress.android.wallet;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;

import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.chat.GifAnimationDrawable;
import org.wordpress.android.ui.accounts.helpers.APIFunctions;

import java.util.ArrayList;
import java.util.List;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardGridArrayAdapter;
public class PaymentsListActivity extends ActionBarActivity {
    ListView gridView;
    List<Payment> paymentsList;

    private PaymentsArrayAdapter adClass;

    private WordPressDB db;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.payments_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getResources().getString(R.string.my_payments));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        gridView = (ListView) findViewById(R.id.paymentsList);

        db = WordPress.wpDB;

        paymentsList = db.getPayments();

        adClass = new PaymentsArrayAdapter(this, paymentsList);
        gridView.setAdapter(adClass);

    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class PaymentsArrayAdapter  extends ArrayAdapter<Payment> {
        Context context;
        private List<Payment> TextValue;
        private Payment payment;

        public PaymentsArrayAdapter(Context context, List<Payment> TextValue) {
            super(context, R.layout.payment_row, TextValue);
            this.context = context;
            this.TextValue= TextValue;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // TODO Auto-generated method stub

            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            payment = paymentsList.get(position);
            final ViewHolder holder;
            if(convertView == null)
            {
                holder = new ViewHolder();
                convertView = inflater.inflate(R.layout.payment_row, parent, false);
                holder.message = (TextView) convertView.findViewById(R.id.message_text);

                holder.confirmLayout = (LinearLayout) convertView.findViewById(R.id.confirm_layout);
                holder.confirmIcon = (ImageView) convertView.findViewById(R.id.confirm_icon);
                holder.confirmText = (TextView) convertView.findViewById(R.id.confirm_text);

                holder.disputeLayout = (LinearLayout) convertView.findViewById(R.id.dispute_layout);
                holder.disputeIcon = (ImageView) convertView.findViewById(R.id.dispute_icon);
                holder.disputeText = (TextView) convertView.findViewById(R.id.dispute_text);

                convertView.setTag(holder);
            }
            else
                holder = (ViewHolder) convertView.getTag();

            holder.message.setText(payment.getMessage());

            if(payment.getConfirmed().equals("1")){
                paymentConfirmed(payment, true, holder, false);
            }
            if(payment.getConfirmed().equals("-1")){
                paymentConfirmed(payment, true, holder, false);
            }

            holder.confirmLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    paymentConfirmed(payment, true, holder, true);
                }
            });

            holder.disputeLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    paymentConfirmed(payment, false, holder, true);

                }
            });

            return convertView;

        }
        public class ViewHolder
        {
            TextView message;
            LinearLayout confirmLayout;
            LinearLayout disputeLayout;
            ImageView confirmIcon;
            ImageView disputeIcon;
            TextView disputeText;
            TextView confirmText;
        }

        @Override
        public long getItemId(int position) {
            //Unimplemented, because we aren't using Sqlite.
            return position;
        }

    }

    public void paymentConfirmed(Payment payment, boolean isConfirmed, PaymentsArrayAdapter.ViewHolder holder, boolean update){

        if(isConfirmed){
            holder.confirmIcon.setColorFilter(getApplicationContext().getResources().getColor(R.color.alert_green), android.graphics.PorterDuff.Mode.MULTIPLY);
            holder.confirmText.setText(getApplicationContext().getResources().getString(R.string.confirmed));
            holder.disputeLayout.setVisibility(View.GONE);

        }else{
            //turn button green, text to confirmed, hide dispute, layout, save to db, api call, handle from notification
            holder.disputeIcon.setColorFilter(getApplicationContext().getResources().getColor(R.color.alert_green), android.graphics.PorterDuff.Mode.MULTIPLY);
            holder.disputeText.setText(getApplicationContext().getResources().getString(R.string.disputed));
            holder.confirmLayout.setVisibility(View.GONE);
        }

        if(update){

            String confirm;

            if(isConfirmed){
                confirm = "1";
                payment.setConfirmed("1");
                WordPress.wpDB.updatePayment(payment);
            }else{
                confirm = "0";
                payment.setConfirmed("-1");
                WordPress.wpDB.updatePayment(payment);
            }

            //send query
            new confirmPayment(payment.getPost(), confirm).execute();
        }

    }

    class confirmPayment extends AsyncTask<String, String, String>{

        private String post_id;
        private String confirm;

        public confirmPayment(String _post_id, String _confirm){
            this.post_id = _post_id;
            this.confirm = _confirm;
        }

        @Override
        protected String doInBackground(String... strings) {
            confirmPayment(post_id, confirm);
            return null;
        }
    }

    public void confirmPayment(String post_id, String confirm){
        APIFunctions userFunction = new APIFunctions();
        JSONObject json = userFunction.confirmPayment(post_id, confirm);

        String responseMessage = "";
        try {
            String res = json.getString("result");
            if (res.equals("OK")) {
                responseMessage = json.getString("message");

            } else {
                responseMessage = json.getString("error");
            }

            Log.d("Confirm payment", responseMessage + "");

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}