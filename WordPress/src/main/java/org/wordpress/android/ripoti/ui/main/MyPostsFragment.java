package org.wordpress.android.ripoti.ui.main;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;

public class MyPostsFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.ripoti_myposts, container, false);
    }

    public static MyPostsFragment newInstance() {
        return new MyPostsFragment();
    }
}