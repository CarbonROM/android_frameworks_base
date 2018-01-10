package com.google.android.systemui.ambientmusic;

import android.animation.ValueAnimator;
import com.google.android.systemui.ambientmusic.AmbientIndicationContainer;

final class AmbientIndicationAnimatorUpdateListener
implements ValueAnimator.AnimatorUpdateListener {
    private final Object mContainer;

    private final void updateAmbientIndicationAnimator(ValueAnimator valueAnimator) {
        ((AmbientIndicationContainer)this.mContainer).updateAnimator(valueAnimator);
    }

    public AmbientIndicationAnimatorUpdateListener(Object object) {
        this.mContainer = object;
    }

    public final void onAnimationUpdate(ValueAnimator valueAnimator) {
        this.updateAmbientIndicationAnimator(valueAnimator);
    }
}

