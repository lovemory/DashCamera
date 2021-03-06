package com.askey.dvr.cdr7010.dashcam.jvcmodule.jvckenwood;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;

import com.askey.dvr.cdr7010.dashcam.application.DashCamApplication;
import com.askey.dvr.cdr7010.dashcam.jvcmodule.local.CommunicationService;
import com.askey.dvr.cdr7010.dashcam.jvcmodule.local.JvcEventHandoutInfo;
import com.askey.dvr.cdr7010.dashcam.jvcmodule.local.JvcStatusParams;
import com.askey.dvr.cdr7010.dashcam.jvcmodule.local.LocalJvcStatusManager;
import com.askey.dvr.cdr7010.dashcam.jvcmodule.local.ManualUploadService;
import com.askey.dvr.cdr7010.dashcam.jvcmodule.local.SystemSettingManager;
import com.askey.dvr.cdr7010.dashcam.jvcmodule.local.UserSettingManager;
import com.askey.dvr.cdr7010.dashcam.ui.CameraRecordFragment;
import com.askey.dvr.cdr7010.dashcam.util.ActivityUtils;
import com.askey.dvr.cdr7010.dashcam.util.EventUtil;
import com.askey.dvr.cdr7010.dashcam.util.Logg;
import com.askey.platform.AskeySettings;
import com.jvckenwood.communication.IMainApp;
import com.jvckenwood.communication.IMainAppCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/***
 * Company: Chengdu Skysoft Info&Tech Co., Ltd.
 * Copyright ©2014-2018 Chengdu Skysoft Info&Tech Co., Ltd.
 * Created by Mark on 2018/5/16.

 * @since:JDK1.6
 * @version:1.0
 * @see
 ***/
public class MainApp {
//    public static final String ACTION_xxx_REQUEST = "xxx";

    private static final String LOG_TAG = "MainApp";
    private static MainApp mMainApp;
    private IMainApp mMainAppInterface;
    private static Context mAppContext;
    private static CountDownLatch countDownLatch;
    private static int startUp;
    private static int rtcInfo;
    private static final String PREFER_KEY_CONTRACT_FLG = "ContractFlg";
    private int tripNum;

    private MainApp() {
        mAppContext = DashCamApplication.getAppContext();
    }

