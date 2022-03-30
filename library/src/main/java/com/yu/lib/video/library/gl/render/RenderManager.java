package com.yu.lib.video.library.gl.render;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import com.yu.lib.video.library.utils.VLog;
import com.yu.lib.video.library.gl.OnYuVideoListener;
import com.yu.lib.video.library.gl.filter.FilterApi;
import com.yu.lib.video.library.gl.render.play.PlayRender;
import com.yu.lib.video.library.gl.render.record.RecordRender;

public class RenderManager {
    public static final int MSG_INTI = 0;
    public static final int MSG_SURFACE_CHANGED = 1;
    public static final int MSG_VIDEO_SIZE_CHANGED = 2;
    public static final int MSG_START_RECORD = 3;
    public static final int MSG_STOP_RECORD = 4;
    public static final int MSG_STOP_SEGMENT_RECORD = 5;
    public static final int MSG_DELETE_SEGMENT_RECORD = 6;
    public static final int MSG_RELEASE = 7;

    private ActualRenderHandler mActualRenderHandler;

    public RenderManager(Context context, boolean isSupportRecord) {
        HandlerThread handlerThread = new HandlerThread("RenderThread");
        handlerThread.start();
        mActualRenderHandler = new ActualRenderHandler(context, handlerThread.getLooper(), isSupportRecord);
    }

    public void postEvent(int what, Object... objects) {
        Message message = Message.obtain();
        message.what = what;
        message.obj = objects;
        mActualRenderHandler.sendMessage(message);
    }

    public static class ActualRenderHandler extends Handler {
        private VideoRender mVideoRender;

        public ActualRenderHandler(Context context, @NonNull Looper looper, boolean isSupportRecord) {
            super(looper);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mVideoRender = new PlayRender(context);
            } else if(isSupportRecord) {
                mVideoRender = new RecordRender(context);
            } else {
                mVideoRender = new PlayRender(context);
            }
        }

        @Override
        public void handleMessage(@NonNull Message m) {
            Object[] params = (Object[]) m.obj;
            switch (m.what) {
                case MSG_INTI:
                    mVideoRender.init((YuVideoParam) params[0], (SurfaceTexture) params[1], (int) params[2], ((OnYuVideoListener) params[3]));
                    break;
                case MSG_SURFACE_CHANGED:
                    mVideoRender.onSurfaceChanged((int) params[0], (int) params[1]);
                    break;
                case MSG_VIDEO_SIZE_CHANGED:
                    mVideoRender.onVideoSizeChanged((int) params[0], (int) params[1]);
                    break;
                case MSG_START_RECORD:
                    if (mVideoRender instanceof VideoRecordRender) {
                        ((VideoRecordRender) mVideoRender).startRecord();
                    } else {
                        VLog.d("RecordRender", "PlayRender 无法进行录制 start 操作，请正确设置初始化函数 !!");
                    }
                    break;
                case MSG_STOP_RECORD:
                    if (mVideoRender instanceof VideoRecordRender) {
                        ((VideoRecordRender) mVideoRender).stopRecord();
                    } else {
                        VLog.d("RecordRender", "PlayRender 无法进行录制 stop 操作 !!");
                    }
                    break;
                case MSG_STOP_SEGMENT_RECORD:
                    if (mVideoRender instanceof VideoRecordRender) {
                        ((VideoRecordRender) mVideoRender).stopSegmentRecord();
                    } else {
                        VLog.d("RecordRender", "PlayRender 无法进行录制 stop 操作 !!");
                    }
                    break;
                case MSG_DELETE_SEGMENT_RECORD:
                    if (mVideoRender instanceof VideoRecordRender) {
                        ((VideoRecordRender) mVideoRender).deleteSegmentRecord((Integer) params[0]);
                    } else {
                        VLog.d("RecordRender", "PlayRender 无法进行录制 stop 操作 !!");
                    }
                    break;
                case MSG_RELEASE:
                    mVideoRender.release();
                    break;
            }
        }
    }

    public FilterApi getFilterApi() {
        return mActualRenderHandler.mVideoRender.getFilterApi();
    }

    public SurfaceTexture getRenderSurfaceTexture() {
        return mActualRenderHandler.mVideoRender.getRenderSurfaceTexture();
    }

}
