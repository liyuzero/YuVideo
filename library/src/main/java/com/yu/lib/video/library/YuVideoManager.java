package com.yu.lib.video.library;

import android.content.Context;
import android.graphics.SurfaceTexture;

import com.yu.lib.video.library.gl.OnYuVideoListener;
import com.yu.lib.video.library.gl.filter.FilterApi;
import com.yu.lib.video.library.gl.render.RenderManager;
import com.yu.lib.video.library.gl.render.YuVideoParam;
import com.yu.lib.video.library.gl.render.VideoRecordRender;

import java.util.concurrent.CountDownLatch;

public class YuVideoManager implements VideoRecordRender {
    public static final int SCALE_TYPE_CENTER_CROP = 0;
    public static final int SCALE_TYPE_FIT_CENTER = 1;
    public static final int SCALE_TYPE_AUTO = 2;

    private RenderManager mRenderManager;

    //用以阻塞 getRenderSurfaceTexture 方法，保证只有surfaceTexture初始化完毕才能返回
    private CountDownLatch mCountDownLatch;

    public YuVideoManager(Context context, boolean isSupportRecord) {
        mRenderManager = new RenderManager(context, isSupportRecord);
    }

    @Override
    public void init(final YuVideoParam yuVideoParam, final SurfaceTexture srcTexture,
                     final int scaleType, final OnYuVideoListener onYuVideoListener) {
        mCountDownLatch = new CountDownLatch(1);
        mRenderManager.postEvent(RenderManager.MSG_INTI, yuVideoParam, srcTexture, scaleType, new OnYuVideoListener() {
            @Override
            public void onRenderTextureInitComplete(boolean isInitSuccess) {
                mCountDownLatch.countDown();
                if(onYuVideoListener != null) {
                    onYuVideoListener.onRenderTextureInitComplete(isInitSuccess);
                }
            }

            @Override
            public void onRenderStart() {
                if(onYuVideoListener != null) {
                    onYuVideoListener.onRenderStart();
                }
            }

            @Override
            public void onRecordSegmentFinish(int segmentIndex, long segmentDuration, long curTotalDuration) {
                if(onYuVideoListener != null) {
                    onYuVideoListener.onRecordSegmentFinish(segmentIndex, segmentDuration, curTotalDuration);
                }
            }

            @Override
            public void onRecordFinish(String outputFilePath, long duration) {
                if(onYuVideoListener != null) {
                    onYuVideoListener.onRecordFinish(outputFilePath, duration);
                }
            }

            @Override
            public void onRecordFail() {
                if(onYuVideoListener != null) {
                    onYuVideoListener.onRecordFail();
                }
            }

            @Override
            public void onRecordDeleteSegment(boolean isSuccess, int index, int count) {

            }

            @Override
            public void onRecordCallBack(int segmentIndex, long curDuration) {
                if(onYuVideoListener != null) {
                    onYuVideoListener.onRecordCallBack(segmentIndex, curDuration);
                }
            }
        });
    }

    @Override
    public void onSurfaceChanged(final int width, final int height) {
        mRenderManager.postEvent(RenderManager.MSG_SURFACE_CHANGED, width, height);
    }

    @Override
    public void onVideoSizeChanged(final int videoWidth, final int videoHeight) {
        mRenderManager.postEvent(RenderManager.MSG_VIDEO_SIZE_CHANGED, videoWidth, videoHeight);
    }

    @Override
    public void startRecord() {
        mRenderManager.postEvent(RenderManager.MSG_START_RECORD);
    }

    @Override
    public void stopSegmentRecord() {
        mRenderManager.postEvent(RenderManager.MSG_STOP_SEGMENT_RECORD);
    }

    @Override
    public void stopRecord() {
        mRenderManager.postEvent(RenderManager.MSG_STOP_RECORD);
    }

    @Override
    public void deleteSegmentRecord(int index) {
        mRenderManager.postEvent(RenderManager.MSG_DELETE_SEGMENT_RECORD, index);
    }

    @Override
    public void release() {
        mRenderManager.postEvent(RenderManager.MSG_RELEASE);
    }

    @Override
    public FilterApi getFilterApi() {
        return mRenderManager.getFilterApi();
    }

    @Override
    public SurfaceTexture getRenderSurfaceTexture() {
        try {
            mCountDownLatch.await();
        } catch (InterruptedException e) {
            //
        }
        return mRenderManager.getRenderSurfaceTexture();
    }
}
