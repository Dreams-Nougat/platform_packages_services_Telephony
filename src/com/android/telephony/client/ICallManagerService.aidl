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

import android.content.Intent;
import android.graphics.Bitmap;
import com.android.telephony.client.ICallManagerServiceCallBack;

interface ICallManagerService {

    void registerCallback(ICallManagerServiceCallBack cb);

    void unregisterCallback(ICallManagerServiceCallBack cb);

    void placeCall(in Intent intent);

    void answerCall();

    void hangupCall();

    /**
     * TODO: Can we merge this with hangupCall and only have one function?
     */
    void hangupRingingCall();

    boolean answerAndEndActive();

    void holdCall();

    void retrieveCall();

    /**
     * Switches currently active call with background hold call.
     */
    void switchCalls();

    void mergeCalls();

    /**
     * Separates a call from a conference. I.E the call separated will be the new active call and
     * the conference will be put on hold.
     */
    void separateCall(int connectionId);

    /**
     * Hangups specificed connection in the currently active conference call.
     */
    void hangupConnection(int connectionId);

    /**
     * Silence the ringsingnal in ongoing call.
     */
    void silenceRinger();

    /**
     * Route sound to speaker, earspeaker or bluetooth headset.
     *
     * @param audioRoute 0 = speaker, 1 = bluetooth, 2 = earpiece.
     */
    void routeSound(int audioRoute);

    /**
     * Get the current sound route.
     *
     * @return 0 = speaker, 1 = bluetooth, 2 = earpiece.
     */
    int getSoundRoute();

    /**
     * This interface should be called in the onResume/onPause function of call activity
     * to notify call service to update the status which will be used by proximity sensor etc.
     *
     * TODO This should have another name. It is not really UI related. It is about the proximity
     * sensor in some UI cases behaves differently.. so this is more about the proximity sensor
     * than the UI.. so it should have name related to the proximity sensor.
     */
    void callUIActived(boolean active);

    /**
     * Toggles mute of microphone on and off.
     *
     * TODO: Change this fucntion name to a better suiting name.
     */
    void muteMic();

    void startDtmf(char c);

    void stopDtmf();

    void requestForegroundCallerInfo(ICallManagerServiceCallBack cb);

    void requestBackgroundCallerInfo(ICallManagerServiceCallBack cb);

    void requestConferenceCallersInfo(ICallManagerServiceCallBack cb);
}
