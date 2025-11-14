package com.example.healplusapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.healplusapp.settings.UserSettings
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val settings = UserSettings(newBase)
        val newContext = settings.applyToContext(newBase)
        super.attachBaseContext(newContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Configura o listener para interceptar cliques antes do Navigation Component
        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.novaAnamneseFragment) {
                // Lógica customizada para abrir a Activity
                val intent = Intent(this, com.example.healplusapp.features.anamnese.ui.AnamneseFormActivity::class.java)
                startActivity(intent)
                // Retorna false para não selecionar o item e não prosseguir com a navegação padrão
                return@setOnItemSelectedListener false
            }
            // Para todos os outros itens, deixa o Navigation Component cuidar
            true
        }

        // Reativa o listener padrão para que o Navigation Component funcione com os outros itens
        // O re-selecionar é importante para que o setupWithNavController funcione corretamente
        bottomNav.setOnItemReselectedListener {
            // Listener vazio para evitar recarregar o mesmo fragmento
        }
    }
}

