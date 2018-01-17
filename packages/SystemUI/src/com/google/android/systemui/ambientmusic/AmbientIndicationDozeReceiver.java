package com.google.android.systemui.ambientmusic;

import com.android.systemui.doze.DozeReceiver;
import com.google.android.systemui.ambientmusic.AmbientIndicationContainer;

public class AmbientIndicationDozeReceiver
implements DozeReceiver {
    private Object mContainer;

    private void setDoze(boolean dozing) {
        ((AmbientIndicationContainer)this.mContainer).updateDozing(dozing);
    }

    public AmbientIndicationDozeReceiver(Object object) {
        this.mContainer = object;
    }

    @Override
    public void setDozing(boolean dozing) {
        this.setDoze(dozing);
    }
}

