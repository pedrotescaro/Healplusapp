package com.example.healplusapp.features.fichas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healplusapp.features.fichas.data.PacienteRepository
import com.example.healplusapp.features.fichas.model.Paciente
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PacienteViewModel @Inject constructor(
    private val repository: PacienteRepository
) : ViewModel() {
    
    val pacientesAtivos: Flow<List<Paciente>> = repository.getAllAtivos()
    val pacientesArquivados: Flow<List<Paciente>> = repository.getAllArquivados()
    
    private val _uiState = MutableStateFlow<PacienteUiState>(PacienteUiState.Idle)
    val uiState: StateFlow<PacienteUiState> = _uiState.asStateFlow()
    
    fun salvarPaciente(paciente: Paciente) {
        viewModelScope.launch {
            try {
                _uiState.value = PacienteUiState.Loading
                val id = repository.salvar(paciente)
                _uiState.value = PacienteUiState.Success(id)
            } catch (e: Exception) {
                _uiState.value = PacienteUiState.Error(e.message ?: "Erro ao salvar paciente")
            }
        }
    }
    
    fun obterPaciente(id: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = PacienteUiState.Loading
                val paciente = repository.getById(id)
                if (paciente != null) {
                    _uiState.value = PacienteUiState.PacienteLoaded(paciente)
                } else {
                    _uiState.value = PacienteUiState.Error("Paciente não encontrado")
                }
            } catch (e: Exception) {
                _uiState.value = PacienteUiState.Error(e.message ?: "Erro ao carregar paciente")
            }
        }
    }
    
    fun arquivarPaciente(id: Long) {
        viewModelScope.launch {
            try {
                repository.arquivar(id)
                _uiState.value = PacienteUiState.Success(id)
            } catch (e: Exception) {
                _uiState.value = PacienteUiState.Error(e.message ?: "Erro ao arquivar paciente")
            }
        }
    }
    
    fun desarquivarPaciente(id: Long) {
        viewModelScope.launch {
            try {
                repository.desarquivar(id)
                _uiState.value = PacienteUiState.Success(id)
            } catch (e: Exception) {
                _uiState.value = PacienteUiState.Error(e.message ?: "Erro ao desarquivar paciente")
            }
        }
    }
    
    fun searchPacientes(query: String): Flow<List<Paciente>> {
        return repository.searchAtivos(query)
    }
    
    fun resetState() {
        _uiState.value = PacienteUiState.Idle
    }
}

sealed class PacienteUiState {
    object Idle : PacienteUiState()
    object Loading : PacienteUiState()
    data class Success(val id: Long) : PacienteUiState()
    data class PacienteLoaded(val paciente: Paciente) : PacienteUiState()
    data class Error(val message: String) : PacienteUiState()
}

