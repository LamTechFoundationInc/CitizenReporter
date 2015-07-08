package org.wordpress.android.ui.posts;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.SuggestionTable;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.Suggestion;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.ui.comments.CommentActions;
import org.wordpress.android.ui.main.RipotiMainActivity;
import org.wordpress.android.ui.suggestion.adapters.SuggestionAdapter;
import org.wordpress.android.ui.suggestion.service.SuggestionEvents;
import org.wordpress.android.ui.suggestion.util.SuggestionServiceConnectionManager;
import org.wordpress.android.ui.suggestion.util.SuggestionUtils;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPHtml;
import org.wordpress.android.util.WPWebViewClient;
import org.wordpress.android.widgets.SuggestionAutoCompleteText;

import java.util.List;

import de.greenrobot.event.EventBus;

public class ViewAssignmentFragment extends Fragment {
    /** Called when the activity is first created. */

    private OnDetailAssignmentActionListener mOnDetailPostActionListener;
    RipotiMainActivity mParentActivity;

    private TextView mTitleTextView, mContentTextView;

    private SuggestionAdapter mSuggestionAdapter;
    private SuggestionServiceConnectionManager mSuggestionServiceConnectionManager;

    private TextView location;
    private TextView bounty;
    private TextView date;
    private TextView assignment_post_author;
    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Don't load the post until we know the width of mContentTextView
        // GlobalLayoutListener on mContentTextView will load the post once it gets laid out
        if (WordPress.currentPost != null && !getView().isLayoutRequested()) {
            loadPost(WordPress.currentPost);
        }
        mParentActivity = (RipotiMainActivity) getActivity();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(SuggestionEvents.SuggestionNameListUpdated event) {
        int remoteBlogId = WordPress.getCurrentRemoteBlogId();
        // check if the updated suggestions are for the current blog and update the suggestions
        if (event.mRemoteBlogId != 0 && event.mRemoteBlogId == remoteBlogId && mSuggestionAdapter != null) {
            List<Suggestion> suggestions = SuggestionTable.getSuggestionsForSite(event.mRemoteBlogId);
            mSuggestionAdapter.setSuggestionList(suggestions);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (mSuggestionServiceConnectionManager != null) {
            mSuggestionServiceConnectionManager.unbindFromService();
        }
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.view_assignment_fragment, container, false);
        v.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                loadPost(WordPress.currentPost);
                v.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
        });

        mTitleTextView = (TextView) v.findViewById(R.id.postTitle);
        mContentTextView = (TextView) v.findViewById(R.id.viewPostTextView);
        assignment_post_author = (TextView) v.findViewById(R.id.assignment_post_author);
        bounty = (TextView) v.findViewById(R.id.text_bounty);
        location = (TextView) v.findViewById(R.id.text_location);
        date = (TextView) v.findViewById(R.id.assignment_list_deadline);
        return v;
    }



    /**
     * Load the post preview as an authenticated URL so stats aren't bumped.
     */
    protected void loadPostPreview() {
        if (WordPress.currentPost != null && !TextUtils.isEmpty(WordPress.currentPost.getPermaLink())) {
            Post post = WordPress.currentPost;
            String url = post.getPermaLink();
            if (-1 == url.indexOf('?')) {
                url = url.concat("?preview=true");
            } else {
                url = url.concat("&preview=true");
            }
            WPWebViewActivity.openUrlByUsingBlogCredentials(getActivity(), WordPress.currentBlog, url);
        }
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            // check that the containing activity implements our callback
            mOnDetailPostActionListener = (OnDetailAssignmentActionListener) activity;
        } catch (ClassCastException e) {
            activity.finish();
            throw new ClassCastException(activity.toString()
                    + " must implement Callback");
        }
    }

    public void loadPost(final Post post) {
        // Don't load if the Post object or title are null, see #395
        if (!isAdded() || getView() == null || post == null || post.getTitle() == null) {
            return;
        }

        // create handler on UI thread
        final Handler handler = new Handler();

        // locate views and determine content in the background to avoid ANR - especially
        // important when using WPHtml.fromHtml() for drafts that contain images since
        // thumbnails may take some time to create
        final WebView webView = (WebView) getView().findViewById(R.id.viewPostWebView);
        webView.setWebViewClient(new WPWebViewClient(WordPress.getCurrentBlog()));
        new Thread() {
            @Override
            public void run() {

                final String title = (TextUtils.isEmpty(post.getTitle())
                                        ? "(" + getResources().getText(R.string.untitled) + ")"
                                        : StringUtils.unescapeHTML(post.getTitle()));

                final String postContent = post.getDescription() + "\n\n" + post.getMoreText();

                final Spanned draftContent;
                final String htmlContent;
                if (post.isLocalDraft()) {
                    View view = getView();
                    int maxWidth = Math.min(view.getWidth(), view.getHeight());

                    draftContent = WPHtml.fromHtml(postContent.replaceAll("\uFFFC", ""), getActivity(), post, maxWidth);
                    htmlContent = null;
                } else {
                    draftContent = null;
                    htmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                                + "<html><head><link rel=\"stylesheet\" type=\"text/css\" href=\"webview.css\" /></head>"
                                + "<body><div id=\"container\">"
                                + StringUtils.addPTags(postContent)
                                + "</div></body></html>";
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        // make sure activity is still valid
                        if (!isAdded())
                            return;

                        mTitleTextView.setText(title);

                        if (post.isLocalDraft()) {
                            mContentTextView.setVisibility(View.VISIBLE);
                            webView.setVisibility(View.GONE);

                            mContentTextView.setText(draftContent);
                        } else {
                            mContentTextView.setVisibility(View.GONE);
                            webView.setVisibility(View.VISIBLE);
                            webView.loadDataWithBaseURL("file:///android_asset/",
                                                        htmlContent,
                                                        "text/html",
                                                        "utf-8",
                                                        null);
                        }
                    }
                });
            }
        }.start();
    }

    public interface OnDetailAssignmentActionListener {
        public void onDetailAssignmentAction(int action, Post post);
    }

    public void clearContent() {
        TextView txtTitle = (TextView) getView().findViewById(R.id.postTitle);
        WebView webView = (WebView) getView().findViewById(R.id.viewPostWebView);
        TextView txtContent = (TextView) getView().findViewById(R.id.viewPostTextView);
        txtTitle.setText("");
        txtContent.setText("");
        String htmlText = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                        + "<html><head><link rel=\"stylesheet\" type=\"text/css\" href=\"webview.css\" /></head>"
                        + "<body><div id=\"container\"></div></body></html>";
        webView.loadDataWithBaseURL("file:///android_asset/", htmlText,
                "text/html", "utf-8", null);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }
        super.onSaveInstanceState(outState);
    }
}
