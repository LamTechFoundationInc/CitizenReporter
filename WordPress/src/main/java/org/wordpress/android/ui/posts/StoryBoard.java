package org.wordpress.android.ui.posts;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;

/**
 * Created by john on 7/12/2015.
 */
public class StoryBoard extends ActionBarActivity {

    private LinearLayout summaryPane;

    private TextView displaySummary;
    private EditText editTextSummary;
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
        dialog.setContentView(R.layout.summary_fragment);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        dialog.findViewById(R.id.closeDialog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}
