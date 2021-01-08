package com.jk.blefeeder.ble

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
import android.os.Looper
import android.os.Message
import android.os.ParcelUuid
import android.util.Log
import cn.p2ppetcam.weight.MyHandler
import com.jk.blefeeder.ble.bean.BLEDev
import com.wyj.base.log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 *扫描类
 *@author abc
 *@time 2020/4/27 13:55
 */
@ExperimentalCoroutinesApi
class BLEScanHelper constructor(val context: Context) : BluetoothAdapter.LeScanCallback {

    private val TAG = BLEScanHelper::class.java.simpleName

    private var mBLEAdapter: BluetoothAdapter

    //5.0 蓝牙扫描对象
    private var mBLEScanner: BluetoothLeScanner? = null

    //5.0及其以上扫描回调对象
    private lateinit var mScanCallback: ScanCallback

    //5.0扫描配置对象
    private lateinit var mScanSettings: ScanSettings

    //扫描过滤器列表
    private lateinit var mScanFilterList: MutableList<ScanFilter>

    //扫描指定地址的address
    private var mCurrentScanAddress: String? = null

    private var mScanLeJob: Job? = null

    private var mStopScanLeJob: Job? = null

    //是否正在扫描
    private var mScanning = false

    private var mScanResultCallback: IScanCallback? = null

    private var mScanCount = 1
    private var mCurrentScanIndex = 0
    private var mScanDuration = 7000L

