package com.example.xyzreader.ui;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
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
 * https://www.bignerdranch.com/blog/a-view-divided-adding-dividers-to-your-recyclerview-with-itemdecoration/
 * https://gist.github.com/yqritc/ccca77dc42f2364777e1
 * https://stackoverflow.com/questions/28531996/android-recyclerview-gridlayoutmanager-column-spacing/28533234
 *
 */
public class EqualOffsetItemDecoration extends RecyclerView.ItemDecoration {

    private int mItemOffset;    // How much offset we want to apply to the child views
    private int mColumnCount;   // The # of columns in the RecyclerView

    public EqualOffsetItemDecoration(int itemOffset, int columnCount) {
        mItemOffset = itemOffset;
        mColumnCount = columnCount;
    }

    public EqualOffsetItemDecoration(@NonNull Context context, @DimenRes int itemOffsetID, @NonNull int columnCount){
        this(context.getResources().getDimensionPixelSize(itemOffsetID), columnCount);
    }

    /*
    Applies margin to the individual child views in order to make room for an ItemDecoration. This
    method is normally used in order to make space for a Drawable which is used as an
    ItemDecoration. However, we're not actually using any sort of drawable for our ItemDecoration.
    The EqualOffsetItemDecoration is simply the margins/offsets between all of the child views in
    our grid layout. Therefore, this is the only method that we need to override.
     */
    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);

        // We first determine the position of the child view...
        int childPosition = parent.getChildAdapterPosition(view);
        // ...and then determine which column the child view is in.
        int childColumn = childPosition % mColumnCount;

        // If the child view is in the left-most column, then it will have the offset applied to
        // its left side
        if (childColumn == 0) {
            outRect.left = mItemOffset;
        }
        // All child views will have the offset applied to their right side
        outRect.right = mItemOffset;

        // If the child view is in the first row, it will also have the offset applied to its
        // top side
        if (childPosition < mColumnCount) {
            outRect.top = mItemOffset;
        }

        // Finally, all child views will have the offset applied to their bottom side
        outRect.bottom = mItemOffset;
    }
}
