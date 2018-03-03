package com.example.xyzreader.ui;

import android.app.LoaderManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = ArticleListActivity.class.toString();
    public static final String EXTRA_STARTING_ARTICLE_POSITION = "starting_article_position";
    public static final String EXTRA_CURRENT_ARTICLE_POSITION = "current_article_position";

    private Toolbar mToolbar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private int mColumnCount;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    private boolean mIsRefreshing = false;

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
        }
    };

    /*
    A bundle which stores all of the data necessary to properly animate the return animation for
    this activity. More specifically, we are using a shared element transition with the thumbnail
    image between the card views in the this activity and the fragments in the detail activity.
    Because the detail activity is using a ViewPager, the user can change articles by swiping, which
    will cause the incorrect image to be animated by default in the card views upon returning to
    this activity. In order to have the correct image to be animated if the user swipes between
    articles, this Bundle stores the position of the article that launched the details activity
    (stored in EXTRA_STARTING_ARTICLE_POSITION) and the most recently viewed article position
    (stored in EXTRA_CURRENT_ARTICLE_POSITION).
     */
    private Bundle mReenterState;

    // A callback which is used to correct the return animation if needed
    private final SharedElementCallback mSharedElementCallback = new SharedElementCallback() {
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            /*
            If mReenterState is not null, that indicates that the details activity had been
            previously launched, since mReenterState is only instantiated within onActivityReturn()
             */
            if (mReenterState != null){
                int startingPosition = mReenterState.getInt(EXTRA_STARTING_ARTICLE_POSITION, 0);
                int currentPosition = mReenterState.getInt(EXTRA_CURRENT_ARTICLE_POSITION, 0);
                if (currentPosition != startingPosition) {
                    /*
                    If startingPosition != currentPosition the user must have swiped to a
                    different page in the DetailsActivity. We must update the shared element
                    so that the correct one falls into place.
                     */
                    String newTransitionName = getString(R.string.transition_name) + currentPosition;

                    /*
                    We obtain the new shared element from the tag that we set on the thumbnail
                    of the card view. The tag was set within onBindViewHolder().
                     */
                    View newSharedElement = mRecyclerView.findViewWithTag(newTransitionName);
                    if (newSharedElement != null) {
                        names.clear();
                        sharedElements.clear();
                        names.add(newTransitionName);
                        sharedElements.put(newTransitionName, newSharedElement);
                    }
                }
                mReenterState = null;
            }
            else {
                /*
                Otherwise, we are not returning from the details activity but rather launching it
                instead. If that is the case, then we ensure that the status and navigation bar
                are also animated so that we do not run into issues such as this one:

                https://www.youtube.com/watch?v=yAbDPjhftlQ&google_comment_id=z12lgj4hfri4f1v4l04cfrf5fmugdn25t2c
                 */
                View navigationBar = findViewById(android.R.id.navigationBarBackground);
                View statusBar = findViewById(android.R.id.statusBarBackground);
                if (navigationBar != null) {
                    String navigationBarTransitionName = ViewCompat.getTransitionName(navigationBar);
                    names.add(navigationBarTransitionName);
                    sharedElements.put(navigationBarTransitionName, navigationBar);
                }
                if (statusBar != null) {
                    String statusBarTransitionName = ViewCompat.getTransitionName(statusBar);
                    names.add(statusBarTransitionName);
                    sharedElements.put(statusBarTransitionName, statusBar);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        // Setting the SharedElementCallback
        setExitSharedElementCallback(mSharedElementCallback);

        mToolbar = (Toolbar) findViewById(R.id.activity_article_list_toolbar);
        setSupportActionBar(mToolbar);

        ActionBar actionBar = getSupportActionBar();
        // Removing the textual title from the app bar since we already have a logo image
        actionBar.setDisplayShowTitleEnabled(false);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mColumnCount = getResources().getInteger(R.integer.list_column_count);

        /*
        In order to have equal spacing along the edges of the screen as well as between the
        child views of the RecyclerView, a StaggeredGridItemDecoration is applied to the RecyclerView.
         */
        StaggeredGridItemDecoration itemDecoration = new StaggeredGridItemDecoration(this, R.dimen.card_view_margin, mColumnCount);
        mRecyclerView.addItemDecoration(itemDecoration);

        getLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            refresh();
        }
    }

    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Adapter adapter = new Adapter(cursor);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);

        StaggeredGridLayoutManager staggeredGridLayoutManager =
                new StaggeredGridLayoutManager(mColumnCount, StaggeredGridLayoutManager.VERTICAL);

        mRecyclerView.setLayoutManager(staggeredGridLayoutManager);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor mCursor;

        public Adapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            view.setTag(mCursor.getString(ArticleLoader.Query.TITLE));
            final ViewHolder vh = new ViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int adapterPosition = vh.getAdapterPosition();

                    Intent detailsIntent = new Intent(
                            Intent.ACTION_VIEW,
                            ItemsContract.Items.buildItemUri(getItemId(adapterPosition))
                    );

                    /*
                    We now include the adapter position of the current article as an extra. This
                    becomes necessary in the details activity to ensure that the return animation
                    is properly handled.
                     */
                    detailsIntent.putExtra(EXTRA_STARTING_ARTICLE_POSITION, adapterPosition);

                    // This is where we actually define the shared element transition.
                    ActivityOptionsCompat activityOptionsCompat =
                            ActivityOptionsCompat.makeSceneTransitionAnimation(
                                    ArticleListActivity.this,
                                    vh.thumbnailView,
                                    vh.thumbnailView.getTransitionName());

                    startActivity(detailsIntent, activityOptionsCompat.toBundle());
                }
            });
            return vh;
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

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            mCursor.moveToPosition(position);
            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {

                holder.subtitleView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + "<br/>" + " by "
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)));
            } else {
                holder.subtitleView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate)
                        + "<br/>" + " by "
                        + mCursor.getString(ArticleLoader.Query.AUTHOR)));
            }
            holder.thumbnailView.setImageUrl(
                    mCursor.getString(ArticleLoader.Query.THUMB_URL),
                    ImageLoaderHelper.getInstance(ArticleListActivity.this).getImageLoader());
            // Keep this in mind. Here is where they set the aspect ratio of the thumbnail
            // for the list items
            holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));

            /*
            Setting the transition name of the image within the ViewHolder. The transition name
            needs to be unique for each ViewHolder since we are using a ViewPager in the
            details activity.
             */
            String transitionName = getResources().getString(R.string.transition_name) + position;
            holder.thumbnailView.setTransitionName(transitionName);
            /*
            We also set the tag to be the same as the transition name. This tag becomes useful later
            on in our SharedElementCallback's implementation of onMapSharedElements().
             */
            holder.thumbnailView.setTag(transitionName);

        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public DynamicHeightNetworkImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;

        public ViewHolder(View view) {
            super(view);
            thumbnailView = (DynamicHeightNetworkImageView) view.findViewById(R.id.thumbnail);
            titleView = (TextView) view.findViewById(R.id.article_title);
            subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
        }
    }

    /*
    This method gets called when we return back to this activity from a called activity, which
    would only be the detail activity in this particular case. This is where we instantiate
    mReenterState as well as set the onPreDrawListener of mRecyclerView.
     */
    @Override
    public void onActivityReenter(int resultCode, Intent data) {
        super.onActivityReenter(resultCode, data);

        /*
        We instantiate mReenterState as well as obtain the starting article position (the one
        that launched the detail activity) and current article position (the most recently
        view article, if the user swiped between articles).
        */
        mReenterState = new Bundle(data.getExtras());
        int startingPosition = mReenterState.getInt(EXTRA_STARTING_ARTICLE_POSITION, 0);
        int currentPosition = mReenterState.getInt(EXTRA_CURRENT_ARTICLE_POSITION, 0);
        if (currentPosition != startingPosition) {
            mRecyclerView.scrollToPosition(currentPosition);
        }

        // We postpone the enter transition until we're certain that mRecyclerView is displayed
        supportPostponeEnterTransition();

        /*
        Adding a OnPreDrawListener to mRecyclerView, which tells us if the RecyclerView is
        ready to be displayed
         */
        mRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                // This is apparently necessary in order to have a smooth re-enter transition
                mRecyclerView.requestLayout();
                /*
                Now that the RecyclerView is displayed, we can finally start the transition animation.
                 */
                supportStartPostponedEnterTransition();

                return true;
            }
        });
    }
}
