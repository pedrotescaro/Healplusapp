package com.example.healplusapp.features.fichas.ui

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.healplusapp.R
import com.example.healplusapp.features.fichas.model.Paciente
import com.example.healplusapp.features.fichas.viewmodel.PacienteUiState
import com.example.healplusapp.features.fichas.viewmodel.PacienteViewModel
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class PacienteFormActivity : AppCompatActivity() {

    private val viewModel: PacienteViewModel by viewModels()
    private var currentId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paciente_form)

        currentId = intent.getLongExtra("id", -1L).takeIf { it > 0 }

        findViewById<MaterialButton>(R.id.btn_salvar_paciente).setOnClickListener { salvar() }

        observeState()
        currentId?.let { viewModel.obterPaciente(it) }
    }

    private fun observarCamposObrigatorios(): Boolean {
        val nome = findViewById<EditText>(R.id.et_nome_completo).text?.toString()?.trim().orEmpty()
        if (nome.isBlank()) {
            Toast.makeText(this, "Nome é obrigatório", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun salvar() {
        if (!observarCamposObrigatorios()) return
        val nome = findViewById<EditText>(R.id.et_nome_completo).text?.toString()?.trim().orEmpty()
        val dataNascimento = findViewById<EditText>(R.id.et_data_nascimento)?.text?.toString()?.trim()
        val telefone = findViewById<EditText>(R.id.et_telefone)?.text?.toString()?.trim()
        val email = findViewById<EditText>(R.id.et_email)?.text?.toString()?.trim()
        val profissao = findViewById<EditText>(R.id.et_profissao)?.text?.toString()?.trim()
        val estadoCivil = findViewById<EditText>(R.id.et_estado_civil)?.text?.toString()?.trim()
        val observacoes = findViewById<EditText>(R.id.et_observacoes)?.text?.toString()?.trim()

        val model = Paciente(
            id = currentId,
            nomeCompleto = nome,
            dataNascimento = dataNascimento,
            telefone = telefone,
            email = email,
            profissao = profissao,
            estadoCivil = estadoCivil,
            observacoes = observacoes
        )
        viewModel.salvarPaciente(model)
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is PacienteUiState.Success -> {
                        Toast.makeText(this@PacienteFormActivity, "Salvo", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    is PacienteUiState.PacienteLoaded -> fillForm(state.paciente)
                    is PacienteUiState.Error -> Toast.makeText(this@PacienteFormActivity, state.message, Toast.LENGTH_LONG).show()
                    else -> {}
                }
            }
        }
    }

    private fun fillForm(p: Paciente) {
        findViewById<EditText>(R.id.et_nome_completo)?.setText(p.nomeCompleto)
        findViewById<EditText>(R.id.et_data_nascimento)?.setText(p.dataNascimento)
        findViewById<EditText>(R.id.et_telefone)?.setText(p.telefone)
        findViewById<EditText>(R.id.et_email)?.setText(p.email)
        findViewById<EditText>(R.id.et_profissao)?.setText(p.profissao)
        findViewById<EditText>(R.id.et_estado_civil)?.setText(p.estadoCivil)
        findViewById<EditText>(R.id.et_observacoes)?.setText(p.observacoes)
    }
}

