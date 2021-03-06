package com.snapsigns.my_signs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentContainer;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.snapsigns.BaseFragment;
import com.snapsigns.ImageSign;
import com.snapsigns.MainActivity;
import com.snapsigns.R;
import com.snapsigns.SnapSigns;
import com.snapsigns.utilities.Constants;

import java.util.ArrayList;
import java.util.List;

import pl.droidsonroids.gif.GifImageView;

/**
 * Creates a list view of past signs taken. When user clicks on an item in list view it
 * opens the activity SignDetail which displays a full screen view on the sign.
 */
public class MySignsFragment extends BaseFragment {
    SnapSigns appContext;
    GridView gridView;
    ImageView fullScreenContainer;
    GifImageView loadingView;
    Toolbar toolbar;
    ImageButton exitFullScreenButton;
    MySignsAdapter mAdapter;
    List<ImageSign> myImageSigns;
    ViewTreeObserver viewTreeObserver;
    MainActivity mActivity;
    TextView emptySignMessage;
    TextView message;

    private final static String TAG = MySignsFragment.class.getSimpleName();
    public final static String IMAGE_URL_KEY = "img_url";


    @Override
    public void onStart() {
        registerImageSignReceiver();
        super.onStart();
    }

    @Override
    public void onStop() {
        getActivity().unregisterReceiver(broadcastReceiver);
        super.onStop();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.i(TAG,"in onCreateView of MySignsFragment");
        View rootView = inflater.inflate(R.layout.my_signs_grid_view, container, false);


        mActivity = (MainActivity) getActivity();
        appContext = (SnapSigns)mActivity.getApplicationContext();
        myImageSigns = appContext.getMyImageSigns();

        toolbar = (Toolbar) rootView.findViewById(R.id.toolbar);
        emptySignMessage = (TextView) rootView.findViewById(R.id.empty_message);
        message = (TextView) rootView.findViewById(R.id.message);
        fullScreenContainer = (ImageView) rootView.findViewById(R.id.fullscreen_container);
        gridView = (GridView) rootView.findViewById(R.id.gridview);
        exitFullScreenButton = (ImageButton) rootView.findViewById(R.id.exit_fullscreen);
        loadingView = (GifImageView)rootView.findViewById(R.id.loading_view);

        mAdapter = new MySignsAdapter(mActivity);
        gridView.setAdapter(mAdapter);

        /* TODO: When an image is selected it will open up a new view with just that image
         * with it's other data  */
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                showFullScreenImage(position);
            }
        });

        exitFullScreenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishFullScreen();
            }
        });

        //on configuration changes (screen rotation) we want fragment member variables to be preserved
        //setRetainInstance(true);

        if(mActivity.signJustSaved){
            gridView.setVisibility(View.INVISIBLE);
            mActivity.showLoadingView();
        }

        if(myImageSigns.isEmpty()){
            emptySignMessage.setVisibility(View.VISIBLE);
        }

        return rootView;
    }

    /******** Broadcast Receiver in charge of notifying adapter when signs are downloaded *******/
    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(Constants.MY_SIGNS.GET_MY_SIGNS)){
                Log.v(TAG,"Retrieved broadcast to update user signs");
                mAdapter.notifyDataSetChanged();

                if(!myImageSigns.isEmpty())
                    emptySignMessage.setVisibility(View.INVISIBLE);

                viewTreeObserver = gridView.getViewTreeObserver();
                viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                       if(mActivity != null) {
                           mActivity.hideLoadingView();
                           gridView.setVisibility(View.VISIBLE);
                           gridView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                       }
                    }
                });
            }


        }
    };

    public void registerImageSignReceiver(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.MY_SIGNS.GET_MY_SIGNS);
        getActivity().registerReceiver(broadcastReceiver, intentFilter);
    }

    private void showFullScreenImage(final int position){
        loadingView.setVisibility(View.VISIBLE);

        Glide.with(mActivity).load(myImageSigns.get(position).imgURL).listener(new RequestListener<String, GlideDrawable>() {
            @Override
            public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                return false;
            }

            @Override
            public boolean onResourceReady(GlideDrawable resource, String model,
                                           Target<GlideDrawable> target, boolean isFromMemoryCache,
                                           boolean isFirstResource) {
                fullScreenContainer.setVisibility(View.VISIBLE);
                if(myImageSigns.get(position).message != null){
                    message.setVisibility(View.VISIBLE);
                    message.setText(myImageSigns.get(position).message);
                }
                loadingView.setVisibility(View.INVISIBLE);
                return false;
            }

        }).into(fullScreenContainer);

        exitFullScreenButton.setVisibility(View.VISIBLE);

        gridView.setVisibility(View.INVISIBLE);
        toolbar.setVisibility(View.INVISIBLE);


        mActivity.hideBottomBar();
    }

    public void finishFullScreen(){
        toolbar.setVisibility(View.VISIBLE);
        gridView.setVisibility(View.VISIBLE);
        mActivity.showBottomBar();
        exitFullScreenButton.setVisibility(View.INVISIBLE);
        fullScreenContainer.setVisibility(View.INVISIBLE);
        message.setVisibility(View.INVISIBLE);
    }
}
