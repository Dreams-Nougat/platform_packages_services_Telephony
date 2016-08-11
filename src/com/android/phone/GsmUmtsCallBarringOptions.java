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

import android.app.ActionBar;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.PhoneFactory;
import com.android.phone.settings.fdn.EditPinPreference;

import java.util.ArrayList;

public class GsmUmtsCallBarringOptions extends TimeConsumingPreferenceActivity
        implements EditPinPreference.OnPinEnteredListener {
    private static final String LOG_TAG = "GsmUmtsCallBarringOptions";
    private static final boolean DBG = (PhoneGlobals.DBG_LEVEL >= 2);

    // String keys for preference lookup
    // Preference is handled solely in xml.
    private static final String BUTTON_BAOC_KEY   = "button_baoc_key";
    private static final String BUTTON_BAOIC_KEY   = "button_baoic_key";
    private static final String BUTTON_BAOICxH_KEY = "button_baoicxh_key";
    private static final String BUTTON_BAIC_KEY = "button_baic_key";
    private static final String BUTTON_BAICr_KEY = "button_baicr_key";
    private static final String BUTTON_BA_ALL_KEY = "button_ba_all_key";
    private static final String BUTTON_BA_CHANGE_PW_KEY = "button_change_pw_key";
    private static final String PW_CHANGE_STATE_KEY = "pin_change_state_key";
    private static final String OLD_PW_KEY = "old_pw_key";
    private static final String NEW_PW_KEY = "new_pw_key";
    private static final String DIALOG_MESSAGE_KEY = "dialog_message_key";
    private static final String DIALOG_PW_ENTRY_KEY = "dialog_pw_enter_key";
    private static final String KEY_STATUS = "toggle";

    private CallBarringEditPreference mButtonBAOC;
    private CallBarringEditPreference mButtonBAOIC;
    private CallBarringEditPreference mButtonBAOICxH;
    private CallBarringEditPreference mButtonBAIC;
    private CallBarringEditPreference mButtonBAICr;
    private EditPinPreference mButtonDeaAll;
    private EditPinPreference mButtonChangePW;

    // State variables
    private int mPwChangeState;
    private String mOldPassword;
    private String mNewPassword;

    private static final int PW_CHANGE_OLD = 0;
    private static final int PW_CHANGE_NEW = 1;
    private static final int PW_CHANGE_REENTER = 2;

    /**
     * Events we handle.
     * The first is used for toggling PW change, the second for the call barring disable.
     */
    private static final int EVENT_PW_CHANGE_COMPLETE = 100;
    private static final int EVENT_DEACT_ALL_COMPLETE = 200;

    // size limits for the password.
    private static final int PW_LENGTH = 4;
    private Phone mPhone;

    private ArrayList<CallBarringEditPreference> mPreferences =
            new ArrayList<CallBarringEditPreference> ();
    private int mInitIndex= 0;
    private boolean mFirstResume;
    private Bundle mIcicle;

    private SubscriptionInfoHelper mSubscriptionInfoHelper;

    /**
     * Delegates to the respective handlers.
     *
     * @param preference The edit pin preference.
     * @param positiveResult If true to change the password or deactivate all requests,
     *                       false to reset the password or do nothing.
     */
    public void onPinEntered(EditPinPreference preference, boolean positiveResult) {
        if (preference == mButtonChangePW) {
            updatePWChangeState(positiveResult);
        } else if (preference == mButtonDeaAll) {
            deactivateAllBarring(positiveResult);
        }
    }

    /**
     * Display a toast for message, like the rest of the settings.
     */
    private final void displayMessage(int strId) {
        Toast.makeText(this, getString(strId), Toast.LENGTH_SHORT).show();
    }

    /**
     * Attempt to deactivate all for call barring settings.
     */
    private void deactivateAllBarring(boolean positiveResult) {
        if (!positiveResult) {
            // return on cancel
            return;
        }
        String password = mButtonDeaAll.getText();
        // validate the length of password first, before submitting it to the
        // RIL for CB disable.
        if (password == null || password.length() != PW_LENGTH) {
            mButtonDeaAll.setText("");
            displayMessage(R.string.call_barring_right_pwd_number);
            return;
        }
        // submit the deactivate all request
        mButtonDeaAll.setText("");
        Message onComplete = myHandler.obtainMessage(EVENT_DEACT_ALL_COMPLETE);
        ((GsmCdmaPhone) mPhone).setCallBarringOption(CommandsInterface.CB_FACILITY_BA_ALL,
                false, password, 0, onComplete);
        this.onStarted(mButtonDeaAll, false);
    }

    /**
     * Attempt to change the password for call barring settings.
     */
    private void updatePWChangeState(boolean positiveResult) {
        if (!positiveResult) {
            // reset the state on cancel
            resetPwChangeState();
            return;
        }

        // Progress through the dialog states, generally in this order:
        // 1. Enter old password
        // 2. Enter new password
        // 3. Re-Enter new password
        // In general, if any invalid entries are made, the dialog re-
        // appears with text to indicate what the issue is.
        switch (mPwChangeState) {
            case PW_CHANGE_OLD:
                mOldPassword = mButtonChangePW.getText();
                mButtonChangePW.setText("");
                if (validatePassword(mOldPassword)) {
                    mPwChangeState = PW_CHANGE_NEW;
                    displayPwChangeDialog();
                } else {
                    displayPwChangeDialog(R.string.call_barring_right_pwd_number, true);
                }
                break;
            case PW_CHANGE_NEW:
                mNewPassword = mButtonChangePW.getText();
                mButtonChangePW.setText("");
                if (validatePassword(mNewPassword)) {
                    mPwChangeState = PW_CHANGE_REENTER;
                    displayPwChangeDialog();
                } else {
                    displayPwChangeDialog(R.string.call_barring_right_pwd_number, true);
                }
                break;
            case PW_CHANGE_REENTER:
                // if the re-entered password is not valid, display a message
                // and reset the state.
                if (!mNewPassword.equals(mButtonChangePW.getText())) {
                    mPwChangeState = PW_CHANGE_NEW;
                    mButtonChangePW.setText("");
                    displayPwChangeDialog(R.string.call_barring_pwd_not_match, true);
                } else {
                    // If the password is valid, then submit the change password
                    // request
                    mButtonChangePW.setText("");
                    Message onComplete = myHandler.obtainMessage(EVENT_PW_CHANGE_COMPLETE);
                    ((GsmCdmaPhone) mPhone).changeCallBarringPassword(
                            CommandsInterface.CB_FACILITY_BA_ALL,
                            mOldPassword, mNewPassword, onComplete);
                    this.onStarted(mButtonChangePW, false);
                }
                break;
        }
    }

    /**
     * Handler for asynchronous replies from the framework layer.
     */
    private Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                // handle the response message for password change from the framework layer.
                case EVENT_PW_CHANGE_COMPLETE: {
                    onFinished(mButtonChangePW, false);
                    AsyncResult ar = (AsyncResult)msg.obj;
                    // Unsuccessful change, display a toast to user with failure reason.
                    if (ar.exception != null) {
                        if (DBG)
                            Log.d(LOG_TAG,
                                    "change password for call barring failed with exception: "
                                            + ar.exception);
                        onException(mButtonChangePW, (CommandException)ar.exception);
                        mButtonChangePW.setEnabled(true);
                    } else if (ar.userObj instanceof Throwable) {
                        onError(mButtonChangePW, RESPONSE_ERROR);
                    } else {
                        // Successful change.
                        displayMessage(R.string.call_barring_change_pwd_success);
                    }
                    resetPwChangeState();
                }
                    break;

                // when we are disabling all call barring, either we are
                // unsuccessful and display a toast, or just update the UI.
                case EVENT_DEACT_ALL_COMPLETE: {
                    onFinished(mButtonDeaAll, false);
                    AsyncResult ar = (AsyncResult)msg.obj;
                    if (ar.exception != null) {
                        if (DBG)
                            Log.d(LOG_TAG, "can not disable all call barring with exception: "
                                    + ar.exception);
                        onException(mButtonDeaAll, (CommandException)ar.exception);
                        mButtonDeaAll.setEnabled(true);
                    } else if (ar.userObj instanceof Throwable) {
                        onError(mButtonDeaAll, RESPONSE_ERROR);
                    } else {
                        // reset to normal behaviour on successful change.
                        displayMessage(R.string.call_barring_deactivate_success);
                        resetCallBarringPrefState(false);
                    }
                }
                    break;
                default:
                    if (DBG)
                        Log.d(LOG_TAG, "Unknown message id: " + msg.what);
                    break;
            }
        }
    };

    /**
     * The next two functions are for updating the message field on the dialog.
     */
    private final void displayPwChangeDialog() {
        displayPwChangeDialog(0, true);
    }

    private final void displayPwChangeDialog(int strId, boolean shouldDisplay) {
        int msgId = 0;
        switch (mPwChangeState) {
            case PW_CHANGE_OLD:
                msgId = R.string.call_barring_old_pwd;
                break;
            case PW_CHANGE_NEW:
                msgId = R.string.call_barring_new_pwd;
                break;
            case PW_CHANGE_REENTER:
                msgId = R.string.call_barring_confirm_pwd;
                break;
            default:
                break;
        }

        // append the note / additional message, if needed.
        if (strId != 0) {
            mButtonChangePW.setDialogMessage(getText(msgId) + "\n" + getText(strId));
        } else {
            mButtonChangePW.setDialogMessage(msgId);
        }

        // only display if requested.
        if (shouldDisplay) {
            mButtonChangePW.showPinDialog();
        }
    }

    /**
     * Reset the state of the password change dialog.
     */
    private final void resetPwChangeState() {
        mPwChangeState = PW_CHANGE_OLD;
        displayPwChangeDialog(0, false);
        mOldPassword = "";
        mNewPassword = "";
    }

    /**
     * Reset the state of the all call barring setting to disable.
     */
    private final void resetCallBarringPrefState(boolean bEnable) {
        for (CallBarringEditPreference pref : mPreferences) {
            pref.isActivated = bEnable;
            pref.updateSummaryText();
        }
    }

    /**
     * Validate the password entry.
     *
     * @param password This is the password to validate
     */
    private boolean validatePassword(String password) {
        // check validity
        if (password == null || password.length() != PW_LENGTH) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (DBG)
            Log.d(LOG_TAG, "onCreate, reading callbarring_options.xml file");
        addPreferencesFromResource(R.xml.callbarring_options);

        mSubscriptionInfoHelper = new SubscriptionInfoHelper(this, getIntent());
        mPhone = mSubscriptionInfoHelper.getPhone();
        if (DBG)
            Log.d(LOG_TAG, "onCreate, reading callbarring_options.xml file finished!");

        // get UI object references
        PreferenceScreen prefSet = getPreferenceScreen();
        mButtonBAOC = (CallBarringEditPreference)prefSet.findPreference(BUTTON_BAOC_KEY);
        mButtonBAOIC = (CallBarringEditPreference)prefSet.findPreference(BUTTON_BAOIC_KEY);
        mButtonBAOICxH = (CallBarringEditPreference)prefSet.findPreference(BUTTON_BAOICxH_KEY);
        mButtonBAIC = (CallBarringEditPreference)prefSet.findPreference(BUTTON_BAIC_KEY);
        mButtonBAICr = (CallBarringEditPreference)prefSet.findPreference(BUTTON_BAICr_KEY);
        mButtonDeaAll = (EditPinPreference)prefSet.findPreference(BUTTON_BA_ALL_KEY);
        mButtonChangePW = (EditPinPreference)prefSet.findPreference(BUTTON_BA_CHANGE_PW_KEY);

        // assign click listener and update state
        mButtonBAOC.setOnPinEnteredListener(this);
        mButtonBAOIC.setOnPinEnteredListener(this);
        mButtonBAOICxH.setOnPinEnteredListener(this);
        mButtonBAIC.setOnPinEnteredListener(this);
        mButtonBAICr.setOnPinEnteredListener(this);
        mButtonDeaAll.setOnPinEnteredListener(this);
        mButtonChangePW.setOnPinEnteredListener(this);

        // store CallBarringEditPreferencence objects in array list.
        mPreferences.add(mButtonBAOC);
        mPreferences.add(mButtonBAOIC);
        mPreferences.add(mButtonBAOICxH);
        mPreferences.add(mButtonBAIC);
        mPreferences.add(mButtonBAICr);

        // unavailable when sim card is not ready.
        if (TelephonyManager.getDefault().getSimState(mPhone.getPhoneId())
                == TelephonyManager.SIM_STATE_READY) {
            mButtonDeaAll.setEnabled(true);
            mButtonChangePW.setEnabled(true);
        } else {
            mButtonDeaAll.setEnabled(false);
            mButtonChangePW.setEnabled(false);
        }

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
                if (DBG)
                    Log.d(LOG_TAG, "start to init ");
                resetPwChangeState();
                mPreferences.get(mInitIndex).init(this, false, mPhone);
            } else {
                if (DBG)
                    Log.d(LOG_TAG, "restore stored states");
                mInitIndex = mPreferences.size();

                for (CallBarringEditPreference pref : mPreferences) {
                    Bundle bundle = mIcicle.getParcelable(pref.getKey());
                    pref.handleCallBarringResult(bundle.getBoolean(KEY_STATUS));
                    pref.init(this, true /* skip querying */, mPhone);
                }
                mPwChangeState = mIcicle.getInt(PW_CHANGE_STATE_KEY);
                mOldPassword = mIcicle.getString(OLD_PW_KEY);
                mNewPassword = mIcicle.getString(NEW_PW_KEY);
                mButtonChangePW.setDialogMessage(mIcicle.getString(DIALOG_MESSAGE_KEY));
                mButtonChangePW.setText(mIcicle.getString(DIALOG_PW_ENTRY_KEY));
            }
            mFirstResume = false;
            mIcicle = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        for (CallBarringEditPreference pref : mPreferences) {
            Bundle bundle = new Bundle();
            bundle.putBoolean(KEY_STATUS, pref.isActivated);
            outState.putParcelable(pref.getKey(), bundle);
        }
        outState.putInt(PW_CHANGE_STATE_KEY, mPwChangeState);
        outState.putString(OLD_PW_KEY, mOldPassword);
        outState.putString(NEW_PW_KEY, mNewPassword);
        outState.putString(DIALOG_MESSAGE_KEY, mButtonChangePW.getDialogMessage().toString());
        outState.putString(DIALOG_PW_ENTRY_KEY, mButtonChangePW.getText());
    }

    /**
     * Finish initialization of this preference and start next.
     *
     * @param preference The preference.
     * @param reading If true to dismiss the busy reading dialog,
     *                false to dismiss the busy saving dialog.
     */
    public void onFinished(Preference preference, boolean reading) {
        if (mInitIndex < mPreferences.size() - 1 && !isFinishing()) {
            mInitIndex++;
            mPreferences.get(mInitIndex).init(this, false, mPhone);
        }
        super.onFinished(preference, reading);
    }

    /**
     * This hook is called whenever an item in your options menu is selected.
     *
     * @param item The selected menu item.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {  // See ActionBar#setDisplayHomeAsUpEnabled()
            CallFeaturesSetting.goUpToTopLevelSetting(this, mSubscriptionInfoHelper);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

