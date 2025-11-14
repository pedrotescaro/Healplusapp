package com.example.healplusapp.features.anamnese

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.Fragment
import com.example.healplusapp.R
import android.content.Intent
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import android.view.ViewGroup
import android.widget.TextView
import org.json.JSONObject

class NovaAnamneseFragment : Fragment() {
    private var selectedImageUri: Uri? = null
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        selectedImageUri = uri
        view?.findViewById<ImageView>(R.id.img_prev_ferida)?.setImageURI(uri)
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_nova_anamnese, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Toggle simples de seção (accordion-like)
        fun toggleSection(headerId: Int, firstChildId: Int, lastChildId: Int) {
            val header = view.findViewById<TextView>(headerId)
            header?.setOnClickListener {
                // Show/hide a faixa de views entre firstChildId..lastChildId
                val container = view as ViewGroup
                var toggling = false
                for (i in 0 until container.childCount) {
                    val child = container.getChildAt(i)
                    if (child.id == firstChildId) toggling = true
                    if (toggling && child.id != headerId) {
                        child.visibility = if (child.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                    }
                    if (child.id == lastChildId) break
                }
            }
        }

        // Mapear seções por cabeçalho e seus primeiros/últimos elementos
        toggleSection(R.id.tv_header_dados, R.id.et_nome_completo, R.id.et_estado_civil)
        toggleSection(R.id.tv_header_tecido, R.id.img_prev_ferida, R.id.btn_remover_imagem)
        toggleSection(R.id.tv_header_dimensoes, R.id.et_largura, R.id.et_etiologia)
        toggleSection(R.id.tv_header_leito, R.id.et_granulacao_percent, R.id.et_necrose_seca_percent)
        toggleSection(R.id.tv_header_infeccao, R.id.et_intensidade_dor, R.id.cb_cultura_realizada)
        toggleSection(R.id.tv_header_umidade, R.id.et_exsudato_quantidade, R.id.et_exsudato_consistencia)
        toggleSection(R.id.tv_header_bordas, R.id.et_bordas_caracteristicas, R.id.et_velocidade_cicatrizacao)
        toggleSection(R.id.tv_header_pele, R.id.et_umidade_pele, R.id.cb_pele_edema)
        toggleSection(R.id.tv_header_reparo, R.id.et_observacoes, R.id.et_data_retorno)
        toggleSection(R.id.tv_header_sociais, R.id.et_fatores_sociais, R.id.et_ingestao_agua)
        toggleSection(R.id.tv_header_historico, R.id.et_objetivo_tratamento, R.id.et_pulsos_perifericos)
        // Máscaras e validações simples
        val etDataNascimento = view.findViewById<EditText>(R.id.et_data_nascimento)
        val etDataConsulta = view.findViewById<EditText>(R.id.et_data_consulta)
        val etDataRetorno = view.findViewById<EditText>(R.id.et_data_retorno)
        val etTelefone = view.findViewById<EditText>(R.id.et_telefone)
        val etLargura = view.findViewById<EditText>(R.id.et_largura)
        val etComprimento = view.findViewById<EditText>(R.id.et_comprimento)
        val etProfundidade = view.findViewById<EditText>(R.id.et_profundidade)

        fun addDateMask(editText: EditText?) {
            editText?.addTextChangedListener(object : TextWatcher {
                private var isUpdating = false
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (isUpdating) return
                    val digits = s.toString().replace("[^0-9]".toRegex(), "")
                    val builder = StringBuilder()
                    var i = 0
                    if (digits.length > 0) { builder.append(digits.substring(0, Math.min(2, digits.length))); i = Math.min(2, digits.length) }
                    if (digits.length > 2) { builder.append('/'); builder.append(digits.substring(2, Math.min(4, digits.length))); i = Math.min(4, digits.length) }
                    if (digits.length > 4) { builder.append('/'); builder.append(digits.substring(4, Math.min(8, digits.length))); i = Math.min(8, digits.length) }
                    isUpdating = true
                    editText?.setText(builder.toString())
                    editText?.setSelection(editText.text.length)
                    isUpdating = false
                }
            })
        }

