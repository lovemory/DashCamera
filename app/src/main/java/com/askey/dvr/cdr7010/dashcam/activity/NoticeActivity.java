package com.askey.dvr.cdr7010.dashcam.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.KeyEvent;

import com.askey.dvr.cdr7010.dashcam.R;
import com.askey.dvr.cdr7010.dashcam.domain.KeyAdapter;
import com.askey.dvr.cdr7010.dashcam.jvcmodule.jvckenwood.VersionUpReceiver;
import com.askey.dvr.cdr7010.dashcam.logic.GlobalLogic;
import com.askey.dvr.cdr7010.dashcam.mvp.view.NoticeFragment;
import com.askey.dvr.cdr7010.dashcam.mvp.view.UpdateFragment;
import com.askey.dvr.cdr7010.dashcam.service.DialogManager;
import com.askey.dvr.cdr7010.dashcam.service.TTSManager;
import com.askey.dvr.cdr7010.dashcam.util.ActivityUtils;
import com.askey.dvr.cdr7010.dashcam.util.Const;
import com.askey.dvr.cdr7010.dashcam.util.EventUtil;
import com.askey.dvr.cdr7010.dashcam.util.Logg;
import com.askey.platform.AskeySettings;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


public class NoticeActivity extends DialogActivity implements NoticeFragment.NoticeListener, UpdateFragment.UpdateListener {
    private static final String TAG = "NoticeActivity";
    private NoticeFragment noticeFragment;
    private UpdateFragment updateFragment;
    private boolean isUpdate;
    private UpdateInfos updateInfos;
    private boolean isNoticeFinish = false;
    private static final String ACTION_EVENT_STARTUP = "com.jvckenwood.versionup.STARTUP";
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Logg.d(TAG, "onReceive: " + action);
            if(ACTION_EVENT_STARTUP.equals(action)){
                int bootinfo = intent.getIntExtra("bootinfo", -1);
                int updateInfo = intent.getIntExtra("updateInfo", -10);
                String farmver = intent.getStringExtra("farmver");
                String soundver = intent.getStringExtra("soundver");
                Logg.i(TAG,"onReceive: STARTUP: bootinfo=" + bootinfo + ", updateInfo=" + updateInfo);
                if(updateInfo == 0) {//None
                    VersionUpReceiver.StartUpInfo startUpInfo = new VersionUpReceiver.StartUpInfo(bootinfo, updateInfo, farmver, soundver);
                    EventUtil.sendEvent(startUpInfo);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notice);
        noticeFragment = NoticeFragment.newInstance(null);
        ActivityUtils.addFragmentToActivity(getSupportFragmentManager(),
                noticeFragment, R.id.contentFrame);
        //add by Mark for PUCDR-1262
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_EVENT_STARTUP);
        registerReceiver(receiver, filter);
        //end add
    }

    @Override
    public void onKeyShortPressed(int keyCode) {
        switch (keyCode) {
            case KeyAdapter.KEY_ENTER:
                if (updateInfos != null && (updateInfos.updateResultState != Const.UPDATE_READY) && isUpdate) {
                    startNextActivity();
                }
        }
    }

    @Override
    public void noticeTimerFinish() {
        isNoticeFinish = true;
    }

    @Override
    public void noticeJump() {
//        String updateResult = (String)SPUtils.get(DashCamApplication.getAppContext(), Const.PREFERENCE_KEY_UPDATE_COMPLETED, "");
//        updateInfo = parseJson(updateResult);
//        //updateInfo = parseFile("/cache/recovery_result");
//            if (updateInfo != null && ((updateInfo.updateResultState == Const.UPDATE_SUCCESS)
//                     ||(updateInfo.updateResultState == Const.UPDATE_FAIL))) {
//                SPUtils.remove(DashCamApplication.getAppContext(), Const.PREFERENCE_KEY_UPDATE_COMPLETED);
//                isUpdate = true;
//                updateFragment = new UpdateFragment();
//                Bundle bundle = new Bundle();
//                bundle.putInt("updateType", (updateInfo.updateType == Const.OTA_UPDATE) ? Const.OTA_UPDATE : Const.SDCARD_UPDATE);
//                updateFragment.setArguments(bundle);
//                ActivityUtils.hideFragment(getSupportFragmentManager(), noticeFragment);
//                ActivityUtils.addFragmentToActivity(getSupportFragmentManager(),
//                        updateFragment, R.id.contentFrame);
//            } else{
//                startNextActivity();
//            }
        if (updateInfos != null && updateInfos.updateType == Const.NONE_UPDATE) {
            Logg.i(TAG, "=noticeJump=None=startNextActivity");
            startNextActivity();
        } else if (updateInfos != null && ((updateInfos.updateResultState == Const.UPDATE_SUCCESS)
                || (updateInfos.updateResultState == Const.UPDATE_FAIL) || updateInfos.updateResultState == Const.UPDATE_READY)) {
            Logg.i(TAG, "=noticeJump=UpdateFragment=startNextActivity");
            isUpdate = true;
            updateFragment = new UpdateFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("updateResultState", updateInfos.updateResultState);
            updateFragment.setArguments(bundle);
            ActivityUtils.hideFragment(getSupportFragmentManager(), noticeFragment);
            ActivityUtils.addFragmentToActivity(getSupportFragmentManager(),
                    updateFragment, R.id.contentFrame);
        } else {
            Logg.i(TAG, "=noticeJump=no update info===");
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onHandleEvent(Object eventType) {
        if (eventType instanceof VersionUpReceiver.StartUpInfo || eventType instanceof VersionUpReceiver.UpdateCompleteInfo
                || eventType instanceof VersionUpReceiver.UpdateReadyInfo) {
            updateInfos = new UpdateInfos();
            if (eventType instanceof VersionUpReceiver.StartUpInfo && ((VersionUpReceiver.StartUpInfo) eventType).updateInfo == 0) {//None
                Logg.i(TAG, "=system_update=None=");
                updateInfos.updateType = Const.NONE_UPDATE;
            } else if (eventType instanceof VersionUpReceiver.UpdateCompleteInfo) {
                if (((VersionUpReceiver.UpdateCompleteInfo) eventType).result == 0) {//成功
                    updateInfos.updateResultState = Const.UPDATE_SUCCESS;
                    Logg.i(TAG, "=system_update_success==");
                } else if (((VersionUpReceiver.UpdateCompleteInfo) eventType).result == -1) {//アップデート失敗
                    updateInfos.updateResultState = Const.UPDATE_FAIL;
                    Logg.i(TAG, "=system_update_fail==");
                }
//                if (((VersionUpReceiver.UpdateCompleteInfo) eventType).type == 0) {//OTA
////                    updateInfo.updateType = Const.OTA_UPDATE;
//                    if (((VersionUpReceiver.UpdateCompleteInfo) eventType).result == 0) {//成功
//                        updateInfo.updateResultState = Const.UPDATE_SUCCESS;
//                        Logg.i(TAG, "=system_update_success=OTA_UPDATE=");
//                    } else if (((VersionUpReceiver.UpdateCompleteInfo) eventType).result == -1) {//アップデート失敗
//                        updateInfo.updateResultState = Const.UPDATE_FAIL;
//                        Logg.i(TAG, "=system_update_fail=OTA_UPDATE=");
//                    }
//
//                } else if (((VersionUpReceiver.UpdateCompleteInfo) eventType).type == 2) {//SDカード
////                    updateInfo.updateType = Const.SDCARD_UPDATE;
//                    if (((VersionUpReceiver.UpdateCompleteInfo) eventType).result == 0) {//成功
//                        updateInfo.updateResultState = Const.UPDATE_SUCCESS;
//                        Logg.i(TAG, "=system_update_success=SDCARD_UPDATE=");
//                    } else if (((VersionUpReceiver.UpdateCompleteInfo) eventType).result == -1) {//アップデート失敗
//                        updateInfo.updateResultState = Const.UPDATE_FAIL;
//                        Logg.i(TAG, "=system_update_fail=SDCARD_UPDATE=");
//                    }
//                }
            } else if (eventType instanceof VersionUpReceiver.UpdateReadyInfo) {
                updateInfos.updateResultState = Const.UPDATE_READY;
            }
            Logg.i(TAG, "=onHandleEvent=isNoticeFinish=" + isNoticeFinish);
            if (isNoticeFinish) {
                noticeJump();
                isNoticeFinish = false;
            }
        }
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    protected boolean handleKeyEvent(KeyEvent event) {
        return false;
    }

    @Override
    public void updateCompleteJump() {
        startNextActivity();
    }

    @Override
    public void displayTipInfo() {
        if (updateInfos != null && (updateInfos.updateResultState == Const.UPDATE_SUCCESS)) {
            DialogManager.getIntance().showDialog(DialogActivity.DIALOG_TYPE_UPDATE, getResources().getString(R.string.system_update_success), true);
            TTSManager.getInstance().ttsNormalStart(201, new int[]{0x0A03});
        } else if (updateInfos != null && (updateInfos.updateResultState == Const.UPDATE_FAIL)) {
            DialogManager.getIntance().showDialog(DialogActivity.DIALOG_TYPE_UPDATE, getResources().getString(R.string.system_update_fail), true);
            TTSManager.getInstance().ttsNormalStart(200, new int[]{0x0A04});
        } else if (updateInfos != null && (updateInfos.updateResultState == Const.UPDATE_READY)) {
            DialogManager.getIntance().showDialog(DialogActivity.DIALOG_TYPE_UPDATE, getResources().getString(R.string.system_update_ready), true);
            TTSManager.getInstance().ttsNormalStart(202, new int[]{0x0A02});
        }
    }

    //        private UpdateInfo parseJson(String updateResult){
//        if(TextUtils.isEmpty(updateResult)){
//            return null;
//        }
//        UpdateInfo updateInfo = new UpdateInfo();
//        try {
//                JSONObject jsonObject = new JSONObject(updateResult);
//                updateInfo.updateType = jsonObject.getInt("type");
//                updateInfo.updateResultState = jsonObject.getInt("result");
//            }catch(Exception e){
//                e.printStackTrace();
//            }
//            return updateInfo;
//    }
//
//    private UpdateInfo parseFile(String filePath){
//        UpdateInfo updateInfo =null;
//        if(TextUtils.isEmpty(filePath)){
//            return null;
//        }
//        File dir = new File(filePath);
//        if (dir.exists()) {
//            try {
//                updateInfo = new UpdateInfo();
//                FileReader mFileReader = new FileReader("/cache/recovery_result");
//                BufferedReader mBufferedReader = new BufferedReader(mFileReader);
//                String mReadText = "";
//                String mTextLine = mBufferedReader.readLine();
//                while (mTextLine!=null) {
//                    mReadText += mTextLine+"\n";
//                    mTextLine = mBufferedReader.readLine();
//                }
//                updateInfo.updateType =Const.SDCARD_UPDATE;
//                if(mReadText.contains("0")) {
//                    updateInfo.updateResultState =Const.UPDATE_SUCCESS;
//                }
//                else {
//                    updateInfo.updateResultState =Const.UPDATE_FAIL;
//                }
//            } catch(Exception e) {
//            }
//        }
//        return updateInfo;
//    }
    public void startNextActivity() {
        if (updateInfos.updateResultState != Const.UPDATE_READY) {//update ready 界面时不跳转
            if (GlobalLogic.getInstance().getInt(AskeySettings.Global.SETUP_WIZARD_AVAILABLE, 1) == Const.FIRST_INIT_SUCCESS) {
                ActivityUtils.startActivity(this, this.getPackageName(), "com.askey.dvr.cdr7010.dashcam.ui.MainActivity", true);
            } else {
                ActivityUtils.startActivity(this, Const.PACKAGE_NAME, Const.CLASS_NAME, true);
            }
        }
    }

    @Override
    public void onDestroy() {
        updateInfos = null;
        if (isUpdate) {
            DialogManager.getIntance().dismissDialog(DialogActivity.DIALOG_TYPE_UPDATE);
            isUpdate = false;
        }
        //add by Mark for PUCDR-1262
        unregisterReceiver(receiver);
        //end add
        super.onDestroy();
    }

    private class UpdateInfos {
        public int updateType = -1;
        public int updateResultState = -1;
    }
}