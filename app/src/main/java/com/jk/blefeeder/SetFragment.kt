package com.jk.blefeeder

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.text.Html
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.base.utils.AppUtils
import com.base.utils.clickDelay
import com.jk.blefeeder.base.BaseFragment
import com.jk.blefeeder.viewmodel.MainViewModel
import com.tencent.mmkv.MMKV
import kotlinx.android.synthetic.main.fragment_set.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * 作者：王颜军 on 2020/7/2 11:40
 * 邮箱：3183424727@qq.com
 */
@ExperimentalCoroutinesApi
class SetFragment : BaseFragment<MainViewModel>() {
    override fun getLayoutId(): Int = R.layout.fragment_set

    override fun injectorViewModel() {
        super.injectorViewModel()
        mViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
    }

    override fun subscribeUi() {
        super.subscribeUi()
        mViewModel?.let { viewModel->
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

            }

        }
    }

    override fun initView() {
        super.initView()

        tv_down.text = Html.fromHtml("<u>http://d.firim.ink/blefeedertest</u>")
        tv_version.text = "版本号：" + AppUtils.getAppVersionName(requireContext())
        tv_down.clickDelay {
            val intent = Intent("android.intent.action.VIEW")
            intent.data = Uri.parse("http://d.firim.ink/blefeedertest")
            startActivity(intent)
        }

        tv_text.clickDelay {
            mViewModel?.saveLocal(
                et_ble_name.text?.toString(),
                et_version.text?.toString(),
                et_rssi.text?.toString(),
                et_feed_num.text?.toString(),
                et_record.text?.toString()
            )
        }
        cb_type.isChecked = MMKV.defaultMMKV().decodeBool("app_type",false)
        cb_type.setOnCheckedChangeListener { _, isChecked ->
            MMKV.defaultMMKV().apply {
                encode("app_type",isChecked)
            }
            mViewModel?.setAppType(isChecked)
        }

        qr_type.isChecked = MMKV.defaultMMKV().decodeBool("qr_type",false)
        qr_type.setOnCheckedChangeListener { _, isChecked ->
            MMKV.defaultMMKV().apply {
                encode("qr_type",isChecked)
            }
        }

    }
}