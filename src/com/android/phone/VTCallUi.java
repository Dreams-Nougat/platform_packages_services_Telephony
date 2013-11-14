/*
 * Copyright (C) 2009-2010 Broadcom Corporation
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

import android.os.Handler;
import android.app.AlertDialog;
import android.view.View;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.content.DialogInterface;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.graphics.drawable.BitmapDrawable;
import android.widget.Toast;
import android.os.Environment;
import android.os.StatFs;
import android.util.AttributeSet;
import android.util.Log;
import java.util.Date;
import java.text.SimpleDateFormat;


import java.io.File;

import com.android.internal.telephony.Call;
//import com.rv.videophone.*;//TODO: JB_DISABLE_VT

public class VTCallUi extends FrameLayout implements CallTime.OnTickListener, View.OnClickListener {
//    private static final String LOG_TAG = VTCallUi.class.getName();
//    private static final boolean DBG = (PhoneGlobals.DBG_LEVEL >= 1);
//
//    private static final int STORAGE_STATUS_OK = 0;
//    private static final int STORAGE_STATUS_LOW = 1;
//    private static final int STORAGE_STATUS_NONE = 2;
//
//    private static final long NO_STORAGE_ERROR = -1L;
//    private static final long CANNOT_STAT_ERROR = -2L;
//    private static final long LOW_STORAGE_THRESHOLD = 2048L * 1024L;
//
//    private static final int MENU_GROUP_VIDEO_CALL = 100;
//    private static final int MENU_GROUP_VIDEO_CALL_REC_START = 101;
//    private static final int MENU_GROUP_VIDEO_CALL_REC_STOP = 102;
//    private static final int MENU_GROUP_VIDEO_CALL_SEND_START = 103;
//    private static final int MENU_GROUP_VIDEO_CALL_SEND_STOP = 104;
//    private static final int MENU_TO_BLOCKVIDEO = 1;
//    private static final int MENU_TO_RECEIVER = 2;
//    private static final int MENU_TO_RECORDING = 3;
//    private static final int MENU_TO_SENDFILE = 4;
//    private static final int MENU_TO_SWITCHCAME = 5;
//    private static final int MENU_TO_CONFIGURE = 6;
//    private static final int MENU_TO_SWITCHVIDEO = 7;
//    private static final int MENU_TO_STOPRECORDING = 8;
//    private static final int MENU_TO_STOPSENDING = 9;
//
//
//    //switch video
//    private static final int MENU_SV_S_REMOTE_B_LOCAL = 10;
//    private static final int MENU_SV_B_REMOTE_S_LOCAL = 11;
//    private static final int MENU_SV_ONLY_B_LOCAL = 12;
//    private static final int MENU_SV_ONLY_B_REMOTE = 13;
//    //send file
//    private static final int MENU_SendFile_VIDEO = 14;
//    private static final int MENU_SendFile_JPEG = 15;
//    private static final int MENU_SendFile_BMP = 16;
//
//    //recording submenu
//    private static final int MENU_RECLOCAL = 17;
//    private static final int MENU_RECREMOTE = 18;
//
//    private Context mContext = null;
//    private PhoneGlobals mApplication = null;
//    private InCallScreen mInCallScreen = null;
//    private PhoneStateListener mStateListener = null;
//    private UserInputListener mUserInputListener = null;
//    private int mWndMode = VideoPhoneDataBean.RVVideoPhone_WINDOWS_B_REMOTE_S_LOCAL;
//    private String mLastAlterValue = "0";
//    private int mCallStatus = VideoPhoneDataBean.RVVideoPhone_Call_UNKNOWN;
//
//    private int mRecordOption=0;
//    private int mSendOption=0;
//
//    private AlertDialog mToggleVideoDlg = null;
//
//    //private EditText mInput;
//    //private TextView mOtherStatus;
//    private TextView mElapsedTime;
//    private CallTime mCallTime;
//    private View mVTCalUIView=null;
//    private Menu mVTmenu = null;
//
//    public static final String VD_EXTDIR = "/vt";
//    public static VTLocalDisplay mLocalDisplay = null;
//    public static VTRemoteDisplay mRemoteDisplay = null;

    public VTCallUi (Context context, AttributeSet attrs) {
        super(context, attrs);

        //if(!VTFramework.getVTFramework().IsVTEnable()) {//TODO: JB_DISABLE_VT
            return;
        //}

//        if (DBG) Log.d (LOG_TAG, "VTCallUi constructor...");
//        if (DBG) Log.d (LOG_TAG, "- this = " + this);
//        if (DBG) Log.d (LOG_TAG, "- context " + context + ", attrs " + attrs);
//
//        mContext = context;
//        mApplication = PhoneGlobals.getInstance();
//        mCallTime = new CallTime(this);
//
//        // Inflate our contents, and add it (to ourself) as a child.
//        LayoutInflater inflater = LayoutInflater.from(context);
//
//        inflater.inflate(
//            R.layout.rv_incall_ui,  // resource
//            this,                      // root
//            true);
//
//
//        setVisibility(View.GONE);
//
//        VTFramework vt = PhoneUtils.getVTFramework();
//
//        if (null != vt) {
//            mStateListener = new PhoneStateListener(this);
//            if (null == mStateListener) Log.e(LOG_TAG, "create PhoneStaterListener error.");
//            vt.registerCallStateChangeListener(mStateListener);
//
//            mUserInputListener = new UserInputListener(this);
//            if (null == mUserInputListener) Log.e(LOG_TAG, "create UserInputListener error.");
//            vt.registerUserInputListener(mUserInputListener);
//
//            vt.registerVTRecord(PhoneUtils.getVTRecord(vt));
//            vt.registerVTAlternative(PhoneUtils.getVTAlternative(vt));
//        }
    }



//    public void setInCallScreenInstance(InCallScreen inCallScreen) {
//        mInCallScreen = inCallScreen;
//    }
//
//    @Override
//    protected void onFinishInflate() {
//        super.onFinishInflate();
//        if (DBG) Log.d (LOG_TAG, "vtCallUi onFinishInflate(this = " + this + ")...");
//        //mInput = (EditText) findViewById(R.id.ed_TransferNum);
//        //mOtherStatus = (TextView) findViewById(R.id.otherCallStatus);
//        //mElapsedTime = (TextView) findViewById(R.id.elapsedTime);
//    }

//    private class PhoneStateListener implements VTCallStateListener {
//        private VTCallUi vtCallUi;
//
//        public PhoneStateListener(VTCallUi vcu) {
//            vtCallUi = vcu;
//        }
//
//        public void onVTCallStateChanged(VTFramework vt, int callId, int state, int reason) {
//
//            if(null == vt) return;
//            Log.d(LOG_TAG, "onVTCallStateChanged(), state="+state+", callId="+callId+", reason="+reason);
//            switch (state) {
//            case VideoPhoneDataBean.RVVideoPhone_Call_IncomingCall: {
//                Log.d(LOG_TAG, "RVVideoPhone_Call_IncomingCall");
//                VideoPhoneDataBean.isIncomingCall = 1;
//                VideoPhoneDataBean.callId = callId;
//                vt.startCameraPreview();
//            }
//            break;
//            case VideoPhoneDataBean.RVVideoPhone_Call_CS_INCOM: {
//                Log.d(LOG_TAG, "RVVideoPhone_Call_CS_INCOM");
//                VideoPhoneDataBean.isIncomingCall = 1;
//                //make sure startCameraPreview after surface created
//                vt.startCameraPreview();
//            }
//            break;
//            case VideoPhoneDataBean.RVVideoPhone_Call_Connected: {
//                Log.d(LOG_TAG, "RVVideoPhone_Call_Connected");
//                VideoPhoneDataBean.callId = callId;
//                VideoPhoneDataBean.isIncomingCall = 0;
//                Log.d(LOG_TAG, "VT Call Connected to startCameraPreview()");
//                vt.startCameraPreview();
//                vtCallUi.PrepareStartVideoCall();
//                //popup an window to ask about send local video sendLocalVideo(vt);
//            }
//            break;
//            case VideoPhoneDataBean.RVVideoPhone_Call_MultimediaRingCC: {
//                VideoPhoneDataBean.callId = callId;
//                VideoPhoneDataBean.isIncomingCall = 0;
//            }
//            break;
//            case VideoPhoneDataBean.RVVideoPhone_Call_Disconnected: {
//                VideoPhoneDataBean.callId = callId;
//            }
//            break;
//            case VideoPhoneDataBean.RVVideoPhone_Call_IDLE: {
//                VideoPhoneDataBean.callId = callId;
//                VideoPhoneDataBean.isIncomingCall = 0;
//            }
//            break;
//            case VideoPhoneDataBean.RVVideoPhone_Call_CS_PROGRESS_INFO_IND: {
//                Log.d(LOG_TAG, "RVVideoPhone_Call_CS_PROGRESS_INFO_IND");
//                //make sure startCameraPreview after surface created
//                vt.startCameraPreview();
//            }
//            break;
//            case VideoPhoneDataBean.RVVideoPhone_Call_CS_END: {
//                Log.d(LOG_TAG, "Had received Call End Message from VT Framework.");
//                vtCallUi.PrepareStopVideoCall();
//                vt.stopCameraPreview();
//                vt.releaseCamera();
//                VideoPhoneDataBean.isIncomingCall = 0;
//                /*popup an window to ask about send local video
//                					if (null != mToggleVideoDlg) {
//                						mToggleVideoDlg.dismiss();
//                						mToggleVideoDlg = null;
//                					}
//                */
//            }
//            default:
//                break;
//            }
//            if (state == VideoPhoneDataBean.RVVideoPhone_Call_Connected
//                    || state == VideoPhoneDataBean.RVVideoPhone_Call_CS_CONNECT) {
//
//                //vtCallUi.setVisibility(View.VISIBLE);
//
//                //if(state == VideoPhoneDataBean.RVVideoPhone_Call_CS_CONNECT){
//                Log.d(LOG_TAG, "VT Call CS_CONNECT/Connected, Prepare to Show VT UI");
//                Handler VTHandler = new Handler();
//                Runnable  UIVisibleEvent = new Runnable() {
//                    public void run() {
//                        Log.d(LOG_TAG, "VT UI Set VISIBLE");
//                        vtCallUi.setVisibility(View.VISIBLE);
//                    }
//                };
//                VTHandler.postDelayed(UIVisibleEvent, 500);
//                //}
//
//                if (null != mInCallScreen) {
//                    mInCallScreen.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//                }
//
//                if (!mCallTime.isTraceRunning()) {
//                    mCallTime.setActiveCallMode(mApplication.phone[mApplication.getCallScreenActiveSimCardId().toInt()].getForegroundCall());
//                    mCallTime.reset();
//                    mCallTime.periodicUpdateTimer();
//                }
//            } else if (state == VideoPhoneDataBean.RVVideoPhone_Call_Disconnected
//                       || state == VideoPhoneDataBean.RVVideoPhone_Call_CS_END) {
//
//                vtCallUi.setVisibility(View.INVISIBLE);
//
//                if (null != mInCallScreen) {
//                    mInCallScreen.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//                }
//
//                if (mCallTime.isTraceRunning()) {
//                    mCallTime.reset();
//                    mCallTime.cancelTimer();
//                }
//
//                resetState();
//            }
//            vtCallUi.setCallStatus(state);
//        }
//    }

