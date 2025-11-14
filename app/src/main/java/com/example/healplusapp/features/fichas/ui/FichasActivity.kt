package com.example.healplusapp.features.fichas.ui

import android.content.Intent
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
import com.example.healplusapp.features.fichas.model.Paciente
import com.example.healplusapp.features.fichas.viewmodel.PacienteViewModel
import com.example.healplusapp.features.fichas.viewmodel.PacienteUiState
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FichasActivity : AppCompatActivity() {
    
    private val viewModel: PacienteViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FichasAdapter
    private var mostrarArquivados = false
    
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
        adapter = FichasAdapter(emptyList()) { paciente ->
            // Abrir detalhes do paciente
            Toast.makeText(this, "Abrir detalhes: ${paciente.nomeCompleto}", Toast.LENGTH_SHORT).show()
        }
        recyclerView.adapter = adapter
    }
    
    private fun setupFab() {
        findViewById<FloatingActionButton>(R.id.fab_adicionar_ficha)?.setOnClickListener {
            // Abrir formulário de novo paciente
            Toast.makeText(this, "Novo paciente", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.pacientesAtivos.collect { pacientes ->
                if (!mostrarArquivados) {
                    adapter.updateList(pacientes)
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.pacientesArquivados.collect { pacientes ->
                if (mostrarArquivados) {
                    adapter.updateList(pacientes)
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is PacienteUiState.Error -> {
                        Toast.makeText(this@FichasActivity, state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetState()
                    }
                    else -> {}
                }
            }
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_fichas, menu)
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
    private val onItemClick: (Paciente) -> Unit
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
    }
    
    override fun getItemCount() = pacientes.size
    
    fun updateList(newList: List<Paciente>) {
        pacientes = newList
        notifyDataSetChanged()
    }
}

