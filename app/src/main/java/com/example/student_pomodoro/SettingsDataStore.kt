package com.example.student_pomodoro

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val WORK_DURATION = intPreferencesKey("work_duration")
        val BREAK_DURATION = intPreferencesKey("break_duration")
        val LONG_BREAK_DURATION = intPreferencesKey("long_break_duration")
        val SESSIONS_BEFORE_LONG_BREAK = intPreferencesKey("sessions_before_long_break")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val AUTO_START_ENABLED = booleanPreferencesKey("auto_start_enabled")
        val DAILY_GOAL = intPreferencesKey("daily_goal")

        const val DEFAULT_WORK_DURATION = 25
        const val DEFAULT_BREAK_DURATION = 5
        const val DEFAULT_LONG_BREAK_DURATION = 15
        const val DEFAULT_SESSIONS_BEFORE_LONG_BREAK = 4
        const val DEFAULT_DAILY_GOAL = 8
    }

    val workDuration: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        prefs[WORK_DURATION] ?: DEFAULT_WORK_DURATION
    }

    val breakDuration: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        prefs[BREAK_DURATION] ?: DEFAULT_BREAK_DURATION
    }

    val longBreakDuration: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        prefs[LONG_BREAK_DURATION] ?: DEFAULT_LONG_BREAK_DURATION
    }

    val sessionsBeforeLongBreak: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        prefs[SESSIONS_BEFORE_LONG_BREAK] ?: DEFAULT_SESSIONS_BEFORE_LONG_BREAK
    }

    val soundEnabled: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[SOUND_ENABLED] ?: true
    }

    val vibrationEnabled: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[VIBRATION_ENABLED] ?: true
    }

    val autoStartEnabled: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[AUTO_START_ENABLED] ?: false
    }

    val dailyGoal: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        prefs[DAILY_GOAL] ?: DEFAULT_DAILY_GOAL
    }

    suspend fun setWorkDuration(minutes: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[WORK_DURATION] = minutes.coerceIn(1, 60)
        }
    }

    suspend fun setBreakDuration(minutes: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[BREAK_DURATION] = minutes.coerceIn(1, 30)
        }
    }

    suspend fun setLongBreakDuration(minutes: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[LONG_BREAK_DURATION] = minutes.coerceIn(5, 45)
        }
    }

    suspend fun setSessionsBeforeLongBreak(count: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[SESSIONS_BEFORE_LONG_BREAK] = count.coerceIn(2, 8)
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[SOUND_ENABLED] = enabled
        }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[VIBRATION_ENABLED] = enabled
        }
    }

    suspend fun setAutoStartEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[AUTO_START_ENABLED] = enabled
        }
    }

    suspend fun setDailyGoal(goal: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[DAILY_GOAL] = goal.coerceIn(1, 20)
        }
    }
}
