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
 * limitations under the License
 */
package com.android.phone;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.database.Cursor;
import android.content.BroadcastReceiver;
import android.os.Message;
import android.net.Uri;
import java.util.ArrayList;
import java.util.List;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.ContentUris;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import com.android.phone.R;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import java.io.ByteArrayOutputStream;

import android.telephony.TelephonyManager;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.IccProvider;
import com.android.internal.telephony.RILConstants.SimCardID;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import android.provider.ContactsContract.Groups;

public class BrcmLoadIccContactsService extends Service {

    private static final String TAG = "LoadIccContactsService";
    private static final boolean DBG = true;
    protected QueryHandler mIccContactsQueryHandler0;
    protected QueryHandler mIccContactsQueryHandler1;

    private static final int MSG_SIM_STATE_LOADED = 8000;
    private static final int MSG_SIM_STATE_NOT_READY = 8001;
    private static final int MSG_PHONEBOOK_DATABASE_DELTED = 8002;
    private static final int MSG_CONTACTS_END_LOADING = 8003;
    private static final int MSG_WAIT_TIMEOUT = 8004;

    private static final int WAIT_DELETION = 1000; // 1 second
    private static final int WAIT_ERROR_DELETION = 30000; // 30 second
    private static final int WAIT_LOADING = 1000; // 1 second

    private boolean mShutdown = false;
    private String mIccCapabilityInfo0 = "";
    private String mIccCapabilityInfo1 = "";
    private String mIccCardState0 = "";
    private String mIccCardState1 = "";

    private static final String ACTION_SIM_CAPABILITY_INFO = "com.android.phone.SIM_CAPABILITY_INFO";
    private static final String ACTION_QUERY_SIM_CAPABILITY_INFO = "com.android.contacts.QUERY_SIM_CAPABILITY_INFO";
    private static final String ICC_CONTACTS_ACCOUNT_TYPE = "com.android.contacts.sim";

    private static final String[] COLUMN_NAMES = new String[] {
        IccProvider.ICC_TAG,
        IccProvider.ICC_NUMBER,
        IccProvider.ICC_EMAILS,
        IccProvider.ICC_ANRS,
        IccProvider.ICC_GROUPS,
        IccProvider.ICC_INDEX
    };

    private static final String[] GROUP_COLUMN_NAME = new String[] {
        IccProvider.ICC_GROUPS,
        IccProvider.ICC_INDEX
    };

    private static final String[] INFO_COLUMN_NAMES = new String[] {
        IccProvider.ICC_ADN_TOTAL, IccProvider.ICC_ADN_USED, IccProvider.ICC_ADN_LEN, IccProvider.ICC_ADN_DIGIT_LEN,
        IccProvider.ICC_EMAIL_TOTAL,IccProvider.ICC_EMAIL_USED, IccProvider.ICC_EMAIL_LEN,
        IccProvider.ICC_ANR_TOTAL,IccProvider.ICC_ANR_USED,IccProvider.ICC_ANR_LEN,
        IccProvider.ICC_GROUP_TOTAL,IccProvider.ICC_GROUP_USED,IccProvider.ICC_GROUP_LEN
    };

    private enum IccContactsState {
        NOT_READY,
        DELETING,
        DELETED,
        SIM_QUERY_AND_ADD,
        LOADED;
    };

    private byte[] mPhotoIcon0 = null;
    private byte[] mPhotoIcon1 = null;

    @Override
    public void onCreate() {
        if (DBG) Log.d(TAG, "=>onCreate()");
        mIccContactsQueryHandler0 = new QueryHandler(this, getContentResolver(), SimCardID.ID_ZERO);

        mIccContactsQueryHandler1 = new QueryHandler(this, getContentResolver(), SimCardID.ID_ONE);

        IntentFilter intentFilter = new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        intentFilter.addAction(Intent.ACTION_SHUTDOWN);
        intentFilter.addAction(ACTION_QUERY_SIM_CAPABILITY_INFO);

        registerReceiver(mIccCardReadyReceiver, intentFilter);

        mIccCapabilityInfo0 = "";
        mIccCapabilityInfo1 = "";

        if (null != mIccContactsQueryHandler0) {
            mIccContactsQueryHandler0.sendEmptyMessage(MSG_SIM_STATE_NOT_READY);
        }
        if (null != mIccContactsQueryHandler1) {
            mIccContactsQueryHandler1.sendEmptyMessage(MSG_SIM_STATE_NOT_READY);
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mIccCardReadyReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void broadcastSimReadyIntent() {
        if(DBG) Log.d(TAG, "broadcastSimReadyIntent(): mIccCapabilityInfo0 = " + mIccCapabilityInfo0 + " mIccCapabilityInfo1 = " + mIccCapabilityInfo1);
        Intent intent = new Intent(ACTION_SIM_CAPABILITY_INFO);
        intent.putExtra("sim1Info", mIccCapabilityInfo0);
        intent.putExtra("sim2Info", mIccCapabilityInfo1);
        intent.setPackage("com.android.contacts");

        sendBroadcast(intent);
    }

    BroadcastReceiver mIccCardReadyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DBG) Log.d(TAG,"onReceive(): action = " + action);
            if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(action)) {

                final String iccCardState = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                final SimCardID simCardId = (SimCardID)(intent.getExtra("simId", SimCardID.ID_ZERO));
                if (DBG) Log.d(TAG,"onReceive(): simCardId = " + simCardId + ", iccCardState = " + iccCardState);

                QueryHandler handler = null;

                if (SimCardID.ID_ONE == simCardId) {
                    handler = mIccContactsQueryHandler1;
                } else if (SimCardID.ID_ZERO == simCardId) {
                    handler = mIccContactsQueryHandler0;
                }

                if (iccCardState.equals(IccCardConstants.INTENT_VALUE_ICC_LOADED)) {
                    TelephonyManager tm;
                    if (simCardId==SimCardID.ID_ONE) {
                        tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE2);
                        mIccCardState1 = iccCardState;
                    } else {
                        tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE1);
                        mIccCardState0 = iccCardState;
                    }

