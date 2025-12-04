package com.example.healplusapp.features.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.healplusapp.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AtividadesAdapter : ListAdapter<ActivityItem, AtividadesAdapter.ViewHolder>(ActivityDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_atividade, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        
        private val textTitle: TextView = itemView.findViewById(R.id.text_title)
        private val textSubtitle: TextView = itemView.findViewById(R.id.text_subtitle)
        private val textTime: TextView = itemView.findViewById(R.id.text_time)
        
        fun bind(activity: ActivityItem) {
            textTitle.text = activity.title
            textSubtitle.text = activity.subtitle
            textTime.text = formatTime(activity.timestamp)
        }
        
        private fun formatTime(timestamp: Long): String {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            return dateFormat.format(Date(timestamp))
        }
    }
}

class ActivityDiffCallback : DiffUtil.ItemCallback<ActivityItem>() {
    override fun areItemsTheSame(oldItem: ActivityItem, newItem: ActivityItem): Boolean {
        return oldItem.timestamp == newItem.timestamp && oldItem.type == newItem.type
    }

    override fun areContentsTheSame(oldItem: ActivityItem, newItem: ActivityItem): Boolean {
        return oldItem == newItem
    }
}

