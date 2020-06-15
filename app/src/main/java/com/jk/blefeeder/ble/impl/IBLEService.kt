package com.jk.blefeeder.ble.impl

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic

/**
 *蓝牙服务UUID监听
 *@author abc
 *@time 2020/4/26 18:20
 */
interface IBLEService {

    fun bleDiscoverService(gatt: BluetoothGatt?, status: Int)

    fun bleCharacteristicRead(gatt: BluetoothGattCharacteristic?, status: Int)

    fun bleCharacteristicWrite(gatt: BluetoothGattCharacteristic?, status: Int)
}