package com.jk.blefeeder.ble.impl

import com.jk.blefeeder.ble.BLEStatusEnum


/**
 *蓝牙状态 接口
 *@author abc
 *@time 2020/4/26 18:11
 */
interface IBLEStatus {
    fun bleStatus(status: BLEStatusEnum)
}