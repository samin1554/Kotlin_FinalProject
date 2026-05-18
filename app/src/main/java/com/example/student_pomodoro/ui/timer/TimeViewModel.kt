package com.example.student_pomodoro.ui.timer

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.student_pomodoro.AlarmReceiver
import com.example.student_pomodoro.AppDatabase
import com.example.student_pomodoro.Session
import com.example.student_pomodoro.SessionRepository
import com.example.student_pomodoro.SessionType
import com.example.student_pomodoro.SettingsDataStore
import com.example.student_pomodoro.StreakManager
import com.example.student_pomodoro.Task
import com.example.student_pomodoro.TaskRepository
import com.example.student_pomodoro.NotificationHelper
import com.example.student_pomodoro.TimerNotificationHelper
import com.example.student_pomodoro.TimerPreferences
import com.example.student_pomodoro.TimerWidgetProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TimeViewModel(application: Application) : AndroidViewModel(application) {

    private val streakManager = StreakManager(application)
    private val settingsDataStore = SettingsDataStore(application)
    private val timerPreferences = TimerPreferences(application)

    private val sessionRepository: SessionRepository
    private val taskRepository: TaskRepository

    private val _workDurationMin = MutableLiveData(SettingsDataStore.DEFAULT_WORK_DURATION)
    val workDurationMin: LiveData<Int> = _workDurationMin

    private val _breakDurationMin = MutableLiveData(SettingsDataStore.DEFAULT_BREAK_DURATION)
    val breakDurationMin: LiveData<Int> = _breakDurationMin

    private val _longBreakDurationMin = MutableLiveData(SettingsDataStore.DEFAULT_LONG_BREAK_DURATION)
    val longBreakDurationMin: LiveData<Int> = _longBreakDurationMin

    private val _sessionsBeforeLongBreak = MutableLiveData(SettingsDataStore.DEFAULT_SESSIONS_BEFORE_LONG_BREAK)
    val sessionsBeforeLongBreak: LiveData<Int> = _sessionsBeforeLongBreak

    private val _autoStartEnabled = MutableLiveData(false)
    val autoStartEnabled: LiveData<Boolean> = _autoStartEnabled

    private val _dailyGoal = MutableLiveData(SettingsDataStore.DEFAULT_DAILY_GOAL)
    val dailyGoal: LiveData<Int> = _dailyGoal

    val currentStreak: LiveData<Int> = streakManager.currentStreakFlow.asLiveData()
    val longestStreak: LiveData<Int> = streakManager.longestStreakFlow.asLiveData()

    private val _timeState = MutableLiveData(TimeState.IDLE)
    val timeState: LiveData<TimeState> = _timeState

    private val _timeLeft = MutableLiveData<Long>()
    val timeLeft: LiveData<Long> = _timeLeft

    private val _currentSession = MutableLiveData(1)
    val currentSession: LiveData<Int> = _currentSession

    private val _isWorkingSession = MutableLiveData(true)
    val isWorkingSession: LiveData<Boolean> = _isWorkingSession

    private val _isLongBreak = MutableLiveData(false)
    val isLongBreak: LiveData<Boolean> = _isLongBreak

    private val _activeTask = MutableLiveData<Task?>(null)
    val activeTask: LiveData<Task?> = _activeTask

    private val _totalSessionsToday = MutableLiveData(0)
    val totalSessionsToday: LiveData<Int> = _totalSessionsToday

    private var countDownTimer: CountDownTimer? = null
    private var infiniteFocusHandler: android.os.Handler? = null
    private var infiniteFocusRunnable: Runnable? = null
    private var infiniteFocusStartTime: Long = 0
    private var infiniteFocusElapsedSecs: Long = 0
    private var finishTimeMillis: Long = 0
    private var totalDurationSecs: Long = 0
    private var pendingIntent: PendingIntent? = null

    init {
        val db = AppDatabase.getDatabase(application)
        sessionRepository = SessionRepository(db.sessionDao())
        taskRepository = TaskRepository(db.taskDao())

        viewModelScope.launch {
            _workDurationMin.value = settingsDataStore.workDuration.first()
            _breakDurationMin.value = settingsDataStore.breakDuration.first()
            _longBreakDurationMin.value = settingsDataStore.longBreakDuration.first()
            _sessionsBeforeLongBreak.value = settingsDataStore.sessionsBeforeLongBreak.first()
            _autoStartEnabled.value = settingsDataStore.autoStartEnabled.first()
            _dailyGoal.value = settingsDataStore.dailyGoal.first()
            restoreTimerState()
            loadTodayStats()
        }

        // Observe settings changes for subsequent updates
        viewModelScope.launch {
            settingsDataStore.workDuration.collect { duration ->
                _workDurationMin.value = duration
                if (_timeState.value == TimeState.IDLE && _isWorkingSession.value == true) {
                    _timeLeft.value = duration * 60L
                }
            }
        }
        viewModelScope.launch {
            settingsDataStore.breakDuration.collect { duration ->
                _breakDurationMin.value = duration
                if (_timeState.value == TimeState.IDLE && _isWorkingSession.value == false && _isLongBreak.value == false) {
                    _timeLeft.value = duration * 60L
                }
            }
        }
        viewModelScope.launch {
            settingsDataStore.longBreakDuration.collect { duration ->
                _longBreakDurationMin.value = duration
                if (_timeState.value == TimeState.IDLE && _isLongBreak.value == true) {
                    _timeLeft.value = duration * 60L
                }
            }
        }
        viewModelScope.launch {
            settingsDataStore.sessionsBeforeLongBreak.collect { count ->
                _sessionsBeforeLongBreak.value = count
            }
        }
        viewModelScope.launch {
            settingsDataStore.autoStartEnabled.collect { enabled ->
                _autoStartEnabled.value = enabled
            }
        }
        viewModelScope.launch {
            settingsDataStore.dailyGoal.collect { goal ->
                _dailyGoal.value = goal
            }
        }
    }

    private suspend fun restoreTimerState() {
        val wasRunning = timerPreferences.isRunning.first()
        if (wasRunning) {
            val finish = timerPreferences.finishTimeMillis.first()
            val now = System.currentTimeMillis()
            if (finish > now) {
                _currentSession.value = timerPreferences.currentSession.first()
                _isWorkingSession.value = timerPreferences.isWorkingSession.first()
                _isLongBreak.value = timerPreferences.isLongBreak.first()
                val taskId = timerPreferences.activeTaskId.first()
                if (taskId >= 0) {
                    _activeTask.value = taskRepository.getTaskById(taskId)
                }
                finishTimeMillis = finish
                val remaining = ((finish - now) / 1000).coerceAtLeast(0)
                totalDurationSecs = getCurrentSessionDurationSecs()
                _timeLeft.value = remaining
                _timeState.value = TimeState.RUNNING
                startCountDown(remaining)
            } else {
                timerPreferences.clearState()
                _timeState.value = TimeState.IDLE
                _timeLeft.value = _workDurationMin.value!! * 60L
            }
        } else {
            _timeState.value = TimeState.IDLE
            _timeLeft.value = _workDurationMin.value!! * 60L
        }
    }

    fun startTimer() {
        if (_timeState.value == TimeState.INFINITE_FOCUS) {
            stopInfiniteFocus()
            return
        }

        if (_timeState.value == TimeState.IDLE) {
            val totalSecs = getCurrentSessionDurationSecs()
            totalDurationSecs = totalSecs
            _timeLeft.value = totalSecs
        }

        val remaining = _timeLeft.value ?: getCurrentSessionDurationSecs()
        finishTimeMillis = System.currentTimeMillis() + (remaining * 1000)

        _timeState.value = TimeState.RUNNING
        startCountDown(remaining)
        saveTimerState()
        scheduleAlarm(finishTimeMillis)
        TimerWidgetProvider.updateWidgets(getApplication())
    }

    fun startInfiniteFocus() {
        countDownTimer?.cancel()
        cancelAlarm()
        TimerNotificationHelper.dismissTimerNotification(getApplication())

        _timeState.value = TimeState.INFINITE_FOCUS
        infiniteFocusElapsedSecs = 0
        infiniteFocusStartTime = System.currentTimeMillis()
        _timeLeft.value = 0

        infiniteFocusHandler = android.os.Handler(android.os.Looper.getMainLooper())
        infiniteFocusRunnable = object : Runnable {
            override fun run() {
                infiniteFocusElapsedSecs = (System.currentTimeMillis() - infiniteFocusStartTime) / 1000
                _timeLeft.postValue(infiniteFocusElapsedSecs)
                updateInfiniteFocusNotification()
                TimerWidgetProvider.updateWidgets(getApplication())
                infiniteFocusHandler?.postDelayed(this, 1000)
            }
        }
        infiniteFocusHandler?.postDelayed(infiniteFocusRunnable!!, 1000)
        updateInfiniteFocusNotification()
        TimerWidgetProvider.updateWidgets(getApplication())
    }

    private fun stopInfiniteFocus() {
        infiniteFocusRunnable?.let { infiniteFocusHandler?.removeCallbacks(it) }
        infiniteFocusHandler = null
        infiniteFocusRunnable = null
        TimerNotificationHelper.dismissTimerNotification(getApplication())

        val elapsedMinutes = (infiniteFocusElapsedSecs / 60).toInt().coerceAtLeast(1)
        val taskId = _activeTask.value?.id ?: -1

        viewModelScope.launch {
            streakManager.incrementStreak()
            sessionRepository.insert(
                Session(
                    timestamp = System.currentTimeMillis(),
                    durationMinutes = elapsedMinutes,
                    type = SessionType.WORK,
                    taskId = if (taskId >= 0) taskId else null
                )
            )
            if (taskId >= 0) {
                taskRepository.incrementPomodoro(taskId)
                _activeTask.value = taskRepository.getTaskById(taskId)
            }
            loadTodayStats()
        }

        _timeState.value = TimeState.IDLE
        _isWorkingSession.value = true
        _isLongBreak.value = false
        _timeLeft.value = (_workDurationMin.value ?: 25) * 60L
    }

    private fun updateInfiniteFocusNotification() {
        val context = getApplication<Application>()
        val secs = infiniteFocusElapsedSecs
        val mins = secs / 60
        val remSecs = secs % 60
        val formatted = String.format("%02d:%02d", mins, remSecs)
        TimerNotificationHelper.updateTimerNotification(context, "Infinite Focus", formatted, 0)
    }

    fun pauseTimer() {
        if (_timeState.value == TimeState.INFINITE_FOCUS) {
            stopInfiniteFocus()
            return
        }
        _timeState.value = TimeState.PAUSED
        countDownTimer?.cancel()
        cancelAlarm()
        val remaining = ((finishTimeMillis - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
        _timeLeft.value = remaining
        TimerNotificationHelper.dismissTimerNotification(getApplication())
        viewModelScope.launch {
            timerPreferences.saveState(
                running = false,
                finishTime = 0,
                timeLeft = remaining,
                session = _currentSession.value ?: 1,
                working = _isWorkingSession.value ?: true,
                taskId = _activeTask.value?.id ?: -1,
                longBreak = _isLongBreak.value ?: false
            )
        }
        TimerWidgetProvider.updateWidgets(getApplication())
    }

    fun resetTimer() {
        if (_timeState.value == TimeState.INFINITE_FOCUS) {
            infiniteFocusRunnable?.let { infiniteFocusHandler?.removeCallbacks(it) }
            infiniteFocusHandler = null
            infiniteFocusRunnable = null
            TimerNotificationHelper.dismissTimerNotification(getApplication())
        }
        if (_timeState.value != TimeState.IDLE) {
            viewModelScope.launch {
                streakManager.resetCurrentStreak()
            }
        }
        countDownTimer?.cancel()
        cancelAlarm()
        TimerNotificationHelper.dismissTimerNotification(getApplication())
        _timeState.value = TimeState.IDLE
        _isWorkingSession.value = true
        _isLongBreak.value = false
        _currentSession.value = 1
        _timeLeft.value = (_workDurationMin.value ?: 25) * 60L
        viewModelScope.launch {
            timerPreferences.clearState()
        }
        TimerWidgetProvider.updateWidgets(getApplication())
    }

    fun skipSession() {
        if (_timeState.value == TimeState.INFINITE_FOCUS) {
            stopInfiniteFocus()
            return
        }
        countDownTimer?.cancel()
        cancelAlarm()
        TimerNotificationHelper.dismissTimerNotification(getApplication())
        onTimerFinished()
        TimerWidgetProvider.updateWidgets(getApplication())
    }

    fun setActiveTask(task: Task?) {
        _activeTask.value = task
        viewModelScope.launch {
            timerPreferences.setActiveTaskId(task?.id ?: -1)
        }
    }

    private fun startCountDown(startSeconds: Long) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(startSeconds * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secs = (millisUntilFinished / 1000).coerceAtLeast(0)
                _timeLeft.value = secs
                updateNotification()
                if (secs % 5L == 0L) {
                    TimerWidgetProvider.updateWidgets(getApplication())
                }
            }

            override fun onFinish() {
                _timeLeft.value = 0
                onTimerFinished()
            }
        }.start()
        updateNotification()
    }

    private fun onTimerFinished() {
        _timeState.value = TimeState.IDLE
        cancelAlarm()
        TimerNotificationHelper.dismissTimerNotification(getApplication())

        val wasWorking = _isWorkingSession.value == true
        val taskId = _activeTask.value?.id ?: -1

        viewModelScope.launch {
            if (wasWorking) {
                streakManager.incrementStreak()
                val duration = _workDurationMin.value ?: 25
                sessionRepository.insert(
                    Session(
                        timestamp = System.currentTimeMillis(),
                        durationMinutes = duration,
                        type = SessionType.WORK,
                        taskId = if (taskId >= 0) taskId else null
                    )
                )
                if (taskId >= 0) {
                    taskRepository.incrementPomodoro(taskId)
                    _activeTask.value = taskRepository.getTaskById(taskId)
                }
                loadTodayStats()

                val session = _currentSession.value ?: 1
                val maxSessions = _sessionsBeforeLongBreak.value ?: 4
                if (session >= maxSessions) {
                    _currentSession.value = 1
                    _isLongBreak.value = true
                    _isWorkingSession.value = false
                    _timeLeft.value = (_longBreakDurationMin.value ?: 15) * 60L
                } else {
                    _currentSession.value = session + 1
                    _isWorkingSession.value = false
                    _isLongBreak.value = false
                    _timeLeft.value = (_breakDurationMin.value ?: 5) * 60L
                }
            } else {
                _isWorkingSession.value = true
                _isLongBreak.value = false
                _timeLeft.value = (_workDurationMin.value ?: 25) * 60L
            }
            timerPreferences.clearState()

            TimerWidgetProvider.updateWidgets(getApplication())

            if (_autoStartEnabled.value == true) {
                startTimer()
            }
        }
    }

    private fun getCurrentSessionDurationSecs(): Long {
        return if (_isWorkingSession.value == true) {
            (_workDurationMin.value ?: 25) * 60L
        } else if (_isLongBreak.value == true) {
            (_longBreakDurationMin.value ?: 15) * 60L
        } else {
            (_breakDurationMin.value ?: 5) * 60L
        }
    }

    private fun saveTimerState() {
        viewModelScope.launch {
            timerPreferences.saveState(
                running = true,
                finishTime = finishTimeMillis,
                timeLeft = _timeLeft.value ?: 0,
                session = _currentSession.value ?: 1,
                working = _isWorkingSession.value ?: true,
                taskId = _activeTask.value?.id ?: -1,
                longBreak = _isLongBreak.value ?: false
            )
        }
    }

    private fun scheduleAlarm(finishMillis: Long) {
        val context = getApplication<Application>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TIMER_FINISHED
            putExtra(AlarmReceiver.EXTRA_SESSION_TYPE, getSessionTypeLabel())
            putExtra(AlarmReceiver.EXTRA_IS_WORKING, _isWorkingSession.value == true)
            putExtra(AlarmReceiver.EXTRA_TASK_ID, _activeTask.value?.id ?: -1)
        }
        pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, finishMillis, pendingIntent!!)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, finishMillis, pendingIntent!!)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, finishMillis, pendingIntent!!)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, finishMillis, pendingIntent!!)
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, finishMillis, pendingIntent!!)
        }
    }

    private fun cancelAlarm() {
        pendingIntent?.let {
            val alarmManager = getApplication<Application>().getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(it)
            it.cancel()
        }
        pendingIntent = null
    }

    private fun getSessionTypeLabel(): String {
        return when {
            _isWorkingSession.value == true -> "Focus Time"
            _isLongBreak.value == true -> "Long Break"
            else -> "Short Break"
        }
    }

    private fun updateNotification() {
        val context = getApplication<Application>()
        val secs = _timeLeft.value ?: 0
        val mins = secs / 60
        val remSecs = secs % 60
        val formatted = String.format("%02d:%02d", mins, remSecs)
        val sessionLabel = getSessionTypeLabel()
        val progress = if (totalDurationSecs > 0) {
            (((totalDurationSecs - secs).toDouble() / totalDurationSecs) * 100).toInt()
        } else 0
        TimerNotificationHelper.updateTimerNotification(context, sessionLabel, formatted, progress)
    }

    private fun loadTodayStats() {
        viewModelScope.launch {
            val count = sessionRepository.getCompletedPomodorosToday()
            _totalSessionsToday.value = count
            NotificationHelper.updateBadgeNotification(getApplication(), count)
        }
    }

    override fun onCleared() {
        super.onCleared()
        countDownTimer?.cancel()
        infiniteFocusRunnable?.let { infiniteFocusHandler?.removeCallbacks(it) }
    }
}
