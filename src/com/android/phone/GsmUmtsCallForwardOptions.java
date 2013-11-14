package com.android.phone;

import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.CommandsInterface;

import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;
import android.view.MenuItem;

import java.util.ArrayList;

import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.RILConstants.SimCardID;
import com.android.internal.telephony.TelephonyIntents;

public class GsmUmtsCallForwardOptions extends TimeConsumingPreferenceActivity {
    private static final String LOG_TAG = "GsmUmtsCallForwardOptions";
    private final boolean DBG = (PhoneGlobals.DBG_LEVEL >= 2);

    private static final String NUM_PROJECTION[] = {Phone.NUMBER};

    private static final String BUTTON_CFU_KEY   = "button_cfu_key";
    private static final String BUTTON_CFB_KEY   = "button_cfb_key";
    private static final String BUTTON_CFNRY_KEY = "button_cfnry_key";
    private static final String BUTTON_CFNRC_KEY = "button_cfnrc_key";
    private static final String BUTTON_DATA_CFU_KEY = "button_data_cfu_key";
    private static final String BUTTON_DATA_CFB_KEY   = "button_data_cfb_key";
    private static final String BUTTON_DATA_CFNRY_KEY = "button_data_cfnry_key";
    private static final String BUTTON_DATA_CFNRC_KEY = "button_data_cfnrc_key";

    private static final String KEY_TOGGLE = "toggle";
    private static final String KEY_STATUS = "status";
    private static final String KEY_NUMBER = "number";

    private CallForwardEditPreference mButtonCFU;
    private CallForwardEditPreference mButtonCFB;
    private CallForwardEditPreference mButtonCFNRy;
    private CallForwardEditPreference mButtonCFNRc;
    private CallForwardEditPreference mButtonDataCFU;
    private CallForwardEditPreference mButtonDataCFB;
    private CallForwardEditPreference mButtonDataCFNRy;
    private CallForwardEditPreference mButtonDataCFNRc;    
    

    private final ArrayList<CallForwardEditPreference> mPreferences =
            new ArrayList<CallForwardEditPreference> ();
    private int mInitIndex= 0;

    private boolean mFirstResume;
    private Bundle mIcicle;
    private int mSimId = SimCardID.ID_ZERO.toInt();

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        String strSimId = getIntent().getDataString();
        try {
            mSimId = Integer.parseInt(strSimId);
        } catch (NumberFormatException ex) {
            mSimId = SimCardID.ID_ZERO.toInt();
        }
        if (DBG) Log.d(LOG_TAG, "----mPhoneId: " + strSimId);

        addPreferencesFromResource(R.xml.callforward_options);

        PreferenceScreen prefSet = getPreferenceScreen();
        mButtonCFU   = (CallForwardEditPreference) prefSet.findPreference(BUTTON_CFU_KEY);
        mButtonCFB   = (CallForwardEditPreference) prefSet.findPreference(BUTTON_CFB_KEY);
        mButtonCFNRy = (CallForwardEditPreference) prefSet.findPreference(BUTTON_CFNRY_KEY);
        mButtonCFNRc = (CallForwardEditPreference) prefSet.findPreference(BUTTON_CFNRC_KEY);
        mButtonDataCFU = (CallForwardEditPreference) prefSet.findPreference(BUTTON_DATA_CFU_KEY);
        mButtonDataCFB   = (CallForwardEditPreference) prefSet.findPreference(BUTTON_DATA_CFB_KEY);
        mButtonDataCFNRy = (CallForwardEditPreference) prefSet.findPreference(BUTTON_DATA_CFNRY_KEY);
        mButtonDataCFNRc = (CallForwardEditPreference) prefSet.findPreference(BUTTON_DATA_CFNRC_KEY);
        
        mButtonCFU.setParentActivity(this, mButtonCFU.reason);
        mButtonCFB.setParentActivity(this, mButtonCFB.reason);
        mButtonCFNRy.setParentActivity(this, mButtonCFNRy.reason);
        mButtonCFNRc.setParentActivity(this, mButtonCFNRc.reason);
        mButtonDataCFU.setParentActivity(this, mButtonDataCFU.reason);
        mButtonDataCFB.setParentActivity(this, mButtonDataCFB.reason);
        mButtonDataCFNRy.setParentActivity(this, mButtonDataCFNRy.reason);
        mButtonDataCFNRc.setParentActivity(this, mButtonDataCFNRc.reason);

        mPreferences.add(mButtonCFU);
        mPreferences.add(mButtonCFB);
        mPreferences.add(mButtonCFNRy);
        mPreferences.add(mButtonCFNRc);
        mPreferences.add(mButtonDataCFU);
        mPreferences.add(mButtonDataCFB);
        mPreferences.add(mButtonDataCFNRy);
        mPreferences.add(mButtonDataCFNRc);        

