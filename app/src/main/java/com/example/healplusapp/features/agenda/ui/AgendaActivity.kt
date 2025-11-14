package com.example.healplusapp.features.agenda.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.healplusapp.R
import com.example.healplusapp.features.agenda.model.Agendamento
import com.example.healplusapp.features.agenda.viewmodel.AgendamentoViewModel
import com.example.healplusapp.features.agenda.viewmodel.AgendamentoUiState
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AgendaActivity : AppCompatActivity() {
    
    private val viewModel: AgendamentoViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AgendaAdapter
    
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
        adapter = AgendaAdapter(emptyList()) { agendamento ->
            // Abrir detalhes do agendamento
            Toast.makeText(this, "Abrir detalhes: ${agendamento.dataAgendamento}", Toast.LENGTH_SHORT).show()
        }
        recyclerView.adapter = adapter
    }
    
    private fun setupFab() {
        findViewById<FloatingActionButton>(R.id.fab_adicionar_agendamento)?.setOnClickListener {
            // Abrir formulário de novo agendamento
            Toast.makeText(this, "Novo agendamento", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.agendamentosAtivos.collect { agendamentos ->
                adapter.updateList(agendamentos)
            }
        }
        
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is AgendamentoUiState.Error -> {
                        Toast.makeText(this@AgendaActivity, state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetState()
                    }
                    else -> {}
                }
            }
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

class AgendaAdapter(
    private var agendamentos: List<Agendamento>,
    private val onItemClick: (Agendamento) -> Unit
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
    }
    
    override fun getItemCount() = agendamentos.size
    
    fun updateList(newList: List<Agendamento>) {
        agendamentos = newList
        notifyDataSetChanged()
    }
}

