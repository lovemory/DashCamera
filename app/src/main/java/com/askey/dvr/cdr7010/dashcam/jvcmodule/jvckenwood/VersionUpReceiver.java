package com.askey.dvr.cdr7010.dashcam.jvcmodule.jvckenwood;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.askey.dvr.cdr7010.dashcam.domain.EventInfo;
import com.askey.dvr.cdr7010.dashcam.util.Logg;
import com.askey.dvr.cdr7010.dashcam.util.SPUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VersionUpReceiver extends BroadcastReceiver{
    public static final String PREFERENCE_KEY_UPDATE_COMPLETED = "updateCompleted";

    private static final String LOG_TAG = "VersionUpReceiver";
    private static final String ACTION_EVENT_DOWNLOAD_RESULT = "com.jvckenwood.versionup.DOWNLOAD_RESULT";
    private static final String ACTION_EVENT_UPDATE_READY = "com.jvckenwood.versionup.UPDATE_READY";
    private static final String ACTION_EVENT_UPDATE_COMPLETED = "com.jvckenwood.versionup.UPDATE_COMPLETED";
    private static final String ACTION_EVENT_UPDATE_CHECK = "com.jvckenwood.versionup.UPDATE_CHECK";

    public static final String ACTION_FOTA_STATUS = "action_fota_status";
    public static final String ACTION_UPDATE_CHECK = "action_update_check";

    private static List<PowerOnRelativeCallback> mPowerOnRelativeCallbackList = Collections.synchronizedList(new ArrayList<PowerOnRelativeCallback>());

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Logg.i(LOG_TAG, "onReceive: action=" + action);
        if(action.equals(ACTION_EVENT_DOWNLOAD_RESULT)){
            //status: 0:完了、-1:失敗(圏外時も失敗を返す)、-2:中断、-3:チェック異常
            int status = intent.getIntExtra("status", -1);
            int http = intent.getIntExtra("http", -1);
            int length = intent.getIntExtra("length", -1);

            Intent broadcastIntent = new Intent(ACTION_FOTA_STATUS);
            broadcastIntent.putExtra("status", status);
            broadcastIntent.putExtra("http", http);
            broadcastIntent.putExtra("length", length);
            context.sendStickyBroadcastAsUser(broadcastIntent, android.os.Process.myUserHandle());
        }else if(action.equals(ACTION_EVENT_UPDATE_READY)){ //null params
            MainAppSending.updateReadyCompleted();
            for(PowerOnRelativeCallback callback : mPowerOnRelativeCallbackList)
                callback.onUpdateReady();
        }else if(action.equals(ACTION_EVENT_UPDATE_COMPLETED)){
            int type = intent.getIntExtra("type", -1);
            int result = intent.getIntExtra("result", -10); //-1被正常使用

            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("type", type); //0：OTA、2：SDカード
                jsonObject.put("result", result); //0：成功、-1：アップデート失敗
                String saveJson = jsonObject.toString();
                Logg.d(LOG_TAG, "Save UPDATE_COMPLETED information : " + saveJson);
                SPUtils.put(context, PREFERENCE_KEY_UPDATE_COMPLETED, saveJson);
            } catch (JSONException e) {
                Logg.e(LOG_TAG, "Save UPDATE_COMPLETED information error: " + e.getMessage());
            }
            for(PowerOnRelativeCallback callback : mPowerOnRelativeCallbackList)
                callback.onUpdateCompleted(type,result);
        }else if(action.equals(ACTION_EVENT_UPDATE_CHECK)){
            int result = intent.getIntExtra("result", 1);
            Intent broadcastIntent = new Intent(ACTION_UPDATE_CHECK);
            broadcastIntent.putExtra("result", result);
            context.sendBroadcastAsUser(broadcastIntent, android.os.Process.myUserHandle());
        }
    }

    private boolean checkEventInfo(EventInfo eventInfo, int eventType){
        if(eventInfo == null){
            Logg.e(LOG_TAG, "checkEventInfo: can't find EventInfo, eventType=" + eventType);
            return false;
        }
        return true;
    }

    public static void registerPowerOnRelativeCallback(PowerOnRelativeCallback callback){
        if (callback == null) return;
        Logg.i(LOG_TAG, "registerPowerOnRelativeCallback: ");
        mPowerOnRelativeCallbackList.add(callback);
    }

    public static void unRegisterPowerOnRelativeCallback(PowerOnRelativeCallback callback){
        if (callback == null) return;
        Logg.i(LOG_TAG, "unRegisterPowerOnRelativeCallback: ");
        mPowerOnRelativeCallbackList.remove(callback);
    }

    public interface PowerOnRelativeCallback{
        void onUpdateReady();
        void onUpdateCompleted(int type, int result);
        void onStartUp(int bootinfo, int updateInfo, String farmver, String soundver);
    }

}