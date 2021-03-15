package com.samourai.sentinel.ui.views.swipeLayout.app;


import com.samourai.sentinel.ui.views.swipeLayout.SwipeBackLayout;

public interface SwipeBackActivityBase {
    /**
     * @return the SwipeBackLayout associated with this activity.
     */
    SwipeBackLayout getSwipeBackLayout();

    void setSwipeBackEnable(boolean enable);

    /**
     * Scroll out contentView and finish the activity
     */
    void scrollToFinishActivity();

}