//    private class UserInputListener implements VTUserInputListener {
//        private VTCallUi vtCallUi;
//
//        public UserInputListener(VTCallUi vcu) {
//            vtCallUi = vcu;
//        }
//
//        public void onUserInput(String userInput) {
//        }
//
//        public void onAscInput(int userInput) {
//        }
//
//        public void onNonStandardUserInput(byte[] object, byte[] data) {
//            if (null == PhoneUtils.getVTFramework()) return;
//
//            // do something using nsUII data..
//            if (object != null && data != null) {
//                Log.d(LOG_TAG, "UserInputListener receive nsUII:" +
//                      "object[" + object.length + "] " + object + ",data[" + data.length + "] " + data);
//            }
//        }
//    }


    public boolean onCreateOptionsMenu (Menu menu) {
//        //init menu items
//        menu.add(MENU_GROUP_VIDEO_CALL, MENU_TO_BLOCKVIDEO,  Menu.NONE, R.string.PauseVideo)
//        .setIcon(android.R.drawable.ic_menu_add);
//
//
//        menu.add(MENU_GROUP_VIDEO_CALL, MENU_TO_SWITCHCAME,  Menu.NONE, R.string.SwitchCamera)
//        .setIcon(android.R.drawable.ic_menu_add);
//
//        //menu.add(MENU_GROUP_VIDEO_CALL, MENU_TO_CONFIGURE,  Menu.NONE, R.string.Config)
//        //        .setIcon(android.R.drawable.ic_menu_add);
//
//
//        //add recording submenu
//        SubMenu recMenu=menu.addSubMenu(MENU_GROUP_VIDEO_CALL_REC_START, MENU_TO_RECORDING, Menu.NONE, R.string.RecordFile)
//                        .setIcon(android.R.drawable.ic_menu_add);
//        recMenu.add(MENU_GROUP_VIDEO_CALL_REC_START, MENU_RECLOCAL, Menu.NONE, R.string.RecLocal);
//        recMenu.add(MENU_GROUP_VIDEO_CALL_REC_START, MENU_RECREMOTE, Menu.NONE, R.string.RecRemote);
//
//        menu.add(MENU_GROUP_VIDEO_CALL_REC_STOP, MENU_TO_STOPRECORDING,  Menu.NONE, R.string.StopRecording)
//        .setIcon(android.R.drawable.ic_menu_add);
//
//        menu.setGroupVisible(MENU_GROUP_VIDEO_CALL_REC_STOP, false);
//
//        //add switch video submenu
//        SubMenu switchVideoMenu=menu.addSubMenu(MENU_GROUP_VIDEO_CALL, MENU_TO_SWITCHVIDEO, Menu.NONE, R.string.SwitchVideo)
//                                .setIcon(android.R.drawable.ic_menu_add);
//        switchVideoMenu.add(MENU_GROUP_VIDEO_CALL, MENU_SV_S_REMOTE_B_LOCAL, Menu.NONE, R.string.SVSRBL);
//        switchVideoMenu.add(MENU_GROUP_VIDEO_CALL, MENU_SV_B_REMOTE_S_LOCAL, Menu.NONE, R.string.SVSLBR);
//        switchVideoMenu.add(MENU_GROUP_VIDEO_CALL, MENU_SV_ONLY_B_LOCAL, Menu.NONE, R.string.SVOnlyBL);
//        switchVideoMenu.add(MENU_GROUP_VIDEO_CALL, MENU_SV_ONLY_B_REMOTE, Menu.NONE, R.string.SVOnlyBR);
//
//        //add send file submenu
//        SubMenu sendFileMenu=menu.addSubMenu(MENU_GROUP_VIDEO_CALL_SEND_START, MENU_TO_SENDFILE,  Menu.NONE, R.string.SendFile)
//                             .setIcon(android.R.drawable.ic_menu_add);
//        sendFileMenu.add(MENU_GROUP_VIDEO_CALL_SEND_START, MENU_SendFile_VIDEO, Menu.NONE, R.string.SendFileVideo);
//        sendFileMenu.add(MENU_GROUP_VIDEO_CALL_SEND_START, MENU_SendFile_JPEG, Menu.NONE, R.string.SendFileJPEG);
//        sendFileMenu.add(MENU_GROUP_VIDEO_CALL_SEND_START, MENU_SendFile_BMP, Menu.NONE, R.string.SendFileBMP);
//
//        menu.add(MENU_GROUP_VIDEO_CALL_SEND_STOP, MENU_TO_STOPSENDING,  Menu.NONE, R.string.StopSending)
//        .setIcon(android.R.drawable.ic_menu_add);
//
//        menu.setGroupVisible(MENU_GROUP_VIDEO_CALL_SEND_STOP, false);

        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu, Intent intent) {
//        //Dymatically change menu items
//        //check now, which speaker or receiver is using.
//        int status = getCallStatus();
//
//        Log.d(LOG_TAG, "onPrepareOptionsMenu for vt ");
//
//        if (null != intent &&
//                (status == VideoPhoneDataBean.RVVideoPhone_Call_Connected
//                 || status == VideoPhoneDataBean.RVVideoPhone_Call_CS_CONNECT)/* VideoPhone&&
//			intent.getBooleanExtra(VTOutgoingCallBroadcaster.EXTRA_VTPHONE,	false) */) {
//
//
//            VTRecord vtRecord = PhoneUtils.getVTRecord(PhoneUtils.getVTFramework());
//
//            if (null == vtRecord) return false;
//
//            if(vtRecord.isRecord()) {
//                menu.setGroupVisible(MENU_GROUP_VIDEO_CALL_REC_STOP, true);
//                menu.setGroupVisible(MENU_GROUP_VIDEO_CALL_REC_START, false);
//
//
//            } else {
//                menu.setGroupVisible(MENU_GROUP_VIDEO_CALL_REC_STOP, false);
//                menu.setGroupVisible(MENU_GROUP_VIDEO_CALL_REC_START, true);
//            }
//
//            MenuItem VTSwitchCameraItem = null;
//            VTSwitchCameraItem = menu.findItem(MENU_TO_SWITCHCAME);
//            VTSwitchCameraItem.setEnabled(true);
//            MenuItem VTPauseVideoItem = null;
//            VTPauseVideoItem = menu.findItem(MENU_TO_BLOCKVIDEO);
//            VTPauseVideoItem.setEnabled(true);
//
//            MenuItem VTSendFileItem = null;
//            VTSendFileItem = menu.findItem(MENU_TO_SENDFILE);
//            VTSendFileItem.setEnabled(true);
//            MenuItem VTStopFileItem = null;
//            VTStopFileItem = menu.findItem(MENU_TO_STOPSENDING);
//            VTStopFileItem.setEnabled(true);
//
//            if(PhoneUtils.getVTAlternative(PhoneUtils.getVTFramework()).isRun()) {
//                if(VTSwitchCameraItem != null) {
//                    VTSwitchCameraItem.setEnabled(false);
//                }
//
//                if(VTPauseVideoItem != null) {
//                    VTPauseVideoItem.setEnabled(false);
//                }
//
//                CompoundButton VTHiddenButton = null;
//                VTHiddenButton = (CompoundButton)mInCallScreen.findViewById(R.id.vthiddenButton);
//
//                if(VTHiddenButton != null) {
//                    if(VTHiddenButton.isChecked()) {
//                        menu.findItem(MENU_TO_SWITCHCAME);
//                        menu.setGroupVisible(MENU_GROUP_VIDEO_CALL_SEND_START, true);
//                        VTSendFileItem.setEnabled(false);
//                        menu.setGroupVisible(MENU_GROUP_VIDEO_CALL_SEND_STOP, false);
//                    } else {
//                        menu.setGroupVisible(MENU_GROUP_VIDEO_CALL_SEND_START, false);
//                        menu.setGroupVisible(MENU_GROUP_VIDEO_CALL_SEND_STOP, true);
//                        VTStopFileItem.setEnabled(true);
//                    }
//                }
//            } else {
//                if(VTSwitchCameraItem != null) {
//                    VTSwitchCameraItem.setEnabled(true);
//                }
//                menu.setGroupVisible(MENU_GROUP_VIDEO_CALL_SEND_START, true);
//                VTSendFileItem.setEnabled(true);
//                menu.setGroupVisible(MENU_GROUP_VIDEO_CALL_SEND_STOP, false);
//            }
//
//            menu.setGroupVisible(MENU_GROUP_VIDEO_CALL, true);
//
//            if(!VTPauseVideoItem.getTitle().equals(mContext.getString(R.string.PauseVideo))) {
//                VTSwitchCameraItem.setEnabled(false);
//                VTSendFileItem.setEnabled(false);
//                VTStopFileItem.setEnabled(false);
//            }
//        } else {
//            menu.setGroupVisible(MENU_GROUP_VIDEO_CALL, false);
//            menu.setGroupVisible(MENU_GROUP_VIDEO_CALL_REC_START, false);
//            menu.setGroupVisible(MENU_GROUP_VIDEO_CALL_REC_STOP, false);
//            menu.setGroupVisible(MENU_GROUP_VIDEO_CALL_SEND_START, false);
//            menu.setGroupVisible(MENU_GROUP_VIDEO_CALL_SEND_STOP, false);
//        }
//
//        mVTmenu = menu;
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
//        //can add other ids
//
//
//        VTFramework vt = PhoneUtils.getVTFramework();
//
//        if (null == vt) {
//            Log.e(LOG_TAG, "onOptionsItemSelected vt null");
//            return true;
//        }
//
//        //String value = mInput.getText().toString();
//
//        //if (DBG) Log.i(LOG_TAG,"menu selected id=" + item.getItemId()
//        //+ ", value=" + value);
//
//        switch (item.getItemId()) {
//        case MENU_TO_BLOCKVIDEO: {
//            ImageButton VTSwitchCameraButton = null;
//            VTSwitchCameraButton = (ImageButton)mInCallScreen.findViewById(R.id.vtswitchcameraButton);
//            CompoundButton VTHiddenButton = null;
//            VTHiddenButton = (CompoundButton)mInCallScreen.findViewById(R.id.vthiddenButton);
//
//            if(item.getTitle().equals(mContext.getString(R.string.PauseVideo))) {
//                item.setTitle(R.string.ResumeVideo);
//
//                if(VTSwitchCameraButton != null) {
//                    VTSwitchCameraButton.setEnabled(false);
//                }
//                if(VTHiddenButton != null) {
//                    VTHiddenButton.setEnabled(false);
//                }
//            } else {
//                item.setTitle(R.string.PauseVideo);
//                if(VTSwitchCameraButton != null) {
//                    VTSwitchCameraButton.setEnabled(true);
//                }
//                if(VTHiddenButton != null) {
//                    VTHiddenButton.setEnabled(true);
//                }
//            }
//            //add receiver function
//            otherCallToggleBlockVideo(vt);
//            if (DBG) Log.w(LOG_TAG,"Pause/Resume local video");
//        }
//        break;
//
//        case MENU_TO_STOPRECORDING:
//            if(mRecordOption==MENU_RECLOCAL) {
//                otherCallOnRecording(vt, "0");
//            } else if(mRecordOption==MENU_RECREMOTE) {
//                otherCallOnRecording(vt, "3");
//            }
//            mRecordOption=0;
//            break;
//
//        case MENU_RECLOCAL: {
//            //show recording format selection
//            Toast.makeText (mApplication, "Local Recording", 3000).show();
//            otherCallOnRecording(vt, "0");
//            mRecordOption=MENU_RECLOCAL;
//            if (DBG) Log.w(LOG_TAG,"recording");
//        }
//        break;
//
//        case MENU_RECREMOTE: {
//
//            //show recording format selection
//            Toast.makeText (mApplication, "Remote Recording", 3000).show();
//            otherCallOnRecording(vt, "3");
//            mRecordOption=MENU_RECREMOTE;
//            if (DBG) Log.w(LOG_TAG,"recording");
//        }
//        break;
//
//        case MENU_TO_STOPSENDING:
//        case MENU_SendFile_JPEG:
//        case MENU_SendFile_VIDEO:
//        case MENU_SendFile_BMP:
//            int option;
//            MenuItem VTSwitchCameraItem = null;
//            VTSwitchCameraItem = mVTmenu.findItem(MENU_TO_SWITCHCAME);
//            ImageButton VTSwitchCameraButton = null;
//            VTSwitchCameraButton = (ImageButton)mInCallScreen.findViewById(R.id.vtswitchcameraButton);
//            CompoundButton VTHiddenButton = null;
//            VTHiddenButton = (CompoundButton)mInCallScreen.findViewById(R.id.vthiddenButton);
//
//            if(item.getItemId()== MENU_TO_STOPSENDING) {
//                option=mSendOption;
//            } else {
//                option=item.getItemId();
//            }
//
//            switch (option) {
//            case MENU_SendFile_VIDEO: {
//                //show send file dialog list
//                otherCallOnPlay(vt, "0");
//                if (DBG) Log.w(LOG_TAG,"video");
//
//            }
//            break;
//
//            case MENU_SendFile_JPEG: {
//                //show send file dialog list
//                otherCallOnPlay(vt, "1");
//                if (DBG) Log.w(LOG_TAG,"JPEG");
//            }
//            break;
//
//
//            case MENU_SendFile_BMP: {
//                //show send file dialog list
//                otherCallOnPlay(vt, "2");
//                if (DBG) Log.w(LOG_TAG,"BMP");
//
//            }
//            break;
//            }
//
//            if(item.getItemId()==MENU_TO_STOPSENDING) {
//                mSendOption=0;
//                if(VTSwitchCameraItem != null) {
//                    VTSwitchCameraItem.setEnabled(true);
//                }
//                if(VTSwitchCameraButton != null) {
//                    VTSwitchCameraButton.setEnabled(true);
//                }
//                if(VTHiddenButton != null) {
//                    VTHiddenButton.setEnabled(true);
//                }
//            } else {
//                mSendOption=item.getItemId();
//                if(VTSwitchCameraItem != null) {
//                    VTSwitchCameraItem.setEnabled(false);
//                }
//                if(VTSwitchCameraButton != null) {
//                    VTSwitchCameraButton.setEnabled(false);
//                }
//                if(VTHiddenButton != null) {
//                    VTHiddenButton.setEnabled(false);
//                }
//            }
//
//            break;
//
//        case MENU_TO_SWITCHCAME: {
//            //switch front or back camera
//            otherCallOnSwitchCameraIcon(vt);
//            if (DBG) Log.w(LOG_TAG,"switch camera");
//        }
//        break;
//
//        //case MENU_TO_CONFIGURE:
//        //	{
//        //show configuration list
//        //		otherCallOnConfigIcon(vt, value);
//        //		if (DBG) Log.w(LOG_TAG,"configuration");
//        //	}
//        //break;
//
//        //case MENU_TO_SWITCHVIDEO:
//        case MENU_SV_S_REMOTE_B_LOCAL: {
//            //show configuration list
//            Toast.makeText (mApplication, "Small Remote Big Local", 3000).show();
//            otherCallOnSwitchWndIcon(vt,VideoPhoneDataBean.RVVideoPhone_WINDOWS_B_LOCAL_S_REMOTE);
//            if (DBG) Log.w(LOG_TAG,"MENU_SV_S_REMOTE_B_LOCAL");
//        }
//        break;
//
//        case MENU_SV_B_REMOTE_S_LOCAL: {
//            //show configuration list
//            Toast.makeText (mApplication, "Small Local Big Remote", 3000).show();
//            otherCallOnSwitchWndIcon(vt,VideoPhoneDataBean.RVVideoPhone_WINDOWS_B_REMOTE_S_LOCAL);
//            if (DBG) Log.w(LOG_TAG,"MENU_SV_B_REMOTE_S_LOCAL");
//        }
//        break;
//
//
//        case MENU_SV_ONLY_B_LOCAL: {
//            //show configuration list
//            Toast.makeText (mApplication, "Only Big Local", 3000).show();
//            otherCallOnSwitchWndIcon(vt,VideoPhoneDataBean.RVVideoPhone_WINDOWS_BIG_LOCAL);
//            if (DBG) Log.w(LOG_TAG,"MENU_SV_ONLY_B_LOCAL");
//        }
//        break;
//
//        case MENU_SV_ONLY_B_REMOTE: {
//            //show configuration list
//            Toast.makeText (mApplication, "Only Big remote", 3000).show();
//            otherCallOnSwitchWndIcon(vt,VideoPhoneDataBean.RVVideoPhone_WINDOWS_BIG_REMOTE);
//            if (DBG) Log.w(LOG_TAG,"MENU_SV_ONLY_B_REMOTE");
//        }
//        break;
//        default:
//            break;
//        }
        return true;
    }