                    if (null != handler) {
                        if ((null != tm) && (TelephonyManager.SIM_STATE_READY == tm.getSimState())) {
                            if (isUSIM(simCardId) || (!isFdnEnabled(simCardId))) {
                                handler.sendEmptyMessage(MSG_SIM_STATE_LOADED);
                            } else {
                                Log.d(TAG,"IccCardConstants.INTENT_VALUE_ICC_LOADED, but SIM is 2G and FDN enabled");
                            }
                        } else {
                            Log.d(TAG,"IccCardConstants.INTENT_VALUE_ICC_LOADED but SIM state is not ready ");
                        }
                    } else
                        Log.e(TAG,"IccCardConstants.INTENT_VALUE_ICC_LOADED but handler==null" + ", simCardId = " + simCardId);
                } else if ((iccCardState.equals(IccCardConstants.INTENT_VALUE_ICC_NOT_READY))
                        || (iccCardState.equals(IccCardConstants.INTENT_VALUE_ICC_ABSENT))) {
                    if (simCardId==SimCardID.ID_ONE) {
                        mIccCardState1 = iccCardState;
                    } else {
                        mIccCardState0 = iccCardState;
                    }
                    if (null != handler) {
                        if (!mShutdown)
                            handler.sendEmptyMessage(MSG_SIM_STATE_NOT_READY);
                    } else
                        Log.e(TAG,"IccCardConstants.INTENT_VALUE_ICC_NOT_READY but handler==null" + ", simCardId = " + simCardId);
                }
            } else if (Intent.ACTION_SHUTDOWN.equals(action)) {
                if (DBG) Log.d(TAG,"onReceive(): Intent.ACTION_SHUTDOWN");
                mShutdown = true;
            } else if (ACTION_QUERY_SIM_CAPABILITY_INFO.equals(action)) {
                new Thread(new Runnable() {
                    public void run() {
                        broadcastSimReadyIntent();
                    }
                }).start();
                if (DBG) Log.d(TAG,"onReceive(): ACTION_QUERY_SIM_CAPABILITY_INFO=>");
            }
        }
    };

    private boolean isFdnEnabled(SimCardID simId) {
        com.android.internal.telephony.Phone phone;
        if (SimCardID.ID_ONE == simId) {
            phone = PhoneFactory.getDefaultPhone(SimCardID.ID_ONE);
        } else {
            phone = PhoneFactory.getDefaultPhone(SimCardID.ID_ZERO);
        }

        IccCard iccCard = phone.getIccCard();
        if (iccCard.getIccFdnEnabled()) {
            if (DBG) Log.d(TAG, "isFdnEnabled("+simId.toInt()+")=>true");
            return true;
        } else {
            if (DBG) Log.d(TAG, "isFdnEnabled("+simId.toInt()+")=>false");
            return false;
        }
    }

    private boolean isUSIM(SimCardID simId) {
        com.android.internal.telephony.Phone phone;
        if (SimCardID.ID_ONE == simId) {
            phone = PhoneFactory.getDefaultPhone(SimCardID.ID_ONE);
        } else {
            phone = PhoneFactory.getDefaultPhone(SimCardID.ID_ZERO);
        }

        IccCard iccCard = phone.getIccCard();
        if (iccCard.isApplicationOnIcc(IccCardApplicationStatus.AppType.APPTYPE_USIM)) {
            if (DBG) Log.d(TAG, "isUSIM("+simId.toInt()+")=>true");
            return true;
        } else {
            if (DBG) Log.d(TAG, "isUSIM("+simId.toInt()+")=>false");
            return false;
        }
    }

    private class IccContactsDeleteThread extends Thread {
        private SimCardID mSimId;
        private Context mContext;
        public IccContactsDeleteThread(Context context, SimCardID simId) {
            super("IccContactsDeleteThread");
            mContext = context;
            mSimId = simId;
        }

        @Override
        public void run() {
            if (DBG) Log.d(TAG,"IccContactsDeleteThread()=>run() simid = " + mSimId);

            QueryHandler handler;
            int deleteResult;
            String simAccountName;
            Uri rawContactUriIsSyncAdapter = RawContacts.CONTENT_URI.buildUpon()
                                             .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                                             .build();
            Uri groupUriIsSyncAdapter = Groups.CONTENT_URI.buildUpon()
                                             .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                                             .build();

            if (mSimId == SimCardID.ID_ONE) {
                handler = mIccContactsQueryHandler1;
                simAccountName = "SIM2";
            } else {
                handler = mIccContactsQueryHandler0;
                simAccountName = "SIM1";
            }

            try {
                deleteResult = getContentResolver().delete(rawContactUriIsSyncAdapter,
                                                                    RawContacts.ACCOUNT_TYPE + "=?" + " AND " + RawContacts.ACCOUNT_NAME + "=?",
                                                                    new String[] {ICC_CONTACTS_ACCOUNT_TYPE, simAccountName});

                if (DBG) {
                    Log.d(TAG, "IccContactsDeleteThread(): account name = " + simAccountName + ", deleteResult = " + deleteResult);

//                    Cursor c = getContentResolver().query(RawContacts.CONTENT_URI,
//                                                                   new String[] {RawContacts._ID},
//                                                                   RawContacts.ACCOUNT_TYPE + "=?" + " AND " + RawContacts.ACCOUNT_NAME + "=?",
//                                                                   new String[] {ICC_CONTACTS_ACCOUNT_TYPE, simAccountName},
//                                                                   null);
//
//                    if (null != c) {
//                        int count = c.getCount();
//                        if (0 < count) {
//                            Log.e(TAG, "IccContactsDeleteThread(): after deletion, count = " + count);
//                        }
//
//                        c.close();
//                    } else {
//                        Log.e(TAG, "IccContactsDeleteThread(): null == c");
//                    }
                }
            } catch (IllegalArgumentException e) {
                deleteResult = -1;
                Log.e(TAG, "IccContactsDeleteThread(): account name = " + simAccountName + ", deleteResult = " + deleteResult);
            }

            if(deleteResult!=-1)
            {
                    try {
                    if (DBG) Log.d(TAG,"IccContactsDeleteThread()=>delete group simid = " + mSimId);

                    deleteResult=getContentResolver().delete(groupUriIsSyncAdapter, Groups.ACCOUNT_TYPE + "=?" + " AND " + Groups.ACCOUNT_NAME + "=?",
                                                                        new String[] {ICC_CONTACTS_ACCOUNT_TYPE, simAccountName});
                } catch (IllegalArgumentException e) {
                    deleteResult = -1;
                    Log.e(TAG, "IccContactsDeleteThread(): group account name = " + simAccountName + ", deleteResult = " + deleteResult);
                }
            }

//            Account simAccount = new Account(simAccountName, ICC_CONTACTS_ACCOUNT_TYPE);
//            AccountManager.get(mContext).removeAccount(simAccount, null, null);
//            Log.d(TAG, "IccContactsDeleteThread(): removeAccount: " + simAccount);

            handler.sendMessage(handler.obtainMessage(MSG_PHONEBOOK_DATABASE_DELTED, deleteResult, 0));

            if (DBG) Log.d(TAG,"IccContactsDeleteThread():run()=> simid = " + mSimId);
        }
    }

    private class IccContactsLoaderThread extends Thread {
        private SimCardID mSimId;

        private List<Long> IccGroupIdList = new ArrayList<Long>();
        private Context mContext;
        public IccContactsLoaderThread(Context context, SimCardID simId) {
            super("IccContactsLoaderThread");
            mContext = context;
            mSimId = simId;
        }

        @Override
        public void run() {
            if (DBG) Log.d(TAG,"IccContactsLoaderThread()=>run() simid = " + mSimId);

            int result = 0;

            QueryHandler handler;
            if (mSimId==SimCardID.ID_ONE) {
                handler = mIccContactsQueryHandler1;
            } else {
                handler = mIccContactsQueryHandler0;
            }

            try {
                result = queryIccContacts();
            } catch (Exception e) {
                Log.e(TAG,"icc contacts query error! simid = " + mSimId + ", exception = " + e);
                result = 0;
            } finally {
                handler.sendMessage(handler.obtainMessage(MSG_CONTACTS_END_LOADING, result, 0));
            }

            if (DBG) Log.d(TAG,"IccContactsLoaderThread():run()=> simid = " + mSimId);
        }

        private boolean queryIccGroupsAndInsertToDataBase() {
            Uri uri;
            Cursor iccGroupsCursor;
            String simAccountName;
            String[] lines;
            int maxGroupSupportNum = 0;
            IccGroupIdList.clear();
            String capabilityInfo;


            if (mSimId == SimCardID.ID_ONE) {
                capabilityInfo = mIccCapabilityInfo1;
            } else {
                capabilityInfo = mIccCapabilityInfo0;
            }

            if (TextUtils.isEmpty(capabilityInfo)) {
                Log.e(TAG, "queryIccGroupsAndInsertToDataBase(): capabilityInfo = " + capabilityInfo);
                return false;
            }

            lines = capabilityInfo.split(",");
            if ((14 != lines.length) || (TextUtils.isEmpty(lines[11]))) {
                Log.e(TAG, "queryIccGroupsAndInsertToDataBase(): Error parsing: capabilityInfo["+mSimId.toInt()+"] = " + capabilityInfo);
                return false;
            }

            try {
                maxGroupSupportNum = Integer.valueOf(lines[11]);
            } catch (NumberFormatException e) {
                Log.e(TAG, "queryIccGroupsAndInsertToDataBase(): NumberFormatException: capabilityInfo["+mSimId.toInt()+"] = " + capabilityInfo);
                return false;
            }

            if (DBG) Log.d(TAG, "queryIccGroupsAndInsertToDataBase() Maximum support Group Num = " + maxGroupSupportNum);

            if (maxGroupSupportNum > 0)
            {
                //should get number from query capability
                for(int i=0;i<maxGroupSupportNum;i++)
                {
                    IccGroupIdList.add(i,Long.valueOf(-1));
                }

                if (mSimId==SimCardID.ID_ONE) {
                    uri = Uri.parse("content://icc2/adn/group");
                    simAccountName = "SIM2";
                } else {
                    uri = Uri.parse("content://icc/adn/group");
                    simAccountName = "SIM1";
                }

                iccGroupsCursor = getContentResolver().query(uri, GROUP_COLUMN_NAME, null, null, null);

                if (null != iccGroupsCursor ) {
                    while (iccGroupsCursor.moveToNext()) {
                        String groupTitle = iccGroupsCursor.getString(0);
                        String iccGroupIdStr = iccGroupsCursor.getString(1);
                        int iccGroupId;
                        try {
                            iccGroupId = Integer.valueOf(iccGroupIdStr);
                        } catch (NumberFormatException e) {
                            iccGroupId = -1;
                        }
                        if (iccGroupId < IccGroupIdList.size()) {
                            ContentValues values = new ContentValues();
                            values.put(Groups.ACCOUNT_TYPE, ICC_CONTACTS_ACCOUNT_TYPE);
                            values.put(Groups.ACCOUNT_NAME, simAccountName);
                            values.put(Groups.DATA_SET,simAccountName);
                            values.put(Groups.SOURCE_ID, iccGroupIdStr);
                            values.put(Groups.TITLE, groupTitle);

                            if (DBG) Log.d(TAG, "queryIccGroupsAndInsertToDataBase(): iccGroupId = " + iccGroupIdStr + ", group name = " + groupTitle);

                            final ContentResolver resolver = getContentResolver();

                            // Create the new group
                            final Uri groupUri = resolver.insert(Groups.CONTENT_URI, values);

                            // If there's no URI, then the insertion failed. Abort early because group members can't be
                            // added if the group doesn't exist
                            if (groupUri == null) {
                                Log.e(TAG, "queryIccGroupsAndInsertToDataBase()(): Couldn't create group with label: " + groupTitle);
                                return false;
                            }
                            if (DBG) Log.d(TAG, "queryIccGroupsAndInsertToDataBase(): groupUri = " + groupUri);

                            IccGroupIdList.set(iccGroupId, ContentUris.parseId(groupUri));
                        } else {
                            Log.e(TAG, "queryIccGroupsAndInsertToDataBase(): iccGroupId = " + iccGroupId + ", IccGroupIdList.size() = " + IccGroupIdList.size());
                        }
                    }

                    if (!iccGroupsCursor.isClosed()) {
                        iccGroupsCursor.close();
                    }
                    iccGroupsCursor = null;
                }
            } else {
                Log.d(TAG, "queryIccGroupsAndInsertToDataBase(): simid = " + mSimId.toInt() + ", Group NOT support");
                return false;
            }

            return true;
        }

        private int queryIccContacts() {
            Uri uri;
            Cursor iccContactsCursor;
            int returnValue = 0;
            if (mSimId==SimCardID.ID_ONE) {
                uri = Uri.parse("content://icc2/adn");
            } else {
                uri = Uri.parse("content://icc/adn");
            }

            if (mSimId == SimCardID.ID_ONE) {
                mIccCapabilityInfo1 = getIccCardCapabilityInfo(getContentResolver(), mSimId, false);
            } else {
                mIccCapabilityInfo0 = getIccCardCapabilityInfo(getContentResolver(), mSimId, false);
            }
            broadcastSimReadyIntent();

            // we must query IccGroup before query Icc contact ,otherwise contact group array will be empty
            queryIccGroupsAndInsertToDataBase();

            if (DBG) Log.d(TAG,"IccContactsLoaderThread():run()=>queryIccContacts() simid = " + mSimId);
            iccContactsCursor = getContentResolver().query(uri, COLUMN_NAMES, null, null, null);
            if (DBG) Log.d(TAG,"IccContactsLoaderThread():run():queryIccContacts()=> simid = " + mSimId);

            if (null != iccContactsCursor) {
                returnValue = onQueryComplete(iccContactsCursor);

                if (!iccContactsCursor.isClosed()) {
                    iccContactsCursor.close();
                }
                iccContactsCursor = null;
            }

            return returnValue;
        }

        protected int onQueryComplete(Cursor c) {
            if (DBG) Log.d(TAG,"onQueryComplete(): cursor = " + c + ", simid = " + mSimId);
            int inserted = 0;

            if (null != c) {
                String simAccountName;
                if (mSimId==SimCardID.ID_ONE) {
                    simAccountName = "SIM2";
                    mIccContactsQueryHandler1.mAccountRemoved = false;
                } else {
                    simAccountName = "SIM1";
                    mIccContactsQueryHandler0.mAccountRemoved = false;
                }
                Account simAccount = new Account(simAccountName, ICC_CONTACTS_ACCOUNT_TYPE);
                final boolean addResult = AccountManager.get(mContext).addAccountExplicitly(simAccount, null, null);
                if (DBG) Log.d(TAG,"onQueryComplete(): addAccountExplicitly(): = " + addResult + ", simId = " + mSimId);
                ContentResolver.setSyncAutomatically(simAccount, ContactsContract.AUTHORITY, true);

                int iSimContactCount = c.getCount();
                if (DBG) Log.d(TAG,"onQueryComplete(): cursor size = " + iSimContactCount);

                if (iSimContactCount > 0) {
                    TelephonyManager tm;
                    if (mSimId==SimCardID.ID_ONE) {
                        tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE2);
                    } else {
                        tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE1);
                    }

                    if (TelephonyManager.SIM_STATE_READY == tm.getSimState()) {
                        inserted = insertIccContactsToContactDatabase(c, getContentResolver());
                    }

                    if (0 == inserted) {
                        Log.e(TAG,"onQueryComplete():Failed to insert to phonebook database. sim id = " + mSimId);
                    }
                } else {
                    inserted = 1;
                }
            }

            if (DBG) Log.d(TAG,"onQueryComplete()=> simid = " + mSimId);

            return inserted;
        }

        private int insertIccContactsToContactDatabase(Cursor c, ContentResolver resolver) {
            if (DBG) Log.d(TAG,"=>insertIccContactsToContactDatabase() simId = " + mSimId);
            int rawContactInsertIndex = 0;
            int columnCount;
            String name;
            String number;
            String allEmails;
            String allANRs;
            String allGroups;
            ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
            boolean exceptionOccurs = false;

            if (DBG) Log.d(TAG, "insertIccContactsToContactDatabase(): Total SIM Contacts = " + c.getCount());

            String accountName = "SIM1";
            if (mSimId == SimCardID.ID_ONE) {
                accountName = "SIM2";
            }

            c.moveToFirst();
            do {
                columnCount = 0;

                if (DBG) Log.d(TAG,"insertIccContactsToContactDatabase(): INDEX = " + c.getString(5));

                // insert record
                ops.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                        .withValue(RawContacts.AGGREGATION_MODE, RawContacts.AGGREGATION_MODE_DISABLED)
                        .withValue(RawContacts.DELETED, 0)
                        .withValue(RawContacts.ACCOUNT_TYPE, ICC_CONTACTS_ACCOUNT_TYPE)
                        .withValue(RawContacts.ACCOUNT_NAME, accountName)
                        .withValue(RawContacts.DATA_SET, accountName)
                        .withValue(RawContacts.SOURCE_ID, c.getString(5))
                        .withValue(RawContacts.VERSION, 1)
                        .withValue(RawContacts.DIRTY, 0)
                        .withValue(RawContacts.SYNC1, null)
                        .withValue(RawContacts.SYNC2, null)
                        .withValue(RawContacts.SYNC3, null)
                        .withValue(RawContacts.SYNC4, null)
                        .withValues(new ContentValues())
                        .build());

                // insert photo
                byte[] photoIcon;

                if (mSimId == SimCardID.ID_ONE) {
                    if (null == mPhotoIcon1) {
                        mPhotoIcon1 = createIccContactIcon();
                    }
                    photoIcon = mPhotoIcon1;
                } else {
                    if (null == mPhotoIcon0) {
                        mPhotoIcon0 = createIccContactIcon();
                    }
                    photoIcon = mPhotoIcon0;
                }

                if (null != photoIcon) {
                    if (DBG) Log.d(TAG, "insertIccContactsToContactDatabase(): [" + rawContactInsertIndex + "].photo");

                    ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                            .withValueBackReference(Photo.RAW_CONTACT_ID, rawContactInsertIndex)
                            .withValue(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE)
                            .withValue(Photo.PHOTO, photoIcon)
                            .withValue(Data.IS_SUPER_PRIMARY, 1)
                            .build());

                    columnCount++;
                }

                // insert name
                name = c.getString(0);
                if (!TextUtils.isEmpty(name)) {
                    if (DBG) Log.d(TAG, "insertIccContactsToContactDatabase(): [" + rawContactInsertIndex + "].name=" + name);

                    ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                            .withValueBackReference(StructuredName.RAW_CONTACT_ID, rawContactInsertIndex)
                            .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                            .withValue(StructuredName.DISPLAY_NAME, name)
                            .build());

                    columnCount++;
                }

                // insert number
                number = c.getString(1);
                if (!TextUtils.isEmpty(number)) {
                    if (DBG) Log.d(TAG, "insertIccContactsToContactDatabase(): [" + rawContactInsertIndex + "].number=" + number);

                    ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                            .withValueBackReference(Phone.RAW_CONTACT_ID, rawContactInsertIndex)
                            .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                            .withValue(Phone.TYPE, Phone.TYPE_MOBILE)
                            .withValue(Phone.NUMBER, number)
                            .withValue(Data.IS_PRIMARY, 1)
                            .build());

                    columnCount++;
                }

                // insert allEmails
                allEmails = c.getString(2);
                if (!TextUtils.isEmpty(allEmails)) {
                    String[] emailAddressArray;
                    emailAddressArray = allEmails.split(",");
                    for (String emailAddress : emailAddressArray) {
                        if (!TextUtils.isEmpty(emailAddress)) {
                            if (DBG) Log.d(TAG, "insertIccContactsToContactDatabase(): [" + rawContactInsertIndex + "].email=" + emailAddress);

                            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                                    .withValueBackReference(Email.RAW_CONTACT_ID, rawContactInsertIndex)
                                    .withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
                                    .withValue(Email.TYPE, Email.TYPE_MOBILE)
                                    .withValue(Email.DATA, emailAddress)
                                    .build());

                            columnCount++;
                        }
                    }
                }

                // insert ANR
                allANRs = c.getString(3);
                if (!TextUtils.isEmpty(allANRs)) {
                    String[] anrAddressArray;
                    anrAddressArray = allANRs.split(",");
                    for (String anr : anrAddressArray) {
                        if (!TextUtils.isEmpty(anr)) {
                            if (DBG) Log.d(TAG, "insertIccContactsToContactDatabase(): [" + rawContactInsertIndex + "].anr=" + anr);

                            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                                    .withValueBackReference(Phone.RAW_CONTACT_ID, rawContactInsertIndex)
                                    .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                                    .withValue(Phone.TYPE, Phone.TYPE_HOME)
                                    .withValue(Phone.NUMBER, anr)
                                    .withValue(Data.IS_PRIMARY, 0)
                                    .build());

                            columnCount++;
                        }
                    }
                }

                // insert Groups
                allGroups = c.getString(4);
                if (!TextUtils.isEmpty(allGroups)) {
                    Log.d(TAG, "insertIccContactsToContactDatabase(): allGroups = " + allGroups);
                    String[] GroupArray;
                    GroupArray = allGroups.split(",");
                    for (String iccGroupIdStr : GroupArray) {
                        try {
                            int iccGroupId = Integer.valueOf(iccGroupIdStr);
                            if (DBG) Log.d(TAG, "insertIccContactsToContactDatabase(): [" + rawContactInsertIndex + "].group=" + iccGroupId);
                            if (iccGroupId < IccGroupIdList.size()) {
                                long groupId = IccGroupIdList.get(iccGroupId);
                                Log.d(TAG, "insertIccContactsToContactDatabase(): groupId =" + groupId);
                                if (0 <= groupId) {
                                    ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                                            .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
                                            .withValue(Data.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE)
                                            .withValue(GroupMembership.GROUP_ROW_ID, groupId)
                                            .build());

                                    columnCount++;
                                } else {
                                    Log.e(TAG, "insertIccContactsToContactDatabase(): error groupId = " + groupId);
                                }
                            } else {
                                Log.e(TAG, "insertIccContactsToContactDatabase(): IccGroupIdList.size() = " + IccGroupIdList.size());
                            }
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "insertIccContactsToContactDatabase(): Empty iccGroupIdStr: " + iccGroupIdStr);
                        }
                    }
                }

                rawContactInsertIndex += (1 + columnCount);

                if (240 < ops.size()) {
                    // To avoid TransactionTooLargeException
                    try {
                        resolver.applyBatch(ContactsContract.AUTHORITY, ops);
                    } catch (OperationApplicationException e) {
                        exceptionOccurs = true;
                        Log.e(TAG,"applyBatch(): OperationApplicationException: " + e.toString() + ". message = " + e.getMessage());
                    } catch (RemoteException e) {
                        exceptionOccurs = true;
                        Log.e(TAG,"applyBatch(): RemoteException: " + e.toString() + ". message = " + e.getMessage());
                    } catch (IllegalArgumentException e) {
                        exceptionOccurs = true;
                        Log.e(TAG,"applyBatch(): IllegalArgumentException: " + e.toString() + ". message = " + e.getMessage());
                    }

                    if (exceptionOccurs) {
                        break;
                    } else {
                        ops.clear();
                        rawContactInsertIndex = 0;
                    }
                }
            } while(c.moveToNext());

            if ((!exceptionOccurs) && (0 < ops.size())) {
                try {
                    resolver.applyBatch(ContactsContract.AUTHORITY, ops);
                } catch (OperationApplicationException e) {
                    exceptionOccurs = true;
                    Log.e(TAG,"applyBatch(): OperationApplicationException: " + e.toString() + ". message = " + e.getMessage());
                } catch (RemoteException e) {
                    exceptionOccurs = true;
                    Log.e(TAG,"applyBatch(): RemoteException: " + e.toString() + ". message = " + e.getMessage());
                } catch (IllegalArgumentException e) {
                    exceptionOccurs = true;
                    Log.e(TAG,"applyBatch(): IllegalArgumentException: " + e.toString() + ". message = " + e.getMessage());
                }
            }

            if (DBG) Log.d(TAG,"insertIccContactsToContactDatabase()=> simId = " + mSimId);

            if (exceptionOccurs)
                return 0;
            else
                return 1;
        }

        private byte[] createIccContactIcon() {
            Bitmap photo;

            if (mSimId == SimCardID.ID_ONE) {
                photo = BitmapFactory.decodeResource(getResources(),R.drawable.ic_sim2_picture);
            } else {
                photo = BitmapFactory.decodeResource(getResources(),R.drawable.ic_sim1_picture);
            }

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            photo.compress(Bitmap.CompressFormat.JPEG, 75, stream);
            return stream.toByteArray();
        }
    }

    private class QueryHandler extends AsyncQueryHandler {
        private SimCardID mSimId;
        private IccContactsState mState;
        private boolean mSimReadyForReading;
        private final Object mLock = new Object();
        private int mRetry;
        private Context mContext;
        public boolean mAccountRemoved = false;

        public QueryHandler(Context context, ContentResolver cr, SimCardID simId) {
            super(cr);
            mContext = context;
            mSimId = simId;
            mState = IccContactsState.NOT_READY;
            mSimReadyForReading = false;
            mRetry = 0;
            mAccountRemoved = false;
        }

        public IccContactsState getState() {
            synchronized (mLock) {
                return mState;
            }
        }

        public IccContactsState getAnotherSimState() {
            IccContactsState anotherSimState;
            if (mSimId == SimCardID.ID_ONE) {
                anotherSimState = mIccContactsQueryHandler0.getState();
            } else if (mSimId == SimCardID.ID_ZERO) {
                anotherSimState = mIccContactsQueryHandler1.getState();
            } else {
                Log.e(TAG, "getAnotherSimState(): error simid : " + mSimId);
                anotherSimState = IccContactsState.NOT_READY;
            }
            if (DBG) Log.d(TAG,"getAnotherSimState(): mSimId = " + mSimId + ", anotherSimState = " + anotherSimState);

            return anotherSimState;
        }

        private void deleteSimAccount() {
            if (!mAccountRemoved) {
                String simAccountName;
                if (mSimId == SimCardID.ID_ONE) {
                    simAccountName = "SIM2";
                } else {
                    simAccountName = "SIM1";
                }
                Account simAccount = new Account(simAccountName, ICC_CONTACTS_ACCOUNT_TYPE);
                AccountManager.get(mContext).removeAccount(simAccount, null, null);
                Log.d(TAG, "deleteSimAccount(): removeAccount: " + simAccount);
                mAccountRemoved = true;
            }
        }

        private void handleSimLoaded() {
            if (DBG) Log.d(TAG,"=>handleSimLoaded(): mState = " + mState + ", mSimId = " + mSimId);

            if (hasMessages(MSG_SIM_STATE_NOT_READY)) {
                if (DBG) Log.w(TAG, "handleSimLoaded(): hasMessages(MSG_SIM_STATE_NOT_READY) , mSimId = " + mSimId);
                return;
            }

            mSimReadyForReading = true;

            if (hasMessages(MSG_SIM_STATE_LOADED)) {
                removeMessages(MSG_SIM_STATE_LOADED);
            }
            if (hasMessages(MSG_WAIT_TIMEOUT)) {
                removeMessages(MSG_WAIT_TIMEOUT);
            }

            synchronized (mLock) {
                switch(mState) {
                    case NOT_READY:
                        deleteSimAccount();
                        if (getAnotherSimState() != IccContactsState.DELETING) {
                            mState = IccContactsState.DELETING;
                            IccContactsDeleteThread deleteThread = new IccContactsDeleteThread(mContext, mSimId);
                            deleteThread.start();
                        } else {
                            if (DBG) Log.d(TAG, "handleSimLoaded(): Another SIM is deleting. Wait...");
                            sendEmptyMessageDelayed(MSG_WAIT_TIMEOUT, WAIT_DELETION); // wait while another SIM is deleting
                        }
                        break;
                    case DELETING:
                        // Do nothing. handleDeleteDone() will check mSimReadyForReading to load SIM contacts
                        break;
                    case DELETED:
                        if (getAnotherSimState() != IccContactsState.SIM_QUERY_AND_ADD) {
                            mState = IccContactsState.SIM_QUERY_AND_ADD;
                            IccContactsLoaderThread loaderThread = new IccContactsLoaderThread(mContext, mSimId);
                            loaderThread.start();
                        } else {
                            if (DBG) Log.d(TAG, "handleSimLoaded(): Another SIM is loading. Wait...");
                            sendEmptyMessageDelayed(MSG_WAIT_TIMEOUT, WAIT_LOADING); // wait while another SIM is loading
                        }
                        break;
                    case SIM_QUERY_AND_ADD:
                        Log.e(TAG, "handleSimLoaded(): Invalid State SIM_QUERY_AND_ADD");
                        break;
                    case LOADED:
                        Log.e(TAG, "handleSimLoaded(): Invalid State LOADED");
                        break;
                    default:
                        Log.e(TAG, "handleSimLoaded(): Invalid State UNKNOWN");
                        break;
                }
            }

            if (DBG) Log.d(TAG,"handleSimLoaded()=> mState = " + mState + ", mSimId = " + mSimId);
        }

        private void handleSimNotReady() {
            if (DBG) Log.d(TAG,"=>handleSimNotReady(): mState = " + mState + ", mSimId = " + mSimId);

            if (mSimReadyForReading) {
                mSimReadyForReading = false;
                if (mSimId == SimCardID.ID_ONE) {
                    mIccCapabilityInfo1 = "";
                } else {
                    mIccCapabilityInfo0 = "";
                }
                broadcastSimReadyIntent();
            }

            if (hasMessages(MSG_WAIT_TIMEOUT)) {
                removeMessages(MSG_WAIT_TIMEOUT);
            }

            mRetry = 0;
            synchronized (mLock) {
                switch(mState) {
                    case NOT_READY:
                        deleteSimAccount();
                        if (getAnotherSimState() != IccContactsState.DELETING) {
                            mState = IccContactsState.DELETING;
                            IccContactsDeleteThread deleteThread = new IccContactsDeleteThread(mContext, mSimId);
                            deleteThread.start();
                        } else {
                            if (DBG) Log.d(TAG, "handleSimNotReady(): Another SIM is deleting. Wait...");
                            sendEmptyMessageDelayed(MSG_WAIT_TIMEOUT, WAIT_DELETION); // wait while another SIM is deleting
                        }
                        break;
                    case DELETING:
                        // Do nothing. handleDeleteDone() will check mSimReadyForReading
                        break;
                    case DELETED:
                        // Do nothing.
                        break;
                    case SIM_QUERY_AND_ADD:
                        // Do nothing. handleEndLoading() will check mSimReadyForReading
                        break;
                    case LOADED:
                        deleteSimAccount();
                        if (getAnotherSimState() != IccContactsState.DELETING) {
                            mState = IccContactsState.DELETING;
                            IccContactsDeleteThread deleteThread = new IccContactsDeleteThread(mContext, mSimId);
                            deleteThread.start();
                        } else {
                            if (DBG) Log.d(TAG, "handleSimNotReady(): Another SIM is deleting. Waiting...");
                            mState = IccContactsState.NOT_READY;
                            sendEmptyMessageDelayed(MSG_WAIT_TIMEOUT, WAIT_DELETION); // wait while another SIM is deleting
                        }
                        break;
                    default:
                        break;
                }
            }

            if (DBG) Log.d(TAG,"handleSimNotReady()=> mState = " + mState + ", mSimId = " + mSimId);
        }

        private void handleDeleteDone(Message msg) {
            if (DBG) Log.d(TAG,"=>handleDeleteDone(): mState = " + mState + ", mSimId = " + mSimId + ", result = " + msg.arg1);
            synchronized (mLock) {
                if (mState == IccContactsState.DELETING) {
                    if (0 > msg.arg1) {
                        Log.e(TAG, "handleEndLoading(): Fail to delete icc contacts. Retry after 30 seconds...");
                        mState = IccContactsState.NOT_READY;
                        sendEmptyMessageDelayed(MSG_WAIT_TIMEOUT, WAIT_ERROR_DELETION);
                    } else {
                        if (mSimReadyForReading) {
                            if (getAnotherSimState() != IccContactsState.SIM_QUERY_AND_ADD) {
                                mState = IccContactsState.SIM_QUERY_AND_ADD;
                                IccContactsLoaderThread loaderThread = new IccContactsLoaderThread(mContext, mSimId);
                                loaderThread.start();
                            } else {
                                if (DBG) Log.d(TAG, "handleDeleteDone(): Another SIM is loading. Wait...");
                                mState = IccContactsState.DELETED;
                                sendEmptyMessageDelayed(MSG_WAIT_TIMEOUT, WAIT_LOADING); // wait while another SIM is loading
                            }
                        } else {
                            mState = IccContactsState.DELETED;
                        }
                    }
                } else {
                    Log.e(TAG, "handleDeleteDone(): Invalid State");
                }
            }
            if (DBG) Log.d(TAG,"handleDeleteDone()=> mState = " + mState + ", mSimId = " + mSimId);
        }

        private void handleEndLoading(Message msg) {
            if (DBG) Log.d(TAG,"=>handleEndLoading(): mState = " + mState + ", mSimId = " + mSimId);
            if (DBG) Log.d(TAG,"=>handleEndLoading(): mSimReadyForReading = " + mSimReadyForReading + ", result = " + msg.arg1 + ", mRetry = " + mRetry);
            synchronized (mLock) {
                if (mState == IccContactsState.SIM_QUERY_AND_ADD) {
                    if (!mSimReadyForReading) {
                        if (getAnotherSimState() != IccContactsState.DELETING) {
                            deleteSimAccount();
                            mState = IccContactsState.DELETING;
                            IccContactsDeleteThread deleteThread = new IccContactsDeleteThread(mContext, mSimId);
                            deleteThread.start();
                        } else {
                            if (DBG) Log.d(TAG, "handleEndLoading(): Another SIM is deleting. Waiting...");
                            mState = IccContactsState.NOT_READY;
                            sendEmptyMessageDelayed(MSG_WAIT_TIMEOUT, WAIT_DELETION); // wait while another SIM is deleting
                        }
                    } else {
                        if (0 == msg.arg1) {
                            mRetry++;
                            if (mRetry <= 3) {
                                deleteSimAccount();
                                mState = IccContactsState.DELETING;
                                IccContactsDeleteThread deleteThread = new IccContactsDeleteThread(mContext, mSimId);
                                deleteThread.start();
                            } else {
                                Log.e(TAG, "handleEndLoading(): end retry");
                                mState = IccContactsState.NOT_READY;
                            }
                        } else {
                            mState = IccContactsState.LOADED;
                            new Thread(new Runnable() {
                                public void run() {
                                    if (mSimId == SimCardID.ID_ONE) {
                                        mIccCapabilityInfo1 = getIccCardCapabilityInfo(getContentResolver(), mSimId, true);
                                    } else {
                                        mIccCapabilityInfo0 = getIccCardCapabilityInfo(getContentResolver(), mSimId, true);
                                    }
                                    broadcastSimReadyIntent();
                                }
                            }).start();
                        }
                    }
                } else {
                    Log.e(TAG, "handleEndLoading(): Invalid State");
                }
            }

            if (DBG) Log.d(TAG,"handleEndLoading()=> mState = " + mState + ", mSimId = " + mSimId);
        }

        private void handleWaitTimeout() {
            if (DBG) Log.d(TAG,"=>handleWaitTimeout(): mState = " + mState + ", mSimId = " + mSimId);

            synchronized (mLock) {
                switch(mState) {
                    case NOT_READY:
                        deleteSimAccount();
                        if (getAnotherSimState() != IccContactsState.DELETING) {
                            mState = IccContactsState.DELETING;
                            IccContactsDeleteThread deleteThread = new IccContactsDeleteThread(mContext, mSimId);
                            deleteThread.start();
                        } else {
                            if (DBG) Log.d(TAG, "handleWaitTimeout(): Another SIM is deleting. Wait...");
                            sendEmptyMessageDelayed(MSG_WAIT_TIMEOUT, WAIT_DELETION); // wait while another SIM is deleting
                        }
                        break;
                    case DELETING:
                        // Do nothing. handleDeleteDone() will check mSimReadyForReading to load SIM contacts or not
                        break;
                    case DELETED:
                        if (mSimReadyForReading) {
                            if (getAnotherSimState() != IccContactsState.SIM_QUERY_AND_ADD) {
                                mState = IccContactsState.SIM_QUERY_AND_ADD;
                                IccContactsLoaderThread loaderThread = new IccContactsLoaderThread(mContext, mSimId);
                                loaderThread.start();
                            } else {
                                if (DBG) Log.d(TAG, "handleDeleteDone(): Another SIM is loading. Wait...");
                                sendEmptyMessageDelayed(MSG_WAIT_TIMEOUT, WAIT_LOADING); // wait while another SIM is loading
                            }
                        }
                        break;
                    case SIM_QUERY_AND_ADD:
                        Log.e(TAG, "handleWaitTimeout(): Invalid State SIM_QUERY_AND_ADD");
                        break;
                    case LOADED:
                        Log.e(TAG, "handleWaitTimeout(): Invalid State LOADED");
                        break;
                    default:
                        break;
                }
            }

            if (DBG) Log.d(TAG,"handleWaitTimeout()=> mState = " + mState + ", mSimId = " + mSimId);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_SIM_STATE_LOADED:
                handleSimLoaded();
                break;
            case MSG_SIM_STATE_NOT_READY:
                handleSimNotReady();
                break;
            case MSG_PHONEBOOK_DATABASE_DELTED:
                handleDeleteDone(msg);
                break;
            case MSG_CONTACTS_END_LOADING:
                handleEndLoading(msg);
                break;
            case MSG_WAIT_TIMEOUT:
                handleWaitTimeout();
                break;
            default:
                super.handleMessage(msg);
                break;
            }
        }
    }

    private String getIccCardCapabilityInfo(ContentResolver resolver, SimCardID simId, boolean loaded) {
        Uri uri;
        Cursor iccInfoCursor;
        String iccInfo;
        StringBuilder buf = new StringBuilder();

        if (loaded) {
            buf.append(1);
        } else {
            buf.append(0);
        }

        if (SimCardID.ID_ONE == simId) {
            uri = Uri.parse("content://icc2/adn/info");
        } else {
            uri = Uri.parse("content://icc/adn/info");
        }

       iccInfoCursor = resolver.query(uri, INFO_COLUMN_NAMES, null, null, null);
        if (null != iccInfoCursor) {
            if (iccInfoCursor.moveToFirst()) {
                for (int i=0;i<13;i++) {
                    String infoStr = "";
                    int num = 0;
                    try {
                        infoStr = iccInfoCursor.getString(i);
                        num = Integer.valueOf(infoStr);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "getIccCardCapabilityInfo(): NumberFormatException. " + i + ": " + infoStr);
                        num = 0;
                    }

                    buf.append(",");
                    buf.append(num);
                }
            }

            if (!iccInfoCursor.isClosed()) {
                iccInfoCursor.close();
            }
            iccInfoCursor = null;
        }

        iccInfo = buf.toString();

        if (DBG) Log.d(TAG,"getIccCardCapabilityInfo(): simId = " + simId + ", iccInfo = " + iccInfo);

        return iccInfo;
    }
}
