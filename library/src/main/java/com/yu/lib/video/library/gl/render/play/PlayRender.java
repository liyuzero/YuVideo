package com.yu.lib.video.library.gl.render.play;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.Looper;

import com.yu.lib.video.library.utils.VLog;
import com.yu.lib.video.library.gl.OnYuVideoListener;
import com.yu.lib.video.library.gl.filter.FilterApi;
import com.yu.lib.video.library.gl.filter.FilterChain;
import com.yu.lib.video.library.gl.gles.EglManager;
import com.yu.lib.video.library.gl.gles.OpenGLUtils;
import com.yu.lib.video.library.gl.render.YuVideoParam;
import com.yu.lib.video.library.gl.render.VideoRender;

import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;

public class PlayRender implements VideoRender {
    public static final String TAG = "PlayRender";

    private EglManager mEglManager;
    private float[] mSTMatrix = new float[16];
    private int mRenderTextureID = OpenGLUtils.HAS_NO_TEXTURE;
    //由OpenGL纹理创建的st，实际是通过它绑定播放器或Camera
    private volatile SurfaceTexture mRenderSurfaceTexture;

    //滤镜
    private FilterChain mFilterChain;
    private OnYuVideoListener mOnYuVideoListener;
    private boolean mIsInitSuccess;
    private boolean mIsFirstFrame;

    private Handler mMainHandler;

    public PlayRender(Context context) {
        mMainHandler = new Handler(Looper.getMainLooper());
        mFilterChain = new FilterChain(context);
    }

    @Override
    public void init(YuVideoParam yuVideoParam, SurfaceTexture srcTexture, int scaleType, OnYuVideoListener listener) {
        mOnYuVideoListener = listener;
        if (mRenderSurfaceTexture != null) {
            mRenderSurfaceTexture.release();
            mRenderSurfaceTexture = null;
        }

        mEglManager = new EglManager();
        mIsInitSuccess = mEglManager.createEglEnv(srcTexture);
        if (mIsInitSuccess) {
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
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    mOnYuVideoListener.onRenderTextureInitComplete(mIsInitSuccess);
                }
            });
        } else {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    mOnYuVideoListener.onRenderTextureInitComplete(false);
                }
            });
            VLog.d(TAG, "初始化失败");
        }

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

    @Override
    public void release() {
        if(mIsInitSuccess) {
            mIsFirstFrame = false;
            mFilterChain.release();
            if (mRenderSurfaceTexture != null) {
                mRenderSurfaceTexture.setOnFrameAvailableListener(null);
                mRenderSurfaceTexture.release();
                mRenderSurfaceTexture = null;
            }
            mEglManager.destroy();
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
        if (mRenderSurfaceTexture == null) {
            return;
        }
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

        mFilterChain.process(mRenderTextureID, mSTMatrix);
        mEglManager.swapBuffer();

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
