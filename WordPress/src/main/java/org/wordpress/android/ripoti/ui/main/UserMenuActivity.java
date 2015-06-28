package org.wordpress.android.ripoti.ui.main;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.media.MediaAddFragment;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.ui.themes.ThemeBrowserActivity;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.CoreEvents;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.ServiceUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.widgets.WPNetworkImageView;
import org.wordpress.android.widgets.WPTextView;

import de.greenrobot.event.EventBus;

public class UserMenuActivity extends ActionBarActivity{

    public static final String ADD_MEDIA_FRAGMENT_TAG = "add-media-fragment";
    private static final long ALERT_ANIM_OFFSET_MS   = 1000l;
    private static final long ALERT_ANIM_DURATION_MS = 1000l;

    private WPNetworkImageView mBlavatarImageView;
    private WPTextView mBlogTitleTextView;
    private WPTextView mBlogSubtitleTextView;
    private LinearLayout mLookAndFeelHeader;
    private RelativeLayout mThemesContainer;
    private Blog mBlog;
    private int mBlavatarSz;

    public void setBlog(Blog blog) {
        mBlog = blog;
        refreshBlogDetails();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ServiceUtils.isServiceRunning(UserMenuActivity.this, StatsService.class)) {
            UserMenuActivity.this.stopService(new Intent(UserMenuActivity.this, StatsService.class));
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBlog = WordPress.getCurrentBlog();

        setContentView(R.layout.my_ripoti_fragment);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.my_ripoti));
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FragmentManager fm = getFragmentManager();
        fm.beginTransaction().add(new MediaAddFragment(), ADD_MEDIA_FRAGMENT_TAG).commit();
        
        mBlavatarSz = getResources().getDimensionPixelSize(R.dimen.blavatar_sz_small);
        mBlavatarImageView = (WPNetworkImageView) findViewById(R.id.my_site_blavatar);
        mBlogTitleTextView = (WPTextView) findViewById(R.id.my_site_title_label);
        mBlogSubtitleTextView = (WPTextView) findViewById(R.id.my_site_subtitle_label);
        mLookAndFeelHeader = (LinearLayout) findViewById(R.id.my_site_look_and_feel_header);
        mThemesContainer = (RelativeLayout) findViewById(R.id.row_themes);


        findViewById(R.id.switch_site).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSitePicker();
            }
        });

        findViewById(R.id.row_view_site).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewCurrentSite(UserMenuActivity.this);
            }
        });

        findViewById(R.id.row_stats).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBlog != null) {
                    ActivityLauncher.viewBlogStats(UserMenuActivity.this, mBlog.getLocalTableBlogId());
                }
            }
        });

        findViewById(R.id.row_blog_posts).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewCurrentBlogPosts(UserMenuActivity.this);
            }
        });

        findViewById(R.id.row_media).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewCurrentBlogMedia(UserMenuActivity.this);
            }
        });

        findViewById(R.id.row_pages).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewCurrentBlogPages(UserMenuActivity.this);
            }
        });

        findViewById(R.id.row_comments).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewCurrentBlogComments(UserMenuActivity.this);
            }
        });

        mThemesContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewCurrentBlogThemes(UserMenuActivity.this);
            }
        });

        findViewById(R.id.row_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewBlogSettingsForResult(UserMenuActivity.this, mBlog);
            }
        });

        findViewById(R.id.row_admin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewBlogAdmin(UserMenuActivity.this, mBlog);
            }
        });

        refreshBlogDetails();
    }

    private void showSitePicker() {

            int localBlogId = (mBlog != null ? mBlog.getLocalTableBlogId() : 0);
            ActivityLauncher.showSitePickerForResult(UserMenuActivity.this, localBlogId);

    }
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RequestCodes.PICTURE_LIBRARY:
                FragmentManager fm = getFragmentManager();
                Fragment addFragment = fm.findFragmentByTag(ADD_MEDIA_FRAGMENT_TAG);
                if (addFragment != null) {
                    addFragment.onActivityResult(requestCode, resultCode, data);
                }
                break;

            case RequestCodes.SITE_PICKER:
                // RESULT_OK = site picker changed the current blog
                if (resultCode == Activity.RESULT_OK) {
                    setBlog(WordPress.getCurrentBlogEvenIfNotVisible());
                }
                break;
        }
    }

    private void showAlert(View view) {
        if (view != null) {
            Animation highlightAnimation = new AlphaAnimation(0.0f, 1.0f);
            highlightAnimation.setInterpolator(new Interpolator() {
                private float bounce(float t) {
                    return t * t * 24.0f;
                }

                public float getInterpolation(float t) {
                    t *= 1.1226f;
                    if (t < 0.184f) return bounce(t);
                    else if (t < 0.545f) return bounce(t - 0.40719f);
                    else if (t < 0.7275f) return -bounce(t - 0.6126f) + 1.0f;
                    else return 0.0f;
                }
            });
            highlightAnimation.setStartOffset(ALERT_ANIM_OFFSET_MS);
            highlightAnimation.setRepeatCount(1);
            highlightAnimation.setRepeatMode(Animation.RESTART);
            highlightAnimation.setDuration(ALERT_ANIM_DURATION_MS);
            view.startAnimation(highlightAnimation);
        }
    }

    private void refreshBlogDetails() {
        if (mBlog == null) {
            return;
        }

        int themesVisibility = ThemeBrowserActivity.isAccessible() ? View.VISIBLE : View.GONE;
        mLookAndFeelHeader.setVisibility(themesVisibility);
        mThemesContainer.setVisibility(themesVisibility);

        mBlavatarImageView.setImageUrl(GravatarUtils.gravatarFromEmail(WP.g), mBlavatarSz), WPNetworkImageView.ImageType.BLAVATAR);

        String blogName = StringUtils.unescapeHTML(mBlog.getBlogName());
        String hostName = StringUtils.getHost(mBlog.getUrl());
        String blogTitle = TextUtils.isEmpty(blogName) ? hostName : blogName;

        mBlogTitleTextView.setText(blogTitle);
        mBlogSubtitleTextView.setText(hostName);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }
    public void onEventMainThread(CoreEvents.MainViewPagerScrolled event) {
    }
}