        fun addPhoneMaskBR(editText: EditText?) {
            editText?.addTextChangedListener(object : TextWatcher {
                private var isUpdating = false
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (isUpdating) return
                    val digits = s.toString().replace("[^0-9]".toRegex(), "")
                    val builder = StringBuilder()
                    var i = 0
                    if (digits.isNotEmpty()) {
                        builder.append('(')
                        val dd = digits.substring(0, Math.min(2, digits.length))
                        builder.append(dd)
                        if (digits.length >= 2) builder.append(") ")
                        i = Math.min(2, digits.length)
                    }
                    if (digits.length > 2) {
                        val nine = if (digits.length - 2 > 9) 9 else digits.length - 2
                        val part1 = digits.substring(2, 2 + Math.min(nine, 5))
                        builder.append(part1)
                        if (digits.length > 7) builder.append('-')
                    }
                    if (digits.length > 7) {
                        val part2 = digits.substring(7, Math.min(digits.length, 11))
                        builder.append(part2)
                    }
                    isUpdating = true
                    editText?.setText(builder.toString())
                    editText?.setSelection(editText.text.length)
                    isUpdating = false
                }
            })
        }

        fun addDecimalSanitizer(editText: EditText?) {
            editText?.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    // Permite dígitos, vírgula e ponto; evita múltiplos separadores
                    val txt = s?.toString() ?: return
                    val cleaned = txt.replace("[^0-9,]".toRegex(), "")
                    if (cleaned != txt) {
                        editText.setText(cleaned)
                        editText.setSelection(cleaned.length)
                    }
                }
            })
        }

        addDateMask(etDataNascimento)
        addDateMask(etDataConsulta)
        addDateMask(etDataRetorno)
        addPhoneMaskBR(etTelefone)
        addDecimalSanitizer(etLargura)
        addDecimalSanitizer(etComprimento)
        addDecimalSanitizer(etProfundidade)

        // Escolha/remoção de imagem
        view.findViewById<Button>(R.id.btn_escolher_imagem)?.setOnClickListener {
            pickImage.launch("image/*")
        }
        view.findViewById<Button>(R.id.btn_remover_imagem)?.setOnClickListener {
            selectedImageUri = null
            view.findViewById<ImageView>(R.id.img_prev_ferida)?.setImageDrawable(null)
            Toast.makeText(requireContext(), "Imagem removida", Toast.LENGTH_SHORT).show()
        }
        val btnSalvar = view.findViewById<Button>(R.id.btn_salvar_anamnese)
        btnSalvar.setOnClickListener {
            val data = JSONObject()

            fun putText(id: Int, key: String) {
                val et = view.findViewById<EditText>(id)
                if (et != null) data.put(key, et.text?.toString()?.trim() ?: "")
            }

            fun putCheck(id: Int, key: String) {
                val cb = view.findViewById<CheckBox>(id)
                if (cb != null) data.put(key, cb.isChecked)
            }

            // Dados Pessoais
            putText(R.id.et_nome_completo, "nomeCompleto")
            putText(R.id.et_data_nascimento, "dataNascimento")
            putText(R.id.et_telefone, "telefone")
            putText(R.id.et_email, "email")
            putText(R.id.et_profissao, "profissao")
            putText(R.id.et_estado_civil, "estadoCivil")

            // Dimensões/Características
            fun normalizedDecimalFrom(id: Int): String {
                val et = view.findViewById<EditText>(id)
                val raw = et?.text?.toString()?.trim() ?: ""
                return raw.replace(',', '.')
            }
            data.put("largura", normalizedDecimalFrom(R.id.et_largura))
            data.put("comprimento", normalizedDecimalFrom(R.id.et_comprimento))
            data.put("profundidade", normalizedDecimalFrom(R.id.et_profundidade))
            putText(R.id.et_localizacao, "localizacao")
            putText(R.id.et_tempo_evolucao, "tempoEvolucao")
            putText(R.id.et_etiologia, "etiologia")

            // Leito da ferida
            putText(R.id.et_granulacao_percent, "granulacaoPercent")
            putText(R.id.et_epitelizacao_percent, "epitelizacaoPercent")
            putText(R.id.et_esfacelo_percent, "esfaceloPercent")
            putText(R.id.et_necrose_seca_percent, "necroseSecaPercent")

            // Infecção/Inflamação
            putText(R.id.et_intensidade_dor, "intensidadeDor")
            putText(R.id.et_fatores_dor, "fatoresDor")
            putCheck(R.id.cb_rubor, "rubor")
            putCheck(R.id.cb_calor, "calor")
            putCheck(R.id.cb_edema, "edema")
            putCheck(R.id.cb_dor_local, "dorLocalInflamacao")
            putCheck(R.id.cb_perda_funcao, "perdaFuncao")

            putCheck(R.id.cb_eritema_perilesional, "eritemaPerilesional")
            putCheck(R.id.cb_calor_local, "calorLocal")
            putCheck(R.id.cb_edema_local, "edemaLocal")
            putCheck(R.id.cb_dor_local_inf, "dorLocalInfeccao")
            putCheck(R.id.cb_exsudato_purulento, "exsudatoPurulento")
            putCheck(R.id.cb_odor_fetido, "odorFetido")
            putCheck(R.id.cb_retardo_cicatrizacao, "retardoCicatrizacao")
            putCheck(R.id.cb_cultura_realizada, "culturaRealizada")

            // Umidade (Exsudato)
            putText(R.id.et_exsudato_quantidade, "exsudatoQuantidade")
            putText(R.id.et_exsudato_tipo, "exsudatoTipo")
            putText(R.id.et_exsudato_consistencia, "exsudatoConsistencia")

            // Bordas
            putText(R.id.et_bordas_caracteristicas, "bordasCaracteristicas")
            putText(R.id.et_bordas_fixacao, "bordasFixacao")
            putText(R.id.et_velocidade_cicatrizacao, "velocidadeCicatrizacao")
            putText(R.id.et_tunel_localizacao, "tunelLocalizacao")

            // Pele Perilesional
            putText(R.id.et_umidade_pele, "umidadePele")
            putText(R.id.et_extensao_alteracao, "extensaoAlteracao")
            putCheck(R.id.cb_pele_integra, "peleIntegra")
            putCheck(R.id.cb_pele_eritematosa, "peleEritematosa")
            putCheck(R.id.cb_pele_macerada, "peleMacerada")
            putCheck(R.id.cb_pele_seca, "peleSecaDescamativa")
            putCheck(R.id.cb_pele_eczema, "peleEczematosa")
            putCheck(R.id.cb_pele_hiperpigmentada, "peleHiperpigmentada")
            putCheck(R.id.cb_pele_hipopigmentada, "peleHipopigmentada")
            putCheck(R.id.cb_pele_indurada, "peleIndurada")
            putCheck(R.id.cb_pele_sensivel, "peleSensivel")
            putCheck(R.id.cb_pele_edema, "peleEdema")

            // Reparo e Recomendações
            putText(R.id.et_observacoes, "observacoesPlano")
            putText(R.id.et_data_consulta, "dataConsulta")
            putText(R.id.et_hora_consulta, "horaConsulta")
            putText(R.id.et_profissional, "profissionalResponsavel")
            putText(R.id.et_conselho, "conselhoProfissional")
            putText(R.id.et_data_retorno, "dataRetorno")

            // Sociais e Histórico
            putText(R.id.et_fatores_sociais, "fatoresSociaisAutocuidado")
            putText(R.id.et_nivel_atividade, "nivelAtividade")
            putText(R.id.et_compreensao_adesao, "compreensaoAdesao")
            putText(R.id.et_suporte_social, "suporteSocialCuidadores")
            putCheck(R.id.cb_atividade_fisica, "praticaAtividadeFisica")
            putCheck(R.id.cb_ingere_alcool, "ingereAlcool")
            putCheck(R.id.cb_fumante, "fumante")
            putText(R.id.et_avaliacao_nutricional, "avaliacaoNutricional")
            putText(R.id.et_ingestao_agua, "ingestaoAguaDia")
            putText(R.id.et_objetivo_tratamento, "objetivoTratamento")
            putText(R.id.et_historico_cicatrizacao, "historicoCicatrizacao")
            putCheck(R.id.cb_alergia, "alergia")
            putCheck(R.id.cb_cirurgias, "cirurgias")
            putCheck(R.id.cb_claudicacao, "claudicacaoIntermitente")
            putCheck(R.id.cb_dor_repouso, "dorRepouso")
            putText(R.id.et_pulsos_perifericos, "pulsosPerifericos")

            // Comorbidades
            putCheck(R.id.cb_dmi, "dmi")
            putCheck(R.id.cb_dmii, "dmii")
            putCheck(R.id.cb_has, "has")
            putCheck(R.id.cb_neoplasia, "neoplasia")
            putCheck(R.id.cb_hiv, "hivAids")
            putCheck(R.id.cb_obesidade, "obesidade")
            putCheck(R.id.cb_cardiopatia, "cardiopatia")
            putCheck(R.id.cb_dpoc, "dpoc")
            putCheck(R.id.cb_doenca_hematologica, "doencaHematologica")
            putCheck(R.id.cb_doenca_vascular, "doencaVascular")
            putCheck(R.id.cb_demencia, "demenciaSenil")
            putCheck(R.id.cb_insuficiencia_renal, "insuficienciaRenal")
            putCheck(R.id.cb_hanseniase, "hanseniase")
            putCheck(R.id.cb_insuficiencia_hepatica, "insuficienciaHepatica")
            putCheck(R.id.cb_doenca_autoimune, "doencaAutoimune")
            putText(R.id.et_outras_condicoes, "outrasCondicoes")

            // Medicamentos em Uso
            putCheck(R.id.cb_anti_hipertensivo, "antiHipertensivo")
            putCheck(R.id.cb_corticoides, "corticoides")
            putCheck(R.id.cb_hipoglicemiantes, "hipoglicemiantesOrais")
            putCheck(R.id.cb_aines, "aines")
            putCheck(R.id.cb_insulina, "insulina")
            putCheck(R.id.cb_drogas_vasoativas, "drogasVasoativas")
            putCheck(R.id.cb_suplemento, "suplemento")
            putCheck(R.id.cb_anticoagulante, "anticoagulante")
            putCheck(R.id.cb_vitaminico, "vitaminico")
            putCheck(R.id.cb_antirretroviral, "antirretroviral")
            putText(R.id.et_outros_medicamentos, "outrosMedicamentos")

            val json = data.toString()
            // Inclui URI da imagem, se houver
            selectedImageUri?.let { data.put("imagemUri", it.toString()) }
            // Validações simples obrigatórias
            val obrigatorios = listOf(
                R.id.et_nome_completo to "Nome é obrigatório",
                R.id.et_data_nascimento to "Data de nascimento é obrigatória",
                R.id.et_localizacao to "Localização é obrigatória"
            )
            var invalido = false
            obrigatorios.forEach { (id, msg) ->
                val et = view.findViewById<EditText>(id)
                if (et != null && et.text.isNullOrBlank()) {
                    et.error = msg
                    invalido = true
                }
            }
            if (invalido) {
                Toast.makeText(requireContext(), "Preencha os campos obrigatórios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validações avançadas
            fun parseIntSafe(id: Int): Int {
                val et = view.findViewById<EditText>(id)
                return et?.text?.toString()?.trim()?.replace("[^0-9]".toRegex(), "")?.toIntOrNull() ?: 0
            }

            val pGran = parseIntSafe(R.id.et_granulacao_percent)
            val pEpi = parseIntSafe(R.id.et_epitelizacao_percent)
            val pEsf = parseIntSafe(R.id.et_esfacelo_percent)
            val pNec = parseIntSafe(R.id.et_necrose_seca_percent)
            val somaPercentuais = pGran + pEpi + pEsf + pNec
            if (somaPercentuais > 100) {
                view.findViewById<EditText>(R.id.et_granulacao_percent)?.error = "Soma > 100%"
                view.findViewById<EditText>(R.id.et_epitelizacao_percent)?.error = "Soma > 100%"
                view.findViewById<EditText>(R.id.et_esfacelo_percent)?.error = "Soma > 100%"
                view.findViewById<EditText>(R.id.et_necrose_seca_percent)?.error = "Soma > 100%"
                Toast.makeText(requireContext(), "A soma dos percentuais do leito da ferida não pode exceder 100% (atual: ${'$'}somaPercentuais%).", Toast.LENGTH_LONG).show()
                Log.w("Anamnese", "Percentuais do leito inválidos: soma=${'$'}somaPercentuais")
                return@setOnClickListener
            }

            // E-mail
            val etEmail = view.findViewById<EditText>(R.id.et_email)
            val emailVal = etEmail?.text?.toString()?.trim().orEmpty()
            if (emailVal.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(emailVal).matches()) {
                etEmail?.error = "E-mail inválido"
                Toast.makeText(requireContext(), "Informe um e-mail válido.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Telefone básico
            val etFone = view.findViewById<EditText>(R.id.et_telefone)
            val foneDigits = etFone?.text?.toString()?.replace("[^0-9]".toRegex(), "").orEmpty()
            if (foneDigits.length < 10) {
                etFone?.error = "Telefone incompleto"
                Toast.makeText(requireContext(), "Informe um telefone válido.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Regera JSON final e loga
            val finalJson = data.toString()
            Log.d("Anamnese", finalJson)

            val intent = Intent(requireContext(), AnamnesePreviewActivity::class.java)
            intent.putExtra("anamnese_json", finalJson)
            startActivity(intent)
        }
    }
}

