package com.askey.dvr.cdr7010.dashcam.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;

import com.askey.dvr.cdr7010.dashcam.domain.Event;
import com.askey.dvr.cdr7010.dashcam.domain.MessageEvent;
import com.askey.dvr.cdr7010.dashcam.ui.utils.UIElementStatusEnum;
import com.askey.dvr.cdr7010.dashcam.util.EventUtil;
import com.askey.dvr.cdr7010.dashcam.util.Logg;

import static com.askey.dvr.cdr7010.dashcam.ui.utils.UIElementStatusEnum.GPSStatusType.GPS_STRENGTH_NOT_FIXES;
import static com.askey.dvr.cdr7010.dashcam.ui.utils.UIElementStatusEnum.GPSStatusType.GPS_STRENGTH_NONE;
import static com.askey.dvr.cdr7010.dashcam.ui.utils.UIElementStatusEnum.GPSStatusType.GPS_STRENGTH_FIXES;

public class GPSReceiver extends BroadcastReceiver{
    private static final String TAG = GPSReceiver.class.getSimpleName();
    public static final String EXTRA_GPS_ENABLED = "enabled";
    public static final String GPS_FIX_CHANGE_ACTION = "android.location.GPS_FIX_CHANGE";
    public static final String GPS_ENABLED_CHANGE_ACTION = "android.location.GPS_ENABLED_CHANGE";
    private static int gpsStatus = 0;
    @Override
    public void onReceive(Context context, Intent intent) {
        Logg.i(TAG, "onReceive: " + intent);
        String action = intent.getAction();
        final boolean enabled = intent.getBooleanExtra(EXTRA_GPS_ENABLED, false);
        final int svCount = intent.getIntExtra(LocationManager.KEY_STATUS_CHANGED, 0);
        if (action.equals(GPS_FIX_CHANGE_ACTION) && enabled) {
            // GPS is getting fixes
            EventUtil.sendEvent(new MessageEvent<UIElementStatusEnum.GPSStatusType>(Event.EventCode.EVENT_GPS, GPS_STRENGTH_FIXES));
        } else if (action.equals(GPS_ENABLED_CHANGE_ACTION) && !enabled) {
            // GPS is off
            EventUtil.sendEvent(new MessageEvent<UIElementStatusEnum.GPSStatusType>(Event.EventCode.EVENT_GPS, GPS_STRENGTH_NONE));
        } else {
            // GPS is on, but not receiving fixes
            if(svCount > 0) {
                EventUtil.sendEvent(new MessageEvent<UIElementStatusEnum.GPSStatusType>(Event.EventCode.EVENT_GPS, GPS_STRENGTH_NOT_FIXES));
            } else {
                EventUtil.sendEvent(new MessageEvent<UIElementStatusEnum.GPSStatusType>(Event.EventCode.EVENT_GPS, GPS_STRENGTH_NONE));
            }
        }

        Logg.i(TAG, "GPS status: " + Integer.toString(gpsStatus));
    }
}