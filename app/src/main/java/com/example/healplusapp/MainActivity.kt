package com.example.healplusapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.healplusapp.features.agenda.data.AgendamentoRepository
import com.example.healplusapp.features.anamnese.data.AnamneseRepository
import com.example.healplusapp.features.agenda.model.Agendamento
import com.example.healplusapp.features.anamnese.model.Anamnese
import com.example.healplusapp.settings.UserSettings
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var anamneseRepository: AnamneseRepository
    @Inject lateinit var agendamentoRepository: AgendamentoRepository
    @Inject lateinit var pacienteRepository: com.example.healplusapp.features.fichas.data.PacienteRepository

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val anamneseSyncCache = mutableMapOf<String, Int>()
    private val agendamentoSyncCache = mutableMapOf<String, Int>()
    private val pacienteSyncCache = mutableMapOf<String, Int>()
    private var profileSettingsHash: Int? = null
    private var sharedPrefs: SharedPreferences? = null
    private lateinit var profileDocumentId: String

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, _ ->
        syncProfileSettingsSnapshot(sharedPreferences)
    }

    override fun attachBaseContext(newBase: Context) {
        val settings = UserSettings(newBase)
        val newContext = settings.applyToContext(newBase)
        super.attachBaseContext(newContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Verifica autenticação primeiro (operação rápida)
        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, com.example.healplusapp.features.auth.LoginActivity::class.java))
            finish()
            return
        }

        // Carrega configurações (operação rápida)
        val userSettings = UserSettings(this)
        sharedPrefs = userSettings.getSharedPreferences().also {
            it.registerOnSharedPreferenceChangeListener(prefListener)
        }

        // Aplica tema antes de setContentView
        if (sharedPrefs?.getBoolean("pref_high_contrast", false) == true) {
            theme.applyStyle(R.style.ThemeOverlay_Heal_HighContrast, true)
        }

        setContentView(R.layout.activity_main)
        
        // Inicializa componentes da UI (operação rápida)
        profileDocumentId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            ?: UUID.randomUUID().toString()

        setupNavigation()

        // Operações pesadas em background (não bloqueiam onCreate)
        setupFirestoreBackends()
    }
    
    private fun setupNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? androidx.navigation.fragment.NavHostFragment
        val navController = navHost?.navController ?: return

        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.novaAnamneseFragment) {
                val intent = Intent(this, com.example.healplusapp.features.anamnese.ui.AnamneseFormActivity::class.java)
                startActivity(intent)
                return@setOnItemSelectedListener false
            }
            androidx.navigation.ui.NavigationUI.onNavDestinationSelected(item, navController)
            true
        }

        bottomNav.setOnItemReselectedListener { }
    }

    override fun onDestroy() {
        sharedPrefs?.unregisterOnSharedPreferenceChangeListener(prefListener)
        super.onDestroy()
    }

    private fun setupFirestoreBackends() {
        // Inicia observadores de forma assíncrona (não bloqueia onCreate)
        observeAnamneses()
        observeAgendamentos()
        observePacientes()
        // Sincroniza configurações do perfil de forma assíncrona
        sharedPrefs?.let { syncProfileSettingsSnapshot(it) }
    }

    private fun observeAnamneses() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                anamneseRepository.getAllAtivas()
                    .debounce(SYNC_DEBOUNCE_MS)
                    .collectLatest { saveAnamnesesInFirestore(it) }
            }
        }
    }

    private fun observeAgendamentos() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                agendamentoRepository.getAllAtivos()
                    .debounce(SYNC_DEBOUNCE_MS)
                    .collectLatest { saveAgendamentosInFirestore(it) }
            }
        }
    }

    private fun observePacientes() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                pacienteRepository.getAllAtivos()
                    .debounce(SYNC_DEBOUNCE_MS)
                    .collectLatest { savePacientesInFirestore(it) }
            }
        }
    }

    private suspend fun saveAnamnesesInFirestore(anamneses: List<Anamnese>) {
        if (anamneses.isEmpty()) return
        val collection = firestore.collection("anamneses")
        for (anamnese in anamneses) {
            val docId = anamnese.id?.toString() ?: continue
            val payloadForHash = anamnese.toFirestorePayload(includeTimestamp = false)
            val newHash = payloadForHash.hashCode()
            if (anamneseSyncCache[docId] == newHash) continue

            val payload = payloadForHash.toMutableMap().apply {
                put("updatedAt", Timestamp.now())
            }

            try {
                collection.document(docId)
                    .set(payload, SetOptions.merge())
                    .await()
                anamneseSyncCache[docId] = newHash
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao salvar anamnese $docId no Firestore", e)
            }
        }
    }

    private suspend fun saveAgendamentosInFirestore(agendamentos: List<Agendamento>) {
        if (agendamentos.isEmpty()) return
        val collection = firestore.collection("agendamentos")
        for (agendamento in agendamentos) {
            val docId = agendamento.id?.toString() ?: continue
            val payloadForHash = agendamento.toFirestorePayload(includeTimestamp = false)
            val newHash = payloadForHash.hashCode()
            if (agendamentoSyncCache[docId] == newHash) continue

            val payload = payloadForHash.toMutableMap().apply {
                put("updatedAt", Timestamp.now())
            }

            try {
                collection.document(docId)
                    .set(payload, SetOptions.merge())
                    .await()
                agendamentoSyncCache[docId] = newHash
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao salvar agendamento $docId no Firestore", e)
            }
        }
    }

    private suspend fun savePacientesInFirestore(pacientes: List<com.example.healplusapp.features.fichas.model.Paciente>) {
        if (pacientes.isEmpty()) return
        val collection = firestore.collection("pacientes")
        for (paciente in pacientes) {
            val docId = paciente.id?.toString() ?: continue
            val payloadForHash = mutableMapOf<String, Any>(
                "nomeCompleto" to paciente.nomeCompleto
            ).apply {
                paciente.dataNascimento?.takeIf { it.isNotBlank() }?.let { this["dataNascimento"] = it }
                paciente.telefone?.takeIf { it.isNotBlank() }?.let { this["telefone"] = it }
                paciente.email?.takeIf { it.isNotBlank() }?.let { this["email"] = it }
                paciente.profissao?.takeIf { it.isNotBlank() }?.let { this["profissao"] = it }
                paciente.estadoCivil?.takeIf { it.isNotBlank() }?.let { this["estadoCivil"] = it }
                paciente.observacoes?.takeIf { it.isNotBlank() }?.let { this["observacoes"] = it }
            }
            val newHash = payloadForHash.hashCode()
            if (pacienteSyncCache[docId] == newHash) continue

            val payload = payloadForHash.toMutableMap().apply {
                put("updatedAt", Timestamp.now())
            }

            try {
                collection.document(docId)
                    .set(payload, SetOptions.merge())
                    .await()
                pacienteSyncCache[docId] = newHash
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao salvar paciente $docId no Firestore", e)
            }
        }
    }

    private fun syncProfileSettingsSnapshot(preferences: SharedPreferences) {
        val payloadForHash = buildProfileSettingsPayload(preferences, includeTimestamp = false)
        val newHash = payloadForHash.hashCode()
        if (profileSettingsHash == newHash) return

        val payload = payloadForHash.toMutableMap().apply {
            put("updatedAt", Timestamp.now())
        }

        lifecycleScope.launch {
            try {
                firestore.collection("profile_settings")
                    .document(profileDocumentId)
                    .set(payload, SetOptions.merge())
                    .await()
                profileSettingsHash = newHash
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao salvar Profile Settings no Firestore", e)
            }
        }
    }

    private fun buildProfileSettingsPayload(
        preferences: SharedPreferences,
        includeTimestamp: Boolean = true
    ): MutableMap<String, Any> {
        val payload = mutableMapOf<String, Any>(
            "darkMode" to preferences.getBoolean("pref_dark_mode", false),
            "highContrast" to preferences.getBoolean("pref_high_contrast", false),
            "fontScale" to preferences.getFloat("pref_font_scale", 1.0f).toDouble(),
            "language" to (preferences.getString("pref_language", Locale.getDefault().toLanguageTag()) ?: "pt-BR"),
            "deviceId" to profileDocumentId
        )
        if (includeTimestamp) {
            payload["updatedAt"] = Timestamp.now()
        }
        return payload
    }

    private fun Anamnese.toFirestorePayload(includeTimestamp: Boolean = true): MutableMap<String, Any> {
        val payload = mutableMapOf<String, Any>(
            "nomeCompleto" to nomeCompleto,
            "dadosJson" to dadosJson
        )
        dataConsulta?.takeIf { it.isNotBlank() }?.let { payload["dataConsulta"] = it }
        localizacao?.takeIf { it.isNotBlank() }?.let { payload["localizacao"] = it }
        if (includeTimestamp) {
            payload["updatedAt"] = Timestamp.now()
        }
        return payload
    }

    private fun Agendamento.toFirestorePayload(includeTimestamp: Boolean = true): MutableMap<String, Any> {
        val payload = mutableMapOf<String, Any>(
            "status" to status,
            "dataAgendamento" to dataAgendamento
        )
        pacienteId?.let { payload["pacienteId"] = it }
        horaAgendamento?.takeIf { it.isNotBlank() }?.let { payload["horaAgendamento"] = it }
        tipoConsulta?.takeIf { it.isNotBlank() }?.let { payload["tipoConsulta"] = it }
        observacoes?.takeIf { it.isNotBlank() }?.let { payload["observacoes"] = it }
        if (includeTimestamp) {
            payload["updatedAt"] = Timestamp.now()
        }
        return payload
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val SYNC_DEBOUNCE_MS = 400L
    }
}
