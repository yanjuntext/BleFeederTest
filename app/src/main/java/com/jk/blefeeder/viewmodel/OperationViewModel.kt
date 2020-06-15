package com.jk.blefeeder.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.jk.blefeeder.TAG
import com.jk.blefeeder.base.BaseAndroidViewModel
import com.jk.blefeeder.ble.BLEManager
import com.jk.blefeeder.ble.BLEStatusEnum
import com.jk.blefeeder.ble.BLEWorkEnum
import com.jk.blefeeder.ble.bean.BleDevTypeEnum
import com.jk.blefeeder.ble.impl.IBLEStatus
import com.jk.blefeeder.ble.impl.IBLEWork
import com.jk.blefeeder.ble.io.BLEIO
import com.jk.blefeeder.ble.io.ParseBLEIO
import com.jk.blefeeder.ble.utils.BLEUtils
import com.jk.blefeeder.room.AppDataBase
import com.wyj.base.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.*

class OperationViewModel(app: Application) : BaseAndroidViewModel(app), IBLEStatus, IBLEWork {

    val address = MutableLiveData<String>()

    val bleStatus = MutableLiveData<BLEStatusEnum>()

    val bleRssiResult = MutableLiveData<Boolean>()
    val bleRssi = MutableLiveData<String?>()

    val bleVersionResult = MutableLiveData<Boolean>()
    val bleVersion = MutableLiveData<String?>()

    val bleFeedResult = MutableLiveData<String>()
    val bleBattery = MutableLiveData<Int>()
    val bleSetMacResult = MutableLiveData<Boolean>()

    val mLocalRssi = MutableLiveData<String?>()
    val mLocalFeedNum = MutableLiveData<String?>()
    val mLocalVersion = MutableLiveData<String>()
    val mLocalRecord = MutableLiveData<String>()

    val mRecordValue = MutableLiveData<String>()

    var mLedIndex = 0
    val mLedStatus = MutableLiveData<String>()

    val mNewMac = MutableLiveData<String>()
    private var newMac: String? = null

    val needSetMac = MutableLiveData<Boolean>()

    private val mLocalDao by lazy { AppDataBase.getInstance(app).getLocalSerDao() }
    private fun getBleManager() =
        BLEManager.getInstance(app, BleDevTypeEnum.catroom)

    fun setBleAddress(address: String) {
        log("address[$address]")
        getBleManager().addIBLEStatus(this).addIBLEWork(this)
        this.address.value = address
        getLocalSet()

        connect()
    }

    fun connect() {

        bleStatus.value = BLEStatusEnum.connectting
        log("connect [${address.value}],new[${mNewMac.value}]")
        if (mNewMac.value.isNullOrEmpty()) {
            log("connect old address")
            getBleManager().connectBLE(address.value ?: "", 3)
        } else {
            log("connect old newAddress")
            getBleManager().connectBLE(mNewMac.value ?: "", 3)
        }
    }

    private fun getLocalSet() {
        launch {
            withContext(Dispatchers.IO) {
                val localList = mLocalDao.queryAll()
                if (localList.isNotEmpty()) {
                    val localSet = localList[0]
                    mLocalFeedNum.postValue(
                        if (localSet.feedNum.isNullOrBlank()) "1" else localSet.feedNum ?: "1"
                    )
                    mLocalRssi.postValue(
                        if (localSet.bleRssi.isNullOrBlank()) "0" else localSet.bleRssi ?: "0"
                    )
                    mLocalVersion.postValue(localSet.bleVersion ?: "")
                    mLocalRecord.postValue(localSet.recordTime ?: "3")
                }
            }
        }
    }

    private fun sendData(data: ByteArray) {
        loadingTime.value = 5000
        getBleManager().sendData(data)
    }

    fun getBattery() = sendData(BLEIO.checkBattery())

    fun getVersion() = sendData(BLEIO.setCheckVersion())

    fun getRssi() = getBleManager().refreshRssi()

    private fun getFeedNum() =
        if (mLocalFeedNum.value.isNullOrBlank()) 1 else (mLocalFeedNum.value ?: "1").toInt()

    fun setMac(mac: Long) {
        val data = BLEIO.setMac(mac)
        val macData = ByteArray(6)
        System.arraycopy(data, 3, macData, 0, 6)
        Log.i(TAG(), "mac all[${ParseBLEIO.getHex(macData, macData.size)}]")
//
        var strMac = ""
        for (i in 0 until 6) {

            strMac += ParseBLEIO.byteToHex(macData[i])
            if (i != 5) {
                strMac += ":"
            }
        }
        newMac = strMac
        sendData(data)
        Log.i(TAG(), "strMac all[${strMac}]")
    }

