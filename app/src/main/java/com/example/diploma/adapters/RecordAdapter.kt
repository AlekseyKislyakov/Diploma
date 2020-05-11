package com.example.diploma.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.diploma.R
import com.example.diploma.entities.Record
import com.jakewharton.rxrelay2.PublishRelay

class RecordAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items = mutableListOf<Record>()

    private val startClicksRelay = PublishRelay.create<Record>().toSerialized()
    private val moreClicksRelay = PublishRelay.create<Record>().toSerialized()

    companion object {
        private const val GAME_TYPE = 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RelayViewHolder {
        return RelayViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_record,
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

    fun add(newItem: Record) {
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
        items.find { port.toString() == it.portNumber }?.started = state
        notifyDataSetChanged()
    }

    fun startClicks(): Observable<MagnetRelay> = startClicksRelay
    fun moreClicks(): Observable<MagnetRelay> = moreClicksRelay

    inner class RelayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: MagnetRelay) = with(itemView) {
            textViewRelayTitle.text = "${item.name} ${item.portNumber}"

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
                    buttonStartRelay.setBackgroundColor(ContextCompat.getColor(context,R.color.colorSilver))
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
