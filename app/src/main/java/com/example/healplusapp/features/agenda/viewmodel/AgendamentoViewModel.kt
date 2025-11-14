package com.example.healplusapp.features.agenda.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healplusapp.features.agenda.data.AgendamentoRepository
import com.example.healplusapp.features.agenda.model.Agendamento
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AgendamentoViewModel @Inject constructor(
    private val repository: AgendamentoRepository
) : ViewModel() {
    
    val agendamentosAtivos: Flow<List<Agendamento>> = repository.getAllAtivos()
    
    private val _uiState = MutableStateFlow<AgendamentoUiState>(AgendamentoUiState.Idle)
    val uiState: StateFlow<AgendamentoUiState> = _uiState.asStateFlow()
    
    fun salvarAgendamento(agendamento: Agendamento) {
        viewModelScope.launch {
            try {
                _uiState.value = AgendamentoUiState.Loading
                val id = repository.salvar(agendamento)
                _uiState.value = AgendamentoUiState.Success(id)
            } catch (e: Exception) {
                _uiState.value = AgendamentoUiState.Error(e.message ?: "Erro ao salvar agendamento")
            }
        }
    }
    
    fun obterAgendamento(id: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = AgendamentoUiState.Loading
                val agendamento = repository.getById(id)
                if (agendamento != null) {
                    _uiState.value = AgendamentoUiState.AgendamentoLoaded(agendamento)
                } else {
                    _uiState.value = AgendamentoUiState.Error("Agendamento não encontrado")
                }
            } catch (e: Exception) {
                _uiState.value = AgendamentoUiState.Error(e.message ?: "Erro ao carregar agendamento")
            }
        }
    }
    
    fun arquivarAgendamento(id: Long) {
        viewModelScope.launch {
            try {
                repository.arquivar(id)
                _uiState.value = AgendamentoUiState.Success(id)
            } catch (e: Exception) {
                _uiState.value = AgendamentoUiState.Error(e.message ?: "Erro ao arquivar agendamento")
            }
        }
    }
    
    fun atualizarStatus(id: Long, status: String) {
        viewModelScope.launch {
            try {
                repository.atualizarStatus(id, status)
                _uiState.value = AgendamentoUiState.Success(id)
            } catch (e: Exception) {
                _uiState.value = AgendamentoUiState.Error(e.message ?: "Erro ao atualizar status")
            }
        }
    }
    
    fun getAgendamentosFuturos(): Flow<List<Agendamento>> {
        val hoje = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        return repository.getAgendamentosFuturos(hoje)
    }
    
    fun getAgendamentosPorData(data: String): Flow<List<Agendamento>> {
        return repository.getAgendamentosPorData(data)
    }
    
    fun getAgendamentosPorPaciente(pacienteId: Long): Flow<List<Agendamento>> {
        return repository.getAgendamentosPorPaciente(pacienteId)
    }
    
    fun resetState() {
        _uiState.value = AgendamentoUiState.Idle
    }
}

sealed class AgendamentoUiState {
    object Idle : AgendamentoUiState()
    object Loading : AgendamentoUiState()
    data class Success(val id: Long) : AgendamentoUiState()
    data class AgendamentoLoaded(val agendamento: Agendamento) : AgendamentoUiState()
    data class Error(val message: String) : AgendamentoUiState()
}

