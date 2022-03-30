package com.yu.lib.video.library.camera

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper

class CameraThreadPool {

    companion object {
        val INSTANCE: CameraThreadPool by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            CameraThreadPool()
        }
    }

    //主线程
    private val mMainHandler = Handler(Looper.getMainLooper())

    //camera预览相关线程
    private val mCameraThread = HandlerThread("camera")
    private var mCameraHandler: Handler

    init {
        mCameraThread.start()
        mCameraHandler = Handler(mCameraThread.looper)
    }

    fun postInCameraThread(runnable: Runnable) {
        mCameraHandler.post(runnable)
    }

    fun postInMainThread(runnable: Runnable) {
        mMainHandler.post(runnable)
    }

}