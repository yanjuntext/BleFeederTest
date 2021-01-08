package com.jk.blefeeder

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
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
import com.jk.blefeeder.tcp.TcpManager
import com.jk.blefeeder.utils.LocalHelper
import com.jk.blefeeder.viewmodel.MainViewModel
import com.wyj.base.log
import com.wyj.base.setStatusBarHeight
import com.wyj.base.toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class MainActivity : BaseActivity<MainViewModel>(), SwipeRefreshLayout.OnRefreshListener,
    BleAdapter.OnConnectListener {


    private val mBleList = mutableListOf<BLEDev>()
    private val adapter by lazy { BleAdapter(this, mBleList) }

    private var mNewMac: String? = null
    private var mOldMac: String? = null
    private var mHasNext = false

    private lateinit var mShowAnimator: Animation
    private lateinit var mDismissAnimator: Animation

    private lateinit var mCloseShowAnimator: Animation
    private lateinit var mCloseDismissAnimator: Animation

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
    private val mSetFragment by lazy { SetFragment() }
    private var mRefreshAnimator: ObjectAnimator? = null


    override fun getLayoutId(): Int = R.layout.activity_main
    override fun initValue(savedInstanceState: Bundle?) {
        ToastUtils.init(this.application, ToastBlackStyle(this))
        super.initValue(savedInstanceState)
    }

    override fun injectorViewModel() {

        super.injectorViewModel()
        mViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        mShowAnimator = AnimationUtils.loadAnimation(this, R.anim.dialog_left_in)
        mDismissAnimator = AnimationUtils.loadAnimation(this, R.anim.dialog_left_out).apply {
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationRepeat(animation: Animation?) {

                }

                override fun onAnimationEnd(animation: Animation?) {
                    v_close.visibility = View.GONE
                    fl_fragment.visibility = View.GONE
                }

                override fun onAnimationStart(animation: Animation?) {
                }

            })
        }

        mCloseShowAnimator = AnimationUtils.loadAnimation(this, R.anim.dialog_alpha_in)
        mCloseDismissAnimator = AnimationUtils.loadAnimation(this, R.anim.dialog_alpha_out)
    }


    private fun addFragment() {
        supportFragmentManager.beginTransaction().add(R.id.fl_fragment, mSetFragment)
            .show(mSetFragment).commit()
    }

    override fun initView() {
        super.initView()
        status_bar.setStatusBarHeight(this)

        addFragment()

        swipeRefresh.setColorSchemeColors(
            ContextCompat.getColor(this, R.color.colorPrimary),
            ContextCompat.getColor(this, R.color.colorAccent),
            ContextCompat.getColor(this, R.color.colorPrimaryDark),
            ContextCompat.getColor(this, R.color.black)
        )
        swipeRefresh.setOnRefreshListener(this)

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
            swipeRefresh.isRefreshing = true
            onRefresh()
        }
        mViewModel?.getLocalSetContent()


//        tv_text.clickDelay {
//            mViewModel?.saveLocal(
//                et_ble_name.text?.toString(),
//                et_version.text?.toString(),
//                et_rssi.text?.toString(),
//                et_feed_num.text?.toString(),
//                et_record.text?.toString()
//            )

//        }
        iv_menu.clickDelay {
            fl_fragment.visibility = View.VISIBLE
            v_close.visibility = View.VISIBLE
            fl_fragment.startAnimation(mShowAnimator)
            v_close.startAnimation(mCloseShowAnimator)
//            main.openDrawer(rl_drawer)
        }

        v_close.clickDelay {
//            fl_fragment.visibility = View.GONE
//            v_close.visibility = View.GONE
            fl_fragment.startAnimation(mDismissAnimator)
            v_close.startAnimation(mCloseDismissAnimator)
        }

        iv_refresh.clickDelay {
            log("mRefreshAnimator?.isStarted[${mRefreshAnimator?.isStarted}]")
            if (mRefreshAnimator?.isStarted == true) return@clickDelay
            swipeRefresh.isRefreshing = true
            onRefresh()
        }

        mRefreshAnimator = iv_refresh.rotation(0f, 360f, 800)
        mRefreshAnimator?.repeatCount = -1
