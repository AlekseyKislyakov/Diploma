package com.example.diploma.adapters

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.recyclerview.widget.RecyclerView
import com.example.diploma.R
import com.example.diploma.entities.Integrity
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import kotlinx.android.synthetic.main.item_integrity.view.*
import java.util.*

class IntegrityAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items = mutableListOf<Integrity>()

    private val startClicksRelay = PublishRelay.create<Integrity>().toSerialized()
    private val moreClicksRelay = PublishRelay.create<Integrity>().toSerialized()

    companion object {
        private const val GAME_TYPE = 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IntegrityViewHolder {
        return IntegrityViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_integrity,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as IntegrityViewHolder).bind(items[position])
    }

    override fun getItemViewType(position: Int): Int {
        return GAME_TYPE
    }

    fun add(newItem: Integrity) {
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

    fun setBroken(port: Int, time: Long) {
        items.find { port.toString() == it.portNumber }?.let {
            it.broken = true
            it.workTime = time - it.startedTime
            notifyDataSetChanged()
        }
    }

    fun startClicks(): Observable<Integrity> = startClicksRelay
    fun moreClicks(): Observable<Integrity> = moreClicksRelay

    inner class IntegrityViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: Integrity) = with(itemView) {
            textViewIntegrityTitle.text = "${item.name} ${item.portNumber}"

            if(item.started) {
                textViewIntegrityStatus.text = "Включен"
            } else {
                textViewIntegrityStatus.text = "Выключен"
            }
            when(item.started) {
                true -> {
                    buttonStartIntegrity.setBackgroundColor(ContextCompat.getColor(context, R.color.colorShamrock))
                }
                false -> {
                    buttonStartIntegrity.setBackgroundColor(ContextCompat.getColor(context,R.color.colorSilver))
                }
            }

            if(item.broken) {
                buttonStartIntegrity.setBackgroundColor(ContextCompat.getColor(context,R.color.colorRed))
                textViewIntegrityStatus.text = "Обрыв линии!"
            }

            buttonStartIntegrity.setOnClickListener {
                startClicksRelay.accept(item)
                if(!item.started) {
                    item.startedTime = System.currentTimeMillis()
                    item.workTime = 0L
                    item.broken = false
                } else {
                    item.broken = false
                }
            }

            buttonConfigureIntegrity.setOnClickListener {
                moreClicksRelay.accept(item)
            }
        }
    }
}
