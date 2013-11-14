package com.android.phone;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.RILConstants.SimCardID;

public class MMIAlertActivity extends AlertActivity
        implements DialogInterface.OnClickListener {

    private static final String TAG = "MMIAlertActivity";
    private PhoneGlobals mApp;
    private Phone mPhone;
    private Phone mPhone2;
    EditText inputText;
    int mSimCardId;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Intent intent = getIntent();

        mApp = PhoneGlobals.getInstance();

        final AlertController.AlertParams ap = mAlertParams;
        //ap.mTitle = getString(R.string.mmiResponse_PENDING);
        LayoutInflater inflater = (LayoutInflater)mApp.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = inflater.inflate(R.layout.dialog_ussd_response, null);
        ap.mView = dialogView;
        ap.mMessage = intent.getStringExtra("mmiMessage");
        ap.mPositiveButtonText = getString(R.string.send_button);
        ap.mNegativeButtonText = getString(R.string.cancel);
        ap.mPositiveButtonListener = this;
        ap.mNegativeButtonListener = this;

        // get the input field.
        inputText = (EditText) dialogView.findViewById(R.id.input_field);

        mSimCardId = intent.getIntExtra("simId", 0);

        if (0 == mSimCardId) {
            mPhone = mApp.getPhone(SimCardID.ID_ZERO);
            if (PhoneUtils.isDualMode) mPhone2 = mApp.getPhone(SimCardID.ID_ONE);
        } else if (1 == mSimCardId) {
            mPhone = mApp.getPhone(SimCardID.ID_ONE);
            if (PhoneUtils.isDualMode) mPhone2 = mApp.getPhone(SimCardID.ID_ZERO);
        }

        setupAlert();
    }

    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case AlertDialog.BUTTON_POSITIVE:
                if (null != mPhone) {
                    Log.d(TAG, "onClick(), BUTTON_POSITIVE: send the USSD response");
                    mPhone.sendUssdResponse(inputText.getText().toString());
                }
                break;
            case AlertDialog.BUTTON_NEGATIVE:
                if (null != mPhone && (!PhoneUtils.isDualMode || null != mPhone2)) {
                    Log.d(TAG, "onClick(), BUTTON_NEGATIVE: cancel pending MMI request");
                    Log.d(TAG, "The pending MMIs number for mPhone " + mPhone.getSimCardId().toInt() + " :" + mPhone.getPendingMmiCodes().size());
                    Log.d(TAG, "The pending MMIs number for mPhone " + mPhone2.getSimCardId().toInt() + " :" + mPhone2.getPendingMmiCodes().size());
                    PhoneUtils.cancelMmiCode(mPhone);
                }
                break;
        }
        finish();
    }

    private void onCancel() {
        if (null != mPhone) {
            Log.d(TAG, "onCancel(), press back key to cancel pending MMI request");
            PhoneUtils.cancelMmiCode(mPhone);
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

