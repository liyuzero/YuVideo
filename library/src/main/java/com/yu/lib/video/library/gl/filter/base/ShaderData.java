package com.yu.lib.video.library.gl.filter.base;

public class ShaderData {

    public static final String INPUT_VERTEX_SHADER =
            "attribute vec4 aPosition; \n" +
                    "attribute vec4 vTextureCoordinate;\n" +
                    "uniform mat4 uSTMatrix;\n" +
                    "uniform mat4 uMatrix;\n" +
                    "varying vec2 textureCoordinate;\n" +
                    "\n" +
                    "void main() {\n" +
                    "    textureCoordinate = (uSTMatrix * vTextureCoordinate).xy;\n" +
                    "    gl_Position = uMatrix * aPosition;\n" +
                    "}";

    public static final String INPUT_OES_FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 textureCoordinate;\n" +
                    "uniform samplerExternalOES inputTexture;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D(inputTexture, textureCoordinate);\n" +
                    "}";

    public static final String DISPLAY_VERTEX_SHADER =
            "attribute vec4 aPosition; \n" +
                    "attribute vec4 vTextureCoordinate;\n" +
                    "\n" +
                    "varying vec2 textureCoordinate;\n" +
                    "\n" +
                    "void main() {\n" +
                    "    textureCoordinate = vTextureCoordinate.xy;\n" +
                    "    gl_Position = aPosition;\n" +
                    "}";

    public static final String DISPLAY_FRAGMENT_SHADER = "precision mediump float;\n" +
            "varying vec2 textureCoordinate;\n" +
            "uniform sampler2D inputTexture;\n" +
            "\n" +
            "void main() {\n" +
            "    gl_FragColor = texture2D(inputTexture, textureCoordinate);\n" +
            "}";

    //顶点世界坐标
    public static final float[] VERTEX_DATA = new float[]{
            -1.0f, -1.0f,  // 0 bottom left
            1.0f, -1.0f,  // 1 bottom right
            -1.0f, 1.0f,  // 2 top left
            1.0f, 1.0f,  // 3 top right
    };

    //纹理坐标，与上面坐标一致
    public static final float[] TEXTURE_VERTEX_DATA = new float[]{
            0.0f, 0.0f,     // 0 left bottom
            1.0f, 0.0f,     // 1 right bottom
            0.0f, 1.0f,     // 2 left top
            1.0f, 1.0f      // 3 right top
    };

    public static final int VERTEX_PER_GROUP_NUM = 2;

}
