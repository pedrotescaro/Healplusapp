package com.example.healplusapp.features.anamnese.controller

import android.content.Context
import com.example.healplusapp.features.anamnese.data.AnamneseRepository
import com.example.healplusapp.features.anamnese.model.Anamnese

class AnamneseController(context: Context) {
    private val repository = AnamneseRepository(context)

    fun listar(): List<Anamnese> = repository.getAll()

    fun obter(id: Long): Anamnese? = repository.getById(id)

    fun salvar(anamnese: Anamnese): Long {
        return if (anamnese.id == null) repository.insert(anamnese) else {
            repository.update(anamnese)
            anamnese.id
        }
    }

    fun excluir(id: Long): Int = repository.delete(id)
}


