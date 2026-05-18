package com.example.student_pomodoro

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.timerDataStore: DataStore<Preferences> by preferencesDataStore(name = "timer_state")

class TimerPreferences(private val context: Context) {

    companion object {
        val IS_RUNNING = booleanPreferencesKey("is_running")
        val FINISH_TIME_MILLIS = longPreferencesKey("finish_time_millis")
        val TIME_LEFT_SECS = longPreferencesKey("time_left_secs")
        val CURRENT_SESSION = intPreferencesKey("current_session")
        val IS_WORKING_SESSION = booleanPreferencesKey("is_working_session")
        val ACTIVE_TASK_ID = intPreferencesKey("active_task_id")
        val IS_LONG_BREAK = booleanPreferencesKey("is_long_break")
    }

    val isRunning: Flow<Boolean> = context.timerDataStore.data.map { it[IS_RUNNING] ?: false }
    val finishTimeMillis: Flow<Long> = context.timerDataStore.data.map { it[FINISH_TIME_MILLIS] ?: 0L }
    val timeLeftSecs: Flow<Long> = context.timerDataStore.data.map { it[TIME_LEFT_SECS] ?: 0L }
    val currentSession: Flow<Int> = context.timerDataStore.data.map { it[CURRENT_SESSION] ?: 1 }
    val isWorkingSession: Flow<Boolean> = context.timerDataStore.data.map { it[IS_WORKING_SESSION] ?: true }
    val activeTaskId: Flow<Int> = context.timerDataStore.data.map { it[ACTIVE_TASK_ID] ?: -1 }
    val isLongBreak: Flow<Boolean> = context.timerDataStore.data.map { it[IS_LONG_BREAK] ?: false }

    suspend fun saveState(
        running: Boolean,
        finishTime: Long,
        timeLeft: Long,
        session: Int,
        working: Boolean,
        taskId: Int,
        longBreak: Boolean
    ) {
        context.timerDataStore.edit { prefs ->
            prefs[IS_RUNNING] = running
            prefs[FINISH_TIME_MILLIS] = finishTime
            prefs[TIME_LEFT_SECS] = timeLeft
            prefs[CURRENT_SESSION] = session
            prefs[IS_WORKING_SESSION] = working
            prefs[ACTIVE_TASK_ID] = taskId
            prefs[IS_LONG_BREAK] = longBreak
        }
    }

    suspend fun clearState() {
        context.timerDataStore.edit { prefs ->
            prefs[IS_RUNNING] = false
            prefs[FINISH_TIME_MILLIS] = 0L
            prefs[TIME_LEFT_SECS] = 0L
        }
    }

    suspend fun setActiveTaskId(taskId: Int) {
        context.timerDataStore.edit { prefs ->
            prefs[ACTIVE_TASK_ID] = taskId
        }
    }
}
