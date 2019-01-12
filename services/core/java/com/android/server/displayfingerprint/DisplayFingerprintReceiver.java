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

package com.android.server.displayfingerprint;

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

public class DisplayFingerprintReceiver extends BroadcastReceiver {
    private static final String TAG = "DisplayFingerprintReceiver";
    private Handler mHandler = new Handler();
    private SettingsObserver mSettingsObserver;
    private Context mContext;
    private boolean mDisplayFingerprintEnable;
    private boolean mDisplayFingerprintRunning = false;

    private class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.DISPLAY_FINGERPRINT_SENSOR_OVERLAY_ENABLE),
                    false, this, UserHandle.USER_CURRENT);
            update();
        }

        @Override
        public void onChange(boolean selfChange) {
            update();
        }

        public void update() {
            Intent displayFingerprint = new Intent(mContext,
                    com.android.systemui.displayfingerprint.DisplayFingerprintService.class);
            ContentResolver resolver = mContext.getContentResolver();

            mDisplayFingerprintEnable = Settings.System.getIntForUser(
                    resolver, Settings.System.DISPLAY_FINGERPRINT_SENSOR_OVERLAY_ENABLE,
                    0, UserHandle.USER_CURRENT);

            if(mDisplayFingerprintEnable) {
                mContext.startService(displayFingerprint);
                mDisplayFingerprintRunning = true;
                Log.d(TAG, "Started DisplayFingerprintService");
            } else {
                mContext.startService(displayFingerprint);
                mDisplayFingerprintRunning = true;
                Log.d(TAG, "Started DisplayFingerprintService");
            }

        }
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        mContext = context;
        try {
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