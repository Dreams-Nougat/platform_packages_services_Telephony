package com.android.phone;

import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.EditTextPreference;
import android.text.InputFilter;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class EditSimNamePreference extends EditTextPreference {
    private static final String LOG_TAG = "SimNameSetting";
    private final boolean DBG = (PhoneGlobals.DBG_LEVEL >= 2);
    private static final int MAX_SIM_NAME_LEN = 25;

    interface OnSimNameEnteredListener {
        void onSimNameEntered(EditSimNamePreference preference, boolean positiveResult);
    }

    private OnSimNameEnteredListener mSimNameListener;

    public void setOnSimNameEnteredListener(OnSimNameEnteredListener listener) {
        mSimNameListener = listener;
    }

    public EditSimNamePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditSimNamePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Overridden to setup the correct dialog layout, as well as setting up
     * other properties for the sim name entry field.
     */
    @Override
    protected View onCreateDialogView() {
        // set the dialog layout
        setDialogLayoutResource(R.layout.edit_sim_select_name);

        View dialog = super.onCreateDialogView();

        // limit the length of sim name
        final EditText textfield = getEditText();
        InputFilter[] FilterArray = new InputFilter[1];
        FilterArray[0] = new InputFilter.LengthFilter(MAX_SIM_NAME_LEN);
        textfield.setFilters(FilterArray);

        return dialog;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (mSimNameListener != null) {
            mSimNameListener.onSimNameEntered(this, positiveResult);
        }
    }
}
