package com.example.healplusapp.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "pacientes",
    indices = [Index(value = ["arquivado", "nomeCompleto"])]
)
data class PacienteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nomeCompleto: String,
    val dataNascimento: String?,
    val telefone: String?,
    val email: String?,
    val profissao: String?,
    val estadoCivil: String?,
    val observacoes: String?,
    val arquivado: Boolean = false,
    val dataCriacao: Long = System.currentTimeMillis(),
    val dataAtualizacao: Long = System.currentTimeMillis()
)

