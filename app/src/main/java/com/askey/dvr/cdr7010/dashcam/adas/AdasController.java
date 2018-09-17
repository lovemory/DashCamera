package com.askey.dvr.cdr7010.dashcam.adas;

import android.content.Context;
import android.graphics.ImageFormat;
import android.location.Location;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import com.askey.dvr.cdr7010.dashcam.adas.AdasStatistics.ProfileItem;
import com.askey.dvr.cdr7010.dashcam.adas.AdasStatistics.TimesItem;
import com.askey.dvr.cdr7010.dashcam.service.GPSStatusManager;
import com.askey.dvr.cdr7010.dashcam.util.TimesPerSecondCounter;
import com.jvckenwood.adas.detection.Detection;
import com.jvckenwood.adas.detection.FC_INPUT;
import com.jvckenwood.adas.util.Constant;
import com.jvckenwood.adas.util.FC_PARAMETER;
import com.jvckenwood.adas.util.Util;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static com.jvckenwood.adas.util.Constant.ADAS_ERROR_DETECT_ALREADY_RUNNING_DETECTION;
import static com.jvckenwood.adas.util.Constant.ADAS_SUCCESS;

public class AdasController implements Util.AdasCallback {
    private static final String TAG = AdasController.class.getSimpleName();

    static final int ADAS_IMAGE_WIDTH = 1280;
    static final int ADAS_IMAGE_HEIGHT = 720;
    static final int BUFFER_NUM = 3;
    private static final int EXPECTED_LISTENER_NUM = 1;

    /* Interval of printing statistics data */
    private static long STATISTICS_INTERVAL_MILL_SEC = 30 * 1000;

    /* Used for some synchronized method to change state after return */
    private static final long STATE_CHANGE_DELAY = 100;
    /* FINETUNE: wait for image reader to release */
    private static final long IMAGE_READER_CLOSE_DELAY = 500;

    /* Properties for debug */
    private static boolean DEBUG;
    private static boolean EXCEPTION_WHEN_ERROR;
    private static boolean DEBUG_FPS;
    private static boolean ADAS_DISABLED;
    private static boolean DEBUG_IMAGE_PROCESS;
    private static boolean PRINT_STATISTICS;

    /* Handler Messages */
    private static final String PROP_DEBUG = "persist.dvr.adas.debug";
    private static final String PROP_DEBUG_FPS = "persist.dvr.adas.debug_fps";
    private static final String PROP_ADAS_DISABLED = "persist.dvr.adas.disabled";
    private static final String PROP_EXCEPTION = "persist.dvr.adas.exception";
    private static final String PROP_DEBUG_PROCESS = "persist.dvr.adas.dbg_proc";
    private static final String PROP_FAKE_SPEED = "persist.dvr.adas.speed";
    private static final String PROP_STATISTICS = "persist.dvr.adas.stat";

    private static AdasController sInstance;

    private Util mAdasImpl;
    private final TimesPerSecondCounter mTpsc;
    private final TimesPerSecondCounter mTpscDidAdas;
    private final TimesPerSecondCounter mTpscFrameDrop;
    private FC_INPUT mFcInput;
    private Queue<ImageRecord> mProcessingImages;
    private Handler mHandler;
    private List<WeakReference<AdasStateListener>> mListeners;
    private ImageReader mImageReader;
    private float mFakeSpeed;
    private float mLocationSpeed = 0;
    private AdasStatistics mStatistics;
    private ReentrantLock mProcessingLock;

    public enum State {
        Uninitialized, Initializing, Stopped, Starting, Started, Stopping, Finishing
    }

    private State mState;