//    public int otherCallOnSwitchCameraIcon(VTFramework vt) {
//        int i = vt.getCameraId();
//        if(i == VideoPhoneDataBean.RVVideoPhone_Camera_Front) {
//            i = VideoPhoneDataBean.RVVideoPhone_Camera_Back;
//        } else {
//            i = VideoPhoneDataBean.RVVideoPhone_Camera_Front;
//        }
//        vt.switchCamera(i);
//        return 0;
//    }

//    private int otherCallOnSwitchWndIcon(VTFramework vt,int WndMode) {
//        //mWndMode++;
//        //if(mWndMode > 3)
//        //	mWndMode = 0;
//        mWndMode=WndMode;
//
//
//        PreviewVideoSurface pv = (PreviewVideoSurface) findViewById(R.id.previewSurface);
//        RemoteVideoSurface rv = (RemoteVideoSurface) findViewById(R.id.remoteSurface);
//
//        pv.setWindowMode(mWndMode);
//        rv.setWindowMode(mWndMode);
//
//        if (VideoPhoneDataBean.RVVideoPhone_WINDOWS_BIG_LOCAL == mWndMode
//                || VideoPhoneDataBean.RVVideoPhone_WINDOWS_BIG_REMOTE == mWndMode) {
//            pv.setVisibility(View.GONE);
//        } else {
//            pv.setVisibility(View.VISIBLE);
//        }
//
//        vt.switchWindows(mWndMode);
//        return 0;
//    }

