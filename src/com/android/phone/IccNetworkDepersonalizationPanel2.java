/*
 * Copyright (C) 2008 The Android Open Source Project
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
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DialerKeyListener;
import android.util.Log;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.telephony.Phone;
import android.app.Activity;

import com.android.internal.telephony.RILConstants.SimCardID;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccCardConstants;

/**
 * "SIM network unlock" PIN entry screen.
 *
 * @see PhoneGlobals.EVENT_SIM_NETWORK_LOCKED
 *
 * TODO: This UI should be part of the lock screen, not the
 * phone app (see bug 1804111).
 */
public class IccNetworkDepersonalizationPanel2 extends Activity {

    //debug constants
    private static final boolean DBG = false;

    //events
    private static final int EVENT_ICC_NTWRK_DEPERSONALIZATION_RESULT = 100;
    private static final int EVENT_ICC2_NTWRK_DEPERSONALIZATION_RESULT = 200;

    private Phone mPhone;
    private Phone mPhone2;

    //UI elements
    private EditText     mPinEntry;
    private LinearLayout mEntryPanel;
    private LinearLayout mStatusPanel;
    private TextView     mStatusText;

    private Button       mUnlockButton;
    private Button       mDismissButton;

    Context mContext;
    private static final String TAG = "IccNetworkDepersonalizationPanel2";

