package com.jk.blefeeder.ble

import android.bluetooth.*
import android.content.Context
import android.os.Build
import android.util.Log
import com.jk.blefeeder.ble.bean.BleDevTypeEnum
import com.jk.blefeeder.ble.impl.IBLEGattCallback
import com.jk.blefeeder.ble.impl.IBLEOTA
import com.jk.blefeeder.ble.ota.SYDBLEOtaHelper
import com.jk.blefeeder.ble.io.ParseBLEIO
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.lang.ref.WeakReference


/**
 *
 *@author abc
 *@time 2020/4/27 16:04
 */
@ExperimentalCoroutinesApi
class BLEGattHelper(context: Context) : BluetoothGattCallback() {
    enum class CharacteristicEnum {
        tx, rx, battery
    }


    private val TAG = BLEGattHelper::class.java.simpleName
    private var context: WeakReference<Context> = WeakReference(context)
    private var mGatt: BluetoothGatt? = null
    private var mBleAdapter: WeakReference<BluetoothAdapter>? = null
    private var mBleGattCallback: WeakReference<IBLEGattCallback>? = null
    private var mBluetoothDeviceAddress: String? = null
    private var mBleDevType = BleDevTypeEnum.bone

    private var mConnectTimeOutJob: Job? = null
    private var mConnectTimeOutTime = 5000L

    private var mDiscoverServiceJob: Job? = null

    private var mSYDBLEOtaHelper: SYDBLEOtaHelper = SYDBLEOtaHelper()

    private var mReconnectIndex = 0
    private var RECONNECT_COUNT = 3
    private var mReconnectJob: Job? = null

    private var mReDiscoverServiceIndex = 0

    fun setBleAdapter(bleAdapter: BluetoothAdapter) {
        mBleAdapter = WeakReference(bleAdapter)
    }

    fun setBleDevType(type: BleDevTypeEnum): BLEGattHelper {
        this.mBleDevType = type
        return this
    }

    fun setBleGattCallback(bleGattCallback: IBLEGattCallback?): BLEGattHelper {
        mBleGattCallback = if (bleGattCallback == null) {
            null
        } else {
            WeakReference(bleGattCallback)
        }
        return this
    }

    fun getCharacteristic(characteristicEnum: CharacteristicEnum) =
            if (mGatt == null) {
                Log.i(TAG, "getCharacteristic error gatt is null")
                null
            } else {

                mGatt?.getService(if (characteristicEnum == CharacteristicEnum.battery) BLEUuids.BATTERY_SERVICE_UUID else BLEUuids.getBLEServiceUuid(
                    mBleDevType
                )
                )?.let {
                    it.getCharacteristic(when (characteristicEnum) {
                        CharacteristicEnum.tx -> BLEUuids.getBLETxCharacteristivUuid(mBleDevType)
                        CharacteristicEnum.rx -> BLEUuids.getBLERxCharacteristivUuid(mBleDevType)
                        else -> BLEUuids.BATTERY_CHARACTERISTIC_UUID
                    })
                }
            }


    fun setBleOtaListener(listener: IBLEOTA) {
        mSYDBLEOtaHelper.setBleOtaListener(listener)
    }

    @Synchronized
    fun connect(address: String?, reconnectCount: Int = 3, isReconnect: Boolean = false): Boolean {
        if (address.isNullOrEmpty() || mBleAdapter?.get() == null) {
            Log.d(TAG, "BluetoothAdapter not initialized or unspecified address.")
            return false
        }

        if (!mBluetoothDeviceAddress.isNullOrEmpty() && mBluetoothDeviceAddress != address && mGatt != null) {
            try {
                return if (mGatt?.connect() == true) {
                    mBleGattCallback?.get()?.onBleStatus(BLEStatusEnum.connected)
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                close()
            }
        }

        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            return false
        }
        RECONNECT_COUNT = reconnectCount
        if (!isReconnect)
            mReconnectIndex = 0
        return mBleAdapter?.get()?.getRemoteDevice(address)?.let {
            connectTimeOut()
            mGatt = it.connectGatt(context.get(), false, this)
            mSYDBLEOtaHelper.setBluetoothGatt(mGatt)
            Log.d(TAG, "Trying to create a new connection.")
            mBluetoothDeviceAddress = address
            true
        } ?: false
    }

    private fun connectTimeOut() {
        mConnectTimeOutJob?.cancel()
        mConnectTimeOutJob = null

        if (mReconnectIndex >= RECONNECT_COUNT) {
            Log.i(TAG, "connectTimeOut ---- [$mReconnectIndex] end")
            close()
            mBleGattCallback?.get()?.onBleStatus(BLEStatusEnum.disconnect)
            return
        }

        mConnectTimeOutJob = GlobalScope.launch(Dispatchers.Main) {
            flow {
                delay(mConnectTimeOutTime)
                emit(1)
            }.flowOn(Dispatchers.IO)
                    .collect {
                        Log.i(TAG, "connectTimeOut ---- [$mReconnectIndex]")
                        reconnectBle()
                    }
        }
    }

