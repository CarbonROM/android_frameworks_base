/*
 * Copyright (C) 2017 ABC rom
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

import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.UserHandle;
import android.util.Log;
import android.provider.Settings;
import android.service.quicksettings.Tile;

import com.android.internal.util.cr.CrUtils;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.systemui.qs.QSHost;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.R;

/** Quick settings tile: Screenshot **/
public class ScreenshotTile extends QSTileImpl<BooleanState> {

    private static final String TAG = "ScreenshotTile";
    private int currTileSelection;


    public ScreenshotTile(QSHost host) {
        super(host);
        currTileSelection = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.SCREENSHOT_DEFAULT_MODE, 0, UserHandle.USER_CURRENT);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.QS_SCREENSHOT_TILE;
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public void handleSetListening(boolean listening) {}

    @Override
    protected void handleClick() {
        currTileSelection++;
        if(currTileSelection > 3)
            currTileSelection = 1;

        Settings.System.putIntForUser(mContext.getContentResolver(),
                Settings.System.SCREENSHOT_DEFAULT_MODE, currTileSelection,
                UserHandle.USER_CURRENT);

        refreshState();
    }

    @Override
    public void handleLongClick() {
        Log.w("SystemUI", "Long click on # " + currTileSelection);
        mHost.collapsePanels();

        //finish collapsing the panel
        try {
             Thread.sleep(1500);
        } catch (InterruptedException ie) {
             // Do nothing
        }
        CrUtils.takeScreenshot(currTileSelection);
    }

    @Override
    public Intent getLongClickIntent() {
        return null;
    }


    @Override
    protected void handleUserSwitch(int newUserId) {
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_screenshot_label);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        if (currTileSelection == 1) {
            state.label = mContext.getString(R.string.quick_settings_screenshot_label);
            state.icon = ResourceIcon.get(R.drawable.ic_qs_screenshot);
            state.contentDescription = mContext.getString(
                    R.string.quick_settings_screenshot_label);
        } else if (currTileSelection == 2) {
            state.label = mContext.getString(R.string.quick_settings_region_screenshot_label);
            state.icon = ResourceIcon.get(R.drawable.ic_qs_region_screenshot);
            state.contentDescription = mContext.getString(
                    R.string.quick_settings_region_screenshot_label);

        } else if (currTileSelection == 3) {
            Log.w("System UI", "extended screenshot tile shown");
            state.label = mContext.getString(R.string.quick_settings_extended_screenshot_label);
            state.icon = ResourceIcon.get(R.drawable.ic_qs_extended_screenshot);
            state.contentDescription = mContext.getString(
                    R.string.quick_settings_extended_screenshot_label);
        }
    }
}