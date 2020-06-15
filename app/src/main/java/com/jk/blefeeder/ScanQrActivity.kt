package com.jk.blefeeder

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import com.google.zxing.Result
import com.google.zxing.client.result.ParsedResult
import com.google.zxing.client.result.ParsedResultType
import com.jk.blefeeder.base.BaseActivity
import com.jk.blefeeder.viewmodel.MainViewModel
import com.mylhyl.zxing.scanner.OnScannerCompletionListener
import kotlinx.android.synthetic.main.activity_scan_qr.*

class ScanQrActivity : BaseActivity<MainViewModel>(), OnScannerCompletionListener {


    override fun getLayoutId(): Int = R.layout.activity_scan_qr

    override fun initView() {
        super.initView()
        scanner_view.setOnScannerCompletionListener(this)
    }

    override fun onResume() {
        scanner_view.onResume()
        super.onResume()
    }

    override fun onPause() {
        scanner_view.onPause()
        super.onPause()
    }

    override fun onScannerCompletion(
        rawResult: Result?,
        parsedResult: ParsedResult?,
        barcode: Bitmap?
    ) {

        if (parsedResult?.type == ParsedResultType.TEXT) {
            setResult(Activity.RESULT_OK, Intent().putExtra("mac", parsedResult.displayResult ?: "0"))
            finish()
        }
    }
}