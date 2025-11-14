package com.example.healplusapp.features.fichas.model

data class Paciente(
    val id: Long? = null,
    val nomeCompleto: String,
    val dataNascimento: String? = null,
    val telefone: String? = null,
    val email: String? = null,
    val profissao: String? = null,
    val estadoCivil: String? = null,
    val observacoes: String? = null
)

