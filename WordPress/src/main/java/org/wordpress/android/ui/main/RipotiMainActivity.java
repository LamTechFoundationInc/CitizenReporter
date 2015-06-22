package org.wordpress.android.ui.main;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;

import org.wordpress.android.GCMIntentService;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.models.CommentStatus;
import org.wordpress.android.models.Note;
import org.wordpress.android.models.Post;
import org.wordpress.android.networking.SelfSignedSSLCertsManager;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.media.MediaAddFragment;
import org.wordpress.android.ui.notifications.NotificationEvents;
import org.wordpress.android.ui.notifications.NotificationsListFragment;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.ui.notifications.utils.SimperiumUtils;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.ui.posts.RipotiPostsListFragment;
import org.wordpress.android.ui.posts.ViewPostFragment;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.prefs.BlogPreferencesActivity;
import org.wordpress.android.ui.reader.ReaderEvents;
import org.wordpress.android.ui.reader.ReaderPostListFragment;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.AuthenticationDialogUtils;
import org.wordpress.android.util.CoreEvents;
import org.wordpress.android.util.CoreEvents.MainViewPagerScrolled;
import org.wordpress.android.util.CoreEvents.UserSignedOutCompletely;
import org.wordpress.android.util.CoreEvents.UserSignedOutWordPressCom;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.SlidingTabLayout;
import org.wordpress.android.widgets.WPAlertDialogFragment;
import org.wordpress.android.widgets.WPMainViewPager;

import de.greenrobot.event.EventBus;

import org.wordpress.android.ui.posts.RipotiPostsListFragment.OnPostActionListener;
import org.wordpress.android.ui.posts.RipotiPostsListFragment.OnPostSelectedListener;
import org.wordpress.android.ui.posts.ViewPostFragment.OnDetailPostActionListener;

/**
 * Main activity which hosts sites, reader, me and notifications tabs
 */

