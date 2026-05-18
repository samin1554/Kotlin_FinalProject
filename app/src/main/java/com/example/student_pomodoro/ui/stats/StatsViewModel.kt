package com.example.student_pomodoro.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.student_pomodoro.AppDatabase
import com.example.student_pomodoro.BestDay
import com.example.student_pomodoro.DayCount
import com.example.student_pomodoro.HourCount
import com.example.student_pomodoro.Session
import com.example.student_pomodoro.SessionRepository
import com.example.student_pomodoro.SettingsDataStore
import com.example.student_pomodoro.StreakManager
import com.example.student_pomodoro.TypeCount
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionRepository: SessionRepository
    private val settingsDataStore = SettingsDataStore(application)
    val allSessions: LiveData<List<Session>>

    private val streakManager = StreakManager(application)
    val currentStreak: LiveData<Int> = streakManager.currentStreakFlow.asLiveData()
    val longestStreak: LiveData<Int> = streakManager.longestStreakFlow.asLiveData()

    private val _todayPomodoros = MutableLiveData(0)
    val todayPomodoros: LiveData<Int> = _todayPomodoros

    private val _todayFocusMinutes = MutableLiveData(0)
    val todayFocusMinutes: LiveData<Int> = _todayFocusMinutes

    private val _weekPomodoros = MutableLiveData(0)
    val weekPomodoros: LiveData<Int> = _weekPomodoros

    private val _last7Days = MutableLiveData<List<DayCount>>(emptyList())
    val last7Days: LiveData<List<DayCount>> = _last7Days

    private val _typeDistribution = MutableLiveData<List<TypeCount>>(emptyList())
    val typeDistribution: LiveData<List<TypeCount>> = _typeDistribution

    private val _hourlyProductivity = MutableLiveData<List<HourCount>>(emptyList())
    val hourlyProductivity: LiveData<List<HourCount>> = _hourlyProductivity

    private val _totalPomodoros = MutableLiveData(0)
    val totalPomodoros: LiveData<Int> = _totalPomodoros

    private val _totalFocusHours = MutableLiveData(0)
    val totalFocusHours: LiveData<Int> = _totalFocusHours

    private val _bestDay = MutableLiveData<BestDay?>(null)
    val bestDay: LiveData<BestDay?> = _bestDay

    private val _dailyGoal = MutableLiveData(SettingsDataStore.DEFAULT_DAILY_GOAL)
    val dailyGoal: LiveData<Int> = _dailyGoal

    init {
        val db = AppDatabase.getDatabase(application)
        sessionRepository = SessionRepository(db.sessionDao())
        allSessions = sessionRepository.allSessions
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            _todayPomodoros.value = sessionRepository.getCompletedPomodorosToday()
            _todayFocusMinutes.value = sessionRepository.getTotalFocusMinutesToday()
            _weekPomodoros.value = sessionRepository.getCompletedPomodorosThisWeek()
            _last7Days.value = sessionRepository.getPomodorosLast7Days()
            _typeDistribution.value = sessionRepository.getSessionTypeDistribution()
            _hourlyProductivity.value = sessionRepository.getSessionsByHourOfDay()
            _totalPomodoros.value = sessionRepository.getTotalPomodoros()
            _totalFocusHours.value = sessionRepository.getTotalFocusMinutes() / 60
            _bestDay.value = sessionRepository.getBestDay()
            _dailyGoal.value = settingsDataStore.dailyGoal.first()
        }
    }

    fun refresh() {
        loadStats()
    }
}
