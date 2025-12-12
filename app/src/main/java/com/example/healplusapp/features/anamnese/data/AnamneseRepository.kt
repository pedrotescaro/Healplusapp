package com.example.healplusapp.features.anamnese.data

import com.example.healplusapp.data.database.dao.AnamneseDao
import com.example.healplusapp.data.database.dao.PacienteDao
import com.example.healplusapp.data.database.entity.AnamneseEntity
import com.example.healplusapp.data.database.entity.PacienteEntity
import com.example.healplusapp.features.anamnese.model.Anamnese
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnamneseRepository @Inject constructor(
    private val anamneseDao: AnamneseDao,
    private val pacienteDao: PacienteDao
) {
    
    fun getAllAtivas(): Flow<List<Anamnese>> {
        return anamneseDao.getAllAtivas().map { entities ->
            entities.map { it.toModel() }
        }
    }
    
    fun getAllArquivadas(): Flow<List<Anamnese>> {
        return anamneseDao.getAllArquivadas().map { entities ->
            entities.map { it.toModel() }
        }
    }
    
    suspend fun getById(id: Long): Anamnese? {
        return anamneseDao.getById(id)?.toModel()
    }
    
    fun searchAtivas(query: String): Flow<List<Anamnese>> {
        return anamneseDao.searchAtivas(query).map { entities ->
            entities.map { it.toModel() }
        }
    }
    
    suspend fun insert(anamnese: Anamnese): Long {
        val entity = anamnese.toEntity()
        val anamneseId = anamneseDao.insert(entity)
        
        criarPacienteSeNaoExistir(anamnese.nomeCompleto)
        
        return anamneseId
    }
    
    private suspend fun criarPacienteSeNaoExistir(nomeCompleto: String) {
        val pacienteExistente = pacienteDao.getByNome(nomeCompleto)
        if (pacienteExistente == null) {
            val now = System.currentTimeMillis()
            val novoPaciente = PacienteEntity(
                nomeCompleto = nomeCompleto,
                dataNascimento = null,
                telefone = null,
                email = null,
                profissao = null,
                estadoCivil = null,
                observacoes = null,
                arquivado = false,
                dataCriacao = now,
                dataAtualizacao = now
            )
            pacienteDao.insert(novoPaciente)
        }
    }
    
    suspend fun update(anamnese: Anamnese) {
        val anamneseAntiga = anamnese.id?.let { anamneseDao.getById(it) }
        val nomeAntigo = anamneseAntiga?.nomeCompleto
        
        val entity = anamnese.toEntity()
        anamneseDao.update(entity.copy(dataAtualizacao = System.currentTimeMillis()))
        
        if (nomeAntigo != null && nomeAntigo != anamnese.nomeCompleto) {
            pacienteDao.updateNome(nomeAntigo, anamnese.nomeCompleto)
        }
    }
    
    suspend fun salvar(anamnese: Anamnese): Long {
        return if (anamnese.id == null) {
            insert(anamnese)
        } else {
            update(anamnese)
            anamnese.id
        }
    }
    
    suspend fun arquivar(id: Long) {
        anamneseDao.arquivar(id)
    }
    
    suspend fun desarquivar(id: Long) {
        anamneseDao.desarquivar(id)
    }
    
    suspend fun countAtivas(): Int {
        return anamneseDao.countAtivas()
    }

    suspend fun getByNomePaciente(nome: String): Anamnese? {
        return anamneseDao.getByNomePaciente(nome)?.toModel()
    }

    suspend fun deleteByNomePaciente(nome: String) {
        anamneseDao.deleteByNomePaciente(nome)
    }
    
    private fun AnamneseEntity.toModel(): Anamnese {
        return Anamnese(
            id = this.id,
            nomeCompleto = this.nomeCompleto,
            dataConsulta = this.dataConsulta,
            localizacao = this.localizacao,
            dadosJson = this.dadosJson
        )
    }
    
    private fun Anamnese.toEntity(): AnamneseEntity {
        val now = System.currentTimeMillis()
        return AnamneseEntity(
            id = this.id ?: 0,
            nomeCompleto = this.nomeCompleto,
            dataConsulta = this.dataConsulta,
            localizacao = this.localizacao,
            dadosJson = this.dadosJson,
            dataCriacao = if (this.id == null) now else 0, // Será atualizado pelo Room
            dataAtualizacao = now
        )
    }
}
