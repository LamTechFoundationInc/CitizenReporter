package org.wordpress.android.ui.posts;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.andexert.expandablelayout.library.ExpandableLayoutListView;
import com.daimajia.slider.library.Animations.DescriptionAnimation;
import com.daimajia.slider.library.SliderLayout;
import com.daimajia.slider.library.SliderTypes.BaseSliderView;
import com.daimajia.slider.library.SliderTypes.TextSliderView;
import com.daimajia.slider.library.Tricks.ViewPagerEx;

import org.wordpress.android.R;
import org.wordpress.android.ui.posts.adapters.GuideArrayAdapter;

import info.hoang8f.widget.FButton;


import java.util.HashMap;

public class StoryBoard extends ActionBarActivity implements BaseSliderView.OnSliderClickListener, ViewPagerEx.OnPageChangeListener{

    private SliderLayout mDemoSlider;

    private LinearLayout summaryPane;
    private LinearLayout guidePane;

    private TextView displaySummary;
    private EditText editTextSummary;

    private String summary="";
    private RelativeLayout yesMedia;
    private RelativeLayout noMedia;

    private TextView text_summary;
    private TextView text_template;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.story_board);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mDemoSlider = (SliderLayout)findViewById(R.id.slider);

        summaryPane = (LinearLayout)findViewById(R.id.summaryPane);
        guidePane = (LinearLayout)findViewById(R.id.guidePane);
        displaySummary = (TextView)findViewById(R.id.displaySummary);
        editTextSummary = (EditText)findViewById(R.id.editTextSummary);

        yesMedia = (RelativeLayout)findViewById(R.id.yesMediaPane);
        noMedia = (RelativeLayout)findViewById(R.id.noMediaPane);

        text_summary = (TextView)findViewById(R.id.text_summary);
        text_template= (TextView)findViewById(R.id.text_template);

        text_summary.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                boolean showTemplate = false;
                togglePanes(showTemplate);
            }
        });
        text_template.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                boolean showTemplate = true;
                togglePanes(showTemplate);
            }
        });

        //on click summary pane, show pop up dialog for post section
        summaryPane.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                showCreateSummaryDialog();
            }
        });

        setUpSlider();

        setUpQuestionnaire();
    }

    public void setUpQuestionnaire(){

        Question questions[] = new Question[]
                {
                        new Question("What happened?", ""),
                        new Question("Why did it happen?", ""),
                        new Question("How did it happen?", ""),
                        new Question("Where did it happen?", ""),
                        new Question("Who was involved?", "")
                };

        GuideArrayAdapter arrayAdapter = new GuideArrayAdapter(this,
                R.layout.view_row, questions);

        String[] titles = {"What Happened?", "Why did it happen?", "How did it happen?", "Where did it happen?", "Who was involved?"};

        ExpandableLayoutListView expandableLayoutListView = (ExpandableLayoutListView) findViewById(R.id.guideListview);
        expandableLayoutListView.setAdapter(arrayAdapter);
    }

    public void togglePanes(boolean showTemplate){
        LinearLayout.LayoutParams activeParam = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        activeParam.setMargins(3, 3, 3, 3);

        LinearLayout.LayoutParams inActiveParam = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        inActiveParam.setMargins(5, 5, 5, 5);

        if (showTemplate){
            summaryPane.setVisibility(View.GONE);
            guidePane.setVisibility(View.VISIBLE);

            text_summary.setTextColor(getResources().getColor(R.color.reader_hyperlink));
            text_template.setTextColor(getResources().getColor(R.color.black));

            text_summary.setLayoutParams(inActiveParam);
            text_template.setLayoutParams(activeParam);

            text_summary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            text_template.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);

            text_template.setBackgroundColor(getResources().getColor(R.color.grey_lighten_10));
            text_summary.setBackgroundColor(getResources().getColor(R.color.grey_lighten_20));

        }else{
            summaryPane.setVisibility(View.VISIBLE);
            guidePane.setVisibility(View.GONE);

            text_summary.setTextColor(getResources().getColor(R.color.black));
            text_template.setTextColor(getResources().getColor(R.color.reader_hyperlink));

            text_summary.setLayoutParams(activeParam);
            text_template.setLayoutParams(inActiveParam);

            text_template.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            text_summary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);

            text_summary.setBackgroundColor(getResources().getColor(R.color.grey_lighten_10));
            text_template.setBackgroundColor(getResources().getColor(R.color.grey_lighten_20));
        }
    }

    public void setUpSlider(){
        HashMap<String,String> file_maps = new HashMap<String, String>();
        file_maps.put("Hannibal", "http://static2.hypable.com/wp-content/uploads/2013/12/hannibal-season-2-release-date.jpg");
        file_maps.put("Big Bang Theory", "http://tvfiles.alphacoders.com/100/hdclearart-10.png");
        file_maps.put("House of Cards", "http://cdn3.nflximg.net/images/3093/2043093.jpg");
        file_maps.put("Game of Thrones", "http://images.boomsbeat.com/data/images/full/19640/game-of-thrones-season-4-jpg.jpg");
        for(String name : file_maps.keySet()){
            TextSliderView textSliderView = new TextSliderView(this);
            // initialize a SliderLayout
            textSliderView
                    .description(name)
                    .image(file_maps.get(name))
                    .setScaleType(BaseSliderView.ScaleType.Fit)
                    .setOnSliderClickListener(this);

            //add your extra information
            textSliderView.bundle(new Bundle());
            textSliderView.getBundle()
                    .putString("extra",name);

            mDemoSlider.addSlider(textSliderView);
        }
        mDemoSlider.setPresetTransformer(SliderLayout.Transformer.Accordion);
        mDemoSlider.setPresetIndicator(SliderLayout.PresetIndicators.Center_Bottom);
        mDemoSlider.setCustomAnimation(new DescriptionAnimation());
        mDemoSlider.setDuration(4000);
        mDemoSlider.addOnPageChangeListener(this);

        toggleMediaPane(true);
    }

    public void toggleMediaPane(boolean hasMedia){
        if(hasMedia){
            noMedia.setVisibility(View.GONE);
            yesMedia.setVisibility(View.VISIBLE);
        }else{
            yesMedia.setVisibility(View.GONE);
            noMedia.setVisibility(View.VISIBLE);
        }
    }

    public void showCreateSummaryDialog(){

        final Dialog dialog = new Dialog(StoryBoard.this);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
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

        editTextSummary.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                String newSummary = "" + editTextSummary.getText().toString();
                if (newSummary.length() > 0) {
                    summary = newSummary;
                    submitButton.setEnabled(true);
                } else {
                    summary = "";
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
    @Override
    protected void onStop() {
        // To prevent a memory leak on rotation, make sure to call stopAutoCycle() on the slider before activity or fragment is destroyed
        mDemoSlider.stopAutoCycle();
        super.onStop();
    }

    @Override
    public void onSliderClick(BaseSliderView slider) {
        Toast.makeText(this,slider.getBundle().get("extra") + "",Toast.LENGTH_SHORT).show();
    }
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

    @Override
    public void onPageSelected(int position) {
        Log.d("Slider Demo", "Page Changed: " + position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {}


}
