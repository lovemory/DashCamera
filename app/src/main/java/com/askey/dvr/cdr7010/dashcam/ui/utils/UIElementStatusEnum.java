package com.askey.dvr.cdr7010.dashcam.ui.utils;
public class UIElementStatusEnum{
    public enum RecordingStatusType{
        RECORDING_UNKNOWN(0),
        RECORDING_CONTINUOUS(1),
        RECORDING_EVENT(2),
        RECORDING_STOP(3),
        RECORDING_ERROR(4);

        public final int value;
        RecordingStatusType(int value){
            this.value = value;
        }
        @Override
        public String toString(){
            return String.valueOf(value);
        }

    }
    public enum MICStatusType{
        MIC_OFF(0),
        MIC_ON(1);
        public final int value;
        MICStatusType(int value){
            this.value = value;
        }
        @Override
        public String toString(){
            return String.valueOf(value);
        }

    }
    public enum LTEStatusType{
        LTE_SIGNAL_STRENGTH_NONE_OR_UNKNOWN(0),
        LTE_SIGNAL_STRENGTH_POOR(1),
        LTE_SIGNAL_STRENGTH_MODERATE(2),
        LTE_SIGNAL_STRENGTH_GOOD(3),
        LTE_SIGNAL_STRENGTH_GREAT(4),
        LTE_NONE(5);

        public final int value;
        LTEStatusType(int value){
            this.value = value;
        }
        @Override
        public String toString(){
            return String.valueOf(value);
        }
    }
    public enum EventRecordingLimitStatusType{
        EVENT_RECORDING_REACH_LIMIT_CONDITION(0),
        EVENT_RECORDING_UNREACHABLE_LIMIT_CONDITION(1);
        public final int value;
        EventRecordingLimitStatusType(int value){
            this.value = value;
        }
        @Override
        public String toString(){
            return String.valueOf(value);
        }
    }
    public enum ParkingRecordingLimitStatusType{
        PARKING_RECORDING_REACH_LIMIT_CONDITION(0),
        PARKING_RECORDING_UNREACHABLE_LIMIT_CONDITION(1);
        public final int value;
        ParkingRecordingLimitStatusType(int value){
            this.value = value;
        }
        @Override
        public String toString(){
            return String.valueOf(value);
        }
    }
    public enum SDcardStatusType{
        SDCARD_MOUNTED(0),// 此时SD是可读写的
        SDCARD_SUPPORTED(1),
        SDCARD_INIT_SUCCESS(2),
        SDCARD_REMOVED(3),
        SDCARD_UNSUPPORTED(4),
        SDCARD_INIT_FAIL(5),
        SDCARD_UNRECOGNIZABLE(6),
        SDCARD_UNMOUNTED(7),
        SDCARD_FULL_LIMIT(8),
        SDCARD_FULL_LIMIT_EXIT(9),
        SDCARD_EVENT_FILE_LIMIT(10),
        SDCARD_EVENT_PICTURE_LIMIT(11),
        SDCARD_ASKEY_NOT_SUPPORTED(12);

        public final int value;
        SDcardStatusType(int value){
            this.value = value;
        }
        @Override
        public String toString(){
            return String.valueOf(value);
        }
    }
    public enum SecondCameraStatusType{
        CONNECTED(0),
        DISCONNECTED(1);

        public final int value;
        SecondCameraStatusType(int value){
            this.value = value;
        }
        @Override
        public String toString(){
            return String.valueOf(value);
        }
    }
    public enum GPSStatusType{
        GPS_STRENGTH_NONE(0),
        GPS_STRENGTH_NOT_FIXES(1),
        GPS_STRENGTH_FIXES(2);


        public final int value;
        GPSStatusType(int value){
            this.value = value;
        }
        @Override
        public String toString(){
            return String.valueOf(value);
        }
    }
    public enum FOTAFileStatus{
        FOTA_FILE_EXIST(0),
        FOTA_FILE_NOT_EXIST(1);
        public final int value;
        FOTAFileStatus(int value){
            this.value = value;
        }
        @Override
        public String toString(){
            return String.valueOf(value);
        }
    }
    public enum SimCardStatus{
        SIM_STATE_UNKNOWN(0),
        SIM_STATE_ABSENT(1),
        SIM_STATE_PIN_REQUIRED(2),
        SIM_STATE_PUK_REQUIRED(3),
        SIM_STATE_NETWORK_LOCKED(4),
        SIM_STATE_READY(5),
        SIM_STATE_NOT_READY(6),
        SIM_STATE_ILLEGAL(7);

        public final int value;
        SimCardStatus(int value){
            this.value = value;
        }
        @Override
        public String toString(){
            return String.valueOf(value);
        }

    }
    public enum SwitchUserEvent{
        SWITCH_USER_PREPARE(0),
        SWITCH_USER_START(1),
        SWITCH_USER_COMPLETE(2);

        public final int value;
        SwitchUserEvent(int value){
            this.value = value;
        }
        @Override
        public String toString(){
            return String.valueOf(value);
        }
    }
    public enum SdCardAndSimCardCheckStatus{
        CHECK_START(0),
        CHECK_STOP(1),
        CHECK_COMPLETE(2);
        public final int value;
        SdCardAndSimCardCheckStatus(int value){
            this.value = value;
        }
        @Override
        public String toString(){
            return String.valueOf(value);
        }
    }
    public enum RecordingPreconditionStatus{
        BATTERY_STATUS_CHARGING(0),
        BATTERY_STATUS_DISCHARGING(1),
        SDCARD_AVAILABLE(2),
        SDCARD_UNAVAILABLE(3),
        SDCARD_RECORDING_FULL_LIMIT(4),
        SDCARD_RECORDING_FULL_LIMIT_EXIT(5),
        SDCARD_EVENT_FILE_OVER_LIMIT(6),
        SDCARD_UNREACH_EVENT_FILE_LIMIT(7),
        SDCARD_PICTURE_FILE_OVER_LIMIT(8),
        SDCARD_UNREACH_PICTURE_LIMIT(9),
        SWITCH_USER_STARTED(10),
        SWITCH_USER_COMPLETED(11),
        LOW_TEMPERATURE(12),
        HIGH_TEMPERATURE(13);

        public final int value;
        RecordingPreconditionStatus(int value){
            this.value = value;
        }
        @Override
        public String toString(){
            return String.valueOf(value);
        }
    }

}