package com.jk.blefeeder.ble

import com.jk.blefeeder.ble.bean.BleDevTypeEnum

/**
 *
 *@author abc
 *@time 2020/5/6 10:27
 */
object BLEName {

    private const val BLE_FEEDER = "AF2B"
    private const val BLE_BONE = "Skymee"

    fun getBleName(bleType: BleDevTypeEnum) = if (bleType == BleDevTypeEnum.bone) BLE_BONE else BLE_FEEDER

}