package com.example.healplusapp.features.anamnese.ui

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.healplusapp.R
import com.example.healplusapp.features.anamnese.AnamnesePreviewActivity
import com.example.healplusapp.features.anamnese.model.Anamnese
import com.example.healplusapp.features.anamnese.viewmodel.AnamneseViewModel
import com.example.healplusapp.features.anamnese.viewmodel.AnamneseUiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.json.JSONObject

@AndroidEntryPoint
class AnamneseFormActivity : AppCompatActivity() {
    private val viewModel: AnamneseViewModel by viewModels()
    private var currentId: Long? = null
    private var selectedImageUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        selectedImageUri = uri
        findViewById<ImageView>(R.id.img_prev_ferida)?.setImageURI(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_anamnese_form)

        // Configurar Toolbar
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back)
        
        // Título dinâmico baseado se é edição ou criação
        currentId = intent.getLongExtra("id", -1L).takeIf { it > 0 }
        supportActionBar?.title = if (currentId != null) "Editar Anamnese" else "Nova Anamnese"
        
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Ligações de UI de botões herdados do fragment
        findViewById<Button>(R.id.btn_escolher_imagem)?.apply {
            setOnClickListener { pickImage.launch("image/*") }
            contentDescription = "Escolher imagem da ferida"
        }
        findViewById<Button>(R.id.btn_remover_imagem)?.apply {
            setOnClickListener {
                selectedImageUri = null
                findViewById<ImageView>(R.id.img_prev_ferida)?.setImageDrawable(null)
                Toast.makeText(this@AnamneseFormActivity, "Imagem removida", Toast.LENGTH_SHORT).show()
            }
            contentDescription = "Remover imagem selecionada"
        }
        
        // FAB de salvar
        findViewById<com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton>(R.id.fab_salvar)?.apply {
            setOnClickListener { salvar() }
            contentDescription = "Salvar anamnese"
        }
        
        // Botão antigo (se existir no layout)
        findViewById<Button>(R.id.btn_salvar_anamnese)?.setOnClickListener { salvar() }

        // Máscaras
        addDateMask(findViewById(R.id.et_data_nascimento))
        addDateMask(findViewById(R.id.et_data_consulta))
        addDateMask(findViewById(R.id.et_data_retorno))
        addPhoneMaskBR(findViewById(R.id.et_telefone))
        addDecimalSanitizer(findViewById(R.id.et_largura))
        addDecimalSanitizer(findViewById(R.id.et_comprimento))
        addDecimalSanitizer(findViewById(R.id.et_profundidade))

        // Atualização dinâmica da barra do leito da ferida
        val watchers = listOf(
            R.id.et_granulacao_percent,
            R.id.et_epitelizacao_percent,
            R.id.et_esfacelo_percent,
            R.id.et_necrose_seca_percent
        )
        watchers.forEach { id ->
            findViewById<EditText>(id)?.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    atualizarLeitoBar()
                }
            })
        }
        atualizarLeitoBar()

        // Observar estado da UI
        observeUiState()

        // Edição: carregar dados se for edição
        currentId = intent.getLongExtra("id", -1L).takeIf { it > 0 }
        currentId?.let { id ->
            viewModel.obterAnamnese(id)
        }
    }
    
    private fun observeUiState() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is AnamneseUiState.Loading -> {
                        // Mostrar loading se necessário
                    }
                    is AnamneseUiState.Success -> {
                        Toast.makeText(this@AnamneseFormActivity, "Salvo com sucesso", Toast.LENGTH_SHORT).show()
                        val jsonCompleto = montarJsonCompleto()
                        val intent = android.content.Intent(this@AnamneseFormActivity, AnamnesePreviewActivity::class.java)
                        intent.putExtra("anamnese_json", jsonCompleto)
                        startActivity(intent)
                        finish()
                    }
                    is AnamneseUiState.AnamneseLoaded -> {
                        fillFormFromModel(state.anamnese)
                    }
                    is AnamneseUiState.Error -> {
                        Toast.makeText(this@AnamneseFormActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                    is AnamneseUiState.Idle -> {
                        // Estado inicial
                    }
                }
            }
        }
    }

    private fun atualizarLeitoBar() {
        fun v(id: Int) = findViewById<android.view.View>(id)
        fun pct(id: Int): Int {
            val et = findViewById<EditText>(id)
            return et?.text?.toString()?.trim()?.replace("[^0-9]".toRegex(), "")?.toIntOrNull() ?: 0
        }
        val g = pct(R.id.et_granulacao_percent).coerceAtLeast(0)
        val e = pct(R.id.et_epitelizacao_percent).coerceAtLeast(0)
        val s = pct(R.id.et_esfacelo_percent).coerceAtLeast(0)
        val n = pct(R.id.et_necrose_seca_percent).coerceAtLeast(0)
        val total = (g + e + s + n).coerceAtLeast(1)

        // Normaliza para 100 somando pesos proporcionais
        val scale = 100f / total
        val wg = (g * scale)
        val we = (e * scale)
        val ws = (s * scale)
        val wn = (n * scale)

        (v(R.id.leito_bar_granulacao)?.layoutParams as? android.widget.LinearLayout.LayoutParams)?.let { lp ->
            lp.weight = wg
            v(R.id.leito_bar_granulacao).layoutParams = lp
        }
        (v(R.id.leito_bar_epitelizacao)?.layoutParams as? android.widget.LinearLayout.LayoutParams)?.let { lp ->
            lp.weight = we
            v(R.id.leito_bar_epitelizacao).layoutParams = lp
        }
        (v(R.id.leito_bar_esfacelo)?.layoutParams as? android.widget.LinearLayout.LayoutParams)?.let { lp ->
            lp.weight = ws
            v(R.id.leito_bar_esfacelo).layoutParams = lp
        }
        (v(R.id.leito_bar_necrose)?.layoutParams as? android.widget.LinearLayout.LayoutParams)?.let { lp ->
            lp.weight = wn
            v(R.id.leito_bar_necrose).layoutParams = lp
        }

        // Força re-layout do container da barra
        findViewById<android.view.View>(R.id.leito_bar_container)?.requestLayout()
    }

    private fun fillFormFromModel(a: Anamnese) {
        // Preencher campos principais
        val nomeEditText = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_nome_completo)
            ?: findViewById<EditText>(R.id.et_nome_completo)
        nomeEditText?.setText(a.nomeCompleto)
        
        findViewById<EditText>(R.id.et_data_consulta)?.setText(a.dataConsulta)
        findViewById<EditText>(R.id.et_localizacao)?.setText(a.localizacao)
        
        // Tentar preencher outros campos do JSON se disponível
        try {
            val json = org.json.JSONObject(a.dadosJson)
            json.optString("dataNascimento")?.takeIf { it.isNotEmpty() }?.let {
                findViewById<EditText>(R.id.et_data_nascimento)?.setText(it)
            }
            json.optString("telefone")?.takeIf { it.isNotEmpty() }?.let {
                findViewById<EditText>(R.id.et_telefone)?.setText(it)
            }
            json.optString("email")?.takeIf { it.isNotEmpty() }?.let {
                findViewById<EditText>(R.id.et_email)?.setText(it)
            }
        } catch (e: Exception) {
            // JSON inválido ou campos não disponíveis
        }
    }

    private fun addDateMask(editText: EditText?) {
        editText?.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return
                val digits = s.toString().replace("[^0-9]".toRegex(), "")
                val builder = StringBuilder()
                if (digits.length > 0) builder.append(digits.substring(0, minOf(2, digits.length)))
                if (digits.length > 2) {
                    builder.append('/')
                    builder.append(digits.substring(2, minOf(4, digits.length)))
                }
                if (digits.length > 4) {
                    builder.append('/')
                    builder.append(digits.substring(4, minOf(8, digits.length)))
                }
                isUpdating = true
                editText.setText(builder.toString())
                editText.setSelection(editText.text.length)
                isUpdating = false
            }
        })
    }

    private fun addPhoneMaskBR(editText: EditText?) {
        editText?.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return
                val digits = s.toString().replace("[^0-9]".toRegex(), "")
                val builder = StringBuilder()
                if (digits.isNotEmpty()) {
                    builder.append('(')
                    val dd = digits.substring(0, minOf(2, digits.length))
                    builder.append(dd)
                    if (digits.length >= 2) builder.append(") ")
                }
                if (digits.length > 2) {
                    val nine = if (digits.length - 2 > 9) 9 else digits.length - 2
                    val part1 = digits.substring(2, 2 + minOf(nine, 5))
                    builder.append(part1)
                    if (digits.length > 7) builder.append('-')
                }
                if (digits.length > 7) {
                    val part2 = digits.substring(7, minOf(digits.length, 11))
                    builder.append(part2)
                }
                isUpdating = true
                editText.setText(builder.toString())
                editText.setSelection(editText.text.length)
                isUpdating = false
            }
        })
    }

    private fun addDecimalSanitizer(editText: EditText?) {
        editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val txt = s?.toString() ?: return
                val cleaned = txt.replace("[^0-9,]".toRegex(), "")
                if (cleaned != txt) {
                    editText.setText(cleaned)
                    editText.setSelection(cleaned.length)
                }
            }
        })
    }

    private fun putText(id: Int, key: String, data: JSONObject) {
        // Tentar TextInputEditText primeiro, depois EditText normal
        val textInputEditText = findViewById<com.google.android.material.textfield.TextInputEditText>(id)
        val editText = findViewById<EditText>(id)
        val text = textInputEditText?.text?.toString()?.trim() 
            ?: editText?.text?.toString()?.trim() 
            ?: ""
        if (text.isNotEmpty() || textInputEditText != null || editText != null) {
            data.put(key, text)
        }
    }

    private fun putCheck(id: Int, key: String, data: JSONObject) {
        val cb = findViewById<CheckBox>(id)
        if (cb != null) data.put(key, cb.isChecked)
    }

    private fun normalizedDecimalFrom(id: Int): String {
        val et = findViewById<EditText>(id)
        val raw = et?.text?.toString()?.trim() ?: ""
        return raw.replace(',', '.')
    }

    private fun montarJsonCompleto(): String {
        val data = JSONObject()
        // Dados Pessoais
        putText(R.id.et_nome_completo, "nomeCompleto", data)
        putText(R.id.et_data_nascimento, "dataNascimento", data)
        putText(R.id.et_telefone, "telefone", data)
        putText(R.id.et_email, "email", data)
        putText(R.id.et_profissao, "profissao", data)
        putText(R.id.et_estado_civil, "estadoCivil", data)
        // Dimensões e características
        data.put("largura", normalizedDecimalFrom(R.id.et_largura))
        data.put("comprimento", normalizedDecimalFrom(R.id.et_comprimento))
        data.put("profundidade", normalizedDecimalFrom(R.id.et_profundidade))
        putText(R.id.et_localizacao, "localizacao", data)
        putText(R.id.et_tempo_evolucao, "tempoEvolucao", data)
        putText(R.id.et_etiologia, "etiologia", data)
        // Leito da ferida
        putText(R.id.et_granulacao_percent, "granulacaoPercent", data)
        putText(R.id.et_epitelizacao_percent, "epitelizacaoPercent", data)
        putText(R.id.et_esfacelo_percent, "esfaceloPercent", data)
        putText(R.id.et_necrose_seca_percent, "necroseSecaPercent", data)
        // Infecção/Inflamação
        putText(R.id.et_intensidade_dor, "intensidadeDor", data)
        putText(R.id.et_fatores_dor, "fatoresDor", data)
        putCheck(R.id.cb_rubor, "rubor", data)
        putCheck(R.id.cb_calor, "calor", data)
        putCheck(R.id.cb_edema, "edema", data)
        putCheck(R.id.cb_dor_local, "dorLocalInflamacao", data)
        putCheck(R.id.cb_perda_funcao, "perdaFuncao", data)
        putCheck(R.id.cb_eritema_perilesional, "eritemaPerilesional", data)
        putCheck(R.id.cb_calor_local, "calorLocal", data)
        putCheck(R.id.cb_edema_local, "edemaLocal", data)
        putCheck(R.id.cb_dor_local_inf, "dorLocalInfeccao", data)
        putCheck(R.id.cb_exsudato_purulento, "exsudatoPurulento", data)
        putCheck(R.id.cb_odor_fetido, "odorFetido", data)
        putCheck(R.id.cb_retardo_cicatrizacao, "retardoCicatrizacao", data)
        putCheck(R.id.cb_cultura_realizada, "culturaRealizada", data)
        // Umidade
        putText(R.id.et_exsudato_quantidade, "exsudatoQuantidade", data)
        putText(R.id.et_exsudato_tipo, "exsudatoTipo", data)
        putText(R.id.et_exsudato_consistencia, "exsudatoConsistencia", data)
        // Bordas
        putText(R.id.et_bordas_caracteristicas, "bordasCaracteristicas", data)
        putText(R.id.et_bordas_fixacao, "bordasFixacao", data)
        putText(R.id.et_velocidade_cicatrizacao, "velocidadeCicatrizacao", data)
        putText(R.id.et_tunel_localizacao, "tunelLocalizacao", data)
        // Pele
        putText(R.id.et_umidade_pele, "umidadePele", data)
        putText(R.id.et_extensao_alteracao, "extensaoAlteracao", data)
        putCheck(R.id.cb_pele_integra, "peleIntegra", data)
        putCheck(R.id.cb_pele_eritematosa, "peleEritematosa", data)
        putCheck(R.id.cb_pele_macerada, "peleMacerada", data)
        putCheck(R.id.cb_pele_seca, "peleSecaDescamativa", data)
        putCheck(R.id.cb_pele_eczema, "peleEczematosa", data)
        putCheck(R.id.cb_pele_hiperpigmentada, "peleHiperpigmentada", data)
        putCheck(R.id.cb_pele_hipopigmentada, "peleHipopigmentada", data)
        putCheck(R.id.cb_pele_indurada, "peleIndurada", data)
        putCheck(R.id.cb_pele_sensivel, "peleSensivel", data)
        putCheck(R.id.cb_pele_edema, "peleEdema", data)
        // Reparo
        putText(R.id.et_observacoes, "observacoesPlano", data)
        putText(R.id.et_data_consulta, "dataConsulta", data)
        putText(R.id.et_hora_consulta, "horaConsulta", data)
        putText(R.id.et_profissional, "profissionalResponsavel", data)
        putText(R.id.et_conselho, "conselhoProfissional", data)
        putText(R.id.et_data_retorno, "dataRetorno", data)
        // Sociais
        putText(R.id.et_fatores_sociais, "fatoresSociaisAutocuidado", data)
        putText(R.id.et_nivel_atividade, "nivelAtividade", data)
        putText(R.id.et_compreensao_adesao, "compreensaoAdesao", data)
        putText(R.id.et_suporte_social, "suporteSocialCuidadores", data)
        putCheck(R.id.cb_atividade_fisica, "praticaAtividadeFisica", data)
        putCheck(R.id.cb_ingere_alcool, "ingereAlcool", data)
        putCheck(R.id.cb_fumante, "fumante", data)
        putText(R.id.et_avaliacao_nutricional, "avaliacaoNutricional", data)
        putText(R.id.et_ingestao_agua, "ingestaoAguaDia", data)
        putText(R.id.et_objetivo_tratamento, "objetivoTratamento", data)
        putText(R.id.et_historico_cicatrizacao, "historicoCicatrizacao", data)
        putCheck(R.id.cb_alergia, "alergia", data)
        putCheck(R.id.cb_cirurgias, "cirurgias", data)
        putCheck(R.id.cb_claudicacao, "claudicacaoIntermitente", data)
        putCheck(R.id.cb_dor_repouso, "dorRepouso", data)
        putText(R.id.et_pulsos_perifericos, "pulsosPerifericos", data)
        // Comorbidades
        putCheck(R.id.cb_dmi, "dmi", data)
        putCheck(R.id.cb_dmii, "dmii", data)
        putCheck(R.id.cb_has, "has", data)
        putCheck(R.id.cb_neoplasia, "neoplasia", data)
        putCheck(R.id.cb_hiv, "hivAids", data)
        putCheck(R.id.cb_obesidade, "obesidade", data)
        putCheck(R.id.cb_cardiopatia, "cardiopatia", data)
        putCheck(R.id.cb_dpoc, "dpoc", data)
        putCheck(R.id.cb_doenca_hematologica, "doencaHematologica", data)
        putCheck(R.id.cb_doenca_vascular, "doencaVascular", data)
        putCheck(R.id.cb_demencia, "demenciaSenil", data)
        putCheck(R.id.cb_insuficiencia_renal, "insuficienciaRenal", data)
        putCheck(R.id.cb_hanseniase, "hanseniase", data)
        putCheck(R.id.cb_insuficiencia_hepatica, "insuficienciaHepatica", data)
        putCheck(R.id.cb_doenca_autoimune, "doencaAutoimune", data)
        putText(R.id.et_outras_condicoes, "outrasCondicoes", data)
        // Medicamentos
        putCheck(R.id.cb_anti_hipertensivo, "antiHipertensivo", data)
        putCheck(R.id.cb_corticoides, "corticoides", data)
        putCheck(R.id.cb_hipoglicemiantes, "hipoglicemiantesOrais", data)
        putCheck(R.id.cb_aines, "aines", data)
        putCheck(R.id.cb_insulina, "insulina", data)
        putCheck(R.id.cb_drogas_vasoativas, "drogasVasoativas", data)
        putCheck(R.id.cb_suplemento, "suplemento", data)
        putCheck(R.id.cb_anticoagulante, "anticoagulante", data)
        putCheck(R.id.cb_vitaminico, "vitaminico", data)
        putCheck(R.id.cb_antirretroviral, "antirretroviral", data)
        putText(R.id.et_outros_medicamentos, "outrosMedicamentos", data)

        selectedImageUri?.let { data.put("imagemUri", it.toString()) }
        return data.toString()
    }

    private fun validarCamposObrigatorios(): Boolean {
        var invalido = false
        
        // Validar nome completo
        val nomeEditText = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_nome_completo)
            ?: findViewById<EditText>(R.id.et_nome_completo)
        val nomeLayout = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.layout_nome_completo)
        
        if (nomeEditText?.text.isNullOrBlank()) {
            nomeLayout?.error = "Nome é obrigatório"
            invalido = true
        } else {
            nomeLayout?.error = null
        }
        
        // Validar outros campos obrigatórios
        val obrigatorios = listOf(
            R.id.et_localizacao to "Localização é obrigatória"
        )
        obrigatorios.forEach { (id, msg) ->
            val et = findViewById<EditText>(id)
            if (et != null && et.text.isNullOrBlank()) {
                et.error = msg
                invalido = true
            } else {
                et?.error = null
            }
        }
        
        if (invalido) Toast.makeText(this, "Preencha os campos obrigatórios", Toast.LENGTH_SHORT).show()
        return !invalido
    }

    private fun validarPercentuaisLeito(): Boolean {
        fun parseIntSafe(id: Int): Int {
            val et = findViewById<EditText>(id)
            return et?.text?.toString()?.trim()?.replace("[^0-9]".toRegex(), "")?.toIntOrNull() ?: 0
        }
        val soma = parseIntSafe(R.id.et_granulacao_percent) +
                parseIntSafe(R.id.et_epitelizacao_percent) +
                parseIntSafe(R.id.et_esfacelo_percent) +
                parseIntSafe(R.id.et_necrose_seca_percent)
        if (soma > 100) {
            findViewById<EditText>(R.id.et_granulacao_percent)?.error = "Soma > 100%"
            findViewById<EditText>(R.id.et_epitelizacao_percent)?.error = "Soma > 100%"
            findViewById<EditText>(R.id.et_esfacelo_percent)?.error = "Soma > 100%"
            findViewById<EditText>(R.id.et_necrose_seca_percent)?.error = "Soma > 100%"
            Toast.makeText(this, "A soma dos percentuais do leito da ferida não pode exceder 100%.", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    private fun validarEmailTelefone(): Boolean {
        val etEmail = findViewById<EditText>(R.id.et_email)
        val emailVal = etEmail?.text?.toString()?.trim().orEmpty()
        if (emailVal.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(emailVal).matches()) {
            etEmail?.error = "E-mail inválido"
            Toast.makeText(this, "Informe um e-mail válido.", Toast.LENGTH_SHORT).show()
            return false
        }
        val etFone = findViewById<EditText>(R.id.et_telefone)
        val foneDigits = etFone?.text?.toString()?.replace("[^0-9]".toRegex(), "").orEmpty()
        if (foneDigits.length < 10) {
            etFone?.error = "Telefone incompleto"
            Toast.makeText(this, "Informe um telefone válido.", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun salvar() {
        if (!validarCamposObrigatorios()) return
        if (!validarPercentuaisLeito()) return
        if (!validarEmailTelefone()) return

        val jsonCompleto = montarJsonCompleto()
        val nomeEditText = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_nome_completo)
            ?: findViewById<EditText>(R.id.et_nome_completo)
        val nome = nomeEditText?.text?.toString()?.trim() ?: ""
        val dataConsulta = findViewById<EditText>(R.id.et_data_consulta)?.text?.toString()?.trim() ?: ""
        val local = findViewById<EditText>(R.id.et_localizacao)?.text?.toString()?.trim() ?: ""

        val model = Anamnese(
            id = currentId,
            nomeCompleto = nome,
            dataConsulta = dataConsulta,
            localizacao = local,
            dadosJson = jsonCompleto
        )
        viewModel.salvarAnamnese(model)
    }
}

