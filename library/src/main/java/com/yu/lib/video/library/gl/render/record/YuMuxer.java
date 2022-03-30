package com.yu.lib.video.library.gl.render.record;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class YuMuxer {
    private MediaMuxer mMediaMuxer;
    //加锁，只有在各个轨道添加完毕、并且MediaMuxer start之后，才允许写入真实数据
    private CountDownLatch mCountDownLatch;

    public YuMuxer(String path, int format, int trackCount) throws IOException {
        mCountDownLatch = new CountDownLatch(trackCount + 1);
        mMediaMuxer = new MediaMuxer(path, format);
    }

    public int addTrack(MediaFormat format) {
        int index = mMediaMuxer.addTrack(format);
        mCountDownLatch.countDown();
        return index;
    }

    public void writeSampleData(int trackIndex, ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo) {
        try {
            //此处，如果 MediaMuxer 还未启动，线程等待，避免写入错误
            mCountDownLatch.await();
        } catch (InterruptedException e) {
            //e.printStackTrace();
        }
        mMediaMuxer.writeSampleData(trackIndex, byteBuf, bufferInfo);
    }

    public void start() {
        if(mCountDownLatch.getCount() == 1) {
            mMediaMuxer.start();
            mCountDownLatch.countDown();
        }
    }

    public void stop() {
        if(mCountDownLatch.getCount() != 0) {
            return;
        }
        mMediaMuxer.release();
    }
}
