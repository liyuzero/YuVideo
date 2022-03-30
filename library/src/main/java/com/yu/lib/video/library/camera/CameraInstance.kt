@file:Suppress("DEPRECATION")

package com.yu.lib.video.library.camera

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.ImageFormat
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.view.Surface
import android.view.SurfaceHolder
import com.yu.lib.video.library.YuVideoConfig

class CameraInstance {
    private var mIsBackCamera = true
    var mCamera: Camera? = null
    var mCameraId: Int = -1
    var mPreviewCallBack: OnCameraPreviewCallBack? = null

    private var mIsCamera2: Boolean? = null
    private var mIsInitSuccess = false

    companion object {
        val instance: CameraInstance by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            CameraInstance()
        }
    }

    @Synchronized
    private fun setCameraParameters(cameraBestSize: Point?) {
        if (cameraBestSize == null || mCamera == null) {
            return
        }
        val parameters = mCamera!!.parameters

        //对焦模式
        val focusMode = CameraPreviewUtils.findSettableValue(
            parameters.supportedFocusModes,
            if (YuVideoConfig.instance.mIsVideoMode) Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
            else Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
        )

        if (focusMode != null) {
            parameters.focusMode = focusMode
        }

        //设置特效
        val colorMode = CameraPreviewUtils.findSettableValue(
            parameters.supportedColorEffects,
            Camera.Parameters.EFFECT_NEGATIVE
        )
        if (colorMode != null) {
            parameters.colorEffect = colorMode
        }

        //设置预览大小
        parameters.setPreviewSize(cameraBestSize.x, cameraBestSize.y)

        //设置输出格式
        parameters.previewFormat = ImageFormat.NV21
        parameters.setRecordingHint(true)
        //设置帧数
        /*
        val range = parameters.supportedPreviewFpsRange
        var min = Int.MAX_VALUE
        var max = Int.MIN_VALUE
        for (j in range.indices) {
            val r = range[j]
            for (k in r.indices) {
                min = min(min, r[k]) / 1000
                max = max(max, r[k]) / 1000
            }
        }
        parameters.setPreviewFpsRange()
        */

        mCamera!!.parameters = parameters
    }

    //设置预览方向
    @Synchronized
    private fun setCameraDisplayOrientation(activity: Activity) {
        val orientation = activity.resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return
        }

        val info = Camera.CameraInfo()
        Camera.getCameraInfo(mCameraId, info)
        var degrees = 0
        when (activity.windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }

        var result: Int
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360
            result = (360 - result) % 360
        } else {
            result = (info.orientation - degrees + 360) % 360
        }
        mCamera?.setDisplayOrientation(result)
    }

    @Synchronized
    private fun openBackCamera() {
        if (mCamera != null && mCameraId != -1 && mIsBackCamera) {
            return
        } else if (mCamera != null) {
            mCamera!!.stopPreview()
            mCamera!!.release()
            mCamera = null
        }
        openCamera(Camera.CameraInfo.CAMERA_FACING_BACK)
    }

    @Synchronized
    private fun openFrontCamera() {
        if (mCamera != null && mCameraId != -1 && !mIsBackCamera) {
            return
        } else if (mCamera != null) {
            mCamera!!.stopPreview()
            mCamera!!.release()
            mCamera = null
        }
        return openCamera(Camera.CameraInfo.CAMERA_FACING_FRONT)
    }

    private fun openCamera(cameraId: Int) {
        val numCameras = Camera.getNumberOfCameras()
        if (numCameras == 0) {
            // "No cameras!"
            return
        }

        var index = 0
        while (index < numCameras) {
            val cameraInfo = Camera.CameraInfo()
            Camera.getCameraInfo(index, cameraInfo)
            // CAMERA_FACING_BACK：手机背面的摄像头
            if (cameraInfo.facing == cameraId) {
                break
            }
            index++
        }

        val camera: Camera = if (index < numCameras) {
            Camera.open(index)
        } else {
            Camera.open(0)
        }

        mCamera = camera
        mIsBackCamera = cameraId == Camera.CameraInfo.CAMERA_FACING_BACK
        mCameraId = cameraId
    }

    private var mPreviewByteBuffer: ByteArray? = null

    private fun isCameraAvailable(): Boolean {
        return mCamera != null && mCameraId != -1
    }

    private fun isActivityAvailable(activity: Activity?): Boolean {
        if (activity == null || activity.isFinishing) {
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed) {
            return false;
        }
        return true
    }

    private fun isSupportCamera2(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)
                val deviceLevel =
                    characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                if (deviceLevel != CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL) {
                    return false
                }
            }
        } else {
            return false
        }
        return true
    }

    @Synchronized
    fun startPreview() {
        if (mIsInitSuccess) {
            mCamera?.startPreview()
        }
    }

    @Synchronized
    fun stopPreview() {
        mCamera?.stopPreview()
    }

    @Synchronized
    fun release() {
        mCamera?.stopPreview()
        mCamera?.release()
        mCamera = null
        mCameraId = -1
        mPreviewCallBack?.releaseComplete()
    }

    @Synchronized
    fun openAndInitCamera(
        context: Activity,
        texture: SurfaceTexture,
        isSetPreviewCallback: Boolean,
        onCameraPreviewCallBack: OnCameraPreviewCallBack
    ) {
        openAndInitCamera(context, null, texture, isSetPreviewCallback, onCameraPreviewCallBack)
    }

    @Synchronized
    fun openAndInitCamera(
        context: Activity,
        holder: SurfaceHolder,
        isSetPreviewCallback: Boolean,
        onCameraPreviewCallBack: OnCameraPreviewCallBack
    ) {
        openAndInitCamera(context, holder, null, isSetPreviewCallback, onCameraPreviewCallBack)
    }

    @Synchronized
    private fun openAndInitCamera(
        context: Activity,
        holder: SurfaceHolder?,
        texture: SurfaceTexture?,
        isSetPreviewCallback: Boolean,
        onCameraPreviewCallBack: OnCameraPreviewCallBack
    ) {
        mPreviewCallBack = onCameraPreviewCallBack
        if (mIsCamera2 == null) {
            mIsCamera2 = isSupportCamera2(context)
        }
        if (mIsCamera2!!) {
            // camera2
            openAndInitCamera1(
                context,
                holder,
                texture,
                isSetPreviewCallback,
                onCameraPreviewCallBack
            )
        } else {
            openAndInitCamera1(
                context,
                holder,
                texture,
                isSetPreviewCallback,
                onCameraPreviewCallBack
            )
        }
    }

    private fun openAndInitCamera1(
        context: Activity?,
        holder: SurfaceHolder?,
        texture: SurfaceTexture?,
        isSetPreviewCallback: Boolean,
        onCameraPreviewCallBack: OnCameraPreviewCallBack
    ) {
        if (YuVideoConfig.instance.mIsOpenBackCamera) openBackCamera() else openFrontCamera()
        if (!isCameraAvailable() && !isActivityAvailable(context)) {
            return
        }
        val cameraBestSize = CameraPreviewUtils.findCameraBestPreviewSize(context!!, mCamera)

        if (cameraBestSize != null) {

            val previewSurfaceSuggestSize =
                CameraPreviewUtils.findScreenBestPreviewSize(context, cameraBestSize)
            if (previewSurfaceSuggestSize != null) {
                onCameraPreviewCallBack.onPreviewSuggestSizeCallBack(
                    previewSurfaceSuggestSize.x,
                    previewSurfaceSuggestSize.y
                )
            }

            //配置参数
            setCameraParameters(cameraBestSize)
            //配置预览方向
            setCameraDisplayOrientation(context)
            //设置预览holder
            if (texture != null) {
                mCamera!!.setPreviewTexture(texture)
            } else if (holder != null) {
                mCamera!!.setPreviewDisplay(holder)
            }
            //设置预览回调
            if (YuVideoConfig.instance.mIsOneShotPreviewMode && isSetPreviewCallback) {
                mCamera!!.setOneShotPreviewCallback { data, _ ->
                    onPreViewCallBack(
                        context,
                        data,
                        cameraBestSize.x,
                        cameraBestSize.y,
                        onCameraPreviewCallBack
                    )
                }
            } else if (isSetPreviewCallback) {
                if (mPreviewByteBuffer == null || mPreviewByteBuffer!!.size != cameraBestSize.x * cameraBestSize.y * 3 / 2) {
                    mPreviewByteBuffer = ByteArray(cameraBestSize.x * cameraBestSize.y * 3 / 2)
                }
                mCamera!!.addCallbackBuffer(mPreviewByteBuffer)
                mCamera!!.setPreviewCallbackWithBuffer { data, _ ->
                    onPreViewCallBack(
                        context,
                        data,
                        cameraBestSize.x,
                        cameraBestSize.y,
                        onCameraPreviewCallBack
                    )
                    mCamera?.addCallbackBuffer(mPreviewByteBuffer)
                }
            }
            mIsInitSuccess = true
        }
    }

    private fun onPreViewCallBack(
        context: Activity?,
        data: ByteArray?,
        w: Int,
        h: Int,
        onCameraPreviewCallBack: OnCameraPreviewCallBack
    ) {
        if (!isActivityAvailable(context) || !isCameraAvailable()) {
            return
        }

        var width = w
        var height = h
        var resultData = data
        if (context!!.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            resultData = rotateYuv420SP(data, width, height)
            if (resultData != null) {
                val temp = width
                width = height
                height = temp
            }
        }

        if (resultData == null) {
            return
        }

        onCameraPreviewCallBack.onPreviewFrame(resultData, mCamera!!, width, height)
    }

    private fun rotateYuv420SP(data: ByteArray?, width: Int, height: Int): ByteArray? {
        if (data == null) {
            return null
        }

        //YUV420sp（nv21格式：V 前 U 后）Y和UV的数量比为：2：1
        if (data.size != width * height * 3 / 2) {
            return null
        }

        //竖屏下需要旋转
        var k = 0
        val rotatedData = ByteArray(data.size)
        //翻转 Y
        for (j in 0 until width) {
            for (i in (height - 1) downTo 0) {
                rotatedData[k++] = data[i * width + j]
            }
        }

        val uvWidth = width
        val uvHeight = height / 2

        //翻转 UV
        for (j in 0 until uvWidth step 2) {
            for (i in (uvHeight + height - 1) downTo (height)) {
                rotatedData[k++] = data[i * uvWidth + j]
                rotatedData[k++] = data[i * uvWidth + j + 1]
            }
        }

        return rotatedData
    }
}