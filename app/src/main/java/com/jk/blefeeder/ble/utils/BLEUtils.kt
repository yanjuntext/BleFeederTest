package com.jk.blefeeder.ble.utils

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent

/**
 *
 *@author abc
 *@time 2020/4/27 11:14
 */
object BLEUtils {

    private fun getBluetoothManager(context: Context) =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private fun getBluetoothAdapter(context: Context) = getBluetoothManager(context).adapter

    /**是否支持蓝牙*/
    fun bluetoothEnable(context: Context) = getBluetoothAdapter(context).isEnabled

    /**打开蓝牙*/
    fun openBluetooth(context: Activity, requestCode: Int) {
        if (!bluetoothEnable(context)) {
            context.startActivityForResult(
                Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                requestCode
            )
        }
    }

    fun openBluetooth(context: Activity) {
        if (!bluetoothEnable(context)) {
            getBluetoothAdapter(context).enable()
        }
    }


}