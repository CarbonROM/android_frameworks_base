package com.google.android.systemui.ambientmusic;

import android.view.View;
import com.google.android.systemui.ambientmusic.AmbientIndicationContainer;

final class AmbientIndicationLayoutChangeListener
implements View.OnLayoutChangeListener {
    private final Object mContainer;

    private final void updateContainerBottomPadding(View view, int n, int n2, int n3, int n4, int n5, int n6, int n7, int n8) {
        ((AmbientIndicationContainer)this.mContainer).updateAmbientIndicationBottomPadding();
    }

    public AmbientIndicationLayoutChangeIndicator(Object object) {
        this.mContainer = object;
    }

    public final void onLayoutChange(View view, int n, int n2, int n3, int n4, int n5, int n6, int n7, int n8) {
        this.updateContainerBottomPadding();
    }
}

