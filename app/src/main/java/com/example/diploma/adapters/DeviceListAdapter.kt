package com.example.diploma.adapters

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.diploma.R
import com.example.diploma.entities.Device
import com.example.diploma.entities.LoadingStatus
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import kotlinx.android.synthetic.main.item_device.view.*

class DeviceListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items = mutableListOf<Device>()

    private val gameClicksRelay = PublishRelay.create<Device>().toSerialized()

    companion object {
        private const val GAME_TYPE = 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        return DeviceViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_device,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as DeviceViewHolder).bind(items[position])
    }

    override fun getItemViewType(position: Int): Int {
        return GAME_TYPE
    }

    fun add(newStory: Device) {
        if (!items.contains(newStory)) {
            items.add(newStory)
        }

        notifyDataSetChanged()
    }

    fun setStatus(newStory: Device, status: LoadingStatus) {
        items.find { it == newStory }?.status = status
        items.find { it != newStory }?.status = LoadingStatus.NONE
        notifyDataSetChanged()
    }

    fun clearItems() {
        items.clear()
        notifyDataSetChanged()
    }

    fun relayClicks(): Observable<Device> = gameClicksRelay

    inner class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: Device) = with(itemView) {
            textViewId.text = item.device.address ?: ""
            textViewName.text = item.device.name ?: ""

            when (item.status) {
                LoadingStatus.NONE -> {
                    imageViewStatus.visibility = View.VISIBLE
                    progressBarStatus.visibility = View.GONE
                    imageViewStatus.setImageResource(R.drawable.bg_status_drawable)
                }
                LoadingStatus.LOADING -> {
                    imageViewStatus.visibility = View.GONE
                    progressBarStatus.visibility = View.VISIBLE
                }
                LoadingStatus.SUCCESS -> {
                    imageViewStatus.visibility = View.VISIBLE
                    progressBarStatus.visibility = View.GONE
                    imageViewStatus.setImageResource(R.drawable.bg_drawable_success)
                }
                LoadingStatus.FAIL -> {
                    imageViewStatus.visibility = View.VISIBLE
                    progressBarStatus.visibility = View.GONE
                    imageViewStatus.setImageResource(R.drawable.bg_drawable_fail)
                }

            }

            itemView.setOnClickListener {
                imageViewStatus.visibility = View.GONE
                progressBarStatus.visibility = View.VISIBLE
                gameClicksRelay.accept(item)
            }
        }
    }
}
