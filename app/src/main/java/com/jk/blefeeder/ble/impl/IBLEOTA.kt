package com.jk.blefeeder.ble.impl

/**
 *
 *@author abc
 *@time 2020/5/11 15:25
 */
interface IBLEOTA {
    /**开启OTA*/
    fun bleOTAStart(complete: Boolean)

    /**写入第一个包*/
    fun writeSectionStart(complete: Boolean)

    fun bleOtaProgress(progress: Int)

    fun bleOtaSending(complete: Boolean)

    /**升级结果*/
    fun bleOtaResult(result: Boolean)
}