package com.android.phone;

import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.MenuItem;

import java.util.ArrayList;

import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.RILConstants.SimCardID;
import com.android.internal.telephony.TelephonyIntents;

public class GsmUmtsAdditionalCallOptions extends
        TimeConsumingPreferenceActivity {
    private static final String LOG_TAG = "GsmUmtsAdditionalCallOptions";
    private final boolean DBG = (PhoneGlobals.DBG_LEVEL >= 2);

    private static final String BUTTON_CLIR_KEY  = "button_clir_key";
    private static final String BUTTON_CW_KEY    = "button_cw_key";

    private CLIRListPreference mCLIRButton;
    private CallWaitingCheckBoxPreference mCWButton;

    private final ArrayList<Preference> mPreferences = new ArrayList<Preference>();
    private int mInitIndex= 0;

    private boolean mFirstResume;
    private Bundle mIcicle;
    private int mSimId = SimCardID.ID_ZERO.toInt();

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        String strSimId = getIntent().getDataString();
        try {
            mSimId = Integer.parseInt(strSimId); //strSimId = 0 or 1
            if(mSimId == SimCardID.ID_ONE.toInt())
                addPreferencesFromResource(R.xml.gsm_umts_additional_options2);
            else if(mSimId == SimCardID.ID_ZERO.toInt())
                addPreferencesFromResource(R.xml.gsm_umts_additional_options);
        } catch (NumberFormatException ex) {
            mSimId = SimCardID.ID_ZERO.toInt(); //Set strSimId default as 0
            addPreferencesFromResource(R.xml.gsm_umts_additional_options);
        }
        if (DBG) Log.d(LOG_TAG, "----mPhoneId: " + strSimId);

        PreferenceScreen prefSet = getPreferenceScreen();
        mCLIRButton = (CLIRListPreference) prefSet.findPreference(BUTTON_CLIR_KEY);
        mCWButton = (CallWaitingCheckBoxPreference) prefSet.findPreference(BUTTON_CW_KEY);

        mPreferences.add(mCLIRButton);
        mPreferences.add(mCWButton);

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
                mCLIRButton.init(this, false, mSimId);
            } else {
                if (DBG) Log.d(LOG_TAG, "restore stored states");
                mInitIndex = mPreferences.size();
                mCLIRButton.init(this, true, mSimId);
                mCWButton.init(this, true, mSimId);
                int[] clirArray = mIcicle.getIntArray(mCLIRButton.getKey());
                if (clirArray != null) {
                    if (DBG) Log.d(LOG_TAG, "onCreate:  clirArray[0]="
                            + clirArray[0] + ", clirArray[1]=" + clirArray[1]);
                    mCLIRButton.handleGetCLIRResult(clirArray);
                } else {
                    mCLIRButton.init(this, false, mSimId);
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

        if (mCLIRButton.clirArray != null) {
            outState.putIntArray(mCLIRButton.getKey(), mCLIRButton.clirArray);
        }
    }

    @Override
    public void onFinished(Preference preference, boolean reading) {
        if (mInitIndex < mPreferences.size()-1 && !isFinishing()) {
            mInitIndex++;
            Preference pref = mPreferences.get(mInitIndex);
            if (pref instanceof CallWaitingCheckBoxPreference) {
                ((CallWaitingCheckBoxPreference) pref).init(this, false, mSimId);
            }
        }
        super.onFinished(preference, reading);
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
