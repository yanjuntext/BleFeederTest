package com.jk.blefeeder.ble.utils


/**
 *
 *@author abc
 *@time 2020/4/28 10:20
 */
object BLEUidUtils {
    private val TAG = BLEUidUtils::class.java.simpleName
    const val BLE_UID_PREFIX = "BLEDEVID"

//    fun addressToUid(address: String): String {
//        val sb = StringBuffer()
//        sb.append(Constant.BLE_UID_PREFIX)
//                .append(formatBleUID(address))
//        MLog.e(TAG, "addressToUid == $sb")
//        return sb.toString()
//    }
//
//    fun bleUidToAddress(bleUid: String) = if (bleUid.startsWith(Constant.BLE_UID_PREFIX)) {
//        val address = formatAddress(bleUid.replaceFirst(Constant.BLE_UID_PREFIX.toRegex(), "")) ?: ""
//        MLog.e(TAG, "getBleUID == $address")
//        address
//    } else bleUid


    private fun formatBleUID(devUID: String): String {
        return devUID.replace(":".toRegex(), "")
    }

    //格式化ble dev UID
    private fun formatAddress(devUID: String): String? {
        var devUID = devUID
        val regex = "(.{2})"
        devUID = devUID.replace(regex.toRegex(), "$1:")
        devUID = devUID.substring(0, devUID.length - 1)
        return devUID
    }
}