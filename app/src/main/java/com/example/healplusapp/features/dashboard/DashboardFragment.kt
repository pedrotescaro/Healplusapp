package com.example.healplusapp.features.dashboard

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.healplusapp.R
import com.example.healplusapp.features.anamnese.ui.AnamneseFormActivity
import com.example.healplusapp.features.anamnese.ui.AnamneseListActivity
import com.example.healplusapp.features.fichas.ui.PacienteFormActivity
import android.widget.ImageView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardFragment : Fragment() {
    
    private val auth = FirebaseAuth.getInstance()
    private val viewModel: DashboardViewModel by viewModels()
    
    private lateinit var swipeRefresh: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    private lateinit var progressLoading: ProgressBar
    private lateinit var statTotalPacientes: TextView
    private lateinit var statAnamnesesMes: TextView
    private lateinit var statProximasConsultas: TextView
    private lateinit var statConsultasPendentes: TextView
    private lateinit var recyclerProximasConsultas: RecyclerView
    private lateinit var recyclerAtividadesRecentes: RecyclerView
    private lateinit var emptyProximasConsultas: TextView
    private lateinit var emptyAtividades: TextView
    
    private lateinit var proximasConsultasAdapter: ProximasConsultasAdapter
    private lateinit var atividadesAdapter: AtividadesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupRecyclerViews()
        setupClickListeners()
        loadUserInfo(view)
        setupObservers()
    }
    
    private fun initViews(view: View) {
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        progressLoading = view.findViewById(R.id.progress_loading)
        statTotalPacientes = view.findViewById(R.id.stat_total_pacientes)
        statAnamnesesMes = view.findViewById(R.id.stat_anamneses_mes)
        statProximasConsultas = view.findViewById(R.id.stat_proximas_consultas)
        statConsultasPendentes = view.findViewById(R.id.stat_consultas_pendentes)
        recyclerProximasConsultas = view.findViewById(R.id.recycler_proximas_consultas)
        recyclerAtividadesRecentes = view.findViewById(R.id.recycler_atividades_recentes)
        emptyProximasConsultas = view.findViewById(R.id.empty_proximas_consultas)
        emptyAtividades = view.findViewById(R.id.empty_atividades)
        
        swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }
    }
    
    private fun setupRecyclerViews() {
        proximasConsultasAdapter = ProximasConsultasAdapter { agendamento ->
            // Navegar para detalhes do agendamento
            Snackbar.make(requireView(), "Consulta: ${agendamento.dataAgendamento}", Snackbar.LENGTH_SHORT).show()
        }
        
        atividadesAdapter = AtividadesAdapter()
        
        recyclerProximasConsultas.layoutManager = LinearLayoutManager(requireContext())
        recyclerProximasConsultas.adapter = proximasConsultasAdapter
        
        recyclerAtividadesRecentes.layoutManager = LinearLayoutManager(requireContext())
        recyclerAtividadesRecentes.adapter = atividadesAdapter
    }
    
    private fun setupClickListeners() {
        view.findViewById<View>(R.id.btn_nova_anamnese).setOnClickListener {
            startActivity(Intent(requireContext(), AnamneseFormActivity::class.java))
        }
        
        view.findViewById<View>(R.id.btn_novo_paciente).setOnClickListener {
            startActivity(Intent(requireContext(), PacienteFormActivity::class.java))
        }
        
        view.findViewById<View>(R.id.btn_nova_consulta).setOnClickListener {
            // TODO: Navegar para criar agendamento
            Snackbar.make(requireView(), "Funcionalidade em desenvolvimento", Snackbar.LENGTH_SHORT).show()
        }
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
                        state.error?.let { 
                            Snackbar.make(requireView(), it, Snackbar.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }
    
    private fun updateUI(data: DashboardData) {
        animateStatChange(statTotalPacientes, data.totalPacientes)
        animateStatChange(statAnamnesesMes, data.anamnesesMes)
        animateStatChange(statProximasConsultas, data.proximasConsultas.size)
        animateStatChange(statConsultasPendentes, data.consultasPendentes)
        
        if (data.proximasConsultas.isEmpty()) {
            emptyProximasConsultas.visibility = View.VISIBLE
            recyclerProximasConsultas.visibility = View.GONE
        } else {
            emptyProximasConsultas.visibility = View.GONE
            recyclerProximasConsultas.visibility = View.VISIBLE
            proximasConsultasAdapter.submitList(data.proximasConsultas)
        }
        
        if (data.atividadesRecentes.isEmpty()) {
            emptyAtividades.visibility = View.VISIBLE
            recyclerAtividadesRecentes.visibility = View.GONE
        } else {
            emptyAtividades.visibility = View.GONE
            recyclerAtividadesRecentes.visibility = View.VISIBLE
            atividadesAdapter.submitList(data.atividadesRecentes)
        }
    }
    
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
    
    private fun showLoading() {
        progressLoading.visibility = View.VISIBLE
    }
    
    private fun hideLoading() {
        progressLoading.visibility = View.GONE
        swipeRefresh.isRefreshing = false
    }

    private fun loadUserInfo(view: View) {
        val user = auth.currentUser
        val imageView = view.findViewById<ImageView>(R.id.image_dashboard_photo)
        val textUser = view.findViewById<TextView>(R.id.text_dashboard_user)

        if (user != null) {
            val userName = user.displayName ?: user.email?.substringBefore("@") ?: "Usuário"
            textUser?.text = "Olá, $userName!"

            val photoUrl = user.photoUrl
            if (photoUrl != null && imageView != null) {
                imageView.load(photoUrl.toString()) {
                    crossfade(true)
                    placeholder(R.drawable.ic_image)
                    error(R.drawable.ic_image)
                }
            }
        } else {
            textUser?.text = "Bem-vindo!"
        }
    }
}

