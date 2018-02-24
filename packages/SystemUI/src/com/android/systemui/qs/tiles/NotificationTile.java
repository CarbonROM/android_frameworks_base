/*
 * Copyright (C) 2018 CarbonROM
 * Copyright (C) 2018 Adin Kwok (adinkwok)
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
import android.provider.Settings;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.service.quicksettings.Tile;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.qs.GlobalSetting;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.R;

public class NotificationTile extends QSTileImpl<BooleanState> {

    private final Icon mIcon = ResourceIcon.get(R.drawable.ic_qs_headsup_enabled);

    public NotificationTile(QSHost host) {
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
        changeNotificationStyle();
        updateHeadsUp();
        refreshState();
    }

    @Override
    public void handleLongClick() {
        int mTickerMode = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_SHOW_TICKER, 1);
        if (mTickerMode == 0) {
            changeNotificationStyle();
        } else if (mTickerMode < 3) {
            Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_SHOW_TICKER, ++mTickerMode);
        } else {
            Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_SHOW_TICKER, 1);
        }
        updateHeadsUp();
        refreshState();
    }

    @Override
    public Intent getLongClickIntent() {
        return null;
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        int mTickerMode = Settings.System.getInt(mContext.getContentResolver(),
                 Settings.System.STATUS_BAR_SHOW_TICKER, 1);
        switch (mTickerMode) {
            case 0:  state.label = mContext.getString(R.string.quick_settings_notify_disabled);
                     state.icon  = ResourceIcon.get(R.drawable.ic_qs_notify_disabled);
                     break;
            case 1:  state.label = mContext.getString(R.string.quick_settings_headsup_enabled);
                     state.icon  = ResourceIcon.get(R.drawable.ic_qs_headsup_enabled);
                     break;
            case 2:  state.label = mContext.getString(R.string.quick_settings_ticker_enabled);
                     state.icon  = ResourceIcon.get(R.drawable.ic_qs_ticker_enabled);
                     break;
            case 3:  state.label = mContext.getString(R.string.quick_settings_headsup_ticker_enabled);
                     state.icon  = ResourceIcon.get(R.drawable.ic_qs_headsup_ticker_enabled);
                     break;
            default: state.label = mContext.getString(R.string.quick_settings_notification_style);
                     state.icon  = ResourceIcon.get(R.drawable.ic_qs_headsup_enabled);
                     break;
        }
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_notification_style);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.CARBONFIBERS;
    }

    private void changeNotificationStyle() {
        int mTickerMode = Settings.System.getInt(mContext.getContentResolver(),
                 Settings.System.STATUS_BAR_SHOW_TICKER, 1);
        int mLastTickerMode = Settings.System.getInt(mContext.getContentResolver(),
                 Settings.System.STATUS_BAR_OLD_SHOW_TICKER, 1);
        if (mTickerMode > 0) {
            // save integer and disable ticker
            Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_OLD_SHOW_TICKER, mTickerMode);
            Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_SHOW_TICKER, 0);
        } else {
            // use saved setting
            Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_SHOW_TICKER, mLastTickerMode);
        }
    }

    private void updateHeadsUp() {
        boolean mShouldHideHeadsUp = (Settings.System.getInt(mContext.getContentResolver(),
                 Settings.System.STATUS_BAR_SHOW_TICKER, 1) == 0) ||
                 (Settings.System.getInt(mContext.getContentResolver(),
                 Settings.System.STATUS_BAR_SHOW_TICKER, 1) == 2);
        Settings.Global.putInt(mContext.getContentResolver(),
                 Settings.Global.HEADS_UP_NOTIFICATIONS_ENABLED,
                 mShouldHideHeadsUp ? 0 : 1);
    }
}
