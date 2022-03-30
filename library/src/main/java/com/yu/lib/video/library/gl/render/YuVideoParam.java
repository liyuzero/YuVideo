package com.yu.lib.video.library.gl.render;

import android.os.Build;
import android.text.TextUtils;

import com.yu.bundles.voice.param.VoiceType;
import com.yu.bundles.voice.param.record.AudioRecordParam;
import com.yu.lib.video.library.gl.BeautyParam;

public class YuVideoParam {
    private BeautyParam mBeautyParam;
    private RecordParam mRecordParam;

    public YuVideoParam() {
        mBeautyParam = new BeautyParam();
        mRecordParam = new RecordParam(new RecordParam.VideoParam(), new RecordParam.AudioParam());
    }

    public void setBeautyParam(BeautyParam beautyParam) {
        mBeautyParam = beautyParam;
    }

    public void setRecordParam(RecordParam recordParam) {
        mRecordParam = recordParam;
    }

    public BeautyParam getBeautyParam() {
        return mBeautyParam;
    }

    public RecordParam getRecordParam() {
        return mRecordParam;
    }

    public static class RecordParam {
        private VideoParam mVideoParam;
        private AudioParam mAudioParam;
        private String mOutputFilePath;
        private boolean mIsSegmentRecord; //支持分段录制
        private String mSegmentDirPath; //分段录制的临时文件的存储目录a

        public RecordParam(VideoParam videoParam, AudioParam audioParam) {
            mVideoParam = videoParam;
            mAudioParam = audioParam;
        }

        public static class BaseParam {}

        public static class AudioParam extends BaseParam {
            // 与抖音相同的音频比特率
            public static final int BIT_RATE = 128000;

            public String mime;
            public int sampleRate; // 采样率
            public int bitRate; // 比特率
            public AudioRecordParam.AudioInChannel audioChannel;
            public VoiceType audioFormat;
            //是否是异步录制
            public boolean mIsAsync;

            public AudioParam() {
                mime = "audio/mp4a-latm";
                bitRate = BIT_RATE;
                sampleRate = 44100; // 44.1[KHz] is only setting guaranteed to be available on all devices.
                audioChannel = AudioRecordParam.AudioInChannel.CHANNEL_IN_STEREO;
                audioFormat = VoiceType.PCM_16BIT;
            }

            public int getChannelCount() {
                return audioChannel == AudioRecordParam.AudioInChannel.CHANNEL_IN_MONO? 1: 2;
            }

            public void setAsync(boolean async) {
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    mIsAsync = false;
                    return;
                }
                mIsAsync = async;
            }

            public boolean isAsync() {
                return mIsAsync;
            }
        }

        public static class VideoParam extends BaseParam {
            /**
             * 16*1000 bps：可视电话质量
             * 128-384 * 1000 bps：视频会议系统质量
             * 1.25 * 1000000 bps：VCD质量（使用MPEG1压缩）
             * 5 * 1000000 bps：DVD质量（使用MPEG2压缩）
             * 8-15 * 1000000 bps：高清晰度电视（HDTV） 质量（使用H.264压缩）
             * 29.4  * 1000000 bps：HD DVD质量
             * 40 * 1000000 bps：蓝光光碟质量（使用MPEG2、H.264或VC-1压缩）
             */
            // 与抖音相同的视频比特率
            public static final int BIT_RATE = 6693560; // 1280 * 720
            public static final int BIT_RATE_LOW = 3921332; // 576 * 1024

            public String mime;
            public int frameRate;
            public int frameIInterval;
            public int videoWidth = 720;
            public int videoHeight = 1280;
            public Resolution resolution = Resolution.P_720;
            public int bitRate;
            public int iFrameRate;
            //是否是异步录制
            public boolean mIsAsync;

            public VideoParam() {
                mime = "video/avc";
                frameRate = 30;
                frameIInterval = 1;
                bitRate = BIT_RATE;
                iFrameRate = 2;
            }

            public void setSize(int videoWidth, int videoHeight) {
                this.videoWidth = videoWidth;
                this.videoHeight = videoHeight;
                this.videoWidth = videoWidth % 2 == 0? videoWidth: videoWidth - 1;
                this.videoHeight = videoHeight % 2 == 0? videoHeight: videoHeight - 1;
                if(videoWidth * videoHeight < 1280 * 720) {
                    bitRate = BIT_RATE_LOW;
                }
            }

            public void setAsync(boolean async) {
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    mIsAsync = false;
                    return;
                }
                mIsAsync = async;
            }

            public boolean isAsync() {
                return mIsAsync;
            }
        }

        public void setVideoParam(VideoParam videoParam) {
            mVideoParam = videoParam;
        }

        public void setAudioParam(AudioParam audioParam) {
            mAudioParam = audioParam;
        }

        public void setOutputFilePath(String outputFilePath) {
            mOutputFilePath = outputFilePath;
        }

        public void setSegmentRecord(boolean segmentRecord, String segmentDirPath) {
            mIsSegmentRecord = segmentRecord;
            if(mIsSegmentRecord) {
                if(TextUtils.isEmpty(segmentDirPath)) {
                    throw new RuntimeException("分段录制，必须设置片段视频临时存储位置");
                }
                this.mSegmentDirPath = segmentDirPath;
            }
        }

        public VideoParam getVideoParam() {
            return mVideoParam;
        }

        public AudioParam getAudioParam() {
            return mAudioParam;
        }

        public String getOutputFilePath() {
            return mOutputFilePath;
        }

        public String getSegmentDirPath() {
            return mSegmentDirPath;
        }

        public boolean isSegmentRecord() {
            return mIsSegmentRecord;
        }
    }

    public enum Resolution {
        P_480, P_720, P_1080
    }

}
