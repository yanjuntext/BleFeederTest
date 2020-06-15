package com.jk.blefeeder.ble

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

/**
 *
 *@author abc
 *@time 2020/4/26 18:06
 */


class BleBrocastReceiver : BroadcastReceiver() {

    private var mIBLEBrocastCallback: IBLEBrocastCallback? = null

    fun registerBLEBrocastCallback(iBLEBrocastCallback: IBLEBrocastCallback?) {
        this.mIBLEBrocastCallback = iBLEBrocastCallback
    }

    fun unRegisterBLEBrocastCallback() {
        this.mIBLEBrocastCallback = null
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let {
            val action = it.action
            mIBLEBrocastCallback?.bleBroacastReceiver(action, intent)
        }
    }


    fun getIntentFilter(): IntentFilter {
        val filter = IntentFilter()
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        return filter
    }

    interface IBLEBrocastCallback {
        fun bleBroacastReceiver(action: String?, intent: Intent)
    }
}