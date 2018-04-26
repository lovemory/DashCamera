package com.askey.dvr.cdr7010.dashcam.core.renderer;

import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.Surface;

import com.askey.dvr.cdr7010.dashcam.core.encoder.IFrameListener;
import com.askey.dvr.cdr7010.dashcam.core.gles.EglCore;
import com.askey.dvr.cdr7010.dashcam.core.gles.OffscreenSurface;
import com.askey.dvr.cdr7010.dashcam.core.gles.WindowSurface;
import com.askey.dvr.cdr7010.dashcam.util.Logg;

public class EGLRenderer implements OnFrameAvailableListener {

    private final static String TAG = "EGLRenderer";

    private final static int MSG_INIT = 0;
    private final static int MSG_DEINIT = 1;
    private final static int MSG_DSLP_SURFACE = 2;
    private final static int MSG_ENC_SURFACE_CREATE = 3;
    private final static int MSG_ENC_SURFACE_DESTROY = 4;
    private final static int MSG_DSLP_CLEAR = 5;
    private final static int MSG_UPDATE_FRAME = 6;

    private RenderHandler mRenderHandler;
    private HandlerThread mRenderThread;
    private IFrameListener mFrameListener;

    public interface OnSurfaceTextureListener {
        void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height);
        void onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture);
    }

    public EGLRenderer() {
        mRenderThread = new HandlerThread("EGLRenderThread");
        mRenderThread.start();
        mRenderHandler = new RenderHandler(mRenderThread.getLooper());
    }

    public void start() {
        mRenderHandler.sendEmptyMessage(MSG_INIT);
    }

    public void stop() {
        mRenderHandler.sendEmptyMessage(MSG_DEINIT);
    }

    @Override
    public void finalize() {
        mRenderThread.quit();
    }

    public void setSurfaceTextureListener(OnSurfaceTextureListener listener) {
        mRenderHandler.mSurfaceTextureListener = listener;
    }

    public void setDisplaySurface(Surface surface, int width, int height) {
        mRenderHandler.setDisplaySurface(surface, width, height);
    }

    public void createEncoderSurface(Surface surface, IFrameListener listener) {
        mFrameListener = listener;
        mRenderHandler.createEncoderSurface(surface);
    }

    public void destroyEncoderSurface() {
        mRenderHandler.destroyEncoderSurface();
        mFrameListener = null;
    }

    public void clear() {
        mRenderHandler.sendEmptyMessage(MSG_DSLP_CLEAR);
    }

    @Override //OnFrameAvailableListener
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        //Logg.d(TAG, "onFrameAvailable");
        mRenderHandler.sendEmptyMessage(MSG_UPDATE_FRAME);
    }

    private class RenderHandler extends Handler {
        private EglCore mEglCore;
        private WindowSurface mDisplaySurface;
        private WindowSurface mEncoderSurface;
        private VideoTextureController mTextureController;
        private SurfaceTexture mInputSurface;
        private final float[] mTmpMatrix = new float[16];
        private int mViewportWidth, mViewportHeight;
        private final Object mDispSync = new Object();
        private final Object mEncSync = new Object();
        private OnSurfaceTextureListener mSurfaceTextureListener;

        public RenderHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case MSG_INIT:
                    init();
                    break;
                case MSG_DEINIT:
                    deinit();
                    break;
                case MSG_DSLP_CLEAR:
                    this.cleanDisplay();
                    break;
                case MSG_UPDATE_FRAME:
                    drawFrame();
                    break;
            }
        }

        private void init() {
            Logg.d(TAG, "init");
            mEglCore = new EglCore(null, EglCore.FLAG_RECORDABLE);
            OffscreenSurface dummySurface = new OffscreenSurface(mEglCore, 1, 1);
            dummySurface.makeCurrent();

            mTextureController = new VideoTextureController();
            mTextureController.prepare();
            mInputSurface = new SurfaceTexture(mTextureController.getTexture());
            mInputSurface.setDefaultBufferSize(1920, 1080);
            mInputSurface.setOnFrameAvailableListener(EGLRenderer.this);

            if (mSurfaceTextureListener != null) {
                mSurfaceTextureListener.onSurfaceTextureAvailable(mInputSurface, 1920, 1080);
            }
        }

        private void deinit() {
            Logg.d(TAG, "deinit");
            synchronized (mDispSync) {
                if (mDisplaySurface != null) {
                    mDisplaySurface.release();
                    mDisplaySurface = null;
                }
            }
            synchronized (mEncSync) {
                if (mEncoderSurface != null) {
                    mEncoderSurface.release();
                    mEncoderSurface = null;
                }
            }
            if (mInputSurface != null) {
                if (mSurfaceTextureListener != null) {
                    mSurfaceTextureListener.onSurfaceTextureDestroyed(mInputSurface);
                }
                mInputSurface.release();
                mInputSurface = null;
            }
            if (mTextureController != null) {
                mTextureController.release();
                mTextureController = null;
            }
            if (mEglCore != null) {
                mEglCore.release();
                mEglCore = null;
            }
        }

        private void setDisplaySurface(Surface surface, int width, int height) {
            synchronized (mDispSync) {
                if (surface != null) {
                    if (mDisplaySurface != null) {
                        mDisplaySurface.release();
                        mDisplaySurface = null;
                    }
                    mDisplaySurface = new WindowSurface(mEglCore, surface, false);
                    mViewportWidth = width;
                    mViewportHeight = height;
                } else if (mDisplaySurface != null) {
                    mDisplaySurface.release();
                    mDisplaySurface = null;
                }
            }
        }

        private void createEncoderSurface(Surface surface) {
            synchronized (mEncSync) {
                mEncoderSurface = new WindowSurface(mEglCore, surface, true);
            }
        }

        private void destroyEncoderSurface() {
            synchronized (mEncSync) {
                if (mEncoderSurface != null) {
                    mEncoderSurface.release();
                    mEncoderSurface = null;
                }
            }
        }

        private void cleanDisplay() {
            synchronized (mDispSync) {
                if (mDisplaySurface != null) {
                    mDisplaySurface.makeCurrent();
                    GLES20.glViewport(0, 0, mViewportWidth, mViewportHeight);
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                    mDisplaySurface.swapBuffers();
                }
            }
        }

        private void drawFrame() {
            if (mEglCore == null) {
                return;
            }

            // Latch the next frame from the camera.
            mInputSurface.updateTexImage();
            mInputSurface.getTransformMatrix(mTmpMatrix);
            mTextureController.setMatrix(mTmpMatrix);

            synchronized (mDispSync) {
                if (mDisplaySurface != null) {
                    mDisplaySurface.makeCurrent();
                    GLES20.glViewport(0, 0, mViewportWidth, mViewportHeight);
                    mTextureController.draw();
                    mDisplaySurface.swapBuffers();
                }
            }

            synchronized (mEncSync) {
                if (mEncoderSurface != null) {
                    mEncoderSurface.makeCurrent();
                    GLES20.glViewport(0, 0, 1920, 1080);
                    mTextureController.draw();
                    mEncoderSurface.setPresentationTime(mInputSurface.getTimestamp());
                    if (mFrameListener != null) {
                        mFrameListener.frameAvailableSoon();
                    }
                    mEncoderSurface.swapBuffers();
                }
            }
        }
    }

}
