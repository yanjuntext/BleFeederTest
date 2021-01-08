package com.jk.blefeeder.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import cn.p2ppetcam.weight.TipDialog

/**
 * 作者：王颜军 on 2020/7/2 11:41
 * 邮箱：3183424727@qq.com
 */
abstract class BaseFragment<T : BaseAndroidViewModel> : Fragment() {

    open var mViewModel: T? = null

    private var mLoading: TipDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initArguments(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(getLayoutId(), container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        createLoading()
        injectorViewModel()
        initView()
        initData()
        subscribeUi()
    }

    private fun createLoading() {
        if(mLoading == null){
            mLoading = TipDialog.Builder(requireContext())
                .setStyle(TipDialog.Style.LOADING)
                .setDuration(0)
                .setCancelable(false)
                .setCanceledOnTouchOutside(false)
                .create()
        }
    }

    abstract fun getLayoutId(): Int

    open fun initArguments(savedInstanceState: Bundle?) {

    }

    open fun injectorViewModel() {
    }

    open fun initView() {}
    open fun initData() {}

    open fun subscribeUi() {}

    fun showLoading(duration:Int = 0,show:Boolean = true){
        mLoading?.let {
            if(show && !it.isShowing){
                it.duration = duration
                it.show()
            }else{
                it.duration = 0
                it.dismiss()
            }
        }
    }
}