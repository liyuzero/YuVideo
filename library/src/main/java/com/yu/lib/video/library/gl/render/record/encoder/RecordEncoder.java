package com.yu.lib.video.library.gl.render.record.encoder;

import android.media.MediaMuxer;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.RequiresApi;

import com.yu.lib.video.library.gl.OnYuVideoListener;
import com.yu.lib.video.library.gl.render.YuVideoParam;
import com.yu.lib.video.library.gl.render.record.YuMuxer;
import com.yu.lib.video.library.gl.render.record.utils.RecordFileUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class RecordEncoder {
    private YuMuxer mMediaMuxer;
    private VideoEncoder mVideoEncoder;
    private AudioEncoder mAudioEncoder;
    private boolean mIsInitSuccess;

    private final Handler mVideoHandler;
    private final Handler mAudioHandler;

    public RecordEncoder(YuVideoParam.RecordParam param, int curSegmentIndex, HandlerThread videoEncodeThread,
                         HandlerThread audioEncodeThread, final OnYuVideoListener onYuVideoListener) {
        mVideoHandler = new Handler(videoEncodeThread.getLooper());
        mAudioHandler = new Handler(audioEncodeThread.getLooper());

        try {
            String filePath = param.isSegmentRecord() ?
                    RecordFileUtils.getTempFilePath(param.getSegmentDirPath(), curSegmentIndex) :
                    param.getOutputFilePath();
            File parentDir = new File(filePath).getParentFile();
            if(!parentDir.exists() && !parentDir.mkdirs()) {
                onYuVideoListener.onRecordFail();
                return;
            }
            mMediaMuxer = new YuMuxer(filePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4, 2);
        } catch (IOException e) {
            onYuVideoListener.onRecordFail();
            return;
        }
        mIsInitSuccess = true;
        mVideoEncoder = new VideoEncoder(param.getVideoParam(), mMediaMuxer);
        mAudioEncoder = new AudioEncoder(mMediaMuxer, param.getAudioParam());
        mIsInitSuccess = mVideoEncoder.isInitSuccess() && mAudioEncoder.isInitSuccess();
    }

    public void encodeVideoFrame() {
        if (!mIsInitSuccess) {
            return;
        }
        mVideoHandler.post(new Runnable() {
            @Override
            public void run() {
                mVideoEncoder.encodeFrame(false);
            }
        });
    }

    public void encodeAudioData(final byte[] audioData, final int len) {
        if (!mIsInitSuccess) {
            return;
        }
        mAudioHandler.post(new Runnable() {
            @Override
            public void run() {
                mAudioEncoder.encodeData(audioData, len);
            }
        });
    }

    public void stopEncode() {
        if (!mIsInitSuccess) {
            return;
        }
        mIsInitSuccess = false;

        final CountDownLatch countDownLatch = new CountDownLatch(2);

        mVideoHandler.post(new Runnable() {
            @Override
            public void run() {
                mVideoEncoder.encodeFrame(true);
                mVideoEncoder.stopEncode();
                countDownLatch.countDown();
            }
        });

        mAudioHandler.post(new Runnable() {
            @Override
            public void run() {
                mAudioEncoder.stopEncode();
                countDownLatch.countDown();
            }
        });

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mMediaMuxer.stop();
        mMediaMuxer = null;
    }

    public Surface getVideoInputSurface() {
        if(!mIsInitSuccess) {
            return null;
        }
        return mVideoEncoder.getInputSurface();
    }
}
