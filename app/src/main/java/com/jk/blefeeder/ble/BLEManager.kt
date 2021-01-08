package com.jk.blefeeder.ble

import android.bluetooth.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.base.utils.MLog
import com.jk.blefeeder.ble.impl.IBLEScan
import com.jk.blefeeder.ble.impl.IBLEService
import com.jk.blefeeder.ble.impl.IBLEStatus
import com.jk.blefeeder.ble.impl.IBLEWork
import com.jk.blefeeder.ble.impl.IBLEGattCallback
import com.jk.blefeeder.ble.bean.BLEDev
import com.jk.blefeeder.ble.bean.BleDevTypeEnum
import com.jk.blefeeder.ble.impl.IBLEOTA
import com.jk.blefeeder.ble.io.ParseBLEIO
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 *
 *@author abc
 *@time 2020/4/27 9:47
 */
@ExperimentalCoroutinesApi
class BLEManager private constructor(val context: Context, var bleDevType: BleDevTypeEnum) :
    IBLEGattCallback(), BleBrocastReceiver.IBLEBrocastCallback, ServiceConnection,
    BLEScanHelper.IScanCallback, IBLEOTA {

    companion object {
        @Volatile
        var INSTANCE: BLEManager? = null


        fun getInstance(
            context: Context,
            type: BleDevTypeEnum = BleDevTypeEnum.catroom
        ): BLEManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: BLEManager(context.applicationContext, type).also {
                    INSTANCE = it
                }
            }
    }

    private val TAG by lazy { BLEManager::class.java.simpleName }


    private val mBluetoothManager: BluetoothManager by lazy { context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }

    private val mBleAdapter by lazy {
        mBluetoothManager.adapter
    }

    private var mBLEBrocastReceiver: BleBrocastReceiver = BleBrocastReceiver().apply {
        registerBLEBrocastCallback(this@BLEManager)
    }

    private var mBLEService: BLEService? = null

    //蓝牙扫描监听
    private var mIBleScanList = mutableListOf<IBLEScan>()
    private var mScanResult = mutableListOf<BLEDev>()
    private var mBLEScanHelper: BLEScanHelper? = null

    //蓝牙状态监听
    private var mBLEStatusCallback = mutableListOf<IBLEStatus>()

    //蓝牙通信监听
    private var mBLEWorkCallback = mutableListOf<IBLEWork>()

    //蓝牙服务监听
    private var mBLEServiceCallback = mutableListOf<IBLEService>()

    private var mBLEOTACallback = mutableListOf<IBLEOTA>()

    private var mBLEStatus: BLEStatusEnum = BLEStatusEnum.close

    //消息重发次数
    private var mBleRetrySendCount = 3

    //消息重发间隔
    private var mBleRetrySendSpaceTime = 3000L

    private var mBleRetryJob: Job? = null

    //发送数据列表
    @Volatile
    private var mBleDataList = mutableListOf<ByteArray>()

    init {
        registerBLEBrocastReceiver()
        bindBLEService()
        mBLEScanHelper = BLEScanHelper(context).also {
            it.setScanResultCallback(this)
        }
    }

    fun setBleType(type: BleDevTypeEnum): BLEManager {
        MLog.i("TAG", "setBleType[$type]");
        this.bleDevType = type
        mBLEService?.setBleDevType(bleDevType)
        return this
    }

    fun getBLeStatus() = mBLEStatus

    /**注册蓝牙数据、状态监听广播*/
    private fun registerBLEBrocastReceiver() {
        context.registerReceiver(mBLEBrocastReceiver, mBLEBrocastReceiver.getIntentFilter())
    }

    private fun unRegisterBLEBrocastREceiver() {
        context.unregisterReceiver(mBLEBrocastReceiver)
    }

    /**绑定蓝牙交互Service*/
    private fun bindBLEService() {
        val intent = Intent(context, BLEService::class.java)
        context.bindService(intent, this, Context.BIND_AUTO_CREATE)
        Log.d(TAG, "bindService")
    }

    fun setRetrySendCount(retrySendCount: Int): BLEManager {
        this.mBleRetrySendCount = retrySendCount
        return this
    }

    fun setRetrySendSpaceTime(retrySendSpaceTime: Long): BLEManager {
        this.mBleRetrySendSpaceTime = retrySendSpaceTime
        return this
    }

    /**添加扫描监听*/
    fun addIBleScan(bleScanResult: IBLEScan): BLEManager {
        val old = mIBleScanList.singleOrNull { it == bleScanResult }
        if (old == null) {
            mIBleScanList.add(bleScanResult)
        }
        return this
    }

    /**移出扫描监听*/
    fun removeIBleScan(bleScanResult: IBLEScan): BLEManager {
        mIBleScanList.remove(bleScanResult)
        return this
    }


    fun removeAllIBLEScan(): BLEManager {
        mIBleScanList.clear()
        return this
    }


    //蓝牙状态监听
    fun addIBLEStatus(bleStatusCallback: IBLEStatus): BLEManager {
        val old = mBLEStatusCallback.singleOrNull { it == bleStatusCallback }
        if (old == null) {
            mBLEStatusCallback.add(bleStatusCallback)
        }
        return this
    }

    fun removeIBLEStatus(bleStatusCallback: IBLEStatus): BLEManager {
        mBLEStatusCallback.remove(bleStatusCallback)
        return this
    }

    fun removeAllIBLEStatus(): BLEManager {
        mBLEStatusCallback.clear()
        return this
    }

    //蓝牙通信监听
    fun addIBLEWork(bleWorkCallback: IBLEWork): BLEManager {
        val old = mBLEWorkCallback.singleOrNull { it == bleWorkCallback }
        if (old == null) {
            mBLEWorkCallback.add(bleWorkCallback)
        }
        return this
    }

    fun removeIBLEWork(bleWorkCallback: IBLEWork): BLEManager {
        mBLEWorkCallback.remove(bleWorkCallback)
        return this
    }

    fun removeAllIBLEWork(): BLEManager {
        mBLEWorkCallback.clear()
        return this
    }

    //蓝牙扫描service UUID
    fun addIBLEService(bleServiceCallback: IBLEService): BLEManager {
        val old = mBLEServiceCallback.singleOrNull { it == bleServiceCallback }
        if (old == null) {
            mBLEServiceCallback.add(bleServiceCallback)
        }
        return this
    }

    fun removeIBLEService(bleServiceCallback: IBLEService): BLEManager {
        mBLEServiceCallback.remove(bleServiceCallback)
        return this
    }

    fun removeAllIBLEService(): BLEManager {
        mBLEServiceCallback.clear()
        return this
    }

    fun addIBLEOta(bleOta: IBLEOTA): BLEManager {
        mBLEOTACallback.remove(mBLEOTACallback.singleOrNull { it == bleOta })
        mBLEOTACallback.add(bleOta)
        return this
    }

    fun removeIBLEOta(bleOta: IBLEOTA): BLEManager {
        mBLEOTACallback.remove(bleOta)
        return this
    }

    fun removeAllIBLEOta(): BLEManager {
        mBLEOTACallback.clear()
        return this
    }

    /**
     * @param duration 扫描时长 毫秒
     * */
    fun startLeScan(duration: Long, index: Int = 1): BLEManager {
        startLeScan(null, index, duration)
        return this
    }

    /**
     * @param uid 扫描指定地址
     * @param duration 扫描时长 毫秒
     * */
    fun startLeScan(address: String?, index: Int = 1, duration: Long): BLEManager {

        mBLEScanHelper?.startLeScan(address, index, duration)
        return this
    }

    fun stopScan(result: Boolean = true): BLEManager {
        Log.i(TAG, "BLEManager  stopScan")
        if (result) {
            mBLEScanHelper?.stopScan(result)
        }
        return this
    }

    fun bleConnected() = mBLEStatus == BLEStatusEnum.connected

    //连接设备
    fun connectBLE(address: String?, reconnectCount: Int = 3): Boolean {
        Log.i(TAG, "connectBLE[${address.isNullOrEmpty()}]")
        if (address.isNullOrEmpty()) return false
        stopScan(false)
        mBLEStatus = BLEStatusEnum.connectting
        Log.i(TAG, "connectBLE[${mBLEService == null}]")
        if (mBLEService == null) return false
        mBLEService?.setBleDevType(bleDevType)
        return mBLEService?.connect(address, reconnectCount) ?: false
    }

    //发送数据
    fun sendData(data: ByteArray): Boolean =
        if (mBLEService != null && mBLEStatus == BLEStatusEnum.connected) {
            Log.i(TAG, "sendData --- [${ParseBLEIO.getHex(data, data.size)}]")
            if (mBleDataList.isEmpty()) {
                mBleDataList.add(data)
                retrySendData()
            } else {
                mBleDataList.add(data)
            }
            true
//                mBLEService?.sendData(data) ?: false
        } else {
            Log.d(TAG, "sendData error mBLEStatus[$mBLEStatus],mBLEService[${mBLEService == null}]")
            false
        }

    //开启OTA
    fun startOta(file: String?): Boolean =
        if (mBLEService != null && mBLEStatus == BLEStatusEnum.connected) {
            mBLEService?.startOta(file)
            true
        } else {
            Log.d(TAG, "startOta error mBLEStatus[$mBLEStatus],mBLEService[${mBLEService == null}]")
            false
        }

    fun closeOta() = mBLEService?.closeOta()

    private fun retrySendData() {
        mBleRetryJob?.cancel()
        mBleRetryJob = null
        if (mBleDataList.isEmpty()) return
        mBleRetryJob = GlobalScope.launch(Dispatchers.Main) {
            flow {
                for (i in 0..mBleRetrySendCount) {
                    emit(i)
                    delay(mBleRetrySendSpaceTime)
                }
            }.flowOn(Dispatchers.IO)
                .collect {
                    if (mBleDataList.isEmpty()) return@collect
                    if (it == mBleRetrySendCount) {
                        val rData = mBleDataList.removeAt(0)
                        mBLEWorkCallback.forEach { call ->
                            call.bleWork(BLEWorkEnum.sendTimeOut, rData)
                        }
                        retrySendData()
                    } else {
                        mBLEService?.sendData(mBleDataList[0])
                    }
                }
        }
    }

    private fun removeAllData() {
        mBleRetryJob?.cancel()
        mBleRetryJob = null
        mBleDataList.clear()
    }

    //设置mtu
    fun setMTU(mtu: Int): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (mBLEService != null && mBLEStatus == BLEStatusEnum.connected) {
                mBLEService?.setMtu(mtu) ?: false
                true
            } else {
                Log.d(
                    TAG,
                    "setMTU error mBLEStatus[$mBLEStatus],mBLEService[${mBLEService == null}]"
                )
                false
            }
        } else {
            Log.d(TAG, "setMTU error adnroid version < 5.0")
            false
        }

    //获取电量
    fun refreshBattery(): Boolean =
        if (mBLEService != null && mBLEStatus == BLEStatusEnum.connected) {
            mBLEService?.getBattery() ?: false
            true
        } else {
            Log.d(
                TAG,
                "refreshBattery error mBLEStatus[$mBLEStatus],mBLEService[${mBLEService == null}]"
            )
            false
        }


    //刷新信号强度
    fun refreshRssi(): Boolean =
        if (mBLEService != null && mBLEStatus == BLEStatusEnum.connected) {
            mBLEService?.getRssi() ?: false
            true
        } else {
            Log.d(
                TAG,
                "refreshRssi error mBLEStatus[$mBLEStatus],mBLEService[${mBLEService == null}]"
            )
            false
        }


    //断开蓝牙连接
    fun disconectBLE(): Boolean {
        removeAllData()
        return mBLEService?.disconnect() ?: false
    }

    fun destory() {


        unRegisterBLEBrocastREceiver()
        context.unbindService(this)

        mBLEScanHelper?.stopScan()
        mBLEScanHelper?.destory()


        removeAllData()

        removeAllIBLEScan()
        removeAllIBLEService()
        removeAllIBLEStatus()
        removeAllIBLEWork()

        mBLEService?.disconnect()
        mBLEService?.stopSelf()

        mBLEService = null
        INSTANCE = null
    }

    override fun bleBroacastReceiver(action: String?, intent: Intent) {
        if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            val connectStatus = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
            if (connectStatus == BluetoothAdapter.STATE_OFF) {
                mBLEStatus = BLEStatusEnum.close
                mBLEStatusCallback.forEach {
                    it.bleStatus(BLEStatusEnum.close)
                }
            } else if (connectStatus == BluetoothAdapter.STATE_ON) {
                mBLEStatus = BLEStatusEnum.opened
                mBLEStatusCallback.forEach {
                    it.bleStatus(BLEStatusEnum.opened)
                }
            }
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Log.d(TAG, "onServiceDisconnected")
        mBLEService = null
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        Log.d(TAG, "onServiceConnected")
        if (service is BLEService.BLEBind) {
            mBLEService = service.getService().apply {
                setBleAdapter(mBleAdapter)
                setBleDevType(bleDevType)
                setBleGattCallback(this@BLEManager)
                setBleOtaListener(this@BLEManager)
            }
        }
    }

    override fun onScanResult(device: BLEDev) {
        mBLEService?.setBleDevType(bleDevType)
        val dev =
            mScanResult.singleOrNull { it.bluetoothDevice.address == device.bluetoothDevice.address }
        if (dev == null) {
            mScanResult.add(device)
//            GlobalScope.launch(Dispatchers.Main) {
//                Log.i(TAG, "onScanResult")
            mIBleScanList.forEach {
                it.bleScanning(device)
            }
//            }
        }

    }

    override fun onStopScan() {
        GlobalScope.launch(Dispatchers.Main) {
            Log.i(TAG, "onStopScan")
            mIBleScanList.forEach {
                it.bleScanResult(mScanResult)
            }
        }

    }

    override fun onStartScan() {
        GlobalScope.launch(Dispatchers.Main) {
            mScanResult.clear()
            Log.i(TAG, "onStartScan")
            mIBleScanList.forEach {
                it.bleStartScanBefor()
            }
        }
    }

    override fun onBleStatus(status: BLEStatusEnum) {
        mBLEStatus = status
        GlobalScope.launch(Dispatchers.Main) {
            if (status == BLEStatusEnum.disconnect) {
                removeAllData()
            }
            mBLEStatusCallback.forEach {
                it.bleStatus(status)
            }
        }

    }

    override fun onBleServiceDiscover(gatt: BluetoothGatt?, status: Int) {
        super.onBleServiceDiscover(gatt, status)
        GlobalScope.launch(Dispatchers.Main) {
            mBLEServiceCallback.forEach {
                it.bleDiscoverService(gatt, status)
            }
        }

    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        super.onCharacteristicRead(gatt, characteristic, status)
        GlobalScope.launch(Dispatchers.Main) {
            mBLEServiceCallback.forEach {
                it.bleCharacteristicRead(characteristic, status)
            }
        }

    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        super.onCharacteristicWrite(gatt, characteristic, status)
        GlobalScope.launch(Dispatchers.Main) {
            mBLEServiceCallback.forEach {
                it.bleCharacteristicWrite(characteristic, status)
            }
        }

    }

    /**开启通知回调*/
    override fun onNotifyWrite(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int
    ) {
        super.onNotifyWrite(gatt, descriptor, status)
        GlobalScope.launch(Dispatchers.Main) {
            mBLEWorkCallback.forEach {
                it.bleWork(BLEWorkEnum.openNotify, null)
            }
        }

    }

    /**信号强度*/
    override fun onReadRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
        super.onReadRssi(gatt, rssi, status)
        GlobalScope.launch(Dispatchers.Main) {
            mBLEWorkCallback.forEach {
                it.bleWork(BLEWorkEnum.rssi, rssi)
            }
        }

    }

    /**数据变化回调  Notify*/
    override fun onCharacteristicChange(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?
    ) {
        GlobalScope.launch(Dispatchers.Main) {
            if (characteristic?.uuid?.toString()
                    ?.toLowerCase() == BLEUuids.BATTERY_CHARACTERISTIC_UUID.toString().toLowerCase()
            ) {
                val data = characteristic.value
                if (data.isNotEmpty()) {
                    mBLEWorkCallback.forEach {
                        it.bleWork(BLEWorkEnum.battery, data[0].toInt())
                    }
                }
            } else {
                characteristic?.value?.let { data ->
                    Log.i(TAG, "onCharacteristicChange [${ParseBLEIO.getHex(data, data.size)}]")
                    mBLEWorkCallback.forEach {
                        it.bleWork(BLEWorkEnum.byteArray, data)
                    }

                    //接受数据知否，发送剩余的命令
                    if (mBleDataList.isNotEmpty()) {
                        if (ParseBLEIO.checkCrc(data)) {
                            //如果数据格式正确，则清除掉当前发送的数据
                            mBleDataList.removeAt(0)
                        }
                        retrySendData()
                    }
                }
            }
        }

    }

    override fun bleOTAStart(complete: Boolean) {
        mBLEOTACallback.forEach {
            it.bleOTAStart(complete)
        }
    }

    override fun writeSectionStart(complete: Boolean) {
        mBLEOTACallback.forEach {
            it.writeSectionStart(complete)
        }
    }

    override fun bleOtaProgress(progress: Int) {
        mBLEOTACallback.forEach {
            it.bleOtaProgress(progress)
        }
    }

    override fun bleOtaSending(complete: Boolean) {
        mBLEOTACallback.forEach {
            it.bleOtaSending(complete)
        }
    }

    override fun bleOtaResult(result: Boolean) {
        mBLEOTACallback.forEach {
            it.bleOtaResult(result)
        }
    }

}