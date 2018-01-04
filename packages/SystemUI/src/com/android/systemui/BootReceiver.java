/*
 * Copyright (C) 2017 The OmniROM Project
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

package com.android.systemui;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

/**
 * Performs a number of miscellaneous, non-system-critical actions
 * after the system has finished booting.
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "SystemUIBootReceiver";
    private Handler mHandler = new Handler();
    private SettingsObserver mSettingsObserver;
    private Context mContext;
    private boolean mSmartPixelsRunning;

    private class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                   Settings.System.SMART_PIXELS_ENABLE),
                   false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                   Settings.System.SMART_PIXELS_PATTERN),
                   false, this, UserHandle.USER_CURRENT);
            resolver.registerContentObserver(Settings.System.getUriFor(
                   Settings.System.SMART_PIXELS_SHIFT_TIMEOUT),
                   false, this, UserHandle.USER_CURRENT);
            update();
        }

        @Override
        public void onChange(boolean selfChange) {
            update();
        }

        public void update() {
            Intent smartPixels = new Intent(mContext, com.android.systemui.smartpixels.SmartPixelsService.class);
            if (!mSmartPixelsRunning && (Settings.System.getIntForUser(mContext.getContentResolver(), Settings.System.SMART_PIXELS_ENABLE, 0,
              ActivityManager.getCurrentUser()) != 0)) {
                mContext.startService(smartPixels);
                mSmartPixelsRunning = true;
                Log.d(TAG, "Started SmartPixels");
            } else if (mSmartPixelsRunning && (Settings.System.getIntForUser(mContext.getContentResolver(), Settings.System.SMART_PIXELS_ENABLE, 0,
              ActivityManager.getCurrentUser()) == 0)) {
                mContext.stopService(smartPixels);
                mSmartPixelsRunning = false;
                Log.d(TAG, "Stopped SmartPixels");
            } else if (mSmartPixelsRunning && (Settings.System.getIntForUser(mContext.getContentResolver(), Settings.System.SMART_PIXELS_ENABLE, 0,
              ActivityManager.getCurrentUser()) != 0)) {
                mContext.stopService(smartPixels);
                mContext.startService(smartPixels);
                Log.d(TAG, "Restarted SmartPixels");
            }
        }
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        mSmartPixelsRunning = false;
        try {
            mContext = context;
            if (mSettingsObserver ==  null) {
                mSettingsObserver = new SettingsObserver(mHandler);
                mSettingsObserver.observe();
            }

        } catch (Exception e) {
            Log.e(TAG, "Can't start load average service", e);
        }
    }
}

