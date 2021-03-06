/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings;

import android.app.AlertDialog;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.UiModeManager;
import android.app.WallpaperManager;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceCategory;
import com.android.settings.sdhz150.SeekBarPreference;
import com.android.settings.sdhz150.SeekBarPreferenceA;
import com.android.settings.sdhz150.LockscreenSeekBarPreference;
import android.text.TextUtils;
import android.util.Log;

import android.text.format.DateFormat;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.text.Spannable;
import android.content.Intent;
import android.content.DialogInterface;
import android.telephony.TelephonyManager;

import com.android.internal.app.NightDisplayController;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.view.RotationPolicy;
import com.android.settings.accessibility.ToggleFontSizePreferenceFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;

import java.util.ArrayList;
import java.util.List;

import static android.provider.Settings.Secure.CAMERA_GESTURE_DISABLED;
import static android.provider.Settings.Secure.DOUBLE_TAP_TO_WAKE;
import static android.provider.Settings.Secure.DOZE_ENABLED;
import static android.provider.Settings.Secure.WAKE_GESTURE_ENABLED;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import cyanogenmod.hardware.CMHardwareManager;
import com.android.internal.widget.LockPatternUtils;

public class DisplaySettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {
    private static final String TAG = "DisplaySettings";

    /** If there is no setting in the provider, use this. */
    private static final int FALLBACK_SCREEN_TIMEOUT_VALUE = 30000;

    private static final String PREF_NOTIFICATION_ALPHA = "notification_alpha";
    private static final String PREF_BLOCK_ON_SECURE_KEYGUARD = "block_on_secure_keyguard";
    private static final String LOCKSCREEN_MAX_NOTIF_CONFIG = "lockscreen_max_notif_cofig";
    private static final String PREF_ROWS_PORTRAIT = "qs_rows_portrait";
    private static final String PREF_COLUMNS = "qs_columns";
    private static final String IMMERSIVE_RECENTS = "immersive_recents";
    private static final String TRANSPARENT_POWER_MENU = "transparent_power_menu";
    private static final String TRANSPARENT_POWER_DIALOG_DIM = "transparent_power_dialog_dim";
    private static final String KEY_LISTVIEW_ANIMATION = "listview_animation";
    private static final String KEY_LISTVIEW_INTERPOLATOR = "listview_interpolator";
    private static final String STATUS_BAR_CARRIER = "status_bar_carrier";
    private static final String CUSTOM_CARRIER_LABEL = "custom_carrier_label";
    private static final String CARRIER_SIZE_STYLE = "carrier_size_style";
    private static final String PREF_TRANSPARENT_VOLUME_DIALOG = "transparent_volume_dialog";
	private static final String PREF_QS_TRANSPARENT_SHADE = "qs_transparent_shade";
    private static final String THREE_FINGER_GESTURE = "three_finger_gesture";
    private static final String KEYGUARD_TOGGLE_TORCH = "keyguard_toggle_torch";
    private static final String STATUS_BAR_NETWORK_TRAFFIC_STYLE = "status_bar_network_traffic_style";
    private static final String KEY_CATEGORY_DISPLAY = "display";
    private static final String KEY_SCREEN_TIMEOUT = "screen_timeout";
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_SCREEN_SAVER = "screensaver";
    private static final String KEY_LIFT_TO_WAKE = "lift_to_wake";
    private static final String KEY_DOZE = "doze";
    private static final String KEY_TAP_TO_WAKE = "tap_to_wake";
    private static final String KEY_AUTO_BRIGHTNESS = "auto_brightness";
    private static final String KEY_AUTO_ROTATE = "auto_rotate";
    private static final String KEY_NIGHT_DISPLAY = "night_display";
    private static final String KEY_NIGHT_MODE = "night_mode";
    private static final String KEY_CAMERA_GESTURE = "camera_gesture";
    private static final String KEY_WALLPAPER = "wallpaper";
    private static final String KEY_VR_DISPLAY_PREF = "vr_display_pref";
    private static final String SCREENSHOT_TYPE = "screenshot_type";

    private Preference mFontSizePref;

    private TimeoutListPreference mScreenTimeoutPreference;
    private ListPreference mNightModePreference;
    private Preference mScreenSaverPreference;
    private SwitchPreference mLiftToWakePreference;
    private SwitchPreference mDozePreference;
    private SwitchPreference mTapToWakePreference;
    private SwitchPreference mAutoBrightnessPreference;
    private SwitchPreference mCameraGesturePreference;
    private SwitchPreference mKeyguardToggleTorch;
    private SwitchPreference mThreeFingerGesture;

    private ListPreference mScreenshotType;
    private ListPreference mStatusBarNetworkTraffic;
    private ListPreference mListViewAnimation;
    private ListPreference mListViewInterpolator;
    private ListPreference mImmersiveRecents;
    private ListPreference mRowsPortrait;
    private ListPreference mQsColumns;

