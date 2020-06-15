package com.jk.blefeeder.ble.impl

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import com.jk.blefeeder.ble.BLEStatusEnum

/**
 *
 *@author abc
 *@time 2020/4/27 17:22
 */
abstract class IBLEGattCallback {

    abstract fun onBleStatus(status: BLEStatusEnum)

    open fun onBleServiceDiscover(gatt: BluetoothGatt?, status: Int) {}

    open fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {}

    open fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {}

    abstract fun onCharacteristicChange(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?)

    open fun onNotifyWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {}

    open fun onReadRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {}

    open fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {}
}