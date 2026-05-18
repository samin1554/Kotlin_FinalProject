package com.example.student_pomodoro.ui.tasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.student_pomodoro.AppDatabase
import com.example.student_pomodoro.Task
import com.example.student_pomodoro.TaskRepository
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TaskRepository
    val allTasks: LiveData<List<Task>>

    init {
        val dao = AppDatabase.getDatabase(application).taskDao()
        repository = TaskRepository(dao)
        allTasks = repository.allTasks
    }

    fun insert(task: Task) = viewModelScope.launch {
        repository.insert(task)
    }

    fun delete(task: Task) = viewModelScope.launch {
        repository.delete(task)
    }

    fun setCompleted(taskId: Int, completed: Boolean) = viewModelScope.launch {
        repository.setCompleted(taskId, completed)
    }

    fun updateTitle(taskId: Int, newTitle: String) = viewModelScope.launch {
        repository.updateTitle(taskId, newTitle)
    }
}
