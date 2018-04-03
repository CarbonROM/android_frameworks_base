/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.location;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.LOCATION_HARDWARE;
import static android.Manifest.permission.WRITE_SECURE_SETTINGS;
import static android.app.AlarmManager.ELAPSED_REALTIME;

import static com.android.internal.util.function.pooled.PooledLambda.obtainRunnable;

import android.Manifest;
import android.annotation.CallbackExecutor;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresFeature;
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.annotation.SystemService;
import android.annotation.TestApi;
import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.app.PropertyInvalidatedCache;
import android.compat.Compatibility;
import android.compat.annotation.ChangeId;
import android.compat.annotation.EnabledAfter;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.ICancellationSignal;
import android.os.Looper;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.ArrayMap;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.location.ProviderProperties;
import com.android.internal.util.Preconditions;
import com.android.internal.util.function.pooled.PooledRunnable;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;

/**
 * This class provides access to the system location services. These services allow applications to
 * obtain periodic updates of the device's geographical location, or to be notified when the device
 * enters the proximity of a given geographical location.
 *
 * <p class="note">Unless noted, all Location API methods require the {@link
 * android.Manifest.permission#ACCESS_COARSE_LOCATION} or {@link
 * android.Manifest.permission#ACCESS_FINE_LOCATION} permissions. If your application only has the
 * coarse permission then it will not have access to fine location providers. Other providers will
 * still return location results, but the exact location will be obfuscated to a coarse level of
 * accuracy.
 */
@SuppressWarnings({"deprecation"})
@SystemService(Context.LOCATION_SERVICE)
@RequiresFeature(PackageManager.FEATURE_LOCATION)
public class LocationManager {

