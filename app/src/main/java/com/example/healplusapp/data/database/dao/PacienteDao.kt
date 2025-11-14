package com.example.healplusapp.data.database.dao

import androidx.room.*
import com.example.healplusapp.data.database.entity.PacienteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PacienteDao {
    
    @Query("SELECT * FROM pacientes WHERE arquivado = 0 ORDER BY nomeCompleto ASC")
    fun getAllAtivos(): Flow<List<PacienteEntity>>
    
    @Query("SELECT * FROM pacientes WHERE arquivado = 1 ORDER BY nomeCompleto ASC")
    fun getAllArquivados(): Flow<List<PacienteEntity>>
    
    @Query("SELECT * FROM pacientes WHERE id = :id AND arquivado = 0")
    suspend fun getById(id: Long): PacienteEntity?
    
    @Query("SELECT * FROM pacientes WHERE nomeCompleto LIKE '%' || :query || '%' AND arquivado = 0 ORDER BY nomeCompleto ASC")
    fun searchAtivos(query: String): Flow<List<PacienteEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(paciente: PacienteEntity): Long
    
    @Update
    suspend fun update(paciente: PacienteEntity)
    
    @Query("UPDATE pacientes SET arquivado = 1, dataAtualizacao = :timestamp WHERE id = :id")
    suspend fun arquivar(id: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE pacientes SET arquivado = 0, dataAtualizacao = :timestamp WHERE id = :id")
    suspend fun desarquivar(id: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("SELECT COUNT(*) FROM pacientes WHERE arquivado = 0")
    suspend fun countAtivos(): Int
}