        // we wait to do the initialization until onResume so that the
        // TimeConsumingPreferenceActivity dialog can display as it
        // relies on onResume / onPause to maintain its foreground state.

        mFirstResume = true;
        mIcicle = icicle;

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mFirstResume) {
            if (mIcicle == null) {
                if (DBG) Log.d(LOG_TAG, "start to init ");
                mPreferences.get(mInitIndex).init(this, false, mSimId);
            } else {
                mInitIndex = mPreferences.size();

                for (CallForwardEditPreference pref : mPreferences) {
                    Bundle bundle = mIcicle.getParcelable(pref.getKey());
                    pref.setToggled(bundle.getBoolean(KEY_TOGGLE));
                    CallForwardInfo cf = new CallForwardInfo();
                    cf.number = bundle.getString(KEY_NUMBER);
                    cf.status = bundle.getInt(KEY_STATUS);
                    pref.handleCallForwardResult(cf);
                    pref.init(this, true, mSimId);
                }
            }
            mFirstResume = false;
            mIcicle=null;
        }

        IntentFilter intentFilter = new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        registerReceiver(mIccCardAbsentReceiver, intentFilter);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        for (CallForwardEditPreference pref : mPreferences) {
            Bundle bundle = new Bundle();
            bundle.putBoolean(KEY_TOGGLE, pref.isToggled());
            if (pref.callForwardInfo != null) {
                bundle.putString(KEY_NUMBER, pref.callForwardInfo.number);
                bundle.putInt(KEY_STATUS, pref.callForwardInfo.status);
            }
            outState.putParcelable(pref.getKey(), bundle);
        }
    }

    @Override
    public void onFinished(Preference preference, boolean reading) {
        if (mInitIndex < mPreferences.size()-1 && !isFinishing()) {
            mInitIndex++;
            mPreferences.get(mInitIndex).init(this, false, mSimId);
        }

        super.onFinished(preference, reading);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (DBG) Log.d(LOG_TAG, "onActivityResult: done");
        if (resultCode != RESULT_OK) {
            if (DBG) Log.d(LOG_TAG, "onActivityResult: contact picker result not OK.");
            return;
        }
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(data.getData(),
                NUM_PROJECTION, null, null, null);
            if ((cursor == null) || (!cursor.moveToFirst())) {
                if (DBG) Log.d(LOG_TAG, "onActivityResult: bad contact data, no results found.");
                return;
            }

            switch (requestCode) {
                case CommandsInterface.CF_REASON_UNCONDITIONAL:
                    mButtonCFU.onPickActivityResult(cursor.getString(0));
                mButtonDataCFU.onPickActivityResult(cursor.getString(0));
                    break;
                case CommandsInterface.CF_REASON_BUSY:
                    mButtonCFB.onPickActivityResult(cursor.getString(0));
                mButtonDataCFB.onPickActivityResult(cursor.getString(0));                
                    break;
                case CommandsInterface.CF_REASON_NO_REPLY:
                    mButtonCFNRy.onPickActivityResult(cursor.getString(0));
                mButtonDataCFNRy.onPickActivityResult(cursor.getString(0));                
                    break;
                case CommandsInterface.CF_REASON_NOT_REACHABLE:
                    mButtonCFNRc.onPickActivityResult(cursor.getString(0));
                mButtonDataCFNRc.onPickActivityResult(cursor.getString(0));                
                    break;
                default:
                    // TODO: may need exception here.
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {  // See ActionBar#setDisplayHomeAsUpEnabled()
            CallFeaturesSetting.goUpToTopLevelSetting(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Receives SIM Absent intent.
     * When a broadcasted intent of SIM absent is received,
     * call setup activity of the relative SIM should be finished.
     */
    BroadcastReceiver mIccCardAbsentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(intent.getAction())) {
                final String iccCardState = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                final SimCardID simCardId = (SimCardID)(intent.getExtra("simId", SimCardID.ID_ZERO));
                if (iccCardState.equals(IccCardConstants.INTENT_VALUE_ICC_ABSENT)
                        && mSimId == simCardId.toInt()) {
                    Log.d(LOG_TAG, "IccCard.MSG_SIM_STATE_ABSENT simCardId = " + simCardId);
                    makeThisFinish();
                }
            }
        }
    };

    /**
     * Finish this activity.
     * This is called when SIM removed.
     */
    private void makeThisFinish() {
        this.finish();
    }

    @Override
    public void onPause() {
        Log.d(LOG_TAG, "onPause");
        super.onPause();
        unregisterReceiver(mIccCardAbsentReceiver);
    }
}
