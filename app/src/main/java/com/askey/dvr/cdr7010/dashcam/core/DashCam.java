package com.askey.dvr.cdr7010.dashcam.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.location.Location;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.Looper;

import com.askey.dvr.cdr7010.dashcam.adas.AdasController;
import com.askey.dvr.cdr7010.dashcam.adas.AdasStateListener;
import com.askey.dvr.cdr7010.dashcam.core.StateMachine.EEvent;
import com.askey.dvr.cdr7010.dashcam.core.camera2.Camera2Controller;
import com.askey.dvr.cdr7010.dashcam.core.camera2.CameraControllerListener;
import com.askey.dvr.cdr7010.dashcam.core.recorder.ExifHelper;
import com.askey.dvr.cdr7010.dashcam.core.recorder.Recorder;
import com.askey.dvr.cdr7010.dashcam.core.renderer.EGLRenderer;
import com.askey.dvr.cdr7010.dashcam.service.FileManager;
import com.askey.dvr.cdr7010.dashcam.service.GPSStatusManager;
import com.askey.dvr.cdr7010.dashcam.util.Logg;
import com.askey.dvr.cdr7010.dashcam.util.SDcardHelper;
import com.askey.dvr.cdr7010.dashcam.util.SetUtils;

import java.util.EnumSet;
import net.sf.marineapi.nmea.util.Position;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.List;

public class DashCam implements DashCamControl, AdasStateListener {

    private String TAG = "DashCam";
    private final Handler mMainThreadHandler;
    private Context mContext;
    private RecordConfig mConfig;
    private Camera2Controller mCamera2Controller;
    private EGLRenderer mRenderer;
    private Recorder mRecorder;
    private SurfaceTexture mSurfaceTexture;
    private StateCallback mStateCallback;
    private StateMachine mStateMachine;
    private AdasController mAdasController;


    /**
     * Functions which need to start Camera
     * Use Function enum & EnumSet to know which function is enabled
     * And does Camera needs to be re-start to disable some outputs (Surface/ImageReader)
     */
    private enum Function {
        RECORD, ADAS
    }

    // Current Enabled Functions
    private final EnumSet<Function> mEnabledFunctions = EnumSet.noneOf(Function.class);

    // User Set / Conditions Control may disable/enable functions and set to this variable
    private final EnumSet<Function> mSetEnabledFunctions = EnumSet.noneOf(Function.class);

    // Check Function is ready to start see @setFunctionReady
    private final EnumSet<Function> mReadyFunctions = EnumSet.noneOf(Function.class);


    private enum Error {
        CAMERA_ACCESS_EXCEPTION
    }
    // Error flags to know what error was happened
    private final EnumSet<Error> mErrorFlag = EnumSet.noneOf(Error.class);

    public interface StateCallback {
        void onStarted();

        void onStoped();

        void onError();

        void onEventStateChanged(boolean on);

        void onEventCompleted(int evevtId, long timestamp, List<String> pictures, String video);
    }

    private boolean mRecording = false;
    private Recorder.StateCallback mRecorderCallback = new Recorder.StateCallback() {
        @Override
        public void onStarted() {
            Logg.d(TAG, "RecorderStateCallback: onStarted");
            mRecording = true;
            if (mStateCallback != null) {
                mStateCallback.onStarted();
            }
        }

        @Override
        public void onStoped() {
            Logg.d(TAG, "RecorderStateCallback: onStoped");
            mRecording = false;
            checkCloseSuccess();
            if (mStateCallback != null) {
                mStateCallback.onStoped();
            }
        }

        @Override
        public void onInterrupted() {
            Logg.d(TAG, "RecorderStateCallback: onInterrupted");
            mRecording = false;
            mStateMachine.dispatchEvent(new StateMachine.Event(EEvent.ERROR,
                    "RecorderStateCallback: onInterrupted"));
            if (mStateCallback != null) {
                mStateCallback.onError();
            }
        }

        @Override
        public void onEventStateChanged(boolean on) {
            Logg.d(TAG, "RecorderStateCallback: onEventStateChanged " + on);
            if (mStateCallback != null) {
                mStateCallback.onEventStateChanged(on);
            }
        }

        @Override
        public void onEventCompleted(int eventId, long timestamp, List<String> pictures, String video) {
            Logg.d(TAG, "RecorderStateCallback: onEventCompleted ");
            if (mStateCallback != null) {
                mStateCallback.onEventCompleted(eventId, timestamp, pictures, video);
            }
        }
    };

