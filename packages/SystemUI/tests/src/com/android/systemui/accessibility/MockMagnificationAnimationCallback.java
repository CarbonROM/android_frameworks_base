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

package com.android.systemui.accessibility;

import android.os.RemoteException;
import android.view.accessibility.IRemoteMagnificationAnimationCallback;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class MockMagnificationAnimationCallback extends IRemoteMagnificationAnimationCallback.Stub {

    private final CountDownLatch mCountDownLatch;
    private final AtomicInteger mSuccessCount;
    private final AtomicInteger mFailedCount;

    MockMagnificationAnimationCallback(CountDownLatch countDownLatch) {
        mCountDownLatch = countDownLatch;
        mSuccessCount = new AtomicInteger();
        mFailedCount = new AtomicInteger();
    }

    public int getSuccessCount() {
        return mSuccessCount.get();
    }

    public int getFailedCount() {
        return mFailedCount.get();
    }

    @Override
    public void onResult(boolean success) throws RemoteException {
        mCountDownLatch.countDown();
        if (success) {
            mSuccessCount.getAndIncrement();
        } else {
            mFailedCount.getAndIncrement();
        }
    }
}