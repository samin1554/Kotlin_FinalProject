package com.example.student_pomodoro.ui.tasks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.student_pomodoro.R
import com.example.student_pomodoro.Task

class TaskAdapter(
    private val onDeleteClick: (Task) -> Unit,
    private val onTaskChecked: (Task, Boolean) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = getItem(position)
        holder.bind(task)
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.task_title)
        private val pomodoroText: TextView = itemView.findViewById(R.id.pomodoro_count)
        private val checkBox: CheckBox = itemView.findViewById(R.id.task_checkbox)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.delete_button)

        fun bind(task: Task) {
            titleText.text = task.title
            pomodoroText.text = "${task.completedPomodoros} Pomodoros"
            checkBox.isChecked = task.isCompleted

            deleteButton.setOnClickListener { onDeleteClick(task) }
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
