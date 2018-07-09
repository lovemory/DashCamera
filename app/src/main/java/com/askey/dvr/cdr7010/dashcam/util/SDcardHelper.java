package com.askey.dvr.cdr7010.dashcam.util;

import android.os.Environment;

import com.askey.dvr.cdr7010.dashcam.logic.GlobalLogic;
import com.askey.dvr.cdr7010.dashcam.service.EventManager;
import com.askey.dvr.cdr7010.dashcam.ui.utils.UIElementStatusEnum;

import static com.askey.dvr.cdr7010.dashcam.ui.utils.UIElementStatusEnum.SDcardStatusType.SDCARD_REMOVED;

public class SDcardHelper{
    public static boolean isSDCardAvailable(){
        String status = Environment.getExternalStorageState();
        if (status.equalsIgnoreCase(Environment.MEDIA_REMOVED)
                || status.equalsIgnoreCase(Environment.MEDIA_BAD_REMOVAL)
                || status.equalsIgnoreCase(Environment.MEDIA_UNMOUNTED)) {
            return false;
        }
        return true;
    }
    public static void checkSdCardExist(){
        if (!isSDCardAvailable()) {
            GlobalLogic.getInstance().setSDCardCurrentStatus(SDCARD_REMOVED);
            EventManager.getInstance().handOutEventInfo(110);
        }
    }
    public static void handleSdcardAbnormalEvent(){
        UIElementStatusEnum.SDcardStatusType sdcardStatus = GlobalLogic.getInstance().getSDCardCurrentStatus();
        switch (sdcardStatus) {
            case SDCARD_UNSUPPORTED:
                EventManager.getInstance().handOutEventInfo(112);
                break;
            case SDCARD_INIT_FAIL:
                EventManager.getInstance().handOutEventInfo(111);
                break;
            case SDCARD_UNRECOGNIZABLE:
                EventManager.getInstance().handOutEventInfo(113);
                break;
            case SDCARD_REMOVED:
                EventManager.getInstance().handOutEventInfo(110);
                break;
            case SDCARD_FULL_LIMIT:
                EventManager.getInstance().handOutEventInfo(116);
                break;
            default:
        }
    }
}