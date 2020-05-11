package com.example.diploma.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.diploma.R
import com.example.diploma.entities.Generator
import com.example.diploma.entities.MagnetRelay
import com.example.diploma.ext.prettyValue
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import kotlinx.android.synthetic.main.item_generator.view.*

class GeneratorAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items = mutableListOf<Generator>()

    private val startClicksRelay = PublishRelay.create<Generator>().toSerialized()
    private val moreClicksRelay = PublishRelay.create<Generator>().toSerialized()

    companion object {
        private const val GAME_TYPE = 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GeneratorViewHolder {
        return GeneratorViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_generator,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as GeneratorViewHolder).bind(items[position])
    }

    override fun getItemViewType(position: Int): Int {
        return GAME_TYPE
    }

    fun add(newItem: Generator) {
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

    fun startClicks(): Observable<Generator> = startClicksRelay
    fun moreClicks(): Observable<Generator> = moreClicksRelay

    inner class GeneratorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: Generator) = with(itemView) {
            textViewGeneratorTitle.text = "${item.name} ${item.portNumber}"

            if(item.started) {
                textViewGeneratorStatus.text = "Включен"
            } else {
                textViewGeneratorStatus.text = "Выключен"
            }
            when(item.started) {
                true -> {
                    buttonStartGenerator.setBackgroundColor(ContextCompat.getColor(context, R.color.colorShamrock))
                }
                false -> {
                    buttonStartGenerator.setBackgroundColor(ContextCompat.getColor(context,R.color.colorSilver))
                }
            }

            textViewGeneratorAmplitude.text = item.ampl.prettyValue()
            textViewGeneratorFrequency.text = item.freq.prettyValue()
            textViewGeneratorShape.text = item.shape.toString()

            buttonStartGenerator.setOnClickListener {
                startClicksRelay.accept(item)
                if(!item.started) {
                    item.startedTime = System.currentTimeMillis()
                }
            }

            buttonConfigureGenerator.setOnClickListener {
                moreClicksRelay.accept(item)
            }
        }
    }
}
