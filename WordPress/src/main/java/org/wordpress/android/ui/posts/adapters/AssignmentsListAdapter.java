package org.wordpress.android.ui.posts.adapters;

import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.AssignmentsListPost;
import org.wordpress.android.models.PostStatus;
import org.wordpress.android.models.AssignmentsListPost;
import org.wordpress.android.ui.main.AssignmentsListFragment;
import org.wordpress.android.ui.main.RipotiPostsListFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for Posts/Pages list
 */
public class AssignmentsListAdapter extends BaseAdapter {
    public static interface OnLoadMoreListener {
        public void onLoadMore();
    }

    public static interface OnPostsLoadedListener {
        public void onPostsLoaded(int postCount);
    }

    private final OnLoadMoreListener mOnLoadMoreListener;
    private final OnPostsLoadedListener mOnPostsLoadedListener;
    private Context mContext;
    private boolean mIsPage;
    private LayoutInflater mLayoutInflater;

    private List<AssignmentsListPost> mPosts = new ArrayList<AssignmentsListPost>();


    public AssignmentsListAdapter(Context context, boolean isPage, OnLoadMoreListener onLoadMoreListener, OnPostsLoadedListener onPostsLoadedListener) {
        mContext = context;
        mIsPage = isPage;
        mOnLoadMoreListener = onLoadMoreListener;
        mOnPostsLoadedListener = onPostsLoadedListener;
        mLayoutInflater = LayoutInflater.from(mContext);
    }

    public List<AssignmentsListPost> getPosts() {
        return mPosts;
    }

    public void setPosts(List<AssignmentsListPost> postsList) {
        if (postsList != null)
            this.mPosts = postsList;
    }

    @Override
    public int getCount() {
        return mPosts.size();
    }

    @Override
    public Object getItem(int position) {
        return mPosts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mPosts.get(position).getPostId();
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        AssignmentsListPost post = mPosts.get(position);
        PostViewWrapper wrapper;
        if (view == null) {
            view = mLayoutInflater.inflate(R.layout.assignment_cardview, parent, false);
            wrapper = new PostViewWrapper(view);
            view.setTag(wrapper);
        } else {
            wrapper = (PostViewWrapper) view.getTag();
        }

        String titleText = post.getTitle();
        if (titleText.equals(""))
            titleText = "(" + mContext.getResources().getText(R.string.untitled) + ")";
        wrapper.getTitle().setText(titleText);

        String excerptText = post.getExcerpt();
        wrapper.getExcerpt().setText(excerptText);

        String locationText = post.getLocation();
        wrapper.getLocation().setText(locationText);

        String bountyText = post.getBounty();
        if (bountyText.equals("KSH 0"))
            bountyText = "n/a";
        wrapper.getBounty().setText(bountyText);

        String deadlineText = post.getDeadline();
        if (deadlineText.equals(""))
            deadlineText = "Open";
        wrapper.getDeadline().setText(deadlineText);

        String mediaTypes = post.getMedia_types();

        if (mediaTypes.contains("image"))
            wrapper.getMedia_type_photo().setColorFilter(R.color.assignment_media_type_required);
        if (mediaTypes.contains("video"))
            wrapper.getMedia_type_video().setColorFilter(R.color.assignment_media_type_required);
        if (mediaTypes.contains("audio"))
            wrapper.getMedia_type_audio().setColorFilter(R.color.assignment_media_type_required);

        // load more posts when we near the end
        if (mOnLoadMoreListener != null && position >= getCount() - 1
                && position >= AssignmentsListFragment.POSTS_REQUEST_COUNT - 1) {
            mOnLoadMoreListener.onLoadMore();
        }

        return view;
    }

    public void loadPosts() {
        if (WordPress.getCurrentBlog() == null) {
            return;
        }

        // load posts from db
        new LoadPostsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void clear() {
        if (mPosts.size() > 0) {
            mPosts.clear();
            notifyDataSetChanged();
        }
    }

    class PostViewWrapper {
        View base;
        TextView title = null;
        TextView excerpt = null;
        TextView location = null;
        TextView bounty = null;
        ImageView media_type_photo = null;
        ImageView media_type_video = null;
        ImageView media_type_audio = null;
        TextView date = null;
        TextView status = null;

        PostViewWrapper(View base) {
            this.base = base;
        }

        ImageView getMedia_type_photo(){
            if (media_type_photo == null) {
                media_type_photo = (ImageView) base.findViewById(R.id.media_type_photo);
            }
            return (media_type_photo);
        }
        ImageView getMedia_type_video(){
            if (media_type_video == null) {
                media_type_video = (ImageView) base.findViewById(R.id.media_type_video);
            }
            return (media_type_video);
        }
        ImageView getMedia_type_audio(){
            if (media_type_audio == null) {
                media_type_audio = (ImageView) base.findViewById(R.id.media_type_audio);
            }
            return (media_type_audio);
        }

        TextView getTitle() {
            if (title == null) {
                title = (TextView) base.findViewById(R.id.text_title);
            }
            return (title);
        }

        TextView getExcerpt() {
            if (excerpt == null) {
                excerpt = (TextView) base.findViewById(R.id.text_excerpt);
            }
            return (excerpt);
        }

        TextView getDeadline() {
            if (date == null) {
                date = (TextView) base.findViewById(R.id.assignment_list_deadline);
            }
            return (date);
        }

        TextView getLocation() {
            if (location == null) {
                location = (TextView) base.findViewById(R.id.text_location);
            }
            return (location);
        }

        TextView getBounty() {
            if (bounty == null) {
                bounty = (TextView) base.findViewById(R.id.text_bounty);
            }
            return (bounty);
        }
    }

    private class LoadPostsTask extends AsyncTask <Void, Void, Boolean> {
        List<AssignmentsListPost> loadedPosts;

        @Override
        protected Boolean doInBackground(Void... nada) {
            loadedPosts = WordPress.wpDB.getAssignmentsListPosts(WordPress.getCurrentLocalTableBlogId(), mIsPage);
            if (postsListMatch(loadedPosts)) {
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                setPosts(loadedPosts);
                notifyDataSetChanged();

                if (mOnPostsLoadedListener != null && mPosts != null) {
                    mOnPostsLoadedListener.onPostsLoaded(mPosts.size());
                }
            }
        }
    }

    public boolean postsListMatch(List<AssignmentsListPost> newPostsList) {
        if (newPostsList == null || newPostsList.size() == 0 || mPosts == null || mPosts.size() != newPostsList.size())
            return false;

        for (int i = 0; i < newPostsList.size(); i++) {
            AssignmentsListPost newPost = newPostsList.get(i);
            AssignmentsListPost currentPost = mPosts.get(i);

            if (newPost.getPostId() != currentPost.getPostId())
                return false;
            if (!newPost.getTitle().equals(currentPost.getTitle()))
                return false;
            if (newPost.getDateCreatedGmt() != currentPost.getDateCreatedGmt())
                return false;
            if (!newPost.getOriginalStatus().equals(currentPost.getOriginalStatus()))
                return false;
            if (newPost.isUploading() != currentPost.isUploading())
                return false;
            if (newPost.isLocalDraft() != currentPost.isLocalDraft())
                return false;
            if (newPost.hasLocalChanges() != currentPost.hasLocalChanges())
                return false;
        }

        return true;
    }

    public int getRemotePostCount() {
        if (mPosts == null)
            return 0;

        int remotePostCount = 0;
        for (AssignmentsListPost post : mPosts) {
            if (!post.isLocalDraft())
                remotePostCount++;
        }

        return remotePostCount;
    }
}
