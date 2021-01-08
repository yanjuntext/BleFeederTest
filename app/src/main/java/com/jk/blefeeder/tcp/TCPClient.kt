package com.jk.blefeeder.tcp

import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.wyj.base.log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.net.Socket

/**
 * 作者：王颜军 on 2020/12/16 09:33
 * 邮箱：3183424727@qq.com
 */

@ExperimentalCoroutinesApi
object TcpManager : TcpObserver {
    private var mTcpClient: TCPClient? = null

    var mTcpObserver: TcpObserver? = null

    fun setDevelopmentType(development: Boolean) {
        if (mTcpClient == null) {
            mTcpClient = TCPClient(this)
        }
        mTcpClient?.setDevelopmentType(development)
    }

    fun sendData(header: ByteArray, data: ByteArray) {
        if(mTcpClient == null){
            setDevelopmentType(MMKV.defaultMMKV().decodeBool("app_type",false))
        }
        mTcpClient?.sendData(header, data)
    }

    fun close() {
        mTcpClient?.close()
    }

    override fun socketDisconnect() {
        mTcpObserver?.socketDisconnect()
    }

    override fun socketConnect() {
        mTcpObserver?.socketConnect()
    }

    override fun update(data: ByteArray, length: Int) {
        mTcpObserver?.update(data, length)
    }

    override fun sendResult(result: Boolean) {
        mTcpObserver?.sendResult(result)
    }


}

@ExperimentalCoroutinesApi
class TCPClient(private var observer: TcpObserver?) {
    //生产烧号地址
    private val SOCKET_IP_ADDRESS_PRODUCTION = "192.168.7.11"

    //研发烧号地址
    private val SOCKET_IP_ADDRESS_DEVELOPMENT = "192.168.5.248"

    //端口号
    private val SOCKET_SERVICE_PORT = 20005

    private var mSocket: Socket? = null

    private var mIsDevelopmentType: Boolean? = null

    private var mCurrentSocketIpAddress = SOCKET_IP_ADDRESS_PRODUCTION

    private var mConnectJob: Job? = null
    private var mSocketIOJob: Job? = null

    fun setDevelopmentType(development: Boolean) {
        log("setDevelopmentType [${development}],[$mIsDevelopmentType]")
        if (mIsDevelopmentType == development) return
        close()
        mIsDevelopmentType = development

        mCurrentSocketIpAddress =
            if (development) SOCKET_IP_ADDRESS_DEVELOPMENT else SOCKET_IP_ADDRESS_PRODUCTION

        connectSocket()
    }

    private fun connectSocket() {
        close()
        mConnectJob = GlobalScope.launch(Dispatchers.Main) {
            flow {
                this@TCPClient.log("connect 1")
                mSocket = Socket(mCurrentSocketIpAddress, SOCKET_SERVICE_PORT)
                this@TCPClient.log("connect 2")
                if (isConnect()) {
                    this@TCPClient.log("connect 3")
                    emit(true)
                } else {
                    emit(false)
                }
            }.flowOn(Dispatchers.IO)
                .catch {
                    it.printStackTrace()
                    observer?.socketDisconnect()
                    this@TCPClient.log("connect error [${it.message}]")
                }
                .onCompletion {
                    this@TCPClient.log("connect completion")
                }
                .collect {
                    this@TCPClient.log("collect [$it]")
                    if (it) {
                        observer?.socketConnect()
                    }
                }
        }
    }

