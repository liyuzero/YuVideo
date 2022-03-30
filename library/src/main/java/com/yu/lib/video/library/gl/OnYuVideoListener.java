package com.yu.lib.video.library.gl;

public interface OnYuVideoListener {
    void onRenderTextureInitComplete(boolean isInitSuccess);
    void onRenderStart();
    void onRecordSegmentFinish(int segmentIndex, long segmentDuration, long curTotalDuration);
    //停止录制了，输出
    void onRecordFinish(String outputFilePath, long duration);
    //录制失败
    void onRecordFail();
    void onRecordDeleteSegment(boolean isSuccess, int index, int count);
    /**
    * 录制回调
    *
    * @param segmentIndex 如果不是分段录制模式，此处返回-1，如果是分段录制，返回对应片段的index
    * @param curDuration 如果不是分段录制模式，返回当前录制时间，如果是分段录制，返回当前所有片段总录制时间
    *
    * */
    void onRecordCallBack(int segmentIndex, long curDuration);
}