    private fun getRecordTime() =
        if (mLocalRecord.value.isNullOrBlank()) 3 else (mLocalRecord.value ?: "3").toInt()

    //录音
    fun setRecordTime() {
        sendData(BLEIO.setFeedSound(BLEIO.FeedSoundEnum.start, getRecordTime()))
    }

    //喂食
    fun feed() = sendData(
        BLEIO.setManualFeed(
            getCalendar(Calendar.HOUR_OF_DAY),
            getCalendar(Calendar.MINUTE),
            getFeedNum()
        )
    )

    //红/绿灯
    fun setLed() {
        mLedIndex = if (mLedIndex == 0) 1 else 0
        sendData(BLEIO.setLed(mLedIndex))
    }


    private fun getCalendar(type: Int) = Calendar.getInstance().get(type)

    private fun refreshMac() {
        val old = address.value ?: ""
        val new = mNewMac.value ?: ""
        if (new.isEmpty()) address.value = old else address.value = new
    }

    fun getCurrentMac(): String? = address.value
    fun getNewMac(): String? = mNewMac.value


    private fun isTrueMac() {
        if (address.value?.startsWith("01:90:01") != true) {
            needSetMac.value = true
        }
    }

    override fun bleStatus(status: BLEStatusEnum) {
        bleStatus.postValue(status)
        when (status) {
            BLEStatusEnum.connected -> {
                refreshMac()
                getBattery()
                getVersion()

                isTrueMac()
            }
        }
    }

    override fun bleWork(type: BLEWorkEnum, data: Any?) {
        when (type) {
            BLEWorkEnum.rssi -> {
                if (data is Int) {
                    loading.value = false
                    bleRssiResult.postValue(data < (mLocalRssi.value ?: "0").toInt())
                    bleRssi.postValue(data.toString())
                }
            }
            BLEWorkEnum.byteArray -> {
                if (data is ByteArray) {
                    when (ParseBLEIO.getBleDataType(data = data)) {
                        ParseBLEIO.BLEDataEnum.check_version -> {
                            loading.value = false
                            val version = ParseBLEIO.parseVersion(data)
                            bleVersion.value = version
                            log("version[$version],localVersion[${mLocalVersion.value}]")
                            bleVersionResult.value = version != mLocalVersion.value
                            if (bleRssi.value.isNullOrBlank()) {
                                getRssi()
                            }
                        }
                        ParseBLEIO.BLEDataEnum.check_battery -> {
                            loading.value = false
                            bleBattery.value = ParseBLEIO.parseBattery(data)
                        }

                        ParseBLEIO.BLEDataEnum.set_mac -> {
                            loading.value = false
                            bleSetMacResult.value = true
                            mNewMac.value = newMac ?: ""
                            getBleManager().disconectBLE()
                        }
                        ParseBLEIO.BLEDataEnum.set_manual_feed -> {
                            loading.value = false
                            when (ParseBLEIO.parseManualFeed(data)) {
                                0 -> bleFeedResult.value = "喂食成功"
                                1 -> bleFeedResult.value = "电机故障（设备异常）"
                                2 -> bleFeedResult.value = "过量出食（堵粮）"
                            }
                        }
                        ParseBLEIO.BLEDataEnum.set_feed_sound -> {
                            when (data[3].toInt()) {
                                1 -> {
                                    mRecordValue.value = "开始录音"
                                }
                                2 -> {
                                    mRecordValue.value = "删除录音"
                                }
                                3 -> {
                                    mRecordValue.value = "播放录音"
                                    loading.value = false
                                }
                                0xa -> {
                                    loading.value = false
                                    mRecordValue.value = "录音结束"
                                    launch {
                                        withContext(Dispatchers.IO) {
                                            delay(500)
                                        }
                                        sendData(BLEIO.setFeedSound(BLEIO.FeedSoundEnum.play, 0))
                                    }

                                }
                            }
                        }
                        ParseBLEIO.BLEDataEnum.set_led -> {
                            loading.value = false
                            if (mLedIndex == 1) {
                                mLedStatus.value = "红灯\n开启"
                            } else {
                                mLedStatus.value = "红灯\n恢复正常"
                            }
                        }
                    }
                }
            }
            BLEWorkEnum.sendTimeOut -> {

            }
        }
    }

    override fun onDestory() {
        getBleManager().removeIBLEStatus(this).removeIBLEWork(this).disconectBLE()
        super.onDestory()
    }
}