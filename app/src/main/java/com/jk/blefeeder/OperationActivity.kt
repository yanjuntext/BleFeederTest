package com.jk.blefeeder

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import cn.p2ppetcam.weight.dialog.CommonDialog
import cn.p2ppetcam.weight.dialog.NewCommonDialog
import cn.p2ppetcam.weight.dialog.base.SDialog
import com.base.utils.clickDelay
import com.base.utils.permission.OnPermission
import com.base.utils.permission.Permission
import com.base.utils.permission.SPermissions
import com.google.android.material.snackbar.Snackbar
import com.jk.blefeeder.base.BaseActivity
import com.jk.blefeeder.ble.BLEStatusEnum
import com.jk.blefeeder.ble.io.FanMode
import com.jk.blefeeder.ble.utils.BLEUtils
import com.jk.blefeeder.viewmodel.OperationViewModel
import com.king.zxing.Intents
import com.wyj.base.log
import com.wyj.base.setStatusBarHeight
import com.wyj.base.startActionForResult
import kotlinx.android.synthetic.main.activity_operation.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class OperationActivity : BaseActivity<OperationViewModel>() {
    private val mDisconnectDialog by lazy {
        CommonDialog.Builder(this)
            .setCancleable(false)
            .setOutClickCancleable(false)
            .setContent("蓝牙连接断开，请重新连接")
            .setConfirm("重新连接", object : SDialog.OnConfirmListrener {
                override fun onCaonfirm(dialog: SDialog?, any: Any?) {
                    showLoading(show = true)
                    mViewModel?.connect()
                }
            })
            .setCancle("取消", object : SDialog.OnCancleListener {
                override fun onCancle(dialog: SDialog?) {
                    showLoading(show = false)
                }
            })
            .builder()
    }

    private val mCloseDialog by lazy {
        CommonDialog.Builder(this)
            .setCancleable(false)
            .setOutClickCancleable(false)
            .setContent("蓝牙关闭，请打开蓝牙")
            .setConfirm("打开蓝牙", object : SDialog.OnConfirmListrener {
                override fun onCaonfirm(dialog: SDialog?, any: Any?) {
                    if (!BLEUtils.bluetoothEnable(this@OperationActivity)) {
                        BLEUtils.openBluetooth(this@OperationActivity)
                    }
                    mViewModel?.connect()
                }
            })
            .builder()
    }

    private val mSetMacDialog by lazy {
        CommonDialog.Builder(this)
            .setCancleable(false)
            .setOutClickCancleable(false)
            .setContent("设备未烧号，请先烧号！！！")
            .setConfirm("烧号", object : SDialog.OnConfirmListrener {
                override fun onCaonfirm(dialog: SDialog?, any: Any?) {
                    setMac()
                }
            })
            .builder()
    }

    private val mComDialog by lazy {
        NewCommonDialog.Builder(this)
            .setCancleable(false)
            .setOutClickCancleable(false)
            .builder()
    }


    private var mReconnectIndex = 0
    private var mUnDisconverIndex = 0

    override fun getLayoutId(): Int = R.layout.activity_operation

    override fun injectorViewModel() {
        super.injectorViewModel()
        mViewModel = ViewModelProvider(this)[OperationViewModel::class.java]
    }

    override fun initValue(savedInstanceState: Bundle?) {
        super.initValue(savedInstanceState)
        mViewModel?.setBleAddress(
            intent.getStringExtra("address") ?: "",
            intent.getStringExtra("name")
        )
    }


    override fun initView() {
        super.initView()
        status_bar.setStatusBarHeight(this)

        tv_battery.clickDelay {
            mViewModel?.getBattery()
        }

        tv_version.clickDelay {
            mViewModel?.getVersion()
        }

        tv_rssi.clickDelay {
            mViewModel?.getRssi()
        }

        tv_feed.clickDelay {
            mViewModel?.feed()
        }

        tv_record.clickDelay {
            mViewModel?.setRecordTime()
        }

        iv_back.clickDelay {
            finish()
        }

        tv_red_led.clickDelay {
            mViewModel?.setLed()
        }

        tv_scan.clickDelay {
            setMac()
        }

        tv_temp.clickDelay {
            mViewModel?.getTemp()
        }
        tv_fan_1.clickDelay {
            mViewModel?.fanMode?.value?.let {
                val fan = when (it.fan) {
                    0 -> 1
                    1 -> 0
                    2 -> 3
                    else -> 2
                }

                mViewModel?.setFanMode(FanMode(fan, it.speed, 1, it.autoFan, it.temp1, it.temp2))
            }
        }
        tv_fan_2.clickDelay {
            mViewModel?.fanMode?.value?.let {
                val fan = when (it.fan) {
                    0 -> 2
                    1 -> 3
                    2 -> 0
                    else -> 1
                }
                mViewModel?.setFanMode(FanMode(fan, it.speed, 1, it.autoFan, it.temp1, it.temp2))
            }
        }
    }

    private fun setMac() {
        SPermissions.with(this)
            .permission(Permission.CAMERA)
            .request(object : OnPermission {
                override fun hasPermission(granted: List<String>, isAll: Boolean) {
                    if (isAll) {

                        startActionForResult(ScanQrActivity::class.java, 1000)
                    } else {
                        showSnackBar("请打开相机权限")
                    }
                }

                override fun noPermission(denied: List<String>, quick: Boolean) {
                    showSnackBar("请打开相机权限")
                }

            })
    }

    private fun showSnackBar(msg: String) {
        val snackbar = Snackbar.make(main, msg, Snackbar.LENGTH_LONG)
        snackbar.view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent))
        snackbar.show()
    }

    override fun subscribeUi() {
        super.subscribeUi()
        mViewModel?.let { viewModel ->

            viewModel.mBleName.observe(this) {
                if (viewModel.isDu10B()) {
                    tv_temp.visibility = View.VISIBLE
                    ll_pack.visibility = View.VISIBLE

                    tv_battery.visibility = View.GONE
                    ll_feeder.visibility = View.GONE
                }
            }
            viewModel.temp.observe(this) {
                tv_temp.text = when {
                    it <= 0 -> "小于0度"
                    it >= 40 -> "大于40度"
                    else -> "$it 度"
                }
            }
            viewModel.fanMode.observe(this) { fanmode ->
                fanmode?.let {
                    tv_fan_1.text = "风扇1\n${if (it.fan == 1 || it.fan == 3) "开" else "关"}"
                    tv_fan_2.text = "风扇2\n${if (it.fan == 2 || it.fan == 3) "开" else "关"}"
                }
            }

            viewModel.bleStatus.observe(this) {
                log("bleStatus [$it],mReconnectIndex=[$mReconnectIndex]")
                when (it) {
                    BLEStatusEnum.connectting -> showLoading(show = true)
                    BLEStatusEnum.connected -> {
                        mDisconnectDialog.dismiss()
                        mReconnectIndex = 0
                        mUnDisconverIndex = 0
                        showLoading(show = false)
                        showSnackBar("连接成功")

                    }
                    BLEStatusEnum.disconnect -> {
                        showLoading(show = false)
                        if (mViewModel?.mNewMac?.value?.isEmpty() == false) {
                            mReconnectIndex++
                            if (mReconnectIndex >= 3) {
                                mDisconnectDialog.show(this)
                            } else {
                                mViewModel?.connect()
                            }
                            showSnackBar("正在连接新的MAC地址")
                        } else {
                            mDisconnectDialog.setContent("蓝牙连接断开，请重新连接").show(this)
                        }

                    }
                    BLEStatusEnum.close -> {
                        showLoading(show = false)
                        mCloseDialog.show(this)
                    }
                    BLEStatusEnum.unDIscoverService -> {
                        if (mViewModel?.mNewMac?.value?.isEmpty() == false) {
                            mUnDisconverIndex++
                            if (mUnDisconverIndex >= 3) {
                                mDisconnectDialog.setContent("未发现服务，请重新连接").show(this)
//                                mDisconnectDialog.show(this)
                            } else {
                                showLoading(show = true)
                                mViewModel?.connectNewMac()
                            }
                            showSnackBar("正在连接新的MAC地址")
                        } else {
                            mUnDisconverIndex++
                            if (mUnDisconverIndex >= 3) {
                                mDisconnectDialog.setContent("未发现服务，请重新连接").show(this)
//                                mDisconnectDialog.show(this)
                            } else {
                                showLoading(show = true)
                                mViewModel?.connect()
                            }
                        }
                    }
                    BLEStatusEnum.opened -> {

                    }
                }
            }

            viewModel.bleRssiResult.observe(this) {
                tv_rssi.isSelected = it
            }
            viewModel.bleRssi.observe(this) {
                tv_rssi.text = "信号强度:\n$it"
            }

            viewModel.bleVersionResult.observe(this) {
                tv_version.isSelected = it
            }
            viewModel.bleVersion.observe(this) {
                tv_version.text = "版本号:\n$it"
            }

            viewModel.bleBattery.observe(this) {
                tv_battery.isSelected = it <= 0
                tv_battery.text = "电池:\n" + if (it > 0) it else "未安装电池"
            }

            viewModel.bleFeedResult.observe(this) {
                tv_feed.isSelected = it != "喂食成功"
                tv_feed.text = "喂食\n$it"
                showSnackBar(it)
            }
            viewModel.bleSetMacResult.observe(this) {
                showSnackBar("设置MAC地址成功")
//                setResult(
//                    Activity.RESULT_OK, Intent().putExtra("new", mViewModel?.mNewMac?.value ?: "")
//                        .putExtra("old", mViewModel?.address?.value ?: "")
//                )
//                finish()
            }

            viewModel.mRecordValue.observe(this) {
                showSnackBar(it)
            }
            viewModel.mLedStatus.observe(this) {
                tv_red_led.text = it
            }

            viewModel.mNewMac.observe(this) {
                refreshMac()
            }
            viewModel.address.observe(this) {
                tv_current_mac.text = "当前MAC:\n$it"
                refreshMac()
            }
            viewModel.needSetMac.observe(this) {
                if (it) {
                    mSetMacDialog.show(this)
                }
            }
            viewModel.mTcpResult.observe(this) {
                log("mTcpResult [$it]")
                if (it < 0) return@observe
                when (it) {
                    0 -> {
                        //烧号并且提交到服务器成功

                        tv_new_mac.isSelected = false
                    }
                    1 -> {
                        // "产品号服务器不支持"
                        tv_new_mac.isSelected = true
                        mComDialog.apply {
                            content = "烧好失败：产品号服务器不支持"
                            confirm = "请重试"
                            onConfirmListrener = object : SDialog.OnConfirmListrener {
                                override fun onCaonfirm(dialog: SDialog?, any: Any?) {

                                }
                            }
                        }.show(this)
                    }
                    2 -> {
                        // "服务器没有可用 UID"
                        tv_new_mac.isSelected = true
                        mComDialog.apply {
                            content = "烧号失败：服务器没有可用 UID"
                            confirm = "请重试"
                            onConfirmListrener = object : SDialog.OnConfirmListrener {
                                override fun onCaonfirm(dialog: SDialog?, any: Any?) {

                                }
                            }
                        }.show(this)
                    }
                    3 -> {
                        //服务器里相同 UID 已烧录过
                        tv_new_mac.isSelected = true
                        mComDialog.apply {
                            content = "烧号失败：服务器里相同 UID 已烧录过"
                            confirm = "请重试"
                            onConfirmListrener = object : SDialog.OnConfirmListrener {
                                override fun onCaonfirm(dialog: SDialog?, any: Any?) {

                                }
                            }
                        }.show(this)
                    }
                    4 -> {
                        //服务器系统错误
                        tv_new_mac.isSelected = true
                        mComDialog.apply {
                            content = "烧号失败：服务器系统错误"
                            confirm = "请重试"
                            onConfirmListrener = object : SDialog.OnConfirmListrener {
                                override fun onCaonfirm(dialog: SDialog?, any: Any?) {

                                }
                            }
                        }.show(this)
                    }
                    5 -> {
                        //服务器连接断开
                        tv_new_mac.isSelected = true
                        mComDialog.apply {
                            content = "服务器连接断开，请检查网络连接是否正常并重新连接"
                            confirm = "重新连接"
                            onConfirmListrener = object : SDialog.OnConfirmListrener {
                                override fun onCaonfirm(dialog: SDialog?, any: Any?) {
                                    mViewModel?.tcpReconnect()
                                }
                            }
                        }.show(this)
                    }
                    6 -> {
                        //可以烧号
                    }
                    7 -> {
                        //不可以烧号
                        tv_new_mac.isSelected = true
                        mComDialog.apply {
                            content = "当前MAC地址不可用，请更换新的MAC地址"
                            confirm = "重新扫码"
                            onConfirmListrener = object : SDialog.OnConfirmListrener {
                                override fun onCaonfirm(dialog: SDialog?, any: Any?) {
                                    setMac()
                                }
                            }
                        }.show(this)
                    }
                    8 -> {
                        //服务器连接成功
                        showSnackBar("服务器连接成功，可以正常工作啦！！！")
                        if (mComDialog.isShowing()) {
                            mComDialog.dismiss()
                        }
                    }
                }
            }
            viewModel.mTcpDataError.observe(this){
                if(it == true){
                    mViewModel?.mTcpDataError?.value = false
                    mComDialog.apply {
                        content = "服务器返回内容不正确，请重新扫码"
                        confirm = "扫码"
                        onConfirmListrener = object : SDialog.OnConfirmListrener {
                            override fun onCaonfirm(dialog: SDialog?, any: Any?) {
                                setMac()
                            }
                        }
                    }.show(this)
                }
            }
        }
    }

    private fun refreshMac() {
        tv_new_mac.visibility = View.VISIBLE
        val current = mViewModel?.getCurrentMac() ?: ""
        val new = mViewModel?.getNewMac() ?: ""
        tv_new_mac.visibility = if (new.isEmpty()) View.GONE else View.VISIBLE
        tv_new_mac.isSelected = new != current
        tv_new_mac.text =
            "当前MAC:\n$current\n烧号MAC:\n$new\n条形码内容：\n${mViewModel?.mLongNewMac?.value ?: ""}"
    }

    override fun onDestroy() {
//        TcpManager.close()
        super.onDestroy()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1000 && resultCode == Activity.RESULT_OK) {
            val strMac = data?.getStringExtra(Intents.Scan.RESULT) ?: "0"
//            val strMac = (data?.getStringExtra("mac") ?: "0")
            Log.i("Operation", "scan qr mac[${strMac}]")
//            strMac.toInt(16)
            try {
//                val mac = strMac.toLong(16)
                val mac = strMac.toLong()
                Log.i("Operation", "int mac[$mac]]")
                Log.i("Operation", "int mac[${strMac.toLong().toString(16)}]] 111")

                mViewModel?.setMac(mac)
            } catch (e: Exception) {
                e.printStackTrace()
                showSnackBar("二维码内容不符合规格")
            }


        }
    }

}