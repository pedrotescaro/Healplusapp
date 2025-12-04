package com.example.healplusapp.features.perfil

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.healplusapp.R
import com.example.healplusapp.settings.UserSettings
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.Locale

class PerfilFragment : Fragment() {

    private lateinit var settings: UserSettings
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_perfil, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // inicializa aqui
        settings = UserSettings(requireContext())

        Snackbar.make(view, getString(R.string.menu_perfil), Snackbar.LENGTH_SHORT).show()

        // Carrega informações do usuário
        loadUserInfo(view)

        val switchDark = view.findViewById<Switch>(R.id.switch_dark_mode)
        val switchContrast = view.findViewById<Switch>(R.id.switch_high_contrast)
        val seekFont = view.findViewById<SeekBar>(R.id.seek_font_scale)
        val textFont = view.findViewById<TextView>(R.id.text_font_scale)
        val spinnerLang = view.findViewById<Spinner>(R.id.spinner_language)
        val buttonSave = view.findViewById<Button>(R.id.button_save_prefs)

        // Carrega configurações salvas
        val prefs = settings.getSharedPreferences()
        switchDark.isChecked = prefs.getBoolean("pref_dark_mode", false)
        switchContrast.isChecked = prefs.getBoolean("pref_high_contrast", false)
        
        val fontScale = prefs.getFloat("pref_font_scale", 1.0f)
        val progress = (fontScale * 10).toInt().coerceIn(0, 16)
        seekFont.progress = progress
        textFont.text = "Tamanho da fonte: ${String.format(Locale.getDefault(), "%.1fx", fontScale)}"

        val langs = listOf("pt-BR", "en-US", "es-ES")
        spinnerLang.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, langs)
        
        // Seleciona o idioma salvo
        val savedLang = prefs.getString("pref_language", "pt-BR") ?: "pt-BR"
        val langIndex = langs.indexOf(savedLang)
        if (langIndex >= 0) {
            spinnerLang.setSelection(langIndex)
        }

        seekFont.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val scale = progress / 10f
                textFont.text = "Tamanho da fonte: ${String.format(Locale.getDefault(), "%.1fx", scale)}"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        buttonSave.setOnClickListener {
            val scale = seekFont.progress / 10f
            settings.setDarkModeEnabled(switchDark.isChecked)
            settings.setHighContrastEnabled(switchContrast.isChecked)
            settings.setFontScale(scale)
            settings.setLanguage(spinnerLang.selectedItem as String)

            // 🔹 recria a Activity para aplicar idioma e escala
            settings.applyToActivity(requireActivity())
            Snackbar.make(view, "Preferências salvas", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun loadUserInfo(view: View) {
        val user = auth.currentUser
        val imageView = view.findViewById<ImageView>(R.id.image_profile_photo)
        val textName = view.findViewById<TextView>(R.id.text_profile_name)
        val textEmail = view.findViewById<TextView>(R.id.text_profile_email)

        if (user != null) {
            // Atualiza nome e email
            textName?.text = user.displayName ?: "Usuário"
            textEmail?.text = user.email ?: ""

            // Carrega foto do perfil
            val photoUrl = user.photoUrl
            if (photoUrl != null && imageView != null) {
                loadImageFromUrl(photoUrl.toString(), imageView)
            }
        } else {
            textName?.text = "Usuário não autenticado"
            textEmail?.text = ""
        }
    }

    private fun loadImageFromUrl(urlString: String, imageView: ImageView) {
        lifecycleScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    val url = URL(urlString)
                    BitmapFactory.decodeStream(url.openConnection().getInputStream())
                }
                imageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                // Se falhar, mantém a imagem padrão
                e.printStackTrace()
            }
        }
    }
}
