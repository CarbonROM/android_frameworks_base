package com.google.android.systemui.ambientmusic;

import com.android.systemui.statusbar.phone.DoubleTapHelper;
import com.google.android.systemui.ambientmusic.AmbientIndicationContainer;

final class AmbientIndicationActivationListener
implements DoubleTapHelper.ActivationListener {
    private final Object mContainer;

    private final void updateActive(boolean bl) {
        ((AmbientIndicationContainer)this.mContainer).cfr_renamed_529(bl);
    }

    public AmbientIndicationActivationListener(Object object) {
        this.mContainer = object;
    }

    @Override
    public final void onActiveChanged(boolean bl) {
        this.updateActive(bl);
    }
}

