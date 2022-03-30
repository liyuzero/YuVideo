package com.yu.lib.video.library.gl.render.record;

import android.opengl.EGLContext;
import android.os.Build;
import android.view.Surface;

import androidx.annotation.RequiresApi;

import com.yu.bundles.voice.manager.RecordManager;
import com.yu.bundles.voice.param.record.AudioRecordParam;
import com.yu.bundles.voice.record.RecordListener;
import com.yu.lib.video.library.gl.filter.base.DisplayFilter;
import com.yu.lib.video.library.gl.filter.base.ShaderData;
import com.yu.lib.video.library.gl.gles.EglCore;
import com.yu.lib.video.library.gl.gles.OpenGLUtils;
import com.yu.lib.video.library.gl.gles.WindowSurface;
import com.yu.lib.video.library.gl.render.YuVideoParam;

import java.nio.FloatBuffer;

//管理录制的所需的源 音频数据 和 视频数据，让结构更清晰
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class RecordDataSource {
    private VideoSource mVideoSource;
    private AudioSource mAudioSource;
    private boolean isRecording;
    private OnRecordSourceDataListener mOnRecordSourceDataListener;
    private long mStartTime;

    public RecordDataSource(EGLContext context, OnRecordSourceDataListener listener) {
        mVideoSource = new VideoSource(context, listener);
        mAudioSource = new AudioSource(listener);
        mOnRecordSourceDataListener = listener;
    }

    //当视频源：摄像头或者视频 有帧变化时会自动触发该方法
    public void onReceiveFrame(int textureId, long timeStamp) {
        if(isRecording) {
            mVideoSource.onReceiveFrame(textureId, timeStamp);
        }
    }

    public void startRecord(Surface surface, YuVideoParam.RecordParam param) {
        if(surface == null) {
            return;
        }
        if(isRecording) {
            return;
        }
        mStartTime = System.currentTimeMillis();
        isRecording = true;
        mVideoSource.startRecord(surface, param.getVideoParam());
        mAudioSource.startRecord(param.getAudioParam());
    }

    public void stopRecord(boolean isSegmentFinish) {
        if(isRecording) {
            isRecording = false;
            mVideoSource.stopRecord();
            mAudioSource.stopRecord();
            if(mOnRecordSourceDataListener != null) {
                mOnRecordSourceDataListener.onRecordStop();
                mOnRecordSourceDataListener.onRecordFinish(System.currentTimeMillis() - mStartTime, isSegmentFinish);
            }
        } else {
            if(mOnRecordSourceDataListener != null) {
                mOnRecordSourceDataListener.onRecordFail();
            }
        }
    }

    public void release() {
        boolean isTempRecording = isRecording;

        isRecording = false;
        mVideoSource.stopRecord();
        mAudioSource.stopRecord();

        if(mOnRecordSourceDataListener != null && isTempRecording) {
            mOnRecordSourceDataListener.onRecordAbort();
        }
    }

    public interface OnRecordSourceDataListener {
        void onReceiveVideoData();
        void onReceiveAudioData(byte[] audioData, int len);
        //和finish区分开，表示输出源停止
        void onRecordStop();
        void onRecordFinish(long duration, boolean isSegmentFinish);
        void onRecordAbort();
        void onRecordFail();
    }

    private static class AudioSource {
        private RecordManager mRecordManager;
        private OnRecordSourceDataListener mOnRecordSourceDataListener;
        private boolean mIsRecording;

        public AudioSource(OnRecordSourceDataListener onRecordSourceDataListener) {
            mOnRecordSourceDataListener = onRecordSourceDataListener;
        }

        public void startRecord(YuVideoParam.RecordParam.AudioParam param) {
            AudioRecordParam audioRecordParam = new AudioRecordParam(param.sampleRate, param.audioChannel);
            audioRecordParam.setIsOutputFile(false);
            audioRecordParam.setCallbackInMainThread(false);
            mRecordManager = new RecordManager(param.audioFormat, audioRecordParam);
            mRecordManager.startRecord(null, new RecordListener(){

                @Override
                public void onStart() {

                }

                @Override
                public void onRecordBytes(final byte[] audioData, final int len, int audioSeq) {
                    if(!mIsRecording) {
                        return;
                    }
                    mOnRecordSourceDataListener.onReceiveAudioData(audioData, len);
                }

                @Override
                public void onFinishRecord(long duration, String filePath) {

                }

                @Override
                public void onCancel() {

                }

                @Override
                public void onError(Exception e) {
                    mOnRecordSourceDataListener.onRecordFail();
                }

                @Override
                public void onAmplitudeChanged(int volume) {

                }
            });
            mIsRecording = true;
        }

        public void stopRecord() {
            mIsRecording = false;
            if(mRecordManager != null) {
                mRecordManager.stopRecord();
                mRecordManager.release();
                mRecordManager = null;
            }
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private static class VideoSource {
        //世界坐标数据
        public FloatBuffer mVertexData = OpenGLUtils.getFloatBuffer(ShaderData.VERTEX_DATA);
        //纹理坐标数据
        public FloatBuffer mTextureCoordinateData = OpenGLUtils.getFloatBuffer(ShaderData.TEXTURE_VERTEX_DATA);

        private DisplayFilter mEncoderFilter;
        private WindowSurface mInputSurface;
        private EglCore mEglCore;
        private EGLContext mShareContext;

        private boolean mIsRecording;
        private OnRecordSourceDataListener mOnRecordSourceDataListener;

        public VideoSource(EGLContext eglContext, OnRecordSourceDataListener onRecordSourceDataListener) {
            mShareContext = eglContext;
            mOnRecordSourceDataListener = onRecordSourceDataListener;
        }

        /**
        * @param surface 渲染视频数据到该surface
        * */
        public void startRecord(Surface surface, YuVideoParam.RecordParam.VideoParam param) {
            if(mIsRecording) {
                return;
            }
            mInputSurface = new WindowSurface(mEglCore = new EglCore(mShareContext, EglCore.FLAG_RECORDABLE), surface, true);
            mInputSurface.makeCurrent();
            mEncoderFilter = new DisplayFilter();
            mEncoderFilter.init();
            mEncoderFilter.onTextureSizeChanged(param.videoWidth, param.videoHeight);
            mEncoderFilter.onSurfaceChanged(param.videoWidth, param.videoHeight);
            mIsRecording = true;
        }

        public void onReceiveFrame(int textureId, long timeStamp) {
            if(mIsRecording) {
                //绘制数据到 MediaCodec 的输入surface上
                mInputSurface.makeCurrent();
                //OpenGL绘制
                mEncoderFilter.drawFrame(textureId, mVertexData, mTextureCoordinateData);
                mInputSurface.setPresentationTime(timeStamp);
                mInputSurface.swapBuffers();
                mOnRecordSourceDataListener.onReceiveVideoData();
            }
        }

        public void stopRecord() {
            if(!mIsRecording) {
                return;
            }
            mIsRecording = false;

            //停止渲染数据到接收的surface
            if (mEncoderFilter != null) {
                mEncoderFilter.release();
                mEncoderFilter = null;
            }
            if (mInputSurface != null) {
                mInputSurface.release();
                mInputSurface = null;
            }
            if (mEglCore != null) {
                mEglCore.release();
                mEglCore = null;
            }
        }

    }

}
