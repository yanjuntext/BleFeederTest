package com.jk.blefeeder.ble.io

import android.util.Log
import com.jk.blefeeder.TAG
import okhttp3.internal.and


/**
 *解析蓝牙数据
 *@author abc
 *@time 2020/4/28 16:33
 */
object ParseBLEIO {

    enum class BLEDataEnum(val value: Int) {
        set_auto_feed(0x1),
        set_manual_feed(0x2),
        check_auto_feed(0x3),
        check_feed_log(0x4),
        set_feed_sound(0x5),
        check_feed_sound(0x6),
        set_time(0x7),
        check_time(0x8),
        check_version(0x9),
        clear_feed_log(0xA),
        reset_dev(0xB),
        set_mac(0xC),
        check_battery(0xD),
        check_crc(0xE),
        check_mac(0xF),
        set_led(0x10),
        none(0)
    }

    fun getBleDataType(data: ByteArray?): BLEDataEnum {
        if (data == null || data.isEmpty() || data.size < 20
            || getCrc(data) != data[19].toInt()
        ) {
            return BLEDataEnum.none
        }
        return when (data[2].toInt()) {
            BLEDataEnum.set_auto_feed.value -> BLEDataEnum.set_auto_feed
            BLEDataEnum.set_manual_feed.value -> BLEDataEnum.set_manual_feed
            BLEDataEnum.check_auto_feed.value -> BLEDataEnum.check_auto_feed
            BLEDataEnum.check_feed_log.value -> BLEDataEnum.check_feed_log
            BLEDataEnum.set_feed_sound.value -> BLEDataEnum.set_feed_sound
            BLEDataEnum.check_feed_sound.value -> BLEDataEnum.check_feed_sound
            BLEDataEnum.set_time.value -> BLEDataEnum.set_time
            BLEDataEnum.check_time.value -> BLEDataEnum.check_time
            BLEDataEnum.check_version.value -> BLEDataEnum.check_version
            BLEDataEnum.clear_feed_log.value -> BLEDataEnum.clear_feed_log
            BLEDataEnum.reset_dev.value -> BLEDataEnum.reset_dev
            BLEDataEnum.set_mac.value -> BLEDataEnum.set_mac
            BLEDataEnum.check_battery.value -> BLEDataEnum.check_battery
            BLEDataEnum.check_crc.value -> BLEDataEnum.check_crc
            BLEDataEnum.check_mac.value -> BLEDataEnum.check_mac
            BLEDataEnum.set_led.value -> BLEDataEnum.set_led
            else -> BLEDataEnum.none
        }
    }

//    fun parseCheckAutoFeed(data: ByteArray): MutableList<FeedInfo> {
//        val list = mutableListOf<FeedInfo>()
//        if (data.size < 20) return list
//        list.add(FeedInfo(data[3].toInt(),
//                data[4].toInt(), data[5].toInt(),
//                data[6].toInt(),
//                data[7].toInt() == 1,
//                data[8].toInt() == 1))
//
//        list.add(FeedInfo(data[9].toInt(),
//                data[10].toInt(), data[11].toInt(),
//                data[12].toInt(),
//                data[13].toInt() == 1,
//                data[14].toInt() == 1))
//
//        return list
//    }
//
//    /**解析喂食记录*/
//    fun parseFeedLog(data: ByteArray): MutableList<FeedLogInfo> {
//        val list = mutableListOf<FeedLogInfo>()
//        val first = ByteArray(8)
//        val end = ByteArray(8)
//
//        System.arraycopy(data, 3, first, 0, 8)
//        System.arraycopy(data, 11, end, 0, 8)
//
//
//
//        if (!isSingFeedLogEnd(first)) {
//            val serviceType = when (first[7].toInt() and 15) {
//                0 -> {
//                    when (first[7].toInt() shr 4) {
//                        1 -> AVIOCTRLDEFs.USER_EVENT_FEED_PETS_TIMER_FEED
//                        2 -> AVIOCTRLDEFs.USER_EVENT_FEED_PETS_MANUL_FEED
//                        else -> AVIOCTRLDEFs.USER_EVENT_FEED_PETS_FEED_BUTTON
//                    }
//                }
//                1 -> AVIOCTRLDEFs.USER_EVENT_FEED_PETS_FEED_WARNING
//                else -> AVIOCTRLDEFs.USER_EVENT_FEED_PETS_OUT_FOOD_WARING
//            }
//            cn.P2PPetCam.www.MLog.i("ParseBLEIO", "serviceType[$serviceType],resule[${first[7].toInt() and 15}],type[${end[7].toInt() shr 4}]")
//            list.add(FeedLogInfo(first[0].toInt(),
//                    first[1].toInt(),
//                    first[2].toInt(),
//                    first[3].toInt(),
//                    first[4].toInt(),
//                    first[5].toInt(),
//                    first[6].toInt(),
//                    serviceType))
//        }
//
//        if (!isSingFeedLogEnd(end)) {
//            val serviceType = when (end[7].toInt() and 15) {
//                0 -> {
//                    when (end[7].toInt() shr 4) {
//                        1 -> AVIOCTRLDEFs.USER_EVENT_FEED_PETS_TIMER_FEED
//                        2 -> AVIOCTRLDEFs.USER_EVENT_FEED_PETS_MANUL_FEED
//                        else -> AVIOCTRLDEFs.USER_EVENT_FEED_PETS_FEED_BUTTON
//                    }
//                }
//                1 -> AVIOCTRLDEFs.USER_EVENT_FEED_PETS_FEED_WARNING
//                else -> AVIOCTRLDEFs.USER_EVENT_FEED_PETS_OUT_FOOD_WARING
//            }
//            cn.P2PPetCam.www.MLog.i("ParseBLEIO", "serviceType[$serviceType],resule[${end[7].toInt() and 15}],type[${end[7].toInt() shr 4}]")
//            list.add(FeedLogInfo(end[0].toInt(),
//                    end[1].toInt(),
//                    end[2].toInt(),
//                    end[3].toInt(),
//                    end[4].toInt(),
//                    end[5].toInt(),
//                    end[6].toInt(),
//                    serviceType))
//        }
//
//        return list
//    }

