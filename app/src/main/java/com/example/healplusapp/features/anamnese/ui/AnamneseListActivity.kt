package com.example.healplusapp.features.anamnese.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.healplusapp.R
import com.example.healplusapp.features.anamnese.model.Anamnese
import com.example.healplusapp.features.anamnese.viewmodel.AnamneseViewModel
import com.example.healplusapp.features.anamnese.viewmodel.AnamneseUiState
import com.example.healplusapp.utils.DialogHelper
import com.example.healplusapp.utils.EmptyStateHelper
import com.example.healplusapp.utils.SnackbarHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AnamneseListActivity : AppCompatActivity() {
    
    private val viewModel: AnamneseViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AnamneseAdapter
    private var mostrarArquivadas = false
    private var searchQuery = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_anamnese_list)
        
        setupToolbar()
        setupRecyclerView()
        setupFab()
        observeViewModel()
    }
    
    private fun setupToolbar() {
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Anamneses"
        
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recycler_anamneses)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AnamneseAdapter(emptyList()) { anamnese ->
            val intent = Intent(this, AnamneseFormActivity::class.java)
            intent.putExtra("id", anamnese.id)
            startActivity(intent)
        }
        recyclerView.adapter = adapter
    }
    
    private fun setupFab() {
        findViewById<FloatingActionButton>(R.id.fab_nova_anamnese)?.apply {
            setOnClickListener {
                val intent = Intent(this@AnamneseListActivity, AnamneseFormActivity::class.java)
                startActivity(intent)
            }
            contentDescription = "Criar nova anamnese"
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val anamnesesFlow = if (mostrarArquivadas) viewModel.anamnesesArquivadas else viewModel.anamnesesAtivas
                
                anamnesesFlow.collect { anamneses ->
                    val filtered = if (searchQuery.isBlank()) {
                        anamneses
                    } else {
                        anamneses.filter {
                            it.nomeCompleto.contains(searchQuery, ignoreCase = true) ||
                            it.dataConsulta?.contains(searchQuery, ignoreCase = true) == true ||
                            it.localizacao?.contains(searchQuery, ignoreCase = true) == true
                        }
                    }
                    
                    adapter.updateList(filtered)
                    
                    val emptyState = findViewById<View>(R.id.empty_state_anamneses)
                    if (filtered.isEmpty()) {
                        EmptyStateHelper.showEmptyState(
                            emptyState,
                            recyclerView,
                            "Nenhuma anamnese encontrada",
                            if (searchQuery.isNotBlank()) {
                                "Tente ajustar a busca"
                            } else {
                                "Toque no botão + para criar uma nova anamnese"
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
                        is AnamneseUiState.Error -> {
                            SnackbarHelper.showError(findViewById(android.R.id.content), state.message)
                            viewModel.resetState()
                        }
                        is AnamneseUiState.Success -> {
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
        menuInflater.inflate(R.menu.menu_anamnese_list, menu)
        
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                searchQuery = newText ?: ""
                // A lista será atualizada automaticamente pelo observer
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
            R.id.menu_mostrar_arquivadas -> {
                mostrarArquivadas = !mostrarArquivadas
                item.title = if (mostrarArquivadas) "Mostrar Ativas" else "Mostrar Arquivadas"
                item.setIcon(if (mostrarArquivadas) R.drawable.ic_archive else R.drawable.ic_image)
                // A lista será atualizada automaticamente pelos observers
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Os dados serão atualizados automaticamente pelo Flow
    }
    
    private fun arquivarAnamnese(anamnese: Anamnese) {
        DialogHelper.showArchiveConfirmDialog(
            this,
            anamnese.nomeCompleto
        ) {
            anamnese.id?.let { viewModel.arquivarAnamnese(it) }
        }
    }
}

class AnamneseAdapter(
    private var anamneses: List<Anamnese>,
    private val onItemClick: (Anamnese) -> Unit
) : RecyclerView.Adapter<AnamneseAdapter.ViewHolder>() {
    
    class ViewHolder(val view: android.view.View) : RecyclerView.ViewHolder(view) {
        val nome: android.widget.TextView = view.findViewById(R.id.text_nome)
        val data: android.widget.TextView = view.findViewById(R.id.text_data)
        val localizacao: android.widget.TextView = view.findViewById(R.id.text_localizacao)
    }
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_anamnese, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val anamnese = anamneses[position]
        holder.nome.text = anamnese.nomeCompleto
        holder.data.text = anamnese.dataConsulta ?: "Sem data"
        holder.localizacao.text = anamnese.localizacao ?: ""
        
        holder.view.contentDescription = "Anamnese de ${anamnese.nomeCompleto}. ${anamnese.dataConsulta ?: ""} ${anamnese.localizacao ?: ""}"
        holder.view.setOnClickListener { onItemClick(anamnese) }
    }
    
    override fun getItemCount() = anamneses.size
    
    fun updateList(newList: List<Anamnese>) {
        anamneses = newList
        notifyDataSetChanged()
    }
}
