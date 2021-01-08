package com.jk.blefeeder.ble.impl

import com.jk.blefeeder.ble.bean.BLEDev


/**
 *
 *@author abc
 *@time 2020/4/27 11:32
 */
interface IBLEScan {
    fun bleStartScanBefor()
    fun bleScanResult(bleList:MutableList<BLEDev>)
    fun bleScanning(bleDev:BLEDev)
}