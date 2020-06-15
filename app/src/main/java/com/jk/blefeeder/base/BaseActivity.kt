package com.jk.blefeeder.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.observe
import cn.p2ppetcam.weight.TipDialog
import com.base.utils.GlobalStatusBarUtil
import com.wyj.base.toast

abstract class BaseActivity<T : BaseAndroidViewModel> : AppCompatActivity() {

    protected var mViewModel: T? = null
    private val mLoading by lazy {
        TipDialog.Builder(this)
            .setStyle(TipDialog.Style.LOADING)
            .setDuration(0)
            .setCancelable(false)
            .setCanceledOnTouchOutside(false)
            .create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GlobalStatusBarUtil.translucent(this)
        setContentView(getLayoutId())
        injectorViewModel()
        initValue(savedInstanceState)
        initView()
        subscribeUi()
    }

    abstract fun getLayoutId(): Int

    open fun initValue(savedInstanceState: Bundle?) {

    }

    open fun injectorViewModel() {

    }

    //登录超时，random失效，需要重新登录
    open fun subscribeUi() {

        mViewModel?.loadingTime?.observe(this) {
            showLoading(it, true)
        }

        mViewModel?.errorMsg?.observe(this) {
            toast(it)
        }

        mViewModel?.loading?.observe(this) {
            showLoading(0, false)
        }

    }

    fun showLoading(duration: Int = 0, show: Boolean = true) {
        if (show && !mLoading.isShowing) {
            mLoading.duration = duration
            mLoading.show()
        } else {
            mLoading.duration = 0
            mLoading.dismiss()
        }
    }

    open fun initView() {}

    override fun onPause() {
        mViewModel?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        mViewModel?.onDestory()
        super.onDestroy()
    }
}