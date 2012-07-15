/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.widget;

import android.content.Context;
import android.preference.Preference;
import android.view.View;
import android.view.ViewGroup;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import com.android.settings.R;

public class Md5Preference extends Preference {
    private static final boolean DEBUG = true;
    private static final String TAG = "Md5Preference";
    private static final String NULL = "Null";
    private float mDensity = 0;

    public Md5Preference(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (DEBUG) Log.d(TAG, "FileInfoPrefenece Object created");
    }

    LinearLayout widgetFrameView;
    ImageView iView;
    View mView;

    @Override
    protected void onBindView(View view) {
        mView = view;
        super.onBindView(view);
        mDensity = getContext().getResources().getDisplayMetrics().density;
    }

    public void isMatch(boolean match_) {
        if (mView == null)
            return;

        iView = new ImageView(getContext());
        if (widgetFrameView == null)
            return;
        widgetFrameView = ((LinearLayout) mView
                .findViewById(android.R.id.widget_frame));

        widgetFrameView.setVisibility(View.VISIBLE);
        widgetFrameView.setPadding(
                widgetFrameView.getPaddingLeft(),
                widgetFrameView.getPaddingTop(),
                ((int) mDensity * 8),
                widgetFrameView.getPaddingBottom()
                );
        // remove old result
        int count = widgetFrameView.getChildCount();
        if (count > 0) {
            widgetFrameView.removeViews(0, count);
        }

        widgetFrameView.setMinimumWidth(0);
        iView.setImageResource(match_ ? R.drawable.ors_match : R.drawable.ors_fail);

        // move pass/fail icon to the left else
        // it gets pushed off the screen right
        widgetFrameView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT, 0.0f));

        widgetFrameView.addView(iView);
    }
}
