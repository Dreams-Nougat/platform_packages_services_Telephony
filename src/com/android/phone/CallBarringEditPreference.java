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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.text.method.DigitsKeyListener;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.phone.settings.fdn.EditPinPreference;

import static com.android.phone.TimeConsumingPreferenceActivity.RESPONSE_ERROR;

public class CallBarringEditPreference extends EditPinPreference {
    private static final String LOG_TAG = "CallBarringEditPreference";
    private static final boolean DBG = (PhoneGlobals.DBG_LEVEL >= 2);

    private String mFacility;
    boolean isActivated = false;
    // private variables
    private CharSequence mEnableText;
    private CharSequence mDisableText;
    private CharSequence mSummaryOn;
    private CharSequence mSummaryOff;
    private int mButtonClicked;
    private String mPassword;
    private MyHandler mHandler = new MyHandler();
    private Phone mPhone;
    private TimeConsumingPreferenceListener mTcpListener;
    private Context mContext;
    // size limits for the password.
    private static final int PW_LENGTH = 4;

    /**
     * CallBarringEditPreference constructor.
     *
     * @param context The context of view.
     * @param attrs The attributes of the XML tag that is inflating EditTextPreference.
     */
    public CallBarringEditPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        // get the summary settings, use CheckBoxPreference as the standard.
        TypedArray typedArray = context.obtainStyledAttributes(attrs,
                android.R.styleable.CheckBoxPreference, 0, 0);
        mSummaryOn = typedArray.getString(android.R.styleable.CheckBoxPreference_summaryOn);
        mSummaryOff = typedArray.getString(android.R.styleable.CheckBoxPreference_summaryOff);
        mDisableText = context.getText(R.string.disable);
        mEnableText = context.getText(R.string.enable);
        typedArray.recycle();

        // get default phone
        mPhone = PhoneFactory.getDefaultPhone();

