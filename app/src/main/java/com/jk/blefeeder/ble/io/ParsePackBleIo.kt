package com.jk.blefeeder.ble.io

/**
 * 作者：王颜军 on 2020/9/25 16:02
 * 邮箱：3183424727@qq.com
 */
object ParsePackBleIo {

    fun getBleDataType(data: ByteArray?): Int {
        if (data == null || data.isEmpty()|| getCrc(data) != data[data.size-1].toInt()) {
            return -1
        }
        return data[2].toInt()
    }

}