package com.android.phone;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Telephony;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.RILConstants.SimCardID;
import com.android.internal.telephony.TelephonyIntents;
import static com.android.internal.telephony.RILConstants.SimCardID.*;


public class BrcmSimDialogActivity extends Activity {
    private static final String TAG = "BrcmSimDialogActivity";

    private TelephonyManager tm0, tm1;

    final Uri SIM_NAMES_CONTENT_URI = Uri.parse("content://com.broadcom.simname/simnames");
    final String[] PROJECTION = {"_id", "sim_imsi", "sim_name", "sim_name_enabled", "sim_in_use"};
    private SimNameQueryHandler mSimNameQueryHandler0;
    private SimNameQueryHandler mSimNameQueryHandler1;

    private LinearLayout mSimDialogLayout;
    private Dialog mSimDialog;
    private TextView mSimName0;
    private TextView mSimName1;

    private static final int MSG_NEW_SIM_0 = 8000;
    private static final int MSG_NEW_SIM_1 = 8001;
    private static final int MSG_OLD_SIM_0 = 8002;
    private static final int MSG_OLD_SIM_1 = 8003;

    private enum SimUsage {
        NA,
        ABSENT,
        IN_USE,
        QUERYING,
        NEW_SIM
    }

    private static SimUsage mSimUsage[] = {SimUsage.NA, SimUsage.NA};
    private boolean mNeedUpdateSimUsageToDb[] = {false, false};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        ContentResolver resolver = getContentResolver();
        mSimNameQueryHandler0 = new SimNameQueryHandler(resolver, ID_ZERO.toInt());
        mSimNameQueryHandler1 = new SimNameQueryHandler(resolver, ID_ONE.toInt());

