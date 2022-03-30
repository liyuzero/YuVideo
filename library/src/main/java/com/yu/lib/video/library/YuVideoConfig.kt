package com.yu.lib.video.library

class YuVideoConfig {

    companion object {
        val instance: YuVideoConfig by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            YuVideoConfig()
        }
    }

    //是否是背部摄像头
    var mIsOpenBackCamera: Boolean = true
    //是否为录像模式
    var mIsVideoMode: Boolean = true
    //预览数据回调模式，默认为连续模式，oneShot下：获取一帧后将会停止获取预览数据
    var mIsOneShotPreviewMode = false
}