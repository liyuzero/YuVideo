package com.yu.lib.video.library.gl.filter.beauty;

import android.content.Context;
import android.opengl.GLES20;

import com.yu.lib.video.library.R;
import com.yu.lib.video.library.gl.gles.OpenGLUtils;
import com.yu.lib.video.library.gl.filter.base.BaseFilter;

public class ComplexionFilter extends BaseFilter {
    private int grayTextureLocation;
    private int mGrayTexture;

    private int lookupTextureLocation;
    private int mLookupTexture;

    private int levelRangeInvLocation;
    private int levelBlackLocation;
    private int alphaLocation;

    private float levelRangeInv;
    private float levelBlack;
    private float alpha;

    public ComplexionFilter(Context context) {
        super(context, R.raw.fragment_beauty_complexion);
    }

    @Override
    public boolean setupProgram(int programId) {
        grayTextureLocation = GLES20.glGetUniformLocation(programId, "grayTexture");
        lookupTextureLocation = GLES20.glGetUniformLocation(programId, "lookupTexture");
        levelRangeInvLocation = GLES20.glGetUniformLocation(programId, "levelRangeInv");
        levelBlackLocation = GLES20.glGetUniformLocation(programId, "levelBlack");
        alphaLocation = GLES20.glGetUniformLocation(programId, "alpha");
        levelRangeInv = 1.040816f;
        levelBlack = 0.01960784f;
        alpha = 0.618f;
        return createTexture();
    }

    private boolean createTexture() {
        mGrayTexture = OpenGLUtils.createTextureFromBitmapRes(mContext, R.raw.texture_skin_gray);
        mLookupTexture = OpenGLUtils.createTextureFromBitmapRes(mContext, R.raw.texture_skin_lookup);
        return mGrayTexture != 0 && mLookupTexture != 0;
    }

    @Override
    public void onDrawBegin() {
        OpenGLUtils.bindTexture(grayTextureLocation, mGrayTexture, 1);
        OpenGLUtils.bindTexture(lookupTextureLocation, mLookupTexture, 2);
        GLES20.glUniform1f(levelRangeInvLocation, levelRangeInv);
        GLES20.glUniform1f(levelBlackLocation, levelBlack);
        GLES20.glUniform1f(alphaLocation, alpha);
    }

    @Override
    public void onDrawEnd() {

    }

    @Override
    public void release() {
        super.release();
        GLES20.glDeleteTextures(2, new int[]{ mGrayTexture, mLookupTexture }, 0);
    }

    /**
     * 美肤程度
     * @param level 0 ~ 1.0f
     */
    public void setComplexionLevel(float level) {
        alpha = level;
    }
}
