package com.jk.blefeeder.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.jk.blefeeder.ble.impl.IBLEScan
import com.jk.blefeeder.base.BaseAndroidViewModel
import com.jk.blefeeder.bean.LocalSet
import com.jk.blefeeder.ble.BLEManager
import com.jk.blefeeder.ble.bean.BLEDev
import com.jk.blefeeder.ble.bean.BleDevTypeEnum
import com.jk.blefeeder.room.AppDataBase
import com.wyj.base.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainViewModel(app: Application) : BaseAndroidViewModel(app), IBLEScan {


    private val mLocalDao by lazy { AppDataBase.getInstance(app).getLocalSerDao() }
    val mBleDevList = MutableLiveData<MutableList<BLEDev>>()

    val mBleName = MutableLiveData<String?>()
    val mBleVersion = MutableLiveData<String?>()
    val mBleRssi = MutableLiveData<String?>()
    val mBleFeedNum = MutableLiveData<String?>()
    val mBleRecordTime = MutableLiveData<String?>()

    val mSaveResult = MutableLiveData<Boolean>()

    val mStopScan = MutableLiveData<Boolean>()

    private fun getBleManager() =
        BLEManager.getInstance(app, BleDevTypeEnum.catroom).addIBleScan(this)



    fun getLocalSetContent() {
        launch {
            withContext(Dispatchers.IO) {
                val localList = mLocalDao.queryAll()
                if (localList.isNotEmpty()) {
                    val localSet = localList[0]
                    mBleName.postValue(localSet.bleName)
                    mBleVersion.postValue(localSet.bleVersion)
                    mBleRssi.postValue(localSet.bleRssi)
                    mBleFeedNum.postValue(localSet.feedNum)
                    mBleRecordTime.postValue(localSet.recordTime)
                }
            }
        }

    }

    fun searchBle() {

        mBleDevList.value = mutableListOf()
        getBleManager().startLeScan(7000L)

    }


    fun saveLocal(name: String?, version: String?, rssi: String?, num: String?,recordTime:String?) {
        if(!recordTime.isNullOrEmpty()){
            if(recordTime.toInt() > 10){
                errorMsg.value = "录音时长最长10S"
                return
            }
        }
        launch {
            withContext(Dispatchers.IO) {
                val localList = mLocalDao.queryAll()
                if (localList.isNotEmpty()) {
                    val localSet = localList[0]
                    localSet.bleName = name
                    localSet.bleRssi = rssi
                    localSet.feedNum = num
                    localSet.bleVersion = version
                    localSet.recordTime = recordTime
                    mLocalDao.update(localSet)
                } else {
                    val localSet = LocalSet(name, rssi, version, num,recordTime)
                    mLocalDao.insert(localSet)
                }
                mSaveResult.postValue(true)
            }
            mBleName.value = name
            mBleRssi.value = rssi
            mBleFeedNum.value = num
            mBleVersion.value = version
            mBleRecordTime.value = recordTime
        }
    }

    private fun getBleName(dev: BLEDev): Boolean? {

        return if ((mBleName.value ?: "").toUpperCase().contains("AF2B") || (mBleName.value
                ?: "").toUpperCase().contains("XAF01")
        ) {
            (dev.bluetoothDevice.name?.contains("AF2B") ?: false) ||
                    dev.bluetoothDevice.name?.contains("XAF01") ?: false
        } else {
            dev.bluetoothDevice.name?.contains((mBleName.value ?: "").toUpperCase())
        }
    }

    override fun bleScanResult(bleList: MutableList<BLEDev>) {
        log("bleScanResult [${mBleName.value}]")
//        if (!mBleName.value.isNullOrBlank()) {
//            val filter =
//                bleList.filter {
//                    getBleName(it) == true
//                }
//                    .toMutableList()
//            mBleDevList.postValue(filter)
//        } else {
//            mBleDevList.postValue(bleList)
//        }
        mStopScan.value = true

    }
    override fun bleScanning(bleDev: BLEDev) {
        mStopScan.postValue(false)
        log("bleScanning [${getBleName(bleDev)}],name[${bleDev.bluetoothDevice.name}]")
        if(getBleName(bleDev) == true){
            val list = mBleDevList.value?: mutableListOf()
            list.add(bleDev)
            mBleDevList.postValue(list)
        }
    }

}