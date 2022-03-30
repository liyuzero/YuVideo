package com.yu.lib.video.library.gl.filter.beauty;

import android.content.Context;
import android.opengl.GLES20;

import com.yu.lib.video.library.R;
import com.yu.lib.video.library.gl.filter.base.BaseFilter;

//磨皮滤镜
public class GaussianBlurFilter extends BaseFilter {
    private int mWidthLocation;
    private int mHeightLocation;
    private int mOpacityLocation;
    private float mOpacity = 0.5f;

    public GaussianBlurFilter(Context context) {
        super(context, R.raw.fragment_gaus_blur);
    }

    @Override
    public boolean setupProgram(int programId) {
        mWidthLocation = GLES20.glGetUniformLocation(programId, "width");
        mHeightLocation = GLES20.glGetUniformLocation(programId, "height");
        mOpacityLocation = GLES20.glGetUniformLocation(programId, "opacity");
        return true;
    }

    @Override
    public void onDrawBegin() {
        GLES20.glUniform1i(mWidthLocation, mFrameWidth);
        GLES20.glUniform1i(mHeightLocation, mFrameHeight);
        GLES20.glUniform1f(mOpacityLocation, mOpacity);
    }

    public void setOpacity(float opacity) {
        mOpacity = opacity;
    }

    @Override
    public void onDrawEnd() {

    }
}