    //收发数据
    fun sendData(header: ByteArray, data: ByteArray) {
        closeIOJob()
        mSocketIOJob = GlobalScope.launch(Dispatchers.Main) {
            flow {
                if (isConnect()) {
                    mSocket?.getOutputStream()?.let {
                        it.write(header)
                        it.write(data)
                        it.flush()
                        emit(TcpData.send(true))
                    }
                    mSocket?.getInputStream()?.let {
                        val bt = ByteArray(1024)
                        val length = it.read(bt)
                        emit(TcpData.receive(bt, length))
                    }
                } else {
                    throw Throwable("没有连接成功，请重新连接")
                }
            }.flowOn(Dispatchers.IO)
                .catch {
                    it.printStackTrace()
                    this@TCPClient.log("send error [${it.message}]")
                    observer?.sendResult(false)
                    observer?.socketDisconnect()
                    connectSocket()
                }
                .onCompletion {
                    this@TCPClient.log("send onCompletion")
                }
                .collect {
                    this@TCPClient.log("send collect [${it.out}],[${it.length}],[${it.data == null}]")
                    if (it.out) {
                        observer?.sendResult(true)
                    } else {
                        observer?.update(it.data ?: byteArrayOf(), it.length)
                    }
                }
        }
    }

    private fun isConnect() = mSocket?.isConnected == true && mSocket?.isClosed != true

    fun close() {
        try {
            closeConnectJob()
            closeIOJob()

            mSocket?.close()
            mSocket = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun closeConnectJob() {
        mConnectJob?.cancel()
        mConnectJob = null
    }

    private fun closeIOJob() {
        mSocketIOJob?.cancel()
        mSocketIOJob = null
    }
}


interface TcpObserver {
    fun socketDisconnect()
    fun socketConnect()
    fun update(data: ByteArray, length: Int)
    fun sendResult(result: Boolean)
}

class TcpData(val out: Boolean, val data: ByteArray? = null, val length: Int) {
    companion object {
        fun send(out: Boolean = true) = TcpData(out, null, 0)
        fun receive(data: ByteArray, length: Int) = TcpData(false, data, length)
    }
}

class JsonInfo(val ap_name: String = "XABle_", val version: String)

const val COMMAND_TYPES_AUTO_BURN_BLE_RESP = 0x21
const val COMMAND_TYPES_AUTO_BURN_BLE_RESULT_RESP = 0x23

/**解析数据*/
object ParseTcpIo {
    fun parseData(data: ByteArray, length: Int): Int {
        if (length > 12) {
            val bJtcp = ByteArray(4)
            System.arraycopy(data, 4, bJtcp, 0, 4)
            if (String(bJtcp) == "JTCP") {
                return Packet.byteArrayToInt_Little(data, 0)
            }
        }
        return -1
    }

    fun isSuccessed(data: ByteArray) = Packet.byteArrayToInt_Little(data, 12) == 0
}

//发送数据
object TcpSendIo {
    /**数据头*/
    fun sendHeader(cmd: Int, length: Int): ByteArray {
        val data = ByteArray(12)
        val cmd = Packet.intToByteArray_Little(cmd)
        System.arraycopy(cmd, 0, data, 0, 4)
        val jtcp = "JTCP".toByteArray()
        log("jtcp[${jtcp.size}]")
        System.arraycopy(jtcp, 0, data, 4, jtcp.size)
        val size = Packet.intToByteArray_Little(length)
        System.arraycopy(size, 0, data, 8, size.size)
        return data
    }

    /**是否可以烧号*/
    fun isUpgrade(sn: String, mcu: String, mode: String, version: String): ByteArray {

        val data = ByteArray(636)
        val bSn = sn.toByteArray()
        System.arraycopy(bSn, 0, data, 0, bSn.size)

        val bMcu = mcu.toByteArray()
        System.arraycopy(bMcu, 0, data, 40, bMcu.size)

        val bMode = mode.toByteArray()
        System.arraycopy(bMode, 0, data, 80, bMode.size)

        val bResult = Gson().toJson(JsonInfo(version = version)).toByteArray()
        System.arraycopy(bResult, 0, data, 124, bResult.size)

        return data
    }

    /**烧号成功*/
    fun upgradeSuccess(sn: String, mcu: String): ByteArray {
        val data = ByteArray(132)
        val result = Packet.intToByteArray_Little(0)
        System.arraycopy(result, 0, data, 0, result.size)

        val bSn = sn.toByteArray()
        System.arraycopy(bSn, 0, data, 4, bSn.size)

        val bMcu = mcu.toByteArray()
        System.arraycopy(bMcu, 0, data, 44, bMcu.size)

        return data
    }
}