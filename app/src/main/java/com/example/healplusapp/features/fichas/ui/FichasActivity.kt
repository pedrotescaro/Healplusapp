package com.example.healplusapp.features.fichas.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.healplusapp.R
import com.example.healplusapp.features.anamnese.data.AnamneseRepository
import com.example.healplusapp.features.anamnese.ui.AnamneseFormActivity
import com.example.healplusapp.features.fichas.model.Paciente
import com.example.healplusapp.features.fichas.viewmodel.PacienteViewModel
import com.example.healplusapp.features.fichas.viewmodel.PacienteUiState
import com.example.healplusapp.utils.DialogHelper
import com.example.healplusapp.utils.EmptyStateHelper
import com.example.healplusapp.utils.SnackbarHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FichasActivity : AppCompatActivity() {
    
    @Inject
    lateinit var anamneseRepository: AnamneseRepository
    
    private val viewModel: PacienteViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FichasAdapter
    private var mostrarArquivados = false
    private var searchQuery = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fichas)
        
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Minhas Fichas"
        
        setupRecyclerView()
        setupFab()
        observeViewModel()
    }
    
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recycler_fichas)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = FichasAdapter(emptyList(),
            onItemClick = { paciente ->
                abrirAnamnese(paciente)
            },
            onEditClick = { paciente ->
                abrirAnamnese(paciente)
            },
            onDeleteClick = { paciente ->
                DialogHelper.showDeleteConfirmDialog(this, paciente.nomeCompleto) {
                    lifecycleScope.launch {
                        anamneseRepository.deleteByNomePaciente(paciente.nomeCompleto)
                        paciente.id?.let { viewModel.deletarPaciente(it) }
                        SnackbarHelper.showSuccess(
                            findViewById(android.R.id.content),
                            "Paciente e anamnese excluídos"
                        )
                    }
                }
            }
        )
        recyclerView.adapter = adapter
    }
    
    private fun abrirAnamnese(paciente: Paciente) {
        lifecycleScope.launch {
            val anamnese = anamneseRepository.getByNomePaciente(paciente.nomeCompleto)
            if (anamnese != null) {
                val intent = Intent(this@FichasActivity, AnamneseFormActivity::class.java)
                intent.putExtra("id", anamnese.id ?: -1L)
                startActivity(intent)
            } else {
                SnackbarHelper.showError(
                    findViewById(android.R.id.content),
                    "Nenhuma anamnese encontrada para este paciente"
                )
            }
        }
    }
    
    private fun setupFab() {
        findViewById<FloatingActionButton>(R.id.fab_adicionar_ficha)?.setOnClickListener {
            startActivity(Intent(this, PacienteFormActivity::class.java))
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val pacientesFlow = if (mostrarArquivados) viewModel.pacientesArquivados else viewModel.pacientesAtivos
                
                pacientesFlow.collect { pacientes ->
                    val filtered = if (searchQuery.isBlank()) {
                        pacientes
                    } else {
                        pacientes.filter {
                            it.nomeCompleto.contains(searchQuery, ignoreCase = true) ||
                            it.telefone?.contains(searchQuery, ignoreCase = true) == true ||
                            it.email?.contains(searchQuery, ignoreCase = true) == true ||
                            it.profissao?.contains(searchQuery, ignoreCase = true) == true
                        }
                    }
                    
                    adapter.updateList(filtered)
                    
                    val emptyState = findViewById<View>(R.id.empty_state_fichas)
                    if (filtered.isEmpty()) {
                        EmptyStateHelper.showEmptyState(
                            emptyState,
                            recyclerView,
                            "Nenhum paciente encontrado",
                            if (searchQuery.isNotBlank()) {
                                "Tente ajustar a busca"
                            } else {
                                "Toque no botão + para adicionar um novo paciente"
                            }
                        )
                    } else {
                        EmptyStateHelper.hideEmptyState(emptyState, recyclerView)
                    }
                }
            }
        }
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is PacienteUiState.Error -> {
                            SnackbarHelper.showError(findViewById(android.R.id.content), state.message)
                            viewModel.resetState()
                        }
                        is PacienteUiState.Success -> {
                            SnackbarHelper.showSuccess(findViewById(android.R.id.content), "Operação realizada com sucesso")
                            viewModel.resetState()
                        }
                        else -> {}
                    }
                }
            }
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_fichas, menu)
        
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                searchQuery = newText ?: ""
                return true
            }
        })
        
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.menu_mostrar_arquivados -> {
                mostrarArquivados = !mostrarArquivados
                item.title = if (mostrarArquivados) "Mostrar Ativos" else "Mostrar Arquivados"
                // A lista será atualizada automaticamente pelos observers
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

class FichasAdapter(
    private var pacientes: List<Paciente>,
    private val onItemClick: (Paciente) -> Unit,
    private val onEditClick: (Paciente) -> Unit,
    private val onDeleteClick: (Paciente) -> Unit
) : RecyclerView.Adapter<FichasAdapter.ViewHolder>() {
    
    class ViewHolder(val view: android.view.View) : RecyclerView.ViewHolder(view) {
        val nome: android.widget.TextView = view.findViewById(R.id.text_nome)
        val info: android.widget.TextView = view.findViewById(R.id.text_info)
        val btnEditar: android.widget.ImageButton = view.findViewById(R.id.btn_editar)
        val btnExcluir: android.widget.ImageButton = view.findViewById(R.id.btn_excluir)
    }
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ficha, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val paciente = pacientes[position]
        holder.nome.text = paciente.nomeCompleto
        holder.info.text = buildString {
            paciente.telefone?.let { append("Tel: $it") }
            paciente.email?.let { 
                if (isNotEmpty()) append(" | ")
                append("Email: $it")
            }
        }
        holder.view.setOnClickListener { onItemClick(paciente) }
        holder.btnEditar.setOnClickListener { onEditClick(paciente) }
        holder.btnExcluir.setOnClickListener { onDeleteClick(paciente) }
    }
    
    override fun getItemCount() = pacientes.size
    
    fun updateList(newList: List<Paciente>) {
        pacientes = newList
        notifyDataSetChanged()
    }
}