    @GuardedBy("mLock")
    private PropertyInvalidatedCache<Integer, Boolean> mLocationEnabledCache =
            new PropertyInvalidatedCache<Integer, Boolean>(
                4,
                CACHE_KEY_LOCATION_ENABLED_PROPERTY) {
                @Override
                protected Boolean recompute(Integer userHandle) {
                    try {
                        return mService.isLocationEnabledForUser(userHandle);
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                }
            };

    private final Object mLock = new Object();

    /**
     * For apps targeting Android R and above, {@link #getProvider(String)} will no longer throw any
     * security exceptions.
     *
     * @hide
     */
    @ChangeId
    @EnabledAfter(targetSdkVersion = Build.VERSION_CODES.Q)
    private static final long GET_PROVIDER_SECURITY_EXCEPTIONS = 150935354L;

    /**
     * For apps targeting Android K and above, supplied {@link PendingIntent}s must be targeted to a
     * specific package.
     *
     * @hide
     */
    @ChangeId
    @EnabledAfter(targetSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
    private static final long TARGETED_PENDING_INTENT = 148963590L;

    /**
     * For apps targeting Android K and above, incomplete locations may not be passed to
     * {@link #setTestProviderLocation}.
     *
     * @hide
     */
    @ChangeId
    @EnabledAfter(targetSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
    private static final long INCOMPLETE_LOCATION = 148964793L;

    /**
     * For apps targeting Android S and above, all {@link GpsStatus} API usage must be replaced with
     * {@link GnssStatus} APIs.
     *
     * @hide
     */
    @ChangeId
    @EnabledAfter(targetSdkVersion = Build.VERSION_CODES.R)
    private static final long GPS_STATUS_USAGE = 144027538L;

    /**
     * Name of the network location provider.
     *
     * <p>This provider determines location based on nearby of cell tower and WiFi access points.
     * Results are retrieved by means of a network lookup.
     */
    public static final String NETWORK_PROVIDER = "network";

    /**
     * Name of the GNSS location provider.
     *
     * <p>This provider determines location using GNSS satellites. Depending on conditions, this
     * provider may take a while to return a location fix. Requires the
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION} permission.
     *
     * <p>The extras Bundle for the GPS location provider can contain the following key/value pairs:
     * <ul>
     * <li> satellites - the number of satellites used to derive the fix
     * </ul>
     */
    public static final String GPS_PROVIDER = "gps";

    /**
     * A special location provider for receiving locations without actually initiating a location
     * fix.
     *
     * <p>This provider can be used to passively receive location updates when other applications or
     * services request them without actually requesting the locations yourself. This provider will
     * only return locations generated by other providers.  You can query the
     * {@link Location#getProvider()} method to determine the actual provider that supplied the
     * location update. Requires the {@link android.Manifest.permission#ACCESS_FINE_LOCATION}
     * permission, although there is no guarantee of fine locations.
     */
    public static final String PASSIVE_PROVIDER = "passive";

    /**
     * The fused location provider.
     *
     * <p>This provider combines may combine inputs from several location sources to provide the
     * best possible location fix. It is implicitly used for all API's that involve the
     * {@link LocationRequest} object.
     *
     * @hide
     */
    @TestApi
    public static final String FUSED_PROVIDER = "fused";

    /**
     * Key used for the Bundle extra holding a boolean indicating whether
     * a proximity alert is entering (true) or exiting (false)..
     */
    public static final String KEY_PROXIMITY_ENTERING = "entering";

    /**
     * This key is no longer in use.
     *
     * <p>Key used for a Bundle extra holding an Integer status value when a status change is
     * broadcast using a PendingIntent.
     *
     * @deprecated Status changes are deprecated and no longer broadcast from Android Q onwards.
     */
    @Deprecated
    public static final String KEY_STATUS_CHANGED = "status";

    /**
     * Key used for an extra holding a boolean enabled/disabled status value when a provider
     * enabled/disabled event is broadcast using a PendingIntent.
     *
     * @see #requestLocationUpdates(String, long, float, PendingIntent)
     */
    public static final String KEY_PROVIDER_ENABLED = "providerEnabled";

    /**
     * Key used for an extra holding a {@link Location} value when a location change is broadcast
     * using a PendingIntent.
     *
     * @see #requestLocationUpdates(String, long, float, PendingIntent)
     */
    public static final String KEY_LOCATION_CHANGED = "location";

    /**
     * Broadcast intent action when the set of enabled location providers changes. To check the
     * status of a provider, use {@link #isProviderEnabled(String)}. From Android Q and above, will
     * include a string intent extra, {@link #EXTRA_PROVIDER_NAME}, with the name of the provider
     * whose state has changed. From Android R and above, will include a boolean intent extra,
     * {@link #EXTRA_PROVIDER_ENABLED}, with the enabled state of the provider.
     *
     * @see #EXTRA_PROVIDER_NAME
     * @see #EXTRA_PROVIDER_ENABLED
     * @see #isProviderEnabled(String)
     */
    public static final String PROVIDERS_CHANGED_ACTION = "android.location.PROVIDERS_CHANGED";

    /**
     * Intent extra included with {@link #PROVIDERS_CHANGED_ACTION} broadcasts, containing the name
     * of the location provider that has changed.
     *
     * @see #PROVIDERS_CHANGED_ACTION
     * @see #EXTRA_PROVIDER_ENABLED
     */
    public static final String EXTRA_PROVIDER_NAME = "android.location.extra.PROVIDER_NAME";

    /**
     * Intent extra included with {@link #PROVIDERS_CHANGED_ACTION} broadcasts, containing the
     * boolean enabled state of the location provider that has changed.
     *
     * @see #PROVIDERS_CHANGED_ACTION
     * @see #EXTRA_PROVIDER_NAME
     */
    public static final String EXTRA_PROVIDER_ENABLED = "android.location.extra.PROVIDER_ENABLED";

    /**
     * Broadcast intent action when the device location enabled state changes. From Android R and
     * above, will include a boolean intent extra, {@link #EXTRA_LOCATION_ENABLED}, with the enabled
     * state of location.
     *
     * @see #EXTRA_LOCATION_ENABLED
     * @see #isLocationEnabled()
     */
    public static final String MODE_CHANGED_ACTION = "android.location.MODE_CHANGED";

    /**
     * Intent extra included with {@link #MODE_CHANGED_ACTION} broadcasts, containing the boolean
     * enabled state of location.
     *
     * @see #MODE_CHANGED_ACTION
     */
    public static final String EXTRA_LOCATION_ENABLED = "android.location.extra.LOCATION_ENABLED";

    /**
     * Broadcast intent action indicating that a high power location requests
     * has either started or stopped being active.  The current state of
     * active location requests should be read from AppOpsManager using
     * {@code OP_MONITOR_HIGH_POWER_LOCATION}.
     *
     * @hide
     */
    public static final String HIGH_POWER_REQUEST_CHANGE_ACTION =
            "android.location.HIGH_POWER_REQUEST_CHANGE";

    /**
     * Broadcast intent action for Settings app to inject a footer at the bottom of location
     * settings. This is for use only by apps that are included in the system image.
     *
     * <p>To inject a footer to location settings, you must declare a broadcast receiver for
     * this action in the manifest:
     * <pre>
     *     &lt;receiver android:name="com.example.android.footer.MyFooterInjector"&gt;
     *         &lt;intent-filter&gt;
     *             &lt;action android:name="com.android.settings.location.INJECT_FOOTER" /&gt;
     *         &lt;/intent-filter&gt;
     *         &lt;meta-data
     *             android:name="com.android.settings.location.FOOTER_STRING"
     *             android:resource="@string/my_injected_footer_string" /&gt;
     *     &lt;/receiver&gt;
     * </pre>
     *
     * <p>This broadcast receiver will never actually be invoked. See also
     * {#METADATA_SETTINGS_FOOTER_STRING}.
     *
     * @hide
     */
    public static final String SETTINGS_FOOTER_DISPLAYED_ACTION =
            "com.android.settings.location.DISPLAYED_FOOTER";

    /**
     * Metadata name for {@link LocationManager#SETTINGS_FOOTER_DISPLAYED_ACTION} broadcast
     * receivers to specify a string resource id as location settings footer text. This is for use
     * only by apps that are included in the system image.
     *
     * <p>See {@link #SETTINGS_FOOTER_DISPLAYED_ACTION} for more detail on how to use.
     *
     * @hide
     */
    public static final String METADATA_SETTINGS_FOOTER_STRING =
            "com.android.settings.location.FOOTER_STRING";

    private static final long GET_CURRENT_LOCATION_MAX_TIMEOUT_MS = 30 * 1000;

    private final Context mContext;

    @UnsupportedAppUsage
    private final ILocationManager mService;

    @GuardedBy("mListeners")
    private final ArrayMap<LocationListener, LocationListenerTransport> mListeners =
            new ArrayMap<>();

    @GuardedBy("mBatchedLocationCallbackManager")
    private final BatchedLocationCallbackManager mBatchedLocationCallbackManager =
            new BatchedLocationCallbackManager();
    private final GnssStatusListenerManager
            mGnssStatusListenerManager = new GnssStatusListenerManager();
    private final GnssMeasurementsListenerManager mGnssMeasurementsListenerManager =
            new GnssMeasurementsListenerManager();
    private final GnssNavigationMessageListenerManager mGnssNavigationMessageListenerTransport =
            new GnssNavigationMessageListenerManager();
    private final GnssAntennaInfoListenerManager mGnssAntennaInfoListenerManager =
            new GnssAntennaInfoListenerManager();

    /**
     * @hide
     */
    public LocationManager(@NonNull Context context, @NonNull ILocationManager service) {
        mService = service;
        mContext = context;
    }

    /**
     * @hide
     */
    @TestApi
    public @NonNull String[] getBackgroundThrottlingWhitelist() {
        try {
            return mService.getBackgroundThrottlingWhitelist();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * @hide
     */
    @TestApi
    public @NonNull String[] getIgnoreSettingsWhitelist() {
        try {
            return mService.getIgnoreSettingsWhitelist();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns the extra location controller package on the device.
     *
     * @hide
     */
    @SystemApi
    public @Nullable String getExtraLocationControllerPackage() {
        try {
            return mService.getExtraLocationControllerPackage();
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            return null;
        }
    }

    /**
     * Set the extra location controller package for location services on the device.
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(Manifest.permission.LOCATION_HARDWARE)
    public void setExtraLocationControllerPackage(@Nullable String packageName) {
        try {
            mService.setExtraLocationControllerPackage(packageName);
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    /**
     * Set whether the extra location controller package is currently enabled on the device.
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(Manifest.permission.LOCATION_HARDWARE)
    public void setExtraLocationControllerPackageEnabled(boolean enabled) {
        try {
            mService.setExtraLocationControllerPackageEnabled(enabled);
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns whether extra location controller package is currently enabled on the device.
     *
     * @hide
     */
    @SystemApi
    public boolean isExtraLocationControllerPackageEnabled() {
        try {
            return mService.isExtraLocationControllerPackageEnabled();
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            return false;
        }
    }

    /**
     * Set the extra location controller package for location services on the device.
     *
     * @removed
     * @deprecated Use {@link #setExtraLocationControllerPackage} instead.
     * @hide
     */
    @Deprecated
    @SystemApi
    @RequiresPermission(Manifest.permission.LOCATION_HARDWARE)
    public void setLocationControllerExtraPackage(String packageName) {
        try {
            mService.setExtraLocationControllerPackage(packageName);
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    /**
     * Set whether the extra location controller package is currently enabled on the device.
     *
     * @removed
     * @deprecated Use {@link #setExtraLocationControllerPackageEnabled} instead.
     * @hide
     */
    @SystemApi
    @Deprecated
    @RequiresPermission(Manifest.permission.LOCATION_HARDWARE)
    public void setLocationControllerExtraPackageEnabled(boolean enabled) {
        try {
            mService.setExtraLocationControllerPackageEnabled(enabled);
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns the current enabled/disabled state of location. To listen for changes, see
     * {@link #MODE_CHANGED_ACTION}.
     *
     * @return true if location is enabled and false if location is disabled.
     */
    public boolean isLocationEnabled() {
        return isLocationEnabledForUser(Process.myUserHandle());
    }

    /**
     * Returns the current enabled/disabled state of location for the given user.
     *
     * @param userHandle the user to query
     * @return true if location is enabled and false if location is disabled.
     *
     * @hide
     */
    @SystemApi
    public boolean isLocationEnabledForUser(@NonNull UserHandle userHandle) {
        synchronized (mLock) {
            if (mLocationEnabledCache != null) {
                return mLocationEnabledCache.query(userHandle.getIdentifier());
            }
        }

        // fallback if cache is disabled
        try {
            return mService.isLocationEnabledForUser(userHandle.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Enables or disables location for the given user.
     *
     * @param enabled true to enable location and false to disable location.
     * @param userHandle the user to set
     *
     * @hide
     */
    @SystemApi
    @TestApi
    @RequiresPermission(WRITE_SECURE_SETTINGS)
    public void setLocationEnabledForUser(boolean enabled, @NonNull UserHandle userHandle) {
        try {
            mService.setLocationEnabledForUser(enabled, userHandle.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns the current enabled/disabled status of the given provider. To listen for changes, see
     * {@link #PROVIDERS_CHANGED_ACTION}.
     *
     * Before API version {@link android.os.Build.VERSION_CODES#LOLLIPOP}, this method would throw
     * {@link SecurityException} if the location permissions were not sufficient to use the
     * specified provider.
     *
     * @param provider a provider listed by {@link #getAllProviders()}
     * @return true if the provider exists and is enabled
     *
     * @throws IllegalArgumentException if provider is null
     */
    public boolean isProviderEnabled(@NonNull String provider) {
        return isProviderEnabledForUser(provider, Process.myUserHandle());
    }

    /**
     * Returns the current enabled/disabled status of the given provider and user. Callers should
     * prefer {@link #isLocationEnabledForUser(UserHandle)} unless they depend on provider-specific
     * APIs.
     *
     * Before API version {@link android.os.Build.VERSION_CODES#LOLLIPOP}, this method would throw
     * {@link SecurityException} if the location permissions were not sufficient to use the
     * specified provider.
     *
     * @param provider a provider listed by {@link #getAllProviders()}
     * @param userHandle the user to query
     * @return true if the provider exists and is enabled
     *
     * @throws IllegalArgumentException if provider is null
     * @hide
     */
    @SystemApi
    public boolean isProviderEnabledForUser(
            @NonNull String provider, @NonNull UserHandle userHandle) {
        Preconditions.checkArgument(provider != null, "invalid null provider");

        try {
            return mService.isProviderEnabledForUser(provider, userHandle.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Method for enabling or disabling a single location provider. This method is deprecated and
     * functions as a best effort. It should not be relied on in any meaningful sense as providers
     * may no longer be enabled or disabled by clients.
     *
     * @param provider a provider listed by {@link #getAllProviders()}
     * @param enabled whether to enable or disable the provider
     * @param userHandle the user to set
     * @return true if the value was set, false otherwise
     *
     * @throws IllegalArgumentException if provider is null
     * @deprecated Do not manipulate providers individually, use
     * {@link #setLocationEnabledForUser(boolean, UserHandle)} instead.
     * @hide
     */
    @Deprecated
    @SystemApi
    @RequiresPermission(WRITE_SECURE_SETTINGS)
    public boolean setProviderEnabledForUser(
            @NonNull String provider, boolean enabled, @NonNull UserHandle userHandle) {
        Preconditions.checkArgument(provider != null, "invalid null provider");

        return Settings.Secure.putStringForUser(
                mContext.getContentResolver(),
                Settings.Secure.LOCATION_PROVIDERS_ALLOWED,
                (enabled ? "+" : "-") + provider,
                userHandle.getIdentifier());
    }

    /**
     * Gets the last known location from the fused provider, or null if there is no last known
     * location. The returned location may be quite old in some circumstances, so the age of the
     * location should always be checked.
     *
     * @return the last known location, or null if not available
     * @throws SecurityException if no suitable location permission is present
     *
     * @hide
     */
    @Nullable
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public Location getLastLocation() {
        try {
            return mService.getLastLocation(null, mContext.getPackageName(),
                    mContext.getAttributionTag());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Gets the last known location from the given provider, or null if there is no last known
     * location. The returned location may be quite old in some circumstances, so the age of the
     * location should always be checked.
     *
     * <p>This will never activate sensors to compute a new location, and will only ever return a
     * cached location.
     *
     * <p>See also {@link #getCurrentLocation(String, CancellationSignal, Executor, Consumer)} which
     * will always attempt to return a current location, but will potentially use additional power
     * in the course of the attempt as compared to this method.
     *
     * @param provider a provider listed by {@link #getAllProviders()}
     * @return the last known location for the given provider, or null if not available
     * @throws SecurityException if no suitable permission is present
     * @throws IllegalArgumentException if provider is null or doesn't exist
     */
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    @Nullable
    public Location getLastKnownLocation(@NonNull String provider) {
        android.util.SeempLog.record(46);
        Preconditions.checkArgument(provider != null, "invalid null provider");

        LocationRequest request = LocationRequest.createFromDeprecatedProvider(
                provider, 0, 0, true);

        try {
            return mService.getLastLocation(request, mContext.getPackageName(),
                    mContext.getAttributionTag());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Asynchronously returns a single current location fix. This may activate sensors in order to
     * compute a new location, unlike {@link #getLastKnownLocation(String)}, which will only return
     * a cached fix if available. The given callback will be invoked once and only once, either with
     * a valid location fix or with a null location fix if the provider was unable to generate a
     * valid location.
     *
     * <p>A client may supply an optional {@link CancellationSignal}. If this is used to cancel the
     * operation, no callback should be expected after the cancellation.
     *
     * <p>This method may return locations from the very recent past (on the order of several
     * seconds), but will never return older locations (for example, several minutes old or older).
     * Clients may rely upon the guarantee that if this method returns a location, it will represent
     * the best estimation of the location of the device in the present moment.
     *
     * <p>Clients calling this method from the background may notice that the method fails to
     * determine a valid location fix more often than while in the foreground. Background
     * applications may be throttled in their location accesses to some degree.
     *
     * @param provider           a provider listed by {@link #getAllProviders()}
     * @param cancellationSignal an optional signal that allows for cancelling this call
     * @param executor           the callback will take place on this {@link Executor}
     * @param consumer           the callback invoked with either a {@link Location} or null
     *
     * @throws IllegalArgumentException if provider is null or doesn't exist
     * @throws IllegalArgumentException if executor is null
     * @throws IllegalArgumentException if consumer is null
     * @throws SecurityException        if no suitable permission is present
     */
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public void getCurrentLocation(@NonNull String provider,
            @Nullable CancellationSignal cancellationSignal,
            @NonNull @CallbackExecutor Executor executor, @NonNull Consumer<Location> consumer) {
        getCurrentLocation(LocationRequest.createFromDeprecatedProvider(provider, 0, 0, true),
                cancellationSignal, executor, consumer);
    }

    /**
     * Asynchronously returns a single current location fix based on the given
     * {@link LocationRequest}.
     *
     * <p>See {@link #getCurrentLocation(String, CancellationSignal, Executor, Consumer)} for more
     * information.
     *
     * @param locationRequest    the location request containing location parameters
     * @param cancellationSignal an optional signal that allows for cancelling this call
     * @param executor           the callback will take place on this {@link Executor}
     * @param consumer           the callback invoked with either a {@link Location} or null
     *
     * @throws IllegalArgumentException if provider is null or doesn't exist
     * @throws IllegalArgumentException if executor is null
     * @throws IllegalArgumentException if consumer is null
     * @throws SecurityException        if no suitable permission is present
     * @hide
     */
    @SystemApi
    @TestApi
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public void getCurrentLocation(@NonNull LocationRequest locationRequest,
            @Nullable CancellationSignal cancellationSignal,
            @NonNull @CallbackExecutor Executor executor, @NonNull Consumer<Location> consumer) {
        LocationRequest currentLocationRequest = new LocationRequest(locationRequest)
                .setNumUpdates(1);
        if (currentLocationRequest.getExpireIn() > GET_CURRENT_LOCATION_MAX_TIMEOUT_MS) {
            currentLocationRequest.setExpireIn(GET_CURRENT_LOCATION_MAX_TIMEOUT_MS);
        }

        GetCurrentLocationTransport transport = new GetCurrentLocationTransport(executor,
                consumer);

        if (cancellationSignal != null) {
            cancellationSignal.throwIfCanceled();
        }

        ICancellationSignal remoteCancellationSignal = CancellationSignal.createTransport();

        try {
            if (mService.getCurrentLocation(currentLocationRequest, remoteCancellationSignal,
                    transport, mContext.getPackageName(), mContext.getAttributionTag(),
                    transport.getListenerId())) {
                transport.register(mContext.getSystemService(AlarmManager.class),
                        remoteCancellationSignal);
                if (cancellationSignal != null) {
                    cancellationSignal.setOnCancelListener(transport::cancel);
                }
            } else {
                transport.fail();
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Register for a single location update using the named provider and a callback.
     *
     * <p>See {@link #requestLocationUpdates(String, long, float, LocationListener, Looper)} for
     * more detail on how to use this method.
     *
     * @param provider a provider listed by {@link #getAllProviders()}
     * @param listener the listener to receive location updates
     * @param looper   the looper handling listener callbacks, or null to use the looper of the
     *                 calling thread
     *
     * @throws IllegalArgumentException if provider is null or doesn't exist
     * @throws IllegalArgumentException if listener is null
     * @throws SecurityException        if no suitable permission is present
     * @deprecated Use {@link #getCurrentLocation(String, CancellationSignal, Executor, Consumer)}
     * instead as it does not carry a risk of extreme battery drain.
     */
    @Deprecated
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public void requestSingleUpdate(
            @NonNull String provider, @NonNull LocationListener listener, @Nullable Looper looper) {
        android.util.SeempLog.record(64);
        Preconditions.checkArgument(provider != null, "invalid null provider");
        Preconditions.checkArgument(listener != null, "invalid null listener");

        LocationRequest request = LocationRequest.createFromDeprecatedProvider(
                provider, 0, 0, true);
        request.setExpireIn(GET_CURRENT_LOCATION_MAX_TIMEOUT_MS);
        requestLocationUpdates(request, listener, looper);
    }

    /**
     * Register for a single location update using a Criteria and a callback.
     *
     * <p>See {@link #requestLocationUpdates(long, float, Criteria, PendingIntent)} for more detail
     * on how to use this method.
     *
     * @param criteria contains parameters to choose the appropriate provider for location updates
     * @param listener the listener to receive location updates
     * @param looper   the looper handling listener callbacks, or null to use the looper of the
     *                 calling thread
     *
     * @throws IllegalArgumentException if criteria is null
     * @throws IllegalArgumentException if listener is null
     * @throws SecurityException        if no suitable permission is present
     * @deprecated Use {@link #getCurrentLocation(String, CancellationSignal, Executor, Consumer)}
     * instead as it does not carry a risk of extreme battery drain.
     */
    @Deprecated
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public void requestSingleUpdate(
            @NonNull Criteria criteria,
            @NonNull LocationListener listener,
            @Nullable Looper looper) {
        android.util.SeempLog.record(64);
        Preconditions.checkArgument(criteria != null, "invalid null criteria");
        Preconditions.checkArgument(listener != null, "invalid null listener");

        LocationRequest request = LocationRequest.createFromDeprecatedCriteria(
                criteria, 0, 0, true);
        request.setExpireIn(GET_CURRENT_LOCATION_MAX_TIMEOUT_MS);
        requestLocationUpdates(request, listener, looper);
    }

    /**
     * Register for a single location update using a named provider and pending intent.
     *
     * <p>See {@link #requestLocationUpdates(long, float, Criteria, PendingIntent)} for more detail
     * on how to use this method.
     *
     * @param provider      a provider listed by {@link #getAllProviders()}
     * @param pendingIntent the pending intent to send location updates
     *
     * @throws IllegalArgumentException if provider is null or doesn't exist
     * @throws IllegalArgumentException if intent is null
     * @throws SecurityException        if no suitable permission is present
     * @deprecated Use {@link #getCurrentLocation(String, CancellationSignal, Executor, Consumer)}
     * instead as it does not carry a risk of extreme battery drain.
     */
    @Deprecated
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public void requestSingleUpdate(@NonNull String provider,
            @NonNull PendingIntent pendingIntent) {
        android.util.SeempLog.record(64);
        Preconditions.checkArgument(provider != null, "invalid null provider");

        LocationRequest request = LocationRequest.createFromDeprecatedProvider(
                provider, 0, 0, true);
        request.setExpireIn(GET_CURRENT_LOCATION_MAX_TIMEOUT_MS);
        requestLocationUpdates(request, pendingIntent);
    }

    /**
     * Register for a single location update using a Criteria and pending intent.
     *
     * <p>See {@link #requestLocationUpdates(long, float, Criteria, PendingIntent)} for more detail
     * on how to use this method.
     *
     * @param criteria      contains parameters to choose the appropriate provider for location
     *                      updates
     * @param pendingIntent the pending intent to send location updates
     *
     * @throws IllegalArgumentException if provider is null or doesn't exist
     * @throws IllegalArgumentException if intent is null
     * @throws SecurityException        if no suitable permission is present
     * @deprecated Use {@link #getCurrentLocation(String, CancellationSignal, Executor, Consumer)}
     * instead as it does not carry a risk of extreme battery drain.
     */
    @Deprecated
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public void requestSingleUpdate(@NonNull Criteria criteria,
            @NonNull PendingIntent pendingIntent) {
        android.util.SeempLog.record(64);
        Preconditions.checkArgument(criteria != null, "invalid null criteria");

        LocationRequest request = LocationRequest.createFromDeprecatedCriteria(
                criteria, 0, 0, true);
        request.setExpireIn(GET_CURRENT_LOCATION_MAX_TIMEOUT_MS);
        requestLocationUpdates(request, pendingIntent);
    }

    /**
     * Register for location updates from the given provider with the given arguments. {@link
     * LocationListener} callbacks will take place on the given {@link Looper} or {@link Executor}.
     * If a null {@link Looper} is supplied, the Looper of the calling thread will be used instead.
     * Only one request can be registered for each unique listener, so any subsequent requests with
     * the same listener will overwrite all associated arguments.
     *
     * <p> It may take a while to receive the first location update. If an immediate location is
     * required, applications may use the {@link #getLastKnownLocation(String)} method.
     *
     * <p> The location update interval can be controlled using the minimum time parameter. The
     * elapsed time between location updates will never be less than this parameter, although it may
     * be more depending on location availability and other factors. Choosing a sensible value for
     * the minimum time parameter is important to conserve battery life. Every location update
     * requires power from a variety of sensors. Select a minimum time parameter as high as possible
     * while still providing a reasonable user experience. If your application is not in the
     * foreground and showing location to the user then your application should consider switching
     * to the {@link #PASSIVE_PROVIDER} instead.
     *
     * <p> The minimum distance parameter can also be used to control the frequency of location
     * updates. If it is greater than 0 then the location provider will only send your application
     * an update when the location has changed by at least minDistance meters, AND when the minimum
     * time has elapsed. However it is more difficult for location providers to save power using the
     * minimum distance parameter, so the minimum time parameter should be the primary tool for
     * conserving battery life.
     *
     * <p> If your application wants to passively observe location updates triggered by other
     * applications, but not consume any additional power otherwise, then use the {@link
     * #PASSIVE_PROVIDER}. This provider does not turn on or modify active location providers, so
     * you do not need to be as careful about minimum time and minimum distance parameters. However,
     * if your application performs heavy work on a location update (such as network activity) then
     * you should select non-zero values for the parameters to rate-limit your update frequency in
     * the case another application enables a location provider with extremely fast updates.
     *
     * <p>In case the provider you have selected is disabled, location updates will cease, and a
     * provider availability update will be sent. As soon as the provider is enabled again, another
     * provider availability update will be sent and location updates will immediately resume.
     *
     * <p> When location callbacks are invoked, the system will hold a wakelock on your
     * application's behalf for some period of time, but not indefinitely. If your application
     * requires a long running wakelock within the location callback, you should acquire it
     * yourself.
     *
     * <p class="note"> Prior to Jellybean, the minTime parameter was only a hint, and some location
     * provider implementations ignored it. For Jellybean and onwards however, it is mandatory for
     * Android compatible devices to observe both the minTime and minDistance parameters.
     *
     * <p>To unregister for location updates, use {@link #removeUpdates(LocationListener)}.
     *
     * @param provider     a provider listed by {@link #getAllProviders()}
     * @param minTimeMs    minimum time interval between location updates in milliseconds
     * @param minDistanceM minimum distance between location updates in meters
     * @param listener     the listener to receive location updates
     *
     * @throws IllegalArgumentException if provider is null or doesn't exist
     * @throws IllegalArgumentException if listener is null
     * @throws RuntimeException if the calling thread has no Looper
     * @throws SecurityException if no suitable permission is present
     */
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public void requestLocationUpdates(@NonNull String provider, long minTimeMs, float minDistanceM,
            @NonNull LocationListener listener) {
        android.util.SeempLog.record(47);
        Preconditions.checkArgument(provider != null, "invalid null provider");
        Preconditions.checkArgument(listener != null, "invalid null listener");

        LocationRequest request = LocationRequest.createFromDeprecatedProvider(
                provider, minTimeMs, minDistanceM, false);
        requestLocationUpdates(request, listener, null);
    }

    /**
     * Register for location updates using the named provider, and a callback on
     * the specified {@link Looper}.
     *
     * <p>See {@link #requestLocationUpdates(String, long, float, LocationListener)}
     * for more detail on how this method works.
     *
     * @param provider     a provider listed by {@link #getAllProviders()}
     * @param minTimeMs    minimum time interval between location updates in milliseconds
     * @param minDistanceM minimum distance between location updates in meters
     * @param listener     the listener to receive location updates
     * @param looper       the looper handling listener callbacks, or null to use the looper of the
     *                     calling thread
     *
     * @throws IllegalArgumentException if provider is null or doesn't exist
     * @throws IllegalArgumentException if listener is null
     * @throws SecurityException if no suitable permission is present
     */
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public void requestLocationUpdates(@NonNull String provider, long minTimeMs, float minDistanceM,
            @NonNull LocationListener listener, @Nullable Looper looper) {
        android.util.SeempLog.record(47);
        Preconditions.checkArgument(provider != null, "invalid null provider");
        Preconditions.checkArgument(listener != null, "invalid null listener");

        LocationRequest request = LocationRequest.createFromDeprecatedProvider(
                provider, minTimeMs, minDistanceM, false);
        requestLocationUpdates(request, listener, looper);
    }

    /**
     * Register for location updates using the named provider, and a callback on
     * the specified {@link Executor}.
     *
     * <p>See {@link #requestLocationUpdates(String, long, float, LocationListener)}
     * for more detail on how this method works.
     *
     * @param provider     a provider listed by {@link #getAllProviders()}
     * @param minTimeMs    minimum time interval between location updates in milliseconds
     * @param minDistanceM minimum distance between location updates in meters
     * @param executor     the executor handling listener callbacks
     * @param listener     the listener to receive location updates
     *
     * @throws IllegalArgumentException if provider is null or doesn't exist
     * @throws IllegalArgumentException if executor is null
     * @throws IllegalArgumentException if listener is null
     * @throws SecurityException if no suitable permission is present
     */
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public void requestLocationUpdates(
            @NonNull String provider,
            long minTimeMs,
            float minDistanceM,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull LocationListener listener) {
        android.util.SeempLog.record(47);
        LocationRequest request = LocationRequest.createFromDeprecatedProvider(
                provider, minTimeMs, minDistanceM, false);
        requestLocationUpdates(request, executor, listener);
    }

    /**
     * Register for location updates using a provider selected through the given Criteria, and a
     * callback on the specified {@link Looper}.
     *
     * <p>See {@link #requestLocationUpdates(String, long, float, LocationListener)}
     * for more detail on how this method works.
     *
     * @param minTimeMs minimum time interval between location updates in milliseconds
     * @param minDistanceM minimum distance between location updates in meters
     * @param criteria contains parameters to choose the appropriate provider for location updates
     * @param listener the listener to receive location updates
     *
     * @throws IllegalArgumentException if criteria is null
     * @throws IllegalArgumentException if listener is null
     * @throws SecurityException if no suitable permission is present
     */
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public void requestLocationUpdates(long minTimeMs, float minDistanceM,
            @NonNull Criteria criteria, @NonNull LocationListener listener,
            @Nullable Looper looper) {
        android.util.SeempLog.record(47);
        Preconditions.checkArgument(criteria != null, "invalid null criteria");
        Preconditions.checkArgument(listener != null, "invalid null listener");

        LocationRequest request = LocationRequest.createFromDeprecatedCriteria(
                criteria, minTimeMs, minDistanceM, false);
        requestLocationUpdates(request, listener, looper);
    }

    /**
     * Register for location updates using a provider selected through the given Criteria, and a
     * callback on the specified {@link Executor}.
     *
     * <p>See {@link #requestLocationUpdates(String, long, float, LocationListener)}
     * for more detail on how this method works.
     *
     * @param minTimeMs minimum time interval between location updates in milliseconds
     * @param minDistanceM minimum distance between location updates in meters
     * @param criteria contains parameters to choose the appropriate provider for location updates
     * @param executor the executor handling listener callbacks
     * @param listener the listener to receive location updates
     *
     * @throws IllegalArgumentException if criteria is null
     * @throws IllegalArgumentException if executor is null
     * @throws IllegalArgumentException if listener is null
     * @throws SecurityException        if no suitable permission is present
     */
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public void requestLocationUpdates(
            long minTimeMs,
            float minDistanceM,
            @NonNull Criteria criteria,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull LocationListener listener) {
        android.util.SeempLog.record(47);
        LocationRequest request = LocationRequest.createFromDeprecatedCriteria(
                criteria, minTimeMs, minDistanceM, false);
        requestLocationUpdates(request, executor, listener);
    }

    /**
     * Register for location updates using the named provider, and callbacks delivered via the
     * provided {@link PendingIntent}.
     *
     * <p>The delivered pending intents will contain extras with the callback information. The keys
     * used for the extras are {@link #KEY_LOCATION_CHANGED} and {@link #KEY_PROVIDER_ENABLED}. See
     * the documentation for each respective extra key for information on the values.
     *
     * <p>To unregister for location updates, use {@link #removeUpdates(PendingIntent)}.
     *
     * <p>See {@link #requestLocationUpdates(String, long, float, LocationListener)}
     * for more detail on how this method works.
     *
     * @param provider      a provider listed by {@link #getAllProviders()}
     * @param minTimeMs     minimum time interval between location updates in milliseconds
     * @param minDistanceM  minimum distance between location updates in meters
     * @param pendingIntent the pending intent to send location updates
     *
     * @throws IllegalArgumentException if provider is null or doesn't exist
     * @throws IllegalArgumentException if pendingIntent is null
     * @throws SecurityException if no suitable permission is present
     */
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public void requestLocationUpdates(@NonNull String provider, long minTimeMs, float minDistanceM,
            @NonNull PendingIntent pendingIntent) {
        android.util.SeempLog.record(47);
        Preconditions.checkArgument(provider != null, "invalid null provider");

        LocationRequest request = LocationRequest.createFromDeprecatedProvider(
                provider, minTimeMs, minDistanceM, false);
        requestLocationUpdates(request, pendingIntent);
    }

    /**
     * Register for location updates using a provider selected through the given Criteria, and
     * callbacks delivered via the provided {@link PendingIntent}.
     *
     * <p>See {@link #requestLocationUpdates(String, long, float, PendingIntent)} for more detail on
     * how this method works.
     *
     * @param minTimeMs minimum time interval between location updates in milliseconds
     * @param minDistanceM minimum distance between location updates in meters
     * @param criteria contains parameters to choose the appropriate provider for location updates
     * @param pendingIntent the pending intent to send location updates
     *
     * @throws IllegalArgumentException if provider is null or doesn't exist
     * @throws IllegalArgumentException if pendingIntent is null
     * @throws SecurityException if no suitable permission is present
     */
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public void requestLocationUpdates(long minTimeMs, float minDistanceM,
            @NonNull Criteria criteria, @NonNull PendingIntent pendingIntent) {
        android.util.SeempLog.record(47);
        Preconditions.checkArgument(criteria != null, "invalid null criteria");

        LocationRequest request = LocationRequest.createFromDeprecatedCriteria(
                criteria, minTimeMs, minDistanceM, false);
        requestLocationUpdates(request, pendingIntent);
    }

    /**
     * Register for location updates using a {@link LocationRequest}, and a callback on the
     * specified {@link Looper}.
     *
     * <p>The system will automatically select and enable the best provider based on the given
     * {@link LocationRequest}. The LocationRequest can be null, in which case the system will
     * choose default low power parameters for location updates, but this is heavily discouraged,
     * and an explicit LocationRequest should always be provided.
     *
     * <p>See {@link #requestLocationUpdates(String, long, float, LocationListener)}
     * for more detail on how this method works.
     *
     * @param locationRequest the location request containing location parameters
     * @param listener the listener to receive location updates
     * @param looper the looper handling listener callbacks, or null to use the looper of the
     *               calling thread
     *
     * @throws IllegalArgumentException if listener is null
     * @throws SecurityException if no suitable permission is present
     *
     * @hide
     */
    @SystemApi
    @TestApi
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public void requestLocationUpdates(
            @Nullable LocationRequest locationRequest,
            @NonNull LocationListener listener,
            @Nullable Looper looper) {
        android.util.SeempLog.record(47);
        Handler handler = looper == null ? new Handler() : new Handler(looper);
        requestLocationUpdates(locationRequest, new HandlerExecutor(handler), listener);
    }

    /**
     * Register for location updates using a {@link LocationRequest}, and a callback on the
     * specified {@link Executor}.
     *
     * <p>See {@link #requestLocationUpdates(LocationRequest, LocationListener, Looper)} for more
     * detail on how this method works.
     *
     * @param locationRequest the location request containing location parameters
     * @param executor the executor handling listener callbacks
     * @param listener the listener to receive location updates
     *
     * @throws IllegalArgumentException if executor is null
     * @throws IllegalArgumentException if listener is null
     * @throws SecurityException if no suitable permission is present
     *
     * @hide
     */
    @SystemApi
    @TestApi
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public void requestLocationUpdates(
            @Nullable LocationRequest locationRequest,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull LocationListener listener) {
        android.util.SeempLog.record(47);
        synchronized (mListeners) {
            LocationListenerTransport transport = mListeners.get(listener);
            if (transport != null) {
                transport.unregister();
            } else {
                transport = new LocationListenerTransport(listener);
                mListeners.put(listener, transport);
            }
            transport.register(executor);

            boolean registered = false;
            try {
                mService.requestLocationUpdates(locationRequest, transport, null,
                        mContext.getPackageName(), mContext.getAttributionTag(),
                        transport.getListenerId());
                registered = true;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            } finally {
                if (!registered) {
                    // allow gc after exception
                    transport.unregister();
                    mListeners.remove(listener);
                }
            }
        }
    }

    /**
     * Register for location updates using a {@link LocationRequest}, and callbacks delivered via
     * the provided {@link PendingIntent}.
     *
     * <p>See {@link #requestLocationUpdates(LocationRequest, LocationListener, Looper)} and
     * {@link #requestLocationUpdates(String, long, float, PendingIntent)} for more detail on how
     * this method works.
     *
     * @param locationRequest the location request containing location parameters
     * @param pendingIntent the pending intent to send location updates
     *
     * @throws IllegalArgumentException if pendingIntent is null
     * @throws SecurityException if no suitable permission is present
     *
     * @hide
     */
    @SystemApi
    @TestApi
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public void requestLocationUpdates(
            @Nullable LocationRequest locationRequest,
            @NonNull PendingIntent pendingIntent) {
        android.util.SeempLog.record(47);
        Preconditions.checkArgument(locationRequest != null, "invalid null location request");
        Preconditions.checkArgument(pendingIntent != null, "invalid null pending intent");
        if (Compatibility.isChangeEnabled(TARGETED_PENDING_INTENT)) {
            Preconditions.checkArgument(pendingIntent.isTargetedToPackage(),
                    "pending intent must be targeted to a package");
        }

        try {
            mService.requestLocationUpdates(locationRequest, null, pendingIntent,
                    mContext.getPackageName(), mContext.getAttributionTag(), null);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Set the last known location with a new location.
     *
     * <p>A privileged client can inject a {@link Location} if it has a better estimate of what
     * the recent location is.  This is especially useful when the device boots up and the GPS
     * chipset is in the process of getting the first fix.  If the client has cached the location,
     * it can inject the {@link Location}, so if an app requests for a {@link Location} from {@link
     * #getLastKnownLocation(String)}, the location information is still useful before getting
     * the first fix.
     *
     * @param location newly available {@link Location} object
     * @return true if the location was injected, false otherwise
     *
     * @throws IllegalArgumentException if location is null
     * @throws SecurityException if permissions are not present
     *
     * @hide
     */
    @RequiresPermission(allOf = {LOCATION_HARDWARE, ACCESS_FINE_LOCATION})
    public boolean injectLocation(@NonNull Location location) {
        Preconditions.checkArgument(location != null, "invalid null location");
        Preconditions.checkArgument(location.isComplete(),
                "incomplete location object, missing timestamp or accuracy?");

        try {
            mService.injectLocation(location);
            return true;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Removes location updates for the specified {@link LocationListener}. Following this call,
     * the listener will no longer receive location updates.
     *
     * @param listener listener that no longer needs location updates
     *
     * @throws IllegalArgumentException if listener is null
     */
    public void removeUpdates(@NonNull LocationListener listener) {
        Preconditions.checkArgument(listener != null, "invalid null listener");

        synchronized (mListeners) {
            LocationListenerTransport transport = mListeners.remove(listener);
            if (transport == null) {
                return;
            }
            transport.unregister();

            try {
                mService.removeUpdates(transport, null);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    /**
     * Removes location updates for the specified {@link PendingIntent}. Following this call, the
     * PendingIntent will no longer receive location updates.
     *
     * @param pendingIntent pending intent that no longer needs location updates
     *
     * @throws IllegalArgumentException if pendingIntent is null
     */
    public void removeUpdates(@NonNull PendingIntent pendingIntent) {
        Preconditions.checkArgument(pendingIntent != null, "invalid null pending intent");

        try {
            mService.removeUpdates(null, pendingIntent);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns a list of the names of all known location providers. All providers are returned,
     * including ones that are not permitted to be accessed by the calling activity or are currently
     * disabled.
     *
     * @return list of provider names
     */
    public @NonNull List<String> getAllProviders() {
        try {
            return mService.getAllProviders();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns a list of the names of location providers. Only providers that the caller has
     * permission to access will be returned.
     *
     * @param enabledOnly if true then only enabled providers are included
     * @return list of provider names
     */
    public @NonNull List<String> getP