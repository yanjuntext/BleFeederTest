package com.jk.blefeeder.ble.ota

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.util.Log
import com.jk.blefeeder.ble.BLEUuids
import com.jk.blefeeder.ble.impl.IBLEOTA
import com.jk.blefeeder.ble.io.ParseBLEIO
import java.lang.ref.WeakReference

/**
 * 盛芯微 ota升级
 *@author abc
 *@time 2020/5/11 14:43
 */
class SYDBLEOtaHelper {
    private val TAG by lazy { SYDBLEOtaHelper::class.java.simpleName }


    private val MAX_TRAN_COUNT_V30 = 20//ota 每包发送最大值
    val MAX_TRANS_SECTIONALL_COUNT = 5120
    val MAX_TRANS_SECTIONALL_PACKET_COUNT = MAX_TRANS_SECTIONALL_COUNT / 20
    var MAX_TRANS_SECTIONALL_SIZE = 0
    private var mOtaPacketAllCount = 0//总共份多少包
    private var mOtaFile: ByteArray? = null
    private var Crc = 0
    private var mSendPacketID = 0
    private var mSectionCrc = 0
    private var mSendSectionID = 0

    private var mGatt:
            WeakReference<BluetoothGatt?>? = null

    private var mOtaGattService: BluetoothGattService? = null
    private var mOtaGattUpdate: BluetoothGattCharacteristic? = null
    private var mOtaListeter: WeakReference<IBLEOTA>? = null


    private var mOtaStatus = OTAStatusEnum.close
    private var mOtaStep = OTAStepEnum.nonu

    private var readData = false


    //设置Gatt
    fun setBluetoothGatt(gatt: BluetoothGatt?) {
        mGatt = WeakReference(gatt)
    }

