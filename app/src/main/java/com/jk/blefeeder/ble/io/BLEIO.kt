package com.jk.blefeeder.ble.io

import android.util.Log
import com.jk.blefeeder.TAG
import com.wyj.base.log
import java.math.BigInteger
import kotlin.math.log


/**
 *
 *@author abc
 *@time 2020/4/28 16:04
 */

private const val BLE_HEAD_ONE = 0x4A.toByte()
private const val BLE_HEAD_TWO = 0x4B.toByte()

fun getCrc(data: ByteArray): Int {
    val size = data.size
    var crc = 0
    for (i in 0 until size - 1) {
        crc = crc xor data[i].toInt()
    }
    return crc
}

private val zeroMac by lazy { mutableListOf("", "0", "00", "000", "0000", "00000", "000000") }


object BLEIO {

    enum class FeedSoundEnum(val value: Int) {
        start(0x1), delete(0x2), play(0x3)
    }

    enum class DataTypeEnum(val value: Int) {
        req(0x1), resp(0xa)
    }

    const val SET_AUTO_FEED_CMD = 0x1
    const val SET_MANUAL_FEED_CMD = 0x2
    const val CHECK_AUTO_FEED_CMD = 0x3
    const val CHECK_FEED_LOG_CMD = 0x4
    const val SET_FEED_SOUND_CMD = 0x5
    const val CHECK_FEED_SOUND_CMD = 0x6
    const val SET_TIME_CMD = 0x7
    const val CHECK_TIME_CMD = 0x8
    const val CHECK_VERSION_CMD = 0x9
    const val CLEAR_FEED_LOG_CMD = 0xA
    const val RESET_CMD = 0xB
    const val SET_MAC_CMD = 0xC
    const val CHECK_BATTERY = 0xD
    const val CHECK_CRC = 0xE
    const val CHECK_MAC = 0xF
    const val SET_LED = 0x10


    private fun getData(cmd: Int): ByteArray {
        val data = ByteArray(20)
        data[0] = BLE_HEAD_ONE
        data[1] = BLE_HEAD_TWO
        data[2] = cmd.toByte()
        return data
    }

    /**
     * 设置喂食计划
     * @param index 第几餐
     * @param hour 小时
     * @param min 分钟
     * @param num 喂食份数
     * @param sound 喂食声音
     * @param feed 喂食开关
     */
    fun setAutoFeed(
        index: Int,
        hour: Int,
        min: Int,
        num: Int,
        sound: Boolean,
        feed: Boolean
    ): ByteArray {
        val data = getData(SET_AUTO_FEED_CMD)
        data[3] = index.toByte()
        data[4] = hour.toByte()
        data[5] = min.toByte()
        data[6] = num.toByte()
        data[7] = if (sound) 1 else 0
        data[8] = if (feed) 1 else 0
        data[19] = getCrc(data).toByte()
        return data
    }

    /**手动喂食*/
    fun setManualFeed(hour: Int, min: Int, num: Int): ByteArray {
        val data = getData(SET_MANUAL_FEED_CMD)
        data[3] = hour.toByte()
        data[4] = min.toByte()
        data[5] = num.toByte()
        data[19] = getCrc(data).toByte()
        return data
    }

    /**查询喂食计划*/
    fun setCheckAutoFeed(dataType: DataTypeEnum): ByteArray {
        val data = getData(CHECK_AUTO_FEED_CMD)
        data[3] = dataType.value.toByte()
        data[19] = getCrc(data).toByte()
        return data
    }

    //收到回复
    fun setCheckAutoFeedResp(index: Int): ByteArray {
        val data = getData(CHECK_AUTO_FEED_CMD)
        data[3] = DataTypeEnum.resp.value.toByte()
        data[4] = index.toByte()
        data[19] = getCrc(data).toByte()
        return data
    }

    /**喂食记录*/
    fun setCheckFeedLog(year: Int, month: Int, day: Int, hour: Int, min: Int, sec: Int): ByteArray {
        val data = getData(CHECK_FEED_LOG_CMD)
        data[3] = DataTypeEnum.req.value.toByte()
        data[4] = year.toByte()
        data[5] = month.toByte()
        data[6] = day.toByte()
        data[7] = hour.toByte()
        data[8] = min.toByte()
        data[9] = sec.toByte()
        data[19] = getCrc(data).toByte()
        return data
    }

