package com.example.student_pomodoro.ui.stats

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.student_pomodoro.R
import com.example.student_pomodoro.Session
import com.example.student_pomodoro.SessionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SessionAdapter : ListAdapter<Session, SessionAdapter.SessionViewHolder>(SessionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_session, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val typeBadge: CardView = itemView.findViewById(R.id.type_badge)
        private val typeText: TextView = itemView.findViewById(R.id.session_type_text)
        private val timeText: TextView = itemView.findViewById(R.id.session_time_text)
        private val durationText: TextView = itemView.findViewById(R.id.session_duration_text)

        private val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

        fun bind(session: Session) {
            val (label, colorRes) = when (session.type) {
                SessionType.WORK -> "Focus" to R.color.focus_tint
                SessionType.BREAK -> "Break" to R.color.break_tint
                SessionType.LONG_BREAK -> "Long Break" to R.color.long_break_tint
            }
            typeText.text = label
            typeBadge.setCardBackgroundColor(ContextCompat.getColor(itemView.context, colorRes))
            timeText.text = dateFormat.format(Date(session.timestamp))
            durationText.text = "${session.durationMinutes} min"
        }
    }

    class SessionDiffCallback : DiffUtil.ItemCallback<Session>() {
        override fun areItemsTheSame(oldItem: Session, newItem: Session): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Session, newItem: Session): Boolean {
            return oldItem == newItem
        }
    }
}
