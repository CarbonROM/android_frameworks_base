/*
 * Copyright (C) 2017 The LineageOS Project
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

import android.content.ContentResolver;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.MenuItem;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.SwitchPreference;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.systemui.R;

public class StatusBarTuner extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

    private static final String STATUSBAR_BATTERYICON_PREFERENCE = "statusbar_battery";

    private static final int BATTERY_STYLE_PORTRAIT = 0;
    private static final int BATTERY_STYLE_TEXT = 4;
    private static final int BATTERY_PERCENT_HIDDEN = 0;

    private ContentResolver mContentResolver;
    private SwitchPreference mStatusBarBattery;
    private int mBatteryIconStyleValue;
    private int mBatteryIconStyleValuePrev;
    private int mBatteryPercentValue;
    private int mBatteryPercentValuePrev;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.status_bar_prefs);

        mContentResolver = getContext().getContentResolver();

        mBatteryIconStyleValue = Settings.System.getIntForUser(mContentResolver,
                Settings.System.STATUS_BAR_BATTERY_STYLE, BATTERY_STYLE_PORTRAIT, UserHandle.USER_CURRENT);
        mBatteryIconStyleValuePrev = Settings.System.getIntForUser(mContentResolver,
                Settings.System.STATUS_BAR_BATTERY_STYLE_PREV, BATTERY_STYLE_PORTRAIT, UserHandle.USER_CURRENT);

        mBatteryPercentValue = Settings.System.getIntForUser(mContentResolver,
                Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, BATTERY_PERCENT_HIDDEN, UserHandle.USER_CURRENT);
        mBatteryPercentValuePrev = Settings.System.getIntForUser(mContentResolver,
                Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT_PREV, BATTERY_PERCENT_HIDDEN, UserHandle.USER_CURRENT);

        mStatusBarBattery = (SwitchPreference) findPreference(STATUSBAR_BATTERYICON_PREFERENCE);
        boolean isBatteryIconHidden = mBatteryIconStyleValue == BATTERY_STYLE_TEXT && mBatteryPercentValue == BATTERY_PERCENT_HIDDEN;
        if (mStatusBarBattery != null) {
            mStatusBarBattery.setChecked(!isBatteryIconHidden);
            mStatusBarBattery.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mStatusBarBattery) {
            if ((Boolean) newValue) {
                mBatteryPercentValue = mBatteryPercentValuePrev;
                mBatteryIconStyleValue = mBatteryIconStyleValuePrev;
            } else {
                Settings.System.putIntForUser(mContentResolver,
                    Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT_PREV, mBatteryPercentValue,
                    UserHandle.USER_CURRENT);
                mBatteryPercentValuePrev = mBatteryPercentValue;
                Settings.System.putIntForUser(mContentResolver,
                    Settings.System.STATUS_BAR_BATTERY_STYLE_PREV, mBatteryIconStyleValue,
                    UserHandle.USER_CURRENT);
                mBatteryIconStyleValuePrev = mBatteryIconStyleValue;

                mBatteryPercentValue = BATTERY_PERCENT_HIDDEN;
                mBatteryIconStyleValue = BATTERY_STYLE_TEXT;
            }
            Settings.System.putIntForUser(mContentResolver,
                Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, mBatteryPercentValue, UserHandle.USER_CURRENT);
            Settings.System.putIntForUser(mContentResolver,
                Settings.System.STATUS_BAR_BATTERY_STYLE, mBatteryIconStyleValue, UserHandle.USER_CURRENT);
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        MetricsLogger.visibility(getContext(), MetricsEvent.TUNER, true);
    }

    @Override
    public void onPause() {
        super.onPause();
        MetricsLogger.visibility(getContext(), MetricsEvent.TUNER, false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getActivity().onBackPressed();
            return true;
        }
        return false;
    }
}
