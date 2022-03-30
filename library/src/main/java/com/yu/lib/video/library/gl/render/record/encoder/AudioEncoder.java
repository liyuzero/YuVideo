package com.yu.lib.video.library.gl.render.record.encoder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.yu.lib.video.library.utils.VLog;
import com.yu.lib.video.library.gl.render.YuVideoParam;
import com.yu.lib.video.library.gl.render.record.RecordRender;
import com.yu.lib.video.library.gl.render.record.YuMuxer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class AudioEncoder {
    private static final int BUFFER_SIZE = 1024 * 16;

    private AEncoder mEncoder;
    private boolean mIsInitSuccess = true;

    public AudioEncoder(YuMuxer mediaMuxer, YuVideoParam.RecordParam.AudioParam param) {
        MediaFormat format = genMediaFormat(param);
        //编码器
        MediaCodec mediaCodec;
        try {
            mediaCodec = MediaCodec.createEncoderByType(param.mime);
        } catch (IOException e) {
            mIsInitSuccess = false;
            return;
        }

        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mediaCodec.start();

        mEncoder = (param.isAsync() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)? new AsyncEncoder(mediaMuxer, mediaCodec):
                new SyncEncoder(mediaMuxer, mediaCodec);
        mEncoder.setRecordHandler(new Handler());
    }

    private MediaFormat genMediaFormat(YuVideoParam.RecordParam.AudioParam param) {
        MediaFormat mediaFormat = MediaFormat.createAudioFormat("audio/mp4a-latm", param.sampleRate, param.getChannelCount());
        mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, param.bitRate);
        mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, BUFFER_SIZE);
        return mediaFormat;
    }

    public boolean isInitSuccess() {
        return mIsInitSuccess;
    }

    public boolean isAddTrackSuccess() {
        return mEncoder.isAddTrackSuccess();
    }

    //编码
    public void encodeData(byte[] audioData, int len) {
        if(!mIsInitSuccess) {
            return;
        }
        mEncoder.encodePCM(audioData, len);
    }

    public void stopEncode() {
        mEncoder.stopEncode();
    }

    public boolean hasWriteData() {
        return mEncoder.mHasWriteData;
    }

    private static abstract class AEncoder {
        protected Handler mRecordHandler;

        public void setRecordHandler(Handler recordHandler) {
            mRecordHandler = recordHandler;
        }

        protected long mPrevOutputPTSUs;

        public abstract void encodePCM(byte[] data, int len);

        protected long getPTSUs() {
            long result = System.nanoTime() / 1000L;
            // presentationTimeUs should be monotonic
            // otherwise muxer fail to write
            if (result < mPrevOutputPTSUs)
                result = (mPrevOutputPTSUs - result) + result;
            return result;
        }

        public abstract boolean isAddTrackSuccess();

        protected boolean mStopEncode;

        public void stopEncode() {
            mStopEncode = true;
        }

        protected boolean mHasWriteData;
    }

    private static class SyncEncoder extends AEncoder {
        private MediaCodec.BufferInfo mBufferInfo;
        private ByteBuffer[] mInputBuffers;
        private ByteBuffer[] mOutputBuffers;

        private volatile Integer mAudioTrackId;

        private YuMuxer mMediaMuxer;
        private MediaCodec mMediaCodec;

        public SyncEncoder(YuMuxer mediaMuxer, MediaCodec mediaCodec) {
            mMediaMuxer = mediaMuxer;
            mMediaCodec = mediaCodec;

            mInputBuffers = mediaCodec.getInputBuffers();
            mOutputBuffers = mediaCodec.getOutputBuffers();
            mBufferInfo = new MediaCodec.BufferInfo();
        }

        @Override
        public void encodePCM(byte[] data, int len) {
            int inputIndex = mMediaCodec.dequeueInputBuffer(4000);
            if (inputIndex >= 0) {
                ByteBuffer buffer = mInputBuffers[inputIndex];
                buffer.clear();

                if(len > 0) {
                    buffer.put(data, 0, len);
                    mMediaCodec.queueInputBuffer(inputIndex, 0, len, getPTSUs(), 0);
                } else {
                    VLog.d(RecordRender.TAG, "pcm data not available");
                }
            }

            int outputIndex = 0;
            while (outputIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
                outputIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 0);
                if (outputIndex >= 0) {
                    ByteBuffer encodedData = mOutputBuffers[outputIndex];
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 || mBufferInfo.size == 0) {
                        mBufferInfo.presentationTimeUs = getPTSUs();
                        mMediaMuxer.writeSampleData(mAudioTrackId, mOutputBuffers[outputIndex], mBufferInfo);
                        mPrevOutputPTSUs = mBufferInfo.presentationTimeUs;
                        mHasWriteData = true;
                    }
                    mMediaCodec.releaseOutputBuffer(outputIndex, false);
                } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    //生成id需要写在此处，muxer重复启动
                    mAudioTrackId = mMediaMuxer.addTrack(mMediaCodec.getOutputFormat());
                    mMediaMuxer.start();
                }
            }
        }

        @Override
        public boolean isAddTrackSuccess() {
            return mAudioTrackId != null;
        }

        @Override
        public void stopEncode() {
            super.stopEncode();

            VLog.d("hehe", "音频编码：" + (!mHasWriteData) + " -- " + (mAudioTrackId != null));
            if(mAudioTrackId != null && !mHasWriteData) {
                mMediaMuxer.writeSampleData(mAudioTrackId, mOutputBuffers[0], mBufferInfo);
            }

            mMediaCodec.stop();
            mMediaCodec.release();
        }
    }

    private static class AsyncEncoder extends AEncoder {
        private YuMuxer mMediaMuxer;
        private MediaCodec mMediaCodec;

        private Integer mAudioTrackId;

        private ArrayList<byte[]> mInputAudioData;
        private ArrayList<Integer> mInputAudioDataLen;

        public AsyncEncoder(final YuMuxer mediaMuxer, MediaCodec mediaCodec) {
            mInputAudioData = new ArrayList<>();
            mInputAudioDataLen = new ArrayList<>();
            mMediaMuxer = mediaMuxer;
            mMediaCodec = mediaCodec;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mMediaCodec.setCallback(new MediaCodec.Callback() {
                    @Override
                    public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                        ByteBuffer buffer = codec.getInputBuffer(index);
                        if(buffer == null) {
                            return;
                        }
                        buffer.clear();

                        if(mInputAudioData.size() > 0) {
                            for (int i=0; i<mInputAudioData.size(); ) {
                                byte[] data = mInputAudioData.get(i);
                                int len = mInputAudioDataLen.get(i);
                                buffer.put(data, 0, len);
                                mMediaCodec.queueInputBuffer(index, 0, len, getPTSUs(), 0);

                                mInputAudioData.remove(0);
                                mInputAudioDataLen.remove(0);
                            }
                        }
                    }

                    @Override
                    public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                        ByteBuffer encodedData = codec.getOutputBuffer(index);
                        if(encodedData == null) {
                            return;
                        }
                        mHasWriteData = true;
                        encodedData.position(info.offset);
                        encodedData.limit(info.offset + info.size);
                        if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0 && info.size != 0) {
                            mMediaCodec.releaseOutputBuffer(index, false);
                        } else {
                            mMediaMuxer.writeSampleData(mAudioTrackId, encodedData, info);
                            mMediaCodec.releaseOutputBuffer(index, false);
                        }
                    }

                    @Override
                    public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {

                    }

                    @Override
                    public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                        mAudioTrackId = mMediaMuxer.addTrack(mMediaCodec.getOutputFormat());
                        mediaMuxer.start();
                    }
                });
            }
        }

        @Override
        public void encodePCM(byte[] data, int len) {
            mInputAudioData.add(data);
            mInputAudioDataLen.add(len);
        }

        @Override
        public boolean isAddTrackSuccess() {
            return mAudioTrackId != null;
        }

        @Override
        public void stopEncode() {
            super.stopEncode();
            mMediaCodec.stop();
            mMediaCodec.release();
            mInputAudioData.clear();
            mInputAudioDataLen.clear();
        }
    }

}
