package com.example.healplusapp.data.di

import android.content.Context
import androidx.room.Room
import com.example.healplusapp.data.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration() // Para desenvolvimento - remover em produção
        .build()
    }
    
    @Provides
    fun provideAnamneseDao(database: AppDatabase) = database.anamneseDao()
    
    @Provides
    fun providePacienteDao(database: AppDatabase) = database.pacienteDao()
    
    @Provides
    fun provideAgendamentoDao(database: AppDatabase) = database.agendamentoDao()
}

