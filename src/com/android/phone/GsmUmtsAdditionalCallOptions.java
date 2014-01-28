package com.android.phone;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.telephony.SimInfoManager;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.MenuItem;

import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneProxyManager;
import com.android.internal.widget.SubscriptionView;
import com.android.phone.SubPickHandler.SubPickListener;

import java.util.ArrayList;

public class GsmUmtsAdditionalCallOptions extends TimeConsumingPreferenceActivity implements
        PhoneGlobals.SimInfoUpdateListener , SubPickListener {
    private static final String LOG_TAG = "GsmUmtsAdditionalCallOptions";
    private final boolean DBG = (PhoneGlobals.DBG_LEVEL >= 2);

    private static final String BUTTON_CLIR_KEY  = "button_clir_key";
    private static final String BUTTON_CW_KEY    = "button_cw_key";

    private CLIRListPreference mCLIRButton;
    private CallWaitingCheckBoxPreference mCWButton;

    private final ArrayList<Preference> mPreferences = new ArrayList<Preference>();
    private int mInitIndex= 0;
    private long mSubId = SubscriptionManager.SIM_NOT_INSERTED;
    private Bundle mBundle;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.gsm_umts_additional_options);
        mBundle = icicle;

        doSubPick();
    }

    private void doSubPick() {
        if (PhoneUtils.getActivatedSubInfoCount(this) == 1) {
            final long subId = PhoneUtils.getFirstActiveSubInfoRecord(this).mSubId;
            onSubPickComplete(SubPickHandler.SUB_PICK_COMPLETE_KEY_SUB, subId, null);
        } else if (SubscriptionManager.getAllSubInfoCount(this) > 1) {
            final SubPickHandler subPickHandler = new SubPickHandler(this,
                    SubPickHandler.getSubPickItemList(this, false, false));
            subPickHandler.setSubPickListener(this);
            subPickHandler.setSubViewThemeType(SubscriptionView.LIGHT_THEME);
            subPickHandler.showSubPickDialog(getPreferenceScreen().getTitle().toString(), true);
        } else {
            finish();
        }
    }

    private void init() {
        mCLIRButton = (CLIRListPreference) getPreferenceScreen().findPreference(BUTTON_CLIR_KEY);
        mCWButton = (CallWaitingCheckBoxPreference) getPreferenceScreen().findPreference(BUTTON_CW_KEY);
        mPreferences.add(mCLIRButton);
        mPreferences.add(mCWButton);

        if (mBundle == null) {
            if (DBG) Log.d(LOG_TAG, "start to init ");
            mCLIRButton.init(this, false, mSubId);
        } else {
            if (DBG) Log.d(LOG_TAG, "restore stored states");
            mInitIndex = mPreferences.size();
            mCLIRButton.init(this, true, mSubId);
            mCWButton.init(this, true, mSubId);
            int[] clirArray = mBundle.getIntArray(mCLIRButton.getKey());
            if (clirArray != null) {
                if (DBG) Log.d(LOG_TAG, "onCreate:  clirArray[0]="
                        + clirArray[0] + ", clirArray[1]=" + clirArray[1]);
                mCLIRButton.handleGetCLIRResult(clirArray);
            } else {
                mCLIRButton.init(this, false, mSubId);
            }
        }

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        PhoneGlobals.getInstance().addSimInfoUpdateListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mCLIRButton != null && mCLIRButton.clirArray != null) {
            outState.putIntArray(mCLIRButton.getKey(), mCLIRButton.clirArray);
        }
    }

    @Override
    public void onFinished(Preference preference, boolean reading) {
        if (mInitIndex < mPreferences.size()-1 && !isFinishing()) {
            mInitIndex++;
            Preference pref = mPreferences.get(mInitIndex);
            if (pref instanceof CallWaitingCheckBoxPreference) {
                ((CallWaitingCheckBoxPreference) pref).init(this, false, mSubId);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        PhoneGlobals.getInstance().removeSimInfoUpdateListener(this);
    }

    @Override
    public void handleSimInfoUpdate() {
        finish();
    }

    public void onSubPickComplete(int completeKey, long subId, Intent intent) {
        if (DBG) Log.d(LOG_TAG, "onSubPickComplete, completeKey = " +
                completeKey + "; subId = " + subId);
        if (SubPickHandler.SUB_PICK_COMPLETE_KEY_SUB == completeKey) {
            mSubId = subId;
            init();
        } else if (SubPickHandler.SUB_PICK_COMPLETE_KEY_CANCEL == completeKey) {
            finish();
        }
    }
}
