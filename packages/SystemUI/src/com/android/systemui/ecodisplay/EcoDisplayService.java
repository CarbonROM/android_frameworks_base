/*
 * Copyright (c) 2015, Sergii Pylypenko
 *           (c) 2018, Joe Maples
 *           (c) 2018, CarbonROM
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

package com.android.systemui.ecodisplay;

import android.Manifest;
import android.app.Service;
import android.content.BroadcastReceiver;
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
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
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

public class EcoDisplayService extends Service implements SensorEventListener{
    public static final String LOG = "Pixel Filter";

    private WindowManager windowManager;
    private ImageView view = null;
    private Bitmap bmp;

    private boolean destroyed = false;
    private boolean intentProcessed = false;
    public static boolean running = false;

    private int startCounter = 0;

    private ScreenOffReceiver screenOffReceiver = null;
    private SensorManager sensors = null;
    private Sensor lightSensor = null;
    private SettingsObserver mSettingsObserver;
    private Context mContext;

    // Pixel Filter Settings
    private int mPattern = 3;
    private int mShiftTimeout = 4;
    private boolean mUseLightSensor = false;
    private int mLightSensorValue = 1000;

    @Override
    public IBinder onBind(Intent intent) {
    return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        running = true;
        Log.d(LOG, "Service started");
        mContext = this;
        if (mSettingsObserver == null) {
            mSettingsObserver = new SettingsObserver(new Handler());
        }
        mSettingsObserver.observe();
        mSettingsObserver.update();

        if (mUseLightSensor) {
            sensors = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            lightSensor = sensors.getDefaultSensor(Sensor.TYPE_LIGHT);
            if (lightSensor != null) {
                sensors.registerListener(this, lightSensor, 1200000, 1000000);
            }

            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            screenOffReceiver = new ScreenOffReceiver();
            registerReceiver(screenOffReceiver, filter);
        } else {
            startFilter();
        }
    }

    public void startFilter() {
        if (view != null) {
            return;
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        view = new ImageView(this);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        bmp = Bitmap.createBitmap(Grids.GridSideSize, Grids.GridSideSize, Bitmap.Config.ARGB_4444);

        updatePattern();
        BitmapDrawable draw = new BitmapDrawable(bmp);
        draw.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        draw.setFilterBitmap(false);
        draw.setAntiAlias(false);
        draw.setTargetDensity(metrics.densityDpi);
        view.setBackground(draw);

        WindowManager.LayoutParams params = getLayoutParams();
        try {
            windowManager.addView(view, params);
        } catch (Exception e) {
            running = false;
            view = null;
            return;
        }


        startCounter++;
        final int handlerStartCounter = startCounter;
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (view == null || destroyed || handlerStartCounter != startCounter) {
                    return;
                }
                updatePattern();
                view.invalidate();
                if (!destroyed) {
                    handler.postDelayed(this, Grids.ShiftTimeouts[mShiftTimeout]);
                }
            }
        }, Grids.ShiftTimeouts[mShiftTimeout]);
    }

    public void stopFilter() {
        if (view == null) {
            return;
        }

        startCounter++;

        windowManager.removeView(view);
        view = null;
    }

    private WindowManager.LayoutParams getLayoutParams()
    {
        Point displaySize = new Point();
        windowManager.getDefaultDisplay().getRealSize(displaySize);
        Point windowSize = new Point();
        windowManager.getDefaultDisplay().getRealSize(windowSize);
        displaySize.x += displaySize.x - windowSize.x;
        displaySize.y += displaySize.y - windowSize.y;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                displaySize.x,
                displaySize.y,
                0,
                0,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                        WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR |
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSPARENT
        );

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
        destroyed = true;
        stopFilter();

        if (lightSensor != null) {
            unregisterReceiver(screenOffReceiver);
            sensors.unregisterListener(this, lightSensor);
        }

        Log.d(LOG, "Service stopped");
        running = false;

    }

    private int getShift() {
        long shift = (System.currentTimeMillis() / Grids.ShiftTimeouts[mPattern]) % Grids.GridSize;
        return Grids.GridShift[(int)shift];
    }

    private void updatePattern() {
        int shift = getShift();
        int shiftX = shift % Grids.GridSideSize;
        int shiftY = shift / Grids.GridSideSize;
        for (int i = 0; i < Grids.GridSize; i++) {
            int x = (i + shiftX) % Grids.GridSideSize;
            int y = ((i / Grids.GridSideSize) + shiftY) % Grids.GridSideSize;
            int color = (Grids.Patterns[mPattern][i] == 0) ? Color.TRANSPARENT : Color.BLACK;
            bmp.setPixel(x, y, color);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.values[0] > mLightSensorValue) {
            stopFilter();
        }
        if (event.values[0] < mLightSensorValue * 0.6f) {
            startFilter();
        }
    }

    class ScreenOffReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (lightSensor != null) {
                if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                    sensors.unregisterListener(EcoDisplayService.this, lightSensor);
                } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                    sensors.registerListener(EcoDisplayService.this, lightSensor, 1200000, 1000000);
                }
            }
        }
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(LOG, "Screen orientation changed, updating window layout");
        WindowManager.LayoutParams params = getLayoutParams();
        windowManager.updateViewLayout(view, params);
    }


    protected class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
           ContentResolver resolver = mContext.getContentResolver();
           resolver.registerContentObserver(Settings.System.getUriFor(
                  Settings.System.ECODISPLAY_PATTERN),
                  false, this, UserHandle.USER_CURRENT);
           resolver.registerContentObserver(Settings.System.getUriFor(
                  Settings.System.ECODISPLAY_SHIFT_TIMEOUT),
                  false, this, UserHandle.USER_CURRENT);
           resolver.registerContentObserver(Settings.System.getUriFor(
                  Settings.System.ECODISPLAY_USE_LIGHT_SENSOR),
                  false, this, UserHandle.USER_CURRENT);
           resolver.registerContentObserver(Settings.System.getUriFor(
                  Settings.System.ECODISPLAY_LIGHT_SENSOR_VALUE),
                  false, this, UserHandle.USER_CURRENT);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
           super.onChange(selfChange, uri);
           update();
           stopFilter();
           startFilter();
        }

        public void update() {
            mPattern = Settings.System.getIntForUser(
                    mContext.getContentResolver(), Settings.System.ECODISPLAY_PATTERN,
                    3, UserHandle.USER_CURRENT);
            mShiftTimeout = Settings.System.getIntForUser(
                    mContext.getContentResolver(), Settings.System.ECODISPLAY_SHIFT_TIMEOUT,
                    4, UserHandle.USER_CURRENT);
            mUseLightSensor = Settings.System.getIntForUser(
                    mContext.getContentResolver(), Settings.System.ECODISPLAY_USE_LIGHT_SENSOR,
                    0, UserHandle.USER_CURRENT) == 1;
            mLightSensorValue = Settings.System.getIntForUser(
                    mContext.getContentResolver(), Settings.System.ECODISPLAY_LIGHT_SENSOR_VALUE,
                    1000, UserHandle.USER_CURRENT);
        }
    }
}