//    private int otherCallOnZoomIcon(VTFramework vt, String value) {
//        if(value == null || value.length() <= 0)
//            value = "0";
//        int v = Integer.parseInt(value);
//        vt.setZoom(v);
//        return 0;
//    }

//    private int otherCallOnBrightIcon(VTFramework vt, String value) {
//        if(value == null || value.length() <= 0) {
//            value = "3";
//        }
//        int v = Integer.parseInt(value);
//        vt.setBrightness(v);
//        return 0;
//    }

//    private int otherCallOnConfigIcon(VTFramework vt, String value) {
//        if (TextUtils.isEmpty(value)) {
//            value = "00";
//        }
//
//        VTConfiguration config = VTConfiguration.getVTConfig();
//        if(value.substring(0,1).equalsIgnoreCase("0")) {
//            //int cameraid = 0;
//            //if(value.length() > 2)
//            //		  cameraid = Integer.valueOf(value.substring(1,value.length()));
//            //if(cameraid == 0)
//            //		  config.setDefaultCamera(0);
//            //else
//            //		  config.setDefaultCamera(1);
//
//            //return;
//            //System.setProperty(VTConfiguration.DEFAULTCAMERA_CFG_PATH_S,"1");
//        } else if(value.equalsIgnoreCase("10")) {
//            Log.w(LOG_TAG, "Click configuration Button to clear default pic.:");
//            String df = null;
//            config.setDefaultImage(df);
//
//        } else if(value.equalsIgnoreCase("11")) {
//            Log.w(LOG_TAG, "Click configuration Button to string default pic.:");
//            config.setDefaultImage("/data/vt/defaultpicture.jpg");
//
//        } else if(value.equalsIgnoreCase("12")) {
//            Log.w(LOG_TAG, "Click configuration Button to clear drawable default pic.:");
//            BitmapDrawable df = null;
//            config.setDrawableAsDefaultImage(df);
//
//        } else if(value.equalsIgnoreCase("13")) {
//            Log.w(LOG_TAG, "Click configuration Button to drawable default pic.:");
//            Resources res=getResources();
//            BitmapDrawable bmpDraw=(BitmapDrawable)res.getDrawable(R.drawable.ic_tab_selected_dialer);
//            config.setDrawableAsDefaultImage(bmpDraw);
//
//        } else if(value.equalsIgnoreCase("20")) {
//            Log.w(LOG_TAG, "Click configuration Button to clear block pic.:");
//            String df = null;
//            config.setBlockedImage(df);
//
//        } else if(value.equalsIgnoreCase("21")) {
//            Log.w(LOG_TAG, "Click configuration Button to string block pic.:");
//            config.setBlockedImage("/data/vt/block.jpg");
//
//        } else if(value.equalsIgnoreCase("22")) {
//            Log.w(LOG_TAG, "Click configuration Button to clear drawable block pic.:");
//            BitmapDrawable df = null;
//            config.setDrawableAsBlockedImage(df);
//
//        } else if(value.equalsIgnoreCase("23")) {
//            Log.w(LOG_TAG, "Click configuration Button to drawable block pic.:");
//            Resources res=getResources();
//            BitmapDrawable bmpDraw=(BitmapDrawable)res.getDrawable(R.drawable.ic_menu_silence_ringer);
//            config.setDrawableAsBlockedImage(bmpDraw);
//        } else if(value.equalsIgnoreCase("30")) {
//            Log.w(LOG_TAG, "Click configuration Button to stop sub.:");
//            config.stopSubstitle();
//
//        } else if(value.equalsIgnoreCase("31")) {
//            Log.w(LOG_TAG, "Click configuration Button to sub pic.:");
//            config.startSubstitle("/data/vt/sub.jpg", 12,12,40,100);
//
//        } else if(value.equalsIgnoreCase("40")) {
//            Log.w(LOG_TAG, "Click configuration Button to set priority video: 0.:");
//            config.setVideoNegotiationMode(0);
//        } else if(value.equalsIgnoreCase("41")) {
//            Log.w(LOG_TAG, "Click configuration Button to set priority video: 1.:");
//            config.setVideoNegotiationMode(1);
//        } else if(value.equalsIgnoreCase("42")) {
//            Log.w(LOG_TAG, "Click configuration Button to set priority video: 2.:");
//            config.setVideoNegotiationMode(2);
//        } else if(value.equalsIgnoreCase("43")) {
//            Log.w(LOG_TAG, "Click configuration Button to set priority video: 3.:");
//            config.setVideoNegotiationMode(3);
//        } else if(value.equalsIgnoreCase("50")) {
//            Log.w(LOG_TAG, "Click configuration Button to set priority audio: 0.:");
//            config.setAudioNegotiationMode(0);
//        } else if(value.equalsIgnoreCase("51")) {
//            Log.w(LOG_TAG, "Click configuration Button to set priority audio: 1.:");
//            config.setAudioNegotiationMode(1);
//        } else if(value.equalsIgnoreCase("52")) {
//            Log.w(LOG_TAG, "Click configuration Button to set priority audio: 2.:");
//            config.setAudioNegotiationMode(2);
//        } else if(value.equalsIgnoreCase("53")) {
//            Log.w(LOG_TAG, "Click configuration Button to set priority audio: 3.:");
//            config.setAudioNegotiationMode(3);
//        } else if(value.equalsIgnoreCase("98")) {
//            // testing for change local surface
//            vt.registerRemoteDisplay(null);
//
//            if(VTCallUi.mRemoteDisplay != null) {
//                VTCallUi.mLocalDisplay.registerHolder(VTCallUi.mRemoteDisplay.mHolder);
//                VTCallUi.mLocalDisplay.registerSurface(VTCallUi.mRemoteDisplay.mSurface);
//                vt.changeRegisterLocalDisplay(VTCallUi.mLocalDisplay);
//            }
//
//        } else if(value.equalsIgnoreCase("99")) {
//            //testing for change local surface
//            vt.registerRemoteDisplay(VTCallUi.mRemoteDisplay);
//            vt.changeRegisterLocalDisplay(VTCallUi.mLocalDisplay);
//
//        }
//
//        return 0;
//    }

    private String createName(long dateTaken) {
        Date date = new Date(dateTaken);
        SimpleDateFormat dateFormat = new SimpleDateFormat(
            "_yyyyMMdd_HHmmss");

        return dateFormat.format(date);
    }

