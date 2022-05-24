package com.andreick.autenticaobiomtrica.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.andreick.autenticaobiomtrica.databinding.ItemAgriculturalProducerBinding
import com.andreick.autenticaobiomtrica.model.AgriculturalProducer

class InfoAdapter(
    private val agriculturalProducers: List<AgriculturalProducer> = listOf()
) : RecyclerView.Adapter<InfoAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemAgriculturalProducerBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(agriculturalProducers[position])
    }

    override fun getItemCount(): Int = agriculturalProducers.size

    class ViewHolder(private val binding: ItemAgriculturalProducerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(agriculturalProducer: AgriculturalProducer) {
            with(binding) {
                tvUnitName.text = agriculturalProducer.name
                tvUnitAddress.text = "Endereço: ${agriculturalProducer.address}"
                tvUnitProducts.text = agriculturalProducer.products
                tvAnnualProduction.text = agriculturalProducer.annualProduction.toString()
                tvProductionDestination.text = agriculturalProducer.productionDestination
                tvUnitHeadcount.text = agriculturalProducer.headcount.toString()
                tvAgriculturalMachinery.text = agriculturalProducer.agriculturalMachineryNumber.toString()
                tvUnitAutomationLevel.text = agriculturalProducer.automationLevel
            }
        }
    }
}