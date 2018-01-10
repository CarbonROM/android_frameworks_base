package com.google.android.systemui.ambientmusic;

import android.view.View;
import com.android.systemui.AutoReinflateContainer;
import com.google.android.systemui.ambientmusic.AmbientIndicationContainer;

final class AmbientIndicationInflateListener
implements AutoReinflateContainer.InflateListener {
    private final Object mContainer;

    private final void setAmbientIndicationView(View view) {
        ((AmbientIndicationContainer)this.mContainer).updateAmbientIndicationView(view);
    }

    public AmbientIndicationInflateListener(Object object) {
        this.mContainer = object;
    }

    @Override
    public final void onInflated(View view) {
        this.setAmbientIndicationView(view);
    }
}