//    private int otherCallOnRecording(VTFramework vt, String value) {
//        int st = getStorageStatus(true);
//        if(st != STORAGE_STATUS_OK) {
//            Toast.makeText (mApplication, "SD storage is too low", 3000).show();
//            return 1;
//        }
//
//        if (TextUtils.isEmpty(value))
//            value = "0";
//
//        Log.w(LOG_TAG, "Click Record Button:"+value+",len="+value.length());
//
//        VTRecord vtRecord = PhoneUtils.getVTRecord(vt);
//        long dateTaken = System.currentTimeMillis();
//        String date=createName(dateTaken);
//
//        if (null == vtRecord) return 1;
//
//        if(value == null || value.length() <= 0 || value.substring(0,1).equalsIgnoreCase("0")) {
//            if(!vtRecord.isRecord()) {
//                String recordFile = "/mnt/sdcard/vt/recordLocal"+date+".3gp";
//                vtRecord.startRecord(VTRecord.LOCAL_AUDIO_LOCALVIDEO,
//                                     recordFile,
//                                     VTRecord.RECORD_FILE_MP4);
//            } else {
//                vtRecord.stopRecord();
//            }
//            return 0;
//        }
//        if(value.substring(0,1).equalsIgnoreCase("3")) {
//            if(!vtRecord.isRecord()) {
//                String recordFile = "/mnt/sdcard/vt/recordRemote"+date+".3gp";
//                vtRecord.startRecord(VTRecord.REMOTE_AUDIO_REMOTEVIDEO,
//                                     recordFile,
//                                     VTRecord.RECORD_FILE_MP4);
//            } else {
//                vtRecord.stopRecord();
//            }
//            return 0;
//        }
//        if(value.substring(0,1).equalsIgnoreCase("1")) {
//            Log.w(LOG_TAG, "Start to record the remote picture.");
//            vtRecord.CapturePicture(VTRecord.PICTURE_MODE_REMOTE,VTRecord.PICTURE_JPEG,"/data/vt/recordRemote"+date+".jpg");
//            return 0;
//        }
//        if(value.substring(0,1).equalsIgnoreCase("2")) {
//            Log.w(LOG_TAG, "Start to record the local picture.");
//            vtRecord.CapturePicture(VTRecord.PICTURE_MODE_LOCAL,VTRecord.PICTURE_JPEG,"/data/vt/recordLocal"+date+".jpg");
//            return 0;
//        }
//        if(value.substring(0,1).equalsIgnoreCase("4")) {
//            Log.w(LOG_TAG, "Start to record the remote bmp picture.");
//            vtRecord.CapturePicture(VTRecord.PICTURE_MODE_REMOTE,VTRecord.PICTURE_BMP,"/data/vt/recordRemote"+date+".bmp");
//            return 0;
//        }
//        if(value.substring(0,1).equalsIgnoreCase("5")) {
//            Log.w(LOG_TAG, "Start to record the local picture.");
//            vtRecord.CapturePicture(VTRecord.PICTURE_MODE_LOCAL,VTRecord.PICTURE_BMP,"/data/vt/recordLocal"+date+".bmp");
//            return 0;
//        }
//
//        if(value.substring(0,1).equalsIgnoreCase("6")) {
//            Log.w(LOG_TAG, "Send Temporal Spatial Trade off Command");
//            vt.SendTemporalSpatialTradeoffCommand(1);
//            return 0;
//        }
//
//        if(value.substring(0,1).equalsIgnoreCase("7")) {
//            Log.w(LOG_TAG, "Send Temporal Spatial Trade off Command");
//            vt.SendTemporalSpatialTradeoffCommand(15);
//            return 0;
//        }
//
//
//        if(value.substring(0,1).equalsIgnoreCase("8")) {
//            Log.w(LOG_TAG, "Send Temporal Spatial Trade off Command");
//            vt.SendTemporalSpatialTradeoffCommand(30);
//            return 0;
//        }
//        return 1;
//    }

