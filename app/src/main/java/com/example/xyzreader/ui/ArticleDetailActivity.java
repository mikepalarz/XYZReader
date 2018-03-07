package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

import java.util.List;
import java.util.Map;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = ArticleDetailActivity.class.getSimpleName();
    private static final String STATE_CURRENT_ARTICLE_POSITION = "state_current_article_position";

    private Cursor mCursor;
    private long mStartId;

    private long mSelectedItemId;

    private ViewPager mPager;
    private MyPagerAdapter mPagerAdapter;

    // A reference to the current article detail fragment that is being displayed
    private ArticleDetailFragment mCurrentFragment;
    // The article position that was used to launch this activity
    private int mStartingArticlePosition;
    /*
    The most up-to-date article position. This value will be the same as mStartingArticlePosition,
    but may change if the user swipes between articles.
     */
    private int mCurrentArticlePosition;

    // A boolean used to determine if the user is returning to the list activity
    private boolean mIsReturning;

    // A boolean used to determine if the device is API 21+
    private boolean mIsLollipop;

    /*
    Another callback which is used to correct the return animation to the article list activity,
    if needed.
     */
    private final SharedElementCallback mSharedElementCallback = new SharedElementCallback() {
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            // If we are returning back to the list activity
            if (mIsReturning) {
                ImageView sharedElement = mCurrentFragment.getArticlePhoto();
                if (sharedElement == null) {
                    // If shared element is null, then it has been scrolled off screen and
                    // no longer visible. In this case we cancel the shared element transition by
                    // removing the shared element from the shared elements map.
                    names.clear();
                    sharedElements.clear();
                }
                else if (mCurrentArticlePosition != mStartingArticlePosition){
                    // If the user has swiped to a different ViewPager page, then we need to
                    // remove the old shared element and replace it with the new shared element
                    // that should be transitioned instead.
                    names.clear();
                    sharedElements.clear();
                    String newTransitionName = ViewCompat.getTransitionName(sharedElement);
                    names.add(newTransitionName);
                    sharedElements.put(newTransitionName, sharedElement);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
        We postpone the enter transition of the activity. This transition isn't actually started
        until we're certain that the article image is being displayed, which is actually determined
        within the ArticleDetailFragment class.
         */
        supportPostponeEnterTransition();

        // Again, we set our SharedElementCallback
        setEnterSharedElementCallback(mSharedElementCallback);
        mIsLollipop = getResources().getBoolean(R.bool.is_api_21);

        /*
         This piece is critical. It ensures that our activity's content will appear beneath the
         status bar. It allows the Window that contains this activity's content to be laid out
         beneath screen decorations such as the status bar. This is exactly what we'd like to have
         in order to have a transparent status bar.
         */
        if (mIsLollipop) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
        setContentView(R.layout.activity_article_detail);

        getLoaderManager().initLoader(0, null, this);

        mPagerAdapter = new MyPagerAdapter(getFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mPagerAdapter);

        // Applies a margin between the pages within the ViewPager. The margin has a width of 1dp.
        mPager.setPageMargin((int) TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));

        /*
         The margin's color is actually black with an alpha value (which stores the transparency)
         of approximately 13% so that it appears gray once displayed. The alpha value is the first
         two values within the hexadecimal number that are used when defining the ColorDrawable,
         which is 0x22. For more details, see this SO post:
         https://stackoverflow.com/questions/5445085/understanding-colors-on-android-six-characters
         */
        mPager.setPageMarginDrawable(new ColorDrawable(0x22000000));

        /*
         Setting a SimpleOnPageChangeListener for us to be able to handle events when the user
         swipes left/right to change the page.
         */
        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            /*
             Called whenever the scroll state changes. In this case, we animate the up button
              depending on the scroll state.
             */
            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }

            /*
             Called when a new page has been selected. In this case, we set mSelectedItemId to
             reflect the newly selected page.
             */
            @Override
            public void onPageSelected(int position) {
                if (mCursor != null) {
                    mCursor.moveToPosition(position);
                }
                mSelectedItemId = mCursor.getLong(ArticleLoader.Query._ID);

                /*
                This is where we actually change the value of mCurrentArticlePosition since this is
                where we detect that the user has swiped between articles.
                 */
                mCurrentArticlePosition = position;
            }
        });

        /*
        A PageTransformer is set onto the ViewPager so that we can add animations to some of our
        views when the user swipes in between pages. Creates a really nice visual polish.
         */
        mPager.setPageTransformer(false, new ViewPager.PageTransformer() {
            @Override
            public void transformPage(View page, float position) {

                /*
                We obtain references to all of the views that we'd like to animate. We use
                page.findViewById() since the views are actually stored within the fragment and
                not within this activity.

                The page is a reference to the page that is currently being displayed. The position
                is a reference to how close to the center the current page is, where -1 indicates
                that the page is completely scrolled off to the left edge of the screen and +1
                indicates that the page is scrolled off to the right edge.
                 */
                ImageView photoView = (ImageView) page.findViewById(R.id.photo);
                TextView title = (TextView) page.findViewById(R.id.article_title);
                TextView byline = (TextView) page.findViewById(R.id.article_byline);
                TextView body = (TextView) page.findViewById(R.id.article_body);
                ImageView logo = (ImageView) page.findViewById(R.id.fragment_article_detail_logo);
                FloatingActionButton fab = (FloatingActionButton) page.findViewById(R.id.share_fab);

                int pageWidth = page.getWidth();

                // As long as the current page is visible, we will apply our animations
                if (position >= -1 && position <= 1) {

                    // photoView will have a parallax effect applied
                    photoView.setAlpha(1 - Math.abs(position));
                    photoView.setTranslationX((float) (-position * 0.5 * pageWidth));

                    title.setTranslationX(position * pageWidth);
                    byline.setTranslationX(position * pageWidth);
                    body.setTranslationX(position * pageWidth);
                    logo.setTranslationX((float) (position * 0.7 * pageWidth));

                    // FAB will rotate, pretty neat!
                    fab.setRotation(360 * position);
                    fab.setTranslationX((float) (position * 1.5 * pageWidth));
                }

            }
        });

        mStartingArticlePosition = getIntent().getIntExtra(ArticleListActivity.EXTRA_STARTING_ARTICLE_POSITION, 0);
        mIsReturning = false;

        /*
        If this activity has just started, we'll set mStartId to the article ID that started this
        activity. mSelectedItemId will be set to mStartId as well.
         */
        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                mStartId = ItemsContract.Items.getItemId(getIntent().getData());
                mSelectedItemId = mStartId;
            }

            mCurrentArticlePosition = mStartingArticlePosition;
        }

        /*
        If the user rotates the device, we want to be sure that they are still being shown the
        current article.
         */
        else {
            mCurrentArticlePosition = savedInstanceState.getInt(STATE_CURRENT_ARTICLE_POSITION);
        }

        mPager.setCurrentItem(mCurrentArticlePosition);

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CURRENT_ARTICLE_POSITION, mCurrentArticlePosition);
    }

    /*
    This method gets called once the exit transition of this activity is started. At this point,
    we want to store the values of mStartingArticlePosition and mCurrentArticlePosition and pass
    those values along to the list activity. These values are later on retrieved within the list
    activity's onActivityReenter().
     */
    @Override
    public void finishAfterTransition() {
        mIsReturning = true;
        Intent data = new Intent();
        data.putExtra(ArticleListActivity.EXTRA_STARTING_ARTICLE_POSITION, mStartingArticlePosition);
        data.putExtra(ArticleListActivity.EXTRA_CURRENT_ARTICLE_POSITION, mCurrentArticlePosition);
        setResult(RESULT_OK, data);

        super.finishAfterTransition();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;
        mPagerAdapter.notifyDataSetChanged();

        // Select the start ID
        if (mStartId > 0) {
            mCursor.moveToFirst();
            // TODO: optimize
            while (!mCursor.isAfterLast()) {
                if (mCursor.getLong(ArticleLoader.Query._ID) == mStartId) {
                    final int position = mCursor.getPosition();
                    mPager.setCurrentItem(position, false);
                    break;
                }
                mCursor.moveToNext();
            }
            mStartId = 0;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        mPagerAdapter.notifyDataSetChanged();
    }

    private class MyPagerAdapter extends FragmentStatePagerAdapter {
        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            // Set mCurrentFragment to the fragment which is being currently displayed
            mCurrentFragment = (ArticleDetailFragment) object;
        }

        @Override
        public Fragment getItem(int position) {
            mCursor.moveToPosition(position);
            /*
            In this case, we pass both the adapter position as well as mStartingArticlePosition to
            the fragment so that we later on ensure that the shared element transition is done
            only if the adapter position and mStartingArticlePosition are the same. In other words,
            we ensure that the shared element transition only occurs for the article that launched
            the details activity.
             */
            return ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID),
                    position,
                    mStartingArticlePosition);
        }

        @Override
        public int getCount() {
            return (mCursor != null) ? mCursor.getCount() : 0;
        }
    }
}
