package com.example.diploma.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.diploma.R
import com.example.diploma.entities.LoadingStatus
import com.example.diploma.entities.Record
import com.example.diploma.ext.convertLongToTime
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import kotlinx.android.synthetic.main.item_record.view.*

class RecordAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items = mutableListOf<Record>()

    private val startClicksRelay = PublishRelay.create<Record>().toSerialized()
    private val moreClicksRelay = PublishRelay.create<Record>().toSerialized()

    companion object {
        private const val GAME_TYPE = 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        return RecordViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_record,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as RecordViewHolder).bind(items[position])
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

    fun setStatus(port: Int, state: LoadingStatus) {
        items.find { port.toString() == it.portNumber }?.apply {
            started = state
            if(state == LoadingStatus.NONE || state == LoadingStatus.FAIL) {
                workTime = System.currentTimeMillis() - startedTime
            }
        }
        notifyDataSetChanged()
    }

    fun updateItem(item: Record, period: Int) {
        items.find { item.portNumber == it.portNumber }?.apply {
            this.period = period
        }
        notifyDataSetChanged()
    }

    fun addRecord(port: Int, value: Int) {
        items.find { port.toString() == it.portNumber }?.apply {
            if (map.isNotEmpty()) {
                map[map.keys.last() + this.period] = (value / 1023.0f) * this.maxAmpl
            } else {
                map[0] = (value / 1023) * this.maxAmpl
            }
        }
    }

    fun startClicks(): Observable<Record> = startClicksRelay
    fun moreClicks(): Observable<Record> = moreClicksRelay

    inner class RecordViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: Record) = with(itemView) {
            textViewRecordTitle.text = item.explicitName

            if(item.started == LoadingStatus.SUCCESS) {
                textViewRecordStatus.text = "Включен"
            } else {
                textViewRecordStatus.text = "Выключен"
            }
            when(item.started) {
                LoadingStatus.SUCCESS -> {
                    progressBarStartRecord.visibility = View.GONE
                    buttonStartRecord.visibility = View.VISIBLE
                    buttonStartRecord.setBackgroundColor(ContextCompat.getColor(context, R.color.colorShamrock))
                }
                LoadingStatus.NONE -> {
                    progressBarStartRecord.visibility = View.GONE
                    buttonStartRecord.visibility = View.VISIBLE
                    buttonStartRecord.setBackgroundResource(R.drawable.bg_silver_ripple)
                }
                LoadingStatus.LOADING -> {
                    progressBarStartRecord.visibility = View.VISIBLE
                    buttonStartRecord.visibility = View.GONE
                }
            }

            textViewRecordPeriod.text = item.period.toString()
            textViewRecordStart.text = if(item.startedTime != 0L) item.startedTime.convertLongToTime() else "-"

            buttonStartRecord.setOnClickListener {
                startClicksRelay.accept(item)
                if(item.started == LoadingStatus.NONE) {
                    item.startedTime = System.currentTimeMillis()
                }
                notifyDataSetChanged()
            }


            buttonConfigureRecord.setOnClickListener {
                moreClicksRelay.accept(item)
            }
        }
    }
}