//        main.addDrawerListener(object : DrawerLayout.DrawerListener {
//            override fun onDrawerStateChanged(newState: Int) {
//            }
//
//            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
//            }
//
//            override fun onDrawerClosed(drawerView: View) {
//            }
//
//            override fun onDrawerOpened(drawerView: View) {
//                mViewModel?.getLocalSetContent()
//            }
//
//        })

//        tv_down.text = Html.fromHtml("<u>http://d.firim.ink/blefeedertest</u>")
////        tv_version.text = "版本号：" + AppUtils.getAppVersionName(this)
////        tv_down.clickDelay {
////            val intent = Intent("android.intent.action.VIEW")
////            intent.data = Uri.parse("http://d.firim.ink/blefeedertest")
////            startActivity(intent)
////        }
    }

    override fun subscribeUi() {
        super.subscribeUi()
        mViewModel?.let { viewModel ->
            viewModel.mBleDevList.observe(this) {
                mBleList.clear()
                mBleList.addAll(it)
                adapter.notifyDataSetChanged()

                it.singleOrNull { it.bluetoothDevice.address == mNewMac }?.let {
                    if (!mHasNext) {
                        mHasNext = true
                        swipeRefresh.isRefreshing = false
                        startActivityForResult(
                            Intent(this, OperationActivity::class.java)
                                .putExtra("address", it.bluetoothDevice.address)
                                .putExtra("name", it.bluetoothDevice.name)
                                .putExtra("oldAddress", mOldMac ?: ""), 1001
                        )
                    }
                }
                swipeRefresh.isRefreshing = false
            }

            viewModel.mStopScan.observe(this) {
                log("stop[${it}]")
                swipeRefresh.isRefreshing = false
                if (it)
                    mRefreshAnimator?.cancel()
                else if (mRefreshAnimator?.isStarted != true) mRefreshAnimator?.start()
            }
        }
        mViewModel?.mAppType?.observe(this, Observer<Boolean> {
            TcpManager.setDevelopmentType(it)
        })

    }

    private var mDownTime = 0L

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (fl_fragment.visibility == View.VISIBLE) {
                fl_fragment.startAnimation(mDismissAnimator)
                v_close.startAnimation(mCloseDismissAnimator)
            } else {
                if (System.currentTimeMillis() - mDownTime > 200L) {
                    toast("再按一次退出")
                    mDownTime = System.currentTimeMillis()
                } else {
                    finish()
                }
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun showSnackBar(msg: String) {
        val snackbar = Snackbar.make(main, msg, LENGTH_LONG)
        snackbar.view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent))
        snackbar.show()
    }

    override fun onConnect(device: BLEDev) {
        swipeRefresh.isRefreshing = false
        startActivityForResult(
            Intent(this, OperationActivity::class.java)
                .putExtra("address", device.bluetoothDevice.address)
                .putExtra("name", device.bluetoothDevice.name), 1001
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        mNewMac = null
        mOldMac = null
        mHasNext = false
        if (requestCode == 1000) {
            swipeRefresh.isRefreshing = true
            onRefresh()
        } else if (requestCode == 1001 && resultCode == Activity.RESULT_OK) {
            data?.let {
                mNewMac = it.getStringExtra("new") ?: ""
                mOldMac = it.getStringExtra("old") ?: ""
                mViewModel?.searchBle()
            }
        }
        swipeRefresh.isRefreshing = false
    }

    override fun onDestroy() {
        iv_refresh.clearAnimation()
        fl_fragment.clearAnimation()
        v_close.clearAnimation()
        super.onDestroy()
    }

    override fun onRefresh() {
        log("onRefresh")
        if (!LocalHelper.isLocationEnable(this)) {
            swipeRefresh.isRefreshing = false
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
                        swipeRefresh.isRefreshing = false
                        showSnackBar("请打开定位权限")
                    }
                }

                override fun noPermission(denied: List<String>, quick: Boolean) {
                    swipeRefresh.isRefreshing = false
                    mRefreshAnimator?.cancel()
                    showSnackBar("请打开定位权限")
                }
            })
    }
}
