package com.example.healplusapp.data.database.dao

import androidx.room.*
import com.example.healplusapp.data.database.entity.AnamneseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnamneseDao {
    
    @Query("SELECT * FROM anamneses WHERE arquivado = 0 ORDER BY dataCriacao DESC")
    fun getAllAtivas(): Flow<List<AnamneseEntity>>
    
    @Query("SELECT * FROM anamneses WHERE arquivado = 1 ORDER BY dataCriacao DESC")
    fun getAllArquivadas(): Flow<List<AnamneseEntity>>
    
    @Query("SELECT * FROM anamneses WHERE id = :id AND arquivado = 0")
    suspend fun getById(id: Long): AnamneseEntity?
    
    @Query("SELECT * FROM anamneses WHERE nomeCompleto LIKE '%' || :query || '%' AND arquivado = 0 ORDER BY dataCriacao DESC")
    fun searchAtivas(query: String): Flow<List<AnamneseEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(anamnese: AnamneseEntity): Long
    
    @Update
    suspend fun update(anamnese: AnamneseEntity)
    
    @Query("UPDATE anamneses SET arquivado = 1, dataAtualizacao = :timestamp WHERE id = :id")
    suspend fun arquivar(id: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE anamneses SET arquivado = 0, dataAtualizacao = :timestamp WHERE id = :id")
    suspend fun desarquivar(id: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("SELECT COUNT(*) FROM anamneses WHERE arquivado = 0")
    suspend fun countAtivas(): Int
}

