package com.jk.blefeeder.ble.ota

/**
 *OTA 升级状态
 *@author abc
 *@time 2020/5/11 15:52
 */
enum class OTAStepEnum {
    start,//OTA启动
    tart_write,//开始写入
    sending,//OTA升级中
    send_last,//发送的最后一个包
    finish,//OTA升级结束
    nonu //默认状态
}