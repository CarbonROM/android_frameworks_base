package com.android.systemui.qs.tiles;

import android.annotation.Nullable;
import android.annotation.NonNull;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile.SignalState;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.qs.logging.QSLogger;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.qs.tileimpl.QSTileImpl.ResourceIcon;
import com.android.systemui.statusbar.connectivity.MobileDataIndicators;
import com.android.systemui.statusbar.connectivity.NetworkController;
import com.android.systemui.statusbar.connectivity.SignalCallback;

import java.util.concurrent.Executors;
import java.util.List;

import javax.inject.Inject;

public class DataSwitchTile extends QSTileImpl<SignalState> {

    public static final String TILE_SPEC = "dataswitch";

    private boolean mCanSwitch = true;
    private boolean mRegistered;
    private int mSimCount;
    private final NetworkController mController;
    private final DataSwitchCallback mDataSwitchCallback = new DataSwitchCallback();

    private final Intent mLongClickIntent;
    private final String mTileLabel;
    private final BroadcastReceiver mSimReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) Log.d(TAG, "mSimReceiver:onReceive");
            refreshState();
        }
    };
    private final PhoneStateListener mPhoneStateListener;
    private final SubscriptionManager mSubscriptionManager;
    private final TelephonyManager mTelephonyManager;

    @Inject
    public DataSwitchTile(
        QSHost host,
        @Background Looper backgroundLooper,
        @Main Handler mainHandler,
        FalsingManager falsingManager,
        MetricsLogger metricsLogger,
        StatusBarStateController statusBarStateController,
        ActivityStarter activityStarter,
        QSLogger qsLogger,
        NetworkController networkController
    ) {
        super(host, backgroundLooper, mainHandler, falsingManager, metricsLogger,
                statusBarStateController, activityStarter, qsLogger);
        mLongClickIntent = new Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS);
        mTileLabel = mContext.getString(R.string.qs_data_switch_label);
        mSubscriptionManager = SubscriptionManager.from(mContext);
        mTelephonyManager = TelephonyManager.from(mContext);
        mPhoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String arg1) {
                mCanSwitch = mTelephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE;
                refreshState();
            }
        };
        mController = networkController;
        mController.observe(getLifecycle(), mDataSwitchCallback);
    }

    @Override
    public boolean isAvailable() {
        int count = TelephonyManager.getDefault().getPhoneCount();
        if (DEBUG) Log.d(TAG, "phoneCount: " + count);
        return count >= 2;
    }

    @Override
    public SignalState newTileState() {
        final SignalState state = new SignalState();
        state.label = mContext.getString(R.string.qs_data_switch_label);
        return state;
    }

    @Override
    public void handleSetListening(boolean listening) {
        if (listening) {
            if (!mRegistered) {
                final IntentFilter filter = new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
                mContext.registerReceiver(mSimReceiver, filter);
                mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
                mRegistered = true;
            }
            refreshState();
        } else if (mRegistered) {
            mContext.unregisterReceiver(mSimReceiver);
            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
            mRegistered = false;
        }
    }

    private void updateSimCount() {
        String simState = SystemProperties.get("gsm.sim.state");
        if (DEBUG) Log.d(TAG, "DataSwitchTile:updateSimCount:simState=" + simState);
        mSimCount = 0;
        try {
            final String[] sims = TextUtils.split(simState, ",");
            for (String sim : sims) {
                if (!sim.isEmpty()
                        && !sim.equalsIgnoreCase(IccCardConstants.INTENT_VALUE_ICC_ABSENT)
                        && !sim.equalsIgnoreCase(IccCardConstants.INTENT_VALUE_ICC_NOT_READY)) {
                    mSimCount++;
                }
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "Error on parsing sim state " + e.getMessage());
        }
        if (DEBUG) Log.d(TAG, "DataSwitchTile:updateSimCount:mSimCount=" + mSimCount);
    }

    @Override
    protected void handleClick(@Nullable View view) {
        if (!mCanSwitch) {
            if (DEBUG) Log.d(TAG, "Call state=" + mTelephonyManager.getCallState());
        } else if (mSimCount == 0) {
            if (DEBUG) Log.d(TAG, "handleClick:no sim card");
            Toast.makeText(mContext, R.string.qs_data_switch_toast_0,
                    Toast.LENGTH_LONG).show();
        } else if (mSimCount == 1) {
            if (DEBUG) Log.d(TAG, "handleClick:only one sim card");
            Toast.makeText(mContext, R.string.qs_data_switch_toast_1,
                    Toast.LENGTH_LONG).show();
        } else {
            Executors.newSingleThreadExecutor().execute(() -> {
                toggleMobileDataEnabled();
                refreshState();
            });
        }
    }

    @Override
    public Intent getLongClickIntent() {
        return mLongClickIntent;
    }

    @Override
    public CharSequence getTileLabel() {
        return mTileLabel;
    }

    @Override
    protected void handleUpdateState(SignalState state, Object arg) {
        final int defaultPhoneId = mSubscriptionManager.getDefaultDataPhoneId();
        if (DEBUG) Log.d(TAG, "default data phone id=" + defaultPhoneId);
        boolean activeSIMZero = defaultPhoneId == 0;

        CallbackInfo cb = (CallbackInfo) arg;
        if (cb == null) {
            cb = mDataSwitchCallback.mInfo;
        }

        updateSimCount();
        state.value = mSimCount == 2;
        if (mSimCount == 1 || mSimCount == 2) {
            state.icon = ResourceIcon.get(activeSIMZero
                    ? R.drawable.ic_qs_data_switch_1
                    : R.drawable.ic_qs_data_switch_2);
            if (cb.dataSubscriptionName != null) {
                state.secondaryLabel = cb.dataSubscriptionName;
            } else {
                state.secondaryLabel = mContext.getString(activeSIMZero
                        ? R.string.qs_data_sim_1
                        : R.string.qs_data_sim_2);
            }
        } else {
            state.icon = ResourceIcon.get(R.drawable.ic_qs_data_switch_1);
            state.secondaryLabel = mContext.getString(R.string.qs_data_sim_none);
        }
        if (mSimCount < 2 || !mCanSwitch) {
            state.state = 0;
            if (!mCanSwitch && DEBUG) Log.d(TAG, "call state isn't idle, set to unavailable.");
        } else {
            state.state = state.value ? 2 : 1;
        }

        state.contentDescription =
                mContext.getString(activeSIMZero
                        ? R.string.qs_data_switch_changed_1
                        : R.string.qs_data_switch_changed_2);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.CARBONFIBERS;
    }

    /**
     * Set whether to enable data for {@code subId}, also whether to disable data for other
     * subscription
     */
    private void toggleMobileDataEnabled() {
        // subIDs aren't necessarily 1 & 2 only !
        final SubscriptionInfo currentSubInfo = mSubscriptionManager.getDefaultDataSubscriptionInfo();
        if (currentSubInfo == null) return;
        final int currentSubID = currentSubInfo.getSubscriptionId();
        final int currentIndex = currentSubInfo.getSimSlotIndex();

        final List<SubscriptionInfo> subInfoList =
                mSubscriptionManager.getActiveSubscriptionInfoList(true);
        SubscriptionInfo newSubInfo = null;
        if (subInfoList != null) {
            if (DEBUG) Log.d(TAG, "subInfos:");
            for (SubscriptionInfo subInfo : subInfoList) {
                final int id = subInfo.getSubscriptionId();
                final int index = subInfo.getSimSlotIndex();
                if (DEBUG) Log.d(TAG, "id: " + id + " index: " + index);
                if (id != currentSubID && index != currentIndex) {
                    newSubInfo = subInfo;
                    break;
                }
            }
        }

        if (newSubInfo == null) {
            if (DEBUG) Log.d(TAG, "Could not find newSubInfo");
            return;
        }

        final int newSubID = newSubInfo.getSubscriptionId();
        mTelephonyManager.createForSubscriptionId(newSubID).setDataEnabled(true);
        mSubscriptionManager.setDefaultDataSubId(newSubID);
        if (DEBUG) Log.d(TAG, "Enabled subID: " + newSubID);

        if (currentSubInfo.isOpportunistic()) {
            // Never disable mobile data for opportunistic subscriptions
            if (DEBUG) Log.d(TAG, "Refusing to disable opportunistic subID: " + currentSubID);
            return;
        }

        mTelephonyManager.createForSubscriptionId(currentSubID).setDataEnabled(false);
        if (DEBUG) Log.d(TAG, "Disabled subID: " + currentSubID);
    }

    private static final class CallbackInfo {
        @Nullable
        CharSequence dataSubscriptionName;
    }

    private final class DataSwitchCallback implements SignalCallback {
        private final CallbackInfo mInfo = new CallbackInfo();

        @Override
        public void setMobileDataIndicators(@NonNull MobileDataIndicators indicators) {
            mInfo.dataSubscriptionName = mController.getMobileDataNetworkName();
            refreshState(mInfo);
        }
    }
}
