package com.example.student_pomodoro

import androidx.lifecycle.LiveData

class TaskRepository(private val taskDao: TaskDao) {

    val allTasks: LiveData<List<Task>> = taskDao.getAllTasks()

    suspend fun insert(task: Task) {
        taskDao.insert(task)
    }

    suspend fun delete(task: Task) {
        taskDao.delete(task)
    }

    suspend fun incrementPomodoro(taskId: Int) {
        taskDao.incrementPomodoro(taskId)
    }

    suspend fun setCompleted(taskId: Int, completed: Boolean) {
        taskDao.setCompleted(taskId, completed)
    }

    suspend fun getTaskById(taskId: Int): Task? {
        return taskDao.getTaskById(taskId)
    }

    suspend fun updateTitle(taskId: Int, newTitle: String) {
        taskDao.updateTitle(taskId, newTitle)
    }
}
