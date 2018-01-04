/*
 * Copyright (C) 2018 CarbonROM
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
import android.os.PowerManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

public class PowerSaveReceiver extends BroadcastReceiver {
    private static final String TAG = "SystemUIPowerSaveReceiver";
    private Handler mHandler = new Handler();
    private SettingsObserver mSettingsObserver;
    private Context mContext;
    private int mCurrentUserId;
    private boolean mSmartPixelsRunning;

    private class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.SMART_PIXELS_ON_POWER_SAVE),
                    false, this, UserHandle.USER_CURRENT);
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
            final PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            Intent smartPixels = new Intent(mContext, com.android.systemui.smartpixels.SmartPixelsService.class);
            if (Settings.System.getIntForUser(mContext.getContentResolver(), Settings.System.SMART_PIXELS_ON_POWER_SAVE,
               0, mCurrentUserId) != 0) {
                if (pm.isPowerSaveMode() && (Settings.System.getIntForUser(mContext.getContentResolver(), Settings.System.SMART_PIXELS_ENABLE, 0,
                  mCurrentUserId) == 0) && !mSmartPixelsRunning) {
                    mContext.startService(smartPixels);
                    mSmartPixelsRunning = true;
                    Log.d(TAG, "Started SmartPixels");
                } else if (!pm.isPowerSaveMode() && (Settings.System.getIntForUser(mContext.getContentResolver(), Settings.System.SMART_PIXELS_ENABLE, 0,
                  mCurrentUserId) == 0) && mSmartPixelsRunning){
                    mContext.stopService(smartPixels);
                    mSmartPixelsRunning = false;
                    Log.d(TAG, "Stopped SmartPixels");
                } else if (pm.isPowerSaveMode() && (Settings.System.getIntForUser(mContext.getContentResolver(), Settings.System.SMART_PIXELS_ENABLE, 0,
                  mCurrentUserId) == 0) && mSmartPixelsRunning){
                    mContext.stopService(smartPixels);
                    mContext.startService(smartPixels);
                    Log.d(TAG, "Restarted SmartPixels");
                }
            } else if ((Settings.System.getIntForUser(mContext.getContentResolver(), Settings.System.SMART_PIXELS_ON_POWER_SAVE, 0,
              mCurrentUserId) == 0) && mSmartPixelsRunning){
                mContext.stopService(smartPixels);
                mSmartPixelsRunning = false;
                Log.d(TAG, "Stopped SmartPixels");
            } else if ((Settings.System.getIntForUser(mContext.getContentResolver(), Settings.System.SMART_PIXELS_ON_POWER_SAVE, 0,
              mCurrentUserId) != 0) && mSmartPixelsRunning){
                mContext.stopService(smartPixels);
                mContext.startService(smartPixels);
                Log.d(TAG, "Restarted SmartPixels");
            }
        }
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        mCurrentUserId = ActivityManager.getCurrentUser();
        mSmartPixelsRunning = false;
        try {
            mContext = context;
            if (mSettingsObserver ==  null) {
                mSettingsObserver = new SettingsObserver(mHandler);
                mSettingsObserver.observe();
            }
            mSettingsObserver.update();
        } catch (Exception e) {
            Log.e(TAG, "Can't start load average service", e);
        }
    }
}

