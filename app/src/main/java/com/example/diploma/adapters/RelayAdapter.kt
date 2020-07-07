package com.example.diploma.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.diploma.R
import com.example.diploma.entities.LoadingStatus
import com.example.diploma.entities.MagnetRelay
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import kotlinx.android.synthetic.main.item_relay.view.*

class RelayAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items = mutableListOf<MagnetRelay>()

    private val startClicksRelay = PublishRelay.create<MagnetRelay>().toSerialized()
    private val moreClicksRelay = PublishRelay.create<MagnetRelay>().toSerialized()

    companion object {
        private const val GAME_TYPE = 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RelayViewHolder {
        return RelayViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_relay,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as RelayViewHolder).bind(items[position])
    }

    override fun getItemViewType(position: Int): Int {
        return GAME_TYPE
    }

    fun add(newItem: MagnetRelay) {
        if (!items.contains(newItem)) {
            items.add(newItem)
        }
        notifyDataSetChanged()
    }

    fun clearItems() {
        items.clear()
        notifyDataSetChanged()
    }

    fun setStatus(port: Int, state: LoadingStatus) {
        items.find { port.toString() == it.portNumber }?.apply {
            started = state
            if(state == LoadingStatus.NONE || state == LoadingStatus.FAIL) {
                workTime = System.currentTimeMillis() - startedTime
            }
        }
        notifyDataSetChanged()
    }

    fun startClicks(): Observable<MagnetRelay> = startClicksRelay
    fun moreClicks(): Observable<MagnetRelay> = moreClicksRelay

    inner class RelayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: MagnetRelay) = with(itemView) {
            textViewRelayTitle.text = item.explicitName

            if(item.started == LoadingStatus.SUCCESS) {
                textViewRelayStatus.text = "Включен"
            } else {
                textViewRelayStatus.text = "Выключен"
            }
            when(item.started) {
                LoadingStatus.SUCCESS -> {
                    progressBarStartRecord.visibility = View.GONE
                    buttonStartRelay.visibility = View.VISIBLE
                    buttonStartRelay.setBackgroundColor(ContextCompat.getColor(context, R.color.colorShamrock))
                }
                LoadingStatus.NONE -> {
                    progressBarStartRecord.visibility = View.GONE
                    buttonStartRelay.visibility = View.VISIBLE
                    buttonStartRelay.setBackgroundResource(R.drawable.bg_silver_ripple)
                }
                LoadingStatus.LOADING -> {
                    progressBarStartRecord.visibility = View.VISIBLE
                    buttonStartRelay.visibility = View.GONE
                }
            }

            buttonStartRelay.setOnClickListener {
                startClicksRelay.accept(item)
                if(item.started == LoadingStatus.NONE) {
                    item.startedTime = System.currentTimeMillis()
                }
                notifyDataSetChanged()
            }

            buttonConfigureRelay.setOnClickListener {
                moreClicksRelay.accept(item)
            }
        }
    }
}
