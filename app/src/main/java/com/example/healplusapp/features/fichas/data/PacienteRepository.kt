package com.example.healplusapp.features.fichas.data

import com.example.healplusapp.data.database.dao.PacienteDao
import com.example.healplusapp.data.database.entity.PacienteEntity
import com.example.healplusapp.features.fichas.model.Paciente
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PacienteRepository @Inject constructor(
    private val pacienteDao: PacienteDao
) {
    
    fun getAllAtivos(): Flow<List<Paciente>> {
        return pacienteDao.getAllAtivos().map { entities ->
            entities.map { it.toModel() }
        }
    }
    
    fun getAllArquivados(): Flow<List<Paciente>> {
        return pacienteDao.getAllArquivados().map { entities ->
            entities.map { it.toModel() }
        }
    }
    
    suspend fun getById(id: Long): Paciente? {
        return pacienteDao.getById(id)?.toModel()
    }
    
    fun searchAtivos(query: String): Flow<List<Paciente>> {
        return pacienteDao.searchAtivos(query).map { entities ->
            entities.map { it.toModel() }
        }
    }
    
    suspend fun insert(paciente: Paciente): Long {
        val entity = paciente.toEntity()
        return pacienteDao.insert(entity)
    }
    
    suspend fun update(paciente: Paciente) {
        val entity = paciente.toEntity()
        pacienteDao.update(entity.copy(dataAtualizacao = System.currentTimeMillis()))
    }
    
    suspend fun salvar(paciente: Paciente): Long {
        return if (paciente.id == null) {
            insert(paciente)
        } else {
            update(paciente)
            paciente.id
        }
    }
    
    suspend fun arquivar(id: Long) {
        pacienteDao.arquivar(id)
    }
    
    suspend fun desarquivar(id: Long) {
        pacienteDao.desarquivar(id)
    }
    
    suspend fun countAtivos(): Int {
        return pacienteDao.countAtivos()
    }
    
    private fun PacienteEntity.toModel(): Paciente {
        return Paciente(
            id = this.id,
            nomeCompleto = this.nomeCompleto,
            dataNascimento = this.dataNascimento,
            telefone = this.telefone,
            email = this.email,
            profissao = this.profissao,
            estadoCivil = this.estadoCivil,
            observacoes = this.observacoes
        )
    }
    
    private fun Paciente.toEntity(): PacienteEntity {
        val now = System.currentTimeMillis()
        return PacienteEntity(
            id = this.id ?: 0,
            nomeCompleto = this.nomeCompleto,
            dataNascimento = this.dataNascimento,
            telefone = this.telefone,
            email = this.email,
            profissao = this.profissao,
            estadoCivil = this.estadoCivil,
            observacoes = this.observacoes,
            dataCriacao = if (this.id == null) now else 0,
            dataAtualizacao = now
        )
    }
}

