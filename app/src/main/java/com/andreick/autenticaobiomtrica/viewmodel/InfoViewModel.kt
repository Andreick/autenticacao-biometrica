package com.andreick.autenticaobiomtrica.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.andreick.autenticaobiomtrica.model.AgriculturalProducer

class InfoViewModel : ViewModel() {

    private val _agriculturalProducers = MutableLiveData(listOf(
        AgriculturalProducer(
            "Santher",
            "Alameda Joaquim Eugênio de Lima, 424 - 9º andar, São Paulo - SP",
            "Madeira, celulose e papel",
            171_000_000,
            "Mercado interno e externo",
            1_122,
            5540,
            "Elevado"
        ),
        AgriculturalProducer(
            "Adúfertil",
            "Av. Beta, 461 - Distrito Industrial, Jundiaí - SP",
            "Adubos e fertilizantes",
            7_300_000,
            "Mercado interno",
            236,
            686,
            "Elevado"
        ),
        AgriculturalProducer(
            "Pamplona",
            "Rod. BR 470 - KM 150, Nº 13891, Rio do Sul - SC",
            "Alimentos e bebidas",
            974_000_000,
            "Mercado interno",
            2519,
            1757,
            "Elevado"
        ),
        AgriculturalProducer(
            "Eucatex",
            "Portaria Juscelino - Avenida Presidente Juscelino Kubitschek, 1830 - Torre I e II - 11º andar, São Paulo - SP",
            "Madeira, Celulose e Papel",
            1_800_000_000,
            "Mercado interno e externo",
            4_383,
            3591,
            "Elevado"
        ),
    ))

    val infos: LiveData<List<AgriculturalProducer>> = _agriculturalProducers
}