    /**喂食记录 收到回复*/
    fun setCheckFeedLogResp(index: Int): ByteArray {
        val data = getData(CHECK_FEED_LOG_CMD)
        data[3] = DataTypeEnum.resp.value.toByte()
        data[4] = index.toByte()
        data[19] = getCrc(data).toByte()
        return data
    }

    /**设置喂食录音*/
    fun setFeedSound(soundType: FeedSoundEnum, time: Int): ByteArray {
        val data = getData(SET_FEED_SOUND_CMD)
        data[3] = soundType.value.toByte()
        data[4] = time.toByte()
        data[19] = getCrc(data).toByte()
        return data
    }

    /**查询喂食录音*/
    fun setCheckFeedSound(): ByteArray {
        val data = getData(CHECK_FEED_SOUND_CMD)
        data[3] = 1
        data[19] = getCrc(data).toByte()
        return data
    }

    /**设置时间*/
    fun setTime(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        min: Int,
        second: Int,
        week: Int
    ): ByteArray {
        val data = getData(SET_TIME_CMD)
        data[3] = year.toByte()
        data[4] = month.toByte()
        data[5] = day.toByte()
        data[6] = hour.toByte()
        data[7] = min.toByte()
        data[8] = second.toByte()
        data[9] = week.toByte()
        data[19] = getCrc(data).toByte()
        return data
    }

    /**查询时间*/
    fun setCheckTime(): ByteArray {
        val data = getData(CHECK_TIME_CMD)
        data[3] = 1
        data[19] = getCrc(data).toByte()
        return data
    }

    /**查询版本号*/
    fun setCheckVersion(): ByteArray {
        val data = getData(CHECK_VERSION_CMD)
        data[3] = 1
        data[19] = getCrc(data).toByte()
        return data
    }

    /**删除喂食记录*/
    fun clearFeedLog(): ByteArray {
        val data = getData(CLEAR_FEED_LOG_CMD)
        data[3] = 1
        data[19] = getCrc(data).toByte()
        return data
    }

    /**复位设备*/
    fun resetDev(): ByteArray {
        val data = getData(RESET_CMD)
        data[3] = 1
        data[19] = getCrc(data).toByte()
        return data
    }

    /**复位设备*/
    fun checkBattery(): ByteArray {
        val data = getData(CHECK_BATTERY)
        data[3] = 1
        data[19] = getCrc(data).toByte()
        return data
    }

    //crcValue[666237786],user[huangzx@jiaketech.cn]
    fun checkCrc(user: String): ByteArray {
        val data = getData(CHECK_CRC)
//
//        Log.i(TAG(),"ty crcValue[${CRC32.get(user.toByteArray()).toUpperCase(Locale.CHINA)}],user[$user]")
//        val cdata = CRC32.get(user.toByteArray()).toUpperCase(Locale.CHINA).toByteArray()
//        if(cdata.size > 16){
//            System.arraycopy(cdata,0,data,3,16)
//        }else{
//            System.arraycopy(cdata,0,data,3,cdata.size)
//        }
//        data[19] = getCrc(data).toByte()
        return data
    }


    fun setMac(mac: Long): ByteArray {
        val data = getData(SET_MAC_CMD)

        data[3] = 1
        data[4] = 144.toByte()
        data[5] = 1
        val hexMac = mac.toString(16)

        val strMac = zeroMac[6 - hexMac.length] + hexMac
        for (i in 0..2) {
            val index = strMac.substring(i * 2, (i + 1) * 2)
            Log.i(TAG(), "mac index[$index],int[${index.toInt(16)}]")
            data[5 + i + 1] = index.toInt(16).toByte()
        }
        data[19] = getCrc(data).toByte()
        Log.i(TAG(), "mac all[${ParseBLEIO.getHex(data, data.size)}]")
        return data
    }

    //指示灯检测
    fun setLed(led: Int): ByteArray {
        val data = getData(SET_LED)
        data[3] = led.toByte()
        data[19] = getCrc(data).toByte()
        return data
    }

}

/**猫背包*/

//温湿度检测
const val PACK_TEMP = 0x1

//风扇工作模式 查询
const val PACK_FAN = 0x2

//风扇工作模式 手动模式
const val PACK_FAN_MANUAL = 0x3

//风扇工作模式 自动模式
const val PACK_FAN_AUTO = 0x4

//版本查询
const val PACK_VERSION = 0x5

