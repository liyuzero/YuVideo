package com.yu.lib.video.library.camera

import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.hardware.Camera
import android.view.WindowManager
import kotlin.math.abs

object CameraPreviewUtils {

    fun findCameraBestPreviewSize(context: Context, camera: Camera?): Point? {
        if(camera == null) {
            return null
        }
        val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = manager.defaultDisplay

        val point = Point()
        try {
            display.getSize(point)
        } catch (ignore: NoSuchMethodError) {
            point.x = display.width
            point.y = display.height
        }

        val orientation = context.resources.configuration.orientation
        var width = 0
        var height = 0
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            //竖屏情况
            width = point.y
            height = point.x
        } else {
            //横屏情况
            width = point.x
            height = point.y
        }

        val previewSizes = camera.parameters.supportedPreviewSizes
        for (size in previewSizes) {
            if (size.width == width && size.height == height) {
                return Point(width, height)
            }
        }

        val reqRatio = width.toFloat() / height
        //找到 摄像头预览比例 和 屏幕比例最接近的 预览尺寸
        var min = java.lang.Float.MAX_VALUE
        var retSize: Camera.Size? = null
        for (size in previewSizes) {
            val deltaRatio = abs(reqRatio - size.width.toFloat() / size.height)
            if (deltaRatio < min) {
                min = deltaRatio
                retSize = size
            }
        }
        return if (retSize == null) null else Point(retSize.width, retSize.height)
    }

    fun findScreenBestPreviewSize(context: Context, bestCameraPreviewSize: Point): Point? {

        //切换竖屏宽高
        val cameraWidth = bestCameraPreviewSize.y
        val cameraHeight = bestCameraPreviewSize.x

        val screenP = Point()
        val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = manager.defaultDisplay
        try {
            display.getSize(screenP)
        } catch (ignore: NoSuchMethodError) { // Older device
            screenP.x = display.width
            screenP.y = display.height
        }

        //屏幕 竖屏下宽高
        var screenWidth = 0
        var screenHeight = 0
        if(context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            //竖屏情况
            screenWidth = screenP.x
            screenHeight = screenP.y
        } else {
            //横屏情况
            screenWidth = screenP.y
            screenHeight = screenP.x
        }

        val cameraRate = cameraWidth.toFloat() / cameraHeight.toFloat()
        val isCameraBig = cameraRate - screenWidth.toFloat() / screenHeight.toFloat() > 0

        val out = Point()
        if(cameraWidth == screenWidth && cameraHeight == screenHeight) {
            out.x = screenWidth
            out.y = screenHeight
        } else {
            out.x = if(isCameraBig) (cameraRate * screenHeight).toInt() else screenWidth
            out.y = if(isCameraBig) screenHeight else (screenWidth / cameraRate).toInt()
        }
        return out
    }

    fun findSettableValue(
        supportedValues: Collection<String>?,
        vararg desiredValues: String
    ): String? {
        var result: String? = null
        if (supportedValues != null) {
            for (desiredValue in desiredValues) {
                if (supportedValues.contains(desiredValue)) {
                    result = desiredValue
                    break
                }
            }
        }
        //Logger.i(TAG, "Settable value: " + result);
        return result
    }

}