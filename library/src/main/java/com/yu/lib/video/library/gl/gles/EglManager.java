package com.yu.lib.video.library.gl.gles;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;

import com.yu.lib.video.library.utils.VLog;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import static android.opengl.EGL14.EGL_CONTEXT_CLIENT_VERSION;
import static javax.microedition.khronos.egl.EGL10.EGL_NO_CONTEXT;
import static javax.microedition.khronos.egl.EGL10.EGL_NO_SURFACE;

public class EglManager {
    private static final String TAG = "EGLUtils";
    private EGLDisplay mEglDisplay;
    private EGL10 mEgl10;
    private EGLSurface mEglSurface;
    private EGLContext mEglContext;
    private boolean mIsDestroy;

    public boolean createEglEnv(SurfaceTexture renderTexture) {
        return createEglEnv(mEgl10.EGL_NO_CONTEXT, renderTexture);
    }

    public boolean createEglEnv(EGLContext shareContext, SurfaceTexture surfaceTexture){
        //1. 得到Egl实例
        mEgl10 =  (EGL10) EGLContext.getEGL();
        //2. 得到默认的显示设备（就是窗口）
        mEglDisplay = mEgl10.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        if (EGL10.EGL_NO_DISPLAY == mEglDisplay){
            VLog.d(TAG, "egl 不可用:err:" + mEgl10.eglGetError());
            return false;
        }
        //3. 初始化默认显示设备
        int[] version = new int[2];
        if(!mEgl10.eglInitialize(mEglDisplay, version)){
            //error msg
            return false;
        }
        //4. 设置显示设备的属性
        int[] attributes = new int[] {
                EGL10.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,  //指定渲染api类别
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 16,			/*default depth buffer 16 choose a RGB_888 surface */
                EGL10.EGL_STENCIL_SIZE, 0,
                EGL10.EGL_NONE      //总是以EGL10.EGL_NONE结尾
        };

        int chooseNum = 1;
        //5. 从系统中获取对应属性的配置
        EGLConfig[] chooseConfigs = new EGLConfig[chooseNum];
        int[] chooseMaxNum = new int[1];
        mEgl10.eglChooseConfig(mEglDisplay, attributes,  chooseConfigs, chooseNum, chooseMaxNum);
        VLog.d(TAG, "chooseMaxNum:" + chooseMaxNum[0]);

        //6. 创建EglContext
        int[] contextAttr = new int[]{
                EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL10.EGL_NONE
        };

        //7. 创建渲染的Surface
        mEglSurface = mEgl10.eglCreateWindowSurface(mEglDisplay, chooseConfigs[0], surfaceTexture, null);
        if(mEglSurface == EGL_NO_SURFACE){
            VLog.d(TAG, "create surface failed,msg:" + mEgl10.eglGetError());
            return false;
        }

        mEglContext = mEgl10.eglCreateContext(mEglDisplay, chooseConfigs[0], shareContext, contextAttr);
        if(mEglContext == EGL_NO_CONTEXT){
            VLog.d(TAG, "create context failed,msg:" + mEgl10.eglGetError());
            return false;
        }

        //8. 绑定EglContext和Surface到显示设备中
        if(!mEgl10.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)){
            VLog.d(TAG, "eglMakeCurrent ailed, msg:" + mEgl10.eglGetError());
            return false;
        }
        return true;
    }

    //9. 刷新数据，显示渲染场景
    public  void swapBuffer(){
        mEgl10.eglSwapBuffers(mEglDisplay, mEglSurface);
    }

    public void destroy(){
        if(!mIsDestroy) {
            mEgl10.eglMakeCurrent(mEglDisplay, EGL_NO_SURFACE,
                    EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
            mEgl10.eglDestroySurface(mEglDisplay, mEglSurface);
            mEgl10.eglDestroyContext(mEglDisplay, mEglContext);
            mEgl10.eglTerminate(mEglDisplay);
        }
        mIsDestroy = true;
    }
}
