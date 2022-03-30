package com.yu.lib.video.library.gl.gles;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class OpenGLUtils {
    private static final String TAG = "ShaderHelper";
    public static final int HAS_NO_TEXTURE = -1;

    public static int createProgram(String vertexStr, String fragmentStr) {
        return linkProgram(compileVertexShader(vertexStr), compileFragmentShader(fragmentStr));
    }

    public static int compileVertexShader(String shaderCode) {
        return compileShader(GLES20.GL_VERTEX_SHADER, shaderCode);
    }

    public static int compileFragmentShader(String shaderCode) {
        return compileShader(GLES20.GL_FRAGMENT_SHADER, shaderCode);
    }

    private static int compileShader(int type, String shaderCode) {
        int shaderObjId = GLES20.glCreateShader(type);
        if(shaderObjId == 0) {
            Log.d(TAG, "could not create shader");
        } else {
            GLES20.glShaderSource(shaderObjId, shaderCode);
            GLES20.glCompileShader(shaderObjId);
            int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(shaderObjId, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
            Log.d(TAG, GLES20.glGetShaderInfoLog(shaderObjId));
            if(compileStatus[0] == 0) {
                //fail
                GLES20.glDeleteShader(shaderObjId);
                Log.d(TAG, "shader compile fail");
            }
        }
        return shaderObjId;
    }

    public static int linkProgram(int vertexShaderId, int fragmentShaderId) {
        int programObjectId = GLES20.glCreateProgram();
        if(programObjectId == 0) {
            Log.d(TAG, "could not create new program");
        }
        GLES20.glAttachShader(programObjectId, vertexShaderId);
        GLES20.glAttachShader(programObjectId, fragmentShaderId);
        GLES20.glLinkProgram(programObjectId);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(programObjectId, GLES20.GL_LINK_STATUS, linkStatus, 0);
        Log.d(TAG, GLES20.glGetProgramInfoLog(programObjectId));
        if(linkStatus[0] == 0) {
            GLES20.glDeleteProgram(programObjectId);
            Log.d(TAG, "link program fail");
        }
        return programObjectId;
    }

    public static boolean isValidateProgram(int programObjectId) {
        GLES20.glValidateProgram(programObjectId);
        int[] validateStatus = new int[1];
        GLES20.glGetProgramiv(programObjectId, GLES20.GL_VALIDATE_STATUS, validateStatus, 0);
        Log.d(TAG, "Results of validating program: " + validateStatus[0] + "\nLog:" + GLES20.glGetProgramInfoLog(programObjectId));
        return validateStatus[0] != 0;
    }

    /**
     * 创建Sampler2D的Framebuffer 和 Texture
     */
    public static void createFrameBuffer(int[] frameBuffer, int[] frameBufferTexture,
                                         int width, int height) {
        GLES20.glGenFramebuffers(frameBuffer.length, frameBuffer, 0);
        GLES20.glGenTextures(frameBufferTexture.length, frameBufferTexture, 0);
        for (int i = 0; i < frameBufferTexture.length; i++) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameBufferTexture[i]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                    GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[i]);
            // 让fbo与纹理绑定起来，后续的操作就是在操作fbo与这个纹理上了
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                    GLES20.GL_TEXTURE_2D, frameBufferTexture[i], 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        }
        checkGlError("createFrameBuffer");
    }

    /**
     * 创建OES 类型的Texture
     */
    public static int createOESTexture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            return createTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        }
        return -1;
    }

    public static int createTexture(int textureType) {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        OpenGLUtils.checkGlError("glGenTextures");
        int textureId = textures[0];
        GLES20.glBindTexture(textureType, textureId);
        OpenGLUtils.checkGlError("glBindTexture " + textureId);
        GLES20.glTexParameterf(textureType, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(textureType, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(textureType, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(textureType, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        OpenGLUtils.checkGlError("glTexParameter");
        return textureId;
    }

    /**
     * 加载bitmap纹理
     */
    public static int createTextureFromBitmapRes(Context context, int bitmapRes) {
        int[] textureHandle = new int[1];
        GLES20.glGenTextures(1, textureHandle, 0);
        if (textureHandle[0] != 0) {
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), bitmapRes);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            bitmap.recycle();
        }
        if (textureHandle[0] == 0) {
            return 0;
        }
        return textureHandle[0];
    }

    /**
     * 绑定纹理
     * @param location  句柄
     * @param texture   纹理id
     * @param index     索引
     */
    public static void bindTexture(int location, int texture, int index) {
        bindTexture(location, texture, index, GLES20.GL_TEXTURE_2D);
    }

    /**
     * 绑定纹理
     * @param location  句柄
     * @param texture   纹理值
     * @param index     绑定的位置
     * @param textureType 纹理类型
     */
    public static void bindTexture(int location, int texture, int index, int textureType) {
        // 最多支持绑定32个纹理
        if (index > 31) {
            throw new IllegalArgumentException("index must be no more than 31!");
        }
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + index);
        GLES20.glBindTexture(textureType, texture);
        GLES20.glUniform1i(location, index);
    }

    /**
     * 检查是否出错
     */
    public static void checkGlError(String op) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            String msg = op + ": glError 0x" + Integer.toHexString(error);
            Log.e(TAG, msg);
        }
    }

    /**
     * 删除纹理
     */
    public static void deleteTexture(int texture) {
        int[] textures = new int[1];
        textures[0] = texture;
        GLES20.glDeleteTextures(1, textures, 0);
    }

    public static final int BYTES_PER_FLOAT = 4;

    public static FloatBuffer getFloatBuffer(float[] data) {
        FloatBuffer floatBuffer = ByteBuffer.allocateDirect(data.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        floatBuffer.put(data);
        return floatBuffer;
    }

    public static String getRawString(Context context, int rawRes) {
        return OpenGLUtils.getRawString(context.getResources().openRawResource(rawRes));
    }

    public static String getRawString(InputStream inputStream) {
        InputStreamReader inputStreamReader = null;
        try {
            inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        BufferedReader reader = new BufferedReader(inputStreamReader);
        StringBuffer sb = new StringBuffer("");
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

}
