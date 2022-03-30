package com.yu.lib.video.library.gl.render;

import android.graphics.SurfaceTexture;

import com.yu.lib.video.library.gl.OnYuVideoListener;
import com.yu.lib.video.library.gl.filter.FilterApi;

public interface VideoRender {
    void init(YuVideoParam yuVideoParam, SurfaceTexture srcTexture, int scaleType, OnYuVideoListener listener);
    void onSurfaceChanged(int width, int height);
    void onVideoSizeChanged(int videoWidth, int videoHeight);
    void release();
    FilterApi getFilterApi();
    SurfaceTexture getRenderSurfaceTexture();
}