public class RipotiMainActivity extends ActionBarActivity
        implements ViewPager.OnPageChangeListener,
        SlidingTabLayout.SingleTabClickListener,
        MediaAddFragment.MediaAddFragmentCallback,
        Bucket.Listener<Note>,OnPostSelectedListener, RipotiPostsListFragment.OnSinglePostLoadedListener, OnPostActionListener,
        OnDetailPostActionListener, WPAlertDialogFragment.OnDialogConfirmListener {
    private WPMainViewPager mViewPager;
    private SlidingTabLayout mTabs;
    private RipotiMainTabAdapter mTabAdapter;

    public static final String ARG_OPENED_FROM_PUSH = "opened_from_push";

    /*
    posts list variables
     */
    public static final String EXTRA_VIEW_PAGES = "viewPages";
    public static final String EXTRA_ERROR_MSG = "errorMessage";
    public static final String EXTRA_ERROR_INFO_TITLE = "errorInfoTitle";
    public static final String EXTRA_ERROR_INFO_LINK = "errorInfoLink";

    public static final int POST_DELETE = 0;
    public static final int POST_SHARE = 1;
    public static final int POST_EDIT = 2;
    private static final int POST_CLEAR = 3;
    public static final int POST_VIEW = 5;
    private static final int ID_DIALOG_DELETING = 1, ID_DIALOG_SHARE = 2;
    private ProgressDialog mLoadingDialog;
    private boolean mIsPage = false;
    private String mErrorMsg = "";

    public RipotiPostsListFragment mPostList;

    @Override
    public void onDetailPostAction(int action, Post post) {

    }

    @Override
    public void onDialogConfirm() {

    }

    @Override
    public void onPostAction(int action, Post post) {

    }

    @Override
    public void onPostSelected(Post post) {

    }

    @Override
    public void onSinglePostLoaded() {

    }
    private void popPostDetail() {

    }
    public boolean isDualPane() {
        return true;
    }
    public void newPost() {
        if (WordPress.getCurrentBlog() == null) {
            if (!isFinishing())
                Toast.makeText(this, R.string.blog_not_found, Toast.LENGTH_SHORT).show();
            return;
        }
        // Create a new post object
        Post newPost = new Post(WordPress.getCurrentBlog().getLocalTableBlogId(), mIsPage);
        WordPress.wpDB.savePost(newPost);
        Intent i = new Intent(this, EditPostActivity.class);
        i.putExtra(EditPostActivity.EXTRA_POSTID, newPost.getLocalTablePostId());
        i.putExtra(EditPostActivity.EXTRA_IS_PAGE, mIsPage);
        i.putExtra(EditPostActivity.EXTRA_IS_NEW_POST, true);
        startActivityForResult(i, RequestCodes.EDIT_POST);
    }
    public void requestPosts() {
        if (WordPress.getCurrentBlog() == null) {
            return;
        }
        // If user has local changes, don't refresh
        if (!WordPress.wpDB.findLocalChanges(WordPress.getCurrentBlog().getLocalTableBlogId(), mIsPage)) {
            popPostDetail();
            mPostList.requestPosts(false);
            mPostList.setRefreshing(true);
        }
    }


    /*
     * tab fragments implement this if their contents can be scrolled, called when user
     * requests to scroll to the top
     */
    public interface OnScrollToTopListener {
        void onScrollToTop();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setStatusBarColor();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.ripoti_main_screen);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //toolbar.inflateMenu(R.menu.home_menu);
        toolbar.setNavigationIcon(R.drawable.dashicon_lock);

        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mViewPager = (WPMainViewPager) findViewById(R.id.viewpager_main);
        mTabAdapter = new RipotiMainTabAdapter(getFragmentManager(), RipotiMainActivity.this);
        mViewPager.setAdapter(mTabAdapter);

        mTabs = (SlidingTabLayout) findViewById(R.id.sliding_tabs);
        mTabs.setSelectedIndicatorColors(getResources().getColor(R.color.tab_indicator));

        // tabs are left-aligned rather than evenly distributed in landscape
        mTabs.setDistributeEvenly(!DisplayUtils.isLandscape(this));

        Integer icons[] = {R.drawable.main_tab_sites,R.drawable.main_tab_reader};
        Integer titles[] = {R.string.assignments, R.string.my_posts};

        // content descriptions
        mTabs.setContentDescription(RipotiMainTabAdapter.TAB_ASSIGNMENTS, getString(R.string.assignments));
        mTabs.setContentDescription(RipotiMainTabAdapter.TAB_MYPOSTS, getString(R.string.my_posts));

        mTabs.setCustomTabView(R.layout.tab_text, R.id.text_tab, titles);

        mTabs.setViewPager(mViewPager);
        mTabs.setOnSingleTabClickListener(this);

        // page change listener must be set on the tab layout rather than the ViewPager
        mTabs.setOnPageChangeListener(this);

        if (savedInstanceState == null) {
            if (AccountHelper.isSignedIn()) {
                // open note detail if activity called from a push, otherwise return to the tab
                // that was showing last time
                boolean openedFromPush = (getIntent() != null && getIntent().getBooleanExtra(ARG_OPENED_FROM_PUSH,
                        false));
                if (openedFromPush) {
                    getIntent().putExtra(ARG_OPENED_FROM_PUSH, false);
                    launchWithNoteId();
                } else {
                    int position = AppPrefs.getMainTabIndex();
                    if (mTabAdapter.isValidPosition(position) && position != mViewPager.getCurrentItem()) {
                        mViewPager.setCurrentItem(position);
                    }
                }
            } else {
                ActivityLauncher.showSignInForResult(this);
            }
        }

        FragmentManager fm = getFragmentManager();
        mPostList = (RipotiPostsListFragment) fm.findFragmentById(R.id.postList);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mIsPage = extras.getBoolean(EXTRA_VIEW_PAGES);
            //showErrorDialogIfNeeded(extras);
        }

        WordPress.currentPost = null;

        if (savedInstanceState != null) {
            popPostDetail();
        }

        //attemptToSelectPost();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu, menu);
        return true;
    }
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.status_bar_tint));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        AppLog.i(T.MAIN, "main activity > new intent");
        if (intent.hasExtra(NotificationsListFragment.NOTE_ID_EXTRA)) {
            launchWithNoteId();
        }
    }

    /*
     * called when app is launched from a push notification, switches to the notification tab
     * and opens the desired note detail
     */
    private void launchWithNoteId() {
        if (isFinishing() || getIntent() == null) return;

        // Check for push authorization request
        if (getIntent().hasExtra(NotificationsUtils.ARG_PUSH_AUTH_TOKEN)) {
            Bundle extras = getIntent().getExtras();
            String token = extras.getString(NotificationsUtils.ARG_PUSH_AUTH_TOKEN, "");
            String title = extras.getString(NotificationsUtils.ARG_PUSH_AUTH_TITLE, "");
            String message = extras.getString(NotificationsUtils.ARG_PUSH_AUTH_MESSAGE, "");
            long expires = extras.getLong(NotificationsUtils.ARG_PUSH_AUTH_EXPIRES, 0);

            long now = System.currentTimeMillis() / 1000;
            if (expires > 0 && now > expires) {
                // Show a toast if the user took too long to open the notification
                ToastUtils.showToast(this, R.string.push_auth_expired, ToastUtils.Duration.LONG);
                AnalyticsTracker.track(AnalyticsTracker.Stat.PUSH_AUTHENTICATION_EXPIRED);
            } else {
                NotificationsUtils.showPushAuthAlert(this, token, title, message);
            }
        }

        mViewPager.setCurrentItem(RipotiMainTabAdapter.TAB_ASSIGNMENTS);

        String noteId = getIntent().getStringExtra(NotificationsListFragment.NOTE_ID_EXTRA);
        boolean shouldShowKeyboard = getIntent().getBooleanExtra(NotificationsListFragment.NOTE_INSTANT_REPLY_EXTRA, false);

        if (!TextUtils.isEmpty(noteId)) {
            NotificationsListFragment.openNote(this, noteId, shouldShowKeyboard, false);
            GCMIntentService.clearNotificationsMap();
        }
    }

    @Override
    public void onPageSelected(int position) {
        // remember the index of this page
        AppPrefs.setMainTabIndex(position);

        switch (position) {/*
            case RipotiMainTabAdapter.TAB_NOTIFS:
                if (getNotificationListFragment() != null) {
                    getNotificationListFragment().updateLastSeenTime();
                    mTabs.setBadge(RipotiMainTabAdapter.TAB_NOTIFS, false);
                }
                break;*/
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        // noop
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        // fire event if the "My Site" page is being scrolled so the fragment can
        // animate its fab to match
        if (position == RipotiMainTabAdapter.TAB_MYPOSTS) {
            EventBus.getDefault().post(new MainViewPagerScrolled(positionOffset));
        }
    }

    /*
     * user tapped a tab above the viewPager - detect when the active tab is clicked and scroll
     * the fragment to the top if available
     */
    @Override
    public void onTabClick(View view, int position) {
        if (position == mViewPager.getCurrentItem()) {
            Fragment fragment = mTabAdapter.getFragment(position);
            if (fragment instanceof OnScrollToTopListener) {
                ((OnScrollToTopListener) fragment).onScrollToTop();
            }
        }
    }

    @Override
    protected void onPause() {
        if (SimperiumUtils.getNotesBucket() != null) {
            SimperiumUtils.getNotesBucket().removeListener(this);
        }

        super.onPause();
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Start listening to Simperium Note bucket
        if (SimperiumUtils.getNotesBucket() != null) {
            SimperiumUtils.getNotesBucket().addListener(this);
        }
        checkNoteBadge();
    }

    /*
     * re-create the fragment adapter so all its fragments are also re-created - used when
     * user signs in/out so the fragments reflect the active account
     */
    private void resetFragments() {
        AppLog.i(T.MAIN, "main activity > reset fragments");

        // remove the event that determines when followed tags/blogs are updated so they're
        // updated when the fragment is recreated (necessary after signin/disconnect)
        EventBus.getDefault().removeStickyEvent(ReaderEvents.UpdatedFollowedTagsAndBlogs.class);

        // remember the current tab position, then recreate the adapter so new fragments are created
        int position = mViewPager.getCurrentItem();
        mTabAdapter = new RipotiMainTabAdapter(getFragmentManager(), RipotiMainActivity.this);
        mViewPager.setAdapter(mTabAdapter);

        // restore previous position
        if (mTabAdapter.isValidPosition(position)) {
            mViewPager.setCurrentItem(position);
        }
    }

    private void moderateCommentOnActivityResult(Intent data) {
        try {
            if (SimperiumUtils.getNotesBucket() != null) {
                Note note = SimperiumUtils.getNotesBucket().get(StringUtils.notNullStr(data.getStringExtra
                        (NotificationsListFragment.NOTE_MODERATE_ID_EXTRA)));
                CommentStatus status = CommentStatus.fromString(data.getStringExtra(
                        NotificationsListFragment.NOTE_MODERATE_STATUS_EXTRA));
                NotificationsUtils.moderateCommentForNote(note, status, this);
            }
        } catch (BucketObjectMissingException e) {
            AppLog.e(T.NOTIFS, e);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RequestCodes.EDIT_POST:
                if (resultCode == RESULT_OK) {
                    MySiteFragment mySiteFragment = getMySiteFragment();
                    if (mySiteFragment != null) {
                        mySiteFragment.onActivityResult(requestCode, resultCode, data);
                    }
                }
                break;
            case RequestCodes.READER_SUBS:
            case RequestCodes.READER_REBLOG:
                ReaderPostListFragment readerFragment = getReaderListFragment();
                if (readerFragment != null) {
                    readerFragment.onActivityResult(requestCode, resultCode, data);
                }
                break;
            case RequestCodes.ADD_ACCOUNT:
                if (resultCode == RESULT_OK) {
                    WordPress.registerForCloudMessaging(this);
                    resetFragments();
                } else if (!AccountHelper.isSignedIn()) {
                    // can't do anything if user isn't signed in (either to wp.com or self-hosted)
                    finish();
                }
                break;
            case RequestCodes.REAUTHENTICATE:
                if (resultCode == RESULT_CANCELED) {
                    ActivityLauncher.showSignInForResult(this);
                } else {
                    WordPress.registerForCloudMessaging(this);
                }
                break;
            case RequestCodes.NOTE_DETAIL:
                if (resultCode == RESULT_OK && data != null) {
                    moderateCommentOnActivityResult(data);
                }
                break;
            case RequestCodes.PICTURE_LIBRARY:
                FragmentManager fm = getFragmentManager();
                Fragment addFragment = fm.findFragmentByTag(MySiteFragment.ADD_MEDIA_FRAGMENT_TAG);
                if (addFragment != null && data != null) {
                    ToastUtils.showToast(this, R.string.image_added);
                    addFragment.onActivityResult(requestCode, resultCode, data);
                }
                break;
            case RequestCodes.SITE_PICKER:
                if (getMySiteFragment() != null) {
                    getMySiteFragment().onActivityResult(requestCode, resultCode, data);
                }
                break;
            case RequestCodes.BLOG_SETTINGS:
                if (resultCode == BlogPreferencesActivity.RESULT_BLOG_REMOVED) {
                    // user removed the current (self-hosted) blog from blog settings
                    if (!AccountHelper.isSignedIn()) {
                        ActivityLauncher.showSignInForResult(this);
                    } else {
                        MySiteFragment mySiteFragment = getMySiteFragment();
                        if (mySiteFragment != null) {
                            mySiteFragment.setBlog(WordPress.getCurrentBlog());
                        }
                    }
                }
                break;
        }
    }

    /*
     * returns the reader list fragment from the reader tab
     */
    private ReaderPostListFragment getReaderListFragment() {
        return getFragmentByPosition(WPMainTabAdapter.TAB_READER, ReaderPostListFragment.class);
    }
    /*
     * returns the notification list fragment from the notification tab
     */
    private NotificationsListFragment getNotificationListFragment() {
        return getFragmentByPosition(WPMainTabAdapter.TAB_NOTIFS, NotificationsListFragment.class);
    }

    /*
     * returns the my site fragment from the sites tab
     */
    public MySiteFragment getMySiteFragment() {
        return getFragmentByPosition(WPMainTabAdapter.TAB_MY_SITE, MySiteFragment.class);
    }


    private <T> T getFragmentByPosition(int position, Class<T> type) {
        Fragment fragment = mTabAdapter != null ? mTabAdapter.getFragment(position) : null;

        if (fragment != null && type.isInstance(fragment)) {
            return type.cast(fragment);
        }

        return null;
    }

    /*
     * badges the notifications tab depending on whether there are unread notes
     */
    private boolean mIsCheckingNoteBadge;
    private void checkNoteBadge() {
        if (mIsCheckingNoteBadge) {
            AppLog.v(T.MAIN, "main activity > already checking note badge");
            return;
        } /*else if (isViewingNotificationsTab()) {
            // Don't show the badge if the notifications tab is active
            //if (mTabs.isBadged(RipotiMainTabAdapter.TAB_NOTIFS)) {
            //    mTabs.setBadge(RipotiMainTabAdapter.TAB_NOTIFS, false);
            //}

            return;
        }*/

        mIsCheckingNoteBadge = true;
        new Thread() {
            @Override
            public void run() {/*
                final boolean hasUnreadNotes = SimperiumUtils.hasUnreadNotes();
                boolean isBadged = mTabs.isBadged(RipotiMainTabAdapter.TAB_NOTIFS);
                if (hasUnreadNotes != isBadged) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTabs.setBadge(RipotiMainTabAdapter.TAB_NOTIFS, hasUnreadNotes);
                            mIsCheckingNoteBadge = false;
                        }
                    });
                } else {
                    mIsCheckingNoteBadge = false;
                }*/
            }
        }.start();
    }

    /*
    private boolean isViewingNotificationsTab() {
        return mViewPager.getCurrentItem() == RipotiMainTabAdapter.TAB_NOTIFS;
    }*/

    // Events

    @SuppressWarnings("unused")
    public void onEventMainThread(UserSignedOutWordPressCom event) {
        resetFragments();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(UserSignedOutCompletely event) {
        ActivityLauncher.showSignInForResult(this);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CoreEvents.InvalidCredentialsDetected event) {
        AuthenticationDialogUtils.showAuthErrorView(this);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CoreEvents.RestApiUnauthorized event) {
        AuthenticationDialogUtils.showAuthErrorView(this);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CoreEvents.TwoFactorAuthenticationDetected event) {
        AuthenticationDialogUtils.showAuthErrorView(this);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CoreEvents.InvalidSslCertificateDetected event) {
        SelfSignedSSLCertsManager.askForSslTrust(this, null);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CoreEvents.LoginLimitDetected event) {
        ToastUtils.showToast(this, R.string.limit_reached, ToastUtils.Duration.LONG);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(NotificationEvents.NotificationsChanged event) {
        checkNoteBadge();
    }

    /*
     * Simperium Note bucket listeners
     */
    @Override
    public void onNetworkChange(Bucket<Note> noteBucket, Bucket.ChangeType changeType, String s) {
        if (changeType == Bucket.ChangeType.INSERT || changeType == Bucket.ChangeType.MODIFY) {
            checkNoteBadge();
        }
    }

    @Override
    public void onBeforeUpdateObject(Bucket<Note> noteBucket, Note note) {
        // noop
    }

    @Override
    public void onDeleteObject(Bucket<Note> noteBucket, Note note) {
        // noop
    }

    @Override
    public void onSaveObject(Bucket<Note> noteBucket, Note note) {
        // noop
    }

    @Override
    public void onMediaAdded(String mediaId) {
    }
}
