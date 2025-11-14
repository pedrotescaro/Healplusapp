package com.example.healplusapp.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.healplusapp.data.database.converters.DateConverters
import com.example.healplusapp.data.database.dao.AgendamentoDao
import com.example.healplusapp.data.database.dao.AnamneseDao
import com.example.healplusapp.data.database.dao.PacienteDao
import com.example.healplusapp.data.database.entity.AgendamentoEntity
import com.example.healplusapp.data.database.entity.AnamneseEntity
import com.example.healplusapp.data.database.entity.PacienteEntity

@Database(
    entities = [
        AnamneseEntity::class,
        PacienteEntity::class,
        AgendamentoEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun anamneseDao(): AnamneseDao
    abstract fun pacienteDao(): PacienteDao
    abstract fun agendamentoDao(): AgendamentoDao
    
    companion object {
        const val DATABASE_NAME = "healplus_database"
    }
}