//    private int otherCallOnPlay(VTFramework vt, String value) {
//        Log.w(LOG_TAG, "Click Send Media File Button:"+value);
//        if (TextUtils.isEmpty(value))
//            value = "0";
//
//        if(value.equalsIgnoreCase("88")) {
//            String fileName = "/data/vt/testplay.3gp";
//            vt.startVideoPlayRemote(1, VTAlternative.MEDIA_FILE_MP4,fileName);
//            return 0;
//        }
//        if(value.equalsIgnoreCase("89")) {
//            vt.stopVideoPlayRemote();
//            return 0;
//        }
//
//        VTAlternative vtAlter = PhoneUtils.getVTAlternative(vt);
//
//        if (null == vtAlter) return 1;
//
//        if(!vtAlter.isRun()) {
//
//            //vt.stopCameraPreview();
//
//            VTStartAlter(vtAlter, value);
//
//        } else {
//            if(mLastAlterValue.equalsIgnoreCase(value)) {
//                vtAlter.stopAlternative(1);
//            } else {
//                vtAlter.stopAlternative(0);
//                VTStartAlter(vtAlter, value);
//            }
//
//            //if(mLastAlterValue.equalsIgnoreCase(value))
//            //		vt.startCameraPreview();
//            //else
//            //		VTStartAlter(value);
//        }
//        return 0;
//    }

//    private void VTStartAlter(VTAlternative vtAlter, String value) {
//        String fileName = "";
//        mLastAlterValue = value;
//        if(value == null || value.length() <= 0 || value.substring(0,1).equalsIgnoreCase("0")) {
//            fileName = "/data/vt/testplay.3gp";
//            vtAlter.startAlternative(VTAlternative.LOCAL_AUDIO_VIDEO,
//                                     fileName,
//                                     VTAlternative.MEDIA_FILE_MP4);
//            return;
//        }
//        if(value.substring(0,1).equalsIgnoreCase("1")) {
//            fileName = "/data/vt/testplay.jpg";
//            vtAlter.startAlternative(VTAlternative.LOCAL_PIC,
//                                     fileName,
//                                     VTAlternative.PICTURE_JPEG);
//            return;
//        }
//        if(value.substring(0,1).equalsIgnoreCase("2")) {
//            fileName = "/data/vt/testplay.bmp";
//            vtAlter.startAlternative(VTAlternative.LOCAL_PIC,
//                                     fileName,
//                                     VTAlternative.PICTURE_JPEG);
//            return;
//        }
//        if(value.substring(0,1).equalsIgnoreCase("3")) {
//            Resources res=getResources();
//            BitmapDrawable bmpDraw=(BitmapDrawable)res.getDrawable(R.drawable.ic_tab_selected_dialer);
//            vtAlter.startAlternative(VTAlternative.LOCAL_PIC,
//                                     bmpDraw);
//            return;
//        }
//        if(value.substring(0,1).equalsIgnoreCase("4")) {
//            fileName = "/data/vt/testplay.3gp";
//            vtAlter.startAlternative(VTAlternative.LOCAL_VIDEO,
//                                     fileName,
//                                     VTAlternative.MEDIA_FILE_MP4);
//            return;
//        }
//        if(value.substring(0,1).equalsIgnoreCase("5")) {
//            fileName = "/data/vt/testplay.3gp";
//            vtAlter.startAlternative(VTAlternative.LOCAL_AUDIO_VIDEO_ONCE,
//                                     fileName,
//                                     VTAlternative.MEDIA_FILE_MP4);
//            return;
//        }
//        if(value.substring(0,1).equalsIgnoreCase("6")) {
//            fileName = "/data/vt/testplay.3gp";
//            vtAlter.startAlternative(VTAlternative.LOCAL_VIDEO_ONCE,
//                                     fileName,
//                                     VTAlternative.MEDIA_FILE_MP4);
//            return;
//        }
//        if(value.substring(0,1).equalsIgnoreCase("7")) {
//            fileName = "/data/vt/testplay.3gp";
//            vtAlter.startAlternative(VTAlternative.LOCAL_VIDEO_NO_SEND,
//                                     fileName,
//                                     VTAlternative.MEDIA_FILE_MP4);
//            return;
//        }
//    }