    private AdasController() {
        if (sInstance != null) {
            throw new RuntimeException("Singleton instance is already created!");
        }
        mState = State.Uninitialized;
        mStatistics = new AdasStatistics();
        mProcessingLock = new ReentrantLock();
        mAdasImpl = Util.getInstance();
        mTpsc = new TimesPerSecondCounter(TAG);
        mTpscDidAdas = new TimesPerSecondCounter(TAG + "_didAdas");
        mTpscFrameDrop = new TimesPerSecondCounter(TAG + "_frameDrop");
        mFcInput = new FC_INPUT();
        mProcessingImages = new LinkedList<>();
        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        mHandler = new AdasHandler(handlerThread.getLooper());
        mListeners = new ArrayList<>();

        ADAS_DISABLED = SystemPropertiesProxy.getBoolean(PROP_ADAS_DISABLED, false);
        Log.v(TAG, "AdasController: ADAS_DISABLED = " + ADAS_DISABLED);

        DEBUG_IMAGE_PROCESS = SystemPropertiesProxy.getBoolean(PROP_DEBUG_PROCESS, false);
        Log.v(TAG, "AdasController: DEBUG_IMAGE_PROCESS = " + DEBUG_IMAGE_PROCESS);

        mFakeSpeed = SystemPropertiesProxy.getInt(PROP_FAKE_SPEED, DEBUG ? 70: 0);
        Log.v(TAG, "AdasController: mFakeSpeed = " + mFakeSpeed);

        /* DEBUG variable enables below DEBUG*/
        DEBUG = SystemPropertiesProxy.getBoolean(PROP_DEBUG, false);
        Log.v(TAG, "AdasController: DEBUG = " + DEBUG);
        EXCEPTION_WHEN_ERROR = SystemPropertiesProxy.getBoolean(PROP_EXCEPTION, DEBUG);
        Log.v(TAG, "AdasController: EXCEPTION_WHEN_ERROR = " + EXCEPTION_WHEN_ERROR);
        PRINT_STATISTICS = SystemPropertiesProxy.getBoolean(PROP_STATISTICS, DEBUG);
        Log.v(TAG, "AdasController: PRINT_STATISTICS = " + PRINT_STATISTICS);
        DEBUG_FPS = SystemPropertiesProxy.getBoolean(PROP_DEBUG_FPS, DEBUG);
        Log.v(TAG, "AdasController: DEBUG_FPS = " + DEBUG_FPS);
        if (DEBUG)
            STATISTICS_INTERVAL_MILL_SEC = 3 * 1000;

    }

    public static AdasController getsInstance() {
        if (sInstance == null) {
            synchronized (AdasController.class) {
                if (sInstance == null) {
                    sInstance = new AdasController();
                }
            }
        }
        return sInstance;
    }

    public void init(Context context) {
        if (ADAS_DISABLED) {
            Log.w(TAG, "init: ADAS_DISABLED");
            return;
        }
        Log.v(TAG, "init");

        assertState("init", State.Uninitialized);

        changeState(State.Initializing);
        FC_PARAMETER fp = FcGetter.getFCParam();

        Log.v(TAG, "initAdas: FC_PARAMETER = " + fp);
        mStatistics.logStart(ProfileItem.Init);
        int result = mAdasImpl.initAdas(fp, context, this);
        mStatistics.logFinish(ProfileItem.Init);
        if (result != ADAS_SUCCESS) {
            handleError("init", "initAdas() failed with return value = " + result);
        }
        assertState("init", State.Initializing);
        changeState(State.Stopped);
    }

    private void changeState(State state) {
        Log.v(TAG, "changeState: state = " + state);
        if (mState != state) {
            mState = state;
            notifyStateChanged(mState);
        }
    }

    private synchronized void notifyStateChanged(State state) {
        removeNullWeakRefs(mListeners);
        synchronized (mListeners) {
            Iterator<WeakReference<AdasStateListener>> iterator = mListeners.iterator();
            while (iterator.hasNext()) {
                WeakReference<AdasStateListener> weakRef = iterator.next();
                AdasStateListener listener = weakRef.get();
                if (listener != null) {
                    listener.onStateChanged(state);
                    continue;
                }
            }
        }

    }

    private void removeNullWeakRefs(List<WeakReference<AdasStateListener>> collection) {
        synchronized (collection) {
            Iterator<WeakReference<AdasStateListener>> iterator = collection.iterator();
            while (iterator.hasNext()) {
                if (iterator.next().get() == null) {
                    iterator.remove();
                }
            }
        }
    }

    private void process(Image image) {
        if (ADAS_DISABLED
                || mState != State.Started
                || mProcessingLock.isLocked()) {
            // Too much logs
            // Log.w(TAG, "process: ADAS_DISABLED=" + ADAS_DISABLED + ", mState=" + mState);
            image.close();
            return;
        }
        if (DEBUG_FPS) {
            mTpsc.update();
        }

        assertState("process", State.Started);
        mHandler.post(()->process_internal(image));
    }

