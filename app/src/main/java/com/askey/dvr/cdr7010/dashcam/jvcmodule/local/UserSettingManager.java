package com.askey.dvr.cdr7010.dashcam.jvcmodule.local;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import com.askey.dvr.cdr7010.dashcam.application.DashCamApplication;
import com.askey.dvr.cdr7010.dashcam.util.Logg;
import com.askey.dvr.cdr7010.dashcam.util.SPUtils;
import com.askey.platform.AskeySettings;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.EnumMap; /**
 * 项目名称：mainapp
 * 类描述：
 * 创建人：skysoft  charles.bai
 * 创建时间：2018/6/26 10:34
 * 修改人：skysoft
 * 修改时间：2018/6/26 10:34
 * 修改备注：
 */
public class UserSettingManager {

    private static final String LOG_TAG = "UserSettingManager";
    private static EnumMap<JvcStatusParams.JvcStatusParam, Object> mReportMap;

    public static void getUserList(EnumMap<JvcStatusParams.JvcStatusParam, Object> enumMap) {
        Logg.d(LOG_TAG, "getUserList: ");
        mReportMap = enumMap;
        Context appContext = DashCamApplication.getAppContext();
        ContentResolver contentResolver = appContext.getContentResolver();
        if(enumMap != null){
            int oos = (int)enumMap.get(JvcStatusParams.JvcStatusParam.OOS);
            //圏外の場合は取得できなかったことを通知するのでMainAPPで保持している設定値(前回値)を使用すること
            if(oos == 0) { // 0:圈内
                String response = (String)enumMap.get(JvcStatusParams.JvcStatusParam.RESPONSE);
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    int status = jsonObject.getInt("status");
                    if(status == 0) { // 0:成功

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        }

    }

    public static void userSettings(EnumMap<JvcStatusParams.JvcStatusParam, Object> enumMap) {
        Logg.d(LOG_TAG, "userSettings: ");
    }


}