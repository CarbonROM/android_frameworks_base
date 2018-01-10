package com.google.android.systemui.ambientmusic;

import com.android.systemui.statusbar.phone.DoubleTapHelper;
import com.google.android.systemui.ambientmusic.AmbientIndicationContainer;

final class AmbientIndicationDoubleTapListenter
implements DoubleTapHelper.DoubleTapListener {
    private final Object mContainer;

    private final boolean isDoubleTap() {
        return ((AmbientIndicationContainer)this.mContainer).getDoubleTap();
    }

    public AmbientIndicationDoubleTapListener(Object object) {
        this.mContainer = object;
    }

    @Override
    public final boolean onDoubleTap() {
        return this.isDoubleTap();
    }
}