    fun isFeedLogEnd(data: ByteArray) =
        data.size >= 20 && ((data[3] + data[4] + data[5] + data[6] + data[7] + data[8] == 0) ||
                (data[11] + data[12] + data[13] + data[14] + data[15] + data[16] == 0))

    private fun isSingFeedLogEnd(data: ByteArray) =
        data.size >= 8 && data[0] + data[1] + data[2] + data[3] + data[4] + data[5] == 0

    /**是否有录音*/
    fun hasSound(data: ByteArray?) =
        if (data == null || data.size < 20 || data[2].toInt() != BLEDataEnum.check_feed_sound.value) false
        else data[3].toInt() == 1

    //检查数据是否合理
    fun checkCrc(data: ByteArray): Boolean =
        if (data.size < 20) false else
            getCrc(data) == data[19].toInt()

    /**解析手动喂食*/
    fun parseManualFeed(data: ByteArray): Int {
        if (data.size < 4) return 0
        return data[3].toInt()
    }

    /**解析电池电量*/
    fun parseBattery(data: ByteArray): Int {
        //data[3].toInt() == 1 外电使用中
        //使用外电时也要显示电量
        return data[4].toInt()
    }

    /**是否使用外电*/
    fun isCharging(data: ByteArray): Boolean =
        (data[2].toInt() == BLEDataEnum.check_battery.value) && (data[3].toInt() == 1)

    fun parseVersion(data: ByteArray): String =
        "${data[3].toInt()}.${data[4].toInt()}.${data[5].toInt()}"

    private val HEXES = "0123456789ABCDEF"
    //byte[] 转 String
    fun getHex(raw: ByteArray?, size: Int): String? {
        //        MLog.e(TAG, "get data from ble dev --------->");
        if (raw == null) {
            return null
        }
        val hex = StringBuilder(2 * raw.size)
        val i = 0
        for ((len, b) in raw.withIndex()) {
            hex.append(HEXES[b and 0xF0 shr 4]).append(HEXES[b and 0x0F]).append(" ")

            //            MLog.e(TAG, cn.P2PPetCam.www.AppBean.StringFormatUtil.Stringformat("raw[%d]== %d", len, b));
//            Log.i(TAG(),"hex[${hex}]")
            if (len + 1 >= size)
                break
        }

        //        MLog.e(TAG, "<--------------   get data from ble dev");
        return hex.toString()
    }

    fun byteToHex(byte:Byte): String {
        return StringBuffer().append(HEXES[byte and 0xF0 shr 4]).append(HEXES[byte and 0x0F]).toString()
    }

}