package com.yu.lib.video.library.demo.learn;

import android.opengl.GLES20;
import android.util.Log;

import com.yu.lib.video.library.utils.VLog;

public class ShaderHelper {
    private static final String TAG = "ShaderHelper";

    public static int compileVertexShader(String shaderCode) {
        return compileShader(GLES20.GL_VERTEX_SHADER, shaderCode);
    }

    public static int compileFragmentShader(String shaderCode) {
        return compileShader(GLES20.GL_FRAGMENT_SHADER, shaderCode);
    }

    private static int compileShader(int type, String shaderCode) {
        int shaderObjId = GLES20.glCreateShader(type);
        if(shaderObjId == 0) {
            VLog.d(TAG, "could not create shader");
        } else {
            GLES20.glShaderSource(shaderObjId, shaderCode);
            GLES20.glCompileShader(shaderObjId);
            int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(shaderObjId, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
            VLog.d(TAG, GLES20.glGetShaderInfoLog(shaderObjId));
            if(compileStatus[0] == 0) {
                //fail
                GLES20.glDeleteShader(shaderObjId);
                VLog.d(TAG, "shader compile fail");
            }
        }
        return shaderObjId;
    }

    public static int linkProgram(int vertexShaderId, int fragmentShaderId) {
        int programObjectId = GLES20.glCreateProgram();
        if(programObjectId == 0) {
            VLog.d(TAG, "could not create new program");
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

    public static int createProgram(String vertexStr, String fragmentStr) {
        return linkProgram(compileVertexShader(vertexStr), compileFragmentShader(fragmentStr));
    }

    public static boolean isValidateProgram(int programObjectId) {
        GLES20.glValidateProgram(programObjectId);
        int[] validateStatus = new int[1];
        GLES20.glGetProgramiv(programObjectId, GLES20.GL_VALIDATE_STATUS, validateStatus, 0);
        Log.d(TAG, "Results of validating program: " + validateStatus[0] + "\nLog:" + GLES20.glGetProgramInfoLog(programObjectId));
        return validateStatus[0] != 0;
    }

}
