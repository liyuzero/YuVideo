package com.yu.lib.video.library.gl.render.record;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.RequiresApi;

import com.yu.lib.video.library.utils.VLog;
import com.yu.lib.video.library.gl.OnYuVideoListener;
import com.yu.lib.video.library.gl.filter.FilterApi;
import com.yu.lib.video.library.gl.filter.FilterChain;
import com.yu.lib.video.library.gl.gles.EglCore;
import com.yu.lib.video.library.gl.gles.OpenGLUtils;
import com.yu.lib.video.library.gl.gles.WindowSurface;
import com.yu.lib.video.library.gl.render.YuVideoParam;
import com.yu.lib.video.library.gl.render.VideoRecordRender;

import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class RecordRender implements VideoRecordRender {
    public static final String TAG = "RecordRender";

    private EglCore mEglCore;
    private FilterChain mFilterChain;
    private WindowSurface mPlaySurface;
    private int mRenderTextureID = OpenGLUtils.HAS_NO_TEXTURE;
    //由OpenGL纹理创建的st，实际是通过它绑定播放器或Camera
    private volatile SurfaceTexture mRenderSurfaceTexture;
    private Handler mMainHandler = new Handler(Looper.getMainLooper());

    private YuVideoParam mYuVideoParam;
    //保证内存可见性
    private volatile boolean mIsInitSuccess;
    private boolean mIsFirstFrame;
    private float[] mSTMatrix = new float[16];

    private RecordManager mRecordManager;
    private OnYuVideoListener mOnYuVideoListener;

    public RecordRender(Context context) {
        mFilterChain = new FilterChain(context);
    }

    @Override
    public void init(YuVideoParam yuVideoParam, SurfaceTexture srcTexture, int scaleType, OnYuVideoListener listener) {
        mYuVideoParam = yuVideoParam;
        mOnYuVideoListener = listener;
        mEglCore = new EglCore(null, EglCore.FLAG_RECORDABLE | EglCore.FLAG_TRY_GLES3);
        mPlaySurface = new WindowSurface(mEglCore, srcTexture);
        mPlaySurface.makeCurrent();

        if (mRenderSurfaceTexture != null) {
            mRenderSurfaceTexture.release();
            mRenderSurfaceTexture = null;
        }

        if (mRenderTextureID != OpenGLUtils.HAS_NO_TEXTURE) {
            OpenGLUtils.deleteTexture(mRenderTextureID);
        }
        mRenderTextureID = OpenGLUtils.createOESTexture();
        mRenderSurfaceTexture = new SurfaceTexture(mRenderTextureID);
        mRenderSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                drawFrame();
            }
        });

        //初始化滤镜
        mFilterChain.init(scaleType);
        //设置初始化美颜参数
        mFilterChain.setBeautyParam(yuVideoParam.getBeautyParam());
        mIsInitSuccess = mFilterChain.isInitSuccess() && mRenderTextureID != OpenGLUtils.HAS_NO_TEXTURE;

        //初始化录制............................. start
        if(mRecordManager != null) {
            mRecordManager.postEvent(RecordManager.MSG_RELEASE);
        }
        if(mRecordManager == null) {
            mRecordManager = new RecordManager();
        }
        mRecordManager.postEvent(RecordManager.MSG_INIT, mEglCore.getEGLContext(), listener);

        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                mOnYuVideoListener.onRenderTextureInitComplete(mIsInitSuccess);
            }
        });
        mIsFirstFrame = true;
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        mFilterChain.onSurfaceChanged(width, height);
    }

    @Override
    public void onVideoSizeChanged(int videoWidth, int videoHeight) {
        mFilterChain.onTextureSizeChanged(videoWidth, videoHeight);
    }

    /** ------------------------------------ 录制 start --------------------------------------- **/

    @Override
    public void startRecord() {
        //理论上只要渲染出界面了，宽、高就必然会有值
        if(mFilterChain.getTextureWidth() == 0 || mFilterChain.getTextureHeight() == 0 ||
                mYuVideoParam == null || mYuVideoParam.getRecordParam() == null) {
            VLog.d(TAG, "record not prepared!!!");
            return;
        }

        int[] size = getBestSize(mFilterChain.getTextureWidth(), mFilterChain.getTextureHeight(), mYuVideoParam.getRecordParam().getVideoParam().resolution);
        mYuVideoParam.getRecordParam().getVideoParam().setSize(size[0], size[1]);
        mRecordManager.postEvent(RecordManager.MSG_START_RECORD, mYuVideoParam.getRecordParam());
    }

    private int[] getBestSize(int textureW, int textureH, YuVideoParam.Resolution resolution) {
        int[] size = new int[]{textureW, textureH};
        int actualW = 720;
        switch (resolution) {
            case P_480:
                actualW = 480;
                break;
            case P_720:
                break;
            case P_1080:
                actualW = 1080;
                break;
        }
        actualW = Math.min(textureW, actualW);
        size[0] = actualW;
        size[1] = (int) (textureH / (float) textureW * actualW);
        return size;
    }

    @Override
    public void stopRecord() {
        mRecordManager.postEvent(RecordManager.MSG_STOP_RECORD);
    }

    @Override
    public void deleteSegmentRecord(int index) {
        mRecordManager.postEvent(RecordManager.MSG_DELETE_SEGMENT_RECORD, index);
    }

    @Override
    public void stopSegmentRecord() {
        mRecordManager.postEvent(RecordManager.MSG_STOP_SEGMENT_RECORD);
    }

    /** ------------------------------------ 录制 end --------------------------------------- **/

    @Override
    public void release() {
        if(mIsInitSuccess) {
            if(mRecordManager != null) {
                mRecordManager.postEvent(RecordManager.MSG_RELEASE);
            }

            mFilterChain.release();
            mPlaySurface.release();
            if (mRenderSurfaceTexture != null) {
                mRenderSurfaceTexture.setOnFrameAvailableListener(null);
                mRenderSurfaceTexture.release();
                mRenderSurfaceTexture = null;
            }
            mEglCore.release();
            mIsFirstFrame = false;
        }
        mIsInitSuccess = false;
    }

    @Override
    public FilterApi getFilterApi() {
        return mFilterChain;
    }

    @Override
    public SurfaceTexture getRenderSurfaceTexture() {
        return mRenderSurfaceTexture;
    }

    private void drawFrame() {
        if(!mIsInitSuccess) {
            return;
        }
        mPlaySurface.makeCurrent();
        // 更新输入纹理
        /*
         *  必须显示的调用updateTexImage()将数据更新到GL_OES_EGL_image_external类型的OpenGL ES
         *  纹理对象中后，SurfaceTexture才有空间来获取下一帧的数据。否则下一帧数据永远不会交给SurfaceTexture。
         * */
        mRenderSurfaceTexture.updateTexImage();
        /*
         *  getTransformMatrix()得到的矩阵，将传统的形如(s,t,0,1的)OpenGL ES
         *  二维纹理坐标列向量转换为纹理流中正确的采样位置。
         * */
        mRenderSurfaceTexture.getTransformMatrix(mSTMatrix);

        //OpenGL绘制
        glClearColor(0.0f, 0f, 0f, 1f);
        glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        int curTexture = mFilterChain.process(mRenderTextureID, mSTMatrix);
        mPlaySurface.swapBuffers();

        //录制逻辑实现............................ start
        if(mRecordManager != null) {
            mRecordManager.postEvent(RecordManager.MSG_ON_RECEIVE_FRAME, curTexture, mRenderSurfaceTexture.getTimestamp());
        }

        //首次渲染
        if(mIsFirstFrame) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(mOnYuVideoListener != null) {
                        mOnYuVideoListener.onRenderStart();
                    }
                }
            });
            mIsFirstFrame = false;
        }
    }
}