    public DashCam(Context context, RecordConfig config, StateCallback callback) {
        TAG = TAG + "-" + config.cameraId();
        mContext = context.getApplicationContext();
        mConfig = config;
        mStateCallback = callback;
        mStateMachine = new StateMachine(this);
        mMainThreadHandler = new Handler(Looper.getMainLooper());
        if (config.adasEnable()) {
            mAdasController = AdasController.getsInstance();
        }
    }

    public boolean isBusy() {
        return mStateMachine.getCurrentState() != mStateMachine.STATE_CLOSE;
    }

    public void startVideoRecord(String reason) {
        Logg.d(TAG, "startVideoRecord " + reason);
        enableFunction(Function.RECORD);
    }

    public void stopVideoRecord(String reason) {
        Logg.d(TAG, "stopVideoRecord " + reason);
        disableFunction(Function.RECORD);
    }

    public void enableAdas(boolean enabled) {
        if (enabled) {
            mAdasController.init(mContext);
            mAdasController.addListener(this);
            enableFunction(Function.ADAS);
        } else {
            disableFunction(Function.ADAS);
            mAdasController.removeListener(this);
            mAdasController.finish();
        }
    }

    private synchronized void enableFunction(Function function) {
        Logg.v(TAG, "enableFunction: " + function.name());
        if (mSetEnabledFunctions.contains(function)) {
            return;
        }
        mSetEnabledFunctions.add(function);
        mMainThreadHandler.removeCallbacks(checkEnabledFunctions);
        mMainThreadHandler.postDelayed(checkEnabledFunctions, 100);
    }

    private synchronized void disableFunction(Function function) {
        Logg.v(TAG, "disableFunction: " + function.name());
        if (!mSetEnabledFunctions.contains(function)) {
            return;
        }
        mSetEnabledFunctions.remove(function);
        mMainThreadHandler.removeCallbacks(checkEnabledFunctions);
        mMainThreadHandler.postDelayed(checkEnabledFunctions, 100);
    }

    private synchronized void clearFunctionReady() {
        mMainThreadHandler.post(() -> {
            mReadyFunctions.clear();
        });

    }

    /**
     * A function may need sometimes to be ready.
     * This method set and check all functions is ready to start the Camera
     * @param function the function which is ready to start
     */
    private void setFunctionReady(Function function) {
        mMainThreadHandler.post(() -> {
            Logg.v(TAG, "setFunctionReady: " + function);
            if (mReadyFunctions.contains(function)) {
                return;
            }
            mReadyFunctions.add(function);
            Logg.v(TAG, "setFunctionReady: mReadyFunctions=" + mReadyFunctions + ", mSetEnabledFunctions=" + mSetEnabledFunctions);
            if (SetUtils.equals(mReadyFunctions, mSetEnabledFunctions)) {
                try {
                    startCamera();
                } catch (CameraAccessException e) {
                    Logg.e(TAG, e.getMessage());
                    mErrorFlag.add(Error.CAMERA_ACCESS_EXCEPTION);
                    mStateCallback.onError();
                    mStateMachine.dispatchEvent(new StateMachine.Event(EEvent.ERROR, e.getMessage()));
                }
            }
        });
    }

    /**
     * Check the changes of functions to change the state of StateMachine
     */
    private Runnable checkEnabledFunctions = () -> {
        Logg.v(TAG, "checkEnabledFunctions: currentTimeMillis=" + System.currentTimeMillis());
        Logg.v(TAG, "checkEnabledFunctions: mSetEnabledFunctions=" + mSetEnabledFunctions + ", mEnabledFunctions=" + mEnabledFunctions);
        if (SetUtils.equals(mSetEnabledFunctions, mEnabledFunctions)) {
            return;
        }

        StateMachine.EEvent event = null;
        String reason = null;
        if (!mEnabledFunctions.contains(Function.RECORD) && mSetEnabledFunctions.contains(Function.RECORD)) {
            event = StateMachine.EEvent.OPEN;
            reason = "No-RECORD --> RECORD";
        } else if (mEnabledFunctions.contains(Function.RECORD) && !mSetEnabledFunctions.contains(Function.RECORD)) {
            event = StateMachine.EEvent.CLOSE;
            reason = "RECORD --> No-RECORD";
        } else if (mSetEnabledFunctions.contains(Function.RECORD) || mSetEnabledFunctions.contains(Function.ADAS)) {
            event = StateMachine.EEvent.OPEN;
            reason = "RECORD or ADAS is enabled: " + mSetEnabledFunctions;
        } else if (mSetEnabledFunctions.size() == 0) {
            event = StateMachine.EEvent.CLOSE;
            reason = "RECORD and ADAS are both disabled";
        }

        if (event != null) {
            mStateMachine.dispatchEvent(new StateMachine.Event(event, reason));
        }

        mEnabledFunctions.clear();
        mEnabledFunctions.addAll(mSetEnabledFunctions);
    };

