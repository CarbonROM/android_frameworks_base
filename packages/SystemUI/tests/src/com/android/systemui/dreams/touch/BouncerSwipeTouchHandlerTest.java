/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.systemui.dreams.touch;

import static com.google.common.truth.Truth.assertThat;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.animation.ValueAnimator;
import android.testing.AndroidTestingRunner;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.VelocityTracker;

import androidx.test.filters.SmallTest;

import com.android.systemui.SysuiTestCase;
import com.android.systemui.shared.system.InputChannelCompat;
import com.android.systemui.statusbar.NotificationShadeWindowController;
import com.android.systemui.statusbar.phone.KeyguardBouncer;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;
import com.android.wm.shell.animation.FlingAnimationUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Random;

@SmallTest
@RunWith(AndroidTestingRunner.class)
public class BouncerSwipeTouchHandlerTest extends SysuiTestCase {
    @Mock
    StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;

    @Mock
    StatusBar mStatusBar;

    @Mock
    NotificationShadeWindowController mNotificationShadeWindowController;

    @Mock
    FlingAnimationUtils mFlingAnimationUtils;


    @Mock
    FlingAnimationUtils mFlingAnimationUtilsClosing;

    @Mock
    DreamTouchHandler.TouchSession mTouchSession;

    BouncerSwipeTouchHandler mTouchHandler;

    @Mock
    BouncerSwipeTouchHandler.ValueAnimatorCreator mValueAnimatorCreator;

    @Mock
    ValueAnimator mValueAnimator;

    @Mock
    BouncerSwipeTouchHandler.VelocityTrackerFactory mVelocityTrackerFactory;

    @Mock
    VelocityTracker mVelocityTracker;

