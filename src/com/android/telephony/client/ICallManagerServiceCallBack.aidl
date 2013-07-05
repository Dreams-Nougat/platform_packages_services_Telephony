/*
 * Copyright (C) 2013 The Android Open Source Project
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


package com.android.telephony.client;

import android.graphics.Bitmap;

oneway interface ICallManagerServiceCallBack {

    void onElapsedTimeUpdated(String elapsedTime);

    void onSoundRouted(int audioRoute);

    void onMicMuteStateChange(boolean isMuted);

    void onIncomming(boolean isWaiting);

    void onOutgoing(boolean isAlerting);

    void onHold(boolean isVoiceMail, boolean isConference);

    void onActive(boolean hasBackgroundCalls, boolean isEmergency, boolean isVoiceMail, boolean isConference);

    void onDisconnecting();

    void onAllCallsDisconnected(int cause);

    void onForegroundCallerInfoUpdated(String name, String number, String typeofnumber, in Bitmap photo, int presentation);

    void onBackgroundCallerInfoUpdated(String name, String number, String typeofnumber, in Bitmap photo, int presentation);
}