//    private int otherCallToggleBlockVideo(VTFramework vt) {
//        if(!vt.hasVTCallConnected()) {
//            //videophone            setStatus(VideoPhoneDataBean.RVVideoPhone_NOCallError);
//            return VideoPhoneDataBean.RVVideoPhone_NOCallError;
//        }
//
//        int jniReturn = -99;
//
//        View v = findViewById(R.id.previewSurface);
//
//        if(vt.isBlock()) {
//            jniReturn = vt.unblockVideoCall(vt.getCallID());
//            //v.setVisibility(View.VISIBLE);
//        } else {
//            jniReturn = vt.blockVideoCall(vt.getCallID());
//            //v.setVisibility(View.GONE);
//        }
//
//        return jniReturn;
//    }

//    private int getStorageStatus(boolean mayHaveSd) {
//        long remaining = mayHaveSd ? getAvailableStorage() : NO_STORAGE_ERROR;
//        if (remaining == NO_STORAGE_ERROR) {
//            return STORAGE_STATUS_NONE;
//        }
//        return remaining < LOW_STORAGE_THRESHOLD
//               ? STORAGE_STATUS_LOW
//               : STORAGE_STATUS_OK;
//    }

//    private static long getAvailableStorage() {
//        try {
//            if (!hasStorage(true)) {
//                return NO_STORAGE_ERROR;
//            } else {
//                String storageDirectory =
//                    Environment.getExternalStorageDirectory().toString();
//                StatFs stat = new StatFs(storageDirectory);
//                return (long) stat.getAvailableBlocks()
//                       * (long) stat.getBlockSize();
//            }
//        } catch (RuntimeException ex) {
//            // if we can't stat the filesystem then we don't know how many
//            // free bytes exist. It might be zero but just leave it
//            // blank since we really don't know.
//            return CANNOT_STAT_ERROR;
//        }
//    }

//    public static boolean hasStorage(boolean requireWriteAccess) {
//        String state = Environment.getExternalStorageState();
//
//        if (Environment.MEDIA_MOUNTED.equals(state)) {
//            if (requireWriteAccess) {
//                boolean writable = checkFsWritable();
//                return writable;
//            } else {
//                return true;
//            }
//        } else if (!requireWriteAccess
//                   && Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
//            return true;
//        }
//        return false;
//    }

//    private static boolean checkFsWritable() {
//        // Create a temporary file to see whether a volume is really writeable.
//        // It's important not to put it in the root directory which may have a
//        // limit on the number of files.
//        String directoryName = Environment.getExternalStorageDirectory().toString() + VD_EXTDIR;
//        File directory = new File(directoryName);
//        if (!directory.isDirectory()) {
//            if (!directory.mkdirs()) {
//                return false;
//            }
//        }
//        return directory.canWrite();
//    }
//
//    private void setCallStatus(int status) {
//        mCallStatus = status;
//        //mOtherStatus.setText(VideoPhoneDataBean.getCallStatusName(status));
//    }
//
//    private int getCallStatus() {
//        return mCallStatus;
//    }

    public void onTickForCallTimeElapsed(long timeElapsed) {
//        // While a call is in progress, update the elapsed time shown
//        // onscreen.
//        Call fg = mApplication.phone[mApplication.getCallScreenActiveSimCardId().toInt()].getForegroundCall();
//
//        if (!fg.isIdle()) {
//            long duration = CallTime.getCallDuration(fg);
//            updateElapsedTime(duration / 1000);
//        }
    }

    private void updateElapsedTime(long timeElapsed) {
        /*
        if (timeElapsed == 0) {
            mElapsedTime.setText("");
        } else {
            mElapsedTime.setText(DateUtils.formatElapsedTime(timeElapsed));
        }
        */
    }

//    private void resetState() {
//        // restore ui init parameters, add more here
//        updateElapsedTime(0);
//        mWndMode = VideoPhoneDataBean.RVVideoPhone_WINDOWS_B_REMOTE_S_LOCAL;
//    }

//    private void sendLocalVideo(final VTFramework vt) {
//        final View v = findViewById(R.id.previewSurface);
//        if (!vt.isBlock()) {
//            v.setVisibility(View.GONE);
//            vt.blockVideoCall(vt.getCallID());
//
//        }
//        mToggleVideoDlg = new AlertDialog.Builder(mInCallScreen)
//        .setTitle(R.string.BlockVideo)
//        .setMessage(R.string.vt_send_local_video)
//        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int which) {
//            }
//        })
//        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int which) {
//                Handler h = new Handler();
//                h.postDelayed(new Runnable() {
//                    public void run() {
//                        v.setVisibility(View.VISIBLE);
//                        if(vt.isBlock()) {
//                            vt.unblockVideoCall(vt.getCallID());
//                            v.setVisibility(View.VISIBLE);
//                        }
//                    }
//                }, 1000);
//            }
//        }).create();
//
//        mToggleVideoDlg.show();
//    }

//    public void hiddenVideo(final VTFramework vt, boolean isHidden) {
//        Log.w(LOG_TAG, "hiddenVideo="+isHidden);
//
//        VTAlternative vtAlter = PhoneUtils.getVTAlternative(vt);
//        ImageButton VTSwitchCameraButton = null;
//        VTSwitchCameraButton = (ImageButton)mInCallScreen.findViewById(R.id.vtswitchcameraButton);
//        /*
//        MenuItem VTSwitchCameraItem = null;
//        VTSwitchCameraItem = mVTmenu.findItem(MENU_TO_SWITCHCAME);
//        */
//        if(isHidden) {
//
//            if(VTSwitchCameraButton != null) {
//                VTSwitchCameraButton.setEnabled(false);
//            }
//            /*
//            if(VTSwitchCameraItem != null){
//                VTSwitchCameraItem.setEnabled(false);
//            }
//            */
//            Resources res = getResources();
//            BitmapDrawable bmpDraw =(BitmapDrawable)res.getDrawable(R.drawable.ic_contact_picture);
//            vtAlter.startAlternative(VTAlternative.LOCAL_PIC, bmpDraw);
//
//            /*
//            String fileName = "/data/vt/hiddenme.jpg";
//            vtAlter.startAlternative(VTAlternative.LOCAL_PIC,
//                                     fileName,
//                                     VTAlternative.PICTURE_JPEG);
//            */
//        } else {
//            vtAlter.stopAlternative(0);
//
//            if(VTSwitchCameraButton != null) {
//                VTSwitchCameraButton.setEnabled(true);
//            }
//            /*
//            if(VTSwitchCameraItem != null){
//                VTSwitchCameraItem.setEnabled(true);
//            }
//            */
//        }
//    }
//
//    public void PrepareStartVideoCall() {
//        CompoundButton VTHiddenButton = null;
//        VTHiddenButton = (CompoundButton)mInCallScreen.findViewById(R.id.vthiddenButton);
//        if(VTHiddenButton != null) {
//            VTHiddenButton.setEnabled(true);
//        }
//
//        ImageButton VTSwitchCameraButton = null;
//        VTSwitchCameraButton = (ImageButton)mInCallScreen.findViewById(R.id.vtswitchcameraButton);
//        if(VTSwitchCameraButton != null) {
//            VTSwitchCameraButton.setEnabled(true);
//        }
//    }
//
//    public void PrepareStopVideoCall() {
//        VTAlternative vtAlter = PhoneUtils.getVTAlternative(PhoneUtils.getVTFramework());
//        Log.w(LOG_TAG, "PrepareStopVideoCall,IsRun="+vtAlter.isRun());
//
//        if(vtAlter.isRun()) {
//            CompoundButton VTHiddenButton = null;
//            VTHiddenButton = (CompoundButton)mInCallScreen.findViewById(R.id.vthiddenButton);
//
//            if(VTHiddenButton != null) {
//                VTHiddenButton.setChecked(false);
//            }
//
//            vtAlter.stopAlternative(0);
//        }
//    }

    public void onClick(View view) {

    }
}

