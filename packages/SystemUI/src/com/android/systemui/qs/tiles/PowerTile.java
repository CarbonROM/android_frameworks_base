/*
 * Copyright (C) 2018 The Android Open Source Project
 * Copyright (C) 2018 Darío (darjwx) Jiménez
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

package com.android.systemui.qs.tiles;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.ServiceManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.LinearLayout;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.systemui.plugins.qs.DetailAdapter;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.qs.GlobalSetting;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.R;

public class PowerTile extends QSTileImpl<BooleanState> {
    private int mTileMode;
    private int mRebootMode;
    private String mPreference;
    private PowerManager mPowerManager;
    private final PowerDetailAdapter mDetailAdapter;

    public PowerTile(QSHost host) {
        super(host);
        mTileMode = 1;
        mRebootMode = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.QUICK_SETTINGS_REBOOT_OPTIONS, 1);
        mPowerManager = (PowerManager) mContext.getSystemService(mContext.POWER_SERVICE);
        mDetailAdapter = (PowerDetailAdapter) createDetailAdapter();
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public void handleSetListening(boolean listening) {

    }

    @Override
    public DetailAdapter getDetailAdapter() {
        return mDetailAdapter;
    }

    @Override
    protected DetailAdapter createDetailAdapter() {
        return new PowerDetailAdapter();
    }

    @Override
    public void handleClick() {
        mTileMode++;
        if (mTileMode >= 4) {
            mTileMode = 1;
        }
        refreshState();
    }

    @Override
    public void handleLongClick() {
        KeyguardManager km = (KeyguardManager) mContext.getSystemService(mContext.KEYGUARD_SERVICE);
        if (mTileMode == 1) {
            showDetail(false);
            mHost.collapsePanels();
            mPowerManager.shutdown(false, mPowerManager.SHUTDOWN_USER_REQUESTED, false);
        } else if (mTileMode == 2) {
            showDetail(false);
            if (!km.isKeyguardLocked()) {
                mPreference = preferenceSelected();
                mPowerManager.rebootCustom(mPreference);
            } else {
                mPowerManager.rebootCustom(null);
            }
        } else {
            showDetail(true);
        }
        refreshState();
    }

    public String preferenceSelected() {
        int selected = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.QUICK_SETTINGS_REBOOT_OPTIONS, 1);
        switch(selected) {
            case 2:
                return mPowerManager.REBOOT_RECOVERY;
            case 3:
                return mPowerManager.REBOOT_BOOTLOADER;
            case 1:
            default:
                return null;
          }
    }

    @Override
    public Intent getLongClickIntent() {
        return null;
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_power_title);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        if (mTileMode == 1) {
            state.label = mContext.getString(R.string.quick_settings_power_off);
            state.icon = ResourceIcon.get(R.drawable.ic_qs_power);
        } else if (mTileMode == 2) {
            if (mRebootMode == 1) {
                state.label = mContext.getString(R.string.global_action_reboot);
                state.icon = ResourceIcon.get(R.drawable.ic_qs_reboot);
            } else if (mRebootMode == 2) {
                state.label = mContext.getString(R.string.global_action_reboot_recovery);
                state.icon = ResourceIcon.get(R.drawable.ic_qs_reboot);
            } else if (mRebootMode == 3) {
                state.label = mContext.getString(R.string.global_action_reboot_bootloader);
                state.icon = ResourceIcon.get(R.drawable.ic_qs_reboot);
            }
        } else {
            state.label = mContext.getString(R.string.quick_settings_preferences);
            state.icon = ResourceIcon.get(R.drawable.ic_qs_preferences);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.CARBONFIBERS;
    }

    private class PowerDetailAdapter implements DetailAdapter, View.OnClickListener {

        @Override
        public CharSequence getTitle() {
            return mContext.getString(R.string.quick_settings_power_details_title);
        }

        @Override
        public int getMetricsCategory() {
            return MetricsEvent.CARBONFIBERS;
        }

        @Override
        public Intent getSettingsIntent() {
            return null;
        }

        @Override
        public Boolean getToggleState() {
            return null;
        }

        @Override
        public void setToggleState(boolean state) {
        }

        @Override
        public View createDetailView(Context context, View convertView, ViewGroup parent) {
            LinearLayout mDetails = convertView != null ? (LinearLayout) convertView
                    : (LinearLayout) LayoutInflater.from(context).inflate(
                    R.layout.qs_power_detail_view, parent, false);
            RadioButton radio_system = mDetails.findViewById(R.id.radio_system);
            radio_system.setOnClickListener(this);
            RadioButton radio_recovery = mDetails.findViewById(R.id.radio_recovery);
            radio_recovery.setOnClickListener(this);
            RadioButton radio_bootloader = mDetails.findViewById(R.id.radio_bootloader);
            radio_bootloader.setOnClickListener(this);
            switch(mRebootMode) {
                case 1:
                    radio_system.toggle();
                    break;
                case 2:
                    radio_recovery.toggle();
                    break;
              case 3:
                    radio_bootloader.toggle();
                    break;
              default:
                    break;
            }
            return mDetails;
        }

        @Override
        public void onClick(View v) {
            showDetail(false);
            switch (v.getId()) {
                case R.id.radio_system:
                    mRebootMode = 1;
                    Settings.System.putInt(mContext.getContentResolver(),
                        Settings.System.QUICK_SETTINGS_REBOOT_OPTIONS, mRebootMode);
                    mTileMode = 2;
                    break;
                case R.id.radio_recovery:
                    mRebootMode = 2;
                    Settings.System.putInt(mContext.getContentResolver(),
                        Settings.System.QUICK_SETTINGS_REBOOT_OPTIONS, mRebootMode);
                    mTileMode = 2;
                    break;
                case R.id.radio_bootloader:
                    mRebootMode = 3;
                    Settings.System.putInt(mContext.getContentResolver(),
                        Settings.System.QUICK_SETTINGS_REBOOT_OPTIONS, mRebootMode);
                    mTileMode = 2;
                    break;
                default:
                    break;
            }
            refreshState();
        }
    }
}
