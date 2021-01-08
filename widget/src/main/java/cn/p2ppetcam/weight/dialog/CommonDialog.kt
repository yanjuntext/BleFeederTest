package cn.p2ppetcam.weight.dialog

import android.content.DialogInterface
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.FragmentActivity
import cn.p2ppetcam.weight.R
import cn.p2ppetcam.weight.dialog.base.SBuilder
import cn.p2ppetcam.weight.dialog.base.SDialog
import com.base.utils.MLog
import com.base.utils.clickDelay
import kotlinx.android.synthetic.main.dialog_default.*
import kotlinx.android.synthetic.main.include_bottom.*
import kotlinx.android.synthetic.main.include_title.*


/**
 *对话框dialog
 *@author abc
 *@time 2019/11/12 17:08
 */
class CommonDialog private constructor(private var builder: Builder) : SDialog(builder) {


    private val TAG by lazy { CommonDialog::class.java.name }
    private var mTvContent:AppCompatTextView? = null

    override fun initView() {

        tv_title.text = if (builder.title.isNullOrEmpty()) {
            tv_title.visibility = View.GONE
            ""
        } else {
            tv_title.visibility = View.VISIBLE
            builder.title
        }
        tv_title.setTextColor(builder.titleColor)
        mTvContent = view?.findViewById(R.id.tv_content)
        MLog.e(TAG, "content[${builder.content}]")
        with(mTvContent) {
            this?.text = builder.content ?: ""
            this?.setTextColor(builder.contentColor)
        }
        with(builder.cancle) {
            if (this.isNullOrEmpty()) {
                divider_ver.visibility = View.GONE
                tv_cancle.visibility = View.GONE
            } else {
                divider_ver.visibility = View.VISIBLE
                tv_cancle.visibility = View.VISIBLE
            }
            tv_cancle.text = this ?: ""
            tv_cancle.setTextColor(builder.cancleColor)
        }

        tv_confirm.text = builder.confirm ?: ""
        tv_confirm.setTextColor(builder.confirmColor)

        tv_cancle.clickDelay {
            this.dismissAllowingStateLoss()
            builder.onCancleListener?.onCancle(this)
        }

        tv_confirm.clickDelay {
            this.dismissAllowingStateLoss()
            builder.onConfirmListrener?.onCaonfirm(this, null)
        }

    }

    private fun refresh(){
        if(!isAdded) return
        with(mTvContent) {
            this?.text = builder.content ?: ""
            this?.setTextColor(builder.contentColor)
        }
    }

    override fun onShow(dialog: DialogInterface?) {

    }


    open class Builder(activity: FragmentActivity) : SBuilder<Builder>(activity) {
        private var mDialog:CommonDialog? = null
        override fun builder(): Builder {
            setContentView(R.layout.dialog_default)
            return this
        }

        override fun show(activity: FragmentActivity)  = (mDialog?:CommonDialog(this).also {
            mDialog = it
        }).also {
            if(it.dialog?.isShowing != true){
                it.show(activity)
            }
            it.refresh()
        }

        fun dismiss(){
            mDialog?.dismiss()
        }
    }
}