        typedArray = context.obtainStyledAttributes(attrs,
                R.styleable.CallBarringEditPreference, 0, R.style.EditPhoneNumberPreference);
        mFacility = typedArray.getString(R.styleable.CallBarringEditPreference_facility);
        typedArray.recycle();
    }

    /**
     * CallBarringEditPreference constructor.
     *
     * @param context The context of view.
     */
    public CallBarringEditPreference(Context context) {
        this(context, null);
    }

    void init(TimeConsumingPreferenceListener listener, boolean skipReading, Phone phone) {
        // Getting selected phone id
        if (DBG) {
            Log.d(LOG_TAG, "Getting callBarringEditPreference phone id = " + phone.getPhoneId());
        }
        mPhone = phone;

        mTcpListener = listener;
        if (!skipReading) {
            // query call barring status
            ((GsmCdmaPhone) mPhone).getCallBarringOption(mFacility, "", 0, mHandler.obtainMessage(
                    MyHandler.MESSAGE_GET_CALL_BARRING));
            if (mTcpListener != null) {
                mTcpListener.onStarted(this, true);
            }
        }
    }

    /**
     * This method will be invoked when a button in the dialog is clicked.
     *
     * @param dialog The dialog that received the click.
     * @param which The button that was clicked or the position of the item clicked.
     */
    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);
        mButtonClicked = which;
    }

    /*
     * Methods called on UI bindings
     */
    @Override
    // called when we're binding the view to the preference.
    protected void onBindView(View view) {
        super.onBindView(view);

        // Sync the summary view
        TextView summaryView = (TextView)view.findViewById(android.R.id.summary);
        if (summaryView != null) {
            CharSequence sum;
            int vis;

            // set summary depending upon mode
            if (isActivated) {
                sum = (mSummaryOn == null) ? getSummary() : mSummaryOn;
            } else {
                sum = (mSummaryOff == null) ? getSummary() : mSummaryOff;
            }

            if (sum != null) {
                summaryView.setText(sum);
                vis = View.VISIBLE;
            } else {
                vis = View.GONE;
            }

            if (vis != summaryView.getVisibility()) {
                summaryView.setVisibility(vis);
            }
        }
    }

    // control the appearance of the dialog depending upon the mode.
    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        builder.setPositiveButton(null, null);
        if (isActivated) {
            builder.setNeutralButton(mDisableText, this);
        } else {
            builder.setNeutralButton(mEnableText, this);
        }
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        // default the button clicked to be the cancel button.
        mButtonClicked = DialogInterface.BUTTON_NEGATIVE;

        final EditText editText = (EditText)view.findViewById(android.R.id.edit);
        if (editText != null) {
            editText.setSingleLine(true);
            editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            editText.setKeyListener(DigitsKeyListener.getInstance());
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (DBG)
            Log.d(LOG_TAG, "mButtonClicked=" + mButtonClicked + ", positiveResult="
                    + positiveResult);
        if (this.mButtonClicked != DialogInterface.BUTTON_NEGATIVE) {
            mPassword = getEditText().getText().toString();

            // check if the password is valid.
            if (mPassword == null || mPassword.length() != PW_LENGTH) {
                Toast.makeText(mContext, mContext.getString(R.string.call_barring_right_pwd_number),
                                Toast.LENGTH_SHORT).show();
                return;
            }

            if (DBG)
                Log.d(LOG_TAG, "onDialogClosed: mPassword=" + mPassword);
            // send set call barring message to RIL layer.
            ((GsmCdmaPhone) mPhone).setCallBarringOption(mFacility, !isActivated, mPassword, 0,
                    mHandler.obtainMessage(MyHandler.MESSAGE_SET_CALL_BARRING));
            if (mTcpListener != null) {
                mTcpListener.onStarted(this, false);
            }
        }
    }

    void handleCallBarringResult(boolean status) {
        isActivated = status;
        if (DBG)
            Log.d(LOG_TAG, "handleGetCBResponse done, isActivated=" + status);
    }

    void updateSummaryText() {
        notifyChanged();
        notifyDependencyChange(shouldDisableDependents());
    }

    /* Decides how to disable dependents.
     *
     * @see android.preference.EditTextPreference#shouldDisableDependents()
     */
    @Override
    public boolean shouldDisableDependents() {
        // update dependents
        boolean shouldDisable = isActivated;
        return shouldDisable;
    }

    // Message protocol:
    // what: get vs. set
    // arg1: action -- register vs. disable
    // arg2: get vs. set for the preceding request
    @SuppressLint("HandlerLeak")
    private class MyHandler extends Handler {
        private static final int MESSAGE_GET_CALL_BARRING = 0;
        private static final int MESSAGE_SET_CALL_BARRING = 1;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_GET_CALL_BARRING:
                    handleGetCallBarringResponse(msg);
                    break;
                case MESSAGE_SET_CALL_BARRING:
                    handleSetCallBarringResponse(msg);
                    break;
                default:
                    break;
            }
        }

        // handle the response message for query CB status.
        private void handleGetCallBarringResponse(Message msg) {
            if (DBG)
                Log.d(LOG_TAG, "handleGetCallBarringResponse: done");
            AsyncResult ar = (AsyncResult)msg.obj;

            if (msg.arg2 == MESSAGE_SET_CALL_BARRING) {
                mTcpListener.onFinished(CallBarringEditPreference.this, false);
            } else {
                mTcpListener.onFinished(CallBarringEditPreference.this, true);
            }

            // unsuccessful query for call barring.
            if (ar.exception != null) {
                if (DBG)
                    Log.d(LOG_TAG, "handleGetCallBarringResponse: ar.exception=" + ar.exception);
                mTcpListener.onException(CallBarringEditPreference.this,
                        (CommandException)ar.exception);
            } else {
                if (ar.userObj instanceof Throwable) {
                    mTcpListener.onError(CallBarringEditPreference.this, RESPONSE_ERROR);
                }
                int[] ints = (int[])ar.result;
                if (ints.length == 0) {
                    if (DBG)
                        Log.d(LOG_TAG, "handleGetCallBarringResponse: ar.result.length==0");
                    setEnabled(false);
                    mTcpListener.onError(CallBarringEditPreference.this, RESPONSE_ERROR);
                } else {
                    handleCallBarringResult(ints[0] != 0);
                    if (DBG)
                        Log.d(LOG_TAG,
                                "handleGetCallBarringResponse: CB state successfully queried: "
                                        + ints[0]);
                }
            }
            // update call barring status.
            updateSummaryText();
        }

        // handle the response message for CB settings.
        private void handleSetCallBarringResponse(Message msg) {
            AsyncResult ar = (AsyncResult)msg.obj;

            if (ar.exception != null || ar.userObj instanceof Throwable) {
                if (DBG)
                    Log.d(LOG_TAG, "handleSetCallBarringResponse: ar.exception=" + ar.exception);
            }
            if (DBG)
                Log.d(LOG_TAG, "handleSetCallBarringResponse: re get");
            ((GsmCdmaPhone) mPhone).getCallBarringOption(
                    mFacility,
                    "",
                    0,
                    obtainMessage(MESSAGE_GET_CALL_BARRING, 0, MESSAGE_SET_CALL_BARRING,
                            ar.exception));
        }
    }
}

