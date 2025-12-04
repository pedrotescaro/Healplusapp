# 📊 Exemplo de Implementação - Dashboard Melhorado

## Estrutura Sugerida

### 1. ViewModel para Dashboard

```kotlin
// features/dashboard/DashboardViewModel.kt
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

    private fun loadDashboardData() {
        viewModelScope.launch {
            combine(
                pacienteRepository.getAllAtivos(),
                anamneseRepository.getAllAtivas(),
                agendamentoRepository.getAllAtivos()
            ) { pacientes, anamneses, agendamentos ->
                DashboardData(
                    totalPacientes = pacientes.size,
                    anamnesesMes = anamneses.filter { 
                        isThisMonth(it.dataConsulta) 
                    }.size,
                    proximasConsultas = agendamentos
                        .filter { isNext7Days(it.dataAgendamento) }
                        .sortedBy { it.dataAgendamento }
                        .take(5),
                    consultasPendentes = agendamentos
                        .filter { it.status == "agendado" }
                        .size,
                    atividadesRecentes = getRecentActivities(anamneses, agendamentos)
                )
            }.collect { data ->
                _uiState.update { it.copy(data = data, isLoading = false) }
            }
        }
    }
}

data class DashboardUiState(
    val data: DashboardData? = null,
    val isLoading: Boolean = true
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
```

### 2. Layout do Dashboard Melhorado

```xml
<!-- fragment_dashboard.xml melhorado -->
<ScrollView>
    <LinearLayout orientation="vertical">
        
        <!-- Header com foto e saudação -->
        <LinearLayout>
            <ImageView id="image_dashboard_photo" />
            <TextView id="text_dashboard_user" />
        </LinearLayout>

        <!-- Cards de Estatísticas -->
        <GridLayout columns="2">
            <Card>
                <TextView text="Pacientes Ativos" />
                <TextView id="stat_total_pacientes" />
            </Card>
            <Card>
                <TextView text="Anamneses do Mês" />
                <TextView id="stat_anamneses_mes" />
            </Card>
            <Card>
                <TextView text="Próximas Consultas" />
                <TextView id="stat_proximas_consultas" />
            </Card>
            <Card>
                <TextView text="Pendentes" />
                <TextView id="stat_consultas_pendentes" />
            </Card>
        </GridLayout>

        <!-- Atalhos Rápidos -->
        <Card>
            <TextView text="Ações Rápidas" />
            <LinearLayout orientation="horizontal">
                <Button id="btn_nova_anamnese" />
                <Button id="btn_novo_paciente" />
                <Button id="btn_nova_consulta" />
            </LinearLayout>
        </Card>

        <!-- Próximas Consultas -->
        <Card>
            <TextView text="Próximas Consultas" />
            <RecyclerView id="recycler_proximas_consultas" />
        </Card>

        <!-- Atividades Recentes -->
        <Card>
            <TextView text="Atividades Recentes" />
            <RecyclerView id="recycler_atividades_recentes" />
        </Card>

    </LinearLayout>
</ScrollView>
```

### 3. Fragment Atualizado

```kotlin
// features/dashboard/DashboardFragment.kt
@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        loadUserInfo()
        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.isLoading) {
                        showLoading()
                    } else {
                        hideLoading()
                        state.data?.let { updateUI(it) }
                    }
                }
            }
        }
    }

    private fun updateUI(data: DashboardData) {
        binding.statTotalPacientes.text = data.totalPacientes.toString()
        binding.statAnamnesesMes.text = data.anamnesesMes.toString()
        binding.statProximasConsultas.text = data.proximasConsultas.size.toString()
        binding.statConsultasPendentes.text = data.consultasPendentes.toString()
        
        setupProximasConsultas(data.proximasConsultas)
        setupAtividadesRecentes(data.atividadesRecentes)
    }

    private fun setupClickListeners() {
        binding.btnNovaAnamnese.setOnClickListener {
            startActivity(Intent(requireContext(), AnamneseFormActivity::class.java))
        }
        binding.btnNovoPaciente.setOnClickListener {
            startActivity(Intent(requireContext(), PacienteFormActivity::class.java))
        }
        binding.btnNovaConsulta.setOnClickListener {
            // Navegar para criar agendamento
        }
    }
}
```

### 4. Adapter para Próximas Consultas

```kotlin
// features/dashboard/ProximasConsultasAdapter.kt
class ProximasConsultasAdapter(
    private val onItemClick: (Agendamento) -> Unit
) : RecyclerView.Adapter<ProximasConsultasAdapter.ViewHolder>() {

    private var consultas = listOf<Agendamento>()

    fun submitList(newList: List<Agendamento>) {
        consultas = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProximaConsultaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(consultas[position])
    }

    override fun getItemCount() = consultas.size

    inner class ViewHolder(
        private val binding: ItemProximaConsultaBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(agendamento: Agendamento) {
            binding.textData.text = formatDate(agendamento.dataAgendamento)
            binding.textHora.text = agendamento.horaAgendamento ?: ""
            binding.textPaciente.text = agendamento.pacienteId?.let { 
                getPacienteName(it) 
            } ?: "Paciente"
            
            binding.root.setOnClickListener {
                onItemClick(agendamento)
            }
        }
    }
}
```

---

## 🎨 Melhorias Visuais

### Cards de Estatísticas com Animações

```kotlin
// Adicionar animação quando os números mudam
private fun animateStatChange(textView: TextView, newValue: Int) {
    val oldValue = textView.text.toString().toIntOrNull() ?: 0
    ValueAnimator.ofInt(oldValue, newValue).apply {
        duration = 500
        addUpdateListener { animator ->
            textView.text = animator.animatedValue.toString()
        }
        start()
    }
}
```

### Cores Temáticas por Tipo

```xml
<!-- colors.xml -->
<color name="stat_pacientes">#4CAF50</color>
<color name="stat_anamneses">#2196F3</color>
<color name="stat_consultas">#FF9800</color>
<color name="stat_pendentes">#F44336</color>
```

---

## 📱 Responsividade

### Layout Adaptativo para Tablets

```xml
<!-- values-sw600dp/layout/fragment_dashboard.xml -->
<!-- Grid de 3 colunas ao invés de 2 -->
```

---

## 🔄 Pull to Refresh

```kotlin
binding.swipeRefresh.setOnRefreshListener {
    viewModel.refresh()
}

// No ViewModel
fun refresh() {
    loadDashboardData()
}
```

---

Este é um exemplo completo de como implementar um Dashboard melhorado. 
A estrutura é escalável e pode ser expandida com mais features conforme necessário.

