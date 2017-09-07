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
    private static final int BATTERY_STYLE_HIDDEN = 5;
    private static final int BATTERY_PERCENT_HIDDEN = 0;
    private static final int BATTERY_PERCENT_SHOW = 2;

    private SwitchPreference mStatusBarClock;
    private SwitchPreference mStatusBarBattery;
    private boolean mBatteryIconShowing;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.statusbar_icon_settings);

        mStatusBarClock = (SwitchPreference) findPreference(STATUSBAR_CLOCKICON_PREFERENCE);
        mStatusBarClock.setChecked((Settings.System.getInt(
                getActivity().getApplicationContext().getContentResolver(),
                Settings.System.STATUSBAR_CLOCK, 1) == 1));
        mStatusBarClock.setOnPreferenceChangeListener(this);

        mStatusBarBattery = (SwitchPreference) findPreference(STATUSBAR_BATTERYICON_PREFERENCE);
        mBatteryIconShowing = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, BATTERY_PERCENT_HIDDEN, UserHandle.USER_CURRENT) != BATTERY_PERCENT_HIDDEN
                || Settings.System.getIntForUser(getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY_STYLE, BATTERY_STYLE_PORTRAIT, UserHandle.USER_CURRENT) != BATTERY_STYLE_HIDDEN;
        mStatusBarBattery.setChecked(mBatteryIconShowing);
        mStatusBarBattery.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getApplicationContext().getContentResolver();
        if (preference == mStatusBarClock) {
            Settings.System.putInt(resolver,
                    Settings.System.STATUSBAR_CLOCK,
                    (Boolean) newValue ? 1 : 0);
            return true;
        } else if (preference == mStatusBarBattery) {
            if ((Boolean) newValue) {
                Settings.System.putIntForUser(resolver,
                    Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT,
                    Settings.System.getIntForUser(resolver,
                        Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT + "_prev", BATTERY_PERCENT_HIDDEN, UserHandle.USER_CURRENT),
                    UserHandle.USER_CURRENT);
                Settings.System.putIntForUser(resolver,
                    Settings.System.STATUS_BAR_BATTERY_STYLE,
                    Settings.System.getIntForUser(resolver,
                        Settings.System.STATUS_BAR_BATTERY_STYLE + "_prev", BATTERY_STYLE_PORTRAIT, UserHandle.USER_CURRENT),
                    UserHandle.USER_CURRENT);
            } else {
                Settings.System.putIntForUser(resolver,
                    Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT + "_prev",
                    Settings.System.getIntForUser(resolver,
                        Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, BATTERY_PERCENT_HIDDEN, UserHandle.USER_CURRENT),
                    UserHandle.USER_CURRENT);
                Settings.System.putIntForUser(resolver,
                    Settings.System.STATUS_BAR_BATTERY_STYLE + "_prev",
                    Settings.System.getIntForUser(resolver,
                        Settings.System.STATUS_BAR_BATTERY_STYLE, BATTERY_STYLE_PORTRAIT, UserHandle.USER_CURRENT),
                    UserHandle.USER_CURRENT);

                Settings.System.putIntForUser(resolver,
                    Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, BATTERY_PERCENT_HIDDEN, UserHandle.USER_CURRENT);
                Settings.System.putIntForUser(resolver,
                    Settings.System.STATUS_BAR_BATTERY_STYLE, BATTERY_STYLE_HIDDEN, UserHandle.USER_CURRENT);
            }
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
