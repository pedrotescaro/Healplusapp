package com.example.healplusapp.features.anamnese.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healplusapp.features.anamnese.data.AnamneseRepository
import com.example.healplusapp.features.anamnese.model.Anamnese
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnamneseViewModel @Inject constructor(
    private val repository: AnamneseRepository
) : ViewModel() {
    
    val anamnesesAtivas: Flow<List<Anamnese>> = repository.getAllAtivas()
    val anamnesesArquivadas: Flow<List<Anamnese>> = repository.getAllArquivadas()
    
    private val _uiState = MutableStateFlow<AnamneseUiState>(AnamneseUiState.Idle)
    val uiState: StateFlow<AnamneseUiState> = _uiState.asStateFlow()
    
    fun salvarAnamnese(anamnese: Anamnese) {
        viewModelScope.launch {
            try {
                _uiState.value = AnamneseUiState.Loading
                val id = repository.salvar(anamnese)
                _uiState.value = AnamneseUiState.Success(id)
            } catch (e: Exception) {
                _uiState.value = AnamneseUiState.Error(e.message ?: "Erro ao salvar anamnese")
            }
        }
    }
    
    fun obterAnamnese(id: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = AnamneseUiState.Loading
                val anamnese = repository.getById(id)
                if (anamnese != null) {
                    _uiState.value = AnamneseUiState.AnamneseLoaded(anamnese)
                } else {
                    _uiState.value = AnamneseUiState.Error("Anamnese não encontrada")
                }
            } catch (e: Exception) {
                _uiState.value = AnamneseUiState.Error(e.message ?: "Erro ao carregar anamnese")
            }
        }
    }
    
    fun arquivarAnamnese(id: Long) {
        viewModelScope.launch {
            try {
                repository.arquivar(id)
                _uiState.value = AnamneseUiState.Success(id)
            } catch (e: Exception) {
                _uiState.value = AnamneseUiState.Error(e.message ?: "Erro ao arquivar anamnese")
            }
        }
    }
    
    fun desarquivarAnamnese(id: Long) {
        viewModelScope.launch {
            try {
                repository.desarquivar(id)
                _uiState.value = AnamneseUiState.Success(id)
            } catch (e: Exception) {
                _uiState.value = AnamneseUiState.Error(e.message ?: "Erro ao desarquivar anamnese")
            }
        }
    }
    
    fun searchAnamneses(query: String): Flow<List<Anamnese>> {
        return repository.searchAtivas(query)
    }
    
    fun resetState() {
        _uiState.value = AnamneseUiState.Idle
    }
}

sealed class AnamneseUiState {
    object Idle : AnamneseUiState()
    object Loading : AnamneseUiState()
    data class Success(val id: Long) : AnamneseUiState()
    data class AnamneseLoaded(val anamnese: Anamnese) : AnamneseUiState()
    data class Error(val message: String) : AnamneseUiState()
}

