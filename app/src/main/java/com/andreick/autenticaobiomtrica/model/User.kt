package com.andreick.autenticaobiomtrica.model

import com.andreick.autenticaobiomtrica.enums.AccessLevel
import java.io.Serializable

data class User(
    val name: String = "",
    val accessLevel: AccessLevel = AccessLevel.LEVEL1,
    val fingerprintName: String = ""
) : Serializable
