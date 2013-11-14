package com.android.phone;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.telephony.RILConstants.SimCardID;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.PreferenceCategory;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.util.Log;
import java.util.HashMap;
import android.os.Handler;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.TelephonyIntents;

public class CellBroadcastSettings extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener, ChannelInfoDialog.Callback,
        DialogInterface.OnDismissListener {
    private static final String TAG = "CellBroadcastSettings";
    private static final boolean DBG = true;

    private ListPreference mListLanguage;
    private ListPreference mListChannelType;

    public static final String CHANNEL_TYPE_MY = "0";
    public static final String CHANNEL_TYPE_ALL = "1";

    private Preference mAddChannel;
    private PreferenceCategory mChannelCategory;

    private static final String KEY_LANGUAGE_LIST = "key_language_list";
    private static final String KEY_ADD_CHANNEL = "key_add_channel";
    private static final String KEY_CHANNEL_CATEGORY = "key_channel_list";

    public static final String KEY_RECEIVE_CHANNEL_TYPE = "key_channel_type";

    public static final int DEFAULT_LANGUAGE_INDEX = 15;
    private static final int MAX_CHANNEL_COUNT = 10;

    private String mStrLang[];
    private Dialog mDialog;
    private HashMap<String, CellBroadcastSettings.ChannelInfo> mChannelPairs;

    private Phone mPhone;
    private int mSimId;
    public static final Uri CB_CHANNELS_CONTENT_URI = Uri
            .parse("content://com.broadcom.cellbroadcast/cbchannels");
    public static final String[] PROJECTION = {
            "_id", "channel_name", "channel_index", "channel_enabled", "sim_id"
    };
    private int mChannelCount;
    private ContentObserver mCbChannelObserver;
    private int mMaxCbChannelCount;
    private final Object mLock = new Object();
    private MyHandler mHandler;
    // Handler keys
    private static final int MESSAGE_CB_CONTENT_CHANGED = 1;
    private static final int MESSAGE_REFRESH_UI_LANGUAGE = 2;
    private static final int WAIT_UPDATE_CB_CONFIG_TIME = 500;
    private boolean mIsThreadWorking = false;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        String strSimId = getIntent().getDataString();
        try {
            mSimId = Integer.parseInt(strSimId);
        } catch (NumberFormatException ex) {
            mSimId = SimCardID.ID_ZERO.toInt();
        }
        if (DBG)
            Log.d(TAG, "onCreate(): mPhoneId: " + strSimId);
        if (SimCardID.ID_ONE.toInt() == mSimId) {
            mPhone = PhoneGlobals.getInstance().phone[1];
        } else {
            mPhone = PhoneGlobals.getInstance().phone[0];
        }

        mHandler = new MyHandler();
        mIsThreadWorking = false;

        addPreferencesFromResource(R.xml.cell_broadcast_settings);

        final PreferenceScreen preferenceScreen = getPreferenceScreen();

        mStrLang = getResources().getStringArray(R.array.cb_list_language_entries);

        mListLanguage = (ListPreference) preferenceScreen.findPreference(KEY_LANGUAGE_LIST);
        // set the listener for the language list preference
        mListLanguage.setOnPreferenceChangeListener(this);

        mListChannelType = (ListPreference) preferenceScreen
                .findPreference(KEY_RECEIVE_CHANNEL_TYPE);
        mListChannelType.setOnPreferenceChangeListener(this);

        mAddChannel = preferenceScreen.findPreference(KEY_ADD_CHANNEL);

        mChannelCategory = (PreferenceCategory) preferenceScreen
                .findPreference(KEY_CHANNEL_CATEGORY);
        // mChannelCategory.setOrderingAsAdded(true);

        mChannelPairs = new HashMap<String, CellBroadcastSettings.ChannelInfo>();

        String property;
        if (mPhone.getSimCardId() == SimCardID.ID_ONE) {
            property = TelephonyProperties.PROPERTY_ICC_MAX_CB_CHANNELS + "_"
                    + String.valueOf(mPhone.getSimCardId().toInt());
        } else {
            property = TelephonyProperties.PROPERTY_ICC_MAX_CB_CHANNELS;
        }
        String propertyValue = SystemProperties.get(property, "0");
        if (null != propertyValue) {
            try {
                mMaxCbChannelCount = Integer.parseInt(propertyValue);
            } catch (NumberFormatException e) {
                Log.e(TAG, "onCreate(): PROPERTY_ICC_MAX_CB_CHANNELS property value = "
                        + propertyValue);
                mMaxCbChannelCount = 0;
            }
        } else {
            Log.e(TAG, "onCreate(): PROPERTY_ICC_MAX_CB_CHANNELS property value == null");
            mMaxCbChannelCount = 0;
        }

        if (mMaxCbChannelCount > MAX_CHANNEL_COUNT)
            mMaxCbChannelCount = MAX_CHANNEL_COUNT;

        mAddChannel.setSummary("Max. number of channel: " + String.valueOf(mMaxCbChannelCount));

        refreshChannelList();

        mCbChannelObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                if (DBG)
                    Log.d(TAG, "=>onChange()");
                mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_CB_CONTENT_CHANGED));
            }
        };

        getContentResolver().registerContentObserver(CB_CHANNELS_CONTENT_URI, true,
                mCbChannelObserver);
    }

    private void handleCbContentChanged() {
        refreshChannelList();
    }

    private class CbSetConfigThread extends Thread {
        private final int mSimId;
        private final boolean mEnable;
        private int[] mChannelList;

        public CbSetConfigThread(int simId, boolean enable, int[] channelList) {
            super("CbSetConfigThread");
            mSimId = simId;
            mEnable = enable;
            if (null != channelList) {
                mChannelList = new int[channelList.length];
                java.lang.System.arraycopy(channelList, 0, mChannelList, 0, channelList.length);
            } else {
                mChannelList = null;
            }
        }

        @Override
        public void run() {
            SmsManager smsManager;
            if (SimCardID.ID_ONE.toInt() == mSimId) {
                smsManager = SmsManager.getDefault(SimCardID.ID_ONE);
            } else {
                smsManager = SmsManager.getDefault(SimCardID.ID_ZERO);
            }
            if ((null != smsManager) && (null != mChannelList)) {
                int len = mChannelList.length;
                boolean success;
                synchronized (mLock) {
                    for (int i = 0; i < len; i++) {
                        if (mEnable) {
                            success = smsManager.enableCellBroadcast(mChannelList[i]);
                            Log.d(TAG, "CbSetConfigThread.run(): enable channel = " +
                                    mChannelList[i] + " result:" + success);
                        } else {
                            success = smsManager.disableCellBroadcast(mChannelList[i]);
                            Log.d(TAG, "CbSetConfigThread.run(): disable channel = " +
                                    mChannelList[i] + " result:" + success);
                        }
                    }
                }
            } else {
                Log.e(TAG, "CbSetConfigThread.run(): smsManager = " + smsManager
                        + ", mChannelList = " + ((mChannelList == null) ? "not null" : "null"));
            }

            mIsThreadWorking = false;
        }
    }

    private class CbSetAllChannelThread extends Thread {
        private final boolean mEnable;

        public CbSetAllChannelThread(boolean enable) {
            super("CbSetAllChannelThread");
            mEnable = enable;
        }

        @Override
        public void run() {
            SmsManager smsManager;
            if (SimCardID.ID_ONE.toInt() == mSimId) {
                smsManager = SmsManager.getDefault(SimCardID.ID_ONE);
            } else {
                smsManager = SmsManager.getDefault(SimCardID.ID_ZERO);
            }
            if (null != smsManager) {
                boolean success;
                synchronized (mLock) {
                    if (mEnable) {
                        success = smsManager.enableCellBroadcastRange(0, 999);
                        Log.d(TAG, "CbSetConfigThread.run(): enable all channel result:"
                                + success);
                    } else {
                        success = smsManager.disableCellBroadcastRange(0, 999);
                        Log.d(TAG, "CbSetConfigThread.run(): disable all channel result:"
                                + success);
                    }
                }
            } else {
                Log.e(TAG, "CbSetAllChannelThread.run(): smsManager = " + smsManager);
            }

            mIsThreadWorking = false;
        }
    }

    private class CbChangeLanguageThread extends Thread {
        private final int mSimId;
        private int mNewLanguageIndex;

        public CbChangeLanguageThread(int simId, int newLanguageIndex) {
            super("CbChangeLanguageThread");
            mSimId = simId;
            mNewLanguageIndex = newLanguageIndex;
        }

        @Override
        public void run() {
            int oldIndex = getCBLanguagePropertyValue(mSimId);
            if (DBG)
                Log.d(TAG, "setCellBroadcastLanguage(): oldIndex = " + oldIndex);

            if (mNewLanguageIndex != oldIndex) {
                SmsManager smsManager;
                if (SimCardID.ID_ONE.toInt() == mSimId) {
                    smsManager = SmsManager.getDefault(SimCardID.ID_ONE);
                } else {
                    smsManager = SmsManager.getDefault(SimCardID.ID_ZERO);
                }

                boolean success;
                if (DEFAULT_LANGUAGE_INDEX != oldIndex) {
                    synchronized (mLock) {
                        success = smsManager.disableCellBroadcast(-1); // -1
                                                                       // means
                                                                       // to set
                                                                       // langId
                        Log.d(TAG, "CbChangeLanguageThread.run(): disable lang result:" + success);
                    }
                    if (DBG)
                        Log.d(TAG, "disable CB langId result = " + success);
                }

                setCBLanguagePropertyValue(mSimId, mNewLanguageIndex);
                mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_REFRESH_UI_LANGUAGE));

                if (DEFAULT_LANGUAGE_INDEX != mNewLanguageIndex) {
                    synchronized (mLock) {
                        success = smsManager.enableCellBroadcast(-1); // -1
                                                                      // means
                                                                      // to set
                                                                      // langId
                        Log.d(TAG, "CbChangeLanguageThread.run(): enable lang result:" + success);
                    }
                    if (DBG)
                        Log.d(TAG, "enable CB langId result = " + success);
                }
            }

            mIsThreadWorking = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        int idx = getCBLanguagePropertyValue(mSimId);
        mListLanguage.setValueIndex(idx);
        mListLanguage.setSummary(mStrLang[idx]);

        String channelType = mListChannelType.getValue();
        if (channelType.equals(CHANNEL_TYPE_ALL)) { // all channels
            mAddChannel.setEnabled(false);
            mChannelCategory.setEnabled(false);
        } else if (channelType.equals(CHANNEL_TYPE_MY)) { // my channel
            mAddChannel.setEnabled(true);
            mChannelCategory.setEnabled(true);
        }
        mListChannelType.setSummary(mListChannelType.getEntry());

        IntentFilter intentFilter = new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        registerReceiver(mIccCardAbsentReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mDialog != null) {
            mDialog.dismiss();
        }

        if (null != mChannelPairs) {
            mChannelPairs.clear();
            mChannelPairs = null;
        }

        getContentResolver().unregisterContentObserver(mCbChannelObserver);
    }

    /**
     * {@inheritDoc}
     */
    public void onDismiss(DialogInterface dialog) {
        if (dialog == mDialog) {
            mDialog = null;
        }
    }

    /**
     * Invoked on each preference click in this hierarchy, overrides
     * PreferenceActivity's implementation. Used to make sure we track the
     * preference click events.
     */
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mListLanguage || preference == mListChannelType) {
            // Do nothing here, because this click will be handled in
            // onPreferenceChange
        } else if (preference == mAddChannel) {
            ChannelInfoDialog dialog = new ChannelInfoDialog(this, this);
            showDialog(dialog);
        } else {
            String strTitle = preference.getTitle().toString();

            int left = strTitle.lastIndexOf(" (");
            int right = strTitle.lastIndexOf(")");
            if ((0 < left) && (right > (left + 2))) {
                String number = strTitle.substring(left + 2, right);

                CellBroadcastSettings.ChannelInfo chInfo = mChannelPairs.get(number);

                if (null != chInfo) {
                    ChannelInfoDialog dialog = new ChannelInfoDialog(this, this, chInfo.getName(),
                            chInfo.getNumber(), chInfo.isEnabled(), chInfo.getRowId());
                    showDialog(dialog);
                } else {
                    Log.e(TAG, "onPreferenceTreeClick(): Fail to find Channel: " + strTitle);
                }
            } else {
                Log.e(TAG, "onPreferenceTreeClick(): " + strTitle + " Fail to find ( and )");
            }
        }
        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mListLanguage) {
            String strLang = (String) objValue;
            if ((null == strLang) || (0 == strLang.length())) {
                Log.e(TAG, "Error Language index: " + strLang);
                return false;
            }

            // set the new language to the array which will be transmitted later
            setCellBroadcastLanguage(mSimId, mListLanguage.findIndexOfValue(strLang));
        } else if (preference == mListChannelType) {
            Log.d(TAG, "channel type!");
            String strChType = (String) objValue;
            if ((null == strChType) || (0 == strChType.length())) {
                Log.e(TAG, "Error channel type: " + strChType);
                return false;
            }

            if (strChType.equals(CHANNEL_TYPE_ALL)) {
                Log.d(TAG, "enable ALL CB channels");

                CbSetAllChannelThread cbSetAllChannelThread = new CbSetAllChannelThread(true);
                mIsThreadWorking = true;
                cbSetAllChannelThread.start();

                mAddChannel.setEnabled(false);
                mChannelCategory.setEnabled(false);
            } else if (strChType.equals(CHANNEL_TYPE_MY)) {
                CbSetAllChannelThread cbSetAllChannelThread = new CbSetAllChannelThread(false);
                mIsThreadWorking = true;
                cbSetAllChannelThread.start();

                mAddChannel.setEnabled(true);
                mChannelCategory.setEnabled(true);

                int[] channelList = getCbChannelListInfo(mSimId);
                CbSetConfigThread cbSetConfigThread = new CbSetConfigThread(mSimId, true,
                        channelList);
                mIsThreadWorking = true;
                cbSetConfigThread.start();
            }
            final int idx = mListChannelType.findIndexOfValue(strChType);
            mListChannelType.setSummary(mListChannelType.getEntries()[idx]);
        }

        return true;
    }

    private void showDialog(Dialog dialog) {
        // Have only one dialog open at a time
        if (mDialog != null) {
            mDialog.dismiss();
        }

        mDialog = dialog;
        if (dialog != null) {
            dialog.setOnDismissListener(this);
            dialog.show();
        }
    }

    private void setCellBroadcastLanguage(int simId, int index) {
        if (DBG)
            Log.d(TAG, "=>setCellBroadcastLanguage(): index = " + index);

        if ((0 > index) || (index > DEFAULT_LANGUAGE_INDEX)) {
            Log.e(TAG, "Error Cell Broadcast Language index==" + index);
            index = DEFAULT_LANGUAGE_INDEX;
        }

        int oldIndex = getCBLanguagePropertyValue(simId);
        if (DBG)
            Log.d(TAG, "setCellBroadcastLanguage(): oldIndex = " + oldIndex);

        if (index != oldIndex) {
            CbChangeLanguageThread cbChangeLangThread = new CbChangeLanguageThread(simId, index);
            mIsThreadWorking = true;
            cbChangeLangThread.start();
        }
    }

    public static void setCBLanguagePropertyValue(int simId, int langIndex) {
        String property;
        if (simId == SimCardID.ID_ONE.toInt()) {
            property = TelephonyProperties.PROPERTY_ICC_CB_LANGUAGE_INDEX + "_"
                    + String.valueOf(simId);
        } else {
            property = TelephonyProperties.PROPERTY_ICC_CB_LANGUAGE_INDEX;
        }

        SystemProperties.set(property, Integer.toString(langIndex));
    }

    public static int getCBLanguagePropertyValue(int simId) {
        String property;
        int langIndex;

        if (simId == SimCardID.ID_ONE.toInt()) {
            property = TelephonyProperties.PROPERTY_ICC_CB_LANGUAGE_INDEX + "_"
                    + String.valueOf(simId);
        } else {
            property = TelephonyProperties.PROPERTY_ICC_CB_LANGUAGE_INDEX;
        }

        String propertyValue = SystemProperties.get(property,
                Integer.toString(DEFAULT_LANGUAGE_INDEX));
        if (null != propertyValue) {
            try {
                langIndex = Integer.parseInt(propertyValue);
            } catch (NumberFormatException e) {
                Log.e(TAG,
                        "getCBLanguagePropertyValue(): PROPERTY_ICC_CB_LANGUAGE_INDEX property value = "
                                + propertyValue);
                langIndex = DEFAULT_LANGUAGE_INDEX;
            }
        } else {
            Log.e(TAG,
                    "getCBLanguagePropertyValue(): PROPERTY_ICC_CB_LANGUAGE_INDEX property value == null");
            langIndex = DEFAULT_LANGUAGE_INDEX;
        }

        return langIndex;
    }

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_CB_CONTENT_CHANGED:
                    if (DBG)
                        Log.d(TAG, "MESSAGE_CB_CONTENT_CHANGED");

                    if (!mIsThreadWorking) {
                        handleCbContentChanged();
                    } else {
                        sendMessageDelayed(obtainMessage(MESSAGE_CB_CONTENT_CHANGED),
                                WAIT_UPDATE_CB_CONFIG_TIME);
                    }
                    break;
                case MESSAGE_REFRESH_UI_LANGUAGE:
                    mListLanguage.setSummary(mStrLang[getCBLanguagePropertyValue(mSimId)]);
                    break;
                default:
                    Log.e(TAG, "Error! Unhandled message. Message: " + msg.what);
                    break;
            }
        }
    }

    private void refreshChannelList() {
        mChannelCategory.removeAll();
        mChannelPairs.clear();
        mChannelCount = 0;

        Cursor cursor;

        cursor = getContentResolver().query(CB_CHANNELS_CONTENT_URI, PROJECTION,
                "sim_id=" + mSimId,
                null,
                "channel_index DESC");

        if (null != cursor) {
            mChannelCount = cursor.getCount();

            if (cursor.moveToFirst()) {
                int rowId;
                String channelName;
                String channelIndex;
                boolean fEnable;
                int iSimId;
                do {
                    rowId = cursor.getInt(0);
                    channelName = cursor.getString(1);
                    channelIndex = cursor.getString(2);
                    fEnable = (1 == cursor.getInt(3));

                    if (DBG) {
                        iSimId = cursor.getInt(4);
                        if (iSimId != mSimId)
                            Log.e(TAG, "refreshChannelList(): sim id error: iSimId = " + iSimId
                                    + ", mSimId = " + mSimId);
                    }

                    CellBroadcastSettings.ChannelInfo newChannel =
                            new CellBroadcastSettings.ChannelInfo(
                                    channelName, channelIndex, fEnable, rowId);
                    mChannelPairs.put(channelIndex, newChannel);

                    Preference pref = new Preference(this);
                    pref.setTitle(channelName + " (" + channelIndex + ")");
                    pref.setSummary(fEnable ? R.string.channel_enabled : R.string.channel_disabled);
                    mChannelCategory.addPreference(pref);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        mAddChannel.setEnabled(mChannelCount < mMaxCbChannelCount);
    }

    public boolean onSaveChannelInfo(int rowId, String oldChannelNumber, String newChannelName,
            String newChannelNumber, boolean fEnable) {
        CellBroadcastSettings.ChannelInfo chInfo;

        chInfo = mChannelPairs.get(newChannelNumber);
        if ((!newChannelNumber.equals(oldChannelNumber)) && (null != chInfo)) {
            return false;
        }

        if (-1 != rowId) {
            // it's an update, delete old first
            int[] deleteChannelList = {
                Integer.valueOf(oldChannelNumber).intValue()
            };

            if (null != deleteChannelList) {
                CbSetConfigThread cbSetConfigThread = new CbSetConfigThread(mSimId, false,
                        deleteChannelList);
                mIsThreadWorking = true;
                cbSetConfigThread.start();
            }
        }

        int[] channelList = {
            Integer.valueOf(newChannelNumber).intValue()
        };

        if (null != channelList) {
            CbSetConfigThread cbSetConfigThread = new CbSetConfigThread(mSimId, fEnable,
                    channelList);
            mIsThreadWorking = true;
            cbSetConfigThread.start();
        }

        ContentValues initialValues = new ContentValues();
        initialValues.put("channel_name", newChannelName);
        initialValues.put("channel_index", newChannelNumber);
        initialValues.put("channel_enabled", fEnable ? 1 : 0);
        initialValues.put("sim_id", mSimId);

        if (-1 == rowId) {
            // add a new channel
            getContentResolver().insert(CB_CHANNELS_CONTENT_URI, initialValues);

        } else {
            // udpate existing channgle with rowId
            getContentResolver().update(CB_CHANNELS_CONTENT_URI, initialValues, "_id=" + rowId,
                    null);
        }

        return true;
    }

    public void onDeleteChannelInfo(int rowId, String channelNumber) {
        int[] channelList = {
            Integer.valueOf(channelNumber).intValue()
        };

        if (null != channelList) {
            CbSetConfigThread cbSetConfigThread = new CbSetConfigThread(mSimId, false, channelList);
            mIsThreadWorking = true;
            cbSetConfigThread.start();
        }

        getContentResolver().delete(CB_CHANNELS_CONTENT_URI, "_id=" + rowId, null);

        return;
    }

    private int[] getCbChannelListInfo(int simId) {
        Cursor cursor;
        int channelCount;

        cursor = getContentResolver().query(CB_CHANNELS_CONTENT_URI, PROJECTION,
                "sim_id=" + simId + " AND channel_enabled=1",
                null,
                "channel_index DESC");

        if (null != cursor) {
            channelCount = cursor.getCount();
            int[] returnValues;
            int arrayIndex = 0;

            returnValues = new int[channelCount];

            if ((null != returnValues) && (cursor.moveToFirst())) {
                String channelIndex;
                int cbChannelIndex;

                do {
                    channelIndex = cursor.getString(2);
                    try {
                        cbChannelIndex = Integer.parseInt(channelIndex);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Error Channel Index: " + channelIndex);
                        cbChannelIndex = 0;
                    }

                    returnValues[arrayIndex] = cbChannelIndex;

                    arrayIndex++;
                } while (cursor.moveToNext());
            }
            cursor.close();

            if (0 < channelCount) {
                return returnValues;
            }
        }

        return null;
    }

    private class ChannelInfo {
        private String mChannelName;
        private String mChannelNumber;
        private boolean mfEnabled;
        private int mRowId;

        public ChannelInfo(String name, String number, boolean fEnabled, int rowId) {
            mChannelName = name;
            mChannelNumber = number;
            mfEnabled = fEnabled;
            mRowId = rowId;
        }

        public String getName() {
            return mChannelName;
        }

        public String getNumber() {
            return mChannelNumber;
        }

        public boolean isEnabled() {
            return mfEnabled;
        }

        public int getRowId() {
            return mRowId;
        }
    }

    /**
     * Receives SIM Absent intent. When a broadcasted intent of SIM absent is
     * received, call setup activity of the relative SIM should be finished.
     */
    BroadcastReceiver mIccCardAbsentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(intent.getAction())) {
                final String iccCardState = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                final SimCardID simCardId = (SimCardID) (intent
                        .getExtra("simId", SimCardID.ID_ZERO));
                if (iccCardState.equals(IccCardConstants.INTENT_VALUE_ICC_ABSENT)
                        && mSimId == simCardId.toInt()) {
                    Log.d(TAG, "IccCardConstants.MSG_SIM_STATE_ABSENT simCardId = " + simCardId);
                    makeThisFinish();
                }
            }
        }
    };

    /**
     * Finish this activity. This is called when SIM removed.
     */
    private void makeThisFinish() {
        this.finish();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
        unregisterReceiver(mIccCardAbsentReceiver);
    }
}
