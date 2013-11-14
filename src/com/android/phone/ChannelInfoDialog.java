package com.android.phone;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.text.TextUtils;
import android.util.Log;

public class ChannelInfoDialog extends AlertDialog implements View.OnClickListener {
	private static final String TAG = "ChannelInfoDialog";
    private static final boolean DBG = false;
	
	private String mInitChannelName;
	private String mInitChannelNumber;
	private boolean mfInitEnabled;
	private int mRowId;
	private EditText mChannelNameEditText;
	private EditText mChannelNumberEditText;
	private CheckBox mEnableChannelCheckBox;
	private Button mSaveButton;
	private Button mCancelButton;
	private Button mDeleteButton;
	Callback mCallback;
	
	public ChannelInfoDialog(Context context,Callback callback, String channelName, String channelNumber, boolean fEnabled, int rowId) {
		super(context);
		
		mCallback = callback;
		mInitChannelName = channelName;
		mInitChannelNumber = channelNumber;
		mfInitEnabled = fEnabled;
		mRowId = rowId;
	}
	
	public ChannelInfoDialog(Context context,Callback callback) {
		this(context,callback,"","",true,-1);
	}
	
	interface Callback {
		boolean onSaveChannelInfo(int rowId, String oldChannelNumber, String newChannelName, String newChannelNumber, boolean fEnable);
		void onDeleteChannelInfo(int rowId, String channelNumber);
	}
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {

		setInverseBackgroundForced(true);
		
		View v = getLayoutInflater().inflate(R.layout.channel_info_dialog, null);
		
		mChannelNameEditText = (EditText) v.findViewById(R.id.channel_name_edit);
		mChannelNumberEditText = (EditText) v.findViewById(R.id.channel_number_edit);
		mEnableChannelCheckBox = (CheckBox) v.findViewById(R.id.enable_channel);
		
		mChannelNameEditText.setText(mInitChannelName);
		mChannelNumberEditText.setText(mInitChannelNumber);
		mEnableChannelCheckBox.setChecked(mfInitEnabled);
		
		mSaveButton = (Button) v.findViewById(R.id.button1);
		mCancelButton = (Button) v.findViewById(R.id.button2);
		mDeleteButton = (Button) v.findViewById(R.id.button3);
		
		mSaveButton.setOnClickListener(this);
		mCancelButton.setOnClickListener(this);
		
		if (-1 == mRowId) {
			mDeleteButton.setVisibility(View.GONE);
			setTitle(R.string.add_channel_title);
		} else {
			mDeleteButton.setOnClickListener(this);
			setTitle(R.string.channel_info_title);
		}
		
		setView(v);
		
		super.onCreate(savedInstanceState);
    }
	
	public void onClick(View v) {
        if (v == mSaveButton) {
        	String name = mChannelNameEditText.getText().toString();
        	String number = mChannelNumberEditText.getText().toString();
        	boolean fEnabled = mEnableChannelCheckBox.isChecked();
        	boolean fNameIsEmpty = TextUtils.isEmpty(name);
        	boolean fNumberIsEmpty = TextUtils.isEmpty(number);
        	if (fNameIsEmpty || fNumberIsEmpty) {
        		AlertDialog.Builder b = new AlertDialog.Builder(getContext());
        		
        		b.setTitle(R.string.channel_info_title);
        		b.setMessage(R.string.info_required);
        		b.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
        			public void onClick(DialogInterface dialog, int which) {
        				
        			}
        		});
        		
        		b.show();
        		
//        		if (fNameIsEmpty) {
//        			mChannelNameEditText.requestFocus();
//        		} else if (fNumberIsEmpty) {
//        			mChannelNumberEditText.requestFocus();
//        		}        		
        		
        		return;
        	}
        	
        	if ((name.equals(mInitChannelName)) && (number.equals(mInitChannelNumber)) && (fEnabled == mfInitEnabled)) {
        		dismiss();
        		return;        		
        	}
        	
        	// callback to check conflict
        	if (null != mCallback) {
        		if (!mCallback.onSaveChannelInfo(mRowId,mInitChannelNumber,name,number,fEnabled)) {
        			AlertDialog.Builder b = new AlertDialog.Builder(getContext());
            		
            		b.setTitle(R.string.channel_info_title);
            		b.setMessage(R.string.channel_exist);
            		b.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            			public void onClick(DialogInterface dialog, int which) {
            				
            			}
            		});
            		
            		b.show();
            		
//            		mChannelNumberEditText.requestFocus();
//            		mChannelNumberEditText.selectAll();
            		return;
        		}   		
        	} else {
        		Log.e(TAG,"null == mCallback");
        	}
        	
        	dismiss();
        	
        } else if (v == mCancelButton) {
            cancel();
        } else if (v == mDeleteButton) {
        	if ((DBG) && (TextUtils.isEmpty(mInitChannelNumber))) {
    			Log.e(TAG,"Error: to delete an empty channel");
    		}
                mCallback.onDeleteChannelInfo(mRowId, mInitChannelNumber);
        	
        	dismiss();
        }
    }
}
