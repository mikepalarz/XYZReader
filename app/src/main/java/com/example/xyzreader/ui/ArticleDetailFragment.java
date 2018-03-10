package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
    public static final String ARG_CURRENT_TRANSITION_ID = "current_transition_id";
    public static final String ARG_STARTING_TRANSITION_ID = "starting_transition_id";

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private int mMutedColor = 0xFF333333;

    private ImageView mPhotoView;
    private boolean mIsCard = false;
    private Toolbar mToolbar;
    private ImageView mUpButton;
    private ImageView mLogo;
    // A reference to the current article position, if the user swiped articles
    private int mCurrentTransitionID;
    // A reference to the starting article position, when the article detail activity was launched
    private int mStartingTransitionID;

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

    public static ArticleDetailFragment newInstance(long itemId, int currentTransitionID, int startingTransitionID) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        // We store the current article position
        arguments.putInt(ARG_CURRENT_TRANSITION_ID, currentTransitionID);
        // ...as well as the starting article position.
        arguments.putInt(ARG_STARTING_TRANSITION_ID, startingTransitionID);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();

        if (arguments.containsKey(ARG_ITEM_ID)) {
            mItemId = arguments.getLong(ARG_ITEM_ID);
        }
        if (arguments.containsKey(ARG_CURRENT_TRANSITION_ID)) {
            mCurrentTransitionID = arguments.getInt(ARG_CURRENT_TRANSITION_ID);
        }
        if (arguments.containsKey(ARG_STARTING_TRANSITION_ID)) {
            mStartingTransitionID = arguments.getInt(ARG_STARTING_TRANSITION_ID);
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
        ViewCompat.setTransitionName(mPhotoView, getString(R.string.transition_name) + mCurrentTransitionID);

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

        final TextView titleView = (TextView) mRootView.findViewById(R.id.article_title);
        final TextView bylineView = (TextView) mRootView.findViewById(R.id.article_byline);

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

            bodyView.setText(Html.fromHtml(
                    mCursor.getString(ArticleLoader.Query.BODY)
                        .replaceAll("\r\n\r\n", "<br/><br/>")
                    )
            );

            // This is where the image is loaded.
            ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                    .get(mCursor.getString(ArticleLoader.Query.PHOTO_URL), new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                            Bitmap bitmap = imageContainer.getBitmap();
                            if (bitmap != null) {
                                /*
                                We generate our Palette asynchronously so that we're not causing
                                any unnecessary delays on the UI thread. We then use the Palette
                                to set the background of the meta bar as well as the title and
                                byline text color.
                                 */
                                Palette
                                        .from(bitmap)
                                        .maximumColorCount(16)
                                        .generate(new Palette.PaletteAsyncListener() {
                                    @Override
                                    public void onGenerated(Palette palette) {

                                        /*
                                        We create a Swatch instance so that we can dynamically
                                        adjust the color the title and byline text.
                                         */
                                        Palette.Swatch vibrantSwatch = palette.getVibrantSwatch();

                                        /*
                                        As long as we have a valid swatch, we adjust the title and
                                        byline text color.
                                         */
                                        if (vibrantSwatch != null) {
                                            mMutedColor = vibrantSwatch.getRgb();
                                            titleView.setTextColor(vibrantSwatch.getTitleTextColor());
                                            bylineView.setTextColor(vibrantSwatch.getTitleTextColor());
                                        }
                                        // Otherwise, we'll just adjust the color of the meta bar BG
                                        else {
                                            mMutedColor = palette.getVibrantColor(0xFF333333);
                                        }

                                        // This is where we set the background color of the meta bar
                                        mRootView.findViewById(R.id.fragment_article_details_meta_bar)
                                                .setBackgroundColor(mMutedColor);
                                    }
                                });
                                mPhotoView.setImageBitmap(imageContainer.getBitmap());

                                /*
                                We only want to start the enter transition if these values are the
                                same. In other words, we only want the transition to occur if the
                                article that started the detail activity is the same as the one that's
                                currently being shown. One of the main reason's this precaution was
                                added is because Alex Lockwood's project was taking the same approach.
                                 */
                                if (mCurrentTransitionID == mStartingTransitionID) {
                                    mPhotoView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                                        @Override
                                        public boolean onPreDraw() {
                                            mPhotoView.getViewTreeObserver().removeOnPreDrawListener(this);
                                            // Start the postponed transition
                                            ActivityCompat.startPostponedEnterTransition(getActivity());
                                            return true;
                                        }
                                    });
                                }
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

    /**
     * Returns the shared element that should be transitioned back to the previous Activity,
     * or null if the view is not visible on the screen.
     */
    @Nullable
    ImageView getArticlePhoto() {
        if (isViewInBounds(getActivity().getWindow().getDecorView(), mPhotoView)) {
            return mPhotoView;
        }
        return null;
    }

    /**
     * Returns true if {@param view} is contained within {@param container}'s bounds.
     */
    private static boolean isViewInBounds(@NonNull View container, @NonNull View view) {
        Rect containerBounds = new Rect();
        container.getHitRect(containerBounds);
        return view.getLocalVisibleRect(containerBounds);
    }

}
