package com.example.healplusapp.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.ForeignKey

@Entity(
    tableName = "agendamentos",
    indices = [
        Index(value = ["arquivado", "dataAgendamento"]),
        Index(value = ["pacienteId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = PacienteEntity::class,
            parentColumns = ["id"],
            childColumns = ["pacienteId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class AgendamentoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pacienteId: Long?,
    val dataAgendamento: String,
    val horaAgendamento: String?,
    val tipoConsulta: String?,
    val observacoes: String?,
    val status: String = "agendado", // agendado, realizado, cancelado
    val arquivado: Boolean = false,
    val dataCriacao: Long = System.currentTimeMillis(),
    val dataAtualizacao: Long = System.currentTimeMillis()
)

