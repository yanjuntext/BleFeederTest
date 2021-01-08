package com.jk.blefeeder

import android.app.Application
import com.tencent.mmkv.MMKV

/**
 * 作者：王颜军 on 2020/12/16 11:03
 * 邮箱：3183424727@qq.com
 */
class App :Application() {
    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)
    }
}