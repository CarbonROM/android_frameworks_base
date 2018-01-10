package com.google.android.systemui.ambientmusic;

import android.view.MotionEvent;
import android.view.View;
import com.google.android.systemui.ambientmusic.AmbientIndicationContainer;

final class AmbientIndicationTouchListener
implements View.OnTouchListener {
    private final Object mContainer;

    private final boolean touchEventStatus(View view, MotionEvent motionEvent) {
        return ((AmbientIndicationContainer)this.mContainer).getTouchEvent(view, motionEvent);
    }

    public AmbientIndicationTouchListener(Object object) {
        this.mContainer = object;
    }

    public final boolean onTouch(View view, MotionEvent motionEvent) {
        return this.touchEventStatus(view, motionEvent);
    }
}

