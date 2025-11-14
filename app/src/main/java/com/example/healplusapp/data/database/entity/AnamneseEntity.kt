package com.example.healplusapp.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "anamneses",
    indices = [Index(value = ["arquivado", "dataConsulta"])]
)
data class AnamneseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nomeCompleto: String,
    val dataConsulta: String?,
    val localizacao: String?,
    val dadosJson: String,
    val arquivado: Boolean = false,
    val dataCriacao: Long = System.currentTimeMillis(),
    val dataAtualizacao: Long = System.currentTimeMillis()
)

