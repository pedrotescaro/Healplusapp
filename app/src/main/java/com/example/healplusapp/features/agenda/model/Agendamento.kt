package com.example.healplusapp.features.agenda.model

data class Agendamento(
    val id: Long? = null,
    val pacienteId: Long? = null,
    val dataAgendamento: String,
    val horaAgendamento: String? = null,
    val tipoConsulta: String? = null,
    val observacoes: String? = null,
    val status: String = "agendado" // agendado, realizado, cancelado
)