    //发现服务
    fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        mOtaGattService = gatt?.getService(BLEUuids.getSYDOTAServiceUuid())
        mOtaGattUpdate = mOtaGattService?.getCharacteristic(BLEUuids.getSYDOTACharacteristicReadUuid())
    }

    fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
        Log.i(TAG, "onCharacteristicWrite[$mOtaPacketAllCount],readData[$readData],step[$mOtaStep]")
        if (OTAStatusEnum.open == mOtaStatus) {
            if (!readData) {
                nextOta(0, null)
            } else {
                readData()
            }
        }
    }

    fun onCharacteristicRead(att: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
        Log.i(TAG, "onCharacteristicRead[$mOtaPacketAllCount],readData[$readData],step[$mOtaStep]")
        if (readData && OTAStatusEnum.open == mOtaStatus) {
            nextOta(status, characteristic?.value)
        }
    }

    //OTA  第一步 开启
    fun startOta(file: String?): Boolean {
        if (mOtaGattService == null || mOtaGattUpdate == null || mGatt?.get() == null
                || file.isNullOrEmpty()) {
            mOtaListeter?.get()?.bleOTAStart(false)
            return false
        }
        if (!initOtaFileDatas(file)) {
            mOtaListeter?.get()?.bleOTAStart(false)
            return false
        }
        mOtaListeter?.get()?.bleOTAStart(true)
        mOtaStatus = OTAStatusEnum.open
        mOtaStep = OTAStepEnum.start
        Log.i(TAG, "sendData startOta 1")
        return if (sendData(SYDOTAIO.getStartOta(), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)) {
            true
        } else {
            mOtaListeter?.get()?.bleOTAStart(false)
            mOtaListeter?.get()?.bleOtaResult(false)
            close()
            false
        }
    }

    private fun nextOta(status: Int, data: ByteArray?) {
        when (mOtaStep) {
            OTAStepEnum.start -> {
                sendFirstOta(data)
            }
            OTAStepEnum.tart_write -> {
                sendAllOta(status)
            }
            OTAStepEnum.sending -> {
                sendFlashContinue(data)
            }
            OTAStepEnum.send_last -> {
                sendLastOta()
            }
            OTAStepEnum.finish -> {
                otaFinish(data)
            }
        }
    }

    //第二步 发送第一个包
    private fun sendFirstOta(data: ByteArray?) {

        mOtaPacketAllCount = (mOtaFile?.size ?: 0) / MAX_TRAN_COUNT_V30 + if ((mOtaFile?.size
                        ?: 0) % MAX_TRAN_COUNT_V30 != 0) 1 else 0
        Log.i(TAG, "mOtaPacketAllCount[$mOtaPacketAllCount]")
        sendFlashContinue(data)
        refreshProgress()
    }

    //第三步
    private fun sendAllOta(status: Int) {
        Log.i(TAG, "sendAllOta status[$status]")
        if (status == 0) {
            val srcPos = mSendPacketID * MAX_TRAN_COUNT_V30
            Log.i(TAG, "srcPos[$srcPos],id[${mSendPacketID}],max[$MAX_TRAN_COUNT_V30],count[$mOtaPacketAllCount]")
            val dataPacket = ByteArray(MAX_TRAN_COUNT_V30)
            when (mSendPacketID) {
                mOtaPacketAllCount -> mOtaStep = OTAStepEnum.nonu
                mOtaPacketAllCount - 1 -> {
                    //fas最后一个包
                    mOtaFile?.let {
                        System.arraycopy(
                            it, srcPos, dataPacket, 0, (mOtaFile?.size
                                ?: 0) - srcPos)
                    }
                    mOtaStep = OTAStepEnum.send_last
                }
                else -> {
                    mOtaFile?.let { System.arraycopy(it, srcPos, dataPacket, 0, MAX_TRAN_COUNT_V30) }
                }
            }

            if (mOtaStep != OTAStepEnum.nonu) {
                Log.i(TAG, "sendData sendAllOta 3")
                sendData(dataPacket, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
                mSendPacketID += 1
            }

            if (mOtaStep != OTAStepEnum.send_last && mSendPacketID % MAX_TRANS_SECTIONALL_PACKET_COUNT == 0) {
                mOtaStep = OTAStepEnum.sending
                readData = true
            }
            mOtaListeter?.get()?.bleOtaSending(true)
            refreshProgress()
        } else {
            mOtaListeter?.get()?.bleOtaSending(false)
            mOtaListeter?.get()?.bleOtaResult(false)
            close()
        }
    }

    //第四步  ACTIONTYPE_CMD_FW_WRITE_END
    private fun sendFlashContinue(data: ByteArray?) {
        mOtaStep = OTAStepEnum.tart_write
        if (mSendSectionID != 0 && data != null && data.size >= 8) {
            val check: Int = ((data[7].toInt() and 0xff) shl 8) or (data[6].toInt() and 0xff)
            //error check and resend
            //error check and resend
            if (check and 0x0000ffff != (mSectionCrc and 0x0000ffff)) {
                mSendSectionID -= 1
                mSendPacketID = MAX_TRANS_SECTIONALL_PACKET_COUNT * mSendSectionID
            }
        }
        MAX_TRANS_SECTIONALL_SIZE = if ((mOtaPacketAllCount - mSendPacketID) > MAX_TRANS_SECTIONALL_PACKET_COUNT)
            MAX_TRANS_SECTIONALL_COUNT
        else (mOtaFile?.size ?: 0) % MAX_TRANS_SECTIONALL_COUNT
        mSectionCrc = 0

        for (i in 0 until MAX_TRANS_SECTIONALL_SIZE) {
            mSectionCrc += ((mOtaFile?.get(mSendSectionID * MAX_TRANS_SECTIONALL_COUNT + i)
                    ?: 0).toInt() and 0x000000FF)
        }
        Log.i(TAG, "sendData sendFlashContinue 2")
        if (sendData(
                SYDOTAIO.getWriteSectionStart(
                    mSectionCrc,
                    MAX_TRANS_SECTIONALL_SIZE,
                    mSendSectionID * MAX_TRANS_SECTIONALL_COUNT
                ), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)) {
            mOtaListeter?.get()?.writeSectionStart(true)
        } else {
            mOtaListeter?.get()?.writeSectionStart(false)
            mOtaListeter?.get()?.bleOtaResult(false)
            close()
        }
        mSendSectionID += 1
        readData = false
    }

    //第五步
    private fun sendLastOta() {
        mOtaStep = OTAStepEnum.finish
        readData = true
        Log.i(TAG, "sendData sendLastOta 4")
        if (sendData(
                SYDOTAIO.getSendLastOta(
                    mOtaFile?.size
                        ?: 0, Crc
                ), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)) {
            mOtaListeter?.get()?.bleOtaSending(true)
        } else {
            mOtaListeter?.get()?.bleOtaSending(false)
            mOtaListeter?.get()?.bleOtaResult(false)
            close()
        }
    }

    //ota升级结束  ACTIONTYPE_CMD_FW_FINISH
    private fun otaFinish(data: ByteArray?) {
        Log.i(TAG, "otaFinish data[${data?.size}]")
        if (data != null) {
            Log.i(TAG, "otaFinish data [${ParseBLEIO.getHex(data, data.size)}]")
        }
        if ((data?.size ?: 0) < 4 || ((((data?.get(0)?.toInt()
                        ?: 0) and 0xFF) == 0x0E) && (((data?.get(3)?.toInt()
                        ?: 0) and 0xFF) == 0x01))) {
            mOtaListeter?.get()?.bleOtaResult(false)
        } else {
            mOtaListeter?.get()?.bleOtaResult(true)
            close()
        }
    }

    fun close() {
        readData = false
        mOtaPacketAllCount = 0
        mOtaFile = null
        Crc = 0
        mSendPacketID = 0
        mSectionCrc = 0
        mSendSectionID = 0
        MAX_TRANS_SECTIONALL_SIZE = 0
        mOtaStatus = OTAStatusEnum.close
        mOtaStep = OTAStepEnum.nonu
    }

    private fun refreshProgress() {
        mOtaListeter?.get()?.bleOtaProgress((mSendPacketID * 1.0 / mOtaPacketAllCount * 100).toInt())
    }

    private fun sendData(data: ByteArray, writeType: Int): Boolean {
        if (mOtaGattService == null || mOtaGattUpdate == null || mGatt?.get() == null) return false
        mOtaGattUpdate?.value = data
        mOtaGattUpdate?.writeType = writeType

        Log.i(TAG, "sendData [${ParseBLEIO.getHex(data, data.size)}]")

        mGatt?.get()?.writeCharacteristic(mOtaGattUpdate)
        return true
    }

    private fun readData() {
        mGatt?.get()?.readCharacteristic(mOtaGattUpdate)
    }


    fun getOtaUpdateCharacteristic() = mOtaGattUpdate

    //初始化OTA file 文件
    private fun initOtaFileDatas(filepath: String): Boolean {
        val datas = SYDOTAUtlis.getFileDatas(filepath)
        if (datas == null || datas.isEmpty()) {
            mOtaListeter?.get()?.bleOTAStart(false)
            return false
        }
        mOtaFile = ByteArray(datas.size)
        System.arraycopy(datas, 0, mOtaFile, 0, datas.size)
        Crc = SYDOTAUtlis.getCrc(mOtaFile)
        return true
    }

    fun setBleOtaListener(listener: IBLEOTA) {
        mOtaListeter = WeakReference(listener)
    }
}