@file:Suppress("DEPRECATION")

package com.yu.lib.video.library.camera

import android.app.Activity
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.view.SurfaceHolder

class YuCameraManager {

    companion object {
        val instance: YuCameraManager by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            YuCameraManager()
        }
    }

    private var mCameraPreviewCallBack: OnCameraPreviewCallBack? = null

    fun setPreviewCallBack(callback: OnCameraPreviewCallBack) {
        mCameraPreviewCallBack = callback
    }

    fun executeRunInCameraThread(runnable: Runnable) {
        CameraThreadPool.INSTANCE.postInCameraThread(runnable)
    }

    fun openCamera(context: Activity, textureView: SurfaceTexture) {
        openCamera(context, textureView, true)
    }

    fun openCamera(context: Activity, textureView: SurfaceTexture, isSetPreviewFrameCallback: Boolean) {
        CameraThreadPool.INSTANCE.postInCameraThread(Runnable {
            CameraInstance.instance.openAndInitCamera(
                context,
                textureView,
                isSetPreviewFrameCallback,
                object : OnCameraPreviewCallBack {
                    override fun onPreviewSuggestSizeCallBack(width: Int, height: Int) {
                        CameraThreadPool.INSTANCE.postInMainThread(Runnable {
                            mCameraPreviewCallBack?.onPreviewSuggestSizeCallBack(width, height)
                        })
                    }

                    override fun onPreviewFrame(
                        data: ByteArray,
                        camera: Camera,
                        width: Int,
                        height: Int
                    ) {
                        CameraThreadPool.INSTANCE.postInMainThread(Runnable {
                            mCameraPreviewCallBack?.onPreviewFrame(data, camera, width, height)
                        })
                    }

                    override fun releaseComplete() {
                        CameraThreadPool.INSTANCE.postInMainThread(Runnable {
                            mCameraPreviewCallBack?.releaseComplete()
                        })
                    }

                })
        })
    }

    fun openCamera(context: Activity, holder: SurfaceHolder) {
        openCamera(context, holder, true)
    }

    fun openCamera(context: Activity, holder: SurfaceHolder, isSetPreviewFrameCallback: Boolean) {
        CameraThreadPool.INSTANCE.postInCameraThread(Runnable {
            CameraInstance.instance.openAndInitCamera(
                context,
                holder,
                isSetPreviewFrameCallback,
                object : OnCameraPreviewCallBack {
                    override fun onPreviewSuggestSizeCallBack(width: Int, height: Int) {
                        CameraThreadPool.INSTANCE.postInMainThread(Runnable {
                            mCameraPreviewCallBack?.onPreviewSuggestSizeCallBack(width, height)
                        })
                    }

                    override fun onPreviewFrame(
                        data: ByteArray,
                        camera: Camera,
                        width: Int,
                        height: Int
                    ) {
                        CameraThreadPool.INSTANCE.postInMainThread(Runnable {
                            mCameraPreviewCallBack?.onPreviewFrame(data, camera, width, height)
                        })
                    }

                    override fun releaseComplete() {
                        CameraThreadPool.INSTANCE.postInMainThread(Runnable {
                            mCameraPreviewCallBack?.releaseComplete()
                        })
                    }

                })
        })
    }

    fun startPreView() {
        CameraThreadPool.INSTANCE.postInCameraThread(Runnable {
            CameraInstance.instance.startPreview()
        })
    }

    fun stopPreView() {
        CameraThreadPool.INSTANCE.postInCameraThread(Runnable {
            CameraInstance.instance.stopPreview()
        })
    }

    fun release() {
        CameraThreadPool.INSTANCE.postInCameraThread(Runnable {
            CameraInstance.instance.release()
        })
    }

}