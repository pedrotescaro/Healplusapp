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
    
    // NotificationScheduler será criado quando necessário (lazy)
    private var notificationScheduler: com.example.healplusapp.features.notifications.NotificationScheduler? = null
    
    private fun getNotificationScheduler(context: android.content.Context): com.example.healplusapp.features.notifications.NotificationScheduler {
        if (notificationScheduler == null) {
            notificationScheduler = com.example.healplusapp.features.notifications.NotificationScheduler(context)
        }
        return notificationScheduler!!
    }
    
    val agendamentosAtivos: Flow<List<Agendamento>> = repository.getAllAtivos()
    
    private val _uiState = MutableStateFlow<AgendamentoUiState>(AgendamentoUiState.Idle)
    val uiState: StateFlow<AgendamentoUiState> = _uiState.asStateFlow()
    
    fun salvarAgendamento(agendamento: Agendamento, context: android.content.Context? = null) {
        viewModelScope.launch {
            try {
                _uiState.value = AgendamentoUiState.Loading
                
                // Se está editando, cancela notificações antigas
                agendamento.id?.let { oldId ->
                    context?.let { ctx ->
                        getNotificationScheduler(ctx).cancelAgendamentoReminder(oldId)
                    }
                }
                
                val id = repository.salvar(agendamento)
                
                // Agenda notificações para o agendamento se contexto fornecido
                context?.let { ctx ->
                    val agendamentoComId = agendamento.copy(id = id)
                    if (agendamentoComId.status == "agendado") {
                        val scheduler = getNotificationScheduler(ctx)
                        // Agenda lembrete 24h antes
                        scheduler.scheduleAgendamentoReminder(agendamentoComId, 24)
                        // Agenda lembrete 1h antes
                        scheduler.scheduleAgendamentoReminder(agendamentoComId, 1)
                    }
                }
                
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
    
    fun atualizarStatus(id: Long, status: String, context: android.content.Context? = null) {
        viewModelScope.launch {
            try {
                repository.atualizarStatus(id, status)
                
                // Se cancelado ou realizado, cancela notificações
                context?.let { ctx ->
                    if (status == "cancelado" || status == "realizado") {
                        getNotificationScheduler(ctx).cancelAgendamentoReminder(id)
                    } else if (status == "agendado") {
                        // Reagenda notificações
                        val agendamento = repository.getById(id)
                        agendamento?.let {
                            val scheduler = getNotificationScheduler(ctx)
                            scheduler.scheduleAgendamentoReminder(it, 24)
                            scheduler.scheduleAgendamentoReminder(it, 1)
                        }
                    }
                }
                
                _uiState.value = AgendamentoUiState.Success(id)
            } catch (e: Exception) {
                _uiState.value = AgendamentoUiState.Error(e.message ?: "Erro ao atualizar status")
            }
        }
    }
    
    fun deletarAgendamento(id: Long, context: android.content.Context? = null) {
        viewModelScope.launch {
            try {
                // Cancela notificações antes de deletar
                context?.let { ctx ->
                    getNotificationScheduler(ctx).cancelAgendamentoReminder(id)
                }
                repository.arquivar(id) // Arquivar ao invés de deletar
                _uiState.value = AgendamentoUiState.Success(id)
            } catch (e: Exception) {
                _uiState.value = AgendamentoUiState.Error(e.message ?: "Erro ao deletar agendamento")
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

