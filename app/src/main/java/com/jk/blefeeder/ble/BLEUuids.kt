package com.jk.blefeeder.ble

import com.jk.blefeeder.ble.bean.BleDevTypeEnum
import java.util.*

/**
 *
 *@author abc
 *@time 2020/4/27 15:47
 */
object BLEUuids {

    //电量Service UUID
    val BATTERY_SERVICE_UUID = UUID.fromString("0000180F-0000-1000-8000-00805F9B34FB")
    //电量Characteristic UUID
    val BATTERY_CHARACTERISTIC_UUID = UUID.fromString("00002A19-0000-1000-8000-00805F9B34FB")
    //Descriptor 通知
    val NOTIFY_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    //骨头
    private val BONE_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    private val BONE_RX_CHARACTERISTIC_UUID =
        UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
    //notify 数据变化通知UUID
    private val BONE_TX_CHARACTERISTIC_UUID =
        UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")


    //猫窝
    private val CAT_ROOM_SERVICE_UUID = UUID.fromString("000000A1-0000-1000-8000-00805f9b34fb")
    private val CAT_ROOM_RX_CHARACTERISTIC_UUID =
        UUID.fromString("000000A2-0000-1000-8000-00805f9b34fb")
    private val CAT_ROOM_TX_CHARACTERISTIC_UUID =
        UUID.fromString("000000A3-0000-1000-8000-00805f9b34fb")

    //盛芯微 OTA
    private val CAT_ROOM_OTA_SERVICE_UUID =
        UUID.fromString("000000B1-0000-1000-8000-00805f9b34fb") // The UUID for service "FF00"
    private val CAT_ROOM_OTA_CHARACTERISTIC_READ_UUID =
        UUID.fromString("000000B2-0000-1000-8000-00805f9b34fb") // The UUID for service "FF01"

    //获取主服务UUID
    fun getBLEServiceUuid(type: BleDevTypeEnum) =
        when (type) {
            BleDevTypeEnum.bone -> BONE_SERVICE_UUID
            else -> CAT_ROOM_SERVICE_UUID
        }

    fun getBLERxCharacteristivUuid(type: BleDevTypeEnum) =
        when (type) {
            BleDevTypeEnum.bone -> BONE_RX_CHARACTERISTIC_UUID
            else -> CAT_ROOM_RX_CHARACTERISTIC_UUID
        }

    fun getBLETxCharacteristivUuid(type: BleDevTypeEnum) = when (type) {
        BleDevTypeEnum.bone -> BONE_TX_CHARACTERISTIC_UUID
        else -> CAT_ROOM_TX_CHARACTERISTIC_UUID
    }

    //获取盛芯微 OTA
    fun getSYDOTAServiceUuid() = CAT_ROOM_OTA_SERVICE_UUID

    fun getSYDOTACharacteristicReadUuid() = CAT_ROOM_OTA_CHARACTERISTIC_READ_UUID

}