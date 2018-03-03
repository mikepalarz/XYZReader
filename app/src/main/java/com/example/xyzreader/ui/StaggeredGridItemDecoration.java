package com.example.xyzreader.ui;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.View;


/**
 * A custom implementation of a RecyclerView.ItemDecoration. This ItemDecoration is used to ensure
 * that we are following the guidance of the MD spec for card collections:
 *
 * https://material.io/guidelines/components/cards.html#cards-content-blocks
 *
 * The spec recommends using 8dp for both the margins and gutters of a card collection. This is not
 * something that can be easily achieved in layout XML since we have no control over the placement of
 * each child view within the RecyclerView. Therefore, an ItemDecoration is used instead, which
 * offers the level of control that we're looking for.
 *
 * This implementation of ItemDecoration was created from the assistance of the following resources:
 *
 * https://stackoverflow.com/questions/35188413/how-to-determine-column-position-in-staggered-grid-layout-manager
 * https://www.bignerdranch.com/blog/a-view-divided-adding-dividers-to-your-recyclerview-with-itemdecoration/
 * https://gist.github.com/yqritc/ccca77dc42f2364777e1
 * https://stackoverflow.com/questions/28531996/android-recyclerview-gridlayoutmanager-column-spacing/28533234
 *
 */
public class StaggeredGridItemDecoration extends RecyclerView.ItemDecoration {

    private static final String TAG = StaggeredGridItemDecoration.class.getSimpleName();

    private int mItemOffset;    // How much offset we want to apply to the child views
    private int mColumnCount;   // The # of columns in the RecyclerView

    public StaggeredGridItemDecoration(int itemOffset, int columnCount) {
        mItemOffset = itemOffset;
        mColumnCount = columnCount;
    }

    public StaggeredGridItemDecoration(@NonNull Context context, @DimenRes int itemOffsetID, @NonNull int columnCount){
        this(context.getResources().getDimensionPixelSize(itemOffsetID), columnCount);
    }

    /*
    Applies margin to the individual child views in order to make room for an ItemDecoration. This
    method is normally used in order to make space for a Drawable which is used as an
    ItemDecoration. However, we're not actually using any sort of drawable for our ItemDecoration.
    The StaggeredGridItemDecoration is simply the margins/offsets between all of the child views in
    our grid layout. Therefore, this is the only method that we need to override.
     */
    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);

        // We first obtain a reference to the layout params of the current view
        StaggeredGridLayoutManager.LayoutParams layoutParams =
                (StaggeredGridLayoutManager.LayoutParams) view.getLayoutParams();

        /*
        A unique behavior of a StaggeredGridLayout is that it places its child views into columns
        based on the size of the child views. Therefore, using something like
        parent.getChildAdapterPosition(view) won't tell us the true position of the view since
        the manager may position the views independent of the adapter's position.

        We can determine which column each child view belongs to by obtaining the span index from
        the layout params.
        */
        int spanIndex = layoutParams.getSpanIndex();

        // If the item belongs to the first column, then we will apply margin to the left of the view
        if (spanIndex == 0) {
            outRect.left = mItemOffset;
        }

        // Regardless of column position, all views will have the margin applied to the right and bottom
        outRect.right = mItemOffset;
        outRect.top = mItemOffset;

    }
}
