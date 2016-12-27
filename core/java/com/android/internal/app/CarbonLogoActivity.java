/*
 * Copyright (C) 2010 The Android Open Source Project
 * This code has been modified. Portions copyright (C) 2013, ParanoidAndroid Project.
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

package com.android.internal.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.provider.Settings;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemProperties;
import android.text.method.AllCapsTransformationMethod;
import android.text.method.TransformationMethod;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class CarbonLogoActivity extends Activity {
    FrameLayout mContent;
    int mCount;
    final Handler mHandler = new Handler();
    final static int SOLID_BGCOLOR = 0xFF1F1F1F;
    final static int CLEAR_BGCOLOR = 0xC0000000;
    final static int TEXT_COLOR = 0xFFFFFFFF;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        Typeface condensed = Typeface.create("sans-serif-condensed", Typeface.NORMAL);

        mContent = new FrameLayout(this);
        mContent.setBackgroundColor(CLEAR_BGCOLOR);

        final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;

        final ImageView bglogo = new ImageView(this);
        bglogo.setImageResource(com.android.internal.R.drawable.carbon_bg_logo);
        bglogo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        bglogo.setVisibility(View.INVISIBLE);

        final ImageView logo = new ImageView(this);
        logo.setImageResource(com.android.internal.R.drawable.carbon_logo);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        logo.setVisibility(View.INVISIBLE);

        final View bg = new View(this);
        bg.setBackgroundColor(SOLID_BGCOLOR);
        bg.setAlpha(0f);

        final int p = (int)(4 * metrics.density);

        final TextView tv = new TextView(this);
        if (condensed != null) tv.setTypeface(condensed);
        tv.setTextSize(20);
        tv.setPadding(p, p, p, p);
        tv.setTextColor(TEXT_COLOR);
        tv.setGravity(Gravity.CENTER);
        tv.setTransformationMethod(new AllCapsTransformationMethod(this));
        tv.setText("Elegant - CarbonROM - Smooth");
        tv.setVisibility(View.INVISIBLE);

        mContent.addView(bg);
        mContent.addView(bglogo, lp);
        mContent.addView(logo, lp);

        final FrameLayout.LayoutParams lp2 = new FrameLayout.LayoutParams(lp);
        lp2.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        lp2.bottomMargin = p;

        mContent.addView(tv, lp2);

        if (logo.getVisibility() != View.VISIBLE) {
            bg.setScaleX(0.01f);
            bg.animate().alpha(1f).scaleX(1f).setStartDelay(500).start();
            bglogo.setAlpha(0f);
            bglogo.setVisibility(View.VISIBLE);
            bglogo.setScaleX(0.5f);
            bglogo.setScaleY(0.5f);
            bglogo.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(600).setStartDelay(500)
                .start();
            logo.setAlpha(0f);
            logo.setVisibility(View.VISIBLE);
            logo.setScaleX(0.5f);
            logo.setScaleY(0.5f);
            logo.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(1000).setStartDelay(800)
                .setInterpolator(new AnticipateOvershootInterpolator())
                .start();
            tv.setAlpha(0f);
            tv.setVisibility(View.VISIBLE);
            tv.animate().alpha(1f).setDuration(1000).setStartDelay(1100).start();
        }

        logo.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                try {
                    startActivity(new Intent(Intent.ACTION_MAIN)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
                            | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                        .addCategory("com.android.internal.category.PLATLOGO"));
                } catch (ActivityNotFoundException ex) {
                    android.util.Log.e("CarbonLogoActivity", "Couldn't catch a break.");
                }
                finish();
                return true;
            }
        });

        setContentView(mContent);
    }
}
