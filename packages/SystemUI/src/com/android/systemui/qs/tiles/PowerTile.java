/*
 * Copyright (C) 2018 The Android Open Source Project
 * Copyright (C) 2018 Darío Jiménez (darjwx)
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

import android.content.Intent;
import android.service.quicksettings.Tile;
import android.os.PowerManager;
import android.os.ServiceManager;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.qs.GlobalSetting;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.R;

public class PowerTile extends QSTileImpl<BooleanState> {

    private boolean mTileMode = true;

    public PowerTile(QSHost host) {
        super(host);
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public void handleSetListening(boolean listening) {
    }

    @Override
    public void handleClick() {
        mTileMode = !mTileMode;
        refreshState();
    }

    @Override
    public void handleLongClick() {
        mHost.collapsePanels();
        PowerManager pm = (PowerManager) mContext.getSystemService(mContext.POWER_SERVICE);
        if (mTileMode) {
            pm.shutdown(false, pm.SHUTDOWN_USER_REQUESTED, false);
        } else {
            pm.reboot(pm.REBOOT_REQUESTED_BY_DEVICE_OWNER);
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
        if (mTileMode) {
            state.label = mContext.getString(R.string.quick_settings_power_off);
            state.icon = ResourceIcon.get(R.drawable.ic_qs_power);
        } else {
            state.label = mContext.getString(R.string.quick_settings_reboot);
            state.icon = ResourceIcon.get(R.drawable.ic_qs_reboot);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.CARBONFIBERS;
    }

}
