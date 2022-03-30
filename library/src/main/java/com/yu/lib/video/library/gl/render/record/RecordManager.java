package com.yu.lib.video.library.gl.render.record;

import android.opengl.EGLContext;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.yu.lib.video.library.gl.OnYuVideoListener;
import com.yu.lib.video.library.gl.render.YuVideoParam;
import com.yu.lib.video.library.gl.render.record.encoder.RecordEncoder;
import com.yu.lib.video.library.gl.render.record.utils.MergeFileUtils;
import com.yu.lib.video.library.gl.render.record.utils.RecordFileUtils;

import java.util.HashMap;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class RecordManager {
    static final int MSG_INIT = 0;
    static final int MSG_START_RECORD = 1;
    static final int MSG_STOP_RECORD = 2;
    static final int MSG_STOP_SEGMENT_RECORD = 3;
    static final int MSG_ON_RECEIVE_FRAME = 4;
    static final int MSG_DELETE_SEGMENT_RECORD = 5;
    static final int MSG_RELEASE = 6;

    private final RecordHandler mRecordHandler;

    public RecordManager() {
        HandlerThread thread = new HandlerThread("RecordThread");
        thread.start();
        mRecordHandler = new RecordHandler(thread.getLooper());
    }

    public void postEvent(int what, Object... objects) {
        Message message = Message.obtain();
        message.what = what;
        message.obj = objects;
        mRecordHandler.sendMessage(message);
    }

    private static class RecordHandler extends Handler {
        private HandlerThread mVideoEncodeThread;
        private HandlerThread mAudioEncodeThread;

        public RecordHandler(@NonNull Looper looper) {
            super(looper);
            mVideoEncodeThread = new HandlerThread("EncodeVideoThread");
            mAudioEncodeThread = new HandlerThread("AudioEncodeThread");
            mVideoEncodeThread.start();
            mAudioEncodeThread.start();
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            Object[] params = (Object[]) msg.obj;
            switch (msg.what) {
                case MSG_INIT:
                    init((EGLContext) params[0], ((OnYuVideoListener) params[1]));
                    break;
                case MSG_START_RECORD:
                    startRecord((YuVideoParam.RecordParam) params[0]);
                    break;
                case MSG_STOP_RECORD:
                    stopRecord(false);
                    break;
                case MSG_STOP_SEGMENT_RECORD:
                    stopRecord(true);
                    break;
                case MSG_ON_RECEIVE_FRAME:
                    mRecordDataSource.onReceiveFrame((Integer) params[0], (Long) params[1]);
                    break;
                case MSG_DELETE_SEGMENT_RECORD:
                    deleteSegmentRecord((Integer) params[0]);
                    break;
                case MSG_RELEASE:
                    release();
                    break;
            }
        }

        private RecordDataSource mRecordDataSource;
        private RecordEncoder mRecordEncoder;
        private YuVideoParam.RecordParam mRecordParam;
        private MergeFileUtils mMergeFileUtils;
        private OnYuVideoListener mOnYuVideoListener;

        private int mCurSegmentIndex = -1;
        private long mSegmentTotalDuration;
        private boolean mIsMergeSegment;

        public void init(EGLContext eglContext, OnYuVideoListener listener) {
            mOnYuVideoListener = listener;
            mRecordDataSource = new RecordDataSource(eglContext, mSourceDataListener);
        }

        private final RecordDataSource.OnRecordSourceDataListener mSourceDataListener = new RecordDataSource.OnRecordSourceDataListener() {

            @Override
            public void onReceiveVideoData() {
                if(mRecordEncoder != null) {
                    mRecordEncoder.encodeVideoFrame();
                }
            }

            @Override
            public void onReceiveAudioData(byte[] audioData, int len) {
                if(mRecordEncoder != null) {
                    mRecordEncoder.encodeAudioData(audioData, len);
                }
            }

            @Override
            public void onRecordStop() {
                if(mRecordEncoder != null) {
                    mRecordEncoder.stopEncode();
                }
            }

            @Override
            public void onRecordFinish(final long duration, boolean isSegmentFinish) {
                if(mRecordParam.isSegmentRecord()) {
                    mSegmentTotalDuration += duration;
                    if(isSegmentFinish) {
                        //单个片段结束
                        mOnYuVideoListener.onRecordSegmentFinish(mCurSegmentIndex, duration, mSegmentTotalDuration);
                    } else {
                        final long totalDuration = mSegmentTotalDuration;
                        //停止录制，开始合并文件
                        mIsMergeSegment = true;
                        mMergeFileUtils = new MergeFileUtils(mRecordParam);
                        mMergeFileUtils.mergeSegmentFiles(RecordFileUtils.getTempSegmentFiles(mRecordParam.getSegmentDirPath(),
                                mCurSegmentIndex), mRecordParam.getOutputFilePath(), new MergeFileUtils.OnMergeFileListener() {
                            @Override
                            public void onMergeSuccess() {
                                mIsMergeSegment = false;
                                mOnYuVideoListener.onRecordFinish(mRecordParam.getOutputFilePath(), totalDuration);
                            }

                            @Override
                            public void onMergeFail() {
                                mIsMergeSegment = false;
                                mOnYuVideoListener.onRecordFail();
                            }
                        });
                        mSegmentTotalDuration = 0;
                        mCurSegmentIndex = 0;
                    }
                } else {
                    mCurSegmentIndex = 0;
                    mOnYuVideoListener.onRecordFinish(mRecordParam.getOutputFilePath(), duration);
                }
            }

            @Override
            public void onRecordAbort() {
                //删除临时文件
                if(mRecordParam.isSegmentRecord()) {
                    RecordFileUtils.deleteAllTempSegmentVideoFile(mRecordParam.getSegmentDirPath(), mCurSegmentIndex);
                } else {
                    RecordFileUtils.deleteRecordFile(mRecordParam.getOutputFilePath());
                }
            }

            @Override
            public void onRecordFail() {
                mOnYuVideoListener.onRecordFail();
            }
        };

        public void deleteSegmentRecord(int segmentIndex) {
        }

        public void startRecord(YuVideoParam.RecordParam param) {
            if(mIsMergeSegment) {
               return;
            }
            mRecordParam = param;
            mRecordEncoder = new RecordEncoder(param, ++mCurSegmentIndex, mVideoEncodeThread, mAudioEncodeThread
                    , mOnYuVideoListener);
            mRecordDataSource.startRecord(mRecordEncoder.getVideoInputSurface(), param);
        }

        public void stopRecord(boolean isSegmentFinish) {
            if(mIsMergeSegment) {
                return;
            }
            mRecordDataSource.stopRecord(isSegmentFinish);
        }

        public void release() {
            if(mIsMergeSegment) {
                mMergeFileUtils.release();
            }
            mRecordDataSource.release();
        }
    }

}