    private void process_internal(Image image) {
        assertState("process_internal", State.Started);
        if (!mProcessingLock.tryLock()) { // Unlock when process is done (didAdasDetect_internal)
            Log.w(TAG, "process: \"stop()\" should has acquired the lock first!!!");
            image.close();
            return;
        }

        if (DEBUG_IMAGE_PROCESS) {
            Log.v(TAG, "process_internal: image = " + image);
        }

        mFcInput.VehicleSpeed = (int) (getSpeed() * 10);
        long timestamp = image.getTimestamp() / 1000000; // nano to ms
        mFcInput.CaptureTime = timestamp / 1000;
        mFcInput.CaptureMilliSec = timestamp % 1000;
        ImageRecord imageRecord = ImageRecord.obtain(mFcInput.CaptureMilliSec, image);

        if (mState != State.Started) {
            Log.v(TAG, "process_internal: not Stopped = " + image);
            imageRecord.recycle();
            return;
        }

        if (Detection.isRunningDetection()) {
            if (DEBUG_FPS) {
                mTpscFrameDrop.update();
            }
            imageRecord.recycle();
            return;
        }

        ByteBuffer y = image.getPlanes()[0].getBuffer();
        ByteBuffer u = image.getPlanes()[1].getBuffer();
        ByteBuffer v = image.getPlanes()[2].getBuffer();
        mStatistics.logStart(ProfileItem.Process);
        int result = Detection.adasDetect(y, u, v, mFcInput);

        if (result == ADAS_ERROR_DETECT_ALREADY_RUNNING_DETECTION) {
            if (DEBUG_IMAGE_PROCESS) {
                Log.v(TAG, "process_internal: ADAS_ERROR_DETECT_ALREADY_RUNNING_DETECTION: close " + imageRecord);
            }
            if (DEBUG_FPS) {
                mTpscFrameDrop.update();
            }
            imageRecord.recycle();
        } else if (result != Constant.ADAS_SUCCESS) {
            Log.e(TAG, "process_internal: adasDetect() result = " + result + ", close " + imageRecord);
            imageRecord.recycle();
        } else {
            assert result == Constant.ADAS_SUCCESS;

            mProcessingImages.add(imageRecord);
            if (DEBUG_IMAGE_PROCESS) {
                Log.v(TAG, "process_internal: new image queued: " + mProcessingImages.size()
                        + ", " + imageRecord + ", speed = " + mFcInput.VehicleSpeed);
            }

        }
    }

    @Override
    public void didAdasDetect(long captureTimeMs) {
        if (DEBUG_IMAGE_PROCESS) {
            Log.v(TAG, "didAdasDetect: captureTime = " + captureTimeMs);
        }
        mStatistics.logFinish(ProfileItem.Process);
        if (DEBUG_FPS) mTpscDidAdas.update();
        mHandler.post(()->didAdasDetect_internal(captureTimeMs));
    }

    private void didAdasDetect_internal(long captureTimeMs) {
        if (DEBUG_IMAGE_PROCESS) {
            Log.v(TAG, "didAdasDetect_internal: captureTimeMs = " + captureTimeMs);
        }

        ImageRecord imageRecord = mProcessingImages.remove();
        if (imageRecord.getTimestamp() != captureTimeMs) {
            throw new RuntimeException("callback captureTimeMs=" + captureTimeMs + ", but oldest record timestamp=" + imageRecord.getTimestamp());
        }
        if (DEBUG_IMAGE_PROCESS) {
            Log.v(TAG, "didAdasDetect_internal: close image = " + imageRecord);
            Log.v(TAG, "didAdasDetect_internal: image dequeue = " + processingImagesToString());
        }
        imageRecord.recycle();
        if (mState == State.Stopping) {
            Log.w(TAG, "didAdasDetect_internal: stop is waiting the lock...");
            if (mProcessingImages.size() > 0) {
                throw new AssertionError("mProcessingImages.size() > 0");
            }
            Log.v(TAG, "didAdasDetect_internal: mImageReader.close() = " + mImageReader);

            mImageReader.close();
            mImageReader = null;
        }
        mProcessingLock.unlock();
    }

    private float getSpeed() {
        if (mFakeSpeed != 0) {
            return mFakeSpeed;
        }
        Location location = GPSStatusManager.getInstance().getCurrentLocation();
        if (location != null) {
            mLocationSpeed = location.getSpeed() * 3.6f; // Convert m/s to km/h
        }
        return mLocationSpeed;
    }

    public synchronized void finish() {
        Log.v(TAG, "finish");
        if (ADAS_DISABLED) {
            return;
        }

        assertState("finish", State.Stopped);
        changeState(State.Finishing);
        mStatistics.logStart(ProfileItem.Finish);
        int result = mAdasImpl.finishAdas();
        if (result != Constant.ADAS_SUCCESS) {
            handleError("finish",
                    "finishAdas() failed with return value = " + result);
        }
    }

    @Override
    public synchronized void didAdasFinish(int i) {
        Log.v(TAG, "didAdasFinish");
        mStatistics.logFinish(ProfileItem.Finish);
        assertState("didAdasFinish", State.Finishing);
        changeState(State.Uninitialized);
    }

    @Override
    public void adasFailure(int i) {
        Log.e(TAG, "adasFailure: " + i);
    }

    private String processingImagesToString() {
        StringBuffer sb = new StringBuffer();
        for (ImageRecord ir :
                mProcessingImages) {
            sb.append(ir + ", ");
        }
        return sb.toString();
    }

    public void addListener(AdasStateListener listener) {
        Log.v(TAG, "addListener: " + listener);
        if (ADAS_DISABLED) {
            return;
        }
        WeakReference<AdasStateListener> weakListener =
                new WeakReference<>(listener);
        removeNullWeakRefs(mListeners);
        synchronized (mListeners) {
            if (mListeners.contains(weakListener)) {
                handleError("addListener", "Add a listener multi-times");
                return;
            }

            mListeners.add(weakListener);
            if (mListeners.size() > EXPECTED_LISTENER_NUM) {
                handleError("addListener", "mListeners.size() = " + mListeners.size());
            }
        }
    }

