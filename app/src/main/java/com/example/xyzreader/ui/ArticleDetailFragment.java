package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;


import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";
    public static final String ARG_PHOTO_POSITION = "item_id";

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private int mMutedColor = 0xFF333333;

    private ImageView mPhotoView;
    private boolean mIsCard = false;
    private Toolbar mToolbar;
    private ImageView mUpButton;
    private ImageView mLogo;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        // This is where we determine if the fragment is a card, which is used for the tablet layout
        mIsCard = getResources().getBoolean(R.bool.detail_is_card);
        setHasOptionsMenu(true);
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);

        mPhotoView = (ImageView) mRootView.findViewById(R.id.photo);
        ViewCompat.setTransitionName(mPhotoView, getString(R.string.transition_name) + mItemId);
        Log.i(TAG, "Transition name: " + ViewCompat.getTransitionName(mPhotoView));

        mRootView.findViewById(R.id.share_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });

        bindViews();

        /*
         Here we set our toolbar as the ActionBar of the fragment in order to disable the title
         that automatically appears for the current fragment.
         */
        mToolbar = (Toolbar) mRootView.findViewById(R.id.fragment_article_detail_toolbar);

        ((AppCompatActivity)getActivity()).setSupportActionBar(mToolbar);
        ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);

        /*
         In this case, we're setting an OnClickListener to the custom up button so that the user is
         brought back to ArticleListActivity when it is clicked.
         */
        mUpButton = (ImageView) mRootView.findViewById(R.id.action_up);
        mUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((AppCompatActivity) getActivity()).onSupportNavigateUp();
            }
        });

        /*
        This is a fancy little adjustment that I've made here. Without this, the logo will always
        be partially covered by the window insets because of 3 reasons:

            1. The way that the UI flags are being adjusted within ArticleDetailActivity
            2. Because I've set the width and height of the logo to wrap_content
            3. A bottom margin of 20dp has been applied to the logo in order to abide to the
                Material Design specs for the app bar.

        The simple solution to this issue would be to just decrease the height and width of the logo
        so that it would be contained within the window insets. However, I knew that there must
        be a fancier way to accomplish this.

        I've set an OnOffsetChangedListener to the AppBarLayout to detect when the app bar is fully
        expanded or collapsed. If the app bar is collapsed, then the bottom margin is removed.
        Otherwise, it is reapplied so that we're still following the MD specs.
         */
        mLogo = (ImageView) mRootView.findViewById(R.id.fragment_article_detail_logo);
        final CollapsingToolbarLayout.LayoutParams layoutParams = (CollapsingToolbarLayout.LayoutParams) mLogo.getLayoutParams();

        AppBarLayout appBarLayout = (AppBarLayout) mRootView.findViewById(R.id.fragment_article_detail_app_bar_layout);
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {

                // If toolbar is collapsed, we'll remove the bottom margin
                if (Math.abs(verticalOffset) == appBarLayout.getTotalScrollRange()) {
                    layoutParams.setMargins(layoutParams.leftMargin, layoutParams.topMargin, layoutParams.rightMargin, 0);
                    mLogo.setLayoutParams(layoutParams);
                }

                /*
                Otherwise, we'll add the bottom margin back in. I could have kept just as a plain
                else statement. However, scrolling started to feel jittery since this was being
                called each time the app bar is being expanded. The additional if() has been added
                to ensure that the bottom margin is only added once.
                 */
                else if (layoutParams.bottomMargin != getLogoBottomMargin()) {
                    layoutParams.setMargins(layoutParams.leftMargin, layoutParams.topMargin, layoutParams.rightMargin, getLogoBottomMargin());
                    mLogo.setLayoutParams(layoutParams);
                }
            }
            });


        return mRootView;
    }


    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    /*
    A method which formats the text for the title of the article, the by line, and the main
    body text.
     */
    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        TextView titleView = (TextView) mRootView.findViewById(R.id.article_title);
        TextView bylineView = (TextView) mRootView.findViewById(R.id.article_byline);

        /*
         A LinkMovementMethod is applied to the byline so that any links contained within this
         TextView are handled appropriately. Links are highlighted when they're focused and an
         Intent is launched when they're clicked.
        */

        bylineView.setMovementMethod(new LinkMovementMethod());
        TextView bodyView = (TextView) mRootView.findViewById(R.id.article_body);

        if (mCursor != null) {

            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {

                int colorInt = ContextCompat.getColor(getActivity(), R.color.white87Percent);
                bylineView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + " by <font color="+ colorInt + ">"
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));

            } else {
                // If date is before 1902, just show the string
                bylineView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate) + " by <font color='#ffffff'>"
                        + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));

            }

            bodyView.setText(mCursor.getString(ArticleLoader.Query.BODY));

            // This is where the image is loaded.
            ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                    .get(mCursor.getString(ArticleLoader.Query.PHOTO_URL), new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                            Bitmap bitmap = imageContainer.getBitmap();
                            if (bitmap != null) {
                                Palette p = Palette.generate(bitmap, 12);
                                mMutedColor = p.getVibrantColor(0xFF333333);
                                mPhotoView.setImageBitmap(imageContainer.getBitmap());

                                mPhotoView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                                    @Override
                                    public boolean onPreDraw() {
                                        mPhotoView.getViewTreeObserver().removeOnPreDrawListener(this);
                                        // Start the postponed transition
                                        ActivityCompat.startPostponedEnterTransition(getActivity());
                                        return true;
                                    }
                                });

                                // This is where we set the background color of the meta bar
                                mRootView.findViewById(R.id.meta_bar)
                                        .setBackgroundColor(mMutedColor);
                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError) {

                        }
                    });
        } else {
            mRootView.setVisibility(View.GONE);
            titleView.setText("N/A");
            bylineView.setText("N/A" );
            bodyView.setText("N/A");
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();

//        mPhotoView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
//            @Override
//            public boolean onPreDraw() {
//                mPhotoView.getViewTreeObserver().removeOnPreDrawListener(this);
//                // Start the postponed transition
//                ActivityCompat.startPostponedEnterTransition(getActivity());
//                return true;
//            }
//        });
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }

    /*
    A helper method which simply returns the bottom margin that should be applied to the logo.
     */
    private int getLogoBottomMargin() {

        return getResources().getDimensionPixelSize(R.dimen.app_bar_logo_bottom_margin);
    }

}
