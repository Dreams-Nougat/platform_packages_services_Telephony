package com.android.phone;

import java.util.List;

import com.android.internal.widget.SubscriptionView;
import com.android.phone.SubPickAdapter.SubPickItem;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ListAdapter;
import android.telephony.SubscriptionController.SubInfoRecord;


public class SubPickHandler implements DialogInterface.OnClickListener,
        DialogInterface.OnCancelListener, DialogInterface.OnShowListener,
        DialogInterface.OnDismissListener, PhoneGlobals.SimInfoUpdateListener {

    private static final String LOG_TAG = "SubPickerHandler";
    public static final long INVALID_SUB_ID = -1;
    public static final int INVALID_SIM_ID = -1;
    public static final String SUGGEST_SUB_ID_EXTRA = "";
    public static final int SUB_PICK_COMPLETE_KEY_SUB = 101;
    public static final int SUB_PICK_COMPLETE_KEY_SIP = 102;
    public static final int SUB_PICK_COMPLETE_KEY_CANCEL = 103;

    private Context mContext;
    private Dialog mDialog;
    private Intent mIntent;
    private List<SubPickItem> mSubPickItemList;
    private SubPickListener mSubPickListener;
    private long mSuggestedSubId = INVALID_SUB_ID;
    private int mSubViewThemeType = SubscriptionView.DARK_THEME;

    public interface SubPickListener {
        void onSubPickComplete(int completeKey, long subId, Intent intent);
    }

    public SubPickHandler(Context context, List<SubPickItem> subPickItemList) {
        mContext = context;
        mSubPickItemList = subPickItemList;
    }

    /**
     * When sub pick done, use this listener to notify the User.
     */
    public void setSubPickListener(SubPickListener listener) {
        mSubPickListener = listener;
    }

    public void setSuggestedSubId(long suggestedSubId) {
        mSuggestedSubId = suggestedSubId;
    }

    public void setSubViewThemeType(int subViewThemeType) {
        mSubViewThemeType = subViewThemeType;
    }

    public void setIntent(Intent intent) {
        mIntent = intent;
    }
    /**
     * create sub pick dialog and show it.
     * notice: call setSubPickListener() before it.
     * @param fullScreen true for full-screen dialog.
     */
    public void showSubPickDialog(String title, boolean fullScreen) {
        SubPickAdapter subPickAdpater = new SubPickAdapter(mContext, mSubViewThemeType, mSuggestedSubId);
        subPickAdpater.setItems(mSubPickItemList);

        mDialog = new AlertDialog.Builder(mContext)
                .setTitle(title)
                .setSingleChoiceItems(subPickAdpater, -1, this)
                .setOnCancelListener(this)
                .create();

        mDialog.setOnShowListener(this);
        mDialog.setOnDismissListener(this);
        if (fullScreen) {
            mDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        mDialog.show();
    }

    private void onSubPickComplete(int completeKey, long subId) {
        Log.d(LOG_TAG, "onSubPickComplete()... completeKey / subId: " + completeKey + " / " + subId);
        dismissDialog();
        if (mSubPickListener != null) {
            mSubPickListener.onSubPickComplete(completeKey, subId, mIntent);
        }
    }

    public void dismissDialog() {
        if (mDialog != null) {
            if (mDialog.isShowing()) {
                mDialog.dismiss();
            }
            mDialog = null;
        }
    }

    @Override
    public void handleSimInfoUpdate() {
        onSubPickComplete(SUB_PICK_COMPLETE_KEY_CANCEL, INVALID_SUB_ID);
    }

    @Override
    public void onDismiss(DialogInterface arg0) {
        PhoneGlobals.getInstance().removeSimInfoUpdateListener(this);
    }

    @Override
    public void onShow(DialogInterface arg0) {
        PhoneGlobals.getInstance().addSimInfoUpdateListener(this);
    }

    @Override
    public void onCancel(DialogInterface arg0) {
        onSubPickComplete(SUB_PICK_COMPLETE_KEY_CANCEL, INVALID_SUB_ID);
    }

    @Override
    public void onClick(DialogInterface dialog, int id) {
        final AlertDialog alert = (AlertDialog) dialog;
        final ListAdapter listAdapter = alert.getListView().getAdapter();
        final SubPickItem subPickerItem = ((SubPickItem)listAdapter.getItem(id));

        if (subPickerItem.mViewType == SubPickAdapter.ITEM_TYPE_INTERNET) {
            onSubPickComplete(SUB_PICK_COMPLETE_KEY_SIP, INVALID_SUB_ID);
        } else if (subPickerItem.mViewType == SubPickAdapter.ITEM_TYPE_SUB) {
            SubInfoRecord subInfoRecord = subPickerItem.mSubInfoRecord;
            onSubPickComplete(SUB_PICK_COMPLETE_KEY_SUB, subInfoRecord.mSubId);
        }
    }

}
