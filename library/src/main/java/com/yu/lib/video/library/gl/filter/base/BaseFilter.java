package com.yu.lib.video.library.gl.filter.base;

import android.content.Context;
import android.opengl.GLES20;
import android.os.Build;
import android.text.TextUtils;

import com.yu.lib.video.library.gl.gles.OpenGLUtils;

import java.nio.FloatBuffer;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

public abstract class BaseFilter {
    protected Context mContext;
    private int mProgramId;

    private int mPositionLocation;

    private int mTextureCoordinateLocation;

    private int mInputTextureLocation;

    protected int mFrameWidth;
    protected int mFrameHeight;

    protected int mDisplayWidth;
    protected int mDisplayHeight;

    private boolean mIsInitialized;
    private boolean mIsNeedInit = true;

    private int mFragmentShaderRawRes;
    private String mVertexShader;
    private String mFragmentShader;
    private boolean mIsEnable = true;

    protected BaseFilter(Context context, int fragmentShaderRawRes) {
        mContext = context;
        mFragmentShaderRawRes = fragmentShaderRawRes;
    }

    protected BaseFilter(String vertexShader, String fragmentShader) {
        mVertexShader = vertexShader;
        mFragmentShader = fragmentShader;
    }

    public boolean init() {
        if(mContext != null) {
            mIsNeedInit = false;
            return init(ShaderData.DISPLAY_VERTEX_SHADER, OpenGLUtils.getRawString(mContext.getResources().openRawResource(mFragmentShaderRawRes)));
        } else {
            mIsNeedInit = false;
            return init(mVertexShader, mFragmentShader);
        }
    }

    private boolean init(String vertexShader, String fragmentShader) {
        mIsInitialized = !TextUtils.isEmpty(vertexShader) && !TextUtils.isEmpty(fragmentShader);
        mProgramId = OpenGLUtils.createProgram(vertexShader, fragmentShader);
        mIsInitialized = OpenGLUtils.isValidateProgram(mProgramId);
        if(!mIsInitialized) {
            return false;
        }
        return initProgramHandle();
    }

    private boolean initProgramHandle() {
        if (!mIsInitialized) {
            return false;
        }
        //vertex
        mPositionLocation = glGetAttribLocation(mProgramId, "aPosition");
        mTextureCoordinateLocation = glGetAttribLocation(mProgramId, "vTextureCoordinate");

        //fragment
        mInputTextureLocation = glGetUniformLocation(mProgramId, "inputTexture");

        return setupProgram(mProgramId);
    }

    public abstract boolean setupProgram(int programId);

    public void onSurfaceChanged(int width, int height) {
        mDisplayWidth = width;
        mDisplayHeight = height;
    }

    //理论上该部分先调用，进行FrameBuffer初始化
    public void onTextureSizeChanged(int textureWidth, int textureHeight) {
        initFrameBuffer(textureWidth, textureHeight);
        mFrameWidth = textureWidth;
        mFrameHeight = textureHeight;
    }

    public int drawFrameBuffer(int textureId, FloatBuffer vertexBuffer, FloatBuffer textureBuffer) {
        if (textureId == OpenGLUtils.HAS_NO_TEXTURE || mFrameBuffers == null || !mIsInitialized) {
            return OpenGLUtils.HAS_NO_TEXTURE;
        }
        //绑定FBO
        glViewport(0, 0, mFrameWidth, mFrameHeight);
        glBindFramebuffer(GL_FRAMEBUFFER, mFrameBuffers[0]);
        glUseProgram(mProgramId);

        //依照传入的textureId
        onDrawTexture(textureId, vertexBuffer, textureBuffer);

        //激活默认帧缓冲，使得渲染操作在主窗口中有视觉效果
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        //给下一个链路进行绘制
        return mFrameBufferTextures[0];
    }

    public void drawFrame(int textureId, FloatBuffer vertexBuffer, FloatBuffer textureBuffer) {
        if(!mIsInitialized || textureId == OpenGLUtils.HAS_NO_TEXTURE) {
            return ;
        }
        glViewport(0, 0, mDisplayWidth, mDisplayHeight);
        glClearColor(0.0f, 0.0f, 0.0f, 1f);
        glClear(GL_COLOR_BUFFER_BIT);

        glUseProgram(mProgramId);

        onDrawTexture(textureId, vertexBuffer, textureBuffer);
    }

    /**
     * 绘制
     */
    private void onDrawTexture(int textureId, FloatBuffer vertexBuffer, FloatBuffer textureBuffer) {
        //绑定数据
        glEnableVertexAttribArray(mPositionLocation);
        vertexBuffer.position(0);
        glVertexAttribPointer(mPositionLocation, 2, GL_FLOAT, false, 0, vertexBuffer);

        textureBuffer.position(0);
        glVertexAttribPointer(mTextureCoordinateLocation, 2, GL_FLOAT, false, 0, textureBuffer);
        glEnableVertexAttribArray(mTextureCoordinateLocation);

        // 绑定纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(getTextureType(), textureId);
        GLES20.glUniform1i(mInputTextureLocation, 0);

        onDrawBegin();

        //开始绘制顶点，顶点组的数量
        glDrawArrays(GL_TRIANGLE_STRIP, 0, ShaderData.VERTEX_DATA.length / ShaderData.VERTEX_PER_GROUP_NUM);

        onDrawEnd();

        // 解绑数据
        GLES20.glDisableVertexAttribArray(mPositionLocation);
        GLES20.glDisableVertexAttribArray(mTextureCoordinateLocation);
        GLES20.glBindTexture(getTextureType(), 0);
        GLES20.glUseProgram(0);
    }

    //填充数据
    public abstract void onDrawBegin();
    public abstract void onDrawEnd();

    public int getTextureType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            return GL_TEXTURE_2D;
        }
        return -1;
    }

    private int[] mFrameBuffers;
    private int[] mFrameBufferTextures;

    /**
     * 创建FBO
     */
    public void initFrameBuffer(int width, int height) {
        if (!mIsInitialized) {
            return;
        }
        if (mFrameBuffers != null && (mFrameWidth != width || mFrameHeight != height)) {
            destroyFrameBuffer();
        }
        if (mFrameBuffers == null) {
            mFrameWidth = width;
            mFrameHeight = height;
            mFrameBuffers = new int[1];
            mFrameBufferTextures = new int[1];
            OpenGLUtils.createFrameBuffer(mFrameBuffers, mFrameBufferTextures, width, height);
        }
    }

    /**
     * 销毁纹理
     */
    public void destroyFrameBuffer() {
        if (!mIsInitialized) {
            return;
        }
        if (mFrameBufferTextures != null) {
            GLES20.glDeleteTextures(1, mFrameBufferTextures, 0);
            mFrameBufferTextures = null;
        }

        if (mFrameBuffers != null) {
            GLES20.glDeleteFramebuffers(1, mFrameBuffers, 0);
            mFrameBuffers = null;
        }
        mFrameWidth = -1;
        mFrameHeight = -1;
    }

    public void release() {
        if (mIsInitialized) {
            glDeleteProgram(mProgramId);
            mProgramId = OpenGLUtils.HAS_NO_TEXTURE;
        }
        destroyFrameBuffer();
    }

    public boolean isInitialized() {
        return mIsInitialized;
    }

    public boolean isNeedInit() {
        return mIsNeedInit;
    }

    public boolean isEnable() {
        return mIsEnable;
    }

    public void setEnable(boolean isEnable) {
        mIsEnable = isEnable;
    }
}
