package com.android.systemui.qs.tiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.SysUIToast;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.SystemSetting;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.qs.tileimpl.QSTileImpl.ResourceIcon;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkController.SignalCallback;
import com.android.systemui.statusbar.policy.NetworkControllerImpl;
import java.lang.reflect.Method;
import org.codeaurora.internal.IExtTelephony;
import org.codeaurora.internal.IExtTelephony.Stub;

public class DataSwitchTile extends QSTileImpl<BooleanState> {
    private boolean mCanSwitch = true;
    protected final NetworkController mController;
    private IExtTelephony mExtTelephony = null;
    private MyCallStateListener mPhoneStateListener;
    private boolean mRegistered = false;
    protected final DataSwitchSignalCallback mSignalCallback = new DataSwitchSignalCallback();
    private int mSimCount = 0;
    BroadcastReceiver mSimReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "mSimReceiver:onReceive");
            refreshState();
        }
    };
    private SubscriptionManager mSubscriptionManager;
    /* access modifiers changed from: private */
    public TelephonyManager mTelephonyManager;
    /* access modifiers changed from: private */
    public boolean mVirtualSimExist = false;

    protected final class DataSwitchSignalCallback implements SignalCallback {
        protected DataSwitchSignalCallback() {
        }

        public void setVirtualSimstate(int[] softSimstate) {
            boolean exist = false;
            if (softSimstate != null && softSimstate.length > 0) {
                int length = softSimstate.length;
                int i = 0;
                while (true) {
                    if (i >= length) {
                        break;
                    } else if (softSimstate[i] != NetworkControllerImpl.SOFTSIM_DISABLE) {
                        exist = true;
                        break;
                    } else {
                        i++;
                    }
                }
            }
            Log.d(TAG, "virtual sim state change: " + mVirtualSimExist + " to " + exist);
            mVirtualSimExist = exist;
            refreshState();
        }
    }

    class MyCallStateListener extends PhoneStateListener {
        MyCallStateListener() {
        }

        public void onCallStateChanged(int state, String arg1) {
            mCanSwitch = mTelephonyManager.getCallState() == 0;
            refreshState();
        }
    }

    @Inject
    public DataSwitchTile(QSHost host) {
        super(host);
        mSubscriptionManager = SubscriptionManager.from(host.getContext());
        mTelephonyManager = (TelephonyManager) mContext.getSystemService("phone");
        mPhoneStateListener = new MyCallStateListener();
        mController = (NetworkController) Dependency.get(NetworkController.class);
    }

    @Override
    public boolean isAvailable() {
        int count = TelephonyManager.getDefault().getPhoneCount();
        Log.d(TAG, "phoneCount: " + count);
        return count >= 2;
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public void handleSetListening(boolean listening) {
        if (listening) {
            if (!mRegistered) {
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.SIM_STATE_CHANGED");
                mContext.registerReceiver(mSimReceiver, filter);
                mTelephonyManager.listen(mPhoneStateListener, 32);
                mController.addCallback(mSignalCallback);
                mRegistered = true;
            }
            refreshState();
        } else if (mRegistered) {
            mContext.unregisterReceiver(mSimReceiver);
            mTelephonyManager.listen(mPhoneStateListener, 0);
            mController.removeCallback(mSignalCallback);
            mRegistered = false;
        }
    }

    private void updateSimCount() {
        String simState = SystemProperties.get("gsm.sim.state");
        Log.d(TAG, "DataSwitchTile:updateSimCount:simState=" + simState);
        mSimCount = 0;
        try {
            String[] sims = TextUtils.split(simState, ",");
            for (int i = 0; i < sims.length; i++) {
                if (!sims[i].isEmpty() && !sims[i].equalsIgnoreCase("ABSENT")) {
                    if (!sims[i].equalsIgnoreCase("NOT_READY")) {
                        mSimCount++;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error to parse sim state");
        }
        Log.d(TAG, "DataSwitchTile:updateSimCount:mSimCount=" + mSimCount);
    }

    private void setDefaultDataSimIndex(int phoneId) {
        try {
            if (mExtTelephony == null) {
                mExtTelephony = Stub.asInterface(ServiceManager.getService("extphone"));
            }
            Log.d(TAG, "oemDdsSwitch:phoneId=" + phoneId);
            mExtTelephony.oemDdsSwitch(phoneId);
        } catch (Exception e) {
            Log.d(TAG, "setDefaultDataSimId", e);
            Log.d(TAG, "clear ext telephony service ref");
            mExtTelephony = null;
        }
    }

    @Override
    protected void handleClick() {
        if (!mCanSwitch) {
            Log.d(TAG, "Call state=" + mTelephonyManager.getCallState());
        } else if (mVirtualSimExist) {
            Log.d(TAG, "virtual sim exist. ignore click.");
        } else if (mSimCount == 0) {
            Log.d(TAG, "handleClick:no sim card");
            SysUIToast.makeText(mContext, mContext.getString(R.string.quick_settings_data_switch_toast_0), Toast.LENGTH_LONG).show();
        } else if (mSimCount == 1) {
            Log.d(TAG, "handleClick:only one sim card");
            SysUIToast.makeText(mContext, mContext.getString(R.string.quick_settings_data_switch_toast_1), Toast.LENGTH_LONG).show();
        } else {
            AsyncTask.execute(new Runnable() {
                public final void run() {
                    setDefaultDataSimIndex(1 - mSubscriptionManager.getDefaultDataPhoneId());
                    refreshState();
                }
            });
        }
    }

    @Override
    public Intent getLongClickIntent() {
        return new Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS);
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_data_switch_label);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        boolean value;
        boolean value2;
        if (arg == null) {
            int defaultPhoneId = mSubscriptionManager.getDefaultDataPhoneId();
            Log.d(TAG, "default data phone id=" + defaultPhoneId);
            if (defaultPhoneId == 0) {
                value2 = true;
            } else {
                value2 = false;
            }
            value = value2;
        } else {
            value = (Boolean) arg;
        }
        updateSimCount();
        int i2 = R.drawable.ic_qs_data_switch_1_disable;
        switch (mSimCount) {
            case 1:
                if (!value) {
                    i2 = R.drawable.ic_qs_data_switch_2_disable;
                }
                state.icon = ResourceIcon.get(i2);
                state.value = false;
                break;
            case 2:
                state.icon = ResourceIcon.get(value ? R.drawable.ic_qs_data_switch_1 : R.drawable.ic_qs_data_switch_2);
                state.value = true;
                break;
            default:
                state.icon = ResourceIcon.get(R.drawable.ic_qs_data_switch_1_disable);
                state.value = false;
                break;
        }
        int i3 = 2;
        if (mSimCount < 2) {
            state.state = 0;
        } else if (mVirtualSimExist) {
            state.state = 0;
            Log.d(TAG, "virtual sim exist, set to unavailable.");
        } else if (!mCanSwitch) {
            state.state = 0;
            Log.d(TAG, "call state isn't idle, set to unavailable.");
        } else {
            if (!state.value) {
                i3 = 1;
            }
            state.state = i3;
        }

        if (value) {
            state.contentDescription = mContext.getString(R.string.accessibility_quick_settings_data_switch_changed_1);
        } else {
            state.contentDescription = mContext.getString(R.string.accessibility_quick_settings_data_switch_changed_2);
        }
        state.label = mContext.getString(R.string.quick_settings_data_switch_label);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.CARBONFIBERS;
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (mState) {
            return mContext.getString(R.string.accessibility_quick_settings_data_switch_changed_1);
        }
        return mContext.getString(R.string.accessibility_quick_settings_data_switch_changed_2);
    }
}
