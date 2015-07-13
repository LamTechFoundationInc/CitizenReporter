package org.wordpress.android.ui.posts.adapters;

import android.app.Dialog;
import android.content.Context;
import android.media.Image;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.posts.Question;

import info.hoang8f.widget.FButton;

public class GuideArrayAdapter extends ArrayAdapter<Question> {

    Context context;
    int layoutResourceId;
    Question data[] = null;

    public GuideArrayAdapter(Context context, int layoutResourceId, Question[] data) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        QuestionHolder holder = null;
        if(row == null)
        {
            LayoutInflater inflater = ((ActionBarActivity)context).getLayoutInflater();
            row = inflater.inflate(R.layout.view_row, parent, false);

            holder = new QuestionHolder();
            holder.txtTitle = (TextView)row.findViewById(R.id.header_text);
            holder.txtContent = (TextView)row.findViewById(R.id.content_text);

            holder.editButton = (ImageView)row.findViewById(R.id.editButton);
            holder.filledButton = (ImageView)row.findViewById(R.id.filledButton);

            row.setTag(holder);
        }
        else
        {
            holder = (QuestionHolder)row.getTag();
        }

        final Question question = data[position];
        holder.txtTitle.setText(question.title);
        holder.txtContent.setText(question.answer);

        final QuestionHolder finalHolder = holder;
        holder.editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCreateSummaryDialog(finalHolder, finalHolder.txtContent, finalHolder.filledButton, question);
            }
        });

        return row;
    }

    public void showCreateSummaryDialog(final QuestionHolder holder, final TextView displaySummary, final ImageView filledButton, final Question question){
        final Dialog dialog = new Dialog(context);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        dialog.setContentView(R.layout.fivew_fragment);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        final FButton submitButton = (FButton)dialog.findViewById(R.id.submitButton);
        submitButton.setEnabled(false);

        final EditText editTextSummary = (EditText)dialog.findViewById(R.id.editTextSummary);

        String summary = "";

        //find current value of summary
        summary = "" + displaySummary.getText().toString();
        //if it's not default & not empty edit editTextSummary
        if((!summary.equals(""))){
            editTextSummary.setText(summary);
            submitButton.setEnabled(true);
        }else{
            summary = "";
        }

        editTextSummary.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                String newSummary = "" + editTextSummary.getText().toString();
                if (newSummary.length() > 0) {
                    submitButton.setEnabled(true);
                } else {
                    submitButton.setEnabled(false);
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        dialog.findViewById(R.id.closeDialog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String summary = editTextSummary.getText().toString();

                if (summary.trim().length() > 0) {
                    displaySummary.setText(summary);
                    filledButton.setColorFilter(context.getResources().getColor(R.color.alert_green), android.graphics.PorterDuff.Mode.MULTIPLY);

                } else {
                    displaySummary.setText("");
                    filledButton.setColorFilter(context.getResources().getColor(R.color.grey), android.graphics.PorterDuff.Mode.MULTIPLY);

                }

                question.setAnswer(summary);
                holder.answer = summary;

                dialog.dismiss();
            }
        });

        dialog.show();
    }
    static class QuestionHolder
    {
        TextView txtTitle;
        TextView txtContent;
        ImageView editButton;
        ImageView filledButton;
        String answer;
    }
}