    private SeekBarPreference mQSShadeAlpha;
    private SeekBarPreference mVolumeDialogAlpha;
    private SeekBarPreference mNotificationsAlpha;
    private SeekBarPreferenceA  mPowerMenuAlpha;
    private SeekBarPreferenceA  mPowerDialogDim;
    private LockscreenSeekBarPreference mMaxKeyguardNotifConfig;
    private SwitchPreference mBlockOnSecureKeyguard;

    private SwitchPreference mStatusBarCarrier;
    private PreferenceScreen mCustomCarrierLabel;
    private ListPreference mCarrierSize;
    private String mCustomCarrierLabelText;
    private static final int MY_USER_ID = UserHandle.myUserId();

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.DISPLAY;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Activity activity = getActivity();
        final ContentResolver resolver = activity.getContentResolver();
        final LockPatternUtils lockPatternUtils = new LockPatternUtils(getActivity());

        addPreferencesFromResource(R.xml.display_settings);

        PreferenceCategory displayPrefs = (PreferenceCategory)
                findPreference(KEY_CATEGORY_DISPLAY);

        mScreenSaverPreference = findPreference(KEY_SCREEN_SAVER);
        if (mScreenSaverPreference != null
                && getResources().getBoolean(
                        com.android.internal.R.bool.config_dreamsSupported) == false) {
            getPreferenceScreen().removePreference(mScreenSaverPreference);
        }

        mCarrierSize = (ListPreference) findPreference(CARRIER_SIZE_STYLE);
        int CarrierSize = Settings.System.getInt(resolver,
                Settings.System.CARRIER_SIZE, 5);
        mCarrierSize.setValue(String.valueOf(CarrierSize));
        mCarrierSize.setSummary(mCarrierSize.getEntry());
        mCarrierSize.setOnPreferenceChangeListener(this);

