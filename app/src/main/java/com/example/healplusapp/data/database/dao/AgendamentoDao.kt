package com.example.healplusapp.data.database.dao

import androidx.room.*
import com.example.healplusapp.data.database.entity.AgendamentoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AgendamentoDao {
    
    @Query("SELECT * FROM agendamentos WHERE arquivado = 0 ORDER BY dataAgendamento ASC, horaAgendamento ASC")
    fun getAllAtivos(): Flow<List<AgendamentoEntity>>
    
    @Query("SELECT * FROM agendamentos WHERE arquivado = 0 AND dataAgendamento >= :dataInicio ORDER BY dataAgendamento ASC, horaAgendamento ASC")
    fun getAgendamentosFuturos(dataInicio: String): Flow<List<AgendamentoEntity>>
    
    @Query("SELECT * FROM agendamentos WHERE arquivado = 0 AND dataAgendamento = :data ORDER BY horaAgendamento ASC")
    fun getAgendamentosPorData(data: String): Flow<List<AgendamentoEntity>>
    
    @Query("SELECT * FROM agendamentos WHERE pacienteId = :pacienteId AND arquivado = 0 ORDER BY dataAgendamento DESC")
    fun getAgendamentosPorPaciente(pacienteId: Long): Flow<List<AgendamentoEntity>>
    
    @Query("SELECT * FROM agendamentos WHERE id = :id AND arquivado = 0")
    suspend fun getById(id: Long): AgendamentoEntity?
    
    @Query("SELECT * FROM agendamentos WHERE arquivado = 1 ORDER BY dataAgendamento DESC")
    fun getAllArquivados(): Flow<List<AgendamentoEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(agendamento: AgendamentoEntity): Long
    
    @Update
    suspend fun update(agendamento: AgendamentoEntity)
    
    @Query("UPDATE agendamentos SET arquivado = 1, dataAtualizacao = :timestamp WHERE id = :id")
    suspend fun arquivar(id: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE agendamentos SET arquivado = 0, dataAtualizacao = :timestamp WHERE id = :id")
    suspend fun desarquivar(id: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE agendamentos SET status = :status, dataAtualizacao = :timestamp WHERE id = :id")
    suspend fun atualizarStatus(id: Long, status: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("SELECT COUNT(*) FROM agendamentos WHERE arquivado = 0 AND dataAgendamento >= date('now')")
    suspend fun countProximos(): Int
}

