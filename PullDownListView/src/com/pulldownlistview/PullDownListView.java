package com.pulldownlistview;

import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.animation.ValueAnimator.AnimatorUpdateListener;
import com.pulldownlistview.util.JLog;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.AbsListView.OnScrollListener;

public class PullDownListView extends RelativeLayout implements
        OnScrollListener {
    static int MAX_PULL_TOP_HEIGHT;//RelativeLayout height
    static int MAX_PULL_BOTTOM_HEIGHT;//RelativeLayout height

    static int REFRESHING_TOP_HEIGHT;//Header height
    static int REFRESHING_BOTTOM_HEIGHT;//bottom view height

    /**
     * 是否位于ListView的顶部
     */
    private boolean isTop;
    /**
     * 是否位于ListView的底部
     */
    private boolean isBottom;
    /**
     * 是否正在刷新
     * 1 getTop > 0
     * 2 getBottom < parent.height
     */
    private boolean isRefreshing;
    /**
     * 是否正在动画
     */
    private boolean isAnimation;

    /**
     * HeaderView
     */
    RelativeLayout layoutHeader;
    /**
     * footerView
     */
    RelativeLayout layoutFooter;

    private int mCurrentY = 0;
    /**
     * 是否滑动
     */
    boolean pullTag = false;
    OnScrollListener mOnScrollListener;
    OnPullHeightChangeListener mOnPullHeightChangeListener;

    public void setOnPullHeightChangeListener(
            OnPullHeightChangeListener listener) {
        this.mOnPullHeightChangeListener = listener;
    }

    public void setOnScrollListener(OnScrollListener listener) {
        mOnScrollListener = listener;
    }

    public PullDownListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }

    public boolean isRefreshing() {
        return this.isRefreshing;
    }

    private ListView mListView = new ListView(getContext()) {

        int lastY = 0;

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            if (isAnimation || isRefreshing) {// 正在刷新或动画
                return super.onTouchEvent(ev);
            }
            RelativeLayout parent = (RelativeLayout) mListView.getParent();

            int currentY = (int) ev.getRawY();//absolute position to screen
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastY = (int) ev.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE: {
                    boolean isToBottom = currentY - lastY >= 0 ? true : false;//是否向下滑

                    int step = Math.abs(currentY - lastY);//滑动的距离
                    lastY = currentY;

                    if (isTop && mListView.getTop() >= 0) {//向下滑

                        if (isToBottom && mListView.getTop() <= MAX_PULL_TOP_HEIGHT) {//下滑，没有滑到底
                            /*MotionEvent event = MotionEvent.obtain(ev); //不知道有什么用
                            ev.setAction(MotionEvent.ACTION_UP);
                            super.onTouchEvent(ev);*/
                            pullTag = true;

                            if (mListView.getTop() > layoutHeader.getHeight()) {//保证ListView拉不到底部
                                step = step / 2;
                            }
                            if ((mListView.getTop() + step) > MAX_PULL_TOP_HEIGHT) {
                                mCurrentY = MAX_PULL_TOP_HEIGHT;
                                scrollTopTo(mCurrentY);
                            } else {
                                mCurrentY += step;
                                scrollTopTo(mCurrentY);
                            }
                        } else if (!isToBottom && mListView.getTop() > 0) {//上滑，没有滑到顶
                            /*MotionEvent event = MotionEvent.obtain(ev);
                            ev.setAction(MotionEvent.ACTION_UP);
                            super.onTouchEvent(ev);*/
                            if ((mListView.getTop() - step) < 0) {
                                mCurrentY = 0;
                                scrollTopTo(mCurrentY);
                            } else {
                                mCurrentY -= step;
                                scrollTopTo(mCurrentY);
                            }
                        } else if (!isToBottom && mListView.getTop() == 0) {//上滑，滑动顶部
                            if (!pullTag) {
                                return super.onTouchEvent(ev);
                            }

                        }

                        return true;
                    } else if (isBottom
                            && mListView.getBottom() <= parent.getHeight()) {//向上滑
                        if (!isToBottom && (parent.getHeight() - mListView.getBottom()) <= MAX_PULL_BOTTOM_HEIGHT) {
                            /*MotionEvent event = MotionEvent.obtain(ev);
                            ev.setAction(MotionEvent.ACTION_UP);
                            super.onTouchEvent(ev);*/
                            pullTag = true;
                            if (parent.getHeight() - mListView.getBottom() > layoutFooter.getHeight()) {
                                step = step / 2;
                            }

                            if ((mListView.getBottom() - step) < (parent.getHeight() - MAX_PULL_BOTTOM_HEIGHT)) {
                                mCurrentY = -MAX_PULL_BOTTOM_HEIGHT;
                                scrollBottomTo(mCurrentY);
                            } else {
                                mCurrentY -= step;
                                scrollBottomTo(mCurrentY);
                            }
                        } else if (isToBottom && (mListView.getBottom() < parent.getHeight())) {
                            if ((mListView.getBottom() + step) > parent.getHeight()) {
                                mCurrentY = 0;
                                scrollBottomTo(mCurrentY);
                            } else {
                                mCurrentY += step;
                                scrollBottomTo(mCurrentY);
                            }
                        } else if (isToBottom && mListView.getBottom() == parent.getHeight()) {
                            if (!pullTag) {
                                return super.onTouchEvent(ev);
                            }
                        }
                        return true;
                    }
                    break;
                }
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    pullTag = false;
                    if (mListView.getTop() > 0) {//ListView应该上移
                        if (mListView.getTop() > REFRESHING_TOP_HEIGHT) {//下拉超过Header的高度，动画到Header位置
                            animateTopTo(layoutHeader.getMeasuredHeight());
                            isRefreshing = true;
                            if (null != mOnPullHeightChangeListener) {
                                mOnPullHeightChangeListener.onRefreshing(true);
                            }
                        } else {
                            animateTopTo(0);//下拉不超过Header的高度，动画到ListView的顶部
                        }

                    } else if (mListView.getBottom() < parent.getHeight()) {//ListView应该下移
                        if ((parent.getHeight() - mListView.getBottom()) > REFRESHING_BOTTOM_HEIGHT) {
                            animateBottomTo(-layoutFooter.getMeasuredHeight());
                            isRefreshing = true;
                            if (null != mOnPullHeightChangeListener) {
                                mOnPullHeightChangeListener.onRefreshing(false);
                            }
                        } else {
                            animateBottomTo(0);
                        }
                    }

            }


            return super.onTouchEvent(ev);
        }

    };

    /**
     * 不断的更新当前的位置：在 Action_move中
     * @param y
     */
    public void scrollBottomTo(int y) {
        mListView.layout(mListView.getLeft(), y, mListView.getRight(),
                this.getMeasuredHeight() + y);
        if (null != mOnPullHeightChangeListener) {
            mOnPullHeightChangeListener.onBottomHeightChange(
                    layoutHeader.getHeight(), -y);
        }
    }

    public void animateBottomTo(final int y) {
        ValueAnimator animator = ValueAnimator.ofInt(mListView.getBottom() - this.getMeasuredHeight(), y);
        animator.setDuration(1000);
        animator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                // TODO Auto-generated method stub
                int frameValue = (Integer) animation.getAnimatedValue();
                mCurrentY = frameValue;
                scrollBottomTo(frameValue);
                if (frameValue == y) {
                    isAnimation = false;
                }
            }
        });
        isAnimation = true;
        animator.start();
    }

    /**
     * 不断的更新当前的位置：在 Action_move中
     * @param y
     */
    public void scrollTopTo(int y) {//通过layout实现ListView的滑动
        mListView.layout(mListView.getLeft(), y, mListView.getRight(),
                this.getMeasuredHeight() + y);
        if (null != mOnPullHeightChangeListener) {
            mOnPullHeightChangeListener.onTopHeightChange(
                    layoutHeader.getHeight(), y);
        }
    }


    public void animateTopTo(final int y) {
        ValueAnimator animator = ValueAnimator.ofInt(mListView.getTop(), y);
        animator.setDuration(300);
        animator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                // TODO Auto-generated method stub
                int frameValue = (Integer) animation.getAnimatedValue();
                mCurrentY = frameValue;
                scrollTopTo(frameValue);
                if (frameValue == y) {
                    isAnimation = false;
                }
            }
        });
        isAnimation = true;
        animator.start();
    }


    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        REFRESHING_TOP_HEIGHT = layoutHeader.getMeasuredHeight();
        REFRESHING_BOTTOM_HEIGHT = layoutFooter.getMeasuredHeight();

        MAX_PULL_TOP_HEIGHT = this.getMeasuredHeight();//the RelativeLayout height
        MAX_PULL_BOTTOM_HEIGHT = this.getMeasuredHeight();

    }

    @Override
    public void onFinishInflate() {

        mListView.setBackgroundColor(0xffffffff);
        mListView.setCacheColorHint(Color.TRANSPARENT);
        mListView.setVerticalScrollBarEnabled(false);
        mListView.setLayoutParams(new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        mListView.setOnScrollListener(this);
        this.addView(mListView);

        layoutHeader = (RelativeLayout) this.findViewById(R.id.layoutHeader);
        layoutFooter = (RelativeLayout) this.findViewById(R.id.layoutFooter);


        super.onFinishInflate();
    }


    public ListView getListView() {
        return this.mListView;
    }

    /**
     * ListView自动上滑到顶部或下滑到底部
     */
    public void pullUp() {
        isRefreshing = false;
        if (mListView.getTop() > 0) {
            animateTopTo(0);
        } else if (mListView.getBottom() < this.getHeight()) {
            animateBottomTo(0);
        }

    }


    @Override
    public void onScroll(AbsListView view, int firstVisibleItem,
                         int visibleItemCount, int totalItemCount) {
        // TODO Auto-generated method stub
        if (null != mOnScrollListener) {
            mOnScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
        }
        if (mListView.getCount() > 0) {
            if ((firstVisibleItem + visibleItemCount) == totalItemCount) {//滑到底部 上滑
                View lastItem = (View) mListView
                        .getChildAt(visibleItemCount - 1);
                if (null != lastItem) {

                    if (lastItem.getBottom() == mListView.getHeight()) {//滑到底部 lastItem.getBottom == mListView.getHeight
                        Log.e("my", lastItem.getBottom() + "");
                        isBottom = true;
                    } else {
                        isBottom = false;
                    }
                }
            } else {
                isBottom = false;
            }
        } else {
            isBottom = false;
        }

        if (mListView.getCount() > 0) {//ListView中有数据
            if (firstVisibleItem == 0) {
                View firstItem = mListView.getChildAt(0);
                if (null != firstItem) {
                    if (firstItem.getTop() == 0) {
                        isTop = true;
                    } else {
                        isTop = false;
                    }
                }
            } else {
                isTop = false;
            }
        } else {//ListView中没有数据
            isTop = true;
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // TODO Auto-generated method stub
        if (null != mOnScrollListener) {
            mOnScrollListener.onScrollStateChanged(view, scrollState);
        }
    }

    // listener call back
    public interface OnPullHeightChangeListener {
        public void onTopHeightChange(int headerHeight, int pullHeight);

        public void onBottomHeightChange(int footerHeight, int pullHeight);

        public void onRefreshing(boolean isTop);
    }
}
