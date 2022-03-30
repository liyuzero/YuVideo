package com.yu.lib.video.library.gl.filter.beauty;

import android.content.Context;

import java.nio.FloatBuffer;

public class BeautyFilter {

    private ComplexionFilter mComplexionFilter;
    private GaussianBlurFilter mGaussianBlurFilter;

    public BeautyFilter(Context context) {
        mComplexionFilter = new ComplexionFilter(context);
        mGaussianBlurFilter = new GaussianBlurFilter(context);
    }

    public void init() {
        mComplexionFilter.init();
        mGaussianBlurFilter.init();
    }

    public void onSurfaceChanged(int w, int h) {
        mComplexionFilter.onSurfaceChanged(w, h);
        mGaussianBlurFilter.onSurfaceChanged(w, h);
    }

    public void onTextureSizeChanged(int textureWidth, int textureHeight) {
        mComplexionFilter.onTextureSizeChanged(textureWidth, textureHeight);
        mGaussianBlurFilter.onTextureSizeChanged(textureWidth, textureHeight);
    }

    public int drawFrameBuffer(int textureId, FloatBuffer vertexBuffer, FloatBuffer textureBuffer) {
        int curId = textureId;
        if(mComplexionFilter.isEnable()) {
            curId = mComplexionFilter.drawFrameBuffer(curId, vertexBuffer, textureBuffer);
        }
        if(mGaussianBlurFilter.isEnable()) {
            curId = mGaussianBlurFilter.drawFrameBuffer(curId, vertexBuffer, textureBuffer);
        }
        return curId;
    }

    public void setComplexionLevel(float level) {
        if(level != 0) {
            mComplexionFilter.setEnable(true);
            mComplexionFilter.setComplexionLevel(level);
        } else {
            mComplexionFilter.setEnable(false);
        }
    }

    public void setGaussLevel(float level) {
        mGaussianBlurFilter.setOpacity(level);
    }
}
