package com.example.healplusapp.features.fichas

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.healplusapp.R
import com.example.healplusapp.features.fichas.ui.FichasActivity

class MinhasFichasFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_minhas_fichas, container, false)
        
        // Abrir Activity de Fichas ao clicar
        view.setOnClickListener {
            startActivity(Intent(requireContext(), FichasActivity::class.java))
        }
        
        return view
    }
}