        tm0 = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE1);
        tm1 = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE2);

        mSimDialogLayout = (LinearLayout) LayoutInflater.from(this).inflate(
                               R.layout.sim_chooser_dialog_view, null);

        mSimName0 = (TextView) mSimDialogLayout.findViewById(R.id.sim1Name);
        mSimName1 = (TextView) mSimDialogLayout.findViewById(R.id.sim2Name);

        // check sim status in the begining
        if (!tm0.hasIccCard()) {
            mSimUsage[ID_ZERO.toInt()] = SimUsage.ABSENT;
            mSimName0.setText(R.string.simContacts_no_sim);
            mSimName0.setEnabled(false);
        } else if (tm0.getSimState() == TelephonyManager.SIM_STATE_READY
                   && mSimUsage[ID_ZERO.toInt()] == SimUsage.NA) {
            startSimNameQuery(ID_ZERO.toInt());
            mSimUsage[ID_ZERO.toInt()] = SimUsage.QUERYING;
        }
        if (!tm1.hasIccCard()) {
            mSimUsage[ID_ONE.toInt()] = SimUsage.ABSENT;
            mSimName1.setText(R.string.simContacts_no_sim);
            mSimName1.setEnabled(false);
        } else if (tm1.getSimState() == TelephonyManager.SIM_STATE_READY
                   && mSimUsage[ID_ONE.toInt()] == SimUsage.NA) {
            startSimNameQuery(ID_ONE.toInt());
            mSimUsage[ID_ONE.toInt()] = SimUsage.QUERYING;
        }

        finishThisIfNecessary();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();

        unregisterReceiver(mSimUpdateReceiver);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();

        IntentFilter intentFilter = new IntentFilter(TelephonyIntents.SPN_STRINGS_UPDATED_ACTION);
        intentFilter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        registerReceiver(mSimUpdateReceiver, intentFilter);
    }

    private void finishThisIfNecessary() {
        if (mSimUsage[ID_ZERO.toInt()] != SimUsage.NA
                && mSimUsage[ID_ONE.toInt()] != SimUsage.NA
                && mSimUsage[ID_ZERO.toInt()] != SimUsage.NEW_SIM
                && mSimUsage[ID_ONE.toInt()] != SimUsage.NEW_SIM
                && mSimUsage[ID_ZERO.toInt()] != SimUsage.QUERYING
                && mSimUsage[ID_ONE.toInt()] != SimUsage.QUERYING) {
            Log.d(TAG, "finish: " + mSimUsage[ID_ZERO.toInt()] + "/" + mSimUsage[ID_ONE.toInt()]);
            finish();
        }
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_NEW_SIM_0:
                Log.d(TAG, "MSG_NEW_SIM_0");
                mSimUsage[ID_ZERO.toInt()] = SimUsage.NEW_SIM;
                if (mSimDialog == null || !mSimDialog.isShowing()) {
                    showSimDialog();
                }
                clearSimInUse();
                startUpdateSimInUse(ID_ZERO.toInt());
                break;

            case MSG_NEW_SIM_1:
                Log.d(TAG, "MSG_NEW_SIM_1");
                mSimUsage[ID_ONE.toInt()] = SimUsage.NEW_SIM;
                if (mSimDialog == null || !mSimDialog.isShowing()) {
                    showSimDialog();
                }
                clearSimInUse();
                startUpdateSimInUse(ID_ONE.toInt());
                break;

            case MSG_OLD_SIM_0:
                Log.d(TAG, "MSG_OLD_SIM_0");
                mSimUsage[ID_ZERO.toInt()] = SimUsage.IN_USE;
                mSimName0.setEnabled(false);
                clearSimInUse();
                finishThisIfNecessary();
                break;

            case MSG_OLD_SIM_1:
                Log.d(TAG, "MSG_OLD_SIM_1");
                mSimUsage[ID_ONE.toInt()] = SimUsage.IN_USE;
                mSimName1.setEnabled(false);
                clearSimInUse();
                finishThisIfNecessary();
            }
        }
    };

    private void showSimDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(mSimDialogLayout);
        builder.setTitle(R.string.new_sim_title);
        builder.setNeutralButton(R.string.sim_mobile_networks_setting, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_NEUTRAL) {
                    Log.d(TAG, "go to setting");
                    Intent intent = new Intent("android.settings.DATA_ROAMING_SETTINGS");
                    startActivity(intent);
                    mSimDialog.dismiss();
                }
            }
        });
        builder.setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_NEGATIVE) {
                    Log.d(TAG, "just dismiss");
                    mSimDialog.dismiss();
                }
            }
        });

        mSimDialog = builder.create();
        mSimDialog.setOnDismissListener(new AlertDialog.OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                finish();
            }
        });
        Log.d(TAG, "showSimDialog");
        mSimDialog.show();
    }

    private void startSimNameQuery(final int simId) {
        Handler queryCB = new Handler();
        queryCB.postDelayed(new Runnable() {
            public void run() {
                Log.d(TAG, "IccCardConstants.INTENT_VALUE_ICC_READY: startQuery(), simId = " + simId);
                if (simId == ID_ONE.toInt()) {
                    String imsi = tm1 != null ? tm1.getSubscriberId() : null;
                    Log.d(TAG, "imsi=[" + imsi + "]");
                    mSimNameQueryHandler1.startQuery(0,
                                                     null,
                                                     SIM_NAMES_CONTENT_URI,
                                                     PROJECTION,
                                                     "sim_imsi='" + imsi + "'",
                                                     null,
                                                     null);
                } else {
                    String imsi = tm0 != null ? tm0.getSubscriberId() : null;
                    Log.d(TAG, "imsi=[" + imsi + "]");
                    mSimNameQueryHandler0.startQuery(0,
                                                     null,
                                                     SIM_NAMES_CONTENT_URI,
                                                     PROJECTION,
                                                     "sim_imsi='" + imsi + "'",
                                                     null,
                                                     null);
                }
            }
        }, 1000);
    }

    private void clearSimInUse() {
        if (mSimUsage[ID_ZERO.toInt()] == SimUsage.NA || mSimUsage[ID_ONE.toInt()] == SimUsage.NA) {
            // some sim usage is not decided yet
            return;
        }

        Handler updateCB = new Handler();
        updateCB.postDelayed(new Runnable() {
            public void run() {
                Log.d(TAG, "IccCardConstants.INTENT_VALUE_ICC_READY: startClear");

                String imsi0 = tm0.getSubscriberId();
                String imsi1 = tm1.getSubscriberId();
                String where0 = (imsi0 == null || imsi0.length() == 0) ?
                                null : "sim_imsi<>'" + imsi0 + "'";
                String where1 = (imsi1 == null || imsi1.length() == 0) ?
                                null : "sim_imsi<>'" + imsi1 + "'";
                String where = null;
                if (where0 == null && where1 == null) {
                    Log.d(TAG, "no imsi available when clear sim_in_use");
                    return;
                } else if (where0 == null || where1 == null) { //only one imsi available
                    where = (where0 == null) ? where1 : where0;
                } else {
                    where = where0 + " AND " + where1;
                    Log.d(TAG, "[" + where + "]");
                }

                ContentValues bundle = new ContentValues();
                bundle.put("sim_in_use", 0);

                mSimNameQueryHandler0.startUpdate(0,
                                                  null,
                                                  SIM_NAMES_CONTENT_URI,
                                                  bundle,
                                                  where,
                                                  null);
            }
        }, 500);
    }

    private void startUpdateSimInUse(final int simId) {
        Handler updateCB = new Handler();
        updateCB.postDelayed(new Runnable() {
            public void run() {
                Log.d(TAG, "IccCardConstants.INTENT_VALUE_ICC_READY: startUpdate(), simId = " + simId);

                String imsi = null;
                SimNameQueryHandler mSimNameUpdateHandler = null;
                if (simId == ID_ONE.toInt()) {
                    imsi = tm1 != null ? tm1.getSubscriberId() : null;
                    mSimNameUpdateHandler = mSimNameQueryHandler1;
                } else {
                    imsi = tm0 != null ? tm0.getSubscriberId() : null;
                    mSimNameUpdateHandler = mSimNameQueryHandler0;
                }

                if (null == imsi || null == mSimNameUpdateHandler) {
                    Log.d(TAG, "no update");
                    return;
                }

                ContentValues bundle = new ContentValues();
                bundle.put("sim_in_use", 1);

                if (mNeedUpdateSimUsageToDb[simId]) {  //update
                    mSimNameUpdateHandler.startUpdate(0,
                                                      null,
                                                      SIM_NAMES_CONTENT_URI,
                                                      bundle,
                                                      "sim_imsi='" + imsi + "'",
                                                      null);
                } else {  //insert
                    bundle.put("sim_imsi", imsi);
                    mSimNameUpdateHandler.startInsert(0,
                                                      null,
                                                      SIM_NAMES_CONTENT_URI,
                                                      bundle);
                }
            }
        }, 2000);
    }

    private class SimNameQueryHandler extends AsyncQueryHandler {
        private int mSimId;

        public SimNameQueryHandler(ContentResolver resolver, int simId) {
            super(resolver);
            mSimId = simId;
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            Log.d(TAG,"onQueryComplete()");
            boolean isNewSim = false;

            if (null != cursor) {
                int channelCount = cursor.getCount();
                Log.d(TAG, "channelCount = " + channelCount);
                if ((channelCount > 0) && (cursor.moveToFirst())) {
                    mNeedUpdateSimUsageToDb[mSimId] = true;

                    if (cursor.getInt(cursor.getColumnIndex("sim_in_use")) == 1) {
                        Log.d(TAG, "not a new SIM");
                    } else {
                        Log.d(TAG, "return to a historical SIM in sim" + mSimId);
                        isNewSim = true;
                    }
                } else {
                    Log.d(TAG, "we have a new SIM in sim" + mSimId);
                    isNewSim = true;
                }
                cursor.close();
            } else {
                Log.d(TAG, "cursor null, we have a new SIM in sim" + mSimId);
                isNewSim = true;
            }

            if (isNewSim) {
                if (mSimId == ID_ONE.toInt()) {
                    mHandler.sendMessage(mHandler.obtainMessage(MSG_NEW_SIM_1));
                } else {
                    mHandler.sendMessage(mHandler.obtainMessage(MSG_NEW_SIM_0));
                }
            } else {
                if (mSimId == ID_ONE.toInt()) {
                    mHandler.sendMessage(mHandler.obtainMessage(MSG_OLD_SIM_1));
                } else {
                    mHandler.sendMessage(mHandler.obtainMessage(MSG_OLD_SIM_0));
                }
            }
        }

        @Override
        protected void onInsertComplete(int token, Object cookie, Uri uri) {
            Log.d(TAG, "onInsertComplete");
            Log.d(TAG, "uri=" + uri.toString()); //delete me
        }

        @Override
        protected void onUpdateComplete(int token, Object cookie, int result) {
            Log.d(TAG, "onUpdateComplete");
            Log.d(TAG, "result=" + result);
        }
    }

    BroadcastReceiver mSimUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TelephonyIntents.SPN_STRINGS_UPDATED_ACTION.equals(intent.getAction())) {
                SimCardID simCardId = (SimCardID)(intent.getExtra("simId", ID_ZERO));
                updateNetworkName(intent.getStringExtra(TelephonyIntents.EXTRA_SPN),
                                  intent.getStringExtra(TelephonyIntents.EXTRA_PLMN), simCardId.toInt());
            } else if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(intent.getAction())) {
                String iccState = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                SimCardID simCardId = (SimCardID)(intent.getExtra("simId", SimCardID.ID_ZERO));
                final int simId;
                simId = simCardId.toInt();
                Log.d(TAG, "ACTION_SIM_STATE_CHANGED: " + iccState + ", simId = " + simId);
                if (IccCardConstants.INTENT_VALUE_ICC_READY.equals(iccState)) {
                    if (SimUsage.NA == mSimUsage[simId]) {
                        startSimNameQuery(simId);
                        mSimUsage[simId] = SimUsage.QUERYING;
                    }
                }
            }
        }
    };

    void updateNetworkName(String spn, String plmn, int simId) {
        if (true) {
            Log.d(TAG, "updateNetworkName spn=" + spn + " plmn=" + plmn + " simId=" + simId);
        }
        StringBuilder str = new StringBuilder();
        boolean something = false;

        if (spn != null) {
            str.append(spn);
            something = true;
        }

        if (plmn != null && !something) {
            str.append(plmn);
            something = true;
        }

        if (something) {
            if (simId == ID_ONE.toInt()) {
                mSimName1.setText(str.toString());
            } else {
                mSimName0.setText(str.toString());
            }
        }
    }
}