    /**开启通知*/
    fun openNotify(type: CharacteristicEnum): Boolean {
        Log.d(TAG, "openNotify[${type}],ble[${mBleDevType}]")
        if (mBleAdapter?.get() == null || mGatt == null) {
            Log.d(TAG, "openNotify BluetoothAdapter not initialized")
            return false
        }
        return getCharacteristic(type)?.let {
            mGatt?.setCharacteristicNotification(it, true)
            it.descriptors?.let { descriptors ->
                it.getDescriptor(BLEUuids.NOTIFY_DESCRIPTOR_UUID)?.let { descriptor ->
                    val writeType = it.writeType
                    it.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    mGatt?.writeDescriptor(descriptor)
                    it.writeType = writeType
                    true
                } ?: false
            } ?: false


        } ?: false
    }

    fun readCharacteristic(type: CharacteristicEnum): Boolean {
        if (mBleAdapter?.get() == null || mGatt == null) {
            Log.d(TAG, "readCharacteristic BluetoothAdapter not initialized")
            return false
        }

        return getCharacteristic(type)?.let {
            mGatt?.readCharacteristic(it)
            true
        } ?: false
    }

    fun writeCharacteristic(data: ByteArray): Boolean {
        if (mBleAdapter == null || mGatt == null) {
            Log.d(TAG, "writeCharacteristic BluetoothAdapter not initialized")
            return false
        }

        Log.i(TAG, "writeCharacteristic [${ParseBLEIO.getHex(data, data.size)}]")
        return getCharacteristic(CharacteristicEnum.rx)?.let {
            it.value = data
            mGatt?.writeCharacteristic(it)
            true
        } ?: false
    }

    fun setMtu(mtu: Int) =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (mBleAdapter == null || mGatt == null) {
                    Log.d(TAG, "BluetoothAdapter not initialized")
                    false
                } else {
                    mGatt?.requestMtu(mtu)
                    true
                }
            } else false

    fun readRssi(): Boolean = if (mBleAdapter == null || mGatt == null) {
        false
    } else {
        mGatt?.readRemoteRssi()
        true
    }

    fun disconnect(callback: Boolean = true): Boolean {

        mDiscoverServiceJob?.cancel()
        mDiscoverServiceJob = null

        mConnectTimeOutJob?.cancel()
        mConnectTimeOutJob = null

        mReconnectJob?.cancel()
        mReconnectJob = null

        mSYDBLEOtaHelper.close()
        mBluetoothDeviceAddress = null

        Log.e(TAG, "disConnect ---  bleDisConnected")
        mGatt?.disconnect()
        mGatt?.close()
        if (callback)
            mBleGattCallback?.get()?.onBleStatus(BLEStatusEnum.disconnect)
        mBluetoothDeviceAddress = null
        mGatt = null
        return true
    }

    fun close() {

        mDiscoverServiceJob?.cancel()
        mDiscoverServiceJob = null

        mConnectTimeOutJob?.cancel()
        mConnectTimeOutJob = null

        mReconnectJob?.cancel()
        mReconnectJob = null

        mSYDBLEOtaHelper.close()
        mBluetoothDeviceAddress = null
//        mGatt?.disconnect()
        mGatt?.close()
//        mGatt = null
    }


    //OTA
    fun startOta(file: String?) {
        mSYDBLEOtaHelper.startOta(file)
    }

    fun closeOta() = mSYDBLEOtaHelper.close()

    private fun reconnectBle() {
        mReconnectJob?.cancel()
        mReconnectJob = null

        mConnectTimeOutJob?.cancel()
        mConnectTimeOutJob = null

        mReconnectIndex++
        val address = mBluetoothDeviceAddress
        close()
        mBleGattCallback?.get()?.onBleStatus(BLEStatusEnum.reconnect)
        mReconnectJob = GlobalScope.launch {
            flow {
                delay(500L)
                emit(address)
            }.flowOn(Dispatchers.IO)
                    .collect {

                        connect(it, isReconnect = true)
                    }
        }
    }

    private fun reDiscoverService(){
        //有时候发现服务不回调,需延时 https://stackoverflow.com/questions/41434555/onservicesdiscovered-never-called-while-connecting-to-gatt-server#comment70285228_41526267
        mDiscoverServiceJob?.cancel()
        mDiscoverServiceJob = null
        mDiscoverServiceJob = GlobalScope.launch(Dispatchers.Main) {
            flow {
                for(i in 0..3){
                    delay(3000L)
                    emit(i)
                }
            }.flowOn(Dispatchers.IO)
                    .collect{
                        mReDiscoverServiceIndex++
                        if(mReDiscoverServiceIndex > 3){
                            mBleGattCallback?.get()?.onBleStatus(BLEStatusEnum.unDIscoverService)
                        }else{
                            mGatt?.discoverServices()
                            mBleGattCallback?.get()?.onBleStatus(BLEStatusEnum.discoverService)
                        }
                        Log.i(TAG, "Attempting to start service discovery,index[$mReDiscoverServiceIndex]")
                    }
        }
    }

    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        super.onConnectionStateChange(gatt, status, newState)
        Log.e(TAG, "ble onConnectionStateChange[$status],[$newState]")
        mConnectTimeOutJob?.cancel()
        if (status == 133 && mReconnectIndex < RECONNECT_COUNT) {
            reconnectBle()
            return
        }
        if (status == BluetoothGatt.GATT_SUCCESS) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.e(TAG, "ble connect successed gatt[${gatt == null}]")
                    //建立连接之后才可以发现服务
                    mDiscoverServiceJob?.cancel()
                    mDiscoverServiceJob = null
