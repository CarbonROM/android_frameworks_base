/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.systemui.tuner;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.hardware.display.AmbientDisplayConfiguration;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.SwitchPreference;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.systemui.R;
import com.android.systemui.shared.plugins.PluginPrefs;

public class StatusbarIconsFragment extends TunerFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "StatusbarIconsFragment";
    private static final String STATUSBAR_CLOCKICON_PREFERENCE = "statusbar_clock";
    private static final String STATUSBAR_BATTERYICON_PREFERENCE = "statusbar_battery";

    private static final int BATTERY_STYLE_PORTRAIT = 0;
    private static final int BATTERY_STYLE_TEXT = 4;
    private static final int BATTERY_PERCENT_HIDDEN = 0;

    private ContentResolver mContentResolver;
    private SwitchPreference mStatusBarClock;
    private SwitchPreference mStatusBarBattery;
    private int mBatteryIconStyleValue;
    private int mBatteryIconStyleValuePrev;
    private int mBatteryPercentValue;
    private int mBatteryPercentValuePrev;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.statusbar_icon_settings);
        mContentResolver = getContext().getContentResolver();

        mStatusBarClock = (SwitchPreference) findPreference(STATUSBAR_CLOCKICON_PREFERENCE);
        mStatusBarClock.setChecked((Settings.System.getInt(
                mContentResolver,
                Settings.System.STATUSBAR_CLOCK, 1) == 1));
        mStatusBarClock.setOnPreferenceChangeListener(this);

        mBatteryIconStyleValue = Settings.System.getIntForUser(mContentResolver,
                Settings.System.STATUS_BAR_BATTERY_STYLE, BATTERY_STYLE_PORTRAIT, UserHandle.USER_CURRENT);
        mBatteryIconStyleValuePrev = Settings.System.getIntForUser(mContentResolver,
                Settings.System.STATUS_BAR_BATTERY_STYLE + "_prev", BATTERY_STYLE_PORTRAIT, UserHandle.USER_CURRENT);

        mBatteryPercentValue = Settings.System.getIntForUser(mContentResolver,
                Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, BATTERY_PERCENT_HIDDEN, UserHandle.USER_CURRENT);
        mBatteryPercentValuePrev = Settings.System.getIntForUser(mContentResolver,
                Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT + "_prev", BATTERY_PERCENT_HIDDEN, UserHandle.USER_CURRENT);

        mStatusBarBattery = (SwitchPreference) findPreference(STATUSBAR_BATTERYICON_PREFERENCE);
        boolean isBatteryIconHidden = mBatteryIconStyleValue == BATTERY_STYLE_TEXT && mBatteryPercentValue == BATTERY_PERCENT_HIDDEN;
        mStatusBarBattery.setChecked(!isBatteryIconHidden);
        mStatusBarBattery.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mStatusBarClock) {
            Settings.System.putInt(mContentResolver,
                    Settings.System.STATUSBAR_CLOCK,
                    (Boolean) newValue ? 1 : 0);
            return true;
        } else if (preference == mStatusBarBattery) {
            if ((Boolean) newValue) {
                Settings.System.putIntForUser(mContentResolver,
                    Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, mBatteryPercentValuePrev, UserHandle.USER_CURRENT);
                mBatteryPercentValue = mBatteryPercentValuePrev;
                Settings.System.putIntForUser(mContentResolver,
                    Settings.System.STATUS_BAR_BATTERY_STYLE, mBatteryIconStyleValuePrev, UserHandle.USER_CURRENT);
                mBatteryIconStyleValue = mBatteryIconStyleValuePrev;
            } else {
                Settings.System.putIntForUser(mContentResolver,
                    Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT + "_prev", mBatteryPercentValue,
                    UserHandle.USER_CURRENT);
                mBatteryPercentValuePrev = mBatteryPercentValue;
                Settings.System.putIntForUser(mContentResolver,
                    Settings.System.STATUS_BAR_BATTERY_STYLE + "_prev", mBatteryIconStyleValue,
                    UserHandle.USER_CURRENT);
                mBatteryIconStyleValuePrev = mBatteryIconStyleValue;

                mBatteryPercentValue = BATTERY_PERCENT_HIDDEN;
                mBatteryIconStyleValue = BATTERY_STYLE_TEXT;

                Settings.System.putIntForUser(mContentResolver,
                    Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, mBatteryPercentValue, UserHandle.USER_CURRENT);
                Settings.System.putIntForUser(mContentResolver,
                    Settings.System.STATUS_BAR_BATTERY_STYLE, mBatteryIconStyleValue, UserHandle.USER_CURRENT);
            }
            return true;
        }

        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.statusbar_icons_blacklist);

        MetricsLogger.visibility(getContext(), MetricsEvent.TUNER, true);
    }
}