//class RemoteVideoSurface extends SurfaceView implements SurfaceHolder.Callback {
//    SurfaceHolder mHolder;
//    private static final String LOG_TAG = RemoteVideoSurface.class.getName();
//    private VTRemoteDisplay mRemoteDisplay = null;
//    private int mWndMode = VideoPhoneDataBean.RVVideoPhone_WINDOWS_B_REMOTE_S_LOCAL;
//    public RemoteVideoSurface(Context context) {
//        super(context);
//
//        // Install a SurfaceHolder.Callback so we get notified when the
//        // underlying surface is created and destroyed.
//        mHolder = getHolder();
//        mHolder.addCallback(this);
//        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
//    }
//
//    public RemoteVideoSurface(Context context, AttributeSet attrs) {
//        super(context, attrs);
//        mHolder = getHolder();
//        mHolder.addCallback(this);
//        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
//    }
//
//    public void surfaceCreated(SurfaceHolder holder) {
//        // The Surface has been created, acquire the camera and tell it where
//        // to draw.
//        Log.d(LOG_TAG, "surfaceCreated()");
//        VTFramework vt = PhoneUtils.getVTFramework();
//        try {
//            if(vt != null) {
//                if(mRemoteDisplay == null) {
//                    mRemoteDisplay = new VTRemoteDisplay();
//                    VTCallUi.mRemoteDisplay = mRemoteDisplay;
//                }
//
//                mRemoteDisplay.registerHolder(mHolder);
//                mRemoteDisplay.registerSurface(mHolder.getSurface());
//                Log.d(LOG_TAG, "surfaceCreated(),vt.registerRemoteDisplay");
//                vt.registerRemoteDisplay(mRemoteDisplay);
//
//                if(vt.hasVTCallConnected()
//                        && (VideoPhoneDataBean.RVVideoPhone_WINDOWS_B_LOCAL_S_REMOTE == mWndMode
//                            || VideoPhoneDataBean.RVVideoPhone_WINDOWS_BIG_LOCAL == mWndMode)) {
//                    Log.d(LOG_TAG, "surfaceCreated(),vt.startCameraPreview()");
//                    vt.startCameraPreview();
//                }
//            }
//        } catch (Exception e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//    }
//
//    public void surfaceDestroyed(SurfaceHolder holder) {
//        // Surface will be destroyed when we return, so stop the preview.
//        // Because the CameraDevice object is not a shared resource, it's very
//        // important to release it when the activity is paused.
//        Log.d(LOG_TAG, "surfaceDestroyed()");
//        VTFramework vt = PhoneUtils.getVTFramework();
//        if(vt != null) {
//            //support vt call in background
//            //vt.stopCameraPreview();
//            Log.d(LOG_TAG, "surfaceDestroyed(),call vt.registerRemoteDisplay(null)");
//            vt.registerRemoteDisplay(null);
//
//            //Log.d(LOG_TAG, "surfaceDestroyed(),call vt.releaseCamera()");
//            //vt.releaseCamera();
//            VTCallUi.mRemoteDisplay = null;
//        }
//    }
//
//    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
//        // Now that the size is known, set up the camera parameters and begin
//        // the preview.
//        int res = 0;
//    }
//
//    public void setWindowMode(int mode) {
//        mWndMode = mode;
//    }
//}

//class PreviewVideoSurface extends SurfaceView implements SurfaceHolder.Callback {
//    SurfaceHolder mHolder;
//    private static final String LOG_TAG = PreviewVideoSurface.class.getName();
//    private VTLocalDisplay mLocalDisplay = null;
//    private int mWndMode = VideoPhoneDataBean.RVVideoPhone_WINDOWS_B_REMOTE_S_LOCAL;
//    public PreviewVideoSurface(Context context) {
//        super(context);
//
//        // Install a SurfaceHolder.Callback so we get notified when the
//        // underlying surface is created and destroyed.
//        mHolder = getHolder();
//        mHolder.addCallback(this);
//        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
//
//        setZOrderMediaOverlay(true);
//    }
//
//    public PreviewVideoSurface(Context context, AttributeSet attrs) {
//        super(context, attrs);
//        mHolder = getHolder();
//        mHolder.addCallback(this);
//        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
//
//        setZOrderMediaOverlay(true);
//    }
//
//    public void surfaceCreated(SurfaceHolder holder) {
//        // The Surface has been created, acquire the camera and tell it where
//        // to draw.
//        Log.d(LOG_TAG, "surfaceCreated()");
//        VTFramework vt = PhoneUtils.getVTFramework();
//        try {
//            if(vt != null) {
//                if(mLocalDisplay == null) {
//                    mLocalDisplay = new VTLocalDisplay();
//                    VTCallUi.mLocalDisplay = mLocalDisplay;
//                }
//
//                mLocalDisplay.registerHolder(mHolder);
//                mLocalDisplay.registerSurface(mHolder.getSurface());
//                Log.d(LOG_TAG, "surfaceCreated(),vt.registerLocalDisplay");
//                vt.registerLocalDisplay(mLocalDisplay);
//
//                if(vt.hasVTCallConnected()
//                        && VideoPhoneDataBean.RVVideoPhone_WINDOWS_B_REMOTE_S_LOCAL == mWndMode) {
//                    Log.d(LOG_TAG, "surfaceCreated(),vt.startCameraPreview()");
//                    vt.startCameraPreview();
//                }
//            }
//        } catch (Exception e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//    }
//
//    public void surfaceDestroyed(SurfaceHolder holder) {
//        // Surface will be destroyed when we return, so stop the preview.
//        // Because the CameraDevice object is not a shared resource, it's very
//        // important to release it when the activity is paused.
//        Log.d(LOG_TAG, "surfaceDestroyed()");
//        VTFramework vt = PhoneUtils.getVTFramework();
//        if(vt != null) {
//            //			vt.stopCameraPreview();
//            Log.d(LOG_TAG, "surfaceDestroyed(),call vt.registerLocalDisplay(null)");
//            vt.registerLocalDisplay(null);
//            VTCallUi.mLocalDisplay = null;
//        }
//    }
//
//    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
//        // Now that the size is known, set up the camera parameters and begin
//        // the preview.
//        int res = 0;
//    }
//
//    public void setWindowMode(int mode) {
//        mWndMode = mode;
//    }
//}
