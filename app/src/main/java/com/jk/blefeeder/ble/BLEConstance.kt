package com.jk.blefeeder.ble

/**
 *
 *@author abc
 *@time 2020/4/26 18:14
 */

/**
 * 蓝牙连接成功
 */
const val ACTION_GATT_CONNECTED = "com.P2PPetCam.www.ACTION_GATT_CONNECTED"
/**
 * 蓝牙断开连接
 */
const val ACTION_GATT_DISCONNECTED = "com.P2PPetCam.www.ACTION_GATT_DISCONNECTED"

/**
 * 发现蓝牙服务
 */
const val ACTION_GATT_SERVICES_DISCOVERED = "com.P2PPetCam.www.ACTION_GATT_SERVICES_DISCOVERED"
/**
 * 蓝牙数据发生改变
 */
const val ACTION_GATT_DATA_AVAILABLE = "com.P2PPetCam.www.ACTION_GATT_DATA_AVAILABLE"
/**
 * 蓝牙电量
 */
const val ACTION_GATT_BATTERY_AVAILABLE = "com.P2PPetCam.www.ACTION_GATT_BATTERY_AVAILABLE"
/**
 * 蓝牙信号强度
 */
const val ACTION_GATT_RSSI_EXTRA = "com.P2PPetCam.www.ACTION_GATT_RSSI_EXTRA"
const val ACTION_GATT_DATA_EXTRA = "com.P2PPetCam.www.ACTION_GATT_DATA_EXTRA"
//版本号
const val ACTION_GATT_VERSION_CODE = "com.P2PPetCam.www.ACTION_GATT_VERSION_CODE"
const val ACTION_GATT_CHARGING_CODE = "com.P2PPetCam.www.ACTION_GATT_CHARGING_CODE"


enum class BLEStatusEnum(value: Int) {
    close(1),//蓝牙关闭
    opened(2),//蓝牙打开
    connectting(3),//蓝牙正在连接
    connected(4),//蓝牙连接成功
    disconnect(5),//蓝牙断开连接
    reconnect(6),//重接
    discoverService(7),//发现服务
    unDIscoverService(8)//没有发现服务
}


enum class BLEWorkEnum(value: Int) {
    openNotify(0),
    battery(1),
    rssi(2),
    byteArray(3),
    sendTimeOut(4)
}

