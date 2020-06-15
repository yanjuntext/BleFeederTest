package com.jk.blefeeder.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jk.blefeeder.R
import com.jk.blefeeder.ble.bean.BLEDev
import kotlinx.android.synthetic.main.adapter_item.view.*

class BleAdapter(val context: Context,var data:MutableList<BLEDev>):RecyclerView.Adapter<BleAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder= ViewHolder(
        LayoutInflater.from(context).inflate(
            R.layout.adapter_item, parent, false
        )
    )

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView(data[position],position)
    }

    var onConnectListener: OnConnectListener? = null


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindView(device: BLEDev, position: Int) {
            itemView.tv_ble_name.text = device.bluetoothDevice.name ?: ""
            itemView.tv_address.text = device.bluetoothDevice.address
            itemView.tv_rssi.text = device.rssi.toString()
            itemView.setOnClickListener {
                this@BleAdapter.onConnectListener?.onConnect(device)
            }

        }
    }

    interface OnConnectListener {
        fun onConnect(device: BLEDev)
    }
}