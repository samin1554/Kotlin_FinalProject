package com.example.student_pomodoro.ui.tasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.student_pomodoro.R
import com.example.student_pomodoro.Task

class TaskFragment : Fragment() {

    private lateinit var taskViewModel: TaskViewModel
    private lateinit var adapter: TaskAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_tasks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        taskViewModel = ViewModelProvider(this)[TaskViewModel::class.java]

        val recyclerView: RecyclerView = view.findViewById(R.id.task_list)
        val inputField: EditText = view.findViewById(R.id.input_field)
        val addButton: Button = view.findViewById(R.id.add_button)

        adapter = TaskAdapter(
            onDeleteClick = { task -> taskViewModel.delete(task) },
            onTaskChecked = { task, isChecked -> 
                // You might want to add a method in ViewModel to update task completion
                // taskViewModel.updateCompletion(task.id, isChecked)
            }
        )
        recyclerView.adapter = adapter

        taskViewModel.allTasks.observe(viewLifecycleOwner) { taskList ->
            adapter.submitList(taskList)
        }

        addButton.setOnClickListener {
            val taskTitle = inputField.text.toString().trim()
            if (taskTitle.isNotEmpty()) {
                val newTask = Task(title = taskTitle)
                taskViewModel.insert(newTask)
                inputField.text.clear()
            } else {
                Toast.makeText(requireContext(), "Please enter a task", Toast.LENGTH_SHORT).show()
            }
        }
    }
}