    //private textwatcher to control text entry.
    private TextWatcher mPinEntryWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence buffer, int start, int olen, int nlen) {
        }

        public void onTextChanged(CharSequence buffer, int start, int olen, int nlen) {
        }

        public void afterTextChanged(Editable buffer) {
            if (SpecialCharSequenceMgr.handleChars(
                    mContext, buffer.toString())) {
                mPinEntry.getText().clear();
            }
        }
    };

    //handler for unlock function results
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            int remaining =0;
            if (msg.what == EVENT_ICC_NTWRK_DEPERSONALIZATION_RESULT) {
                AsyncResult res = (AsyncResult) msg.obj;
                if (res.exception != null) {
                    if (DBG) log("sim1 network depersonalization request failure.");
                    remaining = mPhone.getIccCard().getNetworkLockRemainingStatus();
                    indicateError(remaining);
                    if(remaining > 0){
                    postDelayed(new Runnable() {
                                    public void run() {
                                        hideAlert();
                                        mPinEntry.getText().clear();
                                        mPinEntry.requestFocus();
                                    }
                                }, 3000);
                } else {
                        postDelayed(new Runnable() {
                            public void run() {
                                finish();
                            }
                        }, 2000);
                    }
                } else {
                if (DBG) log("sim1 network depersonalization success.");
                    indicateSuccess();
                    postDelayed(new Runnable() {
                        public void run() {
                            finish();
                        }
                    }, 2000);
                }
            } else if (msg.what == EVENT_ICC2_NTWRK_DEPERSONALIZATION_RESULT) {
                AsyncResult res = (AsyncResult) msg.obj;
                if (res.exception != null) {
                    if (DBG) log("sim2 network depersonalization request failure.");
                    remaining = mPhone2.getIccCard().getNetworkLockRemainingStatus();
                    indicateError(remaining);
                    if(remaining > 0){
                        postDelayed(new Runnable() {
                            public void run() {
                                hideAlert();
                                mPinEntry.getText().clear();
                                mPinEntry.requestFocus();
                            }
                        }, 3000);
                    } else {
                        postDelayed(new Runnable() {
                            public void run() {
                                finish();
                            }
                        }, 2000);
                    }
                } else {
                    if (DBG) log("sim2 network depersonalization success.");
                    indicateSuccess();
                    postDelayed(new Runnable() {
                                    public void run() {
                                        finish();
                                    }
                                }, 2000);
                }
            }
        }
    };

    //constructor
    //public IccNetworkDepersonalizationPanel2(Context context) {
    //    super(context);
    //    mContext = context;
    //}

    @Override
    protected void onCreate(Bundle icicle) {
        Log.i(TAG, "onCreate()...  this = " + this);
        super.onCreate(icicle);
        setContentView(R.layout.sim_ndp2);
        //mContext = getContext();

        // PIN entry text field
        mPinEntry = (EditText) findViewById(R.id.pin_entry);
        mPinEntry.setKeyListener(DialerKeyListener.getInstance());
        mPinEntry.setOnClickListener(mEditTextListener);
        mPinEntry.requestFocus();
        mPinEntry.setFocusable(true);
        mPinEntry.setFocusableInTouchMode(true);

        // Attach the textwatcher
        CharSequence text = mPinEntry.getText();
        Spannable span = (Spannable) text;
        span.setSpan(mPinEntryWatcher, 0, text.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        mEntryPanel = (LinearLayout) findViewById(R.id.entry_panel);

        mUnlockButton = (Button) findViewById(R.id.ndp_unlock);
        mUnlockButton.setOnClickListener(mUnlockListener);

        // The "Dismiss" button is present in some (but not all) products,
        // based on the "sim_network_unlock_allow_dismiss" resource.
        mDismissButton = (Button) findViewById(R.id.ndp_dismiss);
        if (getResources().getBoolean(R.bool.sim_network_unlock_allow_dismiss)) {
            if (DBG) log("Enabling 'Dismiss' button...");
            mDismissButton.setVisibility(View.VISIBLE);
            mDismissButton.setOnClickListener(mDismissListener);
        } else {
            if (DBG) log("Removing 'Dismiss' button...");
            mDismissButton.setVisibility(View.GONE);
        }

        //status panel is used since we're having problems with the alert dialog.
        mStatusPanel = (LinearLayout) findViewById(R.id.status_panel);
        mStatusText = (TextView) findViewById(R.id.status_text);

        mPhone = PhoneGlobals.getPhone(SimCardID.ID_ZERO);
        if (PhoneUtils.isDualMode) {
        	mPhone2 = PhoneGlobals.getPhone(SimCardID.ID_ONE);
        } else {
        	mPhone2 = null;
        }
        if (icicle == null) {
            Intent it = getIntent();
            if(it != null && it.hasExtra("simId")) {
                if(SimCardID.ID_ZERO.toInt() == ((SimCardID)(it.getExtra("simId", SimCardID.ID_ZERO))).toInt()){
                    if (mPhone != null && mPhone.getIccCard().isNetworkPukRequired()) {
                        String status = getResources().getString(R.string.NetworkPUKRequired);
                        PhoneGlobals.getInstance().notificationMgr.postTransientNotification(0, status);
                        finish();
                    }
                } else if (SimCardID.ID_ONE.toInt() == ((SimCardID)(it.getExtra("simId", SimCardID.ID_ZERO))).toInt()) {
                    if (mPhone2 != null && mPhone2.getIccCard().isNetworkPukRequired()) {
                        String status = getResources().getString(R.string.NetworkPUKRequired);
                        PhoneGlobals.getInstance().notificationMgr.postTransientNotification(0, status);
                        finish();
                    }
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    //Mirrors IccPinUnlockPanel.onKeyDown().
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        if (DBG) Log.i(TAG, "onResume()...");
        super.onResume();

    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy()...  this = " + this);
        super.onDestroy();
    }


    View.OnClickListener mEditTextListener = new View.OnClickListener() {
        public void onClick(View v) {
        }
    };

    View.OnClickListener mUnlockListener = new View.OnClickListener() {
        public void onClick(View v) {
            String pin = mPinEntry.getText().toString();

            if (TextUtils.isEmpty(pin)) {
                return;
            }

            if (DBG) log("requesting network depersonalization with code " + pin);

            if(mPhone != null){
                if(mPhone.getIccCard().getState() == IccCardConstants.State.NETWORK_LOCKED){
                    if(DBG) Log.d(TAG,"Try to unlock SIM1 network lock");
                    mPhone.getIccCard().supplyNetworkDepersonalization(pin,
                            Message.obtain(mHandler, EVENT_ICC_NTWRK_DEPERSONALIZATION_RESULT));
                }
            }
            if(mPhone2!= null){
                if(mPhone2.getIccCard().getState() == IccCardConstants.State.NETWORK_LOCKED){
                    if(DBG) Log.d(TAG,"Try to unlock SIM2 network lock");
                    mPhone2.getIccCard().supplyNetworkDepersonalization(pin,
                            Message.obtain(mHandler, EVENT_ICC2_NTWRK_DEPERSONALIZATION_RESULT));
                }
            }
            indicateBusy();
        }
    };

    private void indicateBusy() {
        mStatusText.setText(R.string.requesting_unlock);
        mEntryPanel.setVisibility(View.GONE);
        mStatusPanel.setVisibility(View.VISIBLE);
    }

    private void indicateError(int remaincount) {
        if((remaincount >= 0) && (remaincount <=3)){
            String mMsgFormat = getString(R.string.unlock_failed_remaincount);
            mStatusText.setText(String.format(mMsgFormat, remaincount));
            mEntryPanel.setVisibility(View.GONE);
            mStatusPanel.setVisibility(View.VISIBLE);
        } else {
            if (DBG) Log.e(TAG, "indicateError unknown remain count");
        }
    }

    private void indicateSuccess() {
        mStatusText.setText(R.string.unlock_success);
        mEntryPanel.setVisibility(View.GONE);
        mStatusPanel.setVisibility(View.VISIBLE);
    }

    private void hideAlert() {
        mEntryPanel.setVisibility(View.VISIBLE);
        mStatusPanel.setVisibility(View.GONE);
    }

    View.OnClickListener mDismissListener = new View.OnClickListener() {
            public void onClick(View v) {
                if (DBG) log("mDismissListener: skipping depersonalization...");
                finish();
            }
        };

    private void log(String msg) {
        Log.v(TAG, "[IccNetworkDepersonalizationPanel] " + msg);
    }
}
