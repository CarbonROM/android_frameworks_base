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
import android.service.quicksettings.Tile;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.systemui.plugins.qs.DetailAdapter;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.qs.GlobalSetting;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.R;

public class PowerTile extends QSTileImpl<BooleanState> {

    int mTileMode = 1;
    String mRebootMode;

    private final PowerDetailAdapter mDetailAdapter;

    public PowerTile(QSHost host) {
        super(host);

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
        mTileMode +=1;
        if (mTileMode == 4) {
          mTileMode = 1;
        }
        refreshState();
    }

    @Override
    public void handleLongClick() {
        PowerManager pm = (PowerManager) mContext.getSystemService(mContext.POWER_SERVICE);
        KeyguardManager km = (KeyguardManager) mContext.getSystemService(mContext.KEYGUARD_SERVICE);
        if (mTileMode == 1) {
          showDetail(false);
          mHost.collapsePanels();
          pm.shutdown(false, pm.SHUTDOWN_USER_REQUESTED, false);
        } else if (mTileMode == 2) {
          showDetail(false);
          if (!km.isKeyguardLocked()) {
            pm.rebootCustom(mRebootMode);
          } else {
            pm.rebootCustom(null);
          }
        } else {
          showDetail(true);
        }
        refreshState();
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
      PowerManager pm = (PowerManager) mContext.getSystemService(mContext.POWER_SERVICE);
        if (mTileMode == 1) {
            state.label = mContext.getString(R.string.quick_settings_power_off);
            state.icon = ResourceIcon.get(R.drawable.ic_qs_power);
        } else if (mTileMode == 2 && mRebootMode == null) {
            state.label = mContext.getString(R.string.quick_settings_reboot);
            state.icon = ResourceIcon.get(R.drawable.ic_qs_reboot);
        } else if (mTileMode == 2 && mRebootMode == pm.REBOOT_RECOVERY) {
            state.label = mContext.getString(R.string.quick_settings_recovery);
            state.icon = ResourceIcon.get(R.drawable.ic_qs_reboot);
        } else if (mTileMode == 2 && mRebootMode == pm.REBOOT_BOOTLOADER) {
            state.label = mContext.getString(R.string.quick_settings_bootloader);
            state.icon = ResourceIcon.get(R.drawable.ic_qs_reboot);
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
            final LinearLayout mDetails = convertView != null ? (LinearLayout) convertView
                    : (LinearLayout) LayoutInflater.from(context).inflate(
                    R.layout.qs_power_detail_view, parent, false);

            final Button button_system = mDetails.findViewById(R.id.button_system);
            button_system.setOnClickListener(this);
            final Button button_recovery = mDetails.findViewById(R.id.button_recovery);
            button_recovery.setOnClickListener(this);
            final Button button_bootloader = mDetails.findViewById(R.id.button_bootloader);
            button_bootloader.setOnClickListener(this);

            return mDetails;

        }

        @Override
        public void onClick(View v) {
            PowerManager pm = (PowerManager) mContext.getSystemService(mContext.POWER_SERVICE);

            showDetail(false);

            switch (v.getId()) {
                case R.id.button_system:
                    mRebootMode = null;
                    mTileMode = 2;
                    refreshState();
                break;
                case R.id.button_recovery:
                    mRebootMode = pm.REBOOT_RECOVERY;
                    mTileMode = 2;
                    refreshState();
                break;
                case R.id.button_bootloader:
                    mRebootMode = pm.REBOOT_BOOTLOADER;
                    mTileMode = 2;
                    refreshState();
                break;
                default:
                break;
            }

        }

    }

}
