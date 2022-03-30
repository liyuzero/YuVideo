package com.yu.lib.video.library.gl.filter;

import android.content.Context;

import com.yu.lib.video.library.gl.BeautyParam;
import com.yu.lib.video.library.gl.gles.OpenGLUtils;
import com.yu.lib.video.library.gl.filter.base.BaseFilter;
import com.yu.lib.video.library.gl.filter.base.DisplayFilter;
import com.yu.lib.video.library.gl.filter.base.InputOesFilter;
import com.yu.lib.video.library.gl.filter.base.ShaderData;
import com.yu.lib.video.library.gl.filter.beauty.BeautyFilter;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class FilterChain implements FilterApi {
    //世界坐标数据
    public FloatBuffer mVertexData = OpenGLUtils.getFloatBuffer(ShaderData.VERTEX_DATA);
    //纹理坐标数据
    public FloatBuffer mTextureCoordinateData = OpenGLUtils.getFloatBuffer(ShaderData.TEXTURE_VERTEX_DATA);

    private InputOesFilter mInputFilter;
    private DisplayFilter mOutputFilter;
    private BeautyFilter mBeautyFilter;
    private BaseFilter mSingleFilter;

    public final List<BaseFilter> mFilters = new ArrayList<>();
    private int mCurIndex = -1;

    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int mTextureWidth;
    private int mTextureHeight;

    public FilterChain(Context context) {
        mInputFilter = new InputOesFilter();
        mOutputFilter = new DisplayFilter();
        mBeautyFilter = new BeautyFilter(context);
    }

    public void init(int scaleType) {
        mInputFilter.init(scaleType);
        mInputFilter.init();
        mOutputFilter.init();
        mBeautyFilter.init();
    }

    public void onSurfaceChanged(int width, int height) {
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        mInputFilter.onSurfaceChanged(width, height);
        mOutputFilter.onSurfaceChanged(width, height);
        mBeautyFilter.onSurfaceChanged(width, height);
        for (BaseFilter filter : mFilters) {
            filter.onSurfaceChanged(width, height);
        }
    }

    public void onTextureSizeChanged(int textureWidth, int textureHeight) {
        mTextureWidth = textureWidth;
        mTextureHeight = textureHeight;
        mInputFilter.onTextureSizeChanged(textureWidth, textureHeight);
        mOutputFilter.onTextureSizeChanged(textureWidth, textureHeight);
        mBeautyFilter.onTextureSizeChanged(textureWidth, textureHeight);
        for (BaseFilter filter : mFilters) {
            filter.onTextureSizeChanged(textureWidth, textureHeight);
        }
    }

    public int process(int inputTexture, float[] sTMatrix) {
        mCurIndex = -1;
        mInputFilter.setSTMatrix(sTMatrix);
        int curTextureId = mInputFilter.drawFrameBuffer(inputTexture, mVertexData, mTextureCoordinateData);
        curTextureId = mBeautyFilter.drawFrameBuffer(curTextureId, mVertexData, mTextureCoordinateData);
        return processFilter(curTextureId);
    }

    private int processFilter(int curTextureId) {
        if (++mCurIndex < mFilters.size()) {
            BaseFilter curFilter = mFilters.get(mCurIndex);
            if(curFilter.isNeedInit()) {
                if(curFilter.init()) {
                    curFilter.onSurfaceChanged(mSurfaceWidth, mSurfaceHeight);
                    curFilter.onTextureSizeChanged(mTextureWidth, mTextureHeight);
                } else {
                    return processFilter(curTextureId);
                }
            }
            curTextureId = curFilter.drawFrameBuffer(curTextureId, mVertexData, mTextureCoordinateData);
            return processFilter(curTextureId);
        } else {
            mOutputFilter.drawFrame(curTextureId, mVertexData, mTextureCoordinateData);
            mCurIndex = -1;
            return curTextureId;
        }
    }

    public void release() {
        mInputFilter.release();
        for (BaseFilter filter : mFilters) {
            filter.release();
        }
        mFilters.clear();
        mOutputFilter.release();
    }

    public boolean isInitSuccess() {
        for (BaseFilter filter : mFilters) {
            if (!filter.isInitialized()) {
                return false;
            }
        }
        return mInputFilter.isInitialized() && mOutputFilter.isInitialized();
    }

    @Override
    public void addFilter(final BaseFilter filter) {
        if (!mFilters.contains(filter)) {
            mFilters.add(filter);
        }
    }

    @Override
    public void setSingleFilter(final BaseFilter filter) {
        if(mSingleFilter != null) {
            mFilters.remove(mSingleFilter);
            mSingleFilter.release();
        }
        mSingleFilter = filter;
        if(filter != null) {
            mFilters.add(filter);
        }
    }

    @Override
    public void setBeautyParam(final BeautyParam param) {
        if(param == null) {
            return;
        }
        mBeautyFilter.setComplexionLevel(param.getComplexionLevel());
        mBeautyFilter.setGaussLevel(param.getGaussLevel());
    }

    public int getTextureWidth() {
        return mTextureWidth;
    }

    public int getTextureHeight() {
        return mTextureHeight;
    }
}
