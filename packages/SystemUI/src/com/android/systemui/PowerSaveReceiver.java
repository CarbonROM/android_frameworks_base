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

    private class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(
                    Settings.System.ECODISPLAY_ON_POWER_SAVE),
                    false, this, UserHandle.USER_CURRENT);
            update();
        }

        @Override
        public void onChange(boolean selfChange) {
            update();
        }

        public void update() {
            final PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            if (Settings.System.getIntForUser(mContext.getContentResolver(), Settings.System.ECODISPLAY_ON_POWER_SAVE,
               0, mCurrentUserId) != 0) {
                Intent ecoDisplay = new Intent(mContext, EcoDisplayService.class);
                if (pm.isPowerSaveMode() && (Settings.System.getIntForUser(mContext.getContentResolver(), Settings.System.ECODISPLAY_ENABLE, 0,
                  mCurrentUserId) == 0)) {
                    mContext.startService(ecoDisplay);
                    Log.d(TAG, "Started EcoDisplay");
                } else {
                    mContext.stopService(ecoDisplay);
                    Log.d(TAG, "Stopped EcoDisplay");
                }
            }
        }
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        mCurrentUserId = ActivityManager.getCurrentUser();
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

