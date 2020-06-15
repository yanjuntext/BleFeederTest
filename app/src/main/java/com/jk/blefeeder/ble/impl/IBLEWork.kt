package com.jk.blefeeder.ble.impl

import com.jk.blefeeder.ble.BLEWorkEnum

/**
 *蓝牙数据接口
 *@author abc
 *@time 2020/4/26 18:19
 */
interface IBLEWork {
    fun bleWork(type: BLEWorkEnum, data: Any?)
}