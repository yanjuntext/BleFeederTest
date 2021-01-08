package com.jk.blefeeder

import android.os.Bundle
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import com.google.zxing.DecodeHintType
import com.jk.blefeeder.base.BaseActivity
import com.jk.blefeeder.viewmodel.MainViewModel
import com.king.zxing.*
import com.tencent.mmkv.MMKV
import kotlinx.android.synthetic.main.activity_scan_qr.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class ScanQrActivity : BaseActivity<MainViewModel>(), OnCaptureCallback {
    val KEY_RESULT = Intents.Scan.RESULT

    private var surfaceView: SurfaceView? = null
    private var viewfinderView: ViewfinderView? = null
    private var ivTorch: View? = null

    private var mCaptureHelper: CaptureHelper? = null

    override fun getLayoutId(): Int = R.layout.activity_scan_qr

    override fun initValue(savedInstanceState: Bundle?) {
        super.initValue(savedInstanceState)

    }

    override fun initView() {
        super.initView()
        surfaceView = findViewById(R.id.surfaceView)
        val viewfinderViewId: Int = getViewfinderViewId()
        if (viewfinderViewId != 0) {
            viewfinderView = findViewById(viewfinderViewId)
        }
        val ivTorchId: Int = getIvTorchId()
        if (ivTorchId != 0) {
            ivTorch = findViewById(ivTorchId)
            ivTorch?.visibility = View.INVISIBLE
        }

//        cb_type.setOnCheckedChangeListener { _, isChecked ->
//            MMKV.defaultMMKV().encode("qr_type", isChecked)
//            setDecodeType()
//        }
//        cb_type.isChecked = type

        initCaptureHelper()
        mCaptureHelper?.onCreate()
    }

    private fun initCaptureHelper() {

        mCaptureHelper = CaptureHelper(this, surfaceView, viewfinderView, ivTorch)
        mCaptureHelper?.setOnCaptureCallback(this)
        setDecodeType()
    }

    private fun setDecodeType() {
        mCaptureHelper?.let {
            val type = MMKV.defaultMMKV().decodeBool("qr_type", false)
            if (type) {
                it.decodeFormats(DecodeFormatManager.ONE_D_FORMATS)
                it.decodeHint(DecodeHintType.TRY_HARDER, true)
            } else {
                it.decodeFormats(DecodeFormatManager.QR_CODE_FORMATS)
                it.decodeHint(DecodeHintType.TRY_HARDER, true)
            }
        }

    }

    private fun getViewfinderViewId(): Int {
        return R.id.viewfinderView
    }

    private fun getIvTorchId(): Int {
        return R.id.ivTorch;
    }

    override fun onResume() {
        super.onResume()
        mCaptureHelper?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mCaptureHelper?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mCaptureHelper?.onDestroy()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        mCaptureHelper?.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    /**
     * 接收扫码结果回调
     * @param result 扫码结果
     * @return 返回true表示拦截，将不自动执行后续逻辑，为false表示不拦截，默认不拦截
     */
    override fun onResultCallback(result: String?): Boolean {
        return false
    }
}