package com.example.student_pomodoro.ui.timer

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.student_pomodoro.StreakManager
import kotlinx.coroutines.launch

class TimeViewModel(application: Application) : AndroidViewModel(application) {

    private val streakManager = StreakManager(application)
    
    val currentStreak: LiveData<Int> = streakManager.currentStreakFlow.asLiveData()
    val longestStreak: LiveData<Int> = streakManager.longestStreakFlow.asLiveData()

    var workDurationMin = 25
    var breakDurationMin = 5

    private val _timeState = MutableLiveData(TimeState.IDLE)
    val timeState: LiveData<TimeState> = _timeState

    private val _timeLeft = MutableLiveData<Long>() // seconds
    val timeLeft: LiveData<Long> = _timeLeft

    private val _currentSession = MutableLiveData(1)
    val currentSession: LiveData<Int> = _currentSession

    private val _isWorkingSession = MutableLiveData(true) // working = true, false = on break
    val isWorkingSession: LiveData<Boolean> = _isWorkingSession

    private var handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null

    fun startTimer() {
        if (_timeState.value == TimeState.IDLE) {
            _timeLeft.value = if (_isWorkingSession.value == true) {
                workDurationMin * 60L
            } else {
                breakDurationMin * 60L
            }
        }

        _timeState.value = TimeState.RUNNING
        runnable = object : Runnable {
            override fun run() {
                val current = _timeLeft.value ?: 0
                if (current > 0) {
                    _timeLeft.postValue(current - 1)
                    handler.postDelayed(this, 1000)
                } else {
                    onTimerFinished()
                }
            }
        }
        handler.postDelayed(runnable!!, 1000)
    }

    fun pauseTimer() {
        _timeState.value = TimeState.PAUSED
        runnable?.let { handler.removeCallbacks(it) }
    }

    fun resetTimer() {
        if (_timeState.value != TimeState.IDLE) {
            viewModelScope.launch {
                streakManager.resetCurrentStreak()
            }
        }
        runnable?.let { handler.removeCallbacks(it) }
        _timeState.value = TimeState.IDLE
        _timeLeft.value = workDurationMin * 60L
    }

    private fun onTimerFinished() {
        _timeState.value = TimeState.IDLE

        if (_isWorkingSession.value == true) {
            // Work session done - increment streak
            viewModelScope.launch {
                streakManager.incrementStreak()
            }
            
            val session = _currentSession.value ?: 1
            if (session >= 4) {
                _currentSession.value = 1
            } else {
                _currentSession.value = session + 1
            }
            _isWorkingSession.value = false
            _timeLeft.value = breakDurationMin * 60L
        } else {
            // Break done - back to work
            _isWorkingSession.value = true
            _timeLeft.value = workDurationMin * 60L
        }
    }

    fun updateDuration(workMin: Int, breakMin: Int) {
        workDurationMin = workMin
        breakDurationMin = breakMin
        if (_timeState.value == TimeState.IDLE) {
            _timeLeft.value = if (_isWorkingSession.value == true) workMin * 60L else breakMin * 60L
        }
    }

    override fun onCleared() {
        super.onCleared()
        runnable?.let { handler.removeCallbacks(it) }
    }
}