    public void mute() {
        mStateMachine.dispatchEvent(new StateMachine.Event(EEvent.MUTE, ""));
    }

    public void demute() {
        mStateMachine.dispatchEvent(new StateMachine.Event(EEvent.UNMUTE, ""));
    }


    @Override
    public void onOpenCamera() throws Exception {
        Logg.v(TAG, "onOpenCamera");
        clearFunctionReady();
        mCamera2Controller = new Camera2Controller(mContext, mCameraListener, mMainThreadHandler);
        mCamera2Controller.startBackgroundThread();
        mCamera2Controller.open(mConfig.cameraId());
    }

    @Override
    public void onCameraClosed() {
        Logg.v(TAG, "onCameraClosed: mErrorFlag = " + mErrorFlag);
        if (mSetEnabledFunctions.size() != 0) {
            long delayMillis = 0;
            if (mErrorFlag.size() != 0) {
			    // Any errors, wait for 3 seconds and try again
                delayMillis = 3000;
                mErrorFlag.clear();
            }
            String reason = "onCameraClosed but mSetEnabledFunctions = " + mSetEnabledFunctions;
            StateMachine.Event event = new StateMachine.Event(EEvent.OPEN, reason);
            mStateMachine.dispatchEventDelayed(event, delayMillis);
        }
    }

    @Override
    public void onStartVideoRecord() {
        Logg.d(TAG, "onStartVideoRecord");

    }

