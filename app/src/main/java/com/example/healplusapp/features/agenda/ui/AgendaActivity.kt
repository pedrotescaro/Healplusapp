package com.example.healplusapp.features.agenda.ui

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
import com.example.healplusapp.features.agenda.model.Agendamento
import com.example.healplusapp.features.agenda.viewmodel.AgendamentoViewModel
import com.example.healplusapp.features.agenda.viewmodel.AgendamentoUiState
import com.example.healplusapp.utils.DialogHelper
import com.example.healplusapp.utils.EmptyStateHelper
import com.example.healplusapp.utils.SnackbarHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AgendaActivity : AppCompatActivity() {
    
    private val viewModel: AgendamentoViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AgendaAdapter
    private var searchQuery = ""
    private var filterStatus: String? = null
    private var filterData: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agenda)
        
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Agenda"
        
        setupRecyclerView()
        setupFab()
        observeViewModel()
    }
    
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recycler_agenda)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AgendaAdapter(emptyList(),
            onItemClick = { agendamento ->
                val intent = Intent(this, AgendamentoFormActivity::class.java)
                intent.putExtra("id", agendamento.id ?: -1L)
                startActivity(intent)
            },
            onItemLongClick = { agendamento ->
                DialogHelper.showDeleteConfirmDialog(
                    this,
                    "Agendamento de ${agendamento.dataAgendamento}"
                ) {
                    agendamento.id?.let { viewModel.deletarAgendamento(it) }
                }
            }
        )
        recyclerView.adapter = adapter
    }
    
    private fun setupFab() {
        findViewById<FloatingActionButton>(R.id.fab_adicionar_agendamento)?.setOnClickListener {
            val intent = Intent(this, AgendamentoFormActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.agendamentosAtivos.collect { agendamentos ->
                    val filtered = applyFilters(agendamentos)
                    adapter.updateList(filtered)
                    
                    val emptyState = findViewById<View>(R.id.empty_state_agenda)
                    if (filtered.isEmpty()) {
                        EmptyStateHelper.showEmptyState(
                            emptyState,
                            recyclerView,
                            "Nenhum agendamento encontrado",
                            if (searchQuery.isNotBlank() || filterStatus != null || filterData != null) {
                                "Tente ajustar os filtros de busca"
                            } else {
                                "Toque no botão + para adicionar um novo agendamento"
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
                        is AgendamentoUiState.Error -> {
                            SnackbarHelper.showError(findViewById(android.R.id.content), state.message)
                            viewModel.resetState()
                        }
                        is AgendamentoUiState.Success -> {
                            SnackbarHelper.showSuccess(findViewById(android.R.id.content), "Operação realizada com sucesso")
                            viewModel.resetState()
                        }
                        else -> {}
                    }
                }
            }
        }
    }
    
    private fun applyFilters(agendamentos: List<Agendamento>): List<Agendamento> {
        var filtered = agendamentos
        
        // Filtro de busca
        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter {
                it.dataAgendamento.contains(searchQuery, ignoreCase = true) ||
                it.horaAgendamento?.contains(searchQuery, ignoreCase = true) == true ||
                it.tipoConsulta?.contains(searchQuery, ignoreCase = true) == true ||
                it.observacoes?.contains(searchQuery, ignoreCase = true) == true
            }
        }
        
        // Filtro de status
        filterStatus?.let { status ->
            if (status != "Todos") {
                filtered = filtered.filter { it.status == status.lowercase() }
            }
        }
        
        // Filtro de data
        filterData?.let { data ->
            filtered = filtered.filter { it.dataAgendamento == data }
        }
        
        return filtered
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_agenda, menu)
        
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
            R.id.menu_filter_status -> {
                showStatusFilterDialog()
                true
            }
            R.id.menu_filter_data -> {
                showDataFilterDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showStatusFilterDialog() {
        val statuses = arrayOf("Todos", "Agendado", "Realizado", "Cancelado")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Filtrar por Status")
            .setItems(statuses) { _, which ->
                filterStatus = if (which == 0) null else statuses[which]
            }
            .show()
    }
    
    private fun showDataFilterDialog() {
        // TODO: Implementar date picker
        SnackbarHelper.showInfo(findViewById(android.R.id.content), "Filtro de data em desenvolvimento")
    }
}

class AgendaAdapter(
    private var agendamentos: List<Agendamento>,
    private val onItemClick: (Agendamento) -> Unit,
    private val onItemLongClick: (Agendamento) -> Unit
) : RecyclerView.Adapter<AgendaAdapter.ViewHolder>() {
    
    class ViewHolder(val view: android.view.View) : RecyclerView.ViewHolder(view) {
        val data: android.widget.TextView = view.findViewById(R.id.text_data)
        val hora: android.widget.TextView = view.findViewById(R.id.text_hora)
        val tipo: android.widget.TextView = view.findViewById(R.id.text_tipo)
        val status: android.widget.TextView = view.findViewById(R.id.text_status)
    }
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_agendamento, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val agendamento = agendamentos[position]
        holder.data.text = agendamento.dataAgendamento
        holder.hora.text = agendamento.horaAgendamento ?: "Não informado"
        holder.tipo.text = agendamento.tipoConsulta ?: "Consulta"
        holder.status.text = agendamento.status
        holder.view.setOnClickListener { onItemClick(agendamento) }
        holder.view.setOnLongClickListener { onItemLongClick(agendamento); true }
    }
    
    override fun getItemCount() = agendamentos.size
    
    fun updateList(newList: List<Agendamento>) {
        agendamentos = newList
        notifyDataSetChanged()
    }
}

