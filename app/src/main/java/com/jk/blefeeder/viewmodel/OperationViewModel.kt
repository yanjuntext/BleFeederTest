package com.jk.blefeeder.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.jk.blefeeder.TAG
import com.jk.blefeeder.base.BaseAndroidViewModel
import com.jk.blefeeder.ble.BLEManager
import com.jk.blefeeder.ble.BLEStatusEnum
import com.jk.blefeeder.ble.BLEWorkEnum
import com.jk.blefeeder.ble.bean.BLEDev
import com.jk.blefeeder.ble.bean.BleDevTypeEnum
import com.jk.blefeeder.ble.impl.IBLEScan
import com.jk.blefeeder.ble.impl.IBLEStatus
import com.jk.blefeeder.ble.impl.IBLEWork
import com.jk.blefeeder.ble.io.*
import com.jk.blefeeder.ble.utils.BLEUtils
import com.jk.blefeeder.room.AppDataBase
import com.jk.blefeeder.tcp.*
import com.tencent.mmkv.MMKV
import com.wyj.base.log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.*

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class OperationViewModel(app: Application) : BaseAndroidViewModel(app), IBLEStatus, IBLEWork,
    IBLEScan, TcpObserver {
    val mBleName = MutableLiveData<String?>()
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
    val mLongNewMac = MutableLiveData<Long>()
    private var newMac: String? = null

    val needSetMac = MutableLiveData<Boolean>()


    private var packJob: Job? = null

    private val mLocalDao by lazy { AppDataBase.getInstance(app).getLocalSerDao() }


    /**猫背包*/
    //温度
    val temp = MutableLiveData<Int>()
    val fanMode = MutableLiveData<FanMode?>()


    //TCP
    val mTcpResult = MutableLiveData<Int>()

    val mTcpDataError = MutableLiveData<Boolean?>()

    private fun getBleManager() =
        BLEManager.getInstance(
            app,
            getBleType()
        ).setBleType(
            getBleType()
        )

    private fun getBleType() = when {
        mBleName.value?.toUpperCase()?.contains("DU10") == true -> BleDevTypeEnum.du10b
        mBleName.value?.toUpperCase()?.startsWith("FW") == true -> BleDevTypeEnum.fw
        else -> BleDevTypeEnum.catroom
    }

    fun setBleAddress(address: String, name: String?) {
        log("address[$address],name[$name]")
        mBleName.value = name

        getBleManager().addIBLEStatus(this).addIBLEWork(this)
            .addIBleScan(this)
        this.address.value = address
        getLocalSet()

        connect()
        TcpManager.mTcpObserver = this
    }

    fun connect() {

        bleStatus.value = BLEStatusEnum.connectting
        log("connect [${address.value}],new[${mNewMac.value}]")
        if (mNewMac.value.isNullOrEmpty()) {
            log("connect old address")
            launch {
                withContext(Dispatchers.IO) {
                    delay(1500L)
                }
                getBleManager().connectBLE(address.value ?: "", 3)
            }
        } else {
            log("connect old newAddress")
            launch {
                withContext(Dispatchers.IO) {
                    delay(1500L)
                }
//                withContext(Dispatchers.Main) {
                getBleManager().startLeScan(mNewMac.value ?: "", 3, 7000L)
//                }
            }
        }
    }

    fun isDu10B() = mBleName.value?.toUpperCase()?.contains("DU10") ?: false

    fun isFw() = mBleName.value?.toUpperCase()?.startsWith("FW") ?: false
    fun connectNewMac() {
        getBleManager().connectBLE(mNewMac.value ?: "", 3)
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
        mLongNewMac.value = mac
//        if(isFw()){
//            val nMac = parseMac(mac)
//            if(nMac.isNotEmpty()){
//                startSetMac()
//            }
//        }else{
        tcpIsUpgrade(mac)
//        }
    }

    fun startSetMac() {
        val mac = mLongNewMac.value ?: 0L
        log("startSetMac [$mac]")
        when {
            isDu10B() -> setDu10BMac(mac)
            isFw() -> {
                setFWMac(mac)
            }
            else -> {
                setXAFMac(mac)
            }
        }
//        if (isDu10B()) {
//            val data = PackBleIo.setPackMac(mac)
//            if (data == null) {
//                errorMsg.value = "二维码内容不和规则"
//                return
//            }
//            val macData = ByteArray(6)
//            System.arraycopy(data, 4, macData, 0, 6)
//            Log.i(TAG(), "mac all[${ParseBLEIO.getHex(macData, macData.size)}]")
//            var strMac = ""
//            for (i in 0 until 6) {
//
//                strMac += ParseBLEIO.byteToHex(macData[i])
//                if (i != 5) {
//                    strMac += ":"
//                }
//            }
//            newMac = strMac
//            sendData(data)
//            Log.i(TAG(), "strMac all[${strMac}]")
//        } else {
//            val data = BLEIO.setMac(mac)
//            val macData = ByteArray(6)
//            System.arraycopy(data, 3, macData, 0, 6)
//            Log.i(TAG(), "mac all[${ParseBLEIO.getHex(macData, macData.size)}]")
//            var strMac = ""
//            for (i in 0 until 6) {
//                strMac += ParseBLEIO.byteToHex(macData[i])
//                if (i != 5) {
//                    strMac += ":"
//                }
//            }
//            newMac = strMac
//            sendData(data)
//            Log.i(TAG(), "strMac all[${strMac}]")
//        }
    }

    private fun setDu10BMac(mac: Long) {
        val data = PackBleIo.setPackMac(mac)
        if (data == null) {
            errorMsg.value = "二维码内容不和规则"
            return
        }
        val macData = ByteArray(6)
        System.arraycopy(data, 4, macData, 0, 6)
        Log.i(TAG(), "mac all[${ParseBLEIO.getHex(macData, macData.size)}]")
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

    private fun setFWMac(mac: Long) {
        val data = PackBleIo.setFWPackMac(mac)
        if (data == null) {
            errorMsg.value = "二维码内容不和规则"
            return
        }
        val macData = ByteArray(6)
        System.arraycopy(data, 4, macData, 0, 6)
        Log.i(TAG(), "mac all[${ParseBLEIO.getHex(macData, macData.size)}]")
        var strMac = ""
        for (i in 0 until 6) {

            strMac += ParseBLEIO.byteToHex(macData[i])
            if (i != 5) {
                strMac += ":"
            }
        }
        newMac = strMac
        sendData(data)
    }

    private fun setXAFMac(mac: Long) {
        val data = BLEIO.setMac(mac)
        val macData = ByteArray(6)
        System.arraycopy(data, 3, macData, 0, 6)
        Log.i(TAG(), "mac all[${ParseBLEIO.getHex(macData, macData.size)}]")
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

    /**查询当前Mac地址是否可用*/
    private fun tcpIsUpgrade(mac: Long) {
        val nMac = parseMac(mac)
        log("tcpIsUpgrade [$nMac],[${parseBleName()}],[${bleVersion.value}]")
        if (nMac.isNotEmpty()) {
            TcpManager.sendData(
                TcpSendIo.sendHeader(0x20, 156),
                TcpSendIo.isUpgrade(nMac, nMac, parseBleName(), bleVersion.value ?: "")
            )
        }
    }

    /**上报烧号成功*/
    private fun tcpToUpgradeSuccess(newMac: String) {
        log("tcpToUpgradeSuccess [$newMac]")
        TcpManager.sendData(
            TcpSendIo.sendHeader(0x22, 132),
            TcpSendIo.upgradeSuccess(newMac, newMac)
        )
    }

    private fun parseMac(mac: Long): String {
        val data = PackBleIo.setPackMac(mac)
        if (data == null) {
            errorMsg.value = "二维码内容不和规则"
            return ""
        }
        val macData = ByteArray(6)
        System.arraycopy(data, 4, macData, 0, 6)
        Log.i(TAG(), "mac all[${ParseBLEIO.getHex(macData, macData.size)}]")
        var strMac = ""
        for (i in 0 until 6) {

            strMac += ParseBLEIO.byteToHex(macData[i])
            if (i != 5) {
                strMac += ":"
            }
        }
        return strMac
    }

    fun tcpReconnect() {
        loadingTime.value = 3000
        TcpManager.setDevelopmentType(MMKV.defaultMMKV().decodeBool("app_type", false))
    }

    private fun parseBleName(): String {
        val name = mBleName.value ?: return ""
        val index = name.indexOf("_")
        return if (index > 0) {
            name.substring(0, index).toUpperCase()
        } else name.toUpperCase()

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

        if (new.isNotEmpty()) {
            tcpToUpgradeSuccess(new)
        }
    }

    fun getCurrentMac(): String? = address.value
    fun getNewMac(): String? = mNewMac.value


    private fun isTrueMac() {
        if (address.value?.startsWith("01:90:01") != true) {
            needSetMac.value = true
        }
    }


    /**猫背包*/
    //获取温度
    fun getTemp() {
        sendData(PackBleIo.getTemp())
    }

    //查询工作模式
    fun getFanMode() {
        sendData(PackBleIo.getFan())
    }


    fun setFanMode(fanmode: FanMode?) {
        if (fanmode == null) return
        log("setFanMode [$fanmode]")
        fanMode.value?.let {
            if (getBleManager().sendData(PackBleIo.setManualFanMode(fanmode))) {

                loadingTime.value = 1500
            }

        }
    }

    private fun refreshPackJob() {
        packJob?.cancel()
        packJob = null

        packJob = launch({
            flow {
                while (true) {
                    emit(1)
                    delay(10 * 1000L)
                }
            }.flowOn(Dispatchers.IO)
                .collect {
                    getTemp()
                    getFanMode()
                }
        }, Dispatchers.Main)
    }


    override fun bleStatus(status: BLEStatusEnum) {
        bleStatus.postValue(status)
        when (status) {
            BLEStatusEnum.connected -> {
                refreshMac()
                when {
                    isDu10B() -> refreshPackJob()
                    isFw() -> {

                    }
                    else -> {
                        getBattery()
                        getVersion()
                    }
                }

//                if (isDu10B()) {
//                    refreshPackJob()
//                } else {
//                    getBattery()
//                    getVersion()
//                }
                isTrueMac()
            }
            BLEStatusEnum.disconnect -> {
                packJob?.cancel()
                packJob = null
            }
        }
    }

    override fun bleWork(type: BLEWorkEnum, data: Any?) {
        when {
            isDu10B() -> {
                parseDu10B(type, data)
            }
            isFw() -> parseFW(type, data)
            else -> {
                parseXAF(type, data)
            }
        }
//        if (isDu10B()) {
//            when (type) {
//                BLEWorkEnum.rssi -> {
//                    if (data is Int) {
//                        loading.value = false
//                        bleRssiResult.postValue(data < (mLocalRssi.value ?: "0").toInt())
//                        bleRssi.postValue(data.toString())
//                    }
//                }
//                BLEWorkEnum.byteArray -> {
//                    if (data is ByteArray && data.size == 20) {
//                        when (ParsePackBleIo.getBleDataType(data)) {
//                            PACK_TEMP -> {
//                                temp.value = data[3].toInt()
//                            }
//                            PACK_FAN -> {
//                                //风扇模式
//                                if (data.size >= 8) {
//                                    val fan = data[3].toInt()
//                                    val speed = data[4].toInt()
//                                    val mode = data[5].toInt()
//                                    val autoFan = data[6].toInt()
//                                    val temp1 = data[7].toInt()
//                                    val temp2 = data[8].toInt()
//                                    val sFanMode =
//                                        fanMode.value ?: FanMode(
//                                            fan,
//                                            speed,
//                                            mode,
//                                            autoFan,
//                                            temp1,
//                                            temp2
//                                        )
//                                    with(sFanMode) {
//                                        this.fan = fan
//                                        this.speed = speed
//                                        this.mode = mode
//                                        this.autoFan = autoFan
//                                        this.temp1 = temp1
//                                        this.temp2 = temp2
//                                    }
//                                    fanMode.value = sFanMode
//                                    getRssi()
//                                    loading.value = false
//                                }
//                            }
//                            PACK_FAN_MANUAL -> {
//                                if (data.size == 20) {
//                                    fanMode.value?.let {
//                                        it.mode = 1
//                                        it.fan = data[3].toInt()
//                                        it.speed = data[4].toInt()
//                                        fanMode.value = it
//                                    }
//                                }
//                                loading.value = false
//                            }
//                            PACK_FAN_AUTO -> {
//                                if (data.size == 20) {
//                                    fanMode.value?.let {
//                                        it.mode = 0
//                                        it.autoFan = data[3].toInt()
//                                        it.temp1 = data[4].toInt()
//                                        it.temp2 = data[5].toInt()
//                                        fanMode.value = it
//                                    }
//                                }
//                                loading.value = false
//                            }
//                            PACK_VERSION -> {
//                                //版本号
//                                if (data.size >= 6) {
//                                    val version =
//                                        "${data[3].toInt()}.${data[4].toInt()}.${data[5].toInt()}"
//                                    bleVersion.value = version
//                                    log("version[$version],localVersion[${mLocalVersion.value}]")
//                                    bleVersionResult.value = version != mLocalVersion.value
//                                    if (bleRssi.value.isNullOrBlank()) {
//                                        getRssi()
//                                    }
//                                }
//                            }
//                            PACK_MAC -> {
//                                if (data[3].toInt() == 1) {
//                                    loading.value = false
//                                    mNewMac.value = newMac ?: ""
//                                    bleSetMacResult.value = true
//                                    getBleManager().disconectBLE()
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        } else {
//            when (type) {
//                BLEWorkEnum.rssi -> {
//                    if (data is Int) {
//                        loading.value = false
//                        bleRssiResult.postValue(data < (mLocalRssi.value ?: "0").toInt())
//                        bleRssi.postValue(data.toString())
//                    }
//                }
//                BLEWorkEnum.byteArray -> {
//                    if (data is ByteArray) {
//                        when (ParseBLEIO.getBleDataType(data = data)) {
//                            ParseBLEIO.BLEDataEnum.check_version -> {
//                                loading.value = false
//                                val version = ParseBLEIO.parseVersion(data)
//                                bleVersion.value = version
//                                log("version[$version],localVersion[${mLocalVersion.value}]")
//                                bleVersionResult.value = version != mLocalVersion.value
//                                if (bleRssi.value.isNullOrBlank()) {
//                                    getRssi()
//                                }
//                            }
//                            ParseBLEIO.BLEDataEnum.check_battery -> {
//                                loading.value = false
//                                bleBattery.value = ParseBLEIO.parseBattery(data)
//                            }
//
//                            ParseBLEIO.BLEDataEnum.set_mac -> {
//                                loading.value = false
//                                mNewMac.value = newMac ?: ""
//                                bleSetMacResult.value = true
//                                getBleManager().disconectBLE()
//                            }
//                            ParseBLEIO.BLEDataEnum.set_manual_feed -> {
//                                loading.value = false
//                                when (ParseBLEIO.parseManualFeed(data)) {
//                                    0 -> bleFeedResult.value = "喂食成功"
//                                    1 -> bleFeedResult.value = "电机故障（设备异常）"
//                                    2 -> bleFeedResult.value = "过量出食（堵粮）"
//                                }
//                            }
//                            ParseBLEIO.BLEDataEnum.set_feed_sound -> {
//                                when (data[3].toInt()) {
//                                    1 -> {
//                                        mRecordValue.value = "开始录音"
//                                    }
//                                    2 -> {
//                                        mRecordValue.value = "删除录音"
//                                    }
//                                    3 -> {
//                                        mRecordValue.value = "播放录音"
//                                        loading.value = false
//                                    }
//                                    0xa -> {
//                                        loading.value = false
//                                        mRecordValue.value = "录音结束"
//                                        launch {
//                                            withContext(Dispatchers.IO) {
//                                                delay(500)
//                                            }
//                                            sendData(
//                                                BLEIO.setFeedSound(
//                                                    BLEIO.FeedSoundEnum.play,
//                                                    0
//                                                )
//                                            )
//                                        }
//
//                                    }
//                                }
//                            }
//                            ParseBLEIO.BLEDataEnum.set_led -> {
//                                loading.value = false
//                                if (mLedIndex == 1) {
//                                    mLedStatus.value = "红灯\n开启"
//                                } else {
//                                    mLedStatus.value = "红灯\n恢复正常"
//                                }
//                            }
//                        }
//                    }
//                }
//                BLEWorkEnum.sendTimeOut -> {
//
//                }
//            }
//        }
    }

    private fun parseFW(type: BLEWorkEnum, data: Any?) {
        if (type != BLEWorkEnum.byteArray) return
        if (data is ByteArray) {
            when {
                data[0].toInt() == 8 -> {
                    if (data[3].toInt() == 1) {
                        //设置MAC地址
                        loading.value = false
                        mNewMac.value = newMac ?: ""
                        bleSetMacResult.value = true
                        getBleManager().disconectBLE()
                    }
                }
            }
        }
    }

    private fun parseXAF(type: BLEWorkEnum, data: Any?) {
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
                            mNewMac.value = newMac ?: ""
                            bleSetMacResult.value = true
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
                                        sendData(
                                            BLEIO.setFeedSound(
                                                BLEIO.FeedSoundEnum.play,
                                                0
                                            )
                                        )
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

    private fun parseDu10B(type: BLEWorkEnum, data: Any?) {
        when (type) {
            BLEWorkEnum.rssi -> {
                if (data is Int) {
                    loading.value = false
                    bleRssiResult.postValue(data < (mLocalRssi.value ?: "0").toInt())
                    bleRssi.postValue(data.toString())
                }
            }
            BLEWorkEnum.byteArray -> {
                if (data is ByteArray && data.size == 20) {
                    when (ParsePackBleIo.getBleDataType(data)) {
                        PACK_TEMP -> {
                            temp.value = data[3].toInt()
                        }
                        PACK_FAN -> {
                            //风扇模式
                            if (data.size >= 8) {
                                val fan = data[3].toInt()
                                val speed = data[4].toInt()
                                val mode = data[5].toInt()
                                val autoFan = data[6].toInt()
                                val temp1 = data[7].toInt()
                                val temp2 = data[8].toInt()
                                val sFanMode =
                                    fanMode.value ?: FanMode(
                                        fan,
                                        speed,
                                        mode,
                                        autoFan,
                                        temp1,
                                        temp2
                                    )
                                with(sFanMode) {
                                    this.fan = fan
                                    this.speed = speed
                                    this.mode = mode
                                    this.autoFan = autoFan
                                    this.temp1 = temp1
                                    this.temp2 = temp2
                                }
                                fanMode.value = sFanMode
                                getRssi()
                                loading.value = false
                            }
                        }
                        PACK_FAN_MANUAL -> {
                            if (data.size == 20) {
                                fanMode.value?.let {
                                    it.mode = 1
                                    it.fan = data[3].toInt()
                                    it.speed = data[4].toInt()
                                    fanMode.value = it
                                }
                            }
                            loading.value = false
                        }
                        PACK_FAN_AUTO -> {
                            if (data.size == 20) {
                                fanMode.value?.let {
                                    it.mode = 0
                                    it.autoFan = data[3].toInt()
                                    it.temp1 = data[4].toInt()
                                    it.temp2 = data[5].toInt()
                                    fanMode.value = it
                                }
                            }
                            loading.value = false
                        }
                        PACK_VERSION -> {
                            //版本号
                            if (data.size >= 6) {
                                val version =
                                    "${data[3].toInt()}.${data[4].toInt()}.${data[5].toInt()}"
                                bleVersion.value = version
                                log("version[$version],localVersion[${mLocalVersion.value}]")
                                bleVersionResult.value = version != mLocalVersion.value
                                if (bleRssi.value.isNullOrBlank()) {
                                    getRssi()
                                }
                            }
                        }
                        PACK_MAC -> {
                            if (data[3].toInt() == 1) {
                                loading.value = false
                                mNewMac.value = newMac ?: ""
                                bleSetMacResult.value = true
                                getBleManager().disconectBLE()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestory() {
        getBleManager().removeIBLEStatus(this).removeIBleScan(this).removeIBLEWork(this)
            .disconectBLE()
        super.onDestory()
    }

    override fun bleStartScanBefor() {

    }

    override fun bleScanResult(bleList: MutableList<BLEDev>) {
        if (!mNewMac.value.isNullOrBlank()) {
            launch {
                withContext(Dispatchers.IO) {
                    delay(1500L)
                }
                this@OperationViewModel.log("bleScanResult connect new mac dev")
//                withContext(Dispatchers.Main) {
                getBleManager().connectBLE(mNewMac.value ?: "", 3)
//                }
            }
        }
    }

    override fun bleScanning(bleDev: BLEDev) {
    }

    override fun onCleared() {
        packJob?.cancel()
        packJob = null
        super.onCleared()
    }

    override fun socketDisconnect() {
        loading.value = false
        errorMsg.value = "服务器断开连接，请重试"
        mTcpResult.value = 5
    }

    override fun socketConnect() {
        mTcpResult.value = 8
        loading.value = false
    }


    override fun update(data: ByteArray, length: Int) {
        if (length <= 0) {
            mTcpDataError.postValue(true)
            return
        }
        when (ParseTcpIo.parseData(data, length)) {

            COMMAND_TYPES_AUTO_BURN_BLE_RESP -> {
                val result = Packet.byteArrayToInt_Little(data, 12)
//                mTcpIsUpgrade.value = result
//                if (MideaParseIo.isSuccessed(data)) {
//                    startWriteCode()
//                }
                mTcpResult.value = result
                log("tcp 1 [$result]")
                if (ParseTcpIo.isSuccessed(data)) {
                    //可以烧号
                    mTcpResult.value = 6
                    startSetMac()
                } else {
                    mTcpResult.value = 7
                }
            }
            COMMAND_TYPES_AUTO_BURN_BLE_RESULT_RESP -> {
                val result = Packet.byteArrayToInt_Little(data, 12)
                log("tcp 2 [$result]")
                mTcpResult.value = result
//                mTcpToUpgradeSuccess.value = result
            }
        }
    }

    override fun sendResult(result: Boolean) {

    }
}