//OTA升级准备
const val PACK_OTA_START = 0x6

//设备Mac地址 查询/设置
const val PACK_MAC = 0x7

data class FanMode(
    var fan: Int,
    var speed: Int,
    var mode: Int,
    var autoFan: Int,
    var temp1: Int,
    var temp2: Int
)

object PackBleIo {

    private fun getData(cmd: Int): ByteArray {
        val data = ByteArray(20)
        data[0] = BLE_HEAD_ONE
        data[1] = BLE_HEAD_TWO
        data[2] = cmd.toByte()
        return data
    }

    /**温湿度*/
    fun getTemp(): ByteArray {
        val data = getData(PACK_TEMP)
        data[data.size - 1] = getCrc(data).toByte()
        return data
    }

    /**查询风扇工作模式*/
    fun getFan(): ByteArray {
        val data = getData(PACK_FAN)
        data[3] = 0
        data[data.size - 1] = getCrc(data).toByte()
        return data
    }

    /**设置风扇工作模式*/
    fun setFanMode(fan: Int, mode: Int, temp: Int, speed: Int): ByteArray {
        val data = getData(PACK_FAN)
        data[3] = 1
        data[4] = fan.toByte()
        data[5] = mode.toByte()
        data[6] = temp.toByte()
        data[7] = speed.toByte()
        data[data.size - 1] = getCrc(data).toByte()
        return data
    }

    /**手动模式*/
    fun setManualFanMode(fanMode: FanMode): ByteArray {
        val data = getData(PACK_FAN_MANUAL)
        data[3] = fanMode.fan.toByte()
        data[4] = fanMode.speed.toByte()
        data[data.size - 1] = getCrc(data).toByte()
        return data
    }

    /**自动模式*/
    fun setAutoFanMode(fanMode: FanMode): ByteArray {
        val data = getData(PACK_FAN_AUTO)
        data[3] = fanMode.autoFan.toByte()
        data[4] = fanMode.temp1.toByte()
        data[5] = fanMode.temp2.toByte()
        data[data.size - 1] = getCrc(data).toByte()
        return data
    }


    /**电池电量检测*/
//    fun getBattery(): ByteArray {
//        val data = getData(PACK_BATTERY)
//        data[data.size - 1] = getCrc(data).toByte()
//        return data
//    }
    /**设置MAC地址*/
    fun setPackMac(mac: Long): ByteArray? {
        val data = getData(PACK_MAC)

        data[3] = 1

        data[4] = 1
        data[5] = 144.toByte()
        data[6] = 1
        val hexMac = mac.toString(16)
        if (6 - hexMac.length < 0) {
            return null
        }
        val strMac = zeroMac[6 - hexMac.length] + hexMac
        for (i in 0..2) {
            val index = strMac.substring(i * 2, (i + 1) * 2)
            Log.i(TAG(), "mac index[$index],int[${index.toInt(16)}]")
            data[6 + i + 1] = index.toInt(16).toByte()
        }
        data[19] = getCrc(data).toByte()
        Log.i(TAG(), "mac all[${ParseBLEIO.getHex(data, data.size)}]")
        return data
    }

    /**版本查询*/
    fun getVersion(): ByteArray {
        val data = getData(PACK_VERSION)
        data[data.size - 1] = getCrc(data).toByte()
        return data
    }

    fun otaBefore(): ByteArray {
        val data = getData(PACK_OTA_START)
        data[data.size - 1] = getCrc(data).toByte()
        return data
    }

    //设置FW 设备MAC地址
    fun setFWPackMac(mac: Long): ByteArray? {
        val data = ByteArray(20)
        data[0] = 8;
        data[1] = 0x11
        data[2] = 7
        data[3] = 1

        data[4] = 1
        data[5] = 144.toByte()
        data[6] = 1
        val hexMac = mac.toString(16)
        if (6 - hexMac.length < 0) {
            return null
        }
        val strMac = zeroMac[6 - hexMac.length] + hexMac
        for (i in 0..2) {
            val index = strMac.substring(i * 2, (i + 1) * 2)
            Log.i(TAG(), "mac index[$index],int[${index.toInt(16)}]")
            data[6 + i + 1] = index.toInt(16).toByte()
        }
        data[19] = getCrc(data).toByte()
        Log.i(TAG(), "mac all[${ParseBLEIO.getHex(data, data.size)}]")
        return data
    }
}