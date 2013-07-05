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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class CallManager {
    private static final String TAG = "CallManager";
    private static final boolean DBG = true;

    private Context mContext;

    private static final String ACTION_START_TELEPHONY_SERVICE =
            "android.intent.action.TELEPHONY_SERVICE";
    private ICallManagerService mCallManagerService = null;

    private CallManagerListener mListener;

    private final ServiceConnection mCallManagerServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            log("Call service connected");

            try {
                mCallManagerService = ICallManagerService.Stub.asInterface(service);

                if (mListener != null) {
                    mCallManagerService.registerCallback(mCallBack);

                    // TODO: This is just temporary to temporary solve a pretty obvious issue
                    // in the UI when demoing. Eventually the telephony service should send
                    // out call backs of the current state and information to be shown when
                    // a new client connects.
                    // Sometimes CallerInfo query have been completed before this connection
                    // been setup, then CallManagerService can't notify this client(InCallUI)
                    // the CallerInfo in time, this case only appears during the first call,
                    // to avoid the problem, request another query once this connection
                    // been setup.
                    mCallManagerService.requestForegroundCallerInfo(mCallBack);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (mListener != null) {
                // UI status in call service is not correct due to
                // this connection is connected after resume. The UI status
                // set to call service is lost. Add here!
                mListener.onCallManagerServiceConnected();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            log("call service disconnected");
            mCallManagerService = null;
        }
    };

    /**
     * This implementation is used to receive callbacks from CallManagerService
     */
    private ICallManagerServiceCallBack mCallBack = new ICallManagerServiceCallBack.Stub() {
        public void onSoundRouted(int audioRoute) {
            mListener.onSoundRouted(audioRoute);
        }

        public void onElapsedTimeUpdated(String elapsedTime) {
            mListener.onElapsedTimeUpdated(elapsedTime);
        }

        public void onMicMuteStateChange(boolean newMuteState) {
            mListener.onMicMuteStateChange(newMuteState);
        }

        public void onIncomming(boolean isWaiting) {
            mListener.onIncomming(isWaiting);
        }

        public void onOutgoing(boolean isAlerting) {
            mListener.onOutgoing(isAlerting);
        }

        public void onActive(boolean hasBackgroundCalls, boolean isEmergency,
                boolean isVoiceMail, boolean isConference) {
            mListener.onActive(hasBackgroundCalls, isEmergency, isVoiceMail, isConference);
        }

        public void onHold(boolean isVoiceMail, boolean isConference) {
            mListener.onHold(isVoiceMail, isConference);
        }

        public void onDisconnecting() {
            mListener.onDisconnecting();
        }

        public void onAllCallsDisconnected(int cause) {
            mListener.onAllCallsDisconnected(cause);
        }

        public void onForegroundCallerInfoUpdated(String name, String number,
                String typeofnumber, Bitmap photo, int presentation) {
            mListener.onForegroundCallerInfoUpdated(name, number, typeofnumber, photo,
                    presentation);
        }

        public void onBackgroundCallerInfoUpdated(String name, String number,
                String typeofnumber, Bitmap photo, int presentation) {
            mListener.onBackgroundCallerInfoUpdated(name, number, typeofnumber, photo,
                    presentation);
        }
    };

    public CallManager(Context context, CallManagerListener listener) {
        if (context == null) {
            Log.i(TAG, "new CallManager, context should not be null.");
            throw(new IllegalArgumentException());
        }
        mContext = context;

        // Bind to telephony service.
        Intent serviceIntent = new Intent(ACTION_START_TELEPHONY_SERVICE);
        mContext.bindService(serviceIntent, mCallManagerServiceConnection, Context.BIND_AUTO_CREATE);

        mListener = listener;
    }

    public void placeCall(Intent intent) {
        if(DBG) log("PLACE CALL");
        if (mCallManagerService != null) {
            try {
                mCallManagerService.placeCall(intent);
            } catch (RemoteException re) {
                log("answerCall failed");
            }
        }
    }

    public void answerCall() {
        if(DBG) log("answerCall called");
        if (mCallManagerService != null) {
            try {
                mCallManagerService.answerCall();
            } catch (RemoteException re) {
                log("answerCall failed");
            }
            log("mCallManagerService is null, ingore answerCall");
        }
    }

    public void hangupCall() {
        if(DBG) log("hangupCall called");
        if (mCallManagerService != null) {
            try {
                mCallManagerService.hangupCall();
            } catch (RemoteException re) {
                log("hangupCall failed");
            }
            log("mCallManagerService is null, ingore hangupCall");
        }
    }

    public void retrieveCall() {
        if(DBG) log("retrieveCall called");
        if (mCallManagerService != null) {
            try {
                mCallManagerService.retrieveCall();
            } catch (RemoteException re) {
                log("retrieveCall failed");
            }
            log("mCallManagerService is null, ingore retrieveCall");
        }
    }

    public void holdCall() {
        if(DBG) log("holdCall called");
        if (mCallManagerService != null) {
            try {
                mCallManagerService.holdCall();
            } catch (RemoteException re) {
                log("holdCall failed");
            }
            log("mCallManagerService is null, ingore holdCall");
        }
    }

    public void muteMic() {
        if(DBG) log("muteMic called");
        if (mCallManagerService != null) {
            try {
                mCallManagerService.muteMic();
            } catch (RemoteException re) {
                log("muteMic failed");
            }
            log("mCallManagerService is null, ingore muteMic");
        }
    }

    public int getSoundRoute () {
        if(DBG) log("getSoundRoute called");
        if (mCallManagerService != null) {
            try {
                return mCallManagerService.getSoundRoute();
            } catch (RemoteException re) {
                log("getSoundRoute failed");
            }
            log("mCallManagerService is null, ingore getSoundRoute");
        }
        return 0;
    }

    public void routeSound(int soundRoute) {
        if(DBG) log("routeSound called");
        if (mCallManagerService != null) {
            try {
                mCallManagerService.routeSound(soundRoute);
            } catch (RemoteException re) {
                log("routeSound failed");
            }
            log("mCallManagerService is null, ingore routeSound");
        }
    }

    public void startDtmf(char c) {
        if(DBG) log("startDtmf called");
        if (mCallManagerService != null) {
            try {
                mCallManagerService.startDtmf(c);
            } catch (RemoteException re) {
                log("startDtmf failed");
            }
            log("mCallManagerService is null, ingore startDtmf");
        }
    }

    public void stopDtmf() {
        if(DBG) log("stopDtmf called");
        if (mCallManagerService != null) {
            try {
                mCallManagerService.stopDtmf();
            } catch (RemoteException re) {
                log("stopDtmf failed");
            }
            log("mCallManagerService is null, ingore stopDtmf");
        }
    }

    public void callUIActived(boolean active) {
        if (mCallManagerService != null) {
            try {
                mCallManagerService.callUIActived(active);
            } catch (RemoteException e) {
                log("callUIActived failed");
            }
            log("mCallManagerService is null, ingore callUIActived");
        }
    }

    public void clean() {
        if (mCallManagerService != null) {
            try {
                mCallManagerService.unregisterCallback(mCallBack);
            } catch (RemoteException e) {
                // There is nothing special we need to do if the service
                // has crashed.
            }
        }
        mContext.unbindService(mCallManagerServiceConnection);
    }

    // TODO for future develop, if no need of listener list, then delete this.
    public void addListener() {
    }

    private void log (String message) {
        Log.i(TAG, message);
    }

    /**
     * TODO: This currently just mirrors the AIDL interface which is not correct and the AIDL
     * interface is not final either and needs updates.
     */
    public interface CallManagerListener {
        public void onCallManagerServiceConnected();
        public void onSoundRouted(int audioRoute);
        public void onElapsedTimeUpdated(String elapsedTime);
        public void onMicMuteStateChange(boolean newMuteState);
        public void onIncomming(boolean isWaiting);
        public void onOutgoing(boolean isAlerting);
        public void onActive(boolean hasBackgroundCalls, boolean isEmergency,
                boolean isVoiceMail, boolean isConference);
        public void onHold(boolean isVoiceMail, boolean isConference);
        public void onDisconnecting();
        public void onAllCallsDisconnected(int cause);
        public void onForegroundCallerInfoUpdated(String name, String number,
                String typeofnumber, Bitmap photo, int presentation);
        public void onBackgroundCallerInfoUpdated(String name, String number,
                String typeofnumber, Bitmap photo, int presentation);
    }
}