    public void removeListener(AdasStateListener listener) {
        // Log.v(TAG, "removeListener: " + listener);
        if (ADAS_DISABLED) {
            return;
        }
        boolean found = false;
        removeNullWeakRefs(mListeners);
        synchronized (mListeners) {
            Iterator<WeakReference<AdasStateListener>> iterator = mListeners.iterator();
            while (iterator.hasNext()) {
                WeakReference<AdasStateListener> weakRef = iterator.next();
                if (weakRef.get() == null) {
                    continue;
                }
                if (weakRef.get() == listener) {
                    iterator.remove();
                    found = true;
                }
            }
        }
        if (!found) {
            handleError("removeListener", listener + " never added");
        }
    }

    /**
     * obtainImageReader create new ImageReader every time
     * Because sometimes cameraserver died and may hold its connection and cannot release
     * Always use a new ImageReader fix this issue
     * @return
     */
    private void obtainImageReader() {
        if (mImageReader != null) {
            handleError("obtainImageReader", "mImageReader may not close correctly (leak)");
        }
        mImageReader = ImageReader.newInstance(ADAS_IMAGE_WIDTH, ADAS_IMAGE_HEIGHT, ImageFormat.YUV_420_888, BUFFER_NUM);
        mStatistics.log(TimesItem.NewImageReader);
        Log.v(TAG, "obtainImageReader: new mImageReader = " + mImageReader);
        mImageReader.setOnImageAvailableListener(reader -> {
            Image image = null;
            try {
                image = reader.acquireLatestImage();
                if (image != null) {
                    process(image);
                }
            } catch (IllegalStateException e) {
                // FIXME: find the root cause
                handleError( "onImageAvailable", "FIXME, " + e.getMessage());
            }
        }, null);
    }

    public ImageReader getImageReader() {
        return mImageReader;
    }

    private class AdasHandler extends Handler {
        public AdasHandler(Looper looper) {
            super(looper);
        }

    }

    private void handleError(String func, String message) {
        String completeMessage = func + ", " + message;
        if (EXCEPTION_WHEN_ERROR) {
            throw new RuntimeException(completeMessage);
        }
        Log.e(TAG, completeMessage);
    }

    /**
     * Asynchronized call
     * Wait for State.Started to ensure it is Started
     */
    public void start() {
        if (ADAS_DISABLED) {
            Log.w(TAG, "start: ADAS_DISABLED");
            return;
        }
        Log.v(TAG, "start");

        assertState("start", State.Stopped);
        if (mImageReader != null) {
            Log.w(TAG, "start: closing mImageReader...");
            mImageReader.close();
            mImageReader = null;
        }
        obtainImageReader();

        mHandler.removeCallbacks(mPrintStatistics);
        mHandler.postDelayed(mPrintStatistics, STATISTICS_INTERVAL_MILL_SEC);

        changeState(State.Started);
    };

    private Runnable mPrintStatistics = () -> {
        Log.v(TAG, "mPrintStatistics: " + mStatistics);
        if (mState == State.Started) {
            mHandler.postDelayed(this.mPrintStatistics, STATISTICS_INTERVAL_MILL_SEC);
        }
    };

    /**
     * Synchronized call
     * When it is returned, state changed to State.Stopped
     */
    public void stop() {
        if (ADAS_DISABLED) {
            Log.w(TAG, "stop: ADAS_DISABLED");
            return;
        }
        Log.v(TAG, "stop");
        mStatistics.logStart(ProfileItem.Stop);
        assertState("stop", State.Started);
        try {
            if (!mProcessingLock.tryLock()) { // Unlock in stop_internal
                Log.w(TAG, "stop: \"process()\" has acquired the lock first!!!");
                changeState(State.Stopping);
                if (mProcessingLock.tryLock(1000, TimeUnit.MILLISECONDS)) {
                    Log.w(TAG, "stop: acquire the lock!!!");
                } else {
                    Log.e(TAG, "stop: check why \"process()\" hasn't release the lock");
                    throw new RuntimeException("Timeout: cannot acquire the lock");
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            mProcessingLock.unlock();
            mStatistics.logFinish(ProfileItem.Stop);
            changeState(State.Stopped);
            Log.v(TAG, "stop: END");
        }
    }

    private void assertState(String funcName, State state) {
        if (mState != state) {
            try {
                handleError(funcName, "Assert state = " + state
                        + ", mState = " + mState);
            } catch (RuntimeException e) {
                throw new IllegalStateException(e.getMessage());
            }
        }
    }

    public State getState() {
        return mState;
    }

}
