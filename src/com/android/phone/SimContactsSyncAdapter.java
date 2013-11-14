/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.phone;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

/**
 * SyncAdapter implementation for syncing Sim SyncAdapter contacts to the
 * platform ContactOperations provider.
 */
public class SimContactsSyncAdapter extends AbstractThreadedSyncAdapter {
    private static final boolean DBG = true;
    private static final String TAG = "SimContactsSyncAdapter";

    public SimContactsSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        if (DBG) Log.d(TAG, "=>SimContactsSyncAdapter()");
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
        ContentProviderClient provider, SyncResult syncResult) {
        if (DBG) Log.d(TAG, "=>onPerformSync(): account = " + account + ", authority = " + authority);
    }
}
