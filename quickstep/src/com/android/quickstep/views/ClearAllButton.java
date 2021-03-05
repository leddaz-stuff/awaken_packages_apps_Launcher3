/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.quickstep.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.widget.Button;

import com.android.launcher3.touch.PagedOrientationHandler;
import com.android.quickstep.views.RecentsView.PageCallbacks;
import com.android.quickstep.views.RecentsView.ScrollState;

public class ClearAllButton extends Button implements PageCallbacks {

    public static final FloatProperty<ClearAllButton> VISIBILITY_ALPHA =
            new FloatProperty<ClearAllButton>("visibilityAlpha") {
                @Override
                public Float get(ClearAllButton view) {
                    return view.mVisibilityAlpha;
                }

                @Override
                public void setValue(ClearAllButton view, float v) {
                    view.setVisibilityAlpha(v);
                }
            };

    private float mScrollAlpha = 1;
    private float mContentAlpha = 1;
    private float mVisibilityAlpha = 1;
    private float mGridProgress = 1;

    private boolean mIsRtl;
    private final float mOriginalTranslationX, mOriginalTranslationY;
    private float mNormalTranslationPrimary;
    private float mGridTranslationPrimary;

    private int mScrollOffset;

    public ClearAllButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        mIsRtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        mOriginalTranslationX = getTranslationX();
        mOriginalTranslationY = getTranslationY();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        PagedOrientationHandler orientationHandler = getRecentsView().getPagedOrientationHandler();
        mScrollOffset = orientationHandler.getClearAllScrollOffset(getRecentsView(), mIsRtl);
    }

    private RecentsView getRecentsView() {
        return (RecentsView) getParent();
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        mIsRtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    public void setContentAlpha(float alpha) {
        if (mContentAlpha != alpha) {
            mContentAlpha = alpha;
            updateAlpha();
        }
    }

    public void setVisibilityAlpha(float alpha) {
        if (mVisibilityAlpha != alpha) {
            mVisibilityAlpha = alpha;
            updateAlpha();
        }
    }

    @Override
    public void onPageScroll(ScrollState scrollState) {
        PagedOrientationHandler orientationHandler = getRecentsView().getPagedOrientationHandler();
        float orientationSize = orientationHandler.getPrimaryValue(getWidth(), getHeight());
        if (orientationSize == 0) {
            return;
        }

        float shift;
        if (mIsRtl) {
            shift = Math.min(scrollState.scrollFromEdge, orientationSize);
        } else {
            shift = Math.min(scrollState.scrollFromEdge,
                    orientationSize + getGridTrans(mGridTranslationPrimary))
                    - getGridTrans(mGridTranslationPrimary);
        }
        mNormalTranslationPrimary = mIsRtl ? (mScrollOffset - shift) : (mScrollOffset + shift);
        applyPrimaryTranslation();
        orientationHandler.getSecondaryViewTranslate().set(this,
                orientationHandler.getSecondaryValue(mOriginalTranslationX, mOriginalTranslationY));
        mScrollAlpha = 1 - shift / orientationSize;
        updateAlpha();
    }

    private void updateAlpha() {
        final float alpha = mScrollAlpha * mContentAlpha * mVisibilityAlpha;
        setAlpha(alpha);
        setClickable(Math.min(alpha, 1) == 1);
    }

    public void setGridTranslationPrimary(float gridTranslationPrimary) {
        mGridTranslationPrimary = gridTranslationPrimary;
        applyPrimaryTranslation();
    }

    public float getScrollAdjustment() {
        float scrollAdjustment = 0;
        if (mGridProgress > 0) {
            scrollAdjustment += mGridTranslationPrimary;
        }
        return scrollAdjustment;
    }

    public float getOffsetAdjustment() {
        return getScrollAdjustment();
    }

    /**
     * Moves ClearAllButton between carousel and 2 row grid.
     *
     * @param gridProgress 0 = carousel; 1 = 2 row grid.
     */
    public void setGridProgress(float gridProgress) {
        mGridProgress = gridProgress;
        applyPrimaryTranslation();
    }

    private void applyPrimaryTranslation() {
        RecentsView recentsView = getRecentsView();
        if (recentsView == null) {
            return;
        }

        PagedOrientationHandler orientationHandler = recentsView.getPagedOrientationHandler();
        orientationHandler.getPrimaryViewTranslate().set(this,
                mNormalTranslationPrimary + getGridTrans(mGridTranslationPrimary));
    }

    private float getGridTrans(float endTranslation) {
        return mGridProgress > 0 ? endTranslation : 0;
    }
}