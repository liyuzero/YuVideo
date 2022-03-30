package com.yu.lib.video.library.demo.learn

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.yu.lib.video.library.R
import kotlinx.android.synthetic.main.yu_video_fragment_open_gl.*

/**
*
* */
class OpenGLFragment: Fragment() {

    @SuppressLint("InflateParams")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.yu_video_fragment_open_gl, null)
        loadView(view)
        return view
    }

    private fun loadView(view: View) {
        val activityManager = activity?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val configurationInfo = activityManager.deviceConfigurationInfo
        if(configurationInfo.reqGlEsVersion < 0x20000) {
            Toast.makeText(view.context, "Not support OpenGL 2.0!! ", Toast.LENGTH_SHORT).show()
            return
        }
        val glSurfaceView = view.findViewById<GLSurfaceView>(R.id.glSurfaceView)
        glSurfaceView.setEGLContextClientVersion(2)
        //添加渲染表面
        glSurfaceView.setRenderer(
            GL1Render(
                glSurfaceView.context
            )
        )

        //activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }
}