    public void takeAPicture(final Handler handler) {
        if (mRenderer != null) {
            mRenderer.takeDisplaySnapshot(new EGLRenderer.SnapshotCallback() {
                @Override
                public void onSnapshotAvailable(byte[] data, int width, int height, long timeStamp) {
                    Bitmap bmp = null;
                    BufferedOutputStream bos = null;
                    try {
                        String filePathForPicture = FileManager.getInstance(mContext).getFilePathForPicture(mConfig.cameraId(), timeStamp);
                        ByteBuffer buf = ByteBuffer.wrap(data);
                        bos = new BufferedOutputStream(new FileOutputStream(filePathForPicture));
                        bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                        bmp.copyPixelsFromBuffer(buf);
                        bmp = convertBmp(bmp);
                        bmp.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                        Location currentLocation = GPSStatusManager.getInstance().getCurrentLocation();
                        Position position = null;
                        if (currentLocation != null) {
                            Logg.d(TAG, "currentLocation!=null,getLatitude==" + currentLocation.getLatitude() + ",getLongitude==" + currentLocation.getLongitude());
                            position = new Position(currentLocation.getLatitude(), currentLocation.getLongitude());
                        }
                        Logg.d(TAG, "timeStamp==" + timeStamp);
                        ExifHelper.build(filePathForPicture, timeStamp, position);
                        // TODO: 2018/6/28 上傳文件
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (bmp != null) {
                            bmp.recycle();
                        }
                        if (bos != null) {
                            try {
                                bos.flush();
                                bos.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        handler.sendEmptyMessage(0);
                    }
                }
            });
        }
    }

    private void startCamera() throws CameraAccessException {
        Logg.d(TAG, "startCamera");
        mCamera2Controller.startRecordingVideo();
        if (mRecorder != null) {
            mRecorder.startRecording();
        }
    }

    @Override
    public void onStopVideoRecord() {
        Logg.d(TAG, "onStopVideoRecord");
        mCamera2Controller.stopRecordingVideo();
        try {
            mCamera2Controller.closeCamera();
        } catch (CameraAccessException e) {
            // Nothing we can do here, looks like must fix if any error occurs
            Logg.e(TAG, e.getMessage());
        } finally {
            if (mRenderer != null) {
                mRenderer.stop();
            }

            if (mRecorder != null) {
                mRecorder.stopRecording();
                mRecorder.release();
                mRecorder = null;
            }
            checkCloseSuccess();
        }
    }

    @Override
    public void onMuteAudio() {
        Logg.d(TAG, "onMuteAudio");
        if (mRecorder != null) {
            mRecorder.mute();
        }
    }

    @Override
    public void onDemuteAudio() {
        Logg.d(TAG, "onDemuteAudio");
        if (mRecorder != null) {
            mRecorder.demute();
        }
    }

    @Override
    public void onAdasStarted() {
        Logg.v(TAG, "onAdasStarted");
        enableFunction(Function.ADAS);
    }

    @Override
    public void onAdasStopped() {
        Logg.v(TAG, "onAdasStopped");
        disableFunction(Function.ADAS);
    }

    private CameraControllerListener mCameraListener = new CameraControllerListener() {
        @Override
        public void onCameraOpened() {
            Logg.v(TAG, "onCameraOpened");
            if (mSetEnabledFunctions.contains(Function.RECORD)) {
                try {
                    prepareRecorder();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (mAdasController != null) {
                prepareAdas(mCamera2Controller);
            }
        }

        @Override
        public void onCameraClosed() {
            Logg.v(TAG, "onCameraClosed");
        }

        @Override
        public void onCaptureStarted() {
            Logg.v(TAG, "onCaptureStarted");
            mStateMachine.dispatchEvent(new StateMachine.Event(StateMachine.EEvent.OPEN_SUCCESS, "onCaptureStarted"));
        }

        @Override
        public void onCaptureStopped() {
            Logg.v(TAG, "onCaptureStopped");
            checkCloseSuccess();
        }
    };

    private void checkCloseSuccess() {
        if (mCamera2Controller.getState() == Camera2Controller.State.STOPPED
                && !mRecording) {
            mStateMachine.dispatchEvent(new StateMachine.Event(StateMachine.EEvent.CLOSE_SUCCESS, "checkCloseSuccess"));
        }
    }

    private void prepareRecorder() throws Exception {
        Logg.v(TAG, "prepareRecorder");

        int sdcardStatus = FileManager.getInstance(mContext).checkSdcardAvailable();
        if (!SDcardHelper.isSDCardAvailable(sdcardStatus)) {
            if (mStateCallback != null) {
                mStateCallback.onError();
            }
            throw new RuntimeException("sd card unavailable");
        }


        mRecorder = new Recorder(mContext, mConfig, mRecorderCallback);
        mRecorder.prepare();

        mRenderer = new EGLRenderer(mContext,
                mConfig.videoWidth(),
                mConfig.videoHeight(),
                mConfig.videoStampEnable(),
                new EGLRenderer.OnSurfaceTextureListener() {
                    @Override
                    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                        mSurfaceTexture = surfaceTexture;
                        mRenderer.createEncoderSurface(mRecorder.getInputSurface(), mRecorder);
                        try {
                            mCamera2Controller.setSurface(surfaceTexture);
                            setFunctionReady(Function.RECORD);
                        } catch (Exception e) {
                            mStateMachine.dispatchEvent(new StateMachine.Event(EEvent.ERROR, ""));
                        }
                    }

                    @Override
                    public void onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                        mSurfaceTexture = null;
                    }
                });
        mRenderer.start();

    }

    private void prepareAdas(Camera2Controller camera2Controller) {
        Logg.v(TAG, "prepareAdas");
        camera2Controller.setImageReader(mAdasController.getImageReader());
        setFunctionReady(Function.ADAS);
    }

    private static Bitmap convertBmp(Bitmap bmp) {
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        Bitmap convertBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);// 创建一个新的和SRC长度宽度一样的位图
        Canvas cv = new Canvas(convertBmp);
        Matrix matrix = new Matrix();
//        matrix.postScale(1, -1); //镜像垂直翻转
        matrix.postScale(-1, 1); //镜像水平翻转
        matrix.postRotate(-180); //旋转-180度
        Bitmap newBmp = Bitmap.createBitmap(bmp, 0, 0, w, h, matrix, true);
        cv.drawBitmap(newBmp, new Rect(0, 0, newBmp.getWidth(), newBmp.getHeight()), new Rect(0, 0, w, h), null);
        newBmp.recycle();
        bmp.recycle();
        return convertBmp;
    }
}
