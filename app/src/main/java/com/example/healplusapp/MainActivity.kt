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

        if (UserSettings(this).getSharedPreferences().getBoolean("pref_high_contrast", false)) {
            theme.applyStyle(R.style.ThemeOverlay_Heal_HighContrast, true)
        }

        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as androidx.navigation.fragment.NavHostFragment
        val navController = navHost.navController

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
}

