package org.wordpress.android.ui.posts;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;

import info.hoang8f.widget.FButton;

/**
 * Created by john on 7/12/2015.
 */
public class StoryBoard extends ActionBarActivity {

    private LinearLayout summaryPane;

    private TextView displaySummary;
    private EditText editTextSummary;

    private String summary="";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.story_board);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        summaryPane = (LinearLayout)findViewById(R.id.summaryPane);
        displaySummary = (TextView)findViewById(R.id.displaySummary);
        editTextSummary = (EditText)findViewById(R.id.editTextSummary);

        //on click summary pane, show pop up dialog for post section
        summaryPane.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                showCreateSummaryDialog();
            }
        });
    }

    public void showCreateSummaryDialog(){

        final Dialog dialog = new Dialog(StoryBoard.this);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.summary_fragment);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        final FButton submitButton = (FButton)dialog.findViewById(R.id.submitButton);
        submitButton.setEnabled(false);

        final EditText editTextSummary = (EditText)dialog.findViewById(R.id.editTextSummary);

        //find current value of summary
        summary = "" + displaySummary.getText().toString();
        //if it's not default & not empty edit editTextSummary
        if(!summary.equals(getResources().getString(R.string.summary_prompt)) && (!summary.equals(""))){
            editTextSummary.setText(summary);
            submitButton.setEnabled(true);
        }else{
            summary = "";
        }

        editTextSummary.addTextChangedListener(new TextWatcher(){
            public void afterTextChanged(Editable s) {
                String newSummary = "" + editTextSummary.getText().toString();
                if(newSummary.length()>0){
                    summary = newSummary;
                    submitButton.setEnabled(true);
                }else{
                    summary = "";
                    submitButton.setEnabled(false);
                }
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            public void onTextChanged(CharSequence s, int start, int before, int count){}
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
                if (summary.trim().length() > 0) {
                    displaySummary.setText(summary);
                } else {
                    displaySummary.setText(getResources().getString(R.string.summary_prompt));
                }
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}
