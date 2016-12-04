package com.snapsigns.nearby_signs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.snapsigns.BaseFragment;
import com.snapsigns.ImageSign;
import com.snapsigns.MainActivity;
import com.snapsigns.R;
import com.snapsigns.SnapSigns;
import com.snapsigns.utilities.Constants;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;

/**
 * Displays Nearby Signs
 * For now uses test images.
 * Right Left buttons to go through the images.
 */
public class NearbySignsFragment extends BaseFragment {
    public static  ViewPager mPager;
    View rootView;
    MainActivity mActivity;
    SnapSigns appContext;

    SlidingUpPanelLayout mLayout;
    ListView listView;
    ArrayAdapter<String> arrayAdapter;
    Button postBtn;
    EditText addComment;
    ArrayList<String> comments = new ArrayList<>();

    ImageSign mCurrImageSign;
    List<ImageSign> mNearbySigns;
    SignPagerAdapter mSignPageAdapter;
    ViewTreeObserver viewTreeObserver;


    public static boolean isFullScreen = false;
    public static final int PAGER_REQUEST = 43;
    public static final String POSITION_KEY = "position_key";
    private static final String TAG = NearbySignsFragment.class.getSimpleName();

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

        rootView = inflater.inflate(R.layout.nearby_sign_view_pager, container, false);
        mActivity = (MainActivity) getActivity();
        appContext = (SnapSigns)mActivity.getApplicationContext();
        mNearbySigns = appContext.getNearbySigns();

        /************************ Toolbar Views ****************/
        final Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.toolbar);
        ImageButton gridButton = (ImageButton) rootView.findViewById(R.id.grid_activity_button);
        gridButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((SnapSigns)mActivity.getApplicationContext()).populateAllTags();

                Fragment currentFragment =
                        mActivity.getSupportFragmentManager().findFragmentByTag(MainActivity.NEARBY_SIGNS_FRAGMENT);

                startActivity(new Intent(mActivity,NearbySignsGridActivity.class));
            }
        });

        ImageButton favoriteButton = (ImageButton) rootView.findViewById(R.id.favorite_button);

        favoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        /*********************** ViewPager Views ***************/
        mPager = (ViewPager) rootView.findViewById(R.id.pager);
        mSignPageAdapter = new SignPagerAdapter(mActivity,rootView);
        mPager.setAdapter(mSignPageAdapter);


        /******************* FullScreen Buttons ******************/
        final ImageButton showFullScreen = (ImageButton) rootView.findViewById(R.id.show_fullscreen_button);
        final ImageButton hideFullScreen = (ImageButton) rootView.findViewById(R.id.hide_fullscreen_button);


        showFullScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toolbar.setVisibility(View.GONE);
                mActivity.hideBottomBar();
                showFullScreen.setVisibility(View.INVISIBLE);
                hideFullScreen.setVisibility(View.VISIBLE);
            }
        });

        hideFullScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toolbar.setVisibility(View.VISIBLE);
                mActivity.showBottomBar();
                hideFullScreen.setVisibility(View.INVISIBLE);
                showFullScreen.setVisibility(View.VISIBLE);
            }
        });

        /**************** Comment Box Views ********************/
        mLayout = (SlidingUpPanelLayout) rootView.findViewById(R.id.sliding_layout);
        listView = (ListView) rootView.findViewById(R.id.comment_list);
        // This is the array adapter, it takes the context of the activity as a
        // first parameter, the type of list view as a second parameter and your
        // array as a third parameter.
        arrayAdapter = new ArrayAdapter<String>(
                getContext(),
                android.R.layout.simple_list_item_1,
                comments );
        listView.setAdapter(arrayAdapter);

        addComment = (EditText) rootView.findViewById(R.id.add_comment);
        postBtn = (Button) rootView.findViewById(R.id.post_button);
        postBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newComment = addComment.getText().toString();
                //TODO add comment to imageSign
                arrayAdapter.add(newComment);
                arrayAdapter.notifyDataSetChanged();
                addComment.getText().clear();
            }
        });

        // setupCommentBox();

        if(mNearbySigns.isEmpty()) {
            mPager.setVisibility(View.INVISIBLE);
            mActivity.showLoadingView();

            Snackbar snackbar = Snackbar.make(mActivity.findViewById(android.R.id.content), "Searching for nearby signs....", Snackbar.LENGTH_LONG);
            Snackbar.SnackbarLayout layout = (Snackbar.SnackbarLayout) snackbar.getView();
            ViewGroup.LayoutParams params= layout.getLayoutParams();
            params.height = 220;
            layout.setLayoutParams(params);
            snackbar.show();
            layout.setBackgroundColor(ContextCompat.getColor(mActivity,R.color.dark_purple));
        }

        return rootView;
    }

    /******** Broadcast Receiver in charge of notifying adapter when signs are downloaded *******/
    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(Constants.NEARBY_SIGNS.GET_NEARBY_SIGNS)){
                Log.v(TAG,"Retrieved broadcast to update user signs");
                mSignPageAdapter = new SignPagerAdapter(mActivity,rootView);
                mPager.setVisibility(View.VISIBLE);

                viewTreeObserver = mPager.getViewTreeObserver();
                viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if(mActivity != null) {
                            mPager.setAdapter(mSignPageAdapter);
                            mActivity.hideLoadingView();
                            mPager.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                    }
                });
            }
        }
    };

    public void registerImageSignReceiver(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.NEARBY_SIGNS.GET_NEARBY_SIGNS);
        getActivity().registerReceiver(broadcastReceiver, intentFilter);
    }
    /********************************************************************************************/


    /****** Setting up comment box *******/
    public void setupCommentBox(){
        this.mNearbySigns = mSignPageAdapter.mNearbySigns;

        comments.add("First Entry");

        if(!mNearbySigns.isEmpty()) {
            Log.i(TAG,"should have nearby signs");
            mCurrImageSign = mNearbySigns.get(0);
            if(mCurrImageSign.comments != null)
                comments.addAll(mCurrImageSign.comments);
        } else {
            Log.i(TAG,"no nearby signs");
        }

        /* Sets a listener for when the pages change so we can change the comments with it*/
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                ImageSign imageSign = mNearbySigns.get(position);
                //TODO: Error found here
//                arrayAdapter.clear();
//                arrayAdapter.addAll(imageSign.comments);
//                arrayAdapter.notifyDataSetChanged();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                Log.i(TAG, "onPanelSlide, offset " + slideOffset);
            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                Log.i(TAG, "onPanelStateChanged " + newState);
            }
        });
        mLayout.setFadeOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            }
        });


    }
}
