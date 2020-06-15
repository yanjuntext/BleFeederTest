package com.jk.blefeeder.ble.ota

import java.io.File
import java.io.FileInputStream

/**
 *
 *@author abc
 *@time 2020/5/11 15:09
 */
object SYDOTAUtlis {

    fun getFileDatas(filePath: String): ByteArray? {
        return FileInputStream(File(filePath)).use {
            val length = it.available()
            val data = ByteArray(length)
            it.read(data)
            data
        }
    }

    fun getCrc(data: ByteArray?): Int {
        var crc = 0
        data?.let {
            data.forEach {
                var cc = it.toInt()
                cc = cc and 0x000000FF
                crc += cc
                crc = crc and 0x0000FFFF
//               crc =  ((it.toInt() and 0x000000FF) + crc) and crc
            }
        }
        return crc
    }

}