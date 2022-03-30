@file:Suppress("DEPRECATION")

package com.yu.lib.video.library.camera

import android.hardware.Camera

interface OnCameraPreviewCallBack {
    fun onPreviewSuggestSizeCallBack(width: Int, height: Int)
    fun onPreviewFrame(data: ByteArray, camera: Camera, width: Int, height: Int)
    fun releaseComplete()
}