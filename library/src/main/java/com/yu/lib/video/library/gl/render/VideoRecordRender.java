package com.yu.lib.video.library.gl.render;

public interface VideoRecordRender extends VideoRender {
    void startRecord();
    void stopSegmentRecord();
    void stopRecord();
    void deleteSegmentRecord(int index);
}
