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
import cn.p2ppetcam.weight.dialog.base.SDialog
import com.base.utils.clickDelay
import com.base.utils.permission.OnPermission
import com.base.utils.permission.Permission
import com.base.utils.permission.SPermissions
import com.google.android.material.snackbar.Snackbar
import com.jk.blefeeder.base.BaseActivity
import com.jk.blefeeder.ble.BLEStatusEnum
import com.jk.blefeeder.ble.utils.BLEUtils
import com.jk.blefeeder.viewmodel.OperationViewModel
import com.wyj.base.setStatusBarHeight
import kotlinx.android.synthetic.main.activity_operation.*

class OperationActivity : BaseActivity<OperationViewModel>() {

    private val mDisconnectDialog by lazy {
        CommonDialog.Builder(this)
            .setCancleable(false)
            .setOutClickCancleable(false)
            .setContent("蓝牙连接断开，请重新连接")
            .setConfirm("重新连接", object : SDialog.OnConfirmListrener {
                override fun onCaonfirm(dialog: SDialog?, any: Any?) {
                    mViewModel?.connect()
                }
            })
            .setCancle("取消")
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

    private var mReconnectIndex = 0

    override fun getLayoutId(): Int = R.layout.activity_operation

    override fun injectorViewModel() {
        super.injectorViewModel()
        mViewModel = ViewModelProvider(this)[OperationViewModel::class.java]
    }

    override fun initValue(savedInstanceState: Bundle?) {
        super.initValue(savedInstanceState)
        mViewModel?.setBleAddress(intent.getStringExtra("address") ?: "")
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

    }

    private fun setMac() {
        SPermissions.with(this)
            .permission(Permission.CAMERA)
            .request(object : OnPermission {
                override fun hasPermission(granted: List<String>, isAll: Boolean) {
                    if (isAll) {
                        startActivityForResult(
                            Intent(
                                this@OperationActivity,
                                ScanQrActivity::class.java
                            ), 1000
                        )
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
            viewModel.bleStatus.observe(this) {
                when (it) {
                    BLEStatusEnum.connectting -> showLoading(show = true)
                    BLEStatusEnum.connected -> {
                        mReconnectIndex = 0
                        showLoading(show = false)

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
                            mDisconnectDialog.show(this)
                        }

                    }
                    BLEStatusEnum.close -> {
                        showLoading(show = false)
                        mCloseDialog.show(this)
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
        }
    }

    private fun refreshMac() {
        tv_new_mac.visibility = View.VISIBLE
        val current = mViewModel?.getCurrentMac() ?: ""
        val new = mViewModel?.getNewMac() ?: ""
        tv_new_mac.visibility = if (new.isEmpty()) View.GONE else View.VISIBLE
        tv_new_mac.isSelected = new != current
        tv_new_mac.text = "当前MAC:\n$current\n烧号MAC:\n$new"
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1000 && resultCode == Activity.RESULT_OK) {
            val strMac = (data?.getStringExtra("mac") ?: "0")
            Log.i(TAG(), "scan qr mac[${strMac}]")

            val mac = strMac.toLong(10)
            Log.i(TAG(), "int mac[$mac]]")

            mViewModel?.setMac(mac)

        }
    }

}