    private static final float TOUCH_REGION = .3f;
    private static final float SCREEN_HEIGHT_PX = 100;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mTouchHandler = new BouncerSwipeTouchHandler(
                mStatusBarKeyguardViewManager,
                mStatusBar,
                mNotificationShadeWindowController,
                mValueAnimatorCreator,
                mVelocityTrackerFactory,
                mFlingAnimationUtils,
                mFlingAnimationUtilsClosing,
                TOUCH_REGION);
        when(mStatusBar.getDisplayHeight()).thenReturn(SCREEN_HEIGHT_PX);
        when(mValueAnimatorCreator.create(anyFloat(), anyFloat())).thenReturn(mValueAnimator);
        when(mVelocityTrackerFactory.obtain()).thenReturn(mVelocityTracker);
    }

    private static void beginValidSwipe(GestureDetector.OnGestureListener listener) {
        listener.onDown(MotionEvent.obtain(0, 0,
                MotionEvent.ACTION_DOWN, 0,
                SCREEN_HEIGHT_PX - (.5f * TOUCH_REGION * SCREEN_HEIGHT_PX), 0));
    }

    /**
     * Ensures expansion only happens when touch down happens in valid part of the screen.
     */
    @Test
    public void testSessionStart() {
        mTouchHandler.onSessionStart(mTouchSession);
        verify(mNotificationShadeWindowController).setForcePluginOpen(eq(true), any());
        ArgumentCaptor<InputChannelCompat.InputEventListener> eventListenerCaptor =
                ArgumentCaptor.forClass(InputChannelCompat.InputEventListener.class);
        ArgumentCaptor<GestureDetector.OnGestureListener> gestureListenerCaptor =
                ArgumentCaptor.forClass(GestureDetector.OnGestureListener.class);
        verify(mTouchSession).registerGestureListener(gestureListenerCaptor.capture());
        verify(mTouchSession).registerInputListener(eventListenerCaptor.capture());

        final Random random = new Random(System.currentTimeMillis());

        // If an initial touch down meeting criteria has been met, scroll behavior should be
        // ignored.
        assertThat(gestureListenerCaptor.getValue()
                .onScroll(Mockito.mock(MotionEvent.class),
                        Mockito.mock(MotionEvent.class),
                        random.nextFloat(),
                        random.nextFloat())).isFalse();

        // A touch at the top of the screen should also not trigger listening.
        final MotionEvent touchDownEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN,
                0, 0, 0);

        gestureListenerCaptor.getValue().onDown(touchDownEvent);
        assertThat(gestureListenerCaptor.getValue()
                .onScroll(Mockito.mock(MotionEvent.class),
                        Mockito.mock(MotionEvent.class),
                        random.nextFloat(),
                        random.nextFloat())).isFalse();

        // A touch within range at the bottom of the screen should trigger listening
        beginValidSwipe(gestureListenerCaptor.getValue());
        assertThat(gestureListenerCaptor.getValue()
                .onScroll(Mockito.mock(MotionEvent.class),
                        Mockito.mock(MotionEvent.class),
                        random.nextFloat(),
                        random.nextFloat())).isTrue();
    }

    /**
     * Makes sure expansion amount is proportional to scroll.
     */
    @Test
    public void testExpansionAmount() {
        mTouchHandler.onSessionStart(mTouchSession);
        ArgumentCaptor<GestureDetector.OnGestureListener> gestureListenerCaptor =
                ArgumentCaptor.forClass(GestureDetector.OnGestureListener.class);
        verify(mTouchSession).registerGestureListener(gestureListenerCaptor.capture());

        beginValidSwipe(gestureListenerCaptor.getValue());

        final float scrollAmount = .3f;
        final float distanceY = SCREEN_HEIGHT_PX * scrollAmount;

        final MotionEvent event1 = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE,
                0, SCREEN_HEIGHT_PX, 0);
        final MotionEvent event2 = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE,
                0, SCREEN_HEIGHT_PX  - distanceY, 0);

        assertThat(gestureListenerCaptor.getValue().onScroll(event1, event2, 0 , distanceY))
                .isTrue();

        // Ensure only called once
        verify(mStatusBarKeyguardViewManager)
                .onPanelExpansionChanged(anyFloat(), anyBoolean(), anyBoolean());

        // Ensure correct expansion passed in.
        verify(mStatusBarKeyguardViewManager)
                .onPanelExpansionChanged(eq(1 - scrollAmount), eq(false), eq(true));
    }

    private void swipeToPosition(float position, float velocityY) {
        mTouchHandler.onSessionStart(mTouchSession);
        ArgumentCaptor<GestureDetector.OnGestureListener> gestureListenerCaptor =
                ArgumentCaptor.forClass(GestureDetector.OnGestureListener.class);
        ArgumentCaptor<InputChannelCompat.InputEventListener> inputEventListenerCaptor =
                ArgumentCaptor.forClass(InputChannelCompat.InputEventListener.class);
        verify(mTouchSession).registerGestureListener(gestureListenerCaptor.capture());
        verify(mTouchSession).registerInputListener(inputEventListenerCaptor.capture());

        when(mVelocityTracker.getYVelocity()).thenReturn(velocityY);

        beginValidSwipe(gestureListenerCaptor.getValue());

        final float distanceY = SCREEN_HEIGHT_PX * position;

        final MotionEvent event1 = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE,
                0, SCREEN_HEIGHT_PX, 0);
        final MotionEvent event2 = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE,
                0, SCREEN_HEIGHT_PX  - distanceY, 0);

        assertThat(gestureListenerCaptor.getValue().onScroll(event1, event2, 0 , distanceY))
                .isTrue();

        final MotionEvent upEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP,
                0, 0, 0);

        inputEventListenerCaptor.getValue().onInputEvent(upEvent);
    }

    /**
     * Tests that ending a swipe before the set expansion threshold leads to bouncer collapsing
     * down.
     */
    @Test
    public void testCollapseOnThreshold() {
        final float swipeUpPercentage = .3f;
        swipeToPosition(swipeUpPercentage, -1);

        verify(mValueAnimatorCreator).create(eq(1 - swipeUpPercentage),
                eq(KeyguardBouncer.EXPANSION_VISIBLE));
        verify(mFlingAnimationUtilsClosing).apply(eq(mValueAnimator), anyFloat(), anyFloat(),
                anyFloat(), anyFloat());
        verify(mValueAnimator).start();
    }

    /**
     * Tests that ending a swipe above the set expansion threshold will continue the expansion.
     */
    @Test
    public void testExpandOnThreshold() {
        final float swipeUpPercentage = .7f;
        swipeToPosition(swipeUpPercentage, 1);

        verify(mValueAnimatorCreator).create(eq(1 - swipeUpPercentage),
                eq(KeyguardBouncer.EXPANSION_HIDDEN));
        verify(mFlingAnimationUtils).apply(eq(mValueAnimator), anyFloat(), anyFloat(),
                anyFloat(), anyFloat());
        verify(mValueAnimator).start();
    }
}