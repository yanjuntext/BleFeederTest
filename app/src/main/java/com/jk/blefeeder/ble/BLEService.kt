package com.jk.blefeeder.ble

import android.app.Service
import android.bluetooth.*
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.jk.blefeeder.ble.bean.BleDevTypeEnum
import com.jk.blefeeder.ble.impl.IBLEGattCallback
import com.jk.blefeeder.ble.impl.IBLEOTA
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class BLEService : Service() {

    private val mBind by lazy { BLEBind() }

    private val mGattHelper by lazy {
        BLEGattHelper(this)
    }

    override fun onBind(intent: Intent): IBinder = mBind


    fun setBleAdapter(bleAdapter: BluetoothAdapter): BLEService {
        mGattHelper.setBleAdapter(bleAdapter)
        return this
    }

    fun setBleDevType(type: BleDevTypeEnum): BLEService {
        mGattHelper.setBleDevType(type)
        return this
    }

    fun setBleOtaListener(listener: IBLEOTA): BLEService {
        mGattHelper.setBleOtaListener(listener)
        return this
    }

    fun setBleGattCallback(bleGattCallback: IBLEGattCallback?): BLEService {
        mGattHelper.setBleGattCallback(bleGattCallback)
        return this
    }

    fun connect(bleAddress: String,reconnectCount:Int = 3) = mGattHelper.connect(bleAddress,reconnectCount)

    fun sendData(data: ByteArray) = mGattHelper.writeCharacteristic(data)

    fun setMtu(mtu: Int) = mGattHelper.setMtu(mtu)

    fun getRssi() = mGattHelper.readRssi()

    fun startOta(file:String?) = mGattHelper.startOta(file)
    fun closeOta() = mGattHelper.closeOta()

    fun getBattery() = mGattHelper.readCharacteristic(BLEGattHelper.CharacteristicEnum.battery)

    fun disconnect() = mGattHelper.disconnect()

    override fun onUnbind(intent: Intent?): Boolean {
        mGattHelper.close()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        mGattHelper.close()
        super.onDestroy()
    }

    inner class BLEBind : Binder() {
        fun getService() = this@BLEService
    }

}
