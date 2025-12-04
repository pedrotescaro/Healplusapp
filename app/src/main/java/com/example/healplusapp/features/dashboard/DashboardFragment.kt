package com.example.healplusapp.features.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import coil.load
import com.example.healplusapp.R
import android.content.Intent
import com.example.healplusapp.features.anamnese.ui.AnamneseListActivity
import com.google.firebase.auth.FirebaseAuth

class DashboardFragment : Fragment() {
    
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_dashboard, container, false)
        val btn = v.findViewById<Button>(R.id.btn_open_anamnese_list)
        btn?.setOnClickListener {
            startActivity(Intent(requireContext(), AnamneseListActivity::class.java))
        }
        
        // Carrega informações do usuário
        loadUserInfo(v)
        
        return v
    }

    private fun loadUserInfo(view: View) {
        val user = auth.currentUser
        val imageView = view.findViewById<ImageView>(R.id.image_dashboard_photo)
        val textUser = view.findViewById<TextView>(R.id.text_dashboard_user)

        if (user != null) {
            // Atualiza mensagem de boas-vindas
            val userName = user.displayName ?: user.email?.substringBefore("@") ?: "Usuário"
            textUser?.text = "Olá, $userName!"

            // Carrega foto do perfil usando Coil (com cache automático)
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

