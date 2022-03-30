package com.yu.lib.video.library.gl.filter.base;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Build;

import com.yu.lib.video.library.YuVideoManager;

import static android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
import static android.opengl.GLES20.glGetUniformLocation;

public class InputOesFilter extends BaseFilter {
    //纹理坐标
    private int mSTMatrixLocation;
    private float[] mSTMatrix = new float[16];

    private final float[] projectionMatrix = new float[16];
    private int uMatrixLocation;
    private int mScaleType;

    public InputOesFilter() {
        super(ShaderData.INPUT_VERTEX_SHADER, ShaderData.INPUT_OES_FRAGMENT_SHADER);
    }

    public void init(int scaleType) {
        mScaleType = scaleType;
    }

    @Override
    public boolean setupProgram(int programId) {
        uMatrixLocation = GLES20.glGetUniformLocation(programId, "uMatrix");
        mSTMatrixLocation = glGetUniformLocation(programId, "uSTMatrix");
        return true;
    }

    @Override
    public void onDrawBegin() {
        GLES20.glUniformMatrix4fv(uMatrixLocation, 1, false, projectionMatrix, 0);
        GLES20.glUniformMatrix4fv(mSTMatrixLocation, 1, false, mSTMatrix, 0);
    }

    @Override
    public void onDrawEnd() {

    }

    @Override
    public int getTextureType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            return GL_TEXTURE_EXTERNAL_OES;
        }
        return -1;
    }

    public void setSTMatrix(float[] STMatrix) {
        mSTMatrix = STMatrix;
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        super.onSurfaceChanged(width, height);
    }

    @Override
    public void onTextureSizeChanged(int textureWidth, int textureHeight) {
        super.onTextureSizeChanged(textureWidth, textureHeight);
        updateProjection();
    }

    private void updateProjection() {
        switch (mScaleType) {
            case YuVideoManager.SCALE_TYPE_AUTO:
                if (Math.abs(Math.floor(mFrameWidth * 1.0 / mFrameHeight)
                        - Math.floor(mDisplayWidth * 1.0 / mDisplayHeight)) >= 1) {//视频横屏，容器竖屏 或者 视频竖屏，容器横屏
                    updateFitCenterProjection(mFrameWidth, mFrameHeight);
                } else {
                    updateCenterCropProjection(mFrameWidth, mFrameHeight);
                }
                break;
            case YuVideoManager.SCALE_TYPE_FIT_CENTER:
                updateFitCenterProjection(mFrameWidth, mFrameHeight);
                break;
            case YuVideoManager.SCALE_TYPE_CENTER_CROP:
                updateCenterCropProjection(mFrameWidth, mFrameHeight);
                break;
        }
    }

    private void updateFitCenterProjection(int videoWidth, int videoHeight) {
        float screenRatio = (float) mDisplayWidth / mDisplayHeight;
        float videoRatio = (float) videoWidth / videoHeight;
        if (videoRatio > screenRatio) {
            //视频高度小于surface高度
            Matrix.orthoM(projectionMatrix, 0, -1f, 1f,
                    -videoRatio / screenRatio, videoRatio / screenRatio, -1f, 1f);
        } else {
            Matrix.orthoM(projectionMatrix, 0, -screenRatio / videoRatio, screenRatio / videoRatio, -1f, 1f, -1f, 1f);
        }
    }

    private void updateCenterCropProjection(int videoWidth, int videoHeight) {
        float screenRatio = (float) mDisplayWidth / mDisplayHeight;
        float videoRatio = (float) videoWidth / videoHeight;
        if (videoRatio > screenRatio) {
            Matrix.orthoM(projectionMatrix, 0, -screenRatio / videoRatio,
                    screenRatio / videoRatio, -1, 1, -1f, 1f);
        } else {
            //视频高度大于surface高度
            Matrix.orthoM(projectionMatrix, 0, -1, 1, -videoRatio / screenRatio, videoRatio / screenRatio, -1f, 1f);
        }
    }
}
