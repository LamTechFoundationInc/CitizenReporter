package org.wordpress.android.ui.posts;

import android.app.Activity;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Location;
import android.location.LocationManager;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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

import net.micode.soundrecorder.SoundRecorder;

import org.json.JSONArray;
import org.json.JSONException;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.editor.EditorFragmentAbstract;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.PostLocation;
import org.wordpress.android.models.PostStatus;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.main.RipotiMainActivity;
import org.wordpress.android.ui.media.MediaGalleryActivity;
import org.wordpress.android.ui.media.MediaGalleryPickerActivity;
import org.wordpress.android.ui.media.MediaPickerActivity;
import org.wordpress.android.ui.media.WordPressMediaUtils;
import org.wordpress.android.ui.media.services.MediaUploadService;
import org.wordpress.android.ui.posts.adapters.GuideArrayAdapter;
import org.wordpress.android.ui.suggestion.adapters.TagSuggestionAdapter;
import org.wordpress.android.ui.suggestion.util.SuggestionServiceConnectionManager;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.GeocoderUtils;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.helpers.LocationHelper;
import org.wordpress.android.util.helpers.MediaFile;
import org.wordpress.android.widgets.SuggestionAutoCompleteText;
import org.wordpress.android.widgets.WPAlertDialogFragment;
import org.wordpress.android.widgets.WPViewPager;
import org.wordpress.passcodelock.AppLockManager;

import info.hoang8f.widget.FButton;


import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;

