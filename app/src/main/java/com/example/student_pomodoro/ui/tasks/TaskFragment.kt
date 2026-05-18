package com.example.student_pomodoro.ui.tasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.student_pomodoro.R
import com.example.student_pomodoro.SlideInItemAnimator
import com.example.student_pomodoro.Task
import com.example.student_pomodoro.databinding.FragmentTasksBinding
import com.example.student_pomodoro.ui.timer.TimeViewModel

class TaskFragment : Fragment() {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!

    private lateinit var taskViewModel: TaskViewModel
    private val timerViewModel: TimeViewModel by activityViewModels()
    private lateinit var adapter: TaskAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        taskViewModel = ViewModelProvider(this)[TaskViewModel::class.java]

        adapter = TaskAdapter(
            onDeleteClick = { task -> taskViewModel.delete(task) },
            onTaskChecked = { task, isChecked ->
                taskViewModel.setCompleted(task.id, isChecked)
            },
            onTaskSelected = { task ->
                timerViewModel.setActiveTask(task)
                adapter.selectedTaskId = task.id
                Toast.makeText(requireContext(), "'${task.title}' selected for timer", Toast.LENGTH_SHORT).show()
            },
            onTaskEdit = { task ->
                showEditTaskDialog(task)
            }
        )

        val recyclerView: RecyclerView = binding.taskList
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = SlideInItemAnimator()

        taskViewModel.allTasks.observe(viewLifecycleOwner) { taskList ->
            adapter.submitList(taskList)
            if (taskList.isEmpty()) {
                binding.emptyStateLayout.visibility = View.VISIBLE
                binding.taskList.visibility = View.GONE
            } else {
                binding.emptyStateLayout.visibility = View.GONE
                binding.taskList.visibility = View.VISIBLE
            }
        }

        timerViewModel.activeTask.observe(viewLifecycleOwner) { task ->
            adapter.selectedTaskId = task?.id ?: -1
        }

        binding.addButton.setOnClickListener {
            val taskTitle = binding.inputField.text.toString().trim()
            if (taskTitle.isNotEmpty()) {
                val newTask = Task(title = taskTitle)
                taskViewModel.insert(newTask)
                binding.inputField.text.clear()
            } else {
                Toast.makeText(requireContext(), "Please enter a task", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditTaskDialog(task: Task) {
        val editText = EditText(requireContext()).apply {
            setText(task.title)
            setSingleLine()
            setSelection(task.title.length)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Edit Task")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newTitle = editText.text.toString().trim()
                if (newTitle.isNotEmpty() && newTitle != task.title) {
                    taskViewModel.updateTitle(task.id, newTitle)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
