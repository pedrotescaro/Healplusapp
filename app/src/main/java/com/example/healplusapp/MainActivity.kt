package com.example.healplusapp

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.example.healplusapp.settings.UserSettings
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    private var isUpdatingMenuFromNav = false
    
    override fun attachBaseContext(newBase: Context) {
        val settings = UserSettings(newBase)
        val newContext = settings.applyToContext(newBase)
        super.attachBaseContext(newContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        
        // Gerenciar navegação manualmente para ter controle total
        bottomNav.setOnItemSelectedListener { item ->
            // Ignorar se a atualização veio da navegação (evitar loop)
            if (isUpdatingMenuFromNav) {
                return@setOnItemSelectedListener true
            }
            
            when (item.itemId) {
                R.id.novaAnamneseFragment -> {
                    // Abrir Activity ao invés de Fragment
                    val intent = android.content.Intent(this, com.example.healplusapp.features.anamnese.ui.AnamneseFormActivity::class.java)
                    startActivity(intent)
                    // Manter o item anterior selecionado (não atualizar o menu)
                    false
                }
                else -> {
                    // Navegar para o fragment correspondente
                    try {
                        navController.navigate(item.itemId)
                        true
                    } catch (e: Exception) {
                        // Se houver erro, não atualizar a seleção
                        false
                    }
                }
            }
        }
        
        // Sincronizar o menu com o destino atual da navegação
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Atualizar o menu apenas se não for o item "Nova Anamnese"
            if (destination.id != R.id.novaAnamneseFragment) {
                isUpdatingMenuFromNav = true
                bottomNav.selectedItemId = destination.id
                isUpdatingMenuFromNav = false
            }
        }
    }
}

