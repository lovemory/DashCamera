package com.askey.dvr.cdr7010.dashcam.core;

public interface DashCamControl {
    void onStartVideoRecord();
    void onStopVideoRecord();
    void onMuteAudio();
    void onDemuteAudio();
    void onOpenCamera() throws Exception;
    void onCameraClosed();
}
