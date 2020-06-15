package com.jk.blefeeder.ble.bean

import android.bluetooth.BluetoothDevice

/**
 *
 *@author abc
 *@time 2020/4/27 9:45
 */
data class BLEDev(var bluetoothDevice: BluetoothDevice, var rssi: Int)