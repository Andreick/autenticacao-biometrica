package com.andreick.autenticaobiomtrica.enums

enum class AccessLevel(val role: String) {
    LEVEL1("Funcionário do Ministério do Meio Ambiente"),
    LEVEL2("Diretor de Divisão"),
    LEVEL3("Ministro do Meio Ambiente")
}