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
import com.example.diploma.entities.LoadingStatus
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import kotlinx.android.synthetic.main.item_generator.view.*
import kotlinx.android.synthetic.main.item_integrity.view.*
import kotlinx.android.synthetic.main.item_integrity.view.progressBarStartRecord
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

    fun setStatus(port: Int, state: LoadingStatus) {
        items.find { port.toString() == it.portNumber }?.apply {
            started = state
            if(state == LoadingStatus.NONE || state == LoadingStatus.FAIL) {
                workTime = System.currentTimeMillis() - startedTime
            }
        }
        notifyDataSetChanged()
    }

    fun startClicks(): Observable<Integrity> = startClicksRelay
    fun moreClicks(): Observable<Integrity> = moreClicksRelay

    inner class IntegrityViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: Integrity) = with(itemView) {
            textViewIntegrityTitle.text = item.explicitName

            if(item.started == LoadingStatus.SUCCESS) {
                textViewIntegrityStatus.text = "Включен"
            } else {
                textViewIntegrityStatus.text = "Выключен"
            }
            when(item.started) {
                LoadingStatus.SUCCESS -> {
                    progressBarStartRecord.visibility = View.GONE
                    buttonStartIntegrity.visibility = View.VISIBLE
                    buttonStartIntegrity.setBackgroundColor(ContextCompat.getColor(context, R.color.colorShamrock))
                }
                LoadingStatus.NONE -> {
                    progressBarStartRecord.visibility = View.GONE
                    buttonStartIntegrity.visibility = View.VISIBLE
                    buttonStartIntegrity.setBackgroundResource(R.drawable.bg_silver_ripple)
                }
                LoadingStatus.LOADING -> {
                    progressBarStartRecord.visibility = View.VISIBLE
                    buttonStartIntegrity.visibility = View.GONE
                }
                LoadingStatus.FAIL -> {
                    progressBarStartRecord.visibility = View.GONE
                    buttonStartIntegrity.visibility = View.VISIBLE
                    buttonStartIntegrity.setBackgroundColor(ContextCompat.getColor(context,R.color.colorRed))
                    textViewIntegrityStatus.text = "Обрыв линии!"
                }
            }

            buttonStartIntegrity.setOnClickListener {
                startClicksRelay.accept(item)
                if(item.started == LoadingStatus.NONE) {
                    item.startedTime = System.currentTimeMillis()
                }
                notifyDataSetChanged()
            }

//            buttonStartIntegrity.setOnClickListener {
//                startClicksRelay.accept(item)
//                if(!item.started) {
//                    item.startedTime = System.currentTimeMillis()
//                    item.workTime = 0L
//                    item.broken = false
//                } else {
//                    item.broken = false
//                }
//            }

            buttonConfigureIntegrity.setOnClickListener {
                moreClicksRelay.accept(item)
            }
        }
    }
}