    private val mHandler = object : MyHandler(context, Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (msg.what == 0) {
                mCurrentScanIndex++
                this@BLEScanHelper.log("scan index[${mCurrentScanIndex}],count[$mScanCount]")
                if (mCurrentScanIndex <= mScanCount) {
                    nextScan()
                } else {
                    stopScan()
                }
            }

        }
    }

    private fun nextScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBLEScanner?.startScan(null, mScanSettings, mScanCallback)
        } else {
            mBLEAdapter.startLeScan(this@BLEScanHelper)
        }
        mHandler.sendEmptyMessageDelayed(0, mScanDuration + 1000)
    }

    init {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBLEAdapter = manager.adapter

        //初始化蓝牙回调
        initBluetoothCallBack()
        //初始化蓝牙扫描配置
        initScanSetting()
        //初始化蓝牙过滤器
        initScanFilter()
    }

    fun setScanResultCallback(scanResultCallback: IScanCallback?) {
        this.mScanResultCallback = scanResultCallback
    }

    private fun initBluetoothCallBack() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mScanCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult?) {
                    super.onScanResult(callbackType, result)
                    Log.i(
                        TAG,
                        "ble scan result[${result?.device?.address}],[${result?.device?.name}]"
                    )
                    result?.let {
                        if (it.device.address == mCurrentScanAddress && !mCurrentScanAddress.isNullOrEmpty()) {
                            Log.i(TAG, "onScanResult  stopScan ")
                            stopScan()
                        }
                        mScanResultCallback?.onScanResult(BLEDev(it.device, it.rssi))
                    }
                }

                override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                    super.onBatchScanResults(results)
                    Log.i(TAG, "onBatchScanResults")
                }

                override fun onScanFailed(errorCode: Int) {
                    super.onScanFailed(errorCode)
                    Log.i(TAG, "onScanFailed errorCode[$errorCode]")
                }
            }
        }
    }

    private fun initScanSetting() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return
        val builder = ScanSettings.Builder()
            //设置功耗平衡模式
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
        //android 6.0添加设置回调类型、匹配模式等
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //定义回调类型
            builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            //设置蓝牙LE扫描滤波器硬件匹配的匹配模式
            builder.setMatchMode(ScanSettings.MATCH_MODE_STICKY)
        }
        //芯片组支持批处理芯片上的扫描
        if (mBLEAdapter.isOffloadedScanBatchingSupported()) {
            //设置蓝牙LE扫描的报告延迟的时间（以毫秒为单位）
            //设置为0以立即通知结果
            builder.setReportDelay(0L)
        }
        mScanSettings = builder.build()
    }

    /**
     * 初始化拦截器实现
     * 扫描回调只会返回符合该拦截器UUID的蓝牙设备
     */
    private fun initScanFilter() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return
        mScanFilterList = ArrayList<ScanFilter>()
        val builder = ScanFilter.Builder()
        builder.setServiceUuid(ParcelUuid.fromString("0000fff1-0000-1000-8000-00805f9b34fb"))
        mScanFilterList.add(builder.build())
    }

    fun startLeScan(duration: Long = 5000L, scanCount: Int = 1) {
        startLeScan(null, scanCount, duration)
    }

    fun startLeScan(searchAddress: String?, scanCount: Int = 1, duration: Long = 5000L) {
        if (!mBLEAdapter.isEnabled) return
        Log.i(TAG, "startLeScan[$mCurrentScanAddress]")
        if (!mScanning) {
            mScanResultCallback?.onStartScan()
            mScanning = true
            mCurrentScanAddress = searchAddress
            val count = if (scanCount < 1) 1 else scanCount
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (mBLEScanner == null) {
                    mBLEScanner = mBLEAdapter.bluetoothLeScanner
                }


//                mScanLeJob?.cancel()
//                mScanLeJob = null
//
//                mScanLeJob = GlobalScope.launch(Dispatchers.Main) {
//                    //mScanSettings是ScanSettings实例，mScanCallback是ScanCallback实例，后面进行讲解。
//                    //过滤器列表传空，则可以扫描周围全部蓝牙设备
//
//                    flow {
//                        for (i in 1..count) {
//                            emit(i)
//                            //扫描多次 每次间隔1秒
//                            delay(duration + 1000)
//                        }
//                    }.flowOn(Dispatchers.IO)
//                        .collect {
//                            mBLEScanner?.startScan(null, mScanSettings, mScanCallback)
//                        }
//                    //使用拦截器
//                    //mBleScanner?.startScan(mScanFilterList,mScanSettings,mScanCallback)
//                }
            } else {
//                mScanLeJob?.cancel()
//                mScanLeJob = null
//                mScanLeJob = GlobalScope.launch(Dispatchers.IO) {
//                    flow {
//                        for (i in 1..count) {
//                            emit(i)
//                            delay(duration + 1000)
//                        }
//                    }.flowOn(Dispatchers.IO)
//                        .collect {
//                            mBLEAdapter.startLeScan(this@BLEScanHelper)
//                        }
//                }
            }

            mScanCount = count
            mCurrentScanIndex = 0
            mScanDuration = duration
            mHandler.sendEmptyMessage(0)

//            mStopScanLeJob?.cancel()
//            mStopScanLeJob = null
//            mStopScanLeJob = GlobalScope.launch(Dispatchers.Main) {
//                withContext(Dispatchers.IO) {
//                    delay((duration + 1000) * count)
//                }
//                Log.i(TAG, "mStopScanLeJob  stopScan ")
//                stopScan()
//            }

        }
    }

    fun stopScan(result: Boolean = true) {
        Log.i(TAG, "ble stop scan resule[$result]")

        mHandler.removeCallbacksAndMessages(null)
        mScanLeJob?.cancel()
        mScanLeJob = null

        mStopScanLeJob?.cancel()
        mStopScanLeJob = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBLEScanner?.stopScan(mScanCallback)
        } else {
            mBLEAdapter.stopLeScan(this)
        }

        mScanning = false
        if (result) {
            mScanResultCallback?.onStopScan()
        }
    }

    fun destory() {

        mHandler.removeCallbacksAndMessages(null)

        mScanLeJob?.cancel()
        mScanLeJob = null

        mStopScanLeJob?.cancel()
        mStopScanLeJob = null
    }

    override fun onLeScan(device: BluetoothDevice?, rssi: Int, scanRecord: ByteArray?) {
        Log.i(TAG, "ble scan result[${device?.address}]")
        if (device?.address == mCurrentScanAddress && !mCurrentScanAddress.isNullOrEmpty()) {
            Log.i(TAG, "onLeScan  stopScan ")
            stopScan()
        }
        device?.let {
            mScanResultCallback?.onScanResult(BLEDev(it, rssi))
        }
    }

    interface IScanCallback {
        fun onScanResult(device: BLEDev)
        fun onStopScan()
        fun onStartScan()
    }

}