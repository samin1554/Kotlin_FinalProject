package com.example.student_pomodoro

import androidx.lifecycle.LiveData
import java.util.Calendar

class SessionRepository(private val sessionDao: SessionDao) {

    val allSessions: LiveData<List<Session>> = sessionDao.getAllSessions()

    suspend fun insert(session: Session) = sessionDao.insertSession(session)

    fun getSessionsForToday(): LiveData<List<Session>> {
        val (start, end) = getTodayRange()
        return sessionDao.getSessionsForDay(start, end)
    }

    suspend fun getCompletedPomodorosToday(): Int {
        val (start, _) = getTodayRange()
        return sessionDao.getCompletedPomodorosToday(start)
    }

    suspend fun getTotalFocusMinutesToday(): Int {
        val (start, _) = getTodayRange()
        return sessionDao.getTotalFocusMinutesToday(start) ?: 0
    }

    suspend fun getCompletedPomodorosThisWeek(): Int {
        val weekStart = getWeekStart()
        return sessionDao.getCompletedPomodorosThisWeek(weekStart)
    }

    suspend fun getPomodorosLast7Days(): List<DayCount> {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, -6)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return sessionDao.getPomodorosLast7Days(cal.timeInMillis)
    }

    suspend fun getSessionTypeDistribution(): List<TypeCount> {
        val weekStart = getWeekStart()
        return sessionDao.getSessionTypeDistribution(weekStart)
    }

    suspend fun getSessionsByHourOfDay(): List<HourCount> {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, -30)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return sessionDao.getSessionsByHourOfDay(cal.timeInMillis)
    }

    suspend fun getTotalPomodoros(): Int {
        return sessionDao.getTotalPomodoros()
    }

    suspend fun getTotalFocusMinutes(): Int {
        return sessionDao.getTotalFocusMinutes() ?: 0
    }

    suspend fun getBestDay(): BestDay? {
        return sessionDao.getBestDay()
    }

    private fun getTodayRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val end = cal.timeInMillis
        return start to end
    }

    private fun getWeekStart(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
