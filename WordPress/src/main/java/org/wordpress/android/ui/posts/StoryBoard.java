package org.wordpress.android.ui.posts;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.LinearLayout;

import org.wordpress.android.R;

/**
 * Created by john on 7/12/2015.
 */
public class StoryBoard extends ActionBarActivity {

    private LinearLayout summaryPane;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.story_board);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        summaryPane = (LinearLayout)findViewById(R.id.summaryPane);

        //on click summary pane, show pop up dialog for post section
        summaryPane.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                showCreateSummaryDialog();
            }
        });
    }

    public void showCreateSummaryDialog(){
        SummaryFragment dFragment = new SummaryFragment();
        // Show DialogFragment
        FragmentManager fm = getSupportFragmentManager();
        dFragment.show(fm, "Dialog Fragment");
    }
}
