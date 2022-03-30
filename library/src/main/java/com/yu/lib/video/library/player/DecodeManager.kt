package com.yu.lib.video.library.player

import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import java.lang.Exception

/**
* 编辑已有视频
* */
class DecodeManager {

    companion object {
        val instance: DecodeManager by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            DecodeManager()
        }
    }

    private val mDecodeThread = HandlerThread("player_decode")
    private var mDecodeHandler: Handler

    init {
        mDecodeThread.start()
        mDecodeHandler = Handler(mDecodeThread.looper)
    }

    //提取视频信息
    fun extractVideo(videoSource: String) {
        mDecodeHandler.post {
            var extractor = MediaExtractor()
            try {
                extractor.setDataSource(videoSource)
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }


            extractor = MediaExtractor()
            try {
                extractor.setDataSource("http://vfx.mtime.cn/Video/2019/03/21/mp4/190321153853126488.mp4")
                for (i in 0 until extractor.trackCount) {

                }
            } catch (e: Exception) {

            }
        }
    }

}