        PreferenceScreen prefSet = getPreferenceScreen();
        mStatusBarCarrier = (SwitchPreference) prefSet
                .findPreference(STATUS_BAR_CARRIER);
        mStatusBarCarrier.setChecked((Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_CARRIER, 0) == 1));
        mStatusBarCarrier.setOnPreferenceChangeListener(this);
        mCustomCarrierLabel = (PreferenceScreen) prefSet
                .findPreference(CUSTOM_CARRIER_LABEL);
        if (TelephonyManager.getDefault().isMultiSimEnabled()) {
            prefSet.removePreference(mStatusBarCarrier);
            prefSet.removePreference(mCustomCarrierLabel);
        } else {
            updateCustomLabelTextSummary();
        }

        mBlockOnSecureKeyguard = (SwitchPreference) findPreference(PREF_BLOCK_ON_SECURE_KEYGUARD);
        if (lockPatternUtils.isSecure(MY_USER_ID)) {
            mBlockOnSecureKeyguard.setChecked(Settings.Secure.getInt(resolver,
                    Settings.Secure.STATUS_BAR_LOCKED_ON_SECURE_KEYGUARD, 1) == 1);
            mBlockOnSecureKeyguard.setOnPreferenceChangeListener(this);
        } else if (mBlockOnSecureKeyguard != null) {
            prefSet.removePreference(mBlockOnSecureKeyguard);
        }

        mListViewAnimation = (ListPreference) findPreference(KEY_LISTVIEW_ANIMATION);
        int listviewanimation = Settings.System.getInt(resolver,
                Settings.System.LISTVIEW_ANIMATION, 0);
        mListViewAnimation.setValue(String.valueOf(listviewanimation));
        mListViewAnimation.setSummary(mListViewAnimation.getEntry());
        mListViewAnimation.setOnPreferenceChangeListener(this);

        mListViewInterpolator = (ListPreference) findPreference(KEY_LISTVIEW_INTERPOLATOR);
        int listviewinterpolator = Settings.System.getInt(resolver,
                Settings.System.LISTVIEW_INTERPOLATOR, 0);
        mListViewInterpolator.setValue(String.valueOf(listviewinterpolator));
        mListViewInterpolator.setSummary(mListViewInterpolator.getEntry());
        mListViewInterpolator.setOnPreferenceChangeListener(this);

        // Notifications alpha
        mNotificationsAlpha = (SeekBarPreference) findPreference(PREF_NOTIFICATION_ALPHA);
        int notificationsAlpha = Settings.System.getInt(getContentResolver(),
                Settings.System.NOTIFICATION_ALPHA, 255);
        mNotificationsAlpha.setValue(notificationsAlpha / 1);
        mNotificationsAlpha.setOnPreferenceChangeListener(this);

        mScreenshotType = (ListPreference) findPreference(SCREENSHOT_TYPE);
        int mScreenshotTypeValue = Settings.System.getInt(getContentResolver(),
                Settings.System.SCREENSHOT_TYPE, 0);
        mScreenshotType.setValue(String.valueOf(mScreenshotTypeValue));
        mScreenshotType.setSummary(mScreenshotType.getEntry());
        mScreenshotType.setOnPreferenceChangeListener(this);

        // QS shade alpha
        mQSShadeAlpha = (SeekBarPreference) findPreference(PREF_QS_TRANSPARENT_SHADE);
        int qSShadeAlpha = Settings.System.getInt(getContentResolver(),
                Settings.System.QS_TRANSPARENT_SHADE, 255);
        mQSShadeAlpha.setValue(qSShadeAlpha / 1);
        mQSShadeAlpha.setOnPreferenceChangeListener(this);
        mRowsPortrait = (ListPreference) findPreference(PREF_ROWS_PORTRAIT);
        int rowsPortrait = Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.QS_ROWS_PORTRAIT, 3);
        mRowsPortrait.setValue(String.valueOf(rowsPortrait));
        mRowsPortrait.setSummary(mRowsPortrait.getEntry());
        mRowsPortrait.setOnPreferenceChangeListener(this);

        mQsColumns = (ListPreference) findPreference(PREF_COLUMNS);
        int columnsQs = Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.QS_COLUMNS, 3);
        mQsColumns.setValue(String.valueOf(columnsQs));
        mQsColumns.setSummary(mQsColumns.getEntry());
        mQsColumns.setOnPreferenceChangeListener(this);

        mMaxKeyguardNotifConfig = (LockscreenSeekBarPreference) findPreference(LOCKSCREEN_MAX_NOTIF_CONFIG);
        int kgconf = Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_MAX_NOTIF_CONFIG, 5);
        mMaxKeyguardNotifConfig.setValue(kgconf / 1);
        mMaxKeyguardNotifConfig.setOnPreferenceChangeListener(this);

        // Volume dialog alpha
        mVolumeDialogAlpha = (SeekBarPreference) findPreference(PREF_TRANSPARENT_VOLUME_DIALOG);
        int volumeDialogAlpha = Settings.System.getInt(getContentResolver(),
                Settings.System.TRANSPARENT_VOLUME_DIALOG, 255);
        mVolumeDialogAlpha.setValue(volumeDialogAlpha / 1);
        mVolumeDialogAlpha.setOnPreferenceChangeListener(this);

        mImmersiveRecents = (ListPreference) findPreference(IMMERSIVE_RECENTS);
        mImmersiveRecents.setValue(String.valueOf(Settings.System.getInt(
                resolver, Settings.System.IMMERSIVE_RECENTS, 0)));
        mImmersiveRecents.setSummary(mImmersiveRecents.getEntry());
        mImmersiveRecents.setOnPreferenceChangeListener(this);

            // Power menu alpha
            mPowerMenuAlpha = (SeekBarPreferenceA) findPreference(TRANSPARENT_POWER_MENU);
            int powerMenuAlpha = Settings.System.getInt(resolver,
                    Settings.System.TRANSPARENT_POWER_MENU, 100);
            mPowerMenuAlpha.setValue(powerMenuAlpha / 1);
            mPowerMenuAlpha.setOnPreferenceChangeListener(this);

            // Power/reboot dialog dim
            mPowerDialogDim = (SeekBarPreferenceA) findPreference(TRANSPARENT_POWER_DIALOG_DIM);
            int powerDialogDim = Settings.System.getInt(resolver,
                    Settings.System.TRANSPARENT_POWER_DIALOG_DIM, 50);
            mPowerDialogDim.setValue(powerDialogDim / 1);
            mPowerDialogDim.setOnPreferenceChangeListener(this);

        mStatusBarNetworkTraffic = (ListPreference) findPreference(STATUS_BAR_NETWORK_TRAFFIC_STYLE);
        int networkTrafficStyle = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_NETWORK_TRAFFIC_STYLE, 3);
        mStatusBarNetworkTraffic.setValue(String.valueOf(networkTrafficStyle));
        mStatusBarNetworkTraffic
                .setSummary(mStatusBarNetworkTraffic.getEntry());
        mStatusBarNetworkTraffic.setOnPreferenceChangeListener(this);

        mKeyguardToggleTorch =
                (SwitchPreference) findPreference(KEYGUARD_TOGGLE_TORCH);
        mKeyguardToggleTorch.setChecked((Settings.System.getInt(resolver,
                Settings.System.KEYGUARD_TOGGLE_TORCH, 0) == 1));
        mKeyguardToggleTorch.setOnPreferenceChangeListener(this);

        mThreeFingerGesture =
                (SwitchPreference) findPreference(THREE_FINGER_GESTURE);
        mThreeFingerGesture.setChecked((Settings.System.getInt(resolver,
                Settings.System.THREE_FINGER_GESTURE, 0) == 1));
        mThreeFingerGesture.setOnPreferenceChangeListener(this);

        mScreenTimeoutPreference = (TimeoutListPreference) findPreference(KEY_SCREEN_TIMEOUT);

        mFontSizePref = findPreference(KEY_FONT_SIZE);

        if (!NightDisplayController.isAvailable(activity)) {
            removePreference(KEY_NIGHT_DISPLAY);
        }

        if (displayPrefs != null) {
            mAutoBrightnessPreference = (SwitchPreference) findPreference(KEY_AUTO_BRIGHTNESS);
            if (mAutoBrightnessPreference != null) {
                if (isAutomaticBrightnessAvailable(getResources())) {
                    mAutoBrightnessPreference.setOnPreferenceChangeListener(this);
                } else {
                    displayPrefs.removePreference(mAutoBrightnessPreference);
                }
            }

            mLiftToWakePreference = (SwitchPreference) findPreference(KEY_LIFT_TO_WAKE);
            if (mLiftToWakePreference != null) {
                if (isLiftToWakeAvailable(activity)) {
                    mLiftToWakePreference.setOnPreferenceChangeListener(this);
                } else {
                    displayPrefs.removePreference(mLiftToWakePreference);
                }
            }

            mDozePreference = (SwitchPreference) findPreference(KEY_DOZE);
            if (mDozePreference != null) {
                if (isDozeAvailable(activity)) {
                    mDozePreference.setOnPreferenceChangeListener(this);
                } else {
                    displayPrefs.removePreference(mDozePreference);
                }
            }

            mTapToWakePreference = (SwitchPreference) findPreference(KEY_TAP_TO_WAKE);
            if (mTapToWakePreference != null) {
                if (isTapToWakeAvailable(getResources())) {
                    mTapToWakePreference.setOnPreferenceChangeListener(this);
                } else {
                    displayPrefs.removePreference(mTapToWakePreference);
                }
            }

            mCameraGesturePreference = (SwitchPreference) findPreference(KEY_CAMERA_GESTURE);
            if (mCameraGesturePreference != null) {
                if (isCameraGestureAvailable(getResources())) {
                    mCameraGesturePreference.setOnPreferenceChangeListener(this);
                } else {
                    displayPrefs.removePreference(mCameraGesturePreference);
                }
            }

            DropDownPreference rotatePreference =
                    (DropDownPreference) findPreference(KEY_AUTO_ROTATE);
            if (rotatePreference != null) {
                if (RotationPolicy.isRotationLockToggleVisible(activity)) {
                    int rotateLockedResourceId;
                    // The following block sets the string used when rotation is locked.
                    // If the device locks specifically to portrait or landscape (rather than current
                    // rotation), then we use a different string to include this information.
                    if (allowAllRotations(activity)) {
                        rotateLockedResourceId = R.string.display_auto_rotate_stay_in_current;
                    } else {
                        if (RotationPolicy.getRotationLockOrientation(activity)
                                == Configuration.ORIENTATION_PORTRAIT) {
                            rotateLockedResourceId =
                                    R.string.display_auto_rotate_stay_in_portrait;
                        } else {
                            rotateLockedResourceId =
                                    R.string.display_auto_rotate_stay_in_landscape;
                        }
                    }
                    rotatePreference.setEntries(new CharSequence[] {
                            activity.getString(R.string.display_auto_rotate_rotate),
                            activity.getString(rotateLockedResourceId),
                    });
                    rotatePreference.setEntryValues(new CharSequence[] { "0", "1" });
                    rotatePreference.setValueIndex(RotationPolicy.isRotationLocked(activity) ?
                            1 : 0);
                    rotatePreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            final boolean locked = Integer.parseInt((String) newValue) != 0;
                            MetricsLogger.action(getActivity(), MetricsEvent.ACTION_ROTATION_LOCK,
                                    locked);
                            RotationPolicy.setRotationLock(activity, locked);
                            return true;
                        }
                    });
                } else {
                    displayPrefs.removePreference(rotatePreference);
                }
            }

            DropDownPreference vrDisplayPref =
                    (DropDownPreference) findPreference(KEY_VR_DISPLAY_PREF);
            if (vrDisplayPref != null) {
                if (isVrDisplayModeAvailable(activity)) {
                    vrDisplayPref.setEntries(new CharSequence[] {
                            activity.getString(R.string.display_vr_pref_low_persistence),
                            activity.getString(R.string.display_vr_pref_off),
                    });
                    vrDisplayPref.setEntryValues(new CharSequence[] { "0", "1" });

                    final Context c = activity;
                    int currentUser = ActivityManager.getCurrentUser();
                    int current = Settings.Secure.getIntForUser(c.getContentResolver(),
                                    Settings.Secure.VR_DISPLAY_MODE,
                                    /*default*/Settings.Secure.VR_DISPLAY_MODE_LOW_PERSISTENCE,
                                    currentUser);
                    vrDisplayPref.setValueIndex(current);
                    vrDisplayPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            int i = Integer.parseInt((String) newValue);
                            int u = ActivityManager.getCurrentUser();
                            if (!Settings.Secure.putIntForUser(c.getContentResolver(),
                                    Settings.Secure.VR_DISPLAY_MODE,
                                    i, u)) {
                                Log.e(TAG, "Could not change setting for " +
                                        Settings.Secure.VR_DISPLAY_MODE);
                            }
                            return true;
                        }
                    });
                } else {
                    displayPrefs.removePreference(vrDisplayPref);
                }
            }
        }

        mNightModePreference = (ListPreference) findPreference(KEY_NIGHT_MODE);
        if (mNightModePreference != null) {
            final UiModeManager uiManager = (UiModeManager) getSystemService(
                    Context.UI_MODE_SERVICE);
            final int currentNightMode = uiManager.getNightMode();
            mNightModePreference.setValue(String.valueOf(currentNightMode));
            mNightModePreference.setOnPreferenceChangeListener(this);
        }
    }

     private void updateCustomLabelTextSummary() {
        mCustomCarrierLabelText = Settings.System.getString(getActivity()
                .getContentResolver(), Settings.System.CUSTOM_CARRIER_LABEL);

        if (TextUtils.isEmpty(mCustomCarrierLabelText)) {
            mCustomCarrierLabel
                    .setSummary(R.string.custom_carrier_label_notset);
        } else {
            mCustomCarrierLabel.setSummary(mCustomCarrierLabelText);
        }
    }

    private static boolean allowAllRotations(Context context) {
        return Resources.getSystem().getBoolean(
                com.android.internal.R.bool.config_allowAllRotations);
    }

    private static boolean isLiftToWakeAvailable(Context context) {
        SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        return sensors != null && sensors.getDefaultSensor(Sensor.TYPE_WAKE_GESTURE) != null;
    }

    private static boolean isDozeAvailable(Context context) {
        String name = Build.IS_DEBUGGABLE ? SystemProperties.get("debug.doze.component") : null;
        if (TextUtils.isEmpty(name)) {
            name = context.getResources().getString(
                    com.android.internal.R.string.config_dozeComponent);
        }
        return !TextUtils.isEmpty(name);
    }

    private static boolean isTapToWakeAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_supportDoubleTapWake);
    }

    private static boolean isAutomaticBrightnessAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_automatic_brightness_available);
    }

    private static boolean isCameraGestureAvailable(Resources res) {
        boolean configSet = res.getInteger(
                com.android.internal.R.integer.config_cameraLaunchGestureSensorType) != -1;
        return configSet &&
                !SystemProperties.getBoolean("gesture.disable_camera_launch", false);
    }

    private static boolean isVrDisplayModeAvailable(Context context) {
        PackageManager pm = context.getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_VR_MODE_HIGH_PERFORMANCE);
    }

    private void updateTimeoutPreferenceDescription(long currentTimeout) {
        TimeoutListPreference preference = mScreenTimeoutPreference;
        String summary;
        if (preference.isDisabledByAdmin()) {
            summary = getString(R.string.disabled_by_policy_title);
        } else if (currentTimeout < 0) {
            // Unsupported value
            summary = "";
        } else {
            final CharSequence[] entries = preference.getEntries();
            final CharSequence[] values = preference.getEntryValues();
            if (entries == null || entries.length == 0) {
                summary = "";
            } else {
                int best = 0;
                for (int i = 0; i < values.length; i++) {
                    long timeout = Long.parseLong(values[i].toString());
                    if (currentTimeout >= timeout) {
                        best = i;
                    }
                }
                summary = getString(R.string.screen_timeout_summary, entries[best]);
            }
        }
        preference.setSummary(summary);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateState();

        final long currentTimeout = Settings.System.getLong(getActivity().getContentResolver(),
                SCREEN_OFF_TIMEOUT, FALLBACK_SCREEN_TIMEOUT_VALUE);
        mScreenTimeoutPreference.setValue(String.valueOf(currentTimeout));
        mScreenTimeoutPreference.setOnPreferenceChangeListener(this);
        final DevicePolicyManager dpm = (DevicePolicyManager) getActivity().getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        if (dpm != null) {
            final EnforcedAdmin admin = RestrictedLockUtils.checkIfMaximumTimeToLockIsSet(
                    getActivity());
            final long maxTimeout = dpm
                    .getMaximumTimeToLockForUserAndProfiles(UserHandle.myUserId());
            mScreenTimeoutPreference.removeUnusableTimeouts(maxTimeout, admin);
        }
        updateTimeoutPreferenceDescription(currentTimeout);

        disablePreferenceIfManaged(KEY_WALLPAPER, UserManager.DISALLOW_SET_WALLPAPER);
    }

    private void updateState() {
        updateFontSizeSummary();
        updateScreenSaverSummary();

        // Update auto brightness if it is available.
        if (mAutoBrightnessPreference != null) {
            int brightnessMode = Settings.System.getInt(getContentResolver(),
                    SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);
            mAutoBrightnessPreference.setChecked(brightnessMode != SCREEN_BRIGHTNESS_MODE_MANUAL);
        }

        // Update lift-to-wake if it is available.
        if (mLiftToWakePreference != null) {
            int value = Settings.Secure.getInt(getContentResolver(), WAKE_GESTURE_ENABLED, 0);
            mLiftToWakePreference.setChecked(value != 0);
        }

        // Update tap to wake if it is available.
        if (mTapToWakePreference != null) {
            int value = Settings.Secure.getInt(getContentResolver(), DOUBLE_TAP_TO_WAKE, 0);
            mTapToWakePreference.setChecked(value != 0);
        }

        // Update doze if it is available.
        if (mDozePreference != null) {
            int value = Settings.Secure.getInt(getContentResolver(), DOZE_ENABLED, 1);
            mDozePreference.setChecked(value != 0);
        }

        // Update camera gesture #1 if it is available.
        if (mCameraGesturePreference != null) {
            int value = Settings.Secure.getInt(getContentResolver(), CAMERA_GESTURE_DISABLED, 0);
            mCameraGesturePreference.setChecked(value == 0);
        }
    }

    private void updateScreenSaverSummary() {
        if (mScreenSaverPreference != null) {
            mScreenSaverPreference.setSummary(
                    DreamSettings.getSummaryTextWithDreamName(getActivity()));
        }
    }

    private void updateFontSizeSummary() {
        final Context context = mFontSizePref.getContext();
        final float currentScale = Settings.System.getFloat(context.getContentResolver(),
                Settings.System.FONT_SCALE, 1.0f);
        final Resources res = context.getResources();
        final String[] entries = res.getStringArray(R.array.entries_font_size);
        final String[] strEntryValues = res.getStringArray(R.array.entryvalues_font_size);
        final int index = ToggleFontSizePreferenceFragment.fontSizeValueToIndex(currentScale,
                strEntryValues);
        mFontSizePref.setSummary(entries[index]);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (KEY_SCREEN_TIMEOUT.equals(key)) {
            try {
                int value = Integer.parseInt((String) objValue);
                Settings.System.putInt(getContentResolver(), SCREEN_OFF_TIMEOUT, value);
                updateTimeoutPreferenceDescription(value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist screen timeout setting", e);
            }
        }
        if (preference == mAutoBrightnessPreference) {
            boolean auto = (Boolean) objValue;
            Settings.System.putInt(getContentResolver(), SCREEN_BRIGHTNESS_MODE,
                    auto ? SCREEN_BRIGHTNESS_MODE_AUTOMATIC : SCREEN_BRIGHTNESS_MODE_MANUAL);
        }
        if (preference == mLiftToWakePreference) {
            boolean value = (Boolean) objValue;
            Settings.Secure.putInt(getContentResolver(), WAKE_GESTURE_ENABLED, value ? 1 : 0);
        }
        if (preference == mDozePreference) {
            boolean value = (Boolean) objValue;
            Settings.Secure.putInt(getContentResolver(), DOZE_ENABLED, value ? 1 : 0);
        }
        if (preference == mTapToWakePreference) {
            boolean value = (Boolean) objValue;
            Settings.Secure.putInt(getContentResolver(), DOUBLE_TAP_TO_WAKE, value ? 1 : 0);
        }
        if (preference == mCameraGesturePreference) {
            boolean value = (Boolean) objValue;
            Settings.Secure.putInt(getContentResolver(), CAMERA_GESTURE_DISABLED,
                    value ? 0 : 1 /* Backwards because setting is for disabling */);
        }
        if (preference == mStatusBarNetworkTraffic) {
	    String newValue = (String) objValue;
             int networkTrafficStyle = Integer.valueOf((String) newValue);
             int index = mStatusBarNetworkTraffic
                     .findIndexOfValue((String) newValue);
             Settings.System.putInt(getContentResolver(),
                     Settings.System.STATUS_BAR_NETWORK_TRAFFIC_STYLE,
                     networkTrafficStyle);
             mStatusBarNetworkTraffic.setSummary(mStatusBarNetworkTraffic
                     .getEntries()[index]);
        }
        if  (preference == mScreenshotType) {
            int mScreenshotTypeValue = Integer.parseInt(((String) objValue).toString());
            mScreenshotType.setSummary(
                    mScreenshotType.getEntries()[mScreenshotTypeValue]);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SCREENSHOT_TYPE, mScreenshotTypeValue);
            mScreenshotType.setValue(String.valueOf(mScreenshotTypeValue));
         }
        if (preference == mRowsPortrait) {
            int intValue = Integer.valueOf((String) objValue);
            int index = mRowsPortrait.findIndexOfValue((String) objValue);
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.QS_ROWS_PORTRAIT, intValue);
            preference.setSummary(mRowsPortrait.getEntries()[index]);
        }
        if (preference == mQsColumns) {
            int intValue = Integer.valueOf((String) objValue);
            int index = mQsColumns.findIndexOfValue((String) objValue);
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.QS_COLUMNS, intValue);
            preference.setSummary(mQsColumns.getEntries()[index]);
        }
        if (preference == mMaxKeyguardNotifConfig) {
            int kgconf = (Integer) objValue;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_MAX_NOTIF_CONFIG, kgconf);
        }
        if (preference == mNotificationsAlpha) {
          int alpha = (Integer) objValue;
          Settings.System.putInt(getContentResolver(),
                  Settings.System.NOTIFICATION_ALPHA, alpha * 1);
        }
        if (preference == mQSShadeAlpha) {
            int alpha = (Integer) objValue;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.QS_TRANSPARENT_SHADE, alpha * 1);
        }
        if (preference == mListViewAnimation) {
            int listviewanimation = Integer.valueOf((String) objValue);
            int index = mListViewAnimation.findIndexOfValue((String) objValue);
            Settings.System.putInt(getContentResolver(), Settings.System.LISTVIEW_ANIMATION,
                    listviewanimation);
            mListViewAnimation.setSummary(mListViewAnimation.getEntries()[index]);
        }
        if (preference == mBlockOnSecureKeyguard) {
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.STATUS_BAR_LOCKED_ON_SECURE_KEYGUARD,
                    (Boolean) objValue ? 1 : 0);
        }
        if (preference == mListViewInterpolator) {
            int listviewinterpolator = Integer.valueOf((String) objValue);
            int index = mListViewInterpolator.findIndexOfValue((String) objValue);
            Settings.System.putInt(getContentResolver(), Settings.System.LISTVIEW_INTERPOLATOR,
                    listviewinterpolator);
            mListViewInterpolator.setSummary(mListViewInterpolator.getEntries()[index]);
        }
        if (preference == mVolumeDialogAlpha) {
          int alpha = (Integer) objValue;
          Settings.System.putInt(getContentResolver(),
                  Settings.System.TRANSPARENT_VOLUME_DIALOG, alpha * 1);
        }
        if (preference == mPowerMenuAlpha) {
          int alpha = (Integer) objValue;
          Settings.System.putInt(getContentResolver(),
                  Settings.System.TRANSPARENT_POWER_MENU, alpha * 1);
        }
        if (preference == mPowerDialogDim) {
          int alpha = (Integer) objValue;
          Settings.System.putInt(getContentResolver(),
                  Settings.System.TRANSPARENT_POWER_DIALOG_DIM, alpha * 1);
        }
        if (preference == mImmersiveRecents) {
            Settings.System.putInt(getContentResolver(), Settings.System.IMMERSIVE_RECENTS,
                    Integer.valueOf((String) objValue));
            mImmersiveRecents.setValue(String.valueOf(objValue));
            mImmersiveRecents.setSummary(mImmersiveRecents.getEntry());
        }
        if (preference == mKeyguardToggleTorch) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.KEYGUARD_TOGGLE_TORCH,
                    (Boolean) objValue ? 1 : 0);
        }
        if (preference == mThreeFingerGesture) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.THREE_FINGER_GESTURE,
                    (Boolean) objValue ? 1 : 0);
        }
        if (preference == mStatusBarCarrier) {
             boolean value = (Boolean) objValue;
             Settings.System.putInt(getContentResolver(),
                     Settings.System.STATUS_BAR_CARRIER, value ? 1 : 0);
             Intent i = new Intent();
             i.setAction(Intent.ACTION_CUSTOM_CARRIER_LABEL_CHANGED);
             getActivity().sendBroadcast(i);
         }
         if (preference == mCarrierSize) {
	    String newValue = (String) objValue;
             int CarrierSize = Integer.valueOf((String) newValue);
             int index = mCarrierSize.findIndexOfValue((String) newValue);
             Settings.System.putInt(getContentResolver(), Settings.System.CARRIER_SIZE,
                     CarrierSize);
             mCarrierSize.setSummary(mCarrierSize.getEntries()[index]);
             Intent i = new Intent();
             i.setAction(Intent.ACTION_CUSTOM_CARRIER_LABEL_CHANGED);
             getActivity().sendBroadcast(i);

         }
        if (preference == mNightModePreference) {
            try {
                final int value = Integer.parseInt((String) objValue);
                final UiModeManager uiManager = (UiModeManager) getSystemService(
                        Context.UI_MODE_SERVICE);
                uiManager.setNightMode(value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist night mode setting", e);
            }
        }
        return true;
    }

	 @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        final ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mDozePreference) {
            MetricsLogger.action(getActivity(), MetricsEvent.ACTION_AMBIENT_DISPLAY);
        }
        if (preference.getKey().equals(CUSTOM_CARRIER_LABEL)) {
            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
            alert.setTitle(R.string.custom_carrier_label_title);
            alert.setMessage(R.string.custom_carrier_label_explain);
            LinearLayout parent = new LinearLayout(getActivity());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            parent.setLayoutParams(params);
            // Set an EditText view to get user input
            final EditText input = new EditText(getActivity());
            input.setText(TextUtils.isEmpty(mCustomCarrierLabelText) ? ""
                    : mCustomCarrierLabelText);
            input.setSelection(input.getText().length());
            params.setMargins(60, 0, 60, 0);
            input.setLayoutParams(params);
            parent.addView(input);
            alert.setView(parent);
            alert.setPositiveButton(getString(android.R.string.ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                            String value = ((Spannable) input.getText())
                                    .toString().trim();
                            Settings.System
                                    .putString(
                                            resolver,
                                            Settings.System.CUSTOM_CARRIER_LABEL,
                                            value);
                            updateCustomLabelTextSummary();
                            Intent i = new Intent();
                            i.setAction(Intent.ACTION_CUSTOM_CARRIER_LABEL_CHANGED);
                            getActivity().sendBroadcast(i);
                        }
                    });
            alert.setNegativeButton(getString(android.R.string.cancel), null);
            alert.show();
        } 
          return super.onPreferenceTreeClick(preference);
      }

    @Override
    protected int getHelpResource() {
        return R.string.help_uri_display;
    }

    private void disablePreferenceIfManaged(String key, String restriction) {
        final RestrictedPreference pref = (RestrictedPreference) findPreference(key);
        if (pref != null) {
            pref.setDisabledByAdmin(null);
            if (RestrictedLockUtils.hasBaseUserRestriction(getActivity(), restriction,
                    UserHandle.myUserId())) {
                pref.setEnabled(false);
            } else {
                pref.checkRestrictionAndSetDisabled(restriction);
            }
        }
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final Context mContext;
        private final SummaryLoader mLoader;

        private SummaryProvider(Context context, SummaryLoader loader) {
            mContext = context;
            mLoader = loader;
        }

        @Override
        public void setListening(boolean listening) {
            if (listening) {
                updateSummary();
            }
        }

        private void updateSummary() {
            boolean auto = Settings.System.getInt(mContext.getContentResolver(),
                    SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
                    == SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
            mLoader.setSummary(this, mContext.getString(auto ? R.string.display_summary_on
                    : R.string.display_summary_off));
        }
    }

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY
            = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity,
                                                                   SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.display_settings;
                    result.add(sir);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    ArrayList<String> result = new ArrayList<String>();
                    if (!context.getResources().getBoolean(
                            com.android.internal.R.bool.config_dreamsSupported)) {
                        result.add(KEY_SCREEN_SAVER);
                    }
                    if (!isAutomaticBrightnessAvailable(context.getResources())) {
                        result.add(KEY_AUTO_BRIGHTNESS);
                    }
                    if (!NightDisplayController.isAvailable(context)) {
                        result.add(KEY_NIGHT_DISPLAY);
                    }
                    if (!isLiftToWakeAvailable(context)) {
                        result.add(KEY_LIFT_TO_WAKE);
                    }
                    if (!isDozeAvailable(context)) {
                        result.add(KEY_DOZE);
                    }
                    if (!RotationPolicy.isRotationLockToggleVisible(context)) {
                        result.add(KEY_AUTO_ROTATE);
                    }
                    if (!isTapToWakeAvailable(context.getResources())) {
                        result.add(KEY_TAP_TO_WAKE);
                    }
                    if (!isCameraGestureAvailable(context.getResources())) {
                        result.add(KEY_CAMERA_GESTURE);
                    }
                    if (!isVrDisplayModeAvailable(context)) {
                        result.add(KEY_VR_DISPLAY_PREF);
                    }
                    if (!context.getResources().getBoolean(
                            org.cyanogenmod.platform.internal.R.bool.config_proximityCheckOnWake)) {
                        result.add("proximity_on_wake");
                    }
                    if (!CMHardwareManager.getInstance(context).
                            isSupported(CMHardwareManager.FEATURE_HIGH_TOUCH_SENSITIVITY)) {
                        result.add("high_touch_sensitivity_enable");
                    }
                    return result;
                }
            };
}
