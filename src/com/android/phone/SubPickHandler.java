package com.android.phone;

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
import android.telephony.SubInfoRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SubPickHandler implements DialogInterface.OnClickListener,
        DialogInterface.OnCancelListener, DialogInterface.OnShowListener,
        DialogInterface.OnDismissListener, PhoneGlobals.SimInfoUpdateListener {

    private static final String LOG_TAG = "SubPickerHandler";
    private static final boolean DBG = (PhoneGlobals.DBG_LEVEL >= 2);

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
        mDialog.setCanceledOnTouchOutside(false);
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

    /**
     * generate SubPickItemList for SubPickAdapter.
     * @param context   context to use
     * @param withSipItem   indicate whether including sip item for sub pick dialog.
     * @param withUnInsertSim   indicate whether including item which doesn't have sim inserted.
     * @return
     */
    public static List<SubPickItem> getSubPickItemList(Context context,boolean withSipItem, boolean withUnInsertSim) {

        int simCount = PhoneUtils.getSimCount();
        boolean[] isSimInsertArray = new boolean[simCount];     // true for has insert sim.
        List<SubPickItem> items = new ArrayList<SubPickItem>();

        // add active SubInfo if needed
        List<SubInfoRecord> activeSubInfoLists = PhoneUtils.getActivatedSubInfoList(context);
        if (activeSubInfoLists != null) {
            // sort SubInfoList by simId
            Collections.sort(activeSubInfoLists, new Comparator<SubInfoRecord>() {
                @Override
                public int compare(SubInfoRecord arg0, SubInfoRecord arg1) {
                    return arg0.mSimId - arg1.mSimId;
                }
            });

            Log.d(LOG_TAG, "getSubPickItemList, add the active sub item. ");
            for (SubInfoRecord subInfoRecord : activeSubInfoLists) {
                SubPickItem item = new SubPickItem(subInfoRecord, SubPickAdapter.ITEM_TYPE_SUB, SubPickHandler.INVALID_SIM_ID);
                isSimInsertArray[subInfoRecord.mSimId] = true;
                items.add(item);
            }
        }

        // add sip item if needed
        if (withSipItem) {
            SubPickItem item = new SubPickItem(null, SubPickAdapter.ITEM_TYPE_INTERNET, SubPickHandler.INVALID_SIM_ID);
            items.add(item);
        }

        // add unInsert sims if needed
        if (withUnInsertSim) {
            for (int i = 0; i < simCount; i++) {
                if (!isSimInsertArray[i]) {
                    SubPickItem item = new SubPickItem(null, SubPickAdapter.ITEM_TYPE_TEXT, i);
                    items.add(item);
                }
            }
        }
        if (DBG) {
            dumpSimPickerItems(items);
        }
        return items;
    }

    private static void dumpSimPickerItems(List<SubPickItem> items) {
        Log.d(LOG_TAG, "------------Sim Items Dump Begin------------");
        Log.d(LOG_TAG, "items.size(): " + items.size());
        for (int i = 0; i < items.size(); i++) {
            Log.d(LOG_TAG, "items.get(" + i + ").mSimId: " + items.get(i).mSimId);
            Log.d(LOG_TAG, "items.get(" + i + ").mSubInfoRecord: " + items.get(i).mSubInfoRecord);
        }
        Log.d(LOG_TAG, "------------Sim Items Dump End------------");
    }
}
