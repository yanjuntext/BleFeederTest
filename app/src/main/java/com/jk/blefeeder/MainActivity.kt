package com.jk.blefeeder

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import cn.p2ppetcam.weight.SItemDecoration
import cn.p2ppetcam.weight.dialog.CommonDialog
import cn.p2ppetcam.weight.dialog.base.SDialog
import com.base.utils.clickDelay
import com.base.utils.permission.OnPermission
import com.base.utils.permission.Permission
import com.base.utils.permission.SPermissions
import com.base.utils.rotation
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_LONG
import com.hjq.toast.ToastUtils
import com.hjq.toast.style.ToastBlackStyle
import com.jk.blefeeder.adapter.BleAdapter
import com.jk.blefeeder.base.BaseActivity
import com.jk.blefeeder.ble.bean.BLEDev
import com.jk.blefeeder.ble.utils.BLEUtils
import com.jk.blefeeder.utils.LocalHelper
import com.jk.blefeeder.viewmodel.MainViewModel
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.listener.OnRefreshListener
import com.wyj.base.log
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BaseActivity<MainViewModel>(), OnRefreshListener,
    BleAdapter.OnConnectListener {


    private val mBleList = mutableListOf<BLEDev>()
    private val adapter by lazy { BleAdapter(this, mBleList) }

    private val mLocalDialog by lazy {
        CommonDialog.Builder(this)
            .setCancleable(false)
            .setOutClickCancleable(false)
            .setContent("定为未打开，无法获取蓝牙，请打开定为")
            .setConfirm("去开启", object : SDialog.OnConfirmListrener {
                override fun onCaonfirm(dialog: SDialog?, any: Any?) {
                    this@MainActivity.startActivityForResult(
                        Intent(LocalHelper.LOCAL_INTENT),
                        1000
                    )
                }
            })
            .setCancle("取消")
            .builder()
    }

    private var mRefreshAnimator: ObjectAnimator? = null

    override fun getLayoutId(): Int = R.layout.activity_main
    override fun initValue(savedInstanceState: Bundle?) {
        ToastUtils.init(this.application, ToastBlackStyle(this))
        super.initValue(savedInstanceState)
    }

    override fun injectorViewModel() {

        super.injectorViewModel()
        mViewModel = ViewModelProvider(this)[MainViewModel::class.java]
    }

    override fun initView() {
        super.initView()
        smart_refresh.setOnRefreshListener(this)
        recycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recycler.addItemDecoration(
            SItemDecoration(this).setDividerHeight(1).setDividerColorResource(
                R.color.dialog_divider
            )
        )
        adapter.onConnectListener = this
        recycler.adapter = adapter
        if (!BLEUtils.bluetoothEnable(this)) {
            BLEUtils.openBluetooth(this)
        }
        if (BLEUtils.bluetoothEnable(this)) {
            smart_refresh.autoRefresh()
        }
        mViewModel?.getLocalSetContent()


        tv_text.clickDelay {
            mViewModel?.saveLocal(
                et_ble_name.text?.toString(),
                et_version.text?.toString(),
                et_rssi.text?.toString(),
                et_feed_num.text?.toString(),
                et_record.text?.toString()
            )

        }
        iv_menu.clickDelay {
            main.openDrawer(rl_drawer)
        }

        iv_refresh.clickDelay {
            log("mRefreshAnimator?.isStarted[${mRefreshAnimator?.isStarted}]")
            if (mRefreshAnimator?.isStarted == true) return@clickDelay
            smart_refresh.autoRefresh()
        }

        mRefreshAnimator = iv_refresh.rotation(0f, 360f, 800)
        mRefreshAnimator?.repeatCount = -1
        main.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerStateChanged(newState: Int) {
            }

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
            }

            override fun onDrawerClosed(drawerView: View) {
            }

            override fun onDrawerOpened(drawerView: View) {
                mViewModel?.getLocalSetContent()
            }

        })
    }

    override fun subscribeUi() {
        super.subscribeUi()
        mViewModel?.let { viewModel ->
            viewModel.mBleDevList.observe(this) {
                mBleList.clear()
                mBleList.addAll(it)
                adapter.notifyDataSetChanged()
                smart_refresh.finishRefresh()
            }

            viewModel.mBleName.observe(this) {
                et_ble_name.setText(it ?: "")
            }

            viewModel.mBleVersion.observe(this) {
                et_version.setText(it ?: "")
            }

            viewModel.mBleRssi.observe(this) {
                et_rssi.setText(it ?: "")
            }

            viewModel.mBleFeedNum.observe(this) {
                et_feed_num.setText(if (it.isNullOrEmpty()) "1" else it)
            }
            viewModel.mBleRecordTime.observe(this) {
                et_record.setText(if (it.isNullOrEmpty()) "3" else it)
            }
            viewModel.mSaveResult.observe(this) {
                main.closeDrawer(rl_drawer)
            }
            viewModel.mStopScan.observe(this) {
                log("stop[${it}]")
                if (it)
                    mRefreshAnimator?.cancel()
                else if (mRefreshAnimator?.isStarted != true) mRefreshAnimator?.start()
            }
        }

    }

    override fun onRefresh(refreshLayout: RefreshLayout) {

        if (!LocalHelper.isLocationEnable(this)) {
            smart_refresh.finishRefresh()
            mRefreshAnimator?.cancel()
            mLocalDialog.show(this)
            return
        }
        SPermissions.with(this)
            .permission(Permission.ACCESS_COARSE_LOCATION, Permission.ACCESS_FINE_LOCATION)
            .request(object : OnPermission {
                override fun hasPermission(granted: List<String>, isAll: Boolean) {
                    if (isAll) {
                        mRefreshAnimator?.start()
                        mViewModel?.searchBle()
                    } else {
                        mRefreshAnimator?.cancel()
                        smart_refresh.finishRefresh()
                        showSnackBar("请打开定位权限")
                    }
                }

                override fun noPermission(denied: List<String>, quick: Boolean) {
                    smart_refresh.finishRefresh()
                    mRefreshAnimator?.cancel()
                    showSnackBar("请打开定位权限")
                }
            })
    }

    private fun showSnackBar(msg: String) {
        val snackbar = Snackbar.make(main, msg, LENGTH_LONG)
        snackbar.view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent))
        snackbar.show()
    }

    override fun onConnect(device: BLEDev) {
        startActivity(
            Intent(this, OperationActivity::class.java)
                .putExtra("address", device.bluetoothDevice.address)
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1000) {
            smart_refresh.autoRefresh()
        }
    }

    override fun onDestroy() {
        iv_refresh.clearAnimation()
        super.onDestroy()
    }
}