//
                    mBleGattCallback?.get()?.onBleStatus(BLEStatusEnum.discoverService)
                    mReDiscoverServiceIndex = 0
                    mGatt?.discoverServices()
                    reDiscoverService()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.e(TAG, "ble disconnect--- bleDisConnected[$status],[$newState]")
                    close()
                    mBleGattCallback?.get()?.onBleStatus(BLEStatusEnum.disconnect)
                }
                BluetoothProfile.STATE_CONNECTING -> {
                }
                else -> {
                    close()
                    mBleGattCallback?.get()?.onBleStatus(BLEStatusEnum.disconnect)
                }

            }
        } else {
            close()
            mBleGattCallback?.get()?.onBleStatus(BLEStatusEnum.disconnect)
        }

    }

    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        super.onServicesDiscovered(gatt, status)
        Log.d(TAG, "onServiceDiscovered status[$status]")
        mDiscoverServiceJob?.cancel()
        mDiscoverServiceJob = null
        if (status == BluetoothGatt.GATT_SUCCESS) {
            val services = gatt?.services
            Log.i(TAG, "services [${services?.size}]")
            if (services.isNullOrEmpty()) {

                val address = mBluetoothDeviceAddress
                disconnect(false)
                mBleGattCallback?.get()?.onBleStatus(BLEStatusEnum.reconnect)
                connect(address)
                return
            }
            for (service in services) {
                Log.e(TAG, "service uuid[" + service.uuid + "],type[" + service.type + "]")
                val characteristics = service.characteristics
                for (characteristic in characteristics) {
                    Log.e(TAG, "    characteristic UUID[" + characteristic.uuid + "],value[" + characteristic.value + "]")
                }
            }

            if (mBleDevType == BleDevTypeEnum.bone) {
                openNotify(CharacteristicEnum.battery)
            } else {
                openNotify(CharacteristicEnum.tx)
            }
        }
        mSYDBLEOtaHelper.onServicesDiscovered(gatt, status)
        mBleGattCallback?.get()?.onBleServiceDiscover(gatt, status)
    }

    override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
        super.onCharacteristicRead(gatt, characteristic, status)
        Log.d(TAG, "onCharacteristicRead")

        mBleGattCallback?.get()?.onCharacteristicRead(gatt, characteristic, status)
        if (characteristic?.uuid?.toString()?.toLowerCase() == mSYDBLEOtaHelper.getOtaUpdateCharacteristic()?.uuid?.toString()?.toLowerCase()) {
            mSYDBLEOtaHelper.onCharacteristicRead(gatt, characteristic, status)
        }
    }

    override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
        super.onCharacteristicWrite(gatt, characteristic, status)
        mBleGattCallback?.get()?.onCharacteristicWrite(gatt, characteristic, status)
        if (characteristic?.uuid?.toString()?.toLowerCase() == mSYDBLEOtaHelper.getOtaUpdateCharacteristic()?.uuid?.toString()?.toLowerCase()) {
            mSYDBLEOtaHelper.onCharacteristicWrite(gatt, characteristic, status)
        }
    }

    override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
        super.onCharacteristicChanged(gatt, characteristic)
        mBleGattCallback?.get()?.onCharacteristicChange(gatt, characteristic)
    }

    override fun onDescriptorRead(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
        super.onDescriptorRead(gatt, descriptor, status)

    }

    override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
        super.onDescriptorWrite(gatt, descriptor, status)
        //设置通知回调

        val uuid = descriptor!!.uuid
        val characteristic = descriptor.characteristic
        val characteristicUUID = characteristic.uuid
        val cUuid = characteristicUUID?.toString() ?: ""

        Log.d(TAG, "onDescriptorWrite uuid[${uuid.toString()}],cUuid[$cUuid]")

        if (cUuid.toLowerCase() == BLEUuids.BATTERY_CHARACTERISTIC_UUID.toString().toLowerCase()) {
            openNotify(CharacteristicEnum.tx)
        } else if (cUuid.toLowerCase() == BLEUuids.getBLETxCharacteristivUuid(mBleDevType).toString().toLowerCase()) {
            mBleGattCallback?.get()?.onBleStatus(BLEStatusEnum.connected)
        }

        mBleGattCallback?.get()?.onNotifyWrite(gatt, descriptor, status)
    }

    override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
        super.onReadRemoteRssi(gatt, rssi, status)
        //rssi回调
        mBleGattCallback?.get()?.onReadRssi(gatt, rssi, status)
    }

    override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
        super.onMtuChanged(gatt, mtu, status)
        //mtu回调
        mBleGattCallback?.get()?.onMtuChanged(gatt, mtu, status)
    }


}