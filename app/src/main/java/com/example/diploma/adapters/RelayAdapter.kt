package com.example.diploma.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.diploma.R
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

    fun setStatus(port: Int, state: Boolean) {
        items.find { port.toString() == it.portNumber }?.apply {
            started = state
            if(!state) {
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

            if(item.started) {
                textViewRelayStatus.text = "Включен"
            } else {
                textViewRelayStatus.text = "Выключен"
            }
            when(item.started) {
                true -> {
                    buttonStartRelay.setBackgroundColor(ContextCompat.getColor(context, R.color.colorShamrock))
                }
                false -> {
                    buttonStartRelay.setBackgroundResource(R.drawable.bg_silver_ripple)
                }
            }

            buttonStartRelay.setOnClickListener {
                startClicksRelay.accept(item)
                if(!item.started) {
                    item.startedTime = System.currentTimeMillis()
                }
            }

            buttonConfigureRelay.setOnClickListener {
                moreClicksRelay.accept(item)
            }
        }
    }
}
