/*
 * Copyright (C) 2006 The Android Open Source Project
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

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.telephony.ServiceState;
import android.view.MenuItem;

import com.android.internal.telephony.RILConstants.SimCardID;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccCardConstants;

import java.util.Timer;
import java.util.TimerTask;
import android.content.BroadcastReceiver;

/**
 * "Mobile network settings" screen.  This preference screen lets you
 * enable/disable mobile data, and control data roaming and other
 * network-specific mobile data features.  It's used on non-voice-capable
 * tablets as well as regular phone devices.
 *
 * Note that this PreferenceActivity is part of the phone app, even though
 * you reach it from the "Wireless & Networks" section of the main
 * Settings app.  It's not part of the "Call settings" hierarchy that's
 * available from the Phone app (see CallFeaturesSetting for that.)
 */
public class MobileNetworkSettings extends PreferenceActivity
        implements DialogInterface.OnClickListener,
        DialogInterface.OnDismissListener, Preference.OnPreferenceChangeListener {

    // debug data
    private static final String LOG_TAG = "NetworkSettings";
    private static final boolean DBG = true;
    public static final int REQUEST_CODE_EXIT_ECM = 17;

    //String keys for preference lookup
    private static final String BUTTON_DATA_ENABLED_KEY = "button_data_enabled_key";
    private static final String BUTTON_BPM_ENABLED_KEY = "button_bpm_enabled_key";
    private static final String BUTTON_PREFERED_NETWORK_MODE = "preferred_network_mode_key";
    private static final String BUTTON_ROAMING_KEY = "button_roaming_key";
    private static final String BUTTON_ROAMING_2_KEY = "button_roaming_2_key";
    private static final String BUTTON_CDMA_LTE_DATA_SERVICE_KEY = "cdma_lte_data_service_key";
    private static final String BUTTON_ENABLED_NETWORKS_KEY = "enabled_networks_key";
    private static final String BUTTON_CARRIER_SETTINGS_KEY = "carrier_settings_key";

    private static final String BUTTON_DEFAULT_SIM_DATA_CONNECTION_KEY = "default_sim_for_data_connection_key";

    private static final String BUTTON_DEFAULT_SIM_FOR_3G_NETWORK_KEY = "default_sim_for_3g_network_key";

    static final int preferredNetworkMode = Phone.PREFERRED_NT_MODE;

    //Information about logical "up" Activity
    private static final String UP_ACTIVITY_PACKAGE = "com.android.settings";
    private static final String UP_ACTIVITY_CLASS =
            "com.android.settings.Settings$WirelessSettingsActivity";

    //UI objects
    private ListPreference mButtonPreferredNetworkMode;
    private ListPreference mButtonEnabledNetworks;
    private CheckBoxPreference mButtonDataRoam;
    private CheckBoxPreference mButtonDataRoam2 = null;
    private CheckBoxPreference mButtonDataEnabled;
    private CheckBoxPreference mButtonBPMEnabled = null;
    private Preference mLteDataServicePref;

	//Added for dual sim
    private ListPreference mDefaultSimForDataConnection = null;
    private ListPreference mDefaultSimFor3GNetwork = null;
    private static final int NETWORK_TYPE_UNDEFINED = -1;
    private static final int NETWORK_TYPE_SIM1_3G_SIM2_2G = 0;
    private static final int NETWORK_TYPE_SIM1_2G_SIM2_3G = 1;
    private static final int NETWORK_TYPE_SIM1_2G_SIM2_2G = 2;
    private static final int NETWORK_TYPE_SIM1_3G_SIM2_3G = 3;

    private static final String iface = "rmnet0"; //TODO: this will go away

    private Phone mPhone;
    private Phone mPhone2;
    private MyHandler mHandler;
    private boolean mOkClicked;
    private boolean mOkClicked2;

    //GsmUmts options and Cdma options
    GsmUmtsOptions mGsmUmtsOptions;
    GsmUmtsOptions2 mGsmUmtsOptions2 = null;
    CdmaOptions mCdmaOptions;

    private Preference mClickedPreference;
    private boolean mShow4GForLTE;
    private boolean mIsGlobalCdma;

    //Added for dual sim
    private AlertDialog mRoamConfirm1;
    private AlertDialog mRoamConfirm2;
    private ConnectivityManager mConnManager;

    //BPM OEM hook data format, BRIL_HOOK_SET_BPM_MODE = 0x3;
    private final static byte[] RAW_HOOK_OEM_CMD_BPM_DISABLE = {'B','R','C','M', 3, 0};    
    private final static byte[] RAW_HOOK_OEM_CMD_BPM_ENABLE = {'B','R','C','M', 3, 1};

    private Integer mSIM1NetworkType = null;
    private Integer mSIM2NetworkType = null;

    private static int mDataPreferSIMID = SimCardID.ID_ZERO.toInt();
    private static final int UNLOCK_TIME = 32000;  // ms
    private Timer mUiUnlockTimer = null;
    /**
     * The Data Connection status. One of the following:<p>
     * <ul>
     * <li>IDLE = Initialize value</li>
     * <li>CONNECTING = Currently setting up data connection.</li>
     * <li>CONNECTED = IP traffic should be available.</li>
     * <li>DISCONNECTED = IP not available.</li>
     * </ul>
     */
     
    enum DataConnectionState {
        IDLE, WAIT4RATSWITCHED, CONNECTING, CONNECTED, DISCONNECTED;
    };

    private static DataConnectionState mDataConnectionStatus = DataConnectionState.IDLE;

    /**
     * The RAT switch status. One of the following:<p>
     * <ul>
     * <li>RAT_SWITCH_IDLE = No RAT switch operation</li>
     * <li>SIM1_RAT_SWITCHING = sim1 switch RAT from 2G to 3G or vice versa</li>
     * <li>SIM2_RAT_SWITCHING = sim2 switch RAT from 2G to 3G or vice versa</li>
     * <li>BOTH_SIM_RAT_SWITCHING = both sims switch RAT from 2G to 3G or vice versa</li>
     * </ul>
     */
     
    enum RATSwithStatus {
        RAT_SWITCH_IDLE, SIM1_RAT_SWITCHING, SIM2_RAT_SWITCHING, BOTH_SIM_RAT_SWITCHING;
    };
    private static RATSwithStatus mRatSwitchState = RATSwithStatus.RAT_SWITCH_IDLE;

    private boolean mDual3GEnable = false;

    private boolean mSIM1Available = true;
    private boolean mSIM2Available = true;

    /**
     * The Service status. One of the following:<p>
     * <ul>
     * <li>IDLE = No RAT switch operation</li>
     * <li>IN_SERVICE = Normal operation condition, the phone is registered with an operator either in home network or in roaming.</li>
     * <li>OUT_OF_SERVICE = Phone is not registered with any operator, the phone can be currently searching a new operator to register to,
     *  or not searching to registration at all, or registration is denied, or radio signal is not available.</li>
     * <li>EMERGENCY_ONLY = The phone is registered and locked.  Only emergency numbers are allowed.</li>
     * <li>POWER_OFF = Radio of telephony is explicitly powered off.</li>
     * </ul>
     */

    enum ServiceStatus {
        IDLE, IN_SERVICE, IGNORE_FIRST_OUT_OF_SERVICE , OUT_OF_SERVICE, EMERGENCY_ONLY, POWER_OFF;
    };
	
    private ServiceStatus mSIM1ServiceStatus = ServiceStatus.IN_SERVICE;
    private ServiceStatus mSIM2ServiceStatus = ServiceStatus.IN_SERVICE;

    private final class UIUnlockTimerTask extends TimerTask {
        public void run() {
            if (DBG) log("Lock time out, broadcast intent, mDataPreferSIMID:" + mDataPreferSIMID);
		  Intent intent = new Intent(ConnectivityManager.ACTION_DATA_RECONNECTION_FAIL);
		  intent.putExtra(ConnectivityManager.EXTRA_DATA_CONNECTION_SIMID, mDataPreferSIMID);
		  mPhone.getContext().sendBroadcast(intent);
        }
    }
    /**
     * The UI Un Lock Reason. One of the following:<p>
     * <ul>
     * <li>NORMAL = CONNECTED or IN_SERVICE</li>
     * <li>DATA_CONNECTION_FAIL = DISCONNECTED.</li>
     * <li>NO_SERVICE = OUT_OF_SERVICE / EMERGENCY_ONLY / POWER_OFF.</li>
     * </ul>
     */

    enum UIUnLockReason {
        NORMAL, DATA_PERFER_SWITCH_FAIL ,DATA_CONNECTION_FAIL, NO_SERVICE;
    };



    private void LockPreferenceUI () {
    	if (!PhoneUtils.isDualMode) return;
        if (mButtonDataEnabled.isChecked()) {
            if (null != mDefaultSimForDataConnection) {
                mDefaultSimForDataConnection.setEnabled(false);
                mDefaultSimForDataConnection.setSummary(R.string.default_sim_changed_summary);
            }
        }
        if (null != mDefaultSimFor3GNetwork) {
            mDefaultSimFor3GNetwork.setEnabled(false);
            mDefaultSimFor3GNetwork.setSummary(R.string.default_sim_for_3g_network_changed_summary);
        }

        mButtonDataEnabled.setEnabled(false);

        if (mUiUnlockTimer == null) {
            mUiUnlockTimer  = new Timer();
            if (mUiUnlockTimer != null) {
                if (DBG) log("set unlock timer");
                mUiUnlockTimer.schedule(new UIUnlockTimerTask(), UNLOCK_TIME);
            }
        }
    }

    private void UnLockPreferenceUI (UIUnLockReason UnLockReason){
    	if (!PhoneUtils.isDualMode) return;
        if (mUiUnlockTimer != null) {
            if (DBG) log("cancel timer in UnLockPreferenceUI");
            mUiUnlockTimer.cancel();
            mUiUnlockTimer = null;
        }

        String status;
        switch (UnLockReason) {
            case NORMAL:
                if (DBG) log("UnLockPreferenceUI NORMAL");
                if (mButtonDataEnabled.isChecked()) {
                    if (null != mDefaultSimForDataConnection) {
                        mDefaultSimForDataConnection.setEnabled(true);
                        mDefaultSimForDataConnection.setSummary(R.string.default_sim_data_connection_summary);
                    }
                }
                if (null != mDefaultSimFor3GNetwork) {
                    mDefaultSimFor3GNetwork.setEnabled(true);
                    mDefaultSimFor3GNetwork.setSummary(getCurrentRatSummary());
                }
                break;

            case DATA_PERFER_SWITCH_FAIL:
                if (DBG) log("UnLockPreferenceUI DATA_PERFER_SWITCH_FAIL");
                status = getResources().getString(R.string.default_data_network_switch_error);
                PhoneGlobals.getInstance().notificationMgr.postTransientNotification(0, status);
                if (mButtonDataEnabled.isChecked()) {
                    if (null != mDefaultSimForDataConnection) {
                        mDefaultSimForDataConnection.setEnabled(true);
                        mDefaultSimForDataConnection.setSummary(R.string.default_sim_data_connection_summary);
                    }
                }
                if (null != mDefaultSimFor3GNetwork) {
                    mDefaultSimFor3GNetwork.setEnabled(true);
                    mDefaultSimFor3GNetwork.setSummary(getCurrentRatSummary());
                }
                break;

            case DATA_CONNECTION_FAIL:
                if (DBG) log("UnLockPreferenceUI DATA_CONNECTION_FAIL");
                // pop up a notification for connection fail
                status = getResources().getString(R.string.default_data_network_switch_error);
                PhoneGlobals.getInstance().notificationMgr.postTransientNotification(0, status);
                if (mButtonDataEnabled.isChecked()) {
                    if (null != mDefaultSimForDataConnection) {
                        mDefaultSimForDataConnection.setEnabled(true);
                        mDefaultSimForDataConnection.setSummary(R.string.default_sim_data_connection_summary);
                    }
                }
                if (null != mDefaultSimFor3GNetwork) {
                    mDefaultSimFor3GNetwork.setEnabled(true);
                    mDefaultSimFor3GNetwork.setSummary(getCurrentRatSummary());
                }
                break;

            case NO_SERVICE:
                if (DBG) log("UnLockPreferenceUI NO_SERVICE");
                // pop up a notification for connection fail
                status = getResources().getString(R.string.default_data_network_switch_error);
                PhoneGlobals.getInstance().notificationMgr.postTransientNotification(0, status);
                if (mButtonDataEnabled.isChecked()) {
                    if (null != mDefaultSimForDataConnection) {
                        mDefaultSimForDataConnection.setEnabled(true);
                        mDefaultSimForDataConnection.setSummary(R.string.default_sim_data_connection_summary);
                    }
                }
                if (null != mDefaultSimFor3GNetwork) {
                    mDefaultSimFor3GNetwork.setEnabled(true);
                    mDefaultSimFor3GNetwork.setSummary(getCurrentRatSummary());
                }
                break;
        }
        mButtonDataEnabled.setEnabled(true);
    }
	
    // Broadcast receiver for various intent broadcasts (see onCreate())
    private final BroadcastReceiver mReceiver = new NetworkSettingsBroadcastReceiver();

    /**
     * Receiver for misc intent broadcasts the Settings app cares about.
     */
    private class NetworkSettingsBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ConnectivityManager.ACTION_DATA_CONNECTION_CHANGE_FAIL)) {
                int simid = intent.getIntExtra(ConnectivityManager.EXTRA_DATA_CONNECTION_SIMID, SimCardID.ID_ZERO.toInt());
                if(mDataPreferSIMID == simid){
                    if (DBG) log("Data perfer sim switch fail on SIM: " + mDataPreferSIMID);
                    SystemProperties.set(TelephonyProperties.PROPERTY_DATA_PREFER_SIM_ID, String.valueOf((simid == SimCardID.ID_ZERO.toInt() ? SimCardID.ID_ONE.toInt() : SimCardID.ID_ZERO.toInt())));
                    if (null != mDefaultSimForDataConnection) mDefaultSimForDataConnection.setValue(String.valueOf((simid == SimCardID.ID_ZERO.toInt() ? SimCardID.ID_ONE.toInt() : SimCardID.ID_ZERO.toInt())));
                    mDataConnectionStatus = DataConnectionState.DISCONNECTED; 
                    UnLockPreferenceUI(UIUnLockReason.DATA_PERFER_SWITCH_FAIL);
                }
     		} else if (action.equals(ConnectivityManager.ACTION_DATA_RECONNECTION_FAIL)) {
                int simid = intent.getIntExtra(ConnectivityManager.EXTRA_DATA_CONNECTION_SIMID, SimCardID.ID_ZERO.toInt());
                if(mDataPreferSIMID == simid) {
                    if (DBG) log("Data ReConnection fail on SIM: " + mDataPreferSIMID);
                    // connection fail, restore property value
                    mDataConnectionStatus = DataConnectionState.DISCONNECTED; 
                    UnLockPreferenceUI(UIUnLockReason.DATA_CONNECTION_FAIL);
                }
     		}else if (action.equals(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED)) {
			    if(mDataPreferSIMID == ((SimCardID)(intent.getExtra("simId", SimCardID.ID_ZERO))).toInt()   ) { 	
			        if("CONNECTED".equals(intent.getStringExtra(PhoneConstants.STATE_KEY)))			        {
			            if (DBG) log("Data Connected on SIM: " + mDataPreferSIMID);
			            mDataConnectionStatus = DataConnectionState.CONNECTED; 
			            UnLockPreferenceUI(UIUnLockReason.NORMAL);
			        }
			    }	
            }  else if (action.equals(TelephonyIntents.ACTION_DATA_CONNECTION_FAILED)) {
                int simid = intent.getIntExtra("simId", SimCardID.ID_ZERO.toInt());
			    if(mDataPreferSIMID == simid)
			    { 		  
			        if (DBG) log("Data Connection fail on SIM: " + mDataPreferSIMID);
			        // connection fail, restore property value
			        SystemProperties.set(TelephonyProperties.PROPERTY_DATA_PREFER_SIM_ID, String.valueOf(simid));
			        if (null != mDefaultSimForDataConnection) mDefaultSimForDataConnection.setValue(String.valueOf(simid));
			        mDataConnectionStatus = DataConnectionState.DISCONNECTED;
			        UnLockPreferenceUI(UIUnLockReason.DATA_CONNECTION_FAIL);
			    }						
            }   else if (action.equals(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED)) {
                ServiceState ss = ServiceState.newFromBundle(intent.getExtras());
                if (ss != null) {
                    int state = ss.getState();
                    switch (state) {
                        case ServiceState.STATE_IN_SERVICE:	
                            if(((SimCardID)(intent.getExtra("simId", SimCardID.ID_ZERO))).toInt() == SimCardID.ID_ZERO.toInt()){
                                mSIM1ServiceStatus = ServiceStatus.IN_SERVICE;
					if (mDataConnectionStatus == DataConnectionState.WAIT4RATSWITCHED){
						// Data disabled
						if(mRatSwitchState == RATSwithStatus.SIM1_RAT_SWITCHING){
						    if (DBG) log("SIM1 IN_SERVICE Data disabled SIM1_RAT_SWITCHING");
						    mDataConnectionStatus = DataConnectionState.IDLE;
						    mRatSwitchState = RATSwithStatus.RAT_SWITCH_IDLE;
						    UnLockPreferenceUI(UIUnLockReason.NORMAL);
						}else if (mRatSwitchState == RATSwithStatus.BOTH_SIM_RAT_SWITCHING &&
						    mSIM2ServiceStatus != ServiceStatus.IDLE)	{
						    if (DBG) log("SIM1 IN_SERVICE Data disabled BOTH_SIM_RAT_SWITCHING");
						    mDataConnectionStatus = DataConnectionState.IDLE;
						    mRatSwitchState = RATSwithStatus.RAT_SWITCH_IDLE;
						    UnLockPreferenceUI(UIUnLockReason.NORMAL);
						}
					} else  {
						// Data Enabled
						if(mDataPreferSIMID == SimCardID.ID_ONE.toInt() && mRatSwitchState == RATSwithStatus.SIM1_RAT_SWITCHING){
						    if (DBG) log(" SIM1 IN_SERVICE Data Enabled SIM1_RAT_SWITCHING");
						    mDataConnectionStatus = DataConnectionState.IDLE;
						    mRatSwitchState = RATSwithStatus.RAT_SWITCH_IDLE;
						    UnLockPreferenceUI(UIUnLockReason.NORMAL);
						}
					}
                            }		
                            if(((SimCardID)(intent.getExtra("simId", SimCardID.ID_ONE))).toInt() == SimCardID.ID_ONE.toInt()){
                                mSIM2ServiceStatus = ServiceStatus.IN_SERVICE;
					if (mDataConnectionStatus == DataConnectionState.WAIT4RATSWITCHED){
						// Data disabled
						if(mRatSwitchState == RATSwithStatus.SIM2_RAT_SWITCHING){
						    if (DBG) log("SIM2 IN_SERVICE Data disabled SIM2_RAT_SWITCHING");
						    mDataConnectionStatus = DataConnectionState.IDLE;
						    mRatSwitchState = RATSwithStatus.RAT_SWITCH_IDLE;
						    UnLockPreferenceUI(UIUnLockReason.NORMAL);
						}else if (mRatSwitchState == RATSwithStatus.BOTH_SIM_RAT_SWITCHING &&
						    mSIM1ServiceStatus != ServiceStatus.IDLE)	{
						    if (DBG) log("SIM2 IN_SERVICE Data disabled BOTH_SIM_RAT_SWITCHING");
						    mDataConnectionStatus = DataConnectionState.IDLE;
						    mRatSwitchState = RATSwithStatus.RAT_SWITCH_IDLE;
						    UnLockPreferenceUI(UIUnLockReason.NORMAL);
						}
					} else {
						// Data Enabled
						if(mDataPreferSIMID == SimCardID.ID_ZERO.toInt() && mRatSwitchState == RATSwithStatus.SIM2_RAT_SWITCHING){
						    if (DBG) log("SIM2 IN_SERVICE Data Enabled SIM2_RAT_SWITCHING");
						    mDataConnectionStatus = DataConnectionState.IDLE;
						    mRatSwitchState = RATSwithStatus.RAT_SWITCH_IDLE;
						    UnLockPreferenceUI(UIUnLockReason.NORMAL);
						}
					}
                            }
                        break;					
                        case ServiceState.STATE_OUT_OF_SERVICE:
                            if(((SimCardID)(intent.getExtra("simId", SimCardID.ID_ZERO))).toInt() == SimCardID.ID_ZERO.toInt()){
                                if(mSIM1ServiceStatus == ServiceStatus.IGNORE_FIRST_OUT_OF_SERVICE)		
                                {
					    if (DBG) log("SIM1 IGNORE_FIRST_OUT_OF_SERVICE");
                                    mSIM1ServiceStatus = ServiceStatus.IDLE;
                                    break;								
                                } else{
					    if (DBG) log("SIM1 OUT_OF_SERVICEE");
                                    mSIM1ServiceStatus = ServiceStatus.OUT_OF_SERVICE;
                                }
					if (mDataConnectionStatus == DataConnectionState.WAIT4RATSWITCHED){
						// Data disabled
						if(mRatSwitchState == RATSwithStatus.SIM1_RAT_SWITCHING){
						    if (DBG) log("SIM1 OUT_OF_SERVICE Data disabled SIM1_RAT_SWITCHING");
						    mDataConnectionStatus = DataConnectionState.IDLE;
						    mRatSwitchState = RATSwithStatus.RAT_SWITCH_IDLE;
						    UnLockPreferenceUI(UIUnLockReason.NO_SERVICE);
						}else if (mRatSwitchState == RATSwithStatus.BOTH_SIM_RAT_SWITCHING &&
						    mSIM2ServiceStatus != ServiceStatus.IDLE)	{
						    if (DBG) log("SIM1 OUT_OF_SERVICE Data disabled BOTH_SIM_RAT_SWITCHING");
						    mDataConnectionStatus = DataConnectionState.IDLE;
						    mRatSwitchState = RATSwithStatus.RAT_SWITCH_IDLE;
						    UnLockPreferenceUI(UIUnLockReason.NO_SERVICE);
						}
					} else  {
						// Data Enabled
						if(mDataPreferSIMID == SimCardID.ID_ONE.toInt() && mRatSwitchState == RATSwithStatus.SIM1_RAT_SWITCHING){
						    if (DBG) log("SIM1 STATE_OUT_OF_SERVICE Data Enabled SIM1_RAT_SWITCHING");
						    mDataConnectionStatus = DataConnectionState.IDLE;
						    mRatSwitchState = RATSwithStatus.RAT_SWITCH_IDLE;
						    UnLockPreferenceUI(UIUnLockReason.NO_SERVICE);
						}
					}
                            }		
                            if(((SimCardID)(intent.getExtra("simId", SimCardID.ID_ONE))).toInt() == SimCardID.ID_ONE.toInt()){
                                if(mSIM2ServiceStatus == ServiceStatus.IGNORE_FIRST_OUT_OF_SERVICE)		
                                {
                                    if (DBG) log("SIM2 IGNORE_FIRST_OUT_OF_SERVICE");
                                    mSIM2ServiceStatus = ServiceStatus.IDLE;
                                    break;								
                                } else{
					    if (DBG) log("SIM2 OUT_OF_SERVICEE");
                                    mSIM2ServiceStatus = ServiceStatus.OUT_OF_SERVICE;
                                }
					if (mDataConnectionStatus == DataConnectionState.WAIT4RATSWITCHED){
						// Data disabled
						if(mRatSwitchState == RATSwithStatus.SIM2_RAT_SWITCHING){
                                          if (DBG) log("SIM2 OUT_OF_SERVICE Data disabled SIM2_RAT_SWITCHING");
						    mDataConnectionStatus = DataConnectionState.IDLE;
						    mRatSwitchState = RATSwithStatus.RAT_SWITCH_IDLE;
						    UnLockPreferenceUI(UIUnLockReason.NO_SERVICE);
						}else if (mRatSwitchState == RATSwithStatus.BOTH_SIM_RAT_SWITCHING &&
						    mSIM1ServiceStatus != ServiceStatus.IDLE)	{
                                          if (DBG) log("SIM2 OUT_OF_SERVICE Data disabled BOTH_SIM_RAT_SWITCHING");
						    mDataConnectionStatus = DataConnectionState.IDLE;
						    mRatSwitchState = RATSwithStatus.RAT_SWITCH_IDLE;
						    UnLockPreferenceUI(UIUnLockReason.NO_SERVICE);
						}
					} else {
						// Data Enabled
						if(mDataPreferSIMID == SimCardID.ID_ZERO.toInt() && mRatSwitchState == RATSwithStatus.SIM2_RAT_SWITCHING){
                                          if (DBG) log("SIM2 OUT_OF_SERVICE Data Enabled SIM2_RAT_SWITCHING");
						    mDataConnectionStatus = DataConnectionState.IDLE;
						    mRatSwitchState = RATSwithStatus.RAT_SWITCH_IDLE;
						    UnLockPreferenceUI(UIUnLockReason.NO_SERVICE);
						}
					}
                            }
                        break;					
                        case ServiceState.STATE_EMERGENCY_ONLY:
                            if(((SimCardID)(intent.getExtra("simId", SimCardID.ID_ZERO))).toInt() == SimCardID.ID_ZERO.toInt()){
                                mSIM1ServiceStatus = ServiceStatus.EMERGENCY_ONLY;
					if (mDataConnectionStatus == DataConnectionState.WAIT4RATSWITCHED){
						// Data disabled
						if(mRatSwitchState == RATSwithStatus.SIM1_RAT_SWITCHING){
                                          if (DBG) log("SIM1 EMERGENCY_ONLY Data disabled SIM1_RAT_SWITCHING");
						    mDataConnectionStatus = DataConnectionState.IDLE;
						    mRatSwitchState = RATSwithStatus.RAT_SWITCH_IDLE;
						    UnLockPreferenceUI(UIUnLockReason.NO_SERVICE);
						}else if (mRatSwitchState == RATSwithStatus.BOTH_SIM_RAT_SWITCHING &&
						    mSIM2ServiceStatus != ServiceStatus.IDLE)	{
                                          if (DBG) log("SIM1 EMERGENCY_ONLY Data disabled BOTH_SIM_RAT_SWITCHING");
						    mDataConnectionStatus = DataConnectionState.IDLE;
						    mRatSwitchState = RATSwithStatus.RAT_SWITCH_IDLE;
						    UnLockPreferenceUI(UIUnLockReason.NO_SERVICE);
						}
					} else {
						// Data Enabled
						if(mDataPreferSIMID == SimCardID.ID_ONE.toInt() && mRatSwitchState == RATSwithStatus.SIM1_RAT_SWITCHING){
                                          if (DBG) log("SIM1 EMERGENCY_ONLY Data Enabled SIM1_RAT_SWITCHING");
						    mDataConnectionStatus = DataConnectionState.IDLE;
						    mRatSwitchState = RATSwithStatus.RAT_SWITCH_IDLE;
						    UnLockPreferenceUI(UIUnLockReason.NO_SERVICE);
						}
					}
                            }		
                            if(((SimCardID)(intent.getExtra("simId", SimCardID.ID_ONE))).toInt() == SimCardID.ID_ONE.toInt()){
                                mSIM2ServiceStatus = ServiceStatus.EMERGENCY_ONLY;
					if (mDataConnectionStatus == DataConnectionState.WAIT4RATSWITCHED){
						// Data disabled
						if(mRatSwitchState == RATSwithStatus.SIM2_RAT_SWITCHING){
                                          if (DBG) log("SIM2 EMERGENCY_ONLY Data disabled SIM2_RAT_SWITCHING");
						    mDataConnectionStatus = DataConnectionState.IDLE;
						    mRatSwitchState = RATSwithStatus.RAT_SWITCH_IDLE;
						    UnLockPreferenceUI(UIUnLockReason.NO_SERVICE);
						}else if (mRatSwitchState == RATSwithStatus.BOTH_SIM_RAT_SWITCHING &&
						    mSIM1ServiceStatus != ServiceStatus.IDLE)	{
                                          if (DBG) log("SIM2 EMERGENCY_ONLY Data disabled BOTH_SIM_RAT_SWITCHING");
						    mDataConnectionStatus = DataConnectionState.IDLE;
						    mRatSwitchState = RATSwithStatus.RAT_SWITCH_IDLE;
						    UnLockPreferenceUI(UIUnLockReason.NO_SERVICE);
						}
					} else {
						// Data Enabled
						if(mDataPreferSIMID == SimCardID.ID_ZERO.toInt() && mRatSwitchState == RATSwithStatus.SIM2_RAT_SWITCHING){
                                          if (DBG) log("SIM2 EMERGENCY_ONLY Data Enabled SIM2_RAT_SWITCHING");
						    mDataConnectionStatus = DataConnectionState.IDLE;
						    mRatSwitchState = RATSwithStatus.RAT_SWITCH_IDLE;
						    UnLockPreferenceUI(UIUnLockReason.NO_SERVICE);
						}
					}
                            }
                        break;					
                        case ServiceState.STATE_POWER_OFF:
                            if(((SimCardID)(intent.getExtra("simId", SimCardID.ID_ZERO))).toInt() == SimCardID.ID_ZERO.toInt()){
                                mSIM1ServiceStatus = ServiceStatus.POWER_OFF;
					if (mDataConnectionStatus == DataConnectionState.WAIT4RATSWITCHED){
						// Data disabled
						if(mRatSwitchState == RATSwithStatus.SIM1_RAT_SWITCHING){
                                          if (DBG) log("SIM1 POWER_OFF Data disabled SIM1_RAT_SWITCHING");
						    mDataConnectionStatus = DataConnectionState.IDLE;
						    mRatSwitchState = RATSwithStatus.RAT_SWITCH_IDLE;
						    UnLockPreferenceUI(UIUnLockReason.NO_SERVICE);
						}else if (mRatSwitchState == RATSwithStatus.BOTH_SIM_RAT_SWITCHING &&
						    mSIM2ServiceStatus != ServiceStatus.IDLE)	{
                                          if (DBG) log("SIM1 POWER_OFF Data disabled BOTH_SIM_RAT_SWITCHING");
						    mDataConnectionStatus = DataConnectionState.IDLE;
						    mRatSwitchState = RATSwithStatus.RAT_SWITCH_IDLE;
						    UnLockPreferenceUI(UIUnLockReason.NO_SERVICE);
						}
					} else {
						// Data Enabled
						if(mDataPreferSIMID == SimCardID.ID_ONE.toInt() && mRatSwitchState == RATSwithStatus.SIM1_RAT_SWITCHING){
                                          if (DBG) log("SIM1 POWER_OFF Data Enabled SIM1_RAT_SWITCHING");
						    mDataConnectionStatus = DataConnectionState.IDLE;
						    mRatSwitchState = RATSwithStatus.RAT_SWITCH_IDLE;
						    UnLockPreferenceUI(UIUnLockReason.NO_SERVICE);
						}
					}
                            }		
                            if(((SimCardID)(intent.getExtra("simId", SimCardID.ID_ONE))).toInt() == SimCardID.ID_ONE.toInt()){
                                mSIM2ServiceStatus = ServiceStatus.POWER_OFF;
					if (mDataConnectionStatus == DataConnectionState.WAIT4RATSWITCHED){
						// Data disabled
						if(mRatSwitchState == RATSwithStatus.SIM2_RAT_SWITCHING){
                                          if (DBG) log("SIM2 POWER_OFF Data disabled SIM2_RAT_SWITCHING");
						    mDataConnectionStatus = DataConnectionState.IDLE;
						    mRatSwitchState = RATSwithStatus.RAT_SWITCH_IDLE;
						    UnLockPreferenceUI(UIUnLockReason.NO_SERVICE);
						}else if (mRatSwitchState == RATSwithStatus.BOTH_SIM_RAT_SWITCHING &&
						    mSIM1ServiceStatus != ServiceStatus.IDLE)	{
                                          if (DBG) log("SIM2 POWER_OFF Data disabled BOTH_SIM_RAT_SWITCHING");
						    mDataConnectionStatus = DataConnectionState.IDLE;
						    mRatSwitchState = RATSwithStatus.RAT_SWITCH_IDLE;
						    UnLockPreferenceUI(UIUnLockReason.NO_SERVICE);
						}
					} else {
						// Data Enabled
						if(mDataPreferSIMID == SimCardID.ID_ZERO.toInt() && mRatSwitchState == RATSwithStatus.SIM2_RAT_SWITCHING){
                                          if (DBG) log("SIM2 POWER_OFF Data Enabled SIM2_RAT_SWITCHING");
						    mDataConnectionStatus = DataConnectionState.IDLE;
						    mRatSwitchState = RATSwithStatus.RAT_SWITCH_IDLE;
						    UnLockPreferenceUI(UIUnLockReason.NO_SERVICE);
						}
					}
                            }
                        break;
                    }
                }		
            } 
        }
    }


    //This is a method implemented for DialogInterface.OnClickListener.
    //  Used to dismiss the dialogs when they come up.
    public void onClick(DialogInterface dialog, int which) {
		if (dialog == mRoamConfirm1){
		    if (which == DialogInterface.BUTTON_POSITIVE) {
		        mPhone.setDataRoamingEnabled(true);
		        mOkClicked = true;
		    } else {
		        // Reset the toggle
		        mButtonDataRoam.setChecked(false);
		    }
		} else if (dialog == mRoamConfirm2){
		    if (which == DialogInterface.BUTTON_POSITIVE) {
		        mPhone2.setDataRoamingEnabled(true);
		        mOkClicked2 = true;
		    } else {
		        // Reset the toggle
		        mButtonDataRoam2.setChecked(false);
		    }
		}
    }

    public void onDismiss(DialogInterface dialog) {
        // Assuming that onClick gets called first
		if (dialog == mRoamConfirm1) {
		    if (!mOkClicked) {
		        mButtonDataRoam.setChecked(false);
		    }
		} else if (dialog == mRoamConfirm2) {
		    if (!mOkClicked2) {
		        mButtonDataRoam2.setChecked(false);
		    }
		}
    }

    /**
     * Invoked on each preference click in this hierarchy, overrides
     * PreferenceActivity's implementation.  Used to make sure we track the
     * preference click events.
     */
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        /** TODO: Refactor and get rid of the if's using subclasses */
        if (mGsmUmtsOptions != null &&
                mGsmUmtsOptions.preferenceTreeClick(preference) == true) {
            return true;
        } else if (mGsmUmtsOptions2 != null &&
                mGsmUmtsOptions2.preferenceTreeClick(preference) == true) {
            return true;
        } else if (mCdmaOptions != null &&
                   mCdmaOptions.preferenceTreeClick(preference) == true) {
            if (Boolean.parseBoolean(
                    SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE))) {

                mClickedPreference = preference;

                // In ECM mode launch ECM app dialog
                startActivityForResult(
                    new Intent(TelephonyIntents.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS, null),
                    REQUEST_CODE_EXIT_ECM);
            }
            return true;
        } else if (preference == mButtonPreferredNetworkMode) {
            //displays the value taken from the Settings.System
            int settingsNetworkMode = android.provider.Settings.Global.getInt(mPhone.getContext().
                    getContentResolver(), android.provider.Settings.Global.PREFERRED_NETWORK_MODE,
                    preferredNetworkMode);
            mButtonPreferredNetworkMode.setValue(Integer.toString(settingsNetworkMode));
            return true;
        } else if (preference == mButtonDataRoam) {
            if (DBG) log("onPreferenceTreeClick: preference == mButtonDataRoam.");

            //normally called on the toggle click
            if (mButtonDataRoam.isChecked()) {
                // First confirm with a warning dialog about charges
                mOkClicked = false;
                mRoamConfirm1 = new AlertDialog.Builder(this).setMessage(
                        (PhoneUtils.isDualMode)?getResources().getString(R.string.roaming_warning_sim_1):getResources().getString(R.string.roaming_warning))
                        .setTitle(android.R.string.dialog_alert_title)
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .setPositiveButton(android.R.string.yes, this)
                        .setNegativeButton(android.R.string.no, this).create();
                mRoamConfirm1.setOnDismissListener(this);
                mRoamConfirm1.show();
            } else {
                mPhone.setDataRoamingEnabled(false);
            }
            return true;
        } else if (preference == mButtonDataRoam2) {
            if (DBG) log("onPreferenceTreeClick: preference == mButtonDataRoam2.");

            //normally called on the toggle click
            if (mButtonDataRoam2.isChecked()) {
                // First confirm with a warning dialog about charges
                mOkClicked = false;
                mRoamConfirm2 = new AlertDialog.Builder(this).setMessage(
                        getResources().getString(R.string.roaming_warning_sim_2))
                        .setTitle(android.R.string.dialog_alert_title)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, this)
                        .setNegativeButton(android.R.string.no, this).create();
                mRoamConfirm2.setOnDismissListener(this);
                mRoamConfirm2.show();
            } else {
                mPhone2.setDataRoamingEnabled(false);
            }
            return true;
        } else if (preference == mButtonDataEnabled) {
            if (DBG) log("onPreferenceTreeClick: preference == mButtonDataEnabled.");
            ConnectivityManager cm =
                    (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

            cm.setMobileDataEnabled(mButtonDataEnabled.isChecked());
            if (mButtonDataEnabled.isChecked() && (mSIM1Available || mSIM2Available)) {
                //enable Data connection UI
                mDataPreferSIMID =  SystemProperties.getInt(TelephonyProperties.PROPERTY_DATA_PREFER_SIM_ID, SimCardID.ID_ZERO.toInt());
                if (DBG) log("mDataPreferSIMID:"+ mDataPreferSIMID);
                mDataConnectionStatus = DataConnectionState.CONNECTING;
                LockPreferenceUI();
            } else {
                //disable Data connection UI
                if (null != mDefaultSimForDataConnection) mDefaultSimForDataConnection.setEnabled(false);
            }
            return true;
        } else if (preference == mButtonBPMEnabled) {
            if (DBG) Log.d(LOG_TAG, "Receive BPM_Enable Buttom Event");
            
            if (mButtonBPMEnabled.isChecked()) {
                if (DBG) Log.d(LOG_TAG, "Receive BPM_Enable CheckEvent");
                mPhone.invokeOemRilRequestRaw(RAW_HOOK_OEM_CMD_BPM_ENABLE, mHandler.obtainMessage(MyHandler.MESSAGE_BPM_ENABLE));                
            }else{
                if (DBG) Log.d(LOG_TAG, "Receive BPM_Enable Un-CheckEvent");
                mPhone.invokeOemRilRequestRaw(RAW_HOOK_OEM_CMD_BPM_DISABLE, mHandler.obtainMessage(MyHandler.MESSAGE_BPM_DISABLE)); 
            }
            return true;
        } else if (preference == mDefaultSimForDataConnection) {
        	//Do nothing, we handle state in onPreferenceChange()
        	return true;
        } else if (preference == mDefaultSimFor3GNetwork) {
            //Do nothing. Handle this in onPreferenceChange()
            return true;
        } else if (preference == mLteDataServicePref) {
            String tmpl = android.provider.Settings.Global.getString(getContentResolver(),
                        android.provider.Settings.Global.SETUP_PREPAID_DATA_SERVICE_URL);
            if (!TextUtils.isEmpty(tmpl)) {
                TelephonyManager tm = (TelephonyManager) getSystemService(
                        Context.TELEPHONY_SERVICE);
                String imsi = tm.getSubscriberId();
                if (imsi == null) {
                    imsi = "";
                }
                final String url = TextUtils.isEmpty(tmpl) ? null
                        : TextUtils.expandTemplate(tmpl, imsi).toString();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            } else {
                android.util.Log.e(LOG_TAG, "Missing SETUP_PREPAID_DATA_SERVICE_URL");
            }
            return true;
        }  else if (preference == mButtonEnabledNetworks) {
            int settingsNetworkMode = android.provider.Settings.Global.getInt(mPhone.getContext().
                    getContentResolver(), android.provider.Settings.Global.PREFERRED_NETWORK_MODE,
                    preferredNetworkMode);
            mButtonEnabledNetworks.setValue(Integer.toString(settingsNetworkMode));
            return true;
        } else {
            // if the button is anything but the simple toggle preference,
            // we'll need to disable all preferences to reject all click
            // events until the sub-activity's UI comes up.
            preferenceScreen.setEnabled(false);
            // Let the intents be launched by the Preference manager
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.network_setting);

        mPhone = PhoneGlobals.getPhone(SimCardID.ID_ZERO);
        if (PhoneUtils.isDualMode) {
        	mPhone2 = PhoneGlobals.getPhone(SimCardID.ID_ONE);
        } else {
        	mPhone2 = null;
        }
        mHandler = new MyHandler();

        try {
            Context con = createPackageContext("com.android.systemui", 0);
            int id = con.getResources().getIdentifier("config_show4GForLTE",
                    "bool", "com.android.systemui");
            mShow4GForLTE = con.getResources().getBoolean(id);
        } catch (NameNotFoundException e) {
            loge("NameNotFoundException for show4GFotLTE");
            mShow4GForLTE = false;
        }

        //get UI object references
        PreferenceScreen prefSet = getPreferenceScreen();

        mButtonDataEnabled = (CheckBoxPreference) prefSet.findPreference(BUTTON_DATA_ENABLED_KEY);
        mButtonBPMEnabled = (CheckBoxPreference) prefSet.findPreference(BUTTON_BPM_ENABLED_KEY);
        mButtonDataRoam = (CheckBoxPreference) prefSet.findPreference(BUTTON_ROAMING_KEY);
        mButtonDataRoam2 = (CheckBoxPreference) prefSet.findPreference(BUTTON_ROAMING_2_KEY);
        mButtonPreferredNetworkMode = (ListPreference) prefSet.findPreference(
                BUTTON_PREFERED_NETWORK_MODE);
        mButtonEnabledNetworks = (ListPreference) prefSet.findPreference(
                BUTTON_ENABLED_NETWORKS_KEY);

        mLteDataServicePref = prefSet.findPreference(BUTTON_CDMA_LTE_DATA_SERVICE_KEY);

        boolean isLteOnCdma = mPhone.getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE;

        if (!PhoneUtils.isDualMode) {
            if (null != mButtonBPMEnabled) {
                prefSet.removePreference(mButtonBPMEnabled);
                mButtonBPMEnabled = null;
            }

            mButtonDataRoam.setTitle(R.string.roaming);
            if (null != mButtonDataRoam2) {
                prefSet.removePreference(mButtonDataRoam2);
                mButtonDataRoam2 = null;
            }
        } else {
            int bpmsetting = SystemProperties.getInt(TelephonyProperties.PROPERTY_BPM_SETTING_ENABLE, 0);
            String strLogMsg = String.format("Query Properpty BPM Setting = %d", bpmsetting);
            if (DBG) Log.d(LOG_TAG, strLogMsg);        
            if (bpmsetting > 0){
                mButtonBPMEnabled.setChecked(true);
            }
        }

        mSIM1NetworkType = null;
        mSIM2NetworkType = null;

        if (getResources().getBoolean(R.bool.world_phone) == true) {
            prefSet.removePreference(mButtonEnabledNetworks);
            // set the listener for the mButtonPreferredNetworkMode list preference so we can issue
            // change Preferred Network Mode.
            mButtonPreferredNetworkMode.setOnPreferenceChangeListener(this);

            //Get the networkMode from Settings.System and displays it
            int settingsNetworkMode = android.provider.Settings.Global.getInt(mPhone.getContext().
                    getContentResolver(),android.provider.Settings.Global.PREFERRED_NETWORK_MODE,
                    preferredNetworkMode);
            mButtonPreferredNetworkMode.setValue(Integer.toString(settingsNetworkMode));
            mCdmaOptions = new CdmaOptions(this, prefSet, mPhone);
            mGsmUmtsOptions = new GsmUmtsOptions(this, prefSet);
        } else {
            prefSet.removePreference(mButtonPreferredNetworkMode);

            /*In dual sim mode, remove mButtonEnabledNetworks as well, as per previous
              platform, "2G onlY" etc are removed as dual sim has it's own way menu*/
            if (PhoneUtils.isDualMode) {
		if (DBG) Log.i(LOG_TAG, "dualmode, remove mButtonEnabledNetworks");
                prefSet.removePreference(mButtonEnabledNetworks);
            }

            int phoneType = mPhone.getPhoneType();
            if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                if (isLteOnCdma) {
                    mButtonEnabledNetworks.setEntries(
                            R.array.enabled_networks_cdma_choices);
                    mButtonEnabledNetworks.setEntryValues(
                            R.array.enabled_networks_cdma_values);
                }
                mCdmaOptions = new CdmaOptions(this, prefSet, mPhone);
            } else if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
                if (!getResources().getBoolean(R.bool.config_prefer_2g)
                        && !getResources().getBoolean(R.bool.config_enabled_lte)) {
                    mButtonEnabledNetworks.setEntries(
                            R.array.enabled_networks_except_gsm_lte_choices);
                    mButtonEnabledNetworks.setEntryValues(
                            R.array.enabled_networks_except_gsm_lte_values);
                } else if (!getResources().getBoolean(R.bool.config_prefer_2g)) {
                    int select = (mShow4GForLTE == true) ?
                        R.array.enabled_networks_except_gsm_4g_choices
                        : R.array.enabled_networks_except_gsm_choices;
                    mButtonEnabledNetworks.setEntries(select);
                    mButtonEnabledNetworks.setEntryValues(
                            R.array.enabled_networks_except_gsm_values);
                } else if (!getResources().getBoolean(R.bool.config_enabled_lte)) {
                    mButtonEnabledNetworks.setEntries(
                            R.array.enabled_networks_except_lte_choices);
                    mButtonEnabledNetworks.setEntryValues(
                            R.array.enabled_networks_except_lte_values);
                } else if (mIsGlobalCdma) {
                    mButtonEnabledNetworks.setEntries(
                            R.array.enabled_networks_cdma_choices);
                    mButtonEnabledNetworks.setEntryValues(
                            R.array.enabled_networks_cdma_values);
                } else {
                    int select = (mShow4GForLTE == true) ? R.array.enabled_networks_4g_choices
                        : R.array.enabled_networks_choices;
                    mButtonEnabledNetworks.setEntries(select);
                    mButtonEnabledNetworks.setEntryValues(
                            R.array.enabled_networks_values);
                }
                mGsmUmtsOptions = new GsmUmtsOptions(this, prefSet);
            } else {
                throw new IllegalStateException("Unexpected phone type: " + phoneType);
            }
            if (PhoneUtils.isDualMode) {
                int phoneType2 = mPhone2.getPhoneType();
                if (phoneType2 == PhoneConstants.PHONE_TYPE_CDMA) {
                  //Since we do not deal with cdma phone type for now, so just do nothing here 
                  //TODO: implement it if needed
                } else if (phoneType2 == PhoneConstants.PHONE_TYPE_GSM) {
                    mGsmUmtsOptions2 = new GsmUmtsOptions2(this, prefSet);
                } else {
                    throw new IllegalStateException("Unexpected phone type: " + phoneType);
                }
            }
            mButtonEnabledNetworks.setOnPreferenceChangeListener(this);
            int settingsNetworkMode = android.provider.Settings.Global.getInt(
                    mPhone.getContext().getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE,
                    preferredNetworkMode);
            if (DBG) log("settingsNetworkMode: " + settingsNetworkMode);
            mButtonEnabledNetworks.setValue(Integer.toString(settingsNetworkMode));
        }

        final boolean missingDataServiceUrl = TextUtils.isEmpty(
                android.provider.Settings.Global.getString(getContentResolver(),
                        android.provider.Settings.Global.SETUP_PREPAID_DATA_SERVICE_URL));
        if (!isLteOnCdma || missingDataServiceUrl) {
            prefSet.removePreference(mLteDataServicePref);
        } else {
            android.util.Log.d(LOG_TAG, "keep ltePref");
        }

        mDefaultSimForDataConnection = (ListPreference)prefSet.findPreference(BUTTON_DEFAULT_SIM_DATA_CONNECTION_KEY);
        if (!PhoneUtils.isDualMode) {
            if (null != mDefaultSimForDataConnection) {
                prefSet.removePreference(mDefaultSimForDataConnection);
                mDefaultSimForDataConnection = null;
            }
        } else {
            mDefaultSimForDataConnection.setOnPreferenceChangeListener(this);
            mDefaultSimForDataConnection.setValue(SystemProperties.get(TelephonyProperties.PROPERTY_DATA_PREFER_SIM_ID,
                    String.valueOf(SimCardID.ID_ZERO.toInt())));
        }

        mDefaultSimFor3GNetwork = (ListPreference) prefSet.findPreference(BUTTON_DEFAULT_SIM_FOR_3G_NETWORK_KEY);
        if (!PhoneUtils.isDualMode) {
            if (null != mDefaultSimFor3GNetwork) {
                prefSet.removePreference(mDefaultSimFor3GNetwork);
                mDefaultSimFor3GNetwork = null;
            }
        } else {
            mDefaultSimFor3GNetwork.setOnPreferenceChangeListener(this);
        }

        mDual3GEnable = SystemProperties.getBoolean(TelephonyProperties.PROPERTY_BOTH_3G_ENABLE, false);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDataPreferSIMID = SystemProperties.getInt(TelephonyProperties.PROPERTY_DATA_PREFER_SIM_ID, SimCardID.ID_ZERO.toInt());
        mDual3GEnable = SystemProperties.getBoolean(TelephonyProperties.PROPERTY_BOTH_3G_ENABLE, false);

        if (PhoneUtils.isDualMode) {
	        // Register intent broadcasts.
	        IntentFilter intentFilter =
	        new IntentFilter(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
	        intentFilter.addAction(TelephonyIntents.ACTION_DATA_CONNECTION_FAILED);
	        intentFilter.addAction(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED);
	        intentFilter.addAction(ConnectivityManager.ACTION_DATA_CONNECTION_CHANGE_FAIL);
	        intentFilter.addAction(ConnectivityManager.ACTION_DATA_RECONNECTION_FAIL);
	        registerReceiver(mReceiver, intentFilter);
        }

        // upon resumption from the sub-activity, make sure we re-enable the
        // preferences.
        getPreferenceScreen().setEnabled(true);

        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        mButtonDataEnabled.setChecked(cm.getMobileDataEnabled());

        // Set UI state in onResume because a user could go home, launch some
        // app to change this setting's backend, and re-launch this settings app
        // and the UI state would be inconsistent with actual state
        mButtonDataRoam.setChecked(mPhone.getDataRoamingEnabled());
        if (null != mButtonDataRoam2) mButtonDataRoam2.setChecked(mPhone2.getDataRoamingEnabled());

        if (getPreferenceScreen().findPreference(BUTTON_PREFERED_NETWORK_MODE) != null)  {
            mPhone.getPreferredNetworkType(mHandler.obtainMessage(
                    MyHandler.MESSAGE_GET_PREFERRED_NETWORK_TYPE));
        }
        if (getPreferenceScreen().findPreference(BUTTON_ENABLED_NETWORKS_KEY) != null)  {
            mPhone.getPreferredNetworkType(mHandler.obtainMessage(
                    MyHandler.MESSAGE_GET_PREFERRED_NETWORK_TYPE));
        }

        Phone phone1 = PhoneGlobals.getPhone(SimCardID.ID_ZERO);
        Phone phone2 = null;

        if (PhoneUtils.isDualMode) {
        	phone2 = PhoneGlobals.getPhone(SimCardID.ID_ONE);
        }

        if (null != phone1) {
            IccCard iccCard = phone1.getIccCard();
            if(iccCard.getState() == IccCardConstants.State.READY) {
                if(!iccCard.hasIccCard() || IccCardConstants.State.ABSENT == iccCard.getState()) {
                    if (DBG) log("SIM1 not avaiable");
                    mSIM1Available = false;
                    mSIM1ServiceStatus = ServiceStatus.POWER_OFF;
                }
            } else {
                if (DBG) log("SIM1 not avaiable");
                mSIM1Available = false;
                mSIM1ServiceStatus = ServiceStatus.POWER_OFF;
                mButtonDataRoam.setEnabled(false);
                mGsmUmtsOptions.setOptionsEnabled(false);
            }
        }

        if (null != phone2) {
            IccCard iccCard = phone2.getIccCard();
            if(iccCard.getState() == IccCardConstants.State.READY) {
                if(!iccCard.hasIccCard() || IccCardConstants.State.ABSENT == iccCard.getState()) {
                     if (DBG) log("SIM2 not avaiable");
                     mSIM2Available = false;
                     mSIM2ServiceStatus = ServiceStatus.POWER_OFF;
                }
            } else {
                if (DBG) log("SIM2 not avaiable");
                mSIM2Available = false;
                mSIM2ServiceStatus = ServiceStatus.POWER_OFF;
                if (null != mButtonDataRoam2) mButtonDataRoam2.setEnabled(false);
                if (null != mGsmUmtsOptions2) mGsmUmtsOptions2.setOptionsEnabled(false);
            }
        }

        if (!mButtonDataEnabled.isChecked()){
            //disable Data connection UI
            if (null != mDefaultSimForDataConnection) mDefaultSimForDataConnection.setEnabled(false);
        }
			
        if(DataConnectionState.CONNECTING== mDataConnectionStatus) {
            if (DBG) log("(DataConnectionState.CONNECTING== mDataConnectionStatus)");
                LockPreferenceUI();
        }
        if( DataConnectionState.WAIT4RATSWITCHED== mDataConnectionStatus) {
            if (DBG) log("( DataConnectionState.WAIT4RATSWITCHED== mDataConnectionStatus )");
                LockPreferenceUI();
        }

        if (PhoneUtils.isDualMode) {
            int current3gPreferStatus = getCurrent3gPreferStatus();
            if (mSIM1Available && !mSIM2Available) {
                mDefaultSimForDataConnection.setEntries(R.array.sim2_absent_data_connection_entries);
                mDefaultSimForDataConnection.setEntryValues(R.array.sim2_absent_data_connection_values);
                mDefaultSimFor3GNetwork.setEntries(R.array.sim2_absent_3g_network_entries);
                mDefaultSimFor3GNetwork.setEntryValues(R.array.sim2_absent_3g_network_values); //0, 2
                if (NETWORK_TYPE_SIM1_3G_SIM2_2G == current3gPreferStatus ||
                        NETWORK_TYPE_SIM1_3G_SIM2_3G == current3gPreferStatus) { //SIM1 3G, SIM2 absent
                    log("setValue " + NETWORK_TYPE_SIM1_3G_SIM2_2G);
                    mDefaultSimFor3GNetwork.setValue(String.valueOf(NETWORK_TYPE_SIM1_3G_SIM2_2G)); //0
                } else if (NETWORK_TYPE_SIM1_2G_SIM2_3G == current3gPreferStatus ||
                        NETWORK_TYPE_SIM1_2G_SIM2_2G == current3gPreferStatus) { //SIM1 2G, SIM2 absent
                    log("setValue " + NETWORK_TYPE_SIM1_2G_SIM2_2G);
                    mDefaultSimFor3GNetwork.setValue(String.valueOf(NETWORK_TYPE_SIM1_2G_SIM2_2G)); //2
                }
            } else if(!mSIM1Available && mSIM2Available) {
                mDefaultSimForDataConnection.setEntries(R.array.sim1_absent_data_connection_entries);
                mDefaultSimForDataConnection.setEntryValues(R.array.sim1_absent_data_connection_values);
                mDefaultSimFor3GNetwork.setEntries(R.array.sim1_absent_3g_network_entries);
                mDefaultSimFor3GNetwork.setEntryValues(R.array.sim1_absent_3g_network_values); //1, 2
                if (NETWORK_TYPE_SIM1_2G_SIM2_3G == current3gPreferStatus ||
                        NETWORK_TYPE_SIM1_3G_SIM2_3G == current3gPreferStatus) { //SIM1 absent, SIM2 3G
                    log("setValue " + NETWORK_TYPE_SIM1_2G_SIM2_3G);
                    mDefaultSimFor3GNetwork.setValue(String.valueOf(NETWORK_TYPE_SIM1_2G_SIM2_3G)); //1
                } else if (NETWORK_TYPE_SIM1_3G_SIM2_2G == current3gPreferStatus ||
                        NETWORK_TYPE_SIM1_2G_SIM2_2G == current3gPreferStatus) { //SIM1 absent, SIM2 2G
                    log("setValue " + NETWORK_TYPE_SIM1_2G_SIM2_2G);
                    mDefaultSimFor3GNetwork.setValue(String.valueOf(NETWORK_TYPE_SIM1_2G_SIM2_2G)); //2
                }
            } else if(!mSIM1Available && !mSIM2Available) {
                mDefaultSimForDataConnection.setEnabled(false);
                mDefaultSimFor3GNetwork.setEnabled(false);
            } else if (mDual3GEnable) {
                mDefaultSimFor3GNetwork.setEntries(R.array.dual_3g_network_entries);
                mDefaultSimFor3GNetwork.setEntryValues(R.array.dual_3g_network_values);
                if (current3gPreferStatus != NETWORK_TYPE_UNDEFINED) {
                    mDefaultSimFor3GNetwork.setValue(String.valueOf(current3gPreferStatus));
                }
            } else {
                if (current3gPreferStatus != NETWORK_TYPE_UNDEFINED) {
                    mDefaultSimFor3GNetwork.setValue(String.valueOf(current3gPreferStatus));
                }
            }
    
            mDefaultSimFor3GNetwork.setSummary(getCurrentRatSummary());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (PhoneUtils.isDualMode) {
        	unregisterReceiver(mReceiver);
        }
    }

    /**
     * Implemented to support onPreferenceChangeListener to look for preference
     * changes specifically on CLIR.
     *
     * @param preference is the preference to be changed, should be mButtonCLIR.
     * @param objValue should be the value of the selection, NOT its localized
     * display value.
     */
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mButtonPreferredNetworkMode) {
            //NOTE onPreferenceChange seems to be called even if there is no change
            //Check if the button value is changed from the System.Setting
            mButtonPreferredNetworkMode.setValue((String) objValue);
            int buttonNetworkMode;
            buttonNetworkMode = Integer.valueOf((String) objValue).intValue();
            int settingsNetworkMode = android.provider.Settings.Global.getInt(
                    mPhone.getContext().getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE, preferredNetworkMode);
            if (buttonNetworkMode != settingsNetworkMode) {
                int modemNetworkMode;
                // if new mode is invalid ignore it
                switch (buttonNetworkMode) {
                    case Phone.NT_MODE_WCDMA_PREF:
                    case Phone.NT_MODE_GSM_ONLY:
                    case Phone.NT_MODE_WCDMA_ONLY:
                    case Phone.NT_MODE_GSM_UMTS:
                    case Phone.NT_MODE_CDMA:
                    case Phone.NT_MODE_CDMA_NO_EVDO:
                    case Phone.NT_MODE_EVDO_NO_CDMA:
                    case Phone.NT_MODE_GLOBAL:
                    case Phone.NT_MODE_LTE_CDMA_AND_EVDO:
                    case Phone.NT_MODE_LTE_GSM_WCDMA:
                    case Phone.NT_MODE_LTE_CMDA_EVDO_GSM_WCDMA:
                    case Phone.NT_MODE_LTE_ONLY:
                    case Phone.NT_MODE_LTE_WCDMA:
                        // This is one of the modes we recognize
                        modemNetworkMode = buttonNetworkMode;
                        break;
                    default:
                        loge("Invalid Network Mode (" + buttonNetworkMode + ") chosen. Ignore.");
                        return true;
                }

                UpdatePreferredNetworkModeSummary(buttonNetworkMode);

                android.provider.Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Global.PREFERRED_NETWORK_MODE,
                        buttonNetworkMode );
                //Set the modem network mode
                mPhone.setPreferredNetworkType(modemNetworkMode, mHandler
                        .obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE));
            }
        } else if (preference == mButtonEnabledNetworks) {
            mButtonEnabledNetworks.setValue((String) objValue);
            int buttonNetworkMode;
            buttonNetworkMode = Integer.valueOf((String) objValue).intValue();
            if (DBG) log("buttonNetworkMode: " + buttonNetworkMode);
            int settingsNetworkMode = android.provider.Settings.Global.getInt(
                    mPhone.getContext().getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE, preferredNetworkMode);
            if (buttonNetworkMode != settingsNetworkMode) {
                int modemNetworkMode;
                // if new mode is invalid ignore it
                switch (buttonNetworkMode) {
                    case Phone.NT_MODE_WCDMA_PREF:
                    case Phone.NT_MODE_GSM_ONLY:
                    case Phone.NT_MODE_LTE_GSM_WCDMA:
                    case Phone.NT_MODE_LTE_CMDA_EVDO_GSM_WCDMA:
                    case Phone.NT_MODE_CDMA:
                    case Phone.NT_MODE_CDMA_NO_EVDO:
                    case Phone.NT_MODE_LTE_CDMA_AND_EVDO:
                        // This is one of the modes we recognize
                        modemNetworkMode = buttonNetworkMode;
                        break;
                    default:
                        loge("Invalid Network Mode (" + buttonNetworkMode + ") chosen. Ignore.");
                        return true;
                }

                UpdateEnabledNetworksValueAndSummary(buttonNetworkMode);

                android.provider.Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Global.PREFERRED_NETWORK_MODE,
                        buttonNetworkMode );
                //Set the modem network mode
                mPhone.setPreferredNetworkType(modemNetworkMode, mHandler
                        .obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE));
            }
        } else if (preference == mDefaultSimForDataConnection) {
            if (SystemProperties.get(TelephonyProperties.PROPERTY_DATA_PREFER_SIM_ID,
                String.valueOf(SimCardID.ID_ZERO.toInt())).equals((String) objValue))
                return true;
            if (((String)objValue).equals(String.valueOf(SimCardID.ID_ZERO.toInt()))) {
                if (0 == android.provider.Settings.System.getInt(getContentResolver(), android.provider.Settings.Global.PHONE1_ON, 1)) {
                    log("phone 0 is off");
                    return false;
                }
            } else if (((String)objValue).equals(String.valueOf(SimCardID.ID_ONE.toInt()))) {
                if (0 == android.provider.Settings.System.getInt(getContentResolver(), android.provider.Settings.Global.PHONE2_ON, 1)) {
                    log("phone 1 is off");
                    return false;
                }
            }
            SystemProperties.set(TelephonyProperties.PROPERTY_DATA_PREFER_SIM_ID, (String) objValue);
            switchDataPreferTo(Integer.parseInt((String)objValue));
            mDataPreferSIMID = (Integer.parseInt((String)objValue));
                log(" mDataPreferSIMID:"+ mDataPreferSIMID);
            mDataConnectionStatus = DataConnectionState.CONNECTING;
            LockPreferenceUI();
         } else if (preference == mDefaultSimFor3GNetwork) {
            int current3gPreferStatus = getCurrent3gPreferStatus();
            if (Integer.parseInt((String)objValue) == current3gPreferStatus) {
                log("same setting as before, skip ril setup");
                return true;
            }

            if ((NETWORK_TYPE_SIM1_3G_SIM2_2G == current3gPreferStatus &&  NETWORK_TYPE_SIM1_2G_SIM2_2G == Integer.parseInt((String)objValue))
			|| ( NETWORK_TYPE_SIM1_2G_SIM2_2G == current3gPreferStatus &&  NETWORK_TYPE_SIM1_3G_SIM2_2G == Integer.parseInt((String)objValue))
                        || ( NETWORK_TYPE_SIM1_2G_SIM2_3G == current3gPreferStatus &&  NETWORK_TYPE_SIM1_3G_SIM2_3G == Integer.parseInt((String)objValue))){
                // SIM1 RAT switching
                if (DBG) log("SIM1 RAT switching");
                mRatSwitchState = RATSwithStatus.SIM1_RAT_SWITCHING;
                mSIM1ServiceStatus = ServiceStatus.IGNORE_FIRST_OUT_OF_SERVICE;
            }
            if ((NETWORK_TYPE_SIM1_2G_SIM2_3G == current3gPreferStatus &&  NETWORK_TYPE_SIM1_2G_SIM2_2G == Integer.parseInt((String)objValue))
			|| ( NETWORK_TYPE_SIM1_2G_SIM2_2G == current3gPreferStatus &&  NETWORK_TYPE_SIM1_2G_SIM2_3G == Integer.parseInt((String)objValue))
                        || ( NETWORK_TYPE_SIM1_3G_SIM2_2G == current3gPreferStatus &&  NETWORK_TYPE_SIM1_3G_SIM2_3G == Integer.parseInt((String)objValue))){
                // SIM2 RAT switching
                if (DBG) log("SIM2 RAT switching");
                mRatSwitchState = RATSwithStatus.SIM2_RAT_SWITCHING;
                mSIM2ServiceStatus = ServiceStatus.IGNORE_FIRST_OUT_OF_SERVICE;
            }
            if ((NETWORK_TYPE_SIM1_3G_SIM2_2G == current3gPreferStatus &&  NETWORK_TYPE_SIM1_2G_SIM2_3G == Integer.parseInt((String)objValue))
			|| ( NETWORK_TYPE_SIM1_2G_SIM2_3G == current3gPreferStatus &&  NETWORK_TYPE_SIM1_3G_SIM2_2G == Integer.parseInt((String)objValue))
                        || ( NETWORK_TYPE_SIM1_2G_SIM2_2G == current3gPreferStatus &&  NETWORK_TYPE_SIM1_3G_SIM2_3G == Integer.parseInt((String)objValue))){
                if (DBG) log("mDataPreferSIMID:" + mDataPreferSIMID);
                if( !mSIM1Available && (mDataPreferSIMID == SimCardID.ID_ZERO.toInt())){
                    if (DBG) log(" SIM1 not Available, SIM2 RAT switching");
                    mRatSwitchState = RATSwithStatus.SIM2_RAT_SWITCHING;
                    mSIM2ServiceStatus = ServiceStatus.IGNORE_FIRST_OUT_OF_SERVICE;	
                } else if (!mSIM2Available && (mDataPreferSIMID == SimCardID.ID_ONE.toInt())){
                    if (DBG) log("SIM2 not Available, SIM1 RAT switching");
                    mRatSwitchState = RATSwithStatus.SIM1_RAT_SWITCHING;
                    mSIM1ServiceStatus = ServiceStatus.IGNORE_FIRST_OUT_OF_SERVICE;
                } else{		
                if (DBG) log("BOTH SIM RAT switching");
                mRatSwitchState = RATSwithStatus.BOTH_SIM_RAT_SWITCHING;
                mSIM1ServiceStatus = ServiceStatus.IGNORE_FIRST_OUT_OF_SERVICE;
                mSIM2ServiceStatus = ServiceStatus.IGNORE_FIRST_OUT_OF_SERVICE;
                }
            }
		
            switchNetworkType(Integer.parseInt((String)objValue), current3gPreferStatus);
            mDataPreferSIMID = SystemProperties.getInt(TelephonyProperties.PROPERTY_DATA_PREFER_SIM_ID, SimCardID.ID_ZERO.toInt());
            if (DBG) log("mDataPreferSIMID:" + mDataPreferSIMID);
            if (mButtonDataEnabled.isChecked()){
                if (DBG) log("Data enabled, wait for connected");
                mDataConnectionStatus = DataConnectionState.CONNECTING;
            	} else {
                if (DBG) log("Data disabled, wait for connected");
                mDataConnectionStatus = DataConnectionState.WAIT4RATSWITCHED;
            	}		
		
            LockPreferenceUI();
         }

        // always let the preference setting proceed.
        return true;
    }

    private void switchDataPreferTo(int simId){
        if(mConnManager == null){
            mConnManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        }
        if(simId == SimCardID.ID_ONE.toInt()){
            mConnManager.switchToSim2DataNetwork();
        }else{
            mConnManager.switchToSim1DataNetwork();
        }
    }

    /** BRCM DUAL_3G design.
     * Original RAT switch design from Android takes one RAT option in one RIL daemon.
     * So in 3G/3G case, two RIL commands are needed:
     * mPhone.setPreferredNetworkType(Phone.NT_MODE_WCDMA_PREF,
     *         mHandler.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE_DUALSIM));
     * mPhone2.setPreferredNetworkType(Phone.NT_MODE_WCDMA_PREF,
     *         mHandler.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE_DUALSIM));
     * Then two MESSAGE_SET_PREFERRED_NETWORK_TYPE_DUALSIM events are expected.
     *
     * To avoid this problem, use a one-shot command design in 3G/3G case.
     * A flag in the 16th bit represents the one-shot command.
     * OneShotCmd: 0x10000 | 0x[SIM1RAT]00 | 0x[SIM2RAT]
     * mPhone.setPreferredNetworkType(OneShotCmd,
     *         mHandler.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE_DUALSIM));
     */

    private void switchNetworkType(int preferNetworkType, int previousPreferNetworkType) {
        log("Switching prefer network type to " + preferNetworkType);
        switch (preferNetworkType) {
            case NETWORK_TYPE_SIM1_3G_SIM2_2G:    //SIM1 is 3G
                mSIM1NetworkType = Phone.NT_MODE_WCDMA_PREF;
                mSIM2NetworkType = Phone.NT_MODE_GSM_ONLY;
                if (mDual3GEnable && NETWORK_TYPE_SIM1_2G_SIM2_2G != previousPreferNetworkType) {
                    if (NETWORK_TYPE_SIM1_2G_SIM2_3G == previousPreferNetworkType) {
                        //mPhone.setPreferredNetworkType(Phone.NT_MODE_WCDMA_PREF, mHandler.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE_DUALSIM));
                        //mPhone2.setPreferredNetworkType(Phone.NT_MODE_GSM_ONLY, mHandler.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE_DUALSIM));
                        mPhone.setPreferredNetworkType(0x01 << 16 | Phone.NT_MODE_GSM_ONLY << 8 | Phone.NT_MODE_WCDMA_PREF,
                                mHandler.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE_DUALSIM));
                        log("cmd: " + Integer.toHexString(0x01 << 16 | Phone.NT_MODE_GSM_ONLY << 8 | Phone.NT_MODE_WCDMA_PREF));
                    } else if (NETWORK_TYPE_SIM1_3G_SIM2_3G == previousPreferNetworkType) {
                        mPhone2.setPreferredNetworkType(Phone.NT_MODE_GSM_ONLY, mHandler.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE_DUALSIM));
                    }
                } else {
                    mPhone.setPreferredNetworkType(Phone.NT_MODE_WCDMA_PREF, mHandler.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE_DUALSIM));
                }
                break;
            case NETWORK_TYPE_SIM1_2G_SIM2_3G:    //SIM2 is 3G
                mSIM1NetworkType = Phone.NT_MODE_GSM_ONLY;
                mSIM2NetworkType = Phone.NT_MODE_WCDMA_PREF;
                if (mDual3GEnable && NETWORK_TYPE_SIM1_2G_SIM2_2G != previousPreferNetworkType) {
                    if (NETWORK_TYPE_SIM1_3G_SIM2_2G == previousPreferNetworkType) {
                        //mPhone.setPreferredNetworkType(Phone.NT_MODE_GSM_ONLY, mHandler.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE_DUALSIM));
                        //mPhone2.setPreferredNetworkType(Phone.NT_MODE_WCDMA_PREF, mHandler.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE_DUALSIM));
                        mPhone.setPreferredNetworkType(0x01 << 16 | Phone.NT_MODE_WCDMA_PREF << 8 | Phone.NT_MODE_GSM_ONLY,
                                mHandler.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE_DUALSIM));
                        log("cmd: " + Integer.toHexString(0x01 << 16 | Phone.NT_MODE_WCDMA_PREF << 8 | Phone.NT_MODE_GSM_ONLY));
                    } else if (NETWORK_TYPE_SIM1_3G_SIM2_3G == previousPreferNetworkType) {
                        mPhone.setPreferredNetworkType(Phone.NT_MODE_GSM_ONLY, mHandler.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE_DUALSIM));
                    }
                } else {
                    mPhone2.setPreferredNetworkType(Phone.NT_MODE_WCDMA_PREF, mHandler.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE_DUALSIM));
                }
                break;
            case NETWORK_TYPE_SIM1_2G_SIM2_2G:    //Both SIMs are 2G
                mSIM1NetworkType = Phone.NT_MODE_GSM_ONLY;
                mSIM2NetworkType = Phone.NT_MODE_GSM_ONLY;
                if (NETWORK_TYPE_SIM1_3G_SIM2_2G == previousPreferNetworkType)
                    mPhone.setPreferredNetworkType(Phone.NT_MODE_GSM_ONLY, mHandler.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE_DUALSIM));
                else if (NETWORK_TYPE_SIM1_2G_SIM2_3G == previousPreferNetworkType)
                    mPhone2.setPreferredNetworkType(Phone.NT_MODE_GSM_ONLY, mHandler.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE_DUALSIM));
                else if (mDual3GEnable && NETWORK_TYPE_SIM1_3G_SIM2_3G == previousPreferNetworkType) {
                    //mPhone.setPreferredNetworkType(Phone.NT_MODE_GSM_ONLY, mHandler.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE_DUALSIM));
                    //mPhone2.setPreferredNetworkType(Phone.NT_MODE_GSM_ONLY, mHandler.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE_DUALSIM));
                    mPhone.setPreferredNetworkType(0x01 << 16 | Phone.NT_MODE_GSM_ONLY << 8 | Phone.NT_MODE_GSM_ONLY,
                                mHandler.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE_DUALSIM));
                    log("cmd: " + Integer.toHexString(0x01 << 16 | Phone.NT_MODE_GSM_ONLY << 8 | Phone.NT_MODE_GSM_ONLY));
                }
                break;
            case NETWORK_TYPE_SIM1_3G_SIM2_3G:    //Both SIMs are 3G
                if (!mDual3GEnable) {
                    log("Dual 3G disabled");
                    return;
                }
                if (NETWORK_TYPE_SIM1_3G_SIM2_2G == previousPreferNetworkType)
                    mPhone2.setPreferredNetworkType(Phone.NT_MODE_WCDMA_PREF, mHandler.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE_DUALSIM));
                else if (NETWORK_TYPE_SIM1_2G_SIM2_3G == previousPreferNetworkType)
                    mPhone.setPreferredNetworkType(Phone.NT_MODE_WCDMA_PREF, mHandler.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE_DUALSIM));
                else if (NETWORK_TYPE_SIM1_2G_SIM2_2G == previousPreferNetworkType) {
                    //mPhone.setPreferredNetworkType(Phone.NT_MODE_WCDMA_PREF, mHandler.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE_DUALSIM));
                    //mPhone2.setPreferredNetworkType(Phone.NT_MODE_WCDMA_PREF, mHandler.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE_DUALSIM));
                    mPhone.setPreferredNetworkType(0x01 << 16 | Phone.NT_MODE_WCDMA_PREF << 8 | Phone.NT_MODE_WCDMA_PREF,
                                mHandler.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE_DUALSIM));
                    log("cmd: " + Integer.toHexString(0x01 << 16 | Phone.NT_MODE_WCDMA_PREF << 8 | Phone.NT_MODE_WCDMA_PREF));
                }
                break;
        }
    }

    private int getCurrent3gPreferStatus() {
        String propNetworkTypeSim1 =
                SystemProperties.get(TelephonyProperties.PROPERTY_NETWORK_TYPE,
                String.valueOf(Phone.NT_MODE_WCDMA_PREF));
        String propNetworkTypeSim2 =
                SystemProperties.get(TelephonyProperties.PROPERTY_NETWORK_TYPE + "_"
                + String.valueOf(SimCardID.ID_ONE.toInt()), String.valueOf(Phone.NT_MODE_GSM_ONLY));

        if ((propNetworkTypeSim1.equals(String.valueOf(Phone.NT_MODE_WCDMA_PREF))
                || propNetworkTypeSim1.equals(String.valueOf(Phone.NT_MODE_WCDMA_ONLY)))
                && propNetworkTypeSim2.equals(String.valueOf(Phone.NT_MODE_GSM_ONLY))) {
            // SIM1 3G prefer or 3G only
            // SIM2 2G only
            return NETWORK_TYPE_SIM1_3G_SIM2_2G;
        } else if (propNetworkTypeSim1.equals(String.valueOf(Phone.NT_MODE_GSM_ONLY))
                && (propNetworkTypeSim2.equals(String.valueOf(Phone.NT_MODE_WCDMA_PREF))
                || propNetworkTypeSim2.equals(String.valueOf(Phone.NT_MODE_WCDMA_ONLY)))) {
            // SIM1 2G only
            // SIM2 3G prefer or 3G only
            return NETWORK_TYPE_SIM1_2G_SIM2_3G;
        } else if (propNetworkTypeSim1.equals(String.valueOf(Phone.NT_MODE_GSM_ONLY))
                && propNetworkTypeSim2.equals(String.valueOf(Phone.NT_MODE_GSM_ONLY))) {
            // SIM1 2G only
            // SIM2 2G only
            return NETWORK_TYPE_SIM1_2G_SIM2_2G;
        } else if ((propNetworkTypeSim1.equals(String.valueOf(Phone.NT_MODE_WCDMA_PREF))
                || propNetworkTypeSim1.equals(String.valueOf(Phone.NT_MODE_WCDMA_ONLY)))
                && (propNetworkTypeSim2.equals(String.valueOf(Phone.NT_MODE_WCDMA_PREF))
                || propNetworkTypeSim2.equals(String.valueOf(Phone.NT_MODE_WCDMA_ONLY)))
                && mDual3GEnable) {
            // SIM1 3G prefer or 3G only
            // SIM2 3G prefer or 3G only
            // both 3G property enabled
            return NETWORK_TYPE_SIM1_3G_SIM2_3G;
        } else {
            return NETWORK_TYPE_UNDEFINED;
        }
    }

    private String[] mRatLabels = {
            "WCDMA preferred",
            "GSM only",
            "WCDMA only",
            "GSM auto (PRL)",
            "CDMA auto (PRL)",
            "CDMA only",
            "EvDo only",
            "GSM/CDMA auto (PRL)",
            "Unknown"};

    private String getCurrentRatSummary() {
        int propNetworkTypeSim1 =
                SystemProperties.getInt(TelephonyProperties.PROPERTY_NETWORK_TYPE,
                Phone.NT_MODE_WCDMA_PREF);

        if (!mSIM1Available || propNetworkTypeSim1 > mRatLabels.length - 1) {
            propNetworkTypeSim1 = mRatLabels.length - 1;  // Unknown
        }
        int propNetworkTypeSim2 =
                SystemProperties.getInt(TelephonyProperties.PROPERTY_NETWORK_TYPE + "_"
                + String.valueOf(SimCardID.ID_ONE.toInt()), Phone.NT_MODE_GSM_ONLY);

        if (!mSIM2Available || propNetworkTypeSim2 > mRatLabels.length - 1) {
            propNetworkTypeSim2 = mRatLabels.length - 1;  // Unknown
        }

        return "SIM1 " + mRatLabels[propNetworkTypeSim1] + "/SIM2 " + mRatLabels[propNetworkTypeSim2];
    }

    private class MyHandler extends Handler {

        static final int MESSAGE_GET_PREFERRED_NETWORK_TYPE = 0;
        static final int MESSAGE_SET_PREFERRED_NETWORK_TYPE = 1;
        static final int MESSAGE_BPM_DISABLE = 2;
        static final int MESSAGE_BPM_ENABLE = 3;
        static final int MESSAGE_GET_PREFERRED_NETWORK_TYPE_DUALSIM = 4;
        static final int MESSAGE_SET_PREFERRED_NETWORK_TYPE_DUALSIM = 5;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_GET_PREFERRED_NETWORK_TYPE:
                    handleGetPreferredNetworkTypeResponse(msg);
                    break;

                case MESSAGE_SET_PREFERRED_NETWORK_TYPE:
                    handleSetPreferredNetworkTypeResponse(msg);
                    break;
                case MESSAGE_BPM_ENABLE:
                {
                    AsyncResult ar = (AsyncResult) msg.obj;
                    if (ar.exception == null) {
                        if (DBG) Log.d(LOG_TAG, "RIL Callback BPM_Enable Succeed");
                        SystemProperties.set(TelephonyProperties.PROPERTY_BPM_SETTING_ENABLE, "1");
                        
                    }else{
                        if (DBG) Log.e(LOG_TAG, "Error !! RIL Callback BPM_Enable Failed, Rollback state(false)");
                        if (null != mButtonBPMEnabled) mButtonBPMEnabled.setChecked(false);
                    }
                    break;
                }
                case MESSAGE_BPM_DISABLE:
                {
                    AsyncResult ar = (AsyncResult) msg.obj;
                    if (ar.exception == null) {
                        if (DBG) Log.d(LOG_TAG, "RIL Callback BPM_Disable Succeed");
                        SystemProperties.set(TelephonyProperties.PROPERTY_BPM_SETTING_ENABLE, "0");
                    }else{
                        if (DBG) Log.e(LOG_TAG, "Error !! RIL Callback BPM_Disable Failed, Rollback state(true)");
                        if (null != mButtonBPMEnabled) mButtonBPMEnabled.setChecked(true);
                    }
                    break;
                }
                case MESSAGE_GET_PREFERRED_NETWORK_TYPE_DUALSIM:
                    handleGetPreferredNetworkTypeResponseDualSim(msg);
                    break;
                case MESSAGE_SET_PREFERRED_NETWORK_TYPE_DUALSIM:
                    handleSetPreferredNetworkTypeResponseDualSim(msg);
                    break;
            }
        }

        private void handleGetPreferredNetworkTypeResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;

            if (ar.exception == null) {
                int modemNetworkMode = ((int[])ar.result)[0];

                if (DBG) {
                    log ("handleGetPreferredNetworkTypeResponse: modemNetworkMode = " +
                            modemNetworkMode);
                }

                int settingsNetworkMode = android.provider.Settings.Global.getInt(
                        mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Global.PREFERRED_NETWORK_MODE,
                        preferredNetworkMode);

                if (DBG) {
                    log("handleGetPreferredNetworkTypeReponse: settingsNetworkMode = " +
                            settingsNetworkMode);
                }

                //check that modemNetworkMode is from an accepted value
                if (modemNetworkMode == Phone.NT_MODE_WCDMA_PREF ||
                        modemNetworkMode == Phone.NT_MODE_GSM_ONLY ||
                        modemNetworkMode == Phone.NT_MODE_WCDMA_ONLY ||
                        modemNetworkMode == Phone.NT_MODE_GSM_UMTS ||
                        modemNetworkMode == Phone.NT_MODE_CDMA ||
                        modemNetworkMode == Phone.NT_MODE_CDMA_NO_EVDO ||
                        modemNetworkMode == Phone.NT_MODE_EVDO_NO_CDMA ||
                        modemNetworkMode == Phone.NT_MODE_GLOBAL ||
                        modemNetworkMode == Phone.NT_MODE_LTE_CDMA_AND_EVDO ||
                        modemNetworkMode == Phone.NT_MODE_LTE_GSM_WCDMA ||
                        modemNetworkMode == Phone.NT_MODE_LTE_CMDA_EVDO_GSM_WCDMA ||
                        modemNetworkMode == Phone.NT_MODE_LTE_ONLY ||
                        modemNetworkMode == Phone.NT_MODE_LTE_WCDMA) {
                    if (DBG) {
                        log("handleGetPreferredNetworkTypeResponse: if 1: modemNetworkMode = " +
                                modemNetworkMode);
                    }

                    //check changes in modemNetworkMode and updates settingsNetworkMode
                    if (modemNetworkMode != settingsNetworkMode) {
                        if (DBG) {
                            log("handleGetPreferredNetworkTypeResponse: if 2: " +
                                    "modemNetworkMode != settingsNetworkMode");
                        }

                        settingsNetworkMode = modemNetworkMode;

                        if (DBG) { log("handleGetPreferredNetworkTypeResponse: if 2: " +
                                "settingsNetworkMode = " + settingsNetworkMode);
                        }

                        //changes the Settings.System accordingly to modemNetworkMode
                        android.provider.Settings.Global.putInt(
                                mPhone.getContext().getContentResolver(),
                                android.provider.Settings.Global.PREFERRED_NETWORK_MODE,
                                settingsNetworkMode );
                    }

                    UpdatePreferredNetworkModeSummary(modemNetworkMode);
                    UpdateEnabledNetworksValueAndSummary(modemNetworkMode);
                    // changes the mButtonPreferredNetworkMode accordingly to modemNetworkMode
                    mButtonPreferredNetworkMode.setValue(Integer.toString(modemNetworkMode));
                } else {
                    if (DBG) log("handleGetPreferredNetworkTypeResponse: else: reset to default");
                    resetNetworkModeToDefault();
                }
            }
        }

        private void handleSetPreferredNetworkTypeResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;

            if (ar.exception == null) {
                int networkMode = Integer.valueOf(
                        mButtonPreferredNetworkMode.getValue()).intValue();
                android.provider.Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Global.PREFERRED_NETWORK_MODE,
                        networkMode );
                networkMode = Integer.valueOf(
                        mButtonEnabledNetworks.getValue()).intValue();
                android.provider.Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Global.PREFERRED_NETWORK_MODE,
                        networkMode );
            } else {
                mPhone.getPreferredNetworkType(obtainMessage(MESSAGE_GET_PREFERRED_NETWORK_TYPE));
            }
        }

        private void resetNetworkModeToDefault() {
            //set the mButtonPreferredNetworkMode
            mButtonPreferredNetworkMode.setValue(Integer.toString(preferredNetworkMode));
            mButtonEnabledNetworks.setValue(Integer.toString(preferredNetworkMode));
            //set the Settings.System
            android.provider.Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Global.PREFERRED_NETWORK_MODE,
                        preferredNetworkMode );
            //Set the Modem
            mPhone.setPreferredNetworkType(preferredNetworkMode,
                    this.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE));
        }
    }

    private void UpdatePreferredNetworkModeSummary(int NetworkMode) {
        switch(NetworkMode) {
            case Phone.NT_MODE_WCDMA_PREF:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_wcdma_perf_summary);
                break;
            case Phone.NT_MODE_GSM_ONLY:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_gsm_only_summary);
                break;
            case Phone.NT_MODE_WCDMA_ONLY:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_wcdma_only_summary);
                break;
            case Phone.NT_MODE_GSM_UMTS:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_gsm_wcdma_summary);
                break;
            case Phone.NT_MODE_CDMA:
                switch (mPhone.getLteOnCdmaMode()) {
                    case PhoneConstants.LTE_ON_CDMA_TRUE:
                        mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_cdma_summary);
                    break;
                    case PhoneConstants.LTE_ON_CDMA_FALSE:
                    default:
                        mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_cdma_evdo_summary);
                        break;
                }
                break;
            case Phone.NT_MODE_CDMA_NO_EVDO:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_cdma_only_summary);
                break;
            case Phone.NT_MODE_EVDO_NO_CDMA:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_evdo_only_summary);
                break;
            case Phone.NT_MODE_LTE_ONLY:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_lte_summary);
                break;
            case Phone.NT_MODE_LTE_GSM_WCDMA:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_lte_gsm_wcdma_summary);
                break;
            case Phone.NT_MODE_LTE_CDMA_AND_EVDO:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_lte_cdma_evdo_summary);
                break;
            case Phone.NT_MODE_LTE_CMDA_EVDO_GSM_WCDMA:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_global_summary);
                break;
            case Phone.NT_MODE_GLOBAL:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_cdma_evdo_gsm_wcdma_summary);
                break;
            case Phone.NT_MODE_LTE_WCDMA:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_lte_wcdma_summary);
                break;
            default:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_global_summary);
        }
    }

    private void UpdateEnabledNetworksValueAndSummary(int NetworkMode) {
        switch (NetworkMode) {
            case Phone.NT_MODE_WCDMA_ONLY:
            case Phone.NT_MODE_GSM_UMTS:
            case Phone.NT_MODE_WCDMA_PREF:
                if (!mIsGlobalCdma) {
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(Phone.NT_MODE_WCDMA_PREF));
                    mButtonEnabledNetworks.setSummary(R.string.network_3G);
                } else {
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(Phone.NT_MODE_LTE_CMDA_EVDO_GSM_WCDMA));
                    mButtonEnabledNetworks.setSummary(R.string.network_global);
                }
                break;
            case Phone.NT_MODE_GSM_ONLY:
                if (!mIsGlobalCdma) {
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(Phone.NT_MODE_GSM_ONLY));
                    mButtonEnabledNetworks.setSummary(R.string.network_2G);
                } else {
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(Phone.NT_MODE_LTE_CMDA_EVDO_GSM_WCDMA));
                    mButtonEnabledNetworks.setSummary(R.string.network_global);
                }
                break;
            case Phone.NT_MODE_LTE_GSM_WCDMA:
            case Phone.NT_MODE_LTE_ONLY:
            case Phone.NT_MODE_LTE_WCDMA:
                if (!mIsGlobalCdma) {
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(Phone.NT_MODE_LTE_GSM_WCDMA));
                    mButtonEnabledNetworks.setSummary((mShow4GForLTE == true)
                            ? R.string.network_4G : R.string.network_lte);
                } else {
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(Phone.NT_MODE_LTE_CMDA_EVDO_GSM_WCDMA));
                    mButtonEnabledNetworks.setSummary(R.string.network_global);
                }
                break;
            case Phone.NT_MODE_LTE_CDMA_AND_EVDO:
                mButtonEnabledNetworks.setValue(
                        Integer.toString(Phone.NT_MODE_LTE_CDMA_AND_EVDO));
                mButtonEnabledNetworks.setSummary(R.string.network_lte);
                break;
            case Phone.NT_MODE_CDMA:
            case Phone.NT_MODE_EVDO_NO_CDMA:
            case Phone.NT_MODE_GLOBAL:
                mButtonEnabledNetworks.setValue(
                        Integer.toString(Phone.NT_MODE_CDMA));
                mButtonEnabledNetworks.setSummary(R.string.network_3G);
                break;
            case Phone.NT_MODE_CDMA_NO_EVDO:
                mButtonEnabledNetworks.setValue(
                        Integer.toString(Phone.NT_MODE_CDMA_NO_EVDO));
                mButtonEnabledNetworks.setSummary(R.string.network_1x);
                break;
            case Phone.NT_MODE_LTE_CMDA_EVDO_GSM_WCDMA:
                mButtonEnabledNetworks.setValue(
                        Integer.toString(Phone.NT_MODE_LTE_CMDA_EVDO_GSM_WCDMA));
                mButtonEnabledNetworks.setSummary(R.string.network_global);
                break;
            default:
                String errMsg = "Invalid Network Mode (" + NetworkMode + "). Ignore.";
                loge(errMsg);
                mButtonEnabledNetworks.setSummary(errMsg);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
        case REQUEST_CODE_EXIT_ECM:
            Boolean isChoiceYes =
                data.getBooleanExtra(EmergencyCallbackModeExitDialog.EXTRA_EXIT_ECM_RESULT, false);
            if (isChoiceYes) {
                // If the phone exits from ECM mode, show the CDMA Options
                mCdmaOptions.showDialog(mClickedPreference);
            } else {
                // do nothing
            }
            break;

        default:
            break;
        }
    }

    public void onUpdateSIM1NetworkType(int type) {
        if (DBG) Log.d(LOG_TAG, "=>onUpdateSIM1NetworkType(): type = " + type + ", mSIM1NetworkType = " + mSIM1NetworkType + ", mSIM2NetworkType = " + mSIM2NetworkType);
        if (type == Phone.NT_MODE_WCDMA_PREF) {
            mSIM1NetworkType = type;
        } else if (type == Phone.NT_MODE_GSM_ONLY) {
            mSIM1NetworkType = type;
        }
    }

    public void onUpdateSIM2NetworkType(int type) {
        if (DBG) Log.d(LOG_TAG, "=>onUpdateSIM2NetworkType(): type = " + type + ", mSIM1NetworkType = " + mSIM1NetworkType + ", mSIM2NetworkType = " + mSIM2NetworkType);
        if (type == Phone.NT_MODE_WCDMA_PREF) {
            mSIM2NetworkType = type;
        } else if (type == Phone.NT_MODE_GSM_ONLY) {
            mSIM2NetworkType = type;
        }
    }

    private void handleSetPreferredNetworkTypeResponseDualSim(Message msg) {
        AsyncResult ar = (AsyncResult) msg.obj;

        if (ar.exception == null) {
            // Setting.Secure only has one PREFERRED_NETWORK_MODE for SIM1
            android.provider.Settings.Secure.putInt(mPhone.getContext().getContentResolver(),
            android.provider.Settings.Global.PREFERRED_NETWORK_MODE,
            mSIM1NetworkType);
            if(((Integer.valueOf(SystemProperties.get(TelephonyProperties.PROPERTY_DATA_PREFER_SIM_ID,String.valueOf(SimCardID.ID_ZERO.toInt())))== SimCardID.ID_ZERO.toInt())
            && !mSIM1Available) 	||
            ((Integer.valueOf(SystemProperties.get(TelephonyProperties.PROPERTY_DATA_PREFER_SIM_ID,String.valueOf(SimCardID.ID_ZERO.toInt())))== SimCardID.ID_ONE.toInt())
            && !mSIM2Available)
            ){
                //data prefer sim not exist
                //mDataConnectionStatus = DataConnectionState.DISCONNECTED;
                //UnLockPreferenceUI(UIUnLockReason.NO_SERVICE);
            }
        } else {
            if (DBG) log("set preferred network type, exception=" + ar.exception);
            mDataConnectionStatus = DataConnectionState.DISCONNECTED;
            UnLockPreferenceUI(UIUnLockReason.NO_SERVICE);
            // reset menu
            int current3gPreferStatus = getCurrent3gPreferStatus();
            if(mSIM1Available && !mSIM2Available) {
                if (NETWORK_TYPE_SIM1_3G_SIM2_2G == current3gPreferStatus ||
                        NETWORK_TYPE_SIM1_3G_SIM2_3G == current3gPreferStatus) { //SIM1 3G, SIM2 absent
                    log("setValue " + NETWORK_TYPE_SIM1_3G_SIM2_2G);
                    mDefaultSimFor3GNetwork.setValue(String.valueOf(NETWORK_TYPE_SIM1_3G_SIM2_2G)); //0
                } else if (NETWORK_TYPE_SIM1_2G_SIM2_3G == current3gPreferStatus ||
                        NETWORK_TYPE_SIM1_2G_SIM2_2G == current3gPreferStatus) { //SIM1 2G, SIM2 absent
                    log("setValue " + NETWORK_TYPE_SIM1_2G_SIM2_2G);
                    mDefaultSimFor3GNetwork.setValue(String.valueOf(NETWORK_TYPE_SIM1_2G_SIM2_2G)); //2
                }
            } else if(!mSIM1Available && mSIM2Available) {
                if (NETWORK_TYPE_SIM1_2G_SIM2_3G == current3gPreferStatus ||
                        NETWORK_TYPE_SIM1_3G_SIM2_3G == current3gPreferStatus) { //SIM1 absent, SIM2 3G
                    log("setValue " + NETWORK_TYPE_SIM1_2G_SIM2_3G);
                    mDefaultSimFor3GNetwork.setValue(String.valueOf(NETWORK_TYPE_SIM1_2G_SIM2_3G)); //1
                } else if (NETWORK_TYPE_SIM1_3G_SIM2_2G == current3gPreferStatus ||
                        NETWORK_TYPE_SIM1_2G_SIM2_2G == current3gPreferStatus) { //SIM1 absent, SIM2 2G
                    log("setValue " + NETWORK_TYPE_SIM1_2G_SIM2_2G);
                    mDefaultSimFor3GNetwork.setValue(String.valueOf(NETWORK_TYPE_SIM1_2G_SIM2_2G)); //2
                }
            } else if(!mSIM1Available && !mSIM2Available) {
                mDefaultSimFor3GNetwork.setEnabled(false);
            } else {
                if (current3gPreferStatus != NETWORK_TYPE_UNDEFINED) {
                    mDefaultSimFor3GNetwork.setValue(String.valueOf(current3gPreferStatus));
                }
            }
        }
    }

    private void handleGetPreferredNetworkTypeResponseDualSim(Message msg) {
        AsyncResult ar = (AsyncResult) msg.obj;

        if (ar.exception == null) {
            log("get preferred network type done.");
        } else {
            log("get preferred network type, exception=" + ar.exception);
        }
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    private static void loge(String msg) {
        Log.e(LOG_TAG, msg);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {  // See ActionBar#setDisplayHomeAsUpEnabled()
            // Commenting out "logical up" capability. This is a workaround for issue 5278083.
            //
            // Settings app may not launch this activity via UP_ACTIVITY_CLASS but the other
            // Activity that looks exactly same as UP_ACTIVITY_CLASS ("SubSettings" Activity).
            // At that moment, this Activity launches UP_ACTIVITY_CLASS on top of the Activity.
            // which confuses users.
            // TODO: introduce better mechanism for "up" capability here.
            /*Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setClassName(UP_ACTIVITY_PACKAGE, UP_ACTIVITY_CLASS);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);*/
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
