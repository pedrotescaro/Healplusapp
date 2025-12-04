package com.example.healplusapp.features.agenda.ui

import android.os.Bundle
import android.widget.EditText
import android.widget.Spinner
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.healplusapp.R
import com.example.healplusapp.features.agenda.model.Agendamento
import com.example.healplusapp.features.agenda.viewmodel.AgendamentoViewModel
import com.example.healplusapp.features.agenda.viewmodel.AgendamentoUiState
import com.example.healplusapp.utils.SnackbarHelper
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AgendamentoFormActivity : AppCompatActivity() {

    private val viewModel: AgendamentoViewModel by viewModels()
    private var currentId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agendamento_form)

        currentId = intent.getLongExtra("id", -1L).takeIf { it > 0 }

        setupToolbar()
        setupSpinners()
        findViewById<MaterialButton>(R.id.btn_salvar_agendamento).setOnClickListener { salvar() }

        observeState()
        currentId?.let { viewModel.obterAgendamento(it) }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (currentId == null) "Novo Agendamento" else "Editar Agendamento"
        
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupSpinners() {
        val statusSpinner = findViewById<Spinner>(R.id.spinner_status)
        val statuses = arrayOf("agendado", "realizado", "cancelado")
        statusSpinner.adapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            statuses
        )
    }

    private fun validarCampos(): Boolean {
        val data = findViewById<EditText>(R.id.et_data_agendamento)?.text?.toString()?.trim().orEmpty()
        if (data.isBlank()) {
            SnackbarHelper.showError(findViewById(android.R.id.content), "Data é obrigatória")
            return false
        }
        return true
    }

    private fun salvar() {
        if (!validarCampos()) return

        val dataAgendamento = findViewById<EditText>(R.id.et_data_agendamento)?.text?.toString()?.trim().orEmpty()
        val horaAgendamento = findViewById<EditText>(R.id.et_hora_agendamento)?.text?.toString()?.trim()
        val tipoConsulta = findViewById<EditText>(R.id.et_tipo_consulta)?.text?.toString()?.trim()
        val observacoes = findViewById<EditText>(R.id.et_observacoes)?.text?.toString()?.trim()
        val status = findViewById<Spinner>(R.id.spinner_status)?.selectedItem?.toString() ?: "agendado"
        val pacienteId = intent.getLongExtra("paciente_id", -1L).takeIf { it > 0 }

        val agendamento = Agendamento(
            id = currentId,
            pacienteId = pacienteId,
            dataAgendamento = dataAgendamento,
            horaAgendamento = horaAgendamento,
            tipoConsulta = tipoConsulta,
            observacoes = observacoes,
            status = status
        )

        viewModel.salvarAgendamento(agendamento, this)
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is AgendamentoUiState.Success -> {
                        SnackbarHelper.showSuccess(findViewById(android.R.id.content), "Agendamento salvo com sucesso")
                        finish()
                    }
                    is AgendamentoUiState.AgendamentoLoaded -> fillForm(state.agendamento)
                    is AgendamentoUiState.Error -> {
                        SnackbarHelper.showError(findViewById(android.R.id.content), state.message)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun fillForm(agendamento: Agendamento) {
        findViewById<EditText>(R.id.et_data_agendamento)?.setText(agendamento.dataAgendamento)
        findViewById<EditText>(R.id.et_hora_agendamento)?.setText(agendamento.horaAgendamento)
        findViewById<EditText>(R.id.et_tipo_consulta)?.setText(agendamento.tipoConsulta)
        findViewById<EditText>(R.id.et_observacoes)?.setText(agendamento.observacoes)
        
        val statusSpinner = findViewById<Spinner>(R.id.spinner_status)
        val statuses = arrayOf("agendado", "realizado", "cancelado")
        val index = statuses.indexOf(agendamento.status)
        if (index >= 0) {
            statusSpinner.setSelection(index)
        }
    }
}

