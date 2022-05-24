package com.andreick.autenticaobiomtrica.model

data class AgriculturalProducer(
    val name: String,
    val address: String,
    val products: String,
    val annualProduction: Int,
    val productionDestination: String,
    val headcount: Int,
    val agriculturalMachineryNumber: Int,
    val automationLevel: String
)
