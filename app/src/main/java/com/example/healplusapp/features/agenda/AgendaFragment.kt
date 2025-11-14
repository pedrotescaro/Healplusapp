package com.example.healplusapp.features.agenda

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.healplusapp.R
import com.example.healplusapp.features.agenda.ui.AgendaActivity

class AgendaFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_agenda, container, false)
        
        // Abrir Activity de Agenda ao clicar
        view.setOnClickListener {
            startActivity(Intent(requireContext(), AgendaActivity::class.java))
        }
        
        return view
    }
}

