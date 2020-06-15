package com.jk.blefeeder.ble.ota

/**
 *
 *@author abc
 *@time 2020/5/11 15:15
 */
object SYDOTAIO {

    private const val OTA_STAT: Byte = 0x16
    private const val OTA_WRITE_SENTION_STAR: Byte = 0x14
    private const val OTA_WTITE_LAST_SENTION: Byte = 0x15

    fun getStartOta(): ByteArray {
        val data = ByteArray(2)
        data[0] = OTA_STAT
        data[1] = 0
        return data
    }

    fun getWriteSectionStart(check: Int, size: Int, address: Int): ByteArray {
        val data = ByteArray(10)
        data[0] = OTA_WRITE_SENTION_STAR
        data[1] = 0x13
        data[2] = (address and 0x000000FF).toByte()
        data[3] = (address and 0x0000FF00 shr 8).toByte()
        data[4] = (address and 0x00FF0000 shr 16).toByte()
        data[5] = (address and -0x1000000 shr 24).toByte()
        data[6] = (size and 0x000000FF).toByte()
        data[7] = (size and 0x0000FF00 shr 8).toByte()
        data[8] = (check and 0x000000FF).toByte()
        data[9] = (check and 0x0000FF00 shr 8).toByte()
        return data
    }

    fun getSendLastOta(size: Int, crc: Int): ByteArray {
        val data = ByteArray(8)
        data[0] = OTA_WTITE_LAST_SENTION
        data[1] = 0x04
        data[2] = (size and 0x000000FF).toByte()
        data[3] = ((size and 0x0000FF00) shr 8).toByte()
        data[4] = ((size and 0x00FF0000) shr 16).toByte()
        data[5] = ((size and -0x1000000) shr 24).toByte()
        data[6] = (crc and 0x000000FF).toByte()
        data[7] = ((crc and 0x0000FF00) shr 8).toByte()
        return data
    }
}