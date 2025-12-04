package com.example.healplusapp.features.fichas.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.healplusapp.R
import com.example.healplusapp.features.fichas.model.Paciente
import com.example.healplusapp.features.fichas.viewmodel.PacienteViewModel
import com.example.healplusapp.features.fichas.viewmodel.PacienteUiState
import com.example.healplusapp.utils.DialogHelper
import com.example.healplusapp.utils.EmptyStateHelper
import com.example.healplusapp.utils.SnackbarHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FichasActivity : AppCompatActivity() {
    
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
                val intent = Intent(this, PacienteFormActivity::class.java)
                intent.putExtra("id", paciente.id ?: -1L)
                startActivity(intent)
            },
            onItemLongClick = { paciente ->
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(paciente.nomeCompleto)
                    .setItems(arrayOf("Arquivar", "Deletar")) { _, which ->
                        when (which) {
                            0 -> paciente.id?.let {
                                DialogHelper.showArchiveConfirmDialog(this, paciente.nomeCompleto) {
                                    viewModel.arquivarPaciente(it)
                                }
                            }
                            1 -> paciente.id?.let {
                                DialogHelper.showDeleteConfirmDialog(this, paciente.nomeCompleto) {
                                    viewModel.deletarPaciente(it)
                                }
                            }
                        }
                    }
                    .show()
            }
        )
        recyclerView.adapter = adapter
    }
    
    private fun setupFab() {
        findViewById<FloatingActionButton>(R.id.fab_adicionar_ficha)?.setOnClickListener {
            startActivity(Intent(this, PacienteFormActivity::class.java))
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
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
        
        lifecycleScope.launch {
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
    private val onItemLongClick: (Paciente) -> Unit
) : RecyclerView.Adapter<FichasAdapter.ViewHolder>() {
    
    class ViewHolder(val view: android.view.View) : RecyclerView.ViewHolder(view) {
        val nome: android.widget.TextView = view.findViewById(R.id.text_nome)
        val info: android.widget.TextView = view.findViewById(R.id.text_info)
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
        holder.view.setOnLongClickListener { onItemLongClick(paciente); true }
    }
    
    override fun getItemCount() = pacientes.size
    
    fun updateList(newList: List<Paciente>) {
        pacientes = newList
        notifyDataSetChanged()
    }
}

