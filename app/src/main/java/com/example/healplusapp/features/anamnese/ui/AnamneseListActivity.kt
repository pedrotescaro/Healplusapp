package com.example.healplusapp.features.anamnese.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.healplusapp.R
import com.example.healplusapp.features.anamnese.model.Anamnese
import com.example.healplusapp.features.anamnese.viewmodel.AnamneseViewModel
import com.example.healplusapp.features.anamnese.viewmodel.AnamneseUiState
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AnamneseListActivity : AppCompatActivity() {
    
    private val viewModel: AnamneseViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AnamneseAdapter
    private var mostrarArquivadas = false
    
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
            viewModel.anamnesesAtivas.collect { anamneses ->
                if (!mostrarArquivadas) {
                    adapter.updateList(anamneses)
                    findViewById<View>(R.id.tv_empty).visibility = 
                        if (anamneses.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.anamnesesArquivadas.collect { anamneses ->
                if (mostrarArquivadas) {
                    adapter.updateList(anamneses)
                    findViewById<View>(R.id.tv_empty).visibility = 
                        if (anamneses.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is AnamneseUiState.Error -> {
                        Toast.makeText(this@AnamneseListActivity, state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetState()
                    }
                    else -> {}
                }
            }
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_anamnese_list, menu)
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
        AlertDialog.Builder(this)
            .setTitle("Arquivar anamnese")
            .setMessage("Deseja arquivar '${anamnese.nomeCompleto}'? Os dados não serão deletados, apenas arquivados.")
            .setPositiveButton("Arquivar") { _, _ ->
                anamnese.id?.let { viewModel.arquivarAnamnese(it) }
            }
            .setNegativeButton("Cancelar", null)
            .show()
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
