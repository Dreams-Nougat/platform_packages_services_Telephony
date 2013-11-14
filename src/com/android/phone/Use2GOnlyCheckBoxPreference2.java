/*
 * Copyright (C) 2009 The Android Open Source Project
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
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;
import android.util.Log;

import com.android.internal.telephony.Phone;
/* dual sim */
import com.android.internal.telephony.RILConstants.SimCardID;

public class Use2GOnlyCheckBoxPreference2 extends CheckBoxPreference {
    private static final String LOG_TAG = "Use2GOnlyCheckBoxPreference2";
    private static final boolean DBG = true;

    private Phone mPhone2;
    private MyHandler mHandler;
    private Callback mCallback;
    private int mNetworkType;

    public Use2GOnlyCheckBoxPreference2(Context context) {
        this(context, null);
    }

    public Use2GOnlyCheckBoxPreference2(Context context, AttributeSet attrs) {
        this(context, attrs,com.android.internal.R.attr.checkBoxPreferenceStyle);
    }

    public Use2GOnlyCheckBoxPreference2(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mPhone2 = PhoneGlobals.getPhone(SimCardID.ID_ONE);
        mHandler = new MyHandler();
        mHandler.postDelayed(new Runnable() {
            public void run() {
                mPhone2.getPreferredNetworkType(
                    mHandler.obtainMessage(MyHandler.MESSAGE_GET_PREFERRED_NETWORK_TYPE));
            }
        }, 50);
    }

    public void setNetworkTypeEnabled(boolean enabled) {
        setEnabled(enabled);
    }

    public void setNetworkTypeCallback(Callback callback) {
        mCallback = callback;
    }

    interface Callback {
        void onUpdateNetworkType(int type);
    }

    @Override
    protected void  onClick() {
        super.onClick();

        mNetworkType = isChecked() ? Phone.NT_MODE_GSM_ONLY : Phone.NT_MODE_WCDMA_PREF;
        Log.i(LOG_TAG, "set preferred network type="+mNetworkType);
        android.provider.Settings.Secure.putInt(mPhone2.getContext().getContentResolver(),
                android.provider.Settings.Global.PREFERRED_NETWORK_MODE, mNetworkType);
        mPhone2.setPreferredNetworkType(mNetworkType, mHandler
                .obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE));
   }

    private class MyHandler extends Handler {

        private static final int MESSAGE_GET_PREFERRED_NETWORK_TYPE = 0;
        private static final int MESSAGE_SET_PREFERRED_NETWORK_TYPE = 1;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_GET_PREFERRED_NETWORK_TYPE:
                    handleGetPreferredNetworkTypeResponse(msg);
                    break;

                case MESSAGE_SET_PREFERRED_NETWORK_TYPE:
                    handleSetPreferredNetworkTypeResponse(msg);
                    break;
            }
        }

        private void handleGetPreferredNetworkTypeResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;

            if (ar.exception == null) {
                int type = ((int[])ar.result)[0];
                if (type != Phone.NT_MODE_GSM_ONLY) {
                    // Allow only NT_MODE_GSM_ONLY or NT_MODE_WCDMA_PREF
                    type = Phone.NT_MODE_WCDMA_PREF;
                }
                Log.i(LOG_TAG, "get preferred network type="+type);
                setChecked(type == Phone.NT_MODE_GSM_ONLY);
                android.provider.Settings.Secure.putInt(mPhone2.getContext().getContentResolver(),
                        android.provider.Settings.Global.PREFERRED_NETWORK_MODE, type);
                if (null != mCallback) {
                    mCallback.onUpdateNetworkType(type);
                }
            } else {
                // Weird state, disable the setting
                Log.e(LOG_TAG, "get preferred network type, exception="+ar.exception);
                setEnabled(false);
                if (null != mCallback) {
                    mCallback.onUpdateNetworkType(-1);
                }
            }
        }

        private void handleSetPreferredNetworkTypeResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;

            if (ar.exception != null) {
                // Yikes, error, disable the setting
                setEnabled(false);
                // Set UI to current state
                Log.e(LOG_TAG, "set preferred network type, exception=" + ar.exception);
                mPhone2.getPreferredNetworkType(obtainMessage(MESSAGE_GET_PREFERRED_NETWORK_TYPE));
            } else {
                Log.i(LOG_TAG, "set preferred network type done");
                if (null != mCallback) {
                    mCallback.onUpdateNetworkType(mNetworkType);
                }
            }
        }
    }
}
