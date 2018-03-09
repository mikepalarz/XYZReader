package com.example.xyzreader.ui;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.util.AttributeSet;
import android.view.View;

/**
 * A custom implementation of a CoordinatorLayout behavior. This behavior was designed to be applied
 * to a FAB so that it can respond to scrolling events. This behavior has been designed so that
 * when the user scrolls down, the FAB goes away via hide(). However, then either the user reaches
 * the very end of scrolling or they being to scroll up in a rapid motion, the FAB will reappear
 * via show(). The following resources were used:
 *
 * https://medium.com/google-developers/intercepting-everything-with-coordinatorlayout-behaviors-8c6adc140c26
 * https://github.com/newfivefour/BlogPosts/blob/master/android-coordinatorlayout-scrolling-hide-fab-behavior.md
 *
 */

public class ScrollAwareBehavior extends CoordinatorLayout.Behavior<FloatingActionButton> {

    private final static String TAG = ScrollAwareBehavior.class.getSimpleName();

    /*
    Necessary constructor for this behavior.
     */
    public ScrollAwareBehavior(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    /*
    This method is overriden in order to indicate that this behavior would like to respond to nested
    scroll events. This behavior will then receive subsequent calls to onNestedScroll().
     */
    @Override
    public boolean onStartNestedScroll(final CoordinatorLayout coordinatorLayout, final FloatingActionButton child, final View directTargetChild, final View target, final int nestedScrollAxes) {
        // In this case, we return true as long as scrolling occurs on the vertical axis.
        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL;
    }

    /*
    This method is overriden in order to actually respond to scrolling events.
     */
    @Override
    public void onNestedScroll(final CoordinatorLayout coordinatorLayout, final FloatingActionButton child, final View target, final int dxConsumed, final int dyConsumed, final int dxUnconsumed, final int dyUnconsumed) {
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);

        /*
        Both dyConsumed and dyUnconsumed only tell us how much scrolling has occurred and not the
        actual position of scrolling. However, we want to show the FAB again once the user reaches
        the end of scrolling. Achieving this is not initially possible just by using dyConsumed
        and/or dyUnconsumed. Therefore, an OnScrollChangeListener is applied in order to both detect
        the actual scroll position via scrollY as well as when the user has reached the end of
        scrolling.
         */
        ((NestedScrollView)target).setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {

                /*
                Not exactly sure why this works. This was taken from the following SO post:

                https://stackoverflow.com/questions/36143802/how-to-detect-the-position-of-the-scroll-nestedscrollview-android-at-the-bottom/37630070#37630070
                 */
                if (scrollY == (v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight() ) ) {
                    child.show();
                }
            }
        });

        /*
        If the user is scrolling down, then we will hide the FAB.
         */
        if (dyConsumed > 0) {
            /*
            Setting an OnVisibilityChangedListener is necessary here due to the fact that the
            onNestedScroll() is skipped for views that have their visibility set to GONE. This
            only occurs on Android projects that are using support library version > 25.0.1. This
            bug has been submitted to Google via:

            https://issuetracker.google.com/issues/37130108

            When we call hide() on the FAB, it by default sets the visibility to GONE, which will
            cause onNestedScroll() to no longer be called. In order to fix this, we add a custom
            OnVisibilityChangedListener so that the FAB's visibility is set to INVISIBLE instead so
            that onNestedScroll() will continue to be called. This solution was found from the
            following SO post:

            https://stackoverflow.com/questions/41153619/floating-action-button-not-visible-on-scrolling-after-updating-google-support/41386278#41386278
             */

            child.hide(new FloatingActionButton.OnVisibilityChangedListener() {
                @Override
                public void onHidden(FloatingActionButton fab) {
                    super.onHidden(fab);
                    fab.setVisibility(View.INVISIBLE);
                }
            });
        }

        /*
        Otherwise, if the user is scrolling back up in a faster motion, then we will show the FAB
        once again. If we just wanted to show the FAB when they're scrolling up, then simply
        checking dyConsumed < 0 would have worked. However, through some testing, it was observed
        that faster up scrolling occurred once dyConsumed was ~ -100.
         */
        else if (dyConsumed <= -100){
            child.show();
        }
    }
}
