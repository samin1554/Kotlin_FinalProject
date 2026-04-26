package com.example.student_pomodoro

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "streaks")

class StreakManager(private val context: Context) {

    private val CURRENT_STREAK = intPreferencesKey("current_streak")
    private val LONGEST_STREAK = intPreferencesKey("longest_streak")

    val currentStreakFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[CURRENT_STREAK] ?: 0
    }

    val longestStreakFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[LONGEST_STREAK] ?: 0
    }

    suspend fun incrementStreak() {
        context.dataStore.edit { preferences ->
            val current = (preferences[CURRENT_STREAK] ?: 0) + 1
            preferences[CURRENT_STREAK] = current
            
            val longest = preferences[LONGEST_STREAK] ?: 0
            if (current > longest) {
                preferences[LONGEST_STREAK] = current
            }
        }
    }

    suspend fun resetCurrentStreak() {
        context.dataStore.edit { preferences ->
            preferences[CURRENT_STREAK] = 0
        }
    }
}
