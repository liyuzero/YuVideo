package com.yu.video.tool

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.github.anrwatchdog.ANRWatchDog
import com.yu.bundles.monitorfragment.MAEMonitorFragment
import com.yu.bundles.monitorfragment.MAEPermissionCallback
import com.yu.lib.video.library.demo.test.CameraFragment
import com.yu.lib.video.library.utils.VLog
import com.yu.lib.video.library.demo.test.GLCameraFragment
import com.yu.lib.video.library.player.DecodeManager
import com.yu.lib.video.library.demo.learn.OpenGLFragment
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ANRWatchDog().start()

        VLog.setDebug(BuildConfig.DEBUG)

        findViewById<View>(R.id.camera).setOnClickListener {
            MAEMonitorFragment.getInstance(this).requestPermissionWithFailDialog(arrayOf(
                android.Manifest.permission.CAMERA
            ), "请打开文件、摄像头权限",
                { dialog, _ ->
                    dialog.dismiss()
                }, object : MAEPermissionCallback {
                    override fun onPermissionApplySuccess() {
                        startFragment(it.context, CameraFragment::class.java)
                    }

                    override fun onPermissionApplyFailure(
                        notGrantedPermissions: MutableList<String>?,
                        shouldShowRequestPermissions: MutableList<Boolean>?
                    ) {

                    }

                })
        }

        findViewById<View>(R.id.player).setOnClickListener {
            MAEMonitorFragment.getInstance(this).requestPermissionWithFailDialog(arrayOf(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ), "请打开文件权限",
                { dialog, _ ->
                    dialog.dismiss()
                }, object : MAEPermissionCallback {
                    override fun onPermissionApplySuccess() {
                        DecodeManager.instance.extractVideo("http://vfx.mtime.cn/Video/2019/02/04/mp4/190204084208765161.mp4")
                    }

                    override fun onPermissionApplyFailure(
                        notGrantedPermissions: MutableList<String>?,
                        shouldShowRequestPermissions: MutableList<Boolean>?
                    ) {

                    }

                })
        }

        openGL.setOnClickListener {
            startFragment(it.context, OpenGLFragment::class.java, null)
        }

        openGLCamera.setOnClickListener {
            MAEMonitorFragment.getInstance(this).requestPermissionWithFailDialog(arrayOf(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.RECORD_AUDIO
            ), "请打开文件、摄像头、录音权限",
                { dialog, _ ->
                    dialog.dismiss()
                }, object : MAEPermissionCallback {
                    override fun onPermissionApplySuccess() {
                        startFragment(it.context, GLCameraFragment::class.java, null)
                    }

                    override fun onPermissionApplyFailure(
                        notGrantedPermissions: MutableList<String>?,
                        shouldShowRequestPermissions: MutableList<Boolean>?
                    ) {

                    }

                })
        }
    }

}