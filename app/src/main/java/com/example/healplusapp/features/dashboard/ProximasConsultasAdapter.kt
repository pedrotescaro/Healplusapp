package com.example.healplusapp.features.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.healplusapp.R
import com.example.healplusapp.features.agenda.model.Agendamento

class ProximasConsultasAdapter(
    private val onItemClick: (Agendamento) -> Unit
) : ListAdapter<Agendamento, ProximasConsultasAdapter.ViewHolder>(AgendamentoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_proxima_consulta, parent, false)
        return ViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        itemView: View,
        private val onItemClick: (Agendamento) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val textData: TextView = itemView.findViewById(R.id.text_data)
        private val textHora: TextView = itemView.findViewById(R.id.text_hora)
        private val textTipo: TextView = itemView.findViewById(R.id.text_tipo)
        
        fun bind(agendamento: Agendamento) {
            textData.text = agendamento.dataAgendamento
            textHora.text = agendamento.horaAgendamento ?: "Sem horário"
            textTipo.text = agendamento.tipoConsulta ?: "Consulta"
            
            itemView.setOnClickListener {
                onItemClick(agendamento)
            }
        }
    }
}

class AgendamentoDiffCallback : DiffUtil.ItemCallback<Agendamento>() {
    override fun areItemsTheSame(oldItem: Agendamento, newItem: Agendamento): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Agendamento, newItem: Agendamento): Boolean {
        return oldItem == newItem
    }
}

