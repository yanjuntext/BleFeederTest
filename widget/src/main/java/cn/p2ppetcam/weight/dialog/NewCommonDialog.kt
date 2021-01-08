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
import kotlinx.android.synthetic.main.include_bottom.*


/**
 *对话框dialog
 *@author abc
 *@time 2019/11/12 17:08
 */
class NewCommonDialog private constructor(private var builder: Builder) : SDialog(builder) {

    private var mTvTitle :AppCompatTextView? = null
    private var mTvContent :AppCompatTextView? = null
    private var mVDivider :View? = null
    private var mTvCancle :AppCompatTextView? = null
    private var mTvConfirm :AppCompatTextView? = null

    private val TAG by lazy { NewCommonDialog::class.java.name }
    override fun initView() {
        mTvTitle = view?.findViewById(R.id.tv_title)
        mTvContent = view?.findViewById(R.id.tv_content)
        mVDivider = view?.findViewById(R.id.divider_ver)
        mTvCancle = view?.findViewById(R.id.tv_cancle)
        mTvConfirm = view?.findViewById(R.id.tv_confirm)

        mTvTitle?.text = if (builder.title.isNullOrEmpty()) {
            mTvTitle?.visibility = View.GONE
            ""
        } else {
            mTvTitle?.visibility = View.VISIBLE
            builder.title
        }
        mTvTitle?.setTextColor(builder.titleColor)

        MLog.e(TAG, "content[${builder.content}]")
        with(mTvContent) {
            this?.text = builder.content ?: ""
            this?.setTextColor(builder.contentColor)
        }
        with(builder.cancle) {
            if (this.isNullOrEmpty()) {
                mVDivider?.visibility = View.GONE
                mTvCancle?.visibility = View.GONE
            } else {
                mVDivider?.visibility = View.VISIBLE
                tv_cancle.visibility = View.VISIBLE
            }
            mTvCancle?.text = this ?: ""
            mTvCancle?.setTextColor(builder.cancleColor)
        }

        mTvConfirm?.text = builder.confirm ?: ""
        mTvConfirm?.setTextColor(builder.confirmColor)

        mTvCancle?.clickDelay {
            this.dismissAllowingStateLoss()
            builder.onCancleListener?.onCancle(this)
        }

        mTvConfirm?.clickDelay {
            this.dismissAllowingStateLoss()
            builder.onConfirmListrener?.onCaonfirm(this, builder.mode)
        }

    }

    internal fun refresh(){
        if(!this.isAdded) return
        mTvTitle?.text = if (builder.title.isNullOrEmpty()) {
            mTvTitle?.visibility = View.GONE
            ""
        } else {
            mTvTitle?.visibility = View.VISIBLE
            builder.title
        }
        mTvTitle?.setTextColor(builder.titleColor)

        MLog.e(TAG, "content[${builder.content}]")
        with(mTvContent) {
            this?.text = builder.content ?: ""
            this?.setTextColor(builder.contentColor)
        }
        with(builder.cancle) {
            if (this.isNullOrEmpty()) {
                mVDivider?.visibility = View.GONE
                mTvCancle?.visibility = View.GONE
            } else {
                mVDivider?.visibility = View.VISIBLE
                mTvCancle?.visibility = View.VISIBLE
            }
            mTvCancle?.text = this ?: ""
            mTvCancle?.setTextColor(builder.cancleColor)
        }

        mTvConfirm?.text = builder.confirm ?: ""
        mTvConfirm?.setTextColor(builder.confirmColor)
    }

    override fun onShow(dialog: DialogInterface?) {

    }


    open class Builder(activity: FragmentActivity) : SBuilder<Builder>(activity) {
        private var mDialog: NewCommonDialog? = null
        var mode: Any? = null

        override fun builder(): Builder {
            setContentView(R.layout.dialog_default)
            return this
        }

        override fun show(activity: FragmentActivity) = (mDialog ?: NewCommonDialog(
            this
        ).also {
            mDialog = it
        }).also {
            if(it.dialog?.isShowing != true){
                it.show(activity)
                it.refresh()
            }
        }

        fun isShowing() = mDialog?.dialog?.isShowing?:false

        fun dismiss(){
            mDialog?.dismiss()
        }
    }
}