public class StoryBoard extends ActionBarActivity implements BaseSliderView.OnSliderClickListener,
        ViewPagerEx.OnPageChangeListener,
        View.OnClickListener, TextView.OnEditorActionListener {

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

    private PostLocation mPostLocation;
    private LocationHelper mLocationHelper;

    private FButton enableLocation;

    private Dialog summaryDialog;

    public static final String EXTRA_POSTID = "selectedId";
    public static final String EXTRA_IS_PAGE = "isPage";
    public static final String EXTRA_IS_NEW_POST = "isNewPost";
    public static final String EXTRA_IS_QUICKPRESS = "isQuickPress";
    public static final String EXTRA_QUICKPRESS_BLOG_ID = "quickPressBlogId";
    public static final String EXTRA_SAVED_AS_LOCAL_DRAFT = "savedAsLocalDraft";
    public static final String EXTRA_SHOULD_REFRESH = "shouldRefresh";
    public static final String STATE_KEY_CURRENT_POST = "stateKeyCurrentPost";
    public static final String STATE_KEY_ORIGINAL_POST = "stateKeyOriginalPost";
    private Post mOriginalPost;
    private boolean mIsNewPost;
    private boolean mIsPage;
    private ImageView button_camera;
    private ImageView button_video;
    private ImageView button_mic;
    private String mMediaCapturePath = "";

    private HashMap<String,File> media_map;
    private boolean hasMedia = false;
    
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.locationText) {
            viewLocation();
        } else if (id == R.id.updateLocation) {
            showLocationSearch();
        } else if (id == R.id.removeLocation) {
            removeLocation();
            showLocationAdd();
        } else if (id == R.id.addLocation) {
            showLocationSearch();
        } else if (id == R.id.searchLocation) {
            searchLocation();
        }
    }

    @Override
    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        return false;
    }

    private static enum LocationStatus {NONE, FOUND, NOT_FOUND, SEARCHING}

    private Post mPost;
    public static final String NEW_MEDIA_GALLERY = "NEW_MEDIA_GALLERY";
    public static final String NEW_MEDIA_POST = "NEW_MEDIA_POST";
    private boolean mMediaUploadServiceStarted;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.story_board);
        //get post
        long selectedId = getIntent().getLongExtra("selectedId", 0);
        Bundle extras = getIntent().getExtras();
        String action = getIntent().getAction();
        if (savedInstanceState == null) {
            if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)
                    || NEW_MEDIA_GALLERY.equals(action)
                    || NEW_MEDIA_POST.equals(action)
                    || getIntent().hasExtra(EXTRA_IS_QUICKPRESS)
                    || (extras != null && extras.getInt("quick-media", -1) > -1)) {
                if (getIntent().hasExtra(EXTRA_QUICKPRESS_BLOG_ID)) {
                    // QuickPress might want to use a different blog than the current blog
                    int blogId = getIntent().getIntExtra(EXTRA_QUICKPRESS_BLOG_ID, -1);
                    Blog quickPressBlog = WordPress.wpDB.instantiateBlogByLocalId(blogId);
                    if (quickPressBlog == null) {
                        showErrorAndFinish(R.string.blog_not_found);
                        return;
                    }
                    if (quickPressBlog.isHidden()) {
                        showErrorAndFinish(R.string.error_blog_hidden);
                        return;
                    }
                    WordPress.currentBlog = quickPressBlog;
                }

                // Create a new post for share intents and QuickPress
                mPost = new Post(WordPress.getCurrentLocalTableBlogId(), false);
                WordPress.wpDB.savePost(mPost);
                mIsNewPost = true;

                Log.d("cpath", "1");
            } else if (extras != null) {
                // Load post from the postId passed in extras
                long localTablePostId = extras.getLong(EXTRA_POSTID, -1);
                mIsPage = extras.getBoolean(EXTRA_IS_PAGE);
                mIsNewPost = extras.getBoolean(EXTRA_IS_NEW_POST);
                mPost = WordPress.wpDB.getPostForLocalTablePostId(localTablePostId, false);
                mOriginalPost = WordPress.wpDB.getPostForLocalTablePostId(localTablePostId, false);
                Log.d("cpath", "2");
            } else {
                Log.d("cpath", "3");
                // A postId extra must be passed to this activity
                showErrorAndFinish(R.string.post_not_found);
                return;
            }
        } else {
            Log.d("cpath", "4");
            if (savedInstanceState.containsKey(STATE_KEY_ORIGINAL_POST)) {
                Log.d("cpath", "5");
                try {
                    mPost = (Post) savedInstanceState.getSerializable(STATE_KEY_CURRENT_POST);
                    mOriginalPost = (Post) savedInstanceState.getSerializable(STATE_KEY_ORIGINAL_POST);
                    Log.d("cpath", "6");
                } catch (ClassCastException e) {
                    Log.d("cpath", "7");
                    mPost = null;
                }
            }
        }

        //check for quick capture actions
        int quickMediaType = getIntent().getIntExtra("quick-media", -1);

        if (quickMediaType >= 0) {
            // User selected 'Quick Photo' in the menu drawer
            if (quickMediaType == Constants.QUICK_POST_PHOTO_CAMERA) {
                launchCamera();
            } else if(quickMediaType == Constants.QUICK_POST_VIDEO_CAMERA) {
                launchVideoCamera();
            }else if(quickMediaType == Constants.QUICK_POST_AUDIO_MIC) {
                launchMic();
            }else if (quickMediaType == Constants.QUICK_POST_PHOTO_LIBRARY) {
                WordPressMediaUtils.launchPictureLibrary(this);
            }
            if (mPost != null) {
                mPost.setQuickPostType(Post.QUICK_MEDIA_TYPE_PHOTO);
            }
        }

        //is this from assignment
        int assignmentID = getIntent().getIntExtra("assignment_id", 0);
        if(assignmentID != 0){
            mPost.setAssignment_id(assignmentID);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setTitle(getResources().getString(R.string.story_board));

        mDemoSlider = (SliderLayout)findViewById(R.id.slider);
        media_map = new HashMap<String, File>();
        summaryPane = (LinearLayout)findViewById(R.id.summaryPane);
        guidePane = (LinearLayout)findViewById(R.id.guidePane);
        displaySummary = (TextView)findViewById(R.id.displaySummary);

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

        //quick capture icons
        button_camera = (ImageView)findViewById(R.id.button_camera);
        button_video = (ImageView)findViewById(R.id.button_video);
        button_mic = (ImageView)findViewById(R.id.button_mic);
        //
        button_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchCamera();
            }
        });

        button_video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchVideoCamera();
            }
        });

        button_mic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                launchMic();

            }
        });


        //TODO: need for checking twice?
         if (mPost != null) {
            WordPress.currentPost = mPost;
            loadPost(WordPress.currentPost);
        }else{
             mPost = new Post(WordPress.getCurrentLocalTableBlogId(), false);
             WordPress.currentPost = mPost;
             WordPress.wpDB.savePost(mPost);
             mIsNewPost = true;
         }

        setUpSlider();

        setUpQuestionnaire();


    }

    private void launchMic(){

        Intent i = new Intent(getApplicationContext(), SoundRecorder.class);
        startActivity(i);
    }
    private void launchCamera() {
        WordPressMediaUtils.launchCamera(this, new WordPressMediaUtils.LaunchCameraCallback() {
            @Override
            public void onMediaCapturePathReady(String mediaCapturePath) {
                mMediaCapturePath = mediaCapturePath;
                AppLockManager.getInstance().setExtendedTimeout();
            }
        });
    }
    private void launchVideoCamera() {
        WordPressMediaUtils.launchVideoCamera(this);
        AppLockManager.getInstance().setExtendedTimeout();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null || ((requestCode == RequestCodes.TAKE_PHOTO ||
                requestCode == RequestCodes.TAKE_VIDEO))) {
            switch (requestCode) {
                case RequestCodes.TAKE_PHOTO:
                    if (resultCode == Activity.RESULT_OK) {
                        try {
                            File f = new File(mMediaCapturePath);
                            Uri capturedImageUri = Uri.fromFile(f);
                            if (!addMedia(capturedImageUri, 1)) {
                                ToastUtils.showToast(this, R.string.gallery_error, ToastUtils.Duration.SHORT);
                            }
                            //this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"
                             //       + Environment.getExternalStorageDirectory())));
                            AnalyticsTracker.track(AnalyticsTracker.Stat.EDITOR_ADDED_PHOTO_VIA_LOCAL_LIBRARY);
                        } catch (RuntimeException e) {
                            AppLog.e(AppLog.T.POSTS, e);
                        } catch (OutOfMemoryError e) {
                            AppLog.e(AppLog.T.POSTS, e);
                        }
                    } /*else if (TextUtils.isEmpty(mEditorFragment.getContent())) {
                        // TODO: check if it was mQuickMediaType > -1
                        // Quick Photo was cancelled, delete post and finish activity
                        WordPress.wpDB.deletePost(getPost());
                        finish();
                    }*/
                    break;
                case RequestCodes.TAKE_VIDEO:
                    if (resultCode == Activity.RESULT_OK) {
                        Uri capturedVideoUri = MediaUtils.getLastRecordedVideoUri(this);
                        if (!addMedia(capturedVideoUri, 2)) {
                            ToastUtils.showToast(this, R.string.gallery_error, ToastUtils.Duration.SHORT);
                        }
                    }/* else if (TextUtils.isEmpty(mEditorFragment.getContent())) {
                        // TODO: check if it was mQuickMediaType > -1
                        // Quick Photo was cancelled, delete post and finish activity
                        WordPress.wpDB.deletePost(getPost());
                        finish();
                    }*/
                    break;
            }
        }
    }
    private void queueFileForUpload(String path, int mediaType) {
        // Invalid file path
        if (TextUtils.isEmpty(path)) {
            Toast.makeText(this, R.string.editor_toast_invalid_path, Toast.LENGTH_SHORT).show();
            return;
        }

        // File not found
        File file = null;
        URI fUri = null;
        try {
            fUri = new URI(path);            
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        file = new File(fUri.getPath());

        path = fUri.getPath();

        if (!file.exists()) {
            Toast.makeText(this, R.string.file_not_found, Toast.LENGTH_SHORT).show();
            
        }else{

            Blog blog = WordPress.getCurrentBlog();
            long currentTime = System.currentTimeMillis();
            String mimeType = MediaUtils.getMediaFileMimeType(file);
            String fileName = MediaUtils.getMediaFileName(file, mimeType);
            MediaFile mediaFile = new MediaFile();

            mediaFile.setBlogId(String.valueOf(blog.getLocalTableBlogId()));
            mediaFile.setFileName(fileName);
            mediaFile.setFilePath(path);
            mediaFile.setUploadState("queued");
            mediaFile.setDateCreatedGMT(currentTime);
            mediaFile.setMediaId(String.valueOf(currentTime));

            if (mimeType != null && mimeType.startsWith("image")) {
                // get width and height
                BitmapFactory.Options bfo = new BitmapFactory.Options();
                bfo.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(path, bfo);
                mediaFile.setWidth(bfo.outWidth);
                mediaFile.setHeight(bfo.outHeight);
            }

            if (!TextUtils.isEmpty(mimeType)) {
                mediaFile.setMimeType(mimeType);
            }

            //Add thumbnail to slider and refresh

            //Generate Thumbnail
            String thumbnailURL = generateThumb(file, mimeType, currentTime);

            File thumb = new File(thumbnailURL);

            if(thumb.exists()){
                mediaFile.setThumbnailURL(thumbnailURL);

                //TODO: set caption on slider media_map.put(mPost.getTitle(), file);
                Random randomGenerator = new Random();
                media_map.put(String.valueOf(randomGenerator.nextInt(10000)), thumb);
                setUpSlider();
            }

            WordPress.wpDB.saveMediaFile(mediaFile);
            startMediaUploadService();
        }
    }

    /*
        Generate Thumbnail
     */
    public String generateThumb(File file, String mimeType, long mediaCreationTime){
        String thumbnailUri = "";
        //create thumbnails folder if not exists
        String app_name = getResources().getString(R.string.app_name);

        File mThumbsDir = new File(Environment.getExternalStorageDirectory(), app_name+"/thumbnails");
        if (!mThumbsDir.exists()) {
            if (!mThumbsDir.mkdirs()) {
                return thumbnailUri;
            }
        }

        //create thumb according to type
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String mt = dateFormat.format(mediaCreationTime);

        thumbnailUri = mThumbsDir.getAbsolutePath() + "/" + mt+".jpg";

        File thisThumb = new File(thumbnailUri);

        //if thumbnail does not exist
        if(!thisThumb.exists()){

            //Create thumbnail
            Bitmap bitThumb = null;
            String filename=null;

            if(mimeType.startsWith("video")){
                bitThumb = ThumbnailUtils.createVideoThumbnail(file.getAbsolutePath(), MediaStore.Video.Thumbnails.MICRO_KIND);
            }else if(mimeType.startsWith("image")){
                bitThumb = BitmapFactory.decodeFile(file.getAbsolutePath());
            }

            try{
                filename = mThumbsDir.getAbsolutePath() + "/" + mt+".jpg";

                FileOutputStream out = new FileOutputStream(filename);
                bitThumb.compress(Bitmap.CompressFormat.JPEG, 30, out);
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        return thumbnailUri;
    }

    /**
     * Starts the upload service to upload selected media.
     */
    private void startMediaUploadService() {
        if (!mMediaUploadServiceStarted) {
            startService(new Intent(this, MediaUploadService.class));
            mMediaUploadServiceStarted = true;
        }
    }
    private boolean addMedia(Uri imageUri, int mediaType) {
        if (!MediaUtils.isInMediaStore(imageUri) && !imageUri.toString().startsWith("/")) {
            imageUri = MediaUtils.downloadExternalMedia(this, imageUri);
        }

        if (imageUri == null) {
            return false;
        }

        queueFileForUpload(imageUri.toString(), mediaType);

        //mEditorFragment.appendMediaFile(mediaFile, mediaFile.getFilePath(), WordPress.imageLoader);
        return true;
    }

    private void showErrorAndFinish(int errorMessageId) {
        Toast.makeText(this, getResources().getText(errorMessageId), Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            saveAndFinish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private boolean hasEmptyContentFields() {
        return TextUtils.isEmpty(displaySummary.getText().toString());
    }
    private void saveAndFinish() {
        savePost(true);
        if (hasEmptyContentFields()) {
            // new and empty post? delete it
            if (mIsNewPost) {
                WordPress.wpDB.deletePost(mPost);
            }
        } else if (mOriginalPost != null && !mPost.hasChanges(mOriginalPost)) {
            // if no changes have been made to the post, set it back to the original don't save it
            WordPress.wpDB.updatePost(mOriginalPost);
            WordPress.currentPost = mOriginalPost;
        } else {
            // changes have been made, save the post and ask for the post list to refresh.
            // We consider this being "manual save", it will replace some Android "spans" by an html
            // or a shortcode replacement (for instance for images and galleries)
            savePost(true);
            WordPress.currentPost = mPost;
            /*Intent i = new Intent();
            i.putExtra(EXTRA_SHOULD_REFRESH, true);
            i.putExtra(EXTRA_SAVED_AS_LOCAL_DRAFT, false);
            i.putExtra(EXTRA_IS_PAGE, mIsPage);
            setResult(RESULT_OK, i);
*/


        }

        //ToastUtils.showToast(this, R.string.editor_toast_changes_saved);
        if (!mPost.isPublishable()) {
            ToastUtils.showToast(this, R.string.error_publish_empty_post, ToastUtils.Duration.SHORT);

        }else if (!NetworkUtils.isNetworkAvailable(this)) {
            ToastUtils.showToast(this, R.string.error_publish_no_network, ToastUtils.Duration.SHORT);

        }else{

            PostUploadService.addPostToUpload(mPost);
            startService(new Intent(this, PostUploadService.class));
            /*Intent i = new Intent();
            i.putExtra(EXTRA_SHOULD_REFRESH, true);
            setResult(RESULT_OK, i);
            */
            finish();
        }

    }
    private void savePost(boolean isAutosave) {
        savePost(isAutosave, true);
    }

    private void savePost(boolean isAutosave, boolean updatePost) {
        if (updatePost) {
            updatePostContent();
            updatePostSettings();
        }
        WordPress.wpDB.updatePost(mPost);
    }

    private void updatePostContent(){
        Post post = mPost;
        post.setTitle(mPost.getTitle());
        if (!post.isLocalDraft()) {
            post.setLocalChange(true);
        }
    }

    private void updatePostSettings(){
        String status = PostStatus.toString(PostStatus.DRAFT);
        mPost.setPostStatus(status);

        if (mPost.supportsLocation()) {
            mPost.setLocation(mPostLocation);
        }

        //mPost.setKeywords(tags);

        mPost.setPostStatus(status);
    }

    public void loadPost(Post p){
        //setTitle(p.getTitle());
        if(p != null){
            if(!p.getTitle().equals(""))
                displaySummary.setText("" + p.getTitle());

        }
    }

    public void setUpQuestionnaire(){

        String[] myResArray = getResources().getStringArray(R.array.fivew_and_h);
        List<String> myResArrayList = Arrays.asList(myResArray);

        String tags = mPost.getKeywords();

        Question questions[] = new Question[]
                {
                        //new Question(myResArrayList.get(0), mPost.getQwhat()),
                        new Question(myResArrayList.get(1), mPost.getQwhy()),
                        new Question(myResArrayList.get(2), mPost.getKeywords()),
                        //new Question(myResArrayList.get(3), mPost.getQwhere()),
                        new Question(myResArrayList.get(4), mPost.getQwhen()),
                        new Question(myResArrayList.get(5), mPost.getQhow())
                };

        GuideArrayAdapter arrayAdapter = new GuideArrayAdapter(this,
                R.layout.view_row, questions, mPost);

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

        for(String name : media_map.keySet()){
            TextSliderView textSliderView = new TextSliderView(this);
            // initialize a SliderLayout
            textSliderView
                    .description(name)
                    .image(media_map.get(name))
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

        if(media_map.size()>0){
            hasMedia = true;
        }

        toggleMediaPane(hasMedia);
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

        summaryDialog = new Dialog(StoryBoard.this);
        summaryDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        summaryDialog.getWindow().setLayout(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT);
        summaryDialog.setContentView(R.layout.summary_fragment);
        summaryDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        final FButton submitButton = (FButton)summaryDialog.findViewById(R.id.submitButton);
        submitButton.setEnabled(false);

        final EditText editTextSummary = (EditText)summaryDialog.findViewById(R.id.editTextSummary);

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

        summaryDialog.findViewById(R.id.closeDialog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                summaryDialog.dismiss();
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (summary.trim().length() > 0) {
                    displaySummary.setText(summary);
                    if(!summary.equals(getResources().getString(R.string.summary_prompt))){
                        mPost.setTitle(summary);
                    }
                } else {
                    displaySummary.setText(getResources().getString(R.string.summary_prompt));
                }
                summaryDialog.dismiss();
            }
        });

        enableLocation = (FButton)summaryDialog.findViewById(R.id.enableLocation);
        enableLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent callGPSSettingIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(callGPSSettingIntent);
            }
        });

        initLocation();

        summaryDialog.show();
    }

    @Override
    protected void onResume(){
        super.onResume();

        //TODO: use onactivityresult?
        if(summaryDialog !=null) {
            if (summaryDialog.isShowing()) {
                //resuming from enable location settings?
                initLocation();
            }
        }
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



    /**
     * Location methods
     */

    /*
     * retrieves and displays the friendly address for a lat/long location
     */
    private class GetAddressTask extends AsyncTask<Double, Void, Address> {
        double latitude;
        double longitude;

        @Override
        protected void onPreExecute() {
            setLocationStatus(LocationStatus.SEARCHING);
            showLocationView();
        }

        @Override
        protected Address doInBackground(Double... args) {
            // args will be the latitude, longitude to look up
            latitude = args[0];
            longitude = args[1];

            return GeocoderUtils.getAddressFromCoords(StoryBoard.this, latitude, longitude);
        }

        protected void onPostExecute(Address address) {
            setLocationStatus(LocationStatus.FOUND);
            if (address == null) {
                // show lat/long when Geocoder fails (ugly, but better than not showing anything
                // or showing an error since the location has been assigned to the post already)
                updateLocationText(Double.toString(latitude) + ", " + Double.toString(longitude));
            } else {
                String locationName = GeocoderUtils.getLocationNameFromAddress(address);
                updateLocationText(locationName);
            }
        }
    }

    private class GetCoordsTask extends AsyncTask<String, Void, Address> {
        @Override
        protected void onPreExecute() {
            setLocationStatus(LocationStatus.SEARCHING);
            showLocationView();
        }

        @Override
        protected Address doInBackground(String... args) {
            String locationName = args[0];

            return GeocoderUtils.getAddressFromLocationName(StoryBoard.this, locationName);
        }

        @Override
        protected void onPostExecute(Address address) {
            setLocationStatus(LocationStatus.FOUND);
            showLocationView();

            if (address != null) {
                double[] coordinates = GeocoderUtils.getCoordsFromAddress(address);
                setLocation(coordinates[0], coordinates[1]);

                String locationName = GeocoderUtils.getLocationNameFromAddress(address);
                updateLocationText(locationName);
            } else {
                showLocationNotAvailableError();
                showLocationSearch();
            }
        }
    }

    private LocationHelper.LocationResult locationResult = new LocationHelper.LocationResult() {
        @Override
        public void gotLocation(final Location location) {
            if (StoryBoard.this == null)
                return;
            // note that location will be null when requesting location fails
            StoryBoard.this.runOnUiThread(new Runnable() {
                public void run() {
                    setLocation(location);
                }
            });
        }
    };

    private View mLocationAddSection;
    private View mLocationSearchSection;
    private View mLocationViewSection;
    private TextView mLocationText;
    private EditText mLocationEditText;
    private FButton mButtonSearchLocation;

    private TextWatcher mLocationEditTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }

        @Override
        public void afterTextChanged(Editable s) {
            String buttonText;
            if (s.length() > 0) {
                buttonText = getResources().getString(R.string.search_location);
            } else {
                buttonText = getResources().getString(R.string.search_current_location);
            }
            mButtonSearchLocation.setText(buttonText);
        }
    };
    /*
     * called when activity is created to initialize the location provider, show views related
     * to location if enabled for this blog, and retrieve the current location if necessary
     */
    private void initLocation() {
        // show the location views if a provider was found and this is a post on a blog that has location enabled
        if (hasLocationProvider() && mPost.supportsLocation()) {
            enableLocation.setVisibility(View.GONE);

            View locationRootView = ((ViewStub) summaryDialog.findViewById(R.id.stub_post_location_settings)).inflate();

            mLocationText = (TextView) locationRootView.findViewById(R.id.locationText);
            mLocationText.setOnClickListener(this);

            mLocationAddSection = locationRootView.findViewById(R.id.sectionLocationAdd);
            mLocationSearchSection = locationRootView.findViewById(R.id.sectionLocationSearch);
            mLocationViewSection = locationRootView.findViewById(R.id.sectionLocationView);

            FButton addLocation = (FButton) locationRootView.findViewById(R.id.addLocation);
            addLocation.setOnClickListener(this);

            mButtonSearchLocation = (FButton) locationRootView.findViewById(R.id.searchLocation);
            mButtonSearchLocation.setOnClickListener(this);

            mLocationEditText = (EditText) locationRootView.findViewById(R.id.searchLocationText);
            mLocationEditText.setOnEditorActionListener(this);
            mLocationEditText.addTextChangedListener(mLocationEditTextWatcher);

            Button updateLocation = (FButton) locationRootView.findViewById(R.id.updateLocation);
            Button removeLocation = (FButton) locationRootView.findViewById(R.id.removeLocation);
            updateLocation.setOnClickListener(this);
            removeLocation.setOnClickListener(this);

            // if this post has location attached to it, look up the location address
            if (mPost.hasLocation()) {
                showLocationView();

                PostLocation location = mPost.getLocation();
                setLocation(location.getLatitude(), location.getLongitude());
            } else {
                showLocationAdd();
            }
        }else{
            enableLocation.setVisibility(View.VISIBLE);
        }
    }

    private boolean hasLocationProvider() {
        boolean hasLocationProvider = false;
        LocationManager locationManager = (LocationManager) StoryBoard.this.getSystemService(Activity.LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        if (providers != null) {
            for (String providerName : providers) {
                if (providerName.equals(LocationManager.GPS_PROVIDER)
                        || providerName.equals(LocationManager.NETWORK_PROVIDER)) {
                    hasLocationProvider = true;
                }
            }
        }
        return hasLocationProvider;
    }

    private void showLocationSearch() {
        mLocationAddSection.setVisibility(View.GONE);
        mLocationSearchSection.setVisibility(View.VISIBLE);
        mLocationViewSection.setVisibility(View.GONE);

        EditTextUtils.showSoftInput(mLocationEditText);
    }

    private void showLocationAdd() {
        mLocationAddSection.setVisibility(View.VISIBLE);
        mLocationSearchSection.setVisibility(View.GONE);
        mLocationViewSection.setVisibility(View.GONE);
    }

    private void showLocationView() {
        mLocationAddSection.setVisibility(View.GONE);
        mLocationSearchSection.setVisibility(View.GONE);
        mLocationViewSection.setVisibility(View.VISIBLE);
    }

    private void searchLocation() {
        EditTextUtils.hideSoftInput(mLocationEditText);
        String location = EditTextUtils.getText(mLocationEditText);

        removeLocation();

        if (location.isEmpty()) {
            fetchCurrentLocation();
        } else {
            new GetCoordsTask().execute(location);
        }
    }

    /*
     * get the current location
     */
    private void fetchCurrentLocation() {
        if (mLocationHelper == null) {
            mLocationHelper = new LocationHelper();
        }
        boolean canGetLocation = mLocationHelper.getLocation(StoryBoard.this, locationResult);

        if (canGetLocation) {
            setLocationStatus(LocationStatus.SEARCHING);
            showLocationView();
        } else {
            setLocation(null);
            showLocationNotAvailableError();
            showLocationAdd();
        }
    }

    /*
     * called when location is retrieved/updated for this post - looks up the address to
     * display for the lat/long
     */
    private void setLocation(Location location) {
        if (location != null) {
            setLocation(location.getLatitude(), location.getLongitude());
        } else {
            updateLocationText(getString(R.string.location_not_found));
            setLocationStatus(LocationStatus.NOT_FOUND);
        }
    }

    private void setLocation(double latitude, double longitude) {
        mPostLocation = new PostLocation(latitude, longitude);
        new GetAddressTask().execute(mPostLocation.getLatitude(), mPostLocation.getLongitude());
    }

    private void removeLocation() {
        mPostLocation = null;
        mPost.unsetLocation();

        updateLocationText("");
        setLocationStatus(LocationStatus.NONE);
    }

    private void viewLocation() {
        if (mPostLocation != null && mPostLocation.isValid()) {
            String uri = "geo:" + mPostLocation.getLatitude() + "," + mPostLocation.getLongitude();
            startActivity(new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(uri)));
        } else {
            showLocationNotAvailableError();
            showLocationAdd();
        }
    }

    private void showLocationNotAvailableError() {
        Toast.makeText(StoryBoard.this, getResources().getText(R.string.location_not_found), Toast.LENGTH_SHORT).show();
    }

    private void updateLocationText(String locationName) {
        mLocationText.setText(locationName);
    }

    /*
     * changes the left drawable on the location text to match the passed status
     */
    private void setLocationStatus(LocationStatus status) {


        // animate location text when searching
        if (status == LocationStatus.SEARCHING) {
            updateLocationText(getString(R.string.loading));

            Animation aniBlink = AnimationUtils.loadAnimation(StoryBoard.this, R.anim.blink);
            if (aniBlink != null) {
                mLocationText.startAnimation(aniBlink);
            }
        } else {
            mLocationText.clearAnimation();
        }

        final int drawableId;
        switch (status) {
            case FOUND:
                drawableId = R.drawable.ic_action_location_found;
                break;
            case NOT_FOUND:
                drawableId = R.drawable.ic_action_location_off;
                break;
            case SEARCHING:
                drawableId = R.drawable.ic_action_location_searching;
                break;
            case NONE:
                drawableId = 0;
                break;
            default:
                return;
        }

        mLocationText.setCompoundDrawablesWithIntrinsicBounds(drawableId, 0, 0, 0);
    }

}
