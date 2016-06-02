/**
 * Copyright 2014 Magnus Woxblom
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.woxthebox.draglistview;

import android.content.Context;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

public class DragListView extends FrameLayout {

    public interface DragListListener {
        void onItemDragStarted(int position);

        void onItemDragging(int itemPosition, float x, float y);

        void onItemDragEnded(int fromPosition, int toPosition);
    }

    public static abstract class DragListListenerAdapter implements DragListListener {
        @Override
        public void onItemDragStarted(int position) {
        }

        @Override
        public void onItemDragging(int itemPosition, float x, float y) {
        }

        @Override
        public void onItemDragEnded(int fromPosition, int toPosition) {
        }
    }

    public interface DragListCallback {
        boolean canDragItemAtPosition(int dragPosition);

        boolean canDropItemAtPosition(int dropPosition);
    }

    public static abstract class DragListCallbackAdapter implements DragListCallback {
        @Override
        public boolean canDragItemAtPosition(int dragPosition) {
            return true;
        }

        @Override
        public boolean canDropItemAtPosition(int dropPosition) {
            return true;
        }
    }

    private DragItemRecyclerView mRecyclerView;
    private DragListListener mDragListListener;
    private DragListCallback mDragListCallback;
    private DragItem mDragItem;
    private float mTouchX;
    private float mTouchY;

    public DragListView(Context context) {
        super(context);
    }

    public DragListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DragListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mDragItem = new DragItem(getContext());
        mRecyclerView = createRecyclerView();
        mRecyclerView.setDragItem(mDragItem);
        addView(mRecyclerView);
        addView(mDragItem.getDragItemView());
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        boolean retValue = handleTouchEvent(event);
        return retValue || super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean retValue = handleTouchEvent(event);
        return retValue || super.onTouchEvent(event);
    }

    private boolean handleTouchEvent(MotionEvent event) {
        mTouchX = event.getX();
        mTouchY = event.getY();
        if (isDragging()) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    mRecyclerView.onDragging(event.getX(), event.getY());
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mRecyclerView.onDragEnded();
                    break;
            }
            return true;
        }
        return false;
    }

    private DragItemRecyclerView createRecyclerView() {
        final DragItemRecyclerView recyclerView = (DragItemRecyclerView) LayoutInflater.from(getContext()).inflate(R.layout.drag_item_recycler_view, this, false);
        recyclerView.setMotionEventSplittingEnabled(false);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setVerticalScrollBarEnabled(false);
        recyclerView.setHorizontalScrollBarEnabled(false);
        recyclerView.setDragItemListener(new DragItemRecyclerView.DragItemListener() {
            private int mDragStartPosition;

            @Override
            public void onDragStarted(int itemPosition, float x, float y) {
                getParent().requestDisallowInterceptTouchEvent(true);
                mDragStartPosition = itemPosition;
                if (mDragListListener != null) {
                    mDragListListener.onItemDragStarted(itemPosition);
                }
            }

            @Override
            public void onDragging(int itemPosition, float x, float y) {
                if (mDragListListener != null) {
                    mDragListListener.onItemDragging(itemPosition, x, y);
                }
            }

            @Override
            public void onDragEnded(int newItemPosition) {
                if (mDragListListener != null) {
                    mDragListListener.onItemDragEnded(mDragStartPosition, newItemPosition);
                }
            }
        });
        recyclerView.setDragItemCallback(new DragItemRecyclerView.DragItemCallback() {
            @Override
            public boolean canDragItemAtPosition(int dragPosition) {
                if (mDragListCallback != null) {
                    return mDragListCallback.canDragItemAtPosition(dragPosition);
                }
                return true;
            }

            @Override
            public boolean canDropItemAtPosition(int dropPosition) {
                if (mDragListCallback != null) {
                    return mDragListCallback.canDropItemAtPosition(dropPosition);
                }
                return true;
            }
        });
        return recyclerView;
    }

    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    public DragItemAdapter getAdapter() {
        if (mRecyclerView != null) {
            return (DragItemAdapter) mRecyclerView.getAdapter();
        }
        return null;
    }

    public void setAdapter(DragItemAdapter adapter, boolean hasFixedItemSize) {
        mRecyclerView.setHasFixedSize(hasFixedItemSize);
        mRecyclerView.setAdapter(adapter);
        adapter.setDragStartedListener(new DragItemAdapter.DragStartCallback() {
            @Override
            public boolean startDrag(View itemView, long itemId) {
                return mRecyclerView.startDrag(itemView, itemId, mTouchX, mTouchY);
            }

            @Override
            public boolean isDragging() {
                return mRecyclerView.isDragging();
            }
        });
    }

    public void setLayoutManager(RecyclerView.LayoutManager layout) {
        mRecyclerView.setLayoutManager(layout);
    }

    public void setDragListListener(DragListListener listener) {
        mDragListListener = listener;
    }

    public void setDragListCallback(DragListCallback callback) {
        mDragListCallback = callback;
    }

    public boolean isDragEnabled() {
        return mRecyclerView.isDragEnabled();
    }

    public void setDragEnabled(boolean enabled) {
        mRecyclerView.setDragEnabled(enabled);
    }

    public void setCustomDragItem(DragItem dragItem) {
        removeViewAt(1);

        DragItem newDragItem;
        if (dragItem != null) {
            newDragItem = dragItem;
        } else {
            newDragItem = new DragItem(getContext());
        }

        newDragItem.setCanDragHorizontally(mDragItem.canDragHorizontally());
        newDragItem.setSnapToTouch(mDragItem.isSnapToTouch());
        mDragItem = newDragItem;
        mRecyclerView.setDragItem(mDragItem);
        addView(mDragItem.getDragItemView());
    }

    public boolean isDragging() {
        return mRecyclerView.isDragging();
    }

    public void setCanDragHorizontally(boolean canDragHorizontally) {
        mDragItem.setCanDragHorizontally(canDragHorizontally);
    }

    public void setSnapDragItemToTouch(boolean snapToTouch) {
        mDragItem.setSnapToTouch(snapToTouch);
    }

    public void setCanNotDragAboveTopItem(boolean canNotDragAboveTop) {
        mRecyclerView.setCanNotDragAboveTopItem(canNotDragAboveTop);
    }

    public void setCanNotDragBelowBottomItem(boolean canNotDragBelowBottom) {
        mRecyclerView.setCanNotDragBelowBottomItem(canNotDragBelowBottom);
    }

    public void setScrollingEnabled(boolean scrollingEnabled) {
        mRecyclerView.setScrollingEnabled(scrollingEnabled);
    }
}
