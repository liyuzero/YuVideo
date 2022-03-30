package com.yu.lib.video.library.gl.render.record.encoder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.text.TextUtils;
import android.view.Surface;

import androidx.annotation.RequiresApi;

import com.yu.lib.video.library.utils.VLog;
import com.yu.lib.video.library.gl.render.YuVideoParam;
import com.yu.lib.video.library.gl.render.record.RecordRender;
import com.yu.lib.video.library.gl.render.record.YuMuxer;

import java.io.IOException;
import java.nio.ByteBuffer;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class VideoEncoder {
    private static final String TAG = RecordRender.TAG;

    private Surface mInputSurface;
    private VEncoder mEncoder;
    private boolean mIsInitSuccess = true;

    public VideoEncoder(YuVideoParam.RecordParam.VideoParam param, YuMuxer mediaMuxer) {
        MediaFormat format = genMediaFormat(param);
        MediaCodec mediaCodec;
        try {
            mediaCodec = MediaCodec.createEncoderByType(param.mime);
        } catch (IOException e) {
            mIsInitSuccess = false;
            return;
        }

        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mInputSurface = mediaCodec.createInputSurface();
        mediaCodec.start();

        //同步、异步编码器
        mEncoder = param.isAsync() ? new AsyncEncoder(mediaCodec) : new SyncEncoder(mediaMuxer, mediaCodec);
    }

    private MediaFormat genMediaFormat(YuVideoParam.RecordParam.VideoParam param) {
        //初始化 录制参数
        MediaFormat format = MediaFormat.createVideoFormat(param.mime, param.videoWidth, param.videoHeight);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, param.bitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, param.frameRate);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, param.iFrameRate);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int profile = 0;
            int level = 0;
            if (TextUtils.equals(param.mime, "video/avc")) {
                profile = MediaCodecInfo.CodecProfileLevel.AVCProfileHigh;
                if (param.videoWidth * param.videoHeight >= 1920 * 1080) {
                    level = MediaCodecInfo.CodecProfileLevel.AVCLevel4;
                } else {
                    level = MediaCodecInfo.CodecProfileLevel.AVCLevel31;
                }
            } else if (TextUtils.equals(param.mime, "video/hevc")) {
                profile = MediaCodecInfo.CodecProfileLevel.HEVCProfileMain;
                if (param.videoWidth * param.videoHeight >= 1920 * 1080) {
                    level = MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel4;
                } else {
                    level = MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel31;
                }
            }
            format.setInteger(MediaFormat.KEY_PROFILE, profile);
            format.setInteger(MediaFormat.KEY_LEVEL, level);
        }
        return format;
    }

    public boolean isInitSuccess() {
        return mIsInitSuccess;
    }

    public Surface getInputSurface() {
        return mInputSurface;
    }

    //编码
    public void encodeFrame(boolean isEndFrame) {
        if(!mIsInitSuccess) {
            return;
        }
        mEncoder.encodeFrame(isEndFrame);
    }

    public void stopEncode() {
        mEncoder.stopEncode();
    }

    public boolean isAddTrackSuccess() {
        return mEncoder.getTrackIndex() != null;
    }

    public boolean hasWriteData() {
        return mEncoder.mHasWriteData;
    }

    private static class AsyncEncoder extends VEncoder {
        private MediaCodec mMediaCodec;

        public AsyncEncoder(MediaCodec mediaCodec) {

        }

        public void encodeFrame(boolean isEndFrame) {

        }

        @Override
        public Integer getTrackIndex() {
            return null;
        }

        @Override
        public void stopEncode() {
            super.stopEncode();
        }
    }

    private static abstract class VEncoder {
        abstract void encodeFrame(boolean isEndFrame);
        abstract Integer getTrackIndex();

        protected long prevOutputPTSUs;

        protected long getPTSUs() {
            long result = System.nanoTime() / 1000L;
            // presentationTimeUs should be monotonic
            // otherwise muxer fail to write
            if (result < prevOutputPTSUs)
                result = (prevOutputPTSUs - result) + result;
            return result;
        }

        protected boolean mIsStopEncode;

        public void stopEncode() {
            mIsStopEncode = true;
        }

        protected boolean mHasWriteData;
    }

    private static class SyncEncoder extends VEncoder {
        private MediaCodec mMediaCodec;
        private MediaCodec.BufferInfo mBufferInfo;
        private static final int DEFAULT_TIMEOUT = 10000;
        private Integer mTrackIndex;
        private YuMuxer mMediaMuxer;
        private ByteBuffer[] encoderOutputBuffers;

        public SyncEncoder(YuMuxer mediaMuxer, MediaCodec mediaCodec) {
            mMediaMuxer = mediaMuxer;
            mMediaCodec = mediaCodec;
            mBufferInfo = new MediaCodec.BufferInfo();
            encoderOutputBuffers = mMediaCodec.getOutputBuffers();
        }

        public void encodeFrame(boolean isEndFrame) {
            if(mIsStopEncode) {
                return;
            }
            if (isEndFrame) {
                VLog.d(TAG, "sending end EOS to encoder");
                mMediaCodec.signalEndOfInputStream();
            }

            ByteBuffer[] encoderOutputBuffers = mMediaCodec.getOutputBuffers();
            long waitStartTime = System.currentTimeMillis();
            while (true) {
                //从输入流队列中取数据进行编码操作。
                int encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, DEFAULT_TIMEOUT);
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    //超时、当前没有可用的buffer
                    if (!isEndFrame) {
                        break;      // out of while
                    } else {
                        if (System.currentTimeMillis() - waitStartTime >= 1000) {
                            break;
                        } else {
                            VLog.d(TAG, "no output available, spinning to await EOS");
                        }
                    }
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    encoderOutputBuffers = mMediaCodec.getOutputBuffers();
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    //输出格式已更改，后续数据将遵循新格式。
                    MediaFormat newFormat = mMediaCodec.getOutputFormat();
                    // 提取视频轨道并打开复用器
                    mTrackIndex = mMediaMuxer.addTrack(newFormat);
                    mMediaMuxer.start();
                } else if (encoderStatus < 0) {
                    //当前没有可用的buffer
                    VLog.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " +
                            encoderStatus);
                } else {
                    //获取到有效数据
                    ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                    if (encodedData == null) {
                        throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
                                " was null");
                    }

                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        VLog.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                        mBufferInfo.size = 0;
                    }

                    if (mBufferInfo.size != 0) {
                        mBufferInfo.presentationTimeUs = getPTSUs();
                        prevOutputPTSUs = mBufferInfo.presentationTimeUs;

                        // adjust the ByteBuffer values to match BufferInfo (not needed?)
                        encodedData.position(mBufferInfo.offset);
                        encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
                        // 将编码数据写入复用器中
                        mMediaMuxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
                        mHasWriteData = true;
                        VLog.d(TAG, "sent " + mBufferInfo.size + " bytes to muxer, ts=" +
                                mBufferInfo.presentationTimeUs);
                    }

                    mMediaCodec.releaseOutputBuffer(encoderStatus, false);

                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        if (!isEndFrame) {
                            VLog.w(TAG, "reached end of stream unexpectedly");
                        } else {
                            VLog.d(TAG, "end of stream reached");
                        }
                        break;      // out of while
                    }
                }
            }
        }

        public Integer getTrackIndex() {
            return mTrackIndex;
        }

        @Override
        public void stopEncode() {
            super.stopEncode();

            if(mTrackIndex != null && !mHasWriteData) {
                mMediaMuxer.writeSampleData(mTrackIndex, encoderOutputBuffers[0], mBufferInfo);
            }

            mMediaCodec.stop();
            mMediaCodec.release();
        }
    }
}
