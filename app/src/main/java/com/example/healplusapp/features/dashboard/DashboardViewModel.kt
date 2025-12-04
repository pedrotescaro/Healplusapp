package com.example.healplusapp.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healplusapp.features.agenda.data.AgendamentoRepository
import com.example.healplusapp.features.agenda.model.Agendamento
import com.example.healplusapp.features.anamnese.data.AnamneseRepository
import com.example.healplusapp.features.anamnese.model.Anamnese
import com.example.healplusapp.features.fichas.data.PacienteRepository
import com.example.healplusapp.features.fichas.model.Paciente
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val anamneseRepository: AnamneseRepository,
    private val agendamentoRepository: AgendamentoRepository,
    private val pacienteRepository: PacienteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun refresh() {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            combine(
                pacienteRepository.getAllAtivos(),
                anamneseRepository.getAllAtivas(),
                agendamentoRepository.getAllAtivos()
            ) { pacientes, anamneses, agendamentos ->
                DashboardData(
                    totalPacientes = pacientes.size,
                    anamnesesMes = anamneses.count { isThisMonth(it.dataConsulta) },
                    proximasConsultas = agendamentos
                        .filter { isNext7Days(it.dataAgendamento) && it.status == "agendado" }
                        .sortedBy { parseDate(it.dataAgendamento) }
                        .take(5),
                    consultasPendentes = agendamentos.count { it.status == "agendado" },
                    atividadesRecentes = getRecentActivities(anamneses, agendamentos, pacientes)
                )
            }.collect { data ->
                _uiState.value = DashboardUiState(
                    data = data,
                    isLoading = false
                )
            }
        }
    }

    private fun isThisMonth(dateString: String?): Boolean {
        if (dateString.isNullOrBlank()) return false
        return try {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = dateFormat.parse(dateString)
            val calendar = Calendar.getInstance()
            calendar.time = date ?: return false
            val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            calendar.get(Calendar.MONTH) == currentMonth && 
            calendar.get(Calendar.YEAR) == currentYear
        } catch (e: Exception) {
            false
        }
    }

    private fun isNext7Days(dateString: String): Boolean {
        return try {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = dateFormat.parse(dateString) ?: return false
            val calendar = Calendar.getInstance()
            val today = calendar.timeInMillis
            val targetDate = date.time
            val diff = targetDate - today
            val daysDiff = diff / (1000 * 60 * 60 * 24)
            daysDiff in 0..7
        } catch (e: Exception) {
            false
        }
    }

    private fun parseDate(dateString: String): Long {
        return try {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            dateFormat.parse(dateString)?.time ?: Long.MAX_VALUE
        } catch (e: Exception) {
            Long.MAX_VALUE
        }
    }

    private fun getRecentActivities(
        anamneses: List<Anamnese>,
        agendamentos: List<Agendamento>,
        pacientes: List<Paciente>
    ): List<ActivityItem> {
        val activities = mutableListOf<ActivityItem>()
        
        // Adiciona anamneses recentes
        anamneses.take(3).forEach { anamnese ->
            activities.add(
                ActivityItem(
                    type = ActivityType.ANAMNESE,
                    title = "Anamnese: ${anamnese.nomeCompleto}",
                    subtitle = anamnese.dataConsulta ?: "Sem data",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        
        // Adiciona agendamentos recentes
        agendamentos.sortedByDescending { parseDate(it.dataAgendamento) }
            .take(3)
            .forEach { agendamento ->
                activities.add(
                    ActivityItem(
                        type = ActivityType.AGENDAMENTO,
                        title = "Consulta agendada",
                        subtitle = "${agendamento.dataAgendamento} ${agendamento.horaAgendamento ?: ""}",
                        timestamp = parseDate(agendamento.dataAgendamento)
                    )
                )
            }
        
        return activities.sortedByDescending { it.timestamp }.take(5)
    }
}

data class DashboardUiState(
    val data: DashboardData? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

data class DashboardData(
    val totalPacientes: Int,
    val anamnesesMes: Int,
    val proximasConsultas: List<Agendamento>,
    val consultasPendentes: Int,
    val atividadesRecentes: List<ActivityItem>
)

data class ActivityItem(
    val type: ActivityType,
    val title: String,
    val subtitle: String,
    val timestamp: Long
)

enum class ActivityType {
    ANAMNESE, PACIENTE, AGENDAMENTO
}

