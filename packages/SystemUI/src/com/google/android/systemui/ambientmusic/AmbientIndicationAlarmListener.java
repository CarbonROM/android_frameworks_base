package com.google.android.systemui.ambientmusic;

import android.app.AlarmManager;
import com.google.android.systemui.ambientmusic.AmbientIndicationService;

final class AmbientIndicationAlarmListener
implements AlarmManager.OnAlarmListener {
    private final Object mContainer;

    private final void hideContainer() {
        ((AmbientIndicationService)((Object)this.mContainer)).hideIndicationContainer();
    }

    public AmbientIndicationAlarmListner(Object object) {
        this.mContainer = object;
    }

    public final void onAlarm() {
        this.hideContainer();
    }
}

