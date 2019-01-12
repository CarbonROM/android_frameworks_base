/*
 * Copyright (c) 2018, CarbonROM
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of screen-dimmer-pixel-filter nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package com.android.server.displayfingerprint;
import android.Manifest;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

public class DisplayFingerprintService extends Service {
    public static final String LOG = "DisplayFingerprintService";
    private WindowManager windowManager;
    private ImageView view = null;
    private boolean destroyed = false;
    private boolean intentProcessed = false;
    public static boolean running = false;
    private Context mContext;
    private int xFingerprintCenter;
    private int yFingerprintCenter;
    private int xFingerprintOffset;
    private int yFingerprintOffset;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        running = true;
        mContext = this;
        showFingerprint();
        Log.d(LOG, "Service started");
    }
    public void showFingerprint() {
        if (view != null) {
            return;
        }
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        view = new ImageView(this);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        WindowManager.LayoutParams params = getLayoutParams();

        xFingerprintCenter = (int) (getResources().getDimensionPixelSize(com.android.internal.R.dimen.fingerprint_x_center));
        yFingerprintCenter = (int) (getResources().getDimensionPixelSize(com.android.internal.R.dimen.fingerprint_y_center));
        xFingerprintOffset = (int) (getResources().getDimensionPixelSize(com.android.internal.R.dimen.fingerprint_x_size));
        yFingerprintOffset = (int) (getResources().getDimensionPixelSize(com.android.internal.R.dimen.fingerprint_y_size));

        view.setImageResource(R.drawable.display_fingerprint_icon);
        try {
            windowManager.addView(view, params);
        } catch (Exception e) {
            running = false;
            view = null;
            return;
        }

    }
    public void hideFingerprint() {
        if (view == null) {
            return;
        }
        windowManager.removeView(view);
        view = null;
    }

    private WindowManager.LayoutParams getLayoutParams()
    {
        //DisplayMetrics displayMetrics = new DisplayMetrics();
        //windowManager.getDefaultDisplay().getMetrics(displayMetrics);

        Resources res = getResources();

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                xFingerprintOffset,
                yFingerprintOffset,
                xFingerprintCenter,
                yFingerprintCenter,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSPARENT
        );
        // Use the rounded corners overlay to hide it from screenshots. See 132c9f514.
        params.privateFlags |= WindowManager.LayoutParams.PRIVATE_FLAG_IS_ROUNDED_CORNERS_OVERLAY;
        params.dimAmount = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        params.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE;
        return params;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Intent.ACTION_DELETE.equals(intent.getAction()) ||
                (intentProcessed && Intent.ACTION_INSERT.equals(intent.getAction()))) {
            Log.d(LOG, "Service got shutdown intent");
            stopSelf();
            intentProcessed = true;
            return START_NOT_STICKY;
        }
        intentProcessed = true;
        Log.d(LOG, "Service got intent " + intent.getAction());
        return START_STICKY;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        hideFingerprint();
        Log.d(LOG, "Service stopped");
        running = false;
    }

}