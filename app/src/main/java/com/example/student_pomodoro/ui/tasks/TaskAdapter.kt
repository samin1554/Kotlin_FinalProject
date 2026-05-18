package com.example.student_pomodoro.ui.tasks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.student_pomodoro.R
import com.example.student_pomodoro.Task

class TaskAdapter(
    private val onDeleteClick: (Task) -> Unit,
    private val onTaskChecked: (Task, Boolean) -> Unit,
    private val onTaskSelected: (Task) -> Unit,
    private val onTaskEdit: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    var selectedTaskId: Int = -1
        set(value) {
            val oldValue = field
            field = value
            val oldPosition = currentList.indexOfFirst { it.id == oldValue }
            val newPosition = currentList.indexOfFirst { it.id == value }
            if (oldPosition != -1) notifyItemChanged(oldPosition)
            if (newPosition != -1) notifyItemChanged(newPosition)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = getItem(position)
        holder.bind(task, task.id == selectedTaskId)
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.task_title)
        private val pomodoroText: TextView = itemView.findViewById(R.id.pomodoro_count)
        private val checkBox: CheckBox = itemView.findViewById(R.id.task_checkbox)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.delete_button)
        private val cardView = itemView as com.google.android.material.card.MaterialCardView

        fun bind(task: Task, isSelected: Boolean) {
            titleText.text = task.title
            pomodoroText.text = "${task.completedPomodoros} Pomodoros"
            checkBox.isChecked = task.isCompleted

            if (isSelected) {
                cardView.strokeWidth = 4
                cardView.strokeColor = ContextCompat.getColor(itemView.context, R.color.pomodoro_primary)
                cardView.cardElevation = 4f
            } else {
                cardView.strokeWidth = 0
                cardView.cardElevation = 1f
            }

            itemView.setOnClickListener {
                onTaskSelected(task)
            }

            itemView.setOnLongClickListener {
                onTaskEdit(task)
                true
            }

            deleteButton.setOnClickListener {
                androidx.appcompat.app.AlertDialog.Builder(itemView.context)
                    .setTitle("Delete Task")
                    .setMessage("Are you sure you want to delete '${task.title}'?")
                    .setPositiveButton("Delete") { _, _ -> onDeleteClick(task) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                onTaskChecked(task, isChecked)
            }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem == newItem
        }
    }
}
