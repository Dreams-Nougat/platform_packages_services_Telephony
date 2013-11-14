/*
 * Copyright (C) 2007 The Android Open Source Project
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

import static android.view.Window.PROGRESS_VISIBILITY_OFF;
import static android.view.Window.PROGRESS_VISIBILITY_ON;

import android.app.ListActivity;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Window;
import android.widget.CursorAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.android.internal.telephony.RILConstants.SimCardID;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.IccProvider;
import com.android.internal.telephony.TelephonyIntents;

/**
 * ADN List activity for the Phone app.
 */
public class ADNList extends ListActivity {
    protected static final String TAG = "ADNList";
    protected static final boolean DBG = false;

    private static final String[] COLUMN_NAMES = new String[] {
        IccProvider.ICC_TAG,IccProvider.ICC_NUMBER,IccProvider.ICC_EMAILS,IccProvider.ICC_ANRS,IccProvider.ICC_INDEX
        /*
        "name",
        "number",
        "emails",
        //<3g pb>
        "anrs",
        "index"
        //<end>
        */
    };

    protected static final int NAME_COLUMN = 0;
    protected static final int NUMBER_COLUMN = 1;
    protected static final int EMAILS_COLUMN = 2;
    //<3g pb>
    protected static final int ANR_COLUMN = 3;
    protected static final int INDEX_COLUMN = 4;
    //<end>

    private static final int[] VIEW_NAMES = new int[] {
        android.R.id.text1,
        android.R.id.text2
    };

    protected static final int QUERY_TOKEN = 0;
    protected static final int INSERT_TOKEN = 1;
    protected static final int UPDATE_TOKEN = 2;
    protected static final int DELETE_TOKEN = 3;


    protected QueryHandler mQueryHandler;
    protected CursorAdapter mCursorAdapter;
    protected Cursor mCursor = null;

    private TextView mEmptyText;

    protected int mInitialSelection = -1;

    private SimCardID mActivePhoneId = SimCardID.ID_ZERO;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.adn_list);
        mEmptyText = (TextView) findViewById(android.R.id.empty);
        mQueryHandler = new QueryHandler(getContentResolver());
    }

    @Override
    protected void onResume() {
        super.onResume();
        query();

        IntentFilter intentFilter = new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        registerReceiver(mIccCardAbsentReceiver, intentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mCursor != null) {
            mCursor.deactivate();
        }
    }

    protected Uri resolveIntent() {
        Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(Uri.parse("content://icc/adn"));
        }

        return intent.getData();
    }

    private void query() {
        Uri uri = resolveIntent();
        if (DBG) log("query: starting an async query");

        String authority = uri.getAuthority();
        if (authority.equals("icc")) {
            mActivePhoneId = SimCardID.ID_ZERO;
        }else if (authority.equals("icc2")){
            mActivePhoneId = SimCardID.ID_ONE;
        }
        
        mQueryHandler.startQuery(QUERY_TOKEN, null, uri, COLUMN_NAMES,
                null, null, null);
        displayProgress(true);
    }

    private void reQuery() {
        query();
    }

    private void setAdapter() {
        // NOTE:
        // As it it written, the positioning code below is NOT working.
        // However, this current non-working state is in compliance with
        // the UI paradigm, so we can't really do much to change it.

        // In the future, if we wish to get this "positioning" correct,
        // we'll need to do the following:
        //   1. Change the layout to in the cursor adapter to:
        //     android.R.layout.simple_list_item_checked
        //   2. replace the selection / focus code with:
        //     getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        //     getListView().setItemChecked(mInitialSelection, true);

        // Since the positioning is really only useful for the dialer's
        // SpecialCharSequence case (dialing '2#' to get to the 2nd
        // contact for instance), it doesn't make sense to mess with
        // the usability of the activity just for this case.

        // These artifacts include:
        //  1. UI artifacts (checkbox and highlight at the same time)
        //  2. Allowing the user to edit / create new SIM contacts when
        //    the user is simply trying to retrieve a number into the d
        //    dialer.

        if (mCursorAdapter == null) {
            mCursorAdapter = newAdapter();

            setListAdapter(mCursorAdapter);
        } else {
            mCursorAdapter.changeCursor(mCursor);
        }

        if (mInitialSelection >=0 && mInitialSelection < mCursorAdapter.getCount()) {
            setSelection(mInitialSelection);
            getListView().setFocusableInTouchMode(true);
            boolean gotfocus = getListView().requestFocus();
        }
    }

    protected CursorAdapter newAdapter() {
        return new SimpleCursorAdapter(this,
                    android.R.layout.simple_list_item_2,
                    mCursor, COLUMN_NAMES, VIEW_NAMES);
    }

    private void displayProgress(boolean loading) {
        if (DBG) log("displayProgress: " + loading);

        //mEmptyText.setText(loading ? R.string.simContacts_emptyLoading:
        //    (isAirplaneModeOn(this) ? R.string.simContacts_airplaneMode :
        //        R.string.simContacts_empty));
        if (loading){
            mEmptyText.setText(R.string.simContacts_emptyLoading);
        }else{
        	if (isAirplaneModeOn(this)) {
		    	mEmptyText.setText(R.string.simContacts_airplaneMode);
        	} else {
		    	Phone phone = null;
		    	if(mActivePhoneId != null && mActivePhoneId.toInt() == SimCardID.ID_ONE.toInt())
					phone = PhoneGlobals.getInstance().phone[1];
		    	else
					phone = PhoneGlobals.getInstance().phone[0];
			
		        if (phone != null && phone.getIccCard().getState() != IccCardConstants.State.READY){
		            mEmptyText.setText(R.string.simContacts_no_sim);
		        }else{
		            mEmptyText.setText(R.string.simContacts_empty);
		        }
            }
        }
        getWindow().setFeatureInt(
                Window.FEATURE_INDETERMINATE_PROGRESS,
                loading ? PROGRESS_VISIBILITY_ON : PROGRESS_VISIBILITY_OFF);
    }

    private static boolean isAirplaneModeOn(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) != 0;
    }

    private class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor c) {
            if (DBG) log("onQueryComplete: cursor.count=" + ((c==null)?"null":c.getCount()));
            mCursor = c;
            setAdapter();
            displayProgress(false);

            // Cursor is refreshed and inherited classes may have menu items depending on it.
            invalidateOptionsMenu();
        }

        @Override
        protected void onInsertComplete(int token, Object cookie, Uri uri) {
            if (DBG) log("onInsertComplete: requery");
            reQuery();
        }

        @Override
        protected void onUpdateComplete(int token, Object cookie, int result) {
            if (DBG) log("onUpdateComplete: requery");
            reQuery();
        }

        @Override
        protected void onDeleteComplete(int token, Object cookie, int result) {
            if (DBG) log("onDeleteComplete: requery");
            reQuery();
        }
    }

    protected void log(String msg) {
        Log.d(TAG, "[ADNList] " + msg);
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
                        && mActivePhoneId == simCardId) {
                    log("IccCard.MSG_SIM_STATE_ABSENT simCardId = " + simCardId);
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
    protected void onPause() {
        log("onPause");
        super.onPause();
        unregisterReceiver(mIccCardAbsentReceiver);
    }
}
