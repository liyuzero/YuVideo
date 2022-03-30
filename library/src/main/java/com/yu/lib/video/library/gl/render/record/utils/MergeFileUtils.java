package com.yu.lib.video.library.gl.render.record.utils;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;

import com.yu.lib.video.library.utils.VLog;
import com.yu.lib.video.library.gl.render.YuVideoParam;
import com.yu.lib.video.library.gl.render.record.RecordRender;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class MergeFileUtils {
    private static final String TAG = "MergeFileUtils";
    private ByteBuffer mByteBuffer;

    public MergeFileUtils(YuVideoParam.RecordParam recordParam) {
        mByteBuffer = ByteBuffer.allocate(recordParam.getVideoParam().videoWidth * recordParam.getVideoParam().videoHeight);
    }

    public void mergeSegmentFiles(List<String> files, String outputFile, OnMergeFileListener listener) {
        if(files == null || files.size() == 0 || TextUtils.isEmpty(outputFile)) {
            listener.onMergeFail();
            return;
        }

        long time = System.currentTimeMillis();

        VLog.d(RecordRender.TAG, "合成开始");

        MediaMuxer mediaMuxer = null;
        try {
            mediaMuxer = new MediaMuxer(outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(mediaMuxer == null) {
            listener.onMergeFail();
            return;
        }

        //1、找出有效的轨道编码数据，都是录制数据，
        MediaFormat[] recordFormatArr = getRecordMediaFormat(files);
        if(recordFormatArr == null) {
            listener.onMergeFail();
            return;
        }

        //2、启动muxer，准备合成
        int videoRecordTrackIndex = mediaMuxer.addTrack(recordFormatArr[0]);
        int audioRecordTrackIndex = mediaMuxer.addTrack(recordFormatArr[1]);
        mediaMuxer.start();

        long ptsOffset = 0L;
        for (String filePath: files) {
            ptsOffset = readAndWriteData(mediaMuxer, videoRecordTrackIndex, audioRecordTrackIndex, ptsOffset, filePath);
            if(ptsOffset == -1) {
                listener.onMergeFail();
                return;
            }
        }

        try {
            mediaMuxer.stop();
            mediaMuxer.release();
            listener.onMergeSuccess();
        } catch (Exception e) {
            VLog.e(TAG, "Muxer close error. No data was written");
            listener.onMergeFail();
        }

        VLog.d(RecordRender.TAG, "合成结束：" + (System.currentTimeMillis() - time));
    }

    private long readAndWriteData(MediaMuxer mediaMuxer, int videoRecordTrackIndex,
                                     int audioRecordTrackIndex, long ptsOffset, String filePath) {
        boolean hasVideo, hasAudio;

        int segmentAudioTrackIndex;
        int segmentVideoTrackIndex;

        MediaExtractor audioExtractor = new MediaExtractor();
        try {
            audioExtractor.setDataSource(filePath);
            segmentAudioTrackIndex = getAudioTrackIndex(audioExtractor);
            hasAudio = segmentAudioTrackIndex != -1;
            audioExtractor.selectTrack(segmentAudioTrackIndex);
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }

        MediaExtractor videoExtractor = new MediaExtractor();
        try {
            videoExtractor.setDataSource(filePath);
            segmentVideoTrackIndex = getVideoTrackIndex(videoExtractor);
            hasVideo = segmentVideoTrackIndex != -1;
            videoExtractor.selectTrack(segmentVideoTrackIndex);
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }

        mByteBuffer.rewind();

        if(!hasVideo && !hasAudio) {
            return -1;
        }

        long audioPts = 0L;
        long videoPts = 0L;

        while (true) {
            MediaExtractor readExtractor;
            int curReadTrackIndex;
            int curRecordTrackIndex;

            if(hasAudio && (!hasVideo || audioPts - videoPts <= 50000L)) {
                //录制audio
                readExtractor = audioExtractor;
                curReadTrackIndex = segmentAudioTrackIndex;
                curRecordTrackIndex = audioRecordTrackIndex;
            } else if(hasVideo) {
                //录制video
                readExtractor = videoExtractor;
                curReadTrackIndex = segmentVideoTrackIndex;
                curRecordTrackIndex = videoRecordTrackIndex;
            } else {
                break;
            }

            mByteBuffer.rewind();
            int chunkSize = readExtractor.readSampleData(mByteBuffer, 0);//读取帧数据
            if(chunkSize < 0) {
                if(curReadTrackIndex == segmentVideoTrackIndex) {
                    hasVideo = false;
                }
                if(curReadTrackIndex == segmentAudioTrackIndex) {
                    hasAudio = false;
                }
            } else {
                long presentationTimeUs = readExtractor.getSampleTime();//读取帧的pts
                if(curReadTrackIndex == segmentVideoTrackIndex) {
                    videoPts = presentationTimeUs;
                } else {
                    audioPts = presentationTimeUs;
                }

                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                info.offset = 0;
                info.size = chunkSize;
                info.presentationTimeUs = ptsOffset + presentationTimeUs;//pts重新计算
                if((readExtractor.getSampleFlags() & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        info.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
                    }
                }
                mByteBuffer.rewind();

                mediaMuxer.writeSampleData(curRecordTrackIndex, mByteBuffer, info);//写入文件
                readExtractor.advance();
            }
        }

        //记录当前文件的最后一个pts，作为下一个文件的pts offset
        ptsOffset += Math.max(videoPts, audioPts);
        ptsOffset += 10000L;//前一个文件的最后一帧与后一个文件的第一帧，差10ms，只是估计值，不准确，但能用

        VLog.i(TAG, "finish one file, ptsOffset " + ptsOffset);

        videoExtractor.release();
        audioExtractor.release();

        return ptsOffset;
    }

    private MediaFormat[] getRecordMediaFormat(List<String> files) {
        MediaExtractor extractor = new MediaExtractor();

        MediaFormat videoFormat = null;
        MediaFormat audioFormat = null;

        try {
            extractor.setDataSource(files.get(0));
            for (int i=0; i<extractor.getTrackCount(); i++) {
                MediaFormat mediaFormat = extractor.getTrackFormat(i);
                String mime = mediaFormat.getString("mime");
                if(TextUtils.isEmpty(mime)) {
                    return null;
                }
                if(mime.startsWith("video/")) {
                    //视频
                    videoFormat = extractor.getTrackFormat(i);
                } else if(mime.startsWith("audio/")) {
                    //音频
                    audioFormat = extractor.getTrackFormat(i);
                }
            }
        } catch (IOException e) {
            return null;
        } finally {
            extractor.release();
        }

        return videoFormat == null || audioFormat == null? null: new MediaFormat[]{videoFormat, audioFormat};
    }

    private int getVideoTrackIndex(MediaExtractor extractor) {
        return getTrackIndex(extractor, "video/");
    }

    private int getAudioTrackIndex(MediaExtractor extractor) {
        return getTrackIndex(extractor, "audio/");
    }

    private int getTrackIndex(MediaExtractor extractor, String mimePrefix) {
        for (int i=0; i<extractor.getTrackCount(); i++) {
            MediaFormat mediaFormat = extractor.getTrackFormat(i);
            String mime = mediaFormat.getString("mime");
            if(!TextUtils.isEmpty(mime) && mime.startsWith(mimePrefix)) {
                return i;
            }
        }
        return -1;
    }

    public void release() {

    }

    public interface OnMergeFileListener {
        void onMergeSuccess();
        void onMergeFail();
    }
}
