package com.askey.dvr.cdr7010.dashcam.activity;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;

import com.askey.dvr.cdr7010.dashcam.R;
import com.askey.dvr.cdr7010.dashcam.domain.KeyAdapter;
import com.askey.dvr.cdr7010.dashcam.service.DialogManager;
import com.askey.dvr.cdr7010.dashcam.ui.MainActivity;
import com.askey.dvr.cdr7010.dashcam.util.ActivityUtils;
import com.askey.dvr.cdr7010.dashcam.util.Const;
import com.askey.dvr.cdr7010.dashcam.util.Logg;
import com.askey.dvr.cdr7010.dashcam.widget.CommDialog;
import com.askey.dvr.cdr7010.dashcam.widget.WarningDialog;


public abstract class DialogActivity extends AppCompatActivity {
    private static final String TAG = DialogActivity.class.getSimpleName();
    public static final int DIALOG_TYPE_SDCARD = 1;
    public static final int DIALOG_TYPE_WARNING = 2;
    public static final int DIALOG_TYPE_COMM_TEXT = 3;
    public static final int DIALOG_TYPE_COMM_CONFIRM = 4;
    public static final int DIALOG_TYPE_ERROR =5;
    private Dialog dialog = null;
    private int dialogType = 0;
    private AudioManager audioManager;
    private int maxVolume,currentVolume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DialogManager.getIntance().registerContext(this);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(handleKeyEvent(event)){
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        Logg.d(TAG, "onCreateDialog id=" + id);
        final int dialogMode = id;
        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == Dialog.BUTTON_POSITIVE) {
                    DialogActivity.this.onHandleCommDialogEvent((Dialog) dialog, 0, dialogMode);
                }
                if (which == Dialog.BUTTON_NEGATIVE) {
                    DialogActivity.this.onHandleCommDialogEvent((Dialog) dialog, 1, dialogMode);
                }
            }
        };
        DialogInterface.OnKeyListener onKeyListener = new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
           //     return handleKeyEvent(event);
                return true;
            }
        };
        switch (id) {
            case DIALOG_TYPE_SDCARD:
                dialog = new CommDialog(this, R.style.dialogNoTitle);
                ((CommDialog) dialog).setMessage(args.getString("Message"));
                ((CommDialog) dialog).setType(CommDialog.TYPE_BUTTON_HIDE);
                ((CommDialog) dialog).setNegativeButtonListener(onClickListener);
                ((CommDialog) dialog).setPositiveButtonListener(onClickListener);
                break;
            case DIALOG_TYPE_WARNING:
                dialog = new WarningDialog(this, R.style.dialogNoTitle);
                ((WarningDialog) dialog).setMessage(args.getString("Message"));
                ((WarningDialog) dialog).setOnkeyListener(onKeyListener);
                break;
            case DIALOG_TYPE_COMM_TEXT:
                dialog = new CommDialog(this, R.style.dialogNoTitle);
                ((CommDialog) dialog).setMessage(args.getString("Message"));
                ((CommDialog) dialog).setDialogHeight(args.getInt("Height",0));
                ((CommDialog) dialog).setDialogWidth(args.getInt("Width",0));
                ((CommDialog) dialog).setType(CommDialog.TYPE_BUTTON_HIDE);
                ((CommDialog) dialog).setNegativeButtonListener(onClickListener);
                ((CommDialog) dialog).setPositiveButtonListener(onClickListener);
                break;
            case DIALOG_TYPE_COMM_CONFIRM:
                dialog = new CommDialog(this, R.style.dialogNoTitle);
                ((CommDialog) dialog).setMessage(args.getString("Message"));
                ((CommDialog) dialog).setType(CommDialog.TYPE_BUTTON_OK);
                ((CommDialog) dialog).setNegativeButtonListener(onClickListener);
                ((CommDialog) dialog).setPositiveButtonListener(onClickListener);
                break;
            case DIALOG_TYPE_ERROR:
                dialog = new CommDialog(this, R.style.dialogNoTitle);
                ((CommDialog) dialog).setMessage(args.getString("Message"));
                ((CommDialog) dialog).setDialogHeight(args.getInt("Height",0));
                ((CommDialog) dialog).setDialogWidth(args.getInt("Width",0));
                ((CommDialog) dialog).setType(CommDialog.TYPE_BUTTON_HIDE);
                ((CommDialog) dialog).setNegativeButtonListener(onClickListener);
                ((CommDialog) dialog).setPositiveButtonListener(onClickListener);
                break;
            default:
        }
        return dialog;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        super.onPrepareDialog(id, dialog, args);
        dialogType = id;
        this.dialog = dialog;
        if (id == DIALOG_TYPE_SDCARD) {
            CommDialog sdCardDialog = (CommDialog) dialog;
            sdCardDialog.setMessage(args.getString("Message"));
        }
        if(id == DIALOG_TYPE_WARNING){
            WarningDialog warningDialog = (WarningDialog)dialog;
            warningDialog.setMessage(args.getString("Message"));
        }
        if(id == DIALOG_TYPE_COMM_TEXT){
            CommDialog sdCardDialog = (CommDialog) dialog;
            sdCardDialog.setMessage(args.getString("Message"));
        }
        if(id == DIALOG_TYPE_COMM_CONFIRM){
            CommDialog sdCardDialog = (CommDialog) dialog;
            sdCardDialog.setMessage(args.getString("Message"));
        }
        if(id == DIALOG_TYPE_ERROR){
            CommDialog sdCardDialog = (CommDialog) dialog;
            sdCardDialog.setMessage(args.getString("Message"));
        }
    }

    public boolean isDialogShowing() {
        if (!isFinishing() && null != dialog && dialog.isShowing()) {
            return true;
        }
        return false;
    }

    public void dismissDialog() {
        if (!isFinishing() && null != dialog && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    public int getDialogType() {
        return dialogType;
    }

    public boolean onHandleCommDialogEvent(Dialog dialog, int event, int dialogMode) {
        if (event == 0) {
            switch (dialogMode) {
                case DIALOG_TYPE_SDCARD:
                    break;
                case DIALOG_TYPE_COMM_TEXT:
                    break;
                case DIALOG_TYPE_COMM_CONFIRM:
                    break;
                case DIALOG_TYPE_WARNING:
                    break;
                case DIALOG_TYPE_ERROR:
                    break;
            }
        }
        if (event == 1) {
            switch (dialogMode) {
                case DIALOG_TYPE_SDCARD:
                    break;
                case DIALOG_TYPE_COMM_TEXT:
                    break;
                case DIALOG_TYPE_COMM_CONFIRM:
                    break;
                case DIALOG_TYPE_WARNING:
                    break;
                case DIALOG_TYPE_ERROR:
                    break;
            }
        }
        return true;
    }

    @Override
    public void onDestroy() {
        DialogManager.getIntance().unregisterContext(this);
        super.onDestroy();
    }
    protected  abstract  boolean handleKeyEvent(KeyEvent event);
    private int keydowmRepeatCount =0;

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        keydowmRepeatCount++;
        if(keydowmRepeatCount==1){
            onKeyHoldHalfASecond(keyCode);
        }else if(keydowmRepeatCount==2){
            onKeyHoldOneSecond(keyCode);
        }else if(keydowmRepeatCount==3){
            onKeyHoldThreeSecond(keyCode);
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        event.startTracking();

        if(keydowmRepeatCount==1){
            onContinueKeyHoldHalfASecond(keyCode);
        }else if(keydowmRepeatCount==2){
            onContinueKeyHoldOneSecond(keyCode);
        }else if(keydowmRepeatCount==3){
            onContinueKeyHoldThreeSecond(keyCode);
        }
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        event.startTracking();
        if (keydowmRepeatCount==0) {
            onKeyShortPressed(keyCode);
        }else{
            keydowmRepeatCount= 0;
        }
        return true;
    }

    public void onKeyShortPressed(int keyCode) {

    }

    public  void onKeyHoldHalfASecond(int keyCode){

    }

    public  void onKeyHoldOneSecond(int keyCode){

    }

    public  void onKeyHoldThreeSecond(int keyCode){

    }

    public void onContinueKeyHoldHalfASecond(int keyCode) {

    }

    public void onContinueKeyHoldOneSecond(int keyCode) {

    }

    public void onContinueKeyHoldThreeSecond(int keyCode) {

    }
}