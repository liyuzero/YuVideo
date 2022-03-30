package com.yu.lib.video.library.demo.learn;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.yu.lib.video.library.BuildConfig;
import com.yu.lib.video.library.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_LINES;
import static android.opengl.GLES20.GL_LINE_STRIP;
import static android.opengl.GLES20.GL_POINTS;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glLineWidth;
import static android.opengl.GLES20.glUniform4f;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

public class GL1Render implements GLSurfaceView.Renderer {

    private static final String TAG = "GLRender";

    private static final int POSITION_COMPONENT_COUNT = 2;
    private float[] tableVertices = {
            -0.5f, -0.8f,
            0.5f, 0.8f,
            -0.5f, 0.8f,

            -0.5f, -0.8f,
            0.5f, -0.8f,
            0.5f, 0.8f,

            -0.5f, 0f,
            0.5f, 0f,

            0f, -0.4f,
            0f, 0.4f,

            0f, 0f,

            //边框
            -0.5f, -0.8f,
            -0.5f, 0.8f,

            -0.5f, 0.8f,
            0.5f, 0.8f,

            0.5f, 0.8f,
            0.5f, -0.8f,

            0.5f, -0.8f,
            -0.5f, -0.8f
    };

    private FloatBuffer vertexData = getVertexData();

    FloatBuffer getVertexData() {
        //开辟空间
        FloatBuffer vertexData = ByteBuffer
                .allocateDirect(tableVertices.length * RenderParam.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexData.put(tableVertices);
        return vertexData;
    }

    private static final String U_COLOR = "u_Color";
    private int uColorLocation;

    private static final String A_POSITION = "a_Position";
    private int aPositionLocation;

    private static final String U_MATRIX = "u_Matrix";
    private float[] projectionMatrix = new float[16];
    private int uMatrixLocation;

    private Context mContext;

    public GL1Render(Context mContext) {
        this.mContext = mContext;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        glClearColor(0.0f, 0f, 0f, 1f);

        int programId = ShaderHelper.createProgram(ResourceHelper.getRawString(mContext.getResources().openRawResource(R.raw.test_vertex_shader)),
                ResourceHelper.getRawString(mContext.getResources().openRawResource(R.raw.test_fragment_shader)));
        if(BuildConfig.DEBUG) {
            ShaderHelper.isValidateProgram(programId);
        }
        glUseProgram(programId);

        uColorLocation = glGetUniformLocation(programId, U_COLOR);

        aPositionLocation = glGetAttribLocation(programId, A_POSITION);
        vertexData.position(0);
        glVertexAttribPointer(aPositionLocation, POSITION_COMPONENT_COUNT, GL_FLOAT,
                false, 0, vertexData);
        glEnableVertexAttribArray(aPositionLocation);

        uMatrixLocation = glGetUniformLocation(programId, U_MATRIX);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        glViewport(0, 0, width, height);
        float aspectRatio = width > height? (float) width / (float) height: (float) height / (float) width;

        if(width > height) {
            //横屏
            Matrix.orthoM(projectionMatrix, 0, -aspectRatio, aspectRatio, -1f, 1f, -1f, 1f);
        } else {
            //竖屏
            Matrix.orthoM(projectionMatrix, 0, -1f, 1f, -aspectRatio, aspectRatio, -1f, 1f);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        glClear(GL_COLOR_BUFFER_BIT);

        glUniformMatrix4fv(uMatrixLocation, 1, false, projectionMatrix, 0);

        //设置颜色rbg a
        glUniform4f(uColorLocation, 1.0f, 1.0f, 1.0f, 1.0f);
        //开始绘制
        glDrawArrays(GL_TRIANGLES, 0, 6);

        glLineWidth(4.0f);
        glUniform4f(uColorLocation, 1.0f, 0.0f, 0.0f, 1.0f);
        glDrawArrays(GL_LINES, 6, 2);

        glUniform4f(uColorLocation, 0f, 0f, 1.0f, 1f);
        glDrawArrays(GL_POINTS, 8, 1);

        glUniform4f(uColorLocation, 1f, 0f, 0f, 1f);
        glDrawArrays(GL_POINTS, 9, 1);

        glUniform4f(uColorLocation, 0f, 1f, 0f, 1f);
        glDrawArrays(GL_POINTS, 10, 1);

        glUniform4f(uColorLocation, 1f, 0f, 0f, 1f);
        glLineWidth(5.0f);
        glDrawArrays(GL_LINE_STRIP, 11, 8);
    }
}
