/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.phone;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.ims.ImsManager;

/**
 * Provides automatic switcher of WFC modes depending on network status (home or roaming).
 */
public class WfcModeAutomaticSwitcher extends Handler {

    private static final String LOG_TAG = "WfcModeAutomaticSwitcher";
    private static final boolean DBG = (SystemProperties.getInt("ro.debuggable", 0) == 1);

    private static WfcModeAutomaticSwitcher sInstance = null;

    // Message events
    private static final int MSG_DEFAULT_DATA_SUB_CHANGED_EVENT              = 1;
    private static final int MSG_SERVICE_STATE_CHANGED_EVENT                 = 2;

    // Config index and definition
    private static final int WFC_MODE_AUTO_SWITCH_HOME_INDEX                 = 0;
    private static final int WFC_MODE_AUTO_SWITCH_ROAMING_INDEX              = 1;
    private static final int WFC_MODE_AUTO_SWITCH_LENGTH                     = 2;

    private final Context mContext;
    private final TelephonyManager mTm;
    private final SubscriptionManager mSubscriptionManager;
    private PhoneStateListener mPhoneStateListener;
    private ContentObserver mContentObserver;
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private final int[] mWfcModeAutoSwitchConfig;


    private WfcModeAutomaticSwitcher(Context context, int[] wfcModeAutoSwitchConfig) {
        super();
        mContext = context;
        mTm = TelephonyManager.from(mContext);
        mSubscriptionManager = SubscriptionManager.from(mContext);
        mWfcModeAutoSwitchConfig = wfcModeAutoSwitchConfig;
        registerListener();
    }

    /**
     * Initializes WfcModeAutomaticSwitcher if config is valid.
     *
     * @param context The initialization needed context
     *
     * @return WfcModeAutomaticSwitcher, MAY BE NULL if config is invalid.
     */
    public static WfcModeAutomaticSwitcher init(Context context) {
        int[] wfcModeAutoSwitchConfig = context.getResources().getIntArray(
                R.array.wfc_mode_auto_switch_int_array);
        if (isValidConfig(wfcModeAutoSwitchConfig)) {
            if (sInstance == null) {
                sInstance = new WfcModeAutomaticSwitcher(context, wfcModeAutoSwitchConfig);
            } else {
                if (DBG) Log.wtf(LOG_TAG, "init() called multiple times!");
            }
        }
        return sInstance;
    }

    @Override
    public void handleMessage(Message msg) {
        if (DBG) Log.d(LOG_TAG, "handleMessage msg.what = " + msg.what);
        switch (msg.what) {
            case MSG_DEFAULT_DATA_SUB_CHANGED_EVENT:
                handleDefaultDataSubChanged();
                break;
            case MSG_SERVICE_STATE_CHANGED_EVENT:
                handleServiceStateChanged(msg);
                break;
            default:
                if (DBG) Log.wtf(LOG_TAG, "handleMessage unexpected event");
                break;
        }
    }

    /*
     * Handles dds changed event, refreshes WFC mode auto switch rule.
     *
     * According to the new dds change event
     * (1) unregister the previous PhoneStateListener if dds is different
     * (2) register new PhoneStateListener if new dds is valid
     */
    private void handleDefaultDataSubChanged() {
        if (DBG) Log.d(LOG_TAG, "handleDefaultDataSubChanged");
        int newSubId = mSubscriptionManager.getDefaultDataSubId();
        if (mSubId != newSubId) {
            unregisterListener();
            registerListener();
        }
    }

    /*
     * Handles service state changed event, updates WFC mode when roaming state is changed.
     */
    private void handleServiceStateChanged(Message msg) {
        ServiceState ss = (ServiceState) msg.obj;
        boolean isPhoneRoaming = ss != null ? ss.getRoaming() : false;
        if (DBG) Log.d(LOG_TAG, "handleServiceStateChanged roaming: " + isPhoneRoaming);
        updateWfcMode(isPhoneRoaming);
    }

    /*
     * Updates the WFC mode according to the roaming state of dds.
     */
    private void updateWfcMode(boolean isPhoneRoaming) {
        int newWfcMode = isPhoneRoaming
                ? mWfcModeAutoSwitchConfig[WFC_MODE_AUTO_SWITCH_ROAMING_INDEX]
                : mWfcModeAutoSwitchConfig[WFC_MODE_AUTO_SWITCH_HOME_INDEX];
        if (ImsManager.getWfcMode(mContext) != newWfcMode) {
            if (DBG) Log.d(LOG_TAG, "auto setting wfc mode to: " + newWfcMode);
            ImsManager.setWfcMode(mContext, newWfcMode);
        }
    }

    /*
     * Checks whether the config is valid
     *
     * @param config to be checked
     *
     * @return {@code true} if the config is not null and the lengh is valid.
     * {@link #WFC_MODE_AUTO_SWITCH_LENGTH}, {@code false} otherwise.
     */
    private static boolean isValidConfig(int[] config) {
        return config != null
                && config.length == WFC_MODE_AUTO_SWITCH_LENGTH;
    }

    /*
     * Starts listening for service state changes and dds changes
     */
    private void registerListener() {
        if (DBG) Log.d(LOG_TAG, "registerListener");
        if (mContentObserver == null) {
            mContentObserver = new ContentObserver(this) {
                @Override
                public void onChange(boolean selfChange) {
                    sendMessage(obtainMessage(MSG_DEFAULT_DATA_SUB_CHANGED_EVENT));
                }
            };
            if (DBG) Log.d(LOG_TAG, "registerListener: listen dds change event");
            // Listen default data subId change event.
            mContext.getContentResolver().registerContentObserver(
                    Settings.Global.getUriFor(
                    Settings.Global.MULTI_SIM_DATA_CALL_SUBSCRIPTION),
                    false, mContentObserver);
        }
        int dds = mSubscriptionManager.getDefaultDataSubId();
        if (SubscriptionManager.isValidSubscriptionId(dds)) {
            mSubId = dds;
            mPhoneStateListener = new PhoneStateListener(mSubId) {
                @Override
                public void onServiceStateChanged(ServiceState serviceState) {
                    // Notify that the service state is changed
                    sendMessage(obtainMessage(MSG_SERVICE_STATE_CHANGED_EVENT, serviceState));
                }
            };
            if (DBG) Log.d(LOG_TAG, "registerListener: listen service state change event subId="
                    + mSubId);
            // Listen service state change event.
            mTm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE);
        }
    }

    /*
     * Stops listening for service state changes.
     */
    private void unregisterListener() {
        if (DBG) Log.d(LOG_TAG, "unregisterListener subId: " + mSubId);
        if (mPhoneStateListener != null) {
            mTm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
            mPhoneStateListener = null;
        }
    }
}
