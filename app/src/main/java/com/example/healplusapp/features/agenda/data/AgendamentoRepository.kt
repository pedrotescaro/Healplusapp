package com.example.healplusapp.features.agenda.data

import com.example.healplusapp.data.database.dao.AgendamentoDao
import com.example.healplusapp.data.database.entity.AgendamentoEntity
import com.example.healplusapp.features.agenda.model.Agendamento
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgendamentoRepository @Inject constructor(
    private val agendamentoDao: AgendamentoDao
) {
    
    fun getAllAtivos(): Flow<List<Agendamento>> {
        return agendamentoDao.getAllAtivos().map { entities ->
            entities.map { it.toModel() }
        }
    }
    
    fun getAgendamentosFuturos(dataInicio: String): Flow<List<Agendamento>> {
        return agendamentoDao.getAgendamentosFuturos(dataInicio).map { entities ->
            entities.map { it.toModel() }
        }
    }
    
    fun getAgendamentosPorData(data: String): Flow<List<Agendamento>> {
        return agendamentoDao.getAgendamentosPorData(data).map { entities ->
            entities.map { it.toModel() }
        }
    }
    
    fun getAgendamentosPorPaciente(pacienteId: Long): Flow<List<Agendamento>> {
        return agendamentoDao.getAgendamentosPorPaciente(pacienteId).map { entities ->
            entities.map { it.toModel() }
        }
    }
    
    suspend fun getById(id: Long): Agendamento? {
        return agendamentoDao.getById(id)?.toModel()
    }
    
    suspend fun insert(agendamento: Agendamento): Long {
        val entity = agendamento.toEntity()
        return agendamentoDao.insert(entity)
    }
    
    suspend fun update(agendamento: Agendamento) {
        val entity = agendamento.toEntity()
        agendamentoDao.update(entity.copy(dataAtualizacao = System.currentTimeMillis()))
    }
    
    suspend fun salvar(agendamento: Agendamento): Long {
        return if (agendamento.id == null) {
            insert(agendamento)
        } else {
            update(agendamento)
            agendamento.id
        }
    }
    
    suspend fun arquivar(id: Long) {
        agendamentoDao.arquivar(id)
    }
    
    suspend fun desarquivar(id: Long) {
        agendamentoDao.desarquivar(id)
    }
    
    suspend fun atualizarStatus(id: Long, status: String) {
        agendamentoDao.atualizarStatus(id, status)
    }
    
    suspend fun countProximos(): Int {
        return agendamentoDao.countProximos()
    }
    
    private fun AgendamentoEntity.toModel(): Agendamento {
        return Agendamento(
            id = this.id,
            pacienteId = this.pacienteId,
            dataAgendamento = this.dataAgendamento,
            horaAgendamento = this.horaAgendamento,
            tipoConsulta = this.tipoConsulta,
            observacoes = this.observacoes,
            status = this.status
        )
    }
    
    private fun Agendamento.toEntity(): AgendamentoEntity {
        val now = System.currentTimeMillis()
        return AgendamentoEntity(
            id = this.id ?: 0,
            pacienteId = this.pacienteId,
            dataAgendamento = this.dataAgendamento,
            horaAgendamento = this.horaAgendamento,
            tipoConsulta = this.tipoConsulta,
            observacoes = this.observacoes,
            status = this.status,
            dataCriacao = if (this.id == null) now else 0,
            dataAtualizacao = now
        )
    }
}

