@file:Suppress("DEPRECATION")

package com.yu.lib.video.library.demo.test

import android.annotation.SuppressLint
import android.graphics.*
import android.hardware.Camera
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.yu.lib.video.library.R
import com.yu.lib.video.library.camera.OnCameraPreviewCallBack
import com.yu.lib.video.library.camera.YuCameraManager
import java.io.ByteArrayOutputStream


class CameraFragment : Fragment() {
    private lateinit var mSurfaceView: SurfaceView
    private var mAvailableHolder: SurfaceHolder? = null

    @SuppressLint("InflateParams")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.yu_video_fragment_camera, null)
        loadView(view)
        return view
    }

    private fun loadView(view: View) {
        mSurfaceView = view.findViewById(R.id.surfaceView)
        mSurfaceView.holder.addCallback(HolderCallBack())
        startPreview()

        YuCameraManager.instance.setPreviewCallBack(object : OnCameraPreviewCallBack {
            override fun onPreviewSuggestSizeCallBack(width: Int, height: Int) {
                mSurfaceView.layoutParams = FrameLayout.LayoutParams(width, height)
            }

            override fun onPreviewFrame(
                data: ByteArray,
                camera: Camera,
                width: Int,
                height: Int
            ) {
                /*

                测试旋转是否正常的代码

                */

                val yuvImage = YuvImage(data, ImageFormat.NV21, width, height, null)
                val os = ByteArrayOutputStream()
                yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, os)
                val jpegByteArray = os.toByteArray()

                val bitmap = BitmapFactory.decodeByteArray(jpegByteArray, 0, jpegByteArray.size)
                os.flush()
                os.close()

                val imageView = view.findViewById<ImageView>(R.id.image)
                imageView.setImageBitmap(bitmap)

                
            }

            override fun releaseComplete() {
                //
            }
        })
    }

    private fun startPreview() {
        if (mAvailableHolder != null) {
            YuCameraManager.instance.startPreView()
        }
    }

    private fun stopPreview() {
        if (mAvailableHolder != null) {
            YuCameraManager.instance.stopPreView()
        }
    }

    override fun onResume() {
        super.onResume()

        startPreview()
    }

    override fun onPause() {
        super.onPause()

        stopPreview()
    }

    private inner class HolderCallBack : SurfaceHolder.Callback {

        override fun surfaceCreated(holder: SurfaceHolder?) {
            mAvailableHolder = holder
            onSurfaceCreated()
        }

        override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {}

        override fun surfaceDestroyed(holder: SurfaceHolder?) {
            mAvailableHolder = null
            stopPreview()
        }

    }

    private fun onSurfaceCreated() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity!!.isDestroyed) {
            return
        }
        if (activity == null || activity!!.isFinishing || mAvailableHolder == null) {
            return
        }
        YuCameraManager.instance.openCamera(activity!!, mAvailableHolder!!)
        startPreview()
    }
}