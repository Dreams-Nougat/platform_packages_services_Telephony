/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.app.TabActivity;

import static com.android.internal.telephony.MSimConstants.SUBSCRIPTION_KEY;
import static com.android.internal.telephony.MSimConstants.SUB_SETTING;

public class SelectSubscription extends  TabActivity {

    private static final String LOG_TAG = "SelectSubscription";
    private static final boolean DBG = (PhoneGlobals.DBG_LEVEL >= 2);

    public static final String PACKAGE = "PACKAGE";
    public static final String TARGET_CLASS = "TARGET_CLASS";

    private String[] tabLabel = {"SUB 1", "SUB 2", "SUB 3"};

    private TabSpec subscriptionPref;

    @Override
    public void onPause() {
        super.onPause();
    }

    /*
     * Activity class methods
     */
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (DBG) log("Creating activity");

        setContentView(R.layout.multi_sim_setting);

        TabHost tabHost = getTabHost();

        Intent intent =  getIntent();
        String pkg = intent.getStringExtra(PACKAGE);
        String targetClass = intent.getStringExtra(TARGET_CLASS);
        boolean isSub = intent.getBooleanExtra(SUB_SETTING, false);

        int numPhones = TelephonyManager.getDefault().getPhoneCount();

        for (int i = 0; i < numPhones; i++) {
            log("Creating SelectSub activity = " + i);
            subscriptionPref = tabHost.newTabSpec(tabLabel[i]);
            subscriptionPref.setIndicator(tabLabel[i]);
            intent = new Intent().setClassName(pkg, targetClass)
                    .setAction(intent.getAction()).putExtra(SUBSCRIPTION_KEY, i);
            intent.putExtra(SUB_SETTING, isSub);
            subscriptionPref.setContent(intent);
            tabHost.addTab(subscriptionPref);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

}