    public static MainApp getInstance() {
        if (mMainApp == null) {
            synchronized (MainApp.class) {
                if (mMainApp == null) {
                    mMainApp = new MainApp();
                }
            }
        }
        countDownLatch = new CountDownLatch(4);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    countDownLatch.await();
                    sendStatUpNotify();
                    Log.d(LOG_TAG, "countDownLatch.await()~sendStatUpNotify~~~！");
                } catch (InterruptedException e) {
                    e.printStackTrace();


                }
            }
        }).start();

        return mMainApp;
    }

    private ServiceConnection mMainAppConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Logg.d(LOG_TAG, "onServiceConnected: MainApp");
            mMainAppInterface = IMainApp.Stub.asInterface(service);
            registerCallback(mMainAppCallbackCallback);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Logg.d(LOG_TAG, "onServiceDisconnected: MainApp");
            mMainAppInterface = null;
        }
    };

    private IMainAppCallback.Stub mMainAppCallbackCallback = new IMainAppCallback.Stub() {
        @Override
        public void reportInsuranceTerm(int oos, String response) {
            Logg.d(LOG_TAG, "reportInsuranceTerm: oos=" + oos + ", response=" + response);
            EnumMap<JvcStatusParams.JvcStatusParam, Object> enumMap = new EnumMap<>(JvcStatusParams.JvcStatusParam.class);
            enumMap.put(JvcStatusParams.JvcStatusParam.OOS, oos);
            enumMap.put(JvcStatusParams.JvcStatusParam.RESPONSE, response);
            LocalJvcStatusManager.setInsuranceTerm(enumMap);
            if (oos != 0) {
                sendStatUpNotify();
                Log.d(LOG_TAG,"reportInsuranceTerm oos != 0 ~~~~~sendStatUpNotify~~~ ");
            }
        }

        @Override
        public void reportUserList(int oos, String response) throws RemoteException {
            Logg.d(LOG_TAG, "reportUserList: oos=" + oos + ", response=" + response);

            EnumMap<JvcStatusParams.JvcStatusParam, Object> enumMap = new EnumMap<>(JvcStatusParams.JvcStatusParam.class);
            enumMap.put(JvcStatusParams.JvcStatusParam.OOS, oos);
            enumMap.put(JvcStatusParams.JvcStatusParam.RESPONSE, response);
            UserSettingManager.getUserList(enumMap, countDownLatch);
        }

        @Override
        public void reportSystemSettings(int oos, String response) {
            Logg.d(LOG_TAG, "reportSystemSettings: oos=" + oos + ", response=" + response);
            EnumMap<JvcStatusParams.JvcStatusParam, Object> enumMap = new EnumMap<>(JvcStatusParams.JvcStatusParam.class);
            enumMap.put(JvcStatusParams.JvcStatusParam.OOS, oos);
            enumMap.put(JvcStatusParams.JvcStatusParam.RESPONSE, response);
            SystemSettingManager.systemSetting(enumMap, countDownLatch);
        }

        @Override
        public void reportUserSettings(int oos, String response) {
            Logg.d(LOG_TAG, "reportUserSettings: oos=" + oos + ", response=" + response);
                        EnumMap<JvcStatusParams.JvcStatusParam, Object> enumMap = new EnumMap<>(JvcStatusParams.JvcStatusParam.class);
                        enumMap.put(JvcStatusParams.JvcStatusParam.OOS, oos);
                        enumMap.put(JvcStatusParams.JvcStatusParam.RESPONSE, response);
                        UserSettingManager.userSettings(enumMap, countDownLatch);
        }

        @Override
        public void reportTripInfo(int oos, String response) throws RemoteException {
            Logg.d(LOG_TAG, "reportTripInfo: oos=" + oos + "，tripNum=" + tripNum + ", response=" + response);
            tripNum++;
            if (tripNum == 1) {
                countDownLatch.countDown();
                Log.d(LOG_TAG, "countDownLatch.countDown()  04~~");
            }
        }

        @Override
        public void reportSettingsUpdate(int oos, String response) {
            Logg.d(LOG_TAG, "reportSettingsUpdate: oos=" + oos + ", response=" + response);
            CommunicationService.reportSettingsUpdate(oos, response);

            EnumMap<JvcStatusParams.JvcStatusParam, Object> enumMap = new EnumMap<>(JvcStatusParams.JvcStatusParam.class);
            enumMap.put(JvcStatusParams.JvcStatusParam.OOS, oos);
            enumMap.put(JvcStatusParams.JvcStatusParam.RESPONSE, response);
            SystemSettingManager.settingsUpdate(enumMap);
        }


        @Override
        public void reportDrivingReport(int oos, String response) {
            Logg.d(LOG_TAG, "reportDrivingReport: oos=" + oos + ", response=" + response);
            //id 52, 運転レポート Driving report, eventType define 101
            int eventType = 101;
            JvcEventHandoutInfo info = new JvcEventHandoutInfo(eventType);
            info.setOos(oos);
            if (oos == 0) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    int status = jsonObject.getInt("status");
                    info.setStatus(status);
                    if (status == 0) {
                        int result = jsonObject.getInt("result");
                        info.setResult(result);
                    }
                } catch (JSONException e) {
                    Logg.e(LOG_TAG, "reportDrivingReport: error: " + e.getMessage());
                }
            }
            EventUtil.sendEvent(info);
        }

        @Override
        public void reportManthlyDrivingReport(int oos, String response) {
            Logg.d(LOG_TAG, "reportManthlyDrivingReport: oos=" + oos + ", response=" + response);
            //id 53, 月間運転レポート Monthly driving report, eventType define 102
            int eventType = 102;
            JvcEventHandoutInfo info = new JvcEventHandoutInfo(eventType);
            info.setOos(oos);
            if (oos == 0) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    int status = jsonObject.getInt("status");
                    info.setStatus(status);
                    if (status == 0) {
                        int result = jsonObject.getInt("result");
                        info.setResult(result);
                    }
                } catch (JSONException e) {
                    Logg.e(LOG_TAG, "reportManthlyDrivingReport: error: " + e.getMessage());
                }
            }
            EventUtil.sendEvent(info);
        }

        @Override
        public void reportServerNotifocation(int oos, String response) {
            Logg.d(LOG_TAG, "reportServerNotifocation: oos=" + oos + ", response=" + response);
            //id 51, お知らせ Notice, eventType define 100
            int eventType = 100;
            JvcEventHandoutInfo info = new JvcEventHandoutInfo(eventType);
            info.setOos(oos);
            if (oos == 0) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    int status = jsonObject.getInt("status");
                    info.setStatus(status);
                    if (status == 0) {
                        int type = jsonObject.getInt("type");
                        info.setType(type);
                    }
                } catch (JSONException e) {
                    Logg.e(LOG_TAG, "reportServerNotifocation: error: " + e.getMessage());
                }
            }
            EventUtil.sendEvent(info);
        }

        @Override
        public void reportDrivingAdvice(int oos, String response) {
            Logg.d(LOG_TAG, "reportDrivingAdvice: oos=" + oos + ", response=" + response);
            //id 54, 運転前アドバイス Advice before driving, eventType define 103
            int eventType = 103;
            JvcEventHandoutInfo info = new JvcEventHandoutInfo(eventType);
            info.setOos(oos);
            if (oos == 0) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    int status = jsonObject.getInt("status");
                    info.setStatus(status);
                    String code="";
                    if (status == 0) {
                        code = jsonObject.getString("code");
                        info.setCode(code);
                        EventUtil.sendEvent(info);
                    }
                } catch (JSONException e) {
                    Logg.e(LOG_TAG, "reportDrivingAdvice: error: " + e.getMessage());
                    info.setCode("");
                }
            }
        }

        @Override
        public void reportTxEventProgress(List<String> path, int result) {
            Logg.d(LOG_TAG, "reportTxEventProgress: ");
            if (CameraRecordFragment.isBatteryDisconnected()) {
                Logg.d(LOG_TAG, "SHUT DOWN...");
                ActivityUtils.shutDown(DashCamApplication.getAppContext());
            }
        }

        @Override
        public void reportTxManualProgress(int progress1, int total1, int progress2, int total2) {
            Logg.d(LOG_TAG, "reportTxManualProgress: progress1=" + progress1 + ", total1=" + total1 + ", progress2=" + progress2 + ", total2=" + total2);
            ManualUploadService.reportTxManualProgress(progress1, total1, progress2, total2);
        }

        @Override
        public void logUploadResult(int oos, String result) {
            Logg.d(LOG_TAG, "logUploadResult: oos=" + oos + ", result=" + result);

            String outputFilePath = DashCamApplication.getAppContext().getFilesDir().getAbsolutePath();
            File outputFile = new File(outputFilePath);
            for (File file : outputFile.listFiles()) {
                if (file.getName().startsWith("log_"))
                    file.delete();
            }
        }
    };

    public boolean bindJvcMainAppService() {
        Logg.d(LOG_TAG, "bindJvcMainAppService: ");
        Intent intent = new Intent("com.jvckenwood.communication.IMainApp");
        intent.setClassName("com.jvckenwood.communication", "com.jvckenwood.communication.CommunicationService");
//        intent.setPackage("com.jvckenwood.communication");
        return mAppContext.bindService(intent, mMainAppConn, Context.BIND_AUTO_CREATE);
    }

    public void unBindJvcMainAppService() {
        Logg.d(LOG_TAG, "unBindJvcMainAppService: ");
        unregisterCallback(mMainAppCallbackCallback);
        mAppContext.unbindService(mMainAppConn);
    }

    /**
     * **********************************************
     * ********************** API ****************************
     * **********************************************
     */
    public void registerCallback(IMainAppCallback callback) {
        Logg.d(LOG_TAG, "registerCallback: ");
        if (!checkConnection())
            return;

        try {
            mMainAppInterface.registerCallback(callback);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void unregisterCallback(IMainAppCallback callback) {
        Logg.d(LOG_TAG, "unregisterCallback: ");
        if (!checkConnection())
            return;

        try {
            mMainAppInterface.unregisterCallback(callback);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void settingsUpdateRequest(String setings) {
        Logg.d(LOG_TAG, "settingsUpdateRequest: setings=" + setings);
        if (!checkConnection())
            return;

        try {
            mMainAppInterface.settingsUpdateRequest(setings);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void manualUploadCancel() {
        Logg.d(LOG_TAG, "manualUploadCancel: ");
        if (!checkConnection())
            return;

        try {
            mMainAppInterface.manualUploadCancel();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void logUpload(String zipPath) {
        Logg.d(LOG_TAG, "logUpload: zipPath=" + zipPath);
        if (!checkConnection())
            return;

        try {
            mMainAppInterface.logUpload(zipPath);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    private boolean checkConnection() {
        if (mMainAppInterface == null) {
            Logg.e(LOG_TAG, "checkConnection: service not connected.");
            return false;
        }

        return true;
    }

    public static void sendStatUpNotify() {
        Log.d(LOG_TAG, "sendStatUpNotify~~~~!");
            ContentResolver contentResolver = mAppContext.getContentResolver();
            try {
                rtcInfo = Settings.Global.getInt(contentResolver, AskeySettings.Global.SYSSET_RTCINFO);
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
            try {
                startUp = Settings.Global.getInt(contentResolver, AskeySettings.Global.SYSSET_STARTUP_INFO);
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
            MainAppSending.startupNotify(startUp, rtcInfo);
    }
}