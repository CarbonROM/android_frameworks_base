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

package com.android.systemui.qs.tiles;

import android.content.Intent;
import android.net.Uri;
import android.service.quicksettings.Tile;
import android.widget.Toast;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.qs.QSHost;
import com.android.systemui.R;
import com.android.systemui.SysUIToast;

public class CarbonTile extends QSTileImpl<BooleanState> {

    private final Icon mIcon = ResourceIcon.get(R.drawable.ic_qs_carbon);

    private static final Intent CARBONROM_WEBSITE =
        new Intent(Intent.ACTION_VIEW,Uri.parse("https://carbonrom.org"));

    public CarbonTile(QSHost host) {
        super(host);
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public void handleClick() {
        mHost.collapsePanels();
        SysUIToast.makeText(mContext, mContext.getString(
            R.string.quick_settings_carbon_toast),
            Toast.LENGTH_SHORT).show();
        refreshState();
    }

    @Override
    public Intent getLongClickIntent() {
        return CARBONROM_WEBSITE;
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.label = mContext.getString(R.string.quick_settings_carbon_label);
        state.icon = ResourceIcon.get(R.drawable.ic_qs_carbon);
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_carbon_label);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.CARBONFIBERS;
    }

    @Override
    public void handleSetListening(boolean listening) {
    }
}
