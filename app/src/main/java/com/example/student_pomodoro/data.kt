package com.example.student_pomodoro

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

enum class SessionType {
    WORK, BREAK, LONG_BREAK
}

data class DayCount(
    val dayTimestamp: Long,
    val count: Int
)

data class TypeCount(
    val type: SessionType,
    val count: Int
)

data class HourCount(
    val hour: Int,
    val count: Int
)

data class BestDay(
    val dayTimestamp: Long,
    val count: Int
)

class Converters {
    @TypeConverter
    fun fromSessionType(value: SessionType): String = value.name

    @TypeConverter
    fun toSessionType(value: String): SessionType = SessionType.valueOf(value)
}

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val completedPomodoros: Int = 0,
    val isCompleted: Boolean = false
)

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY id DESC")
    fun getAllTasks(): LiveData<List<Task>>

    @Insert
    suspend fun insert(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("UPDATE tasks SET completedPomodoros = completedPomodoros + 1 WHERE id = :taskId")
    suspend fun incrementPomodoro(taskId: Int)

    @Query("UPDATE tasks SET isCompleted = :completed WHERE id = :taskId")
    suspend fun setCompleted(taskId: Int, completed: Boolean)

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: Int): Task?

    @Query("UPDATE tasks SET title = :newTitle WHERE id = :taskId")
    suspend fun updateTitle(taskId: Int, newTitle: String)
}

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val durationMinutes: Int,
    val type: SessionType,
    val taskId: Int? = null
)

@Dao
interface SessionDao {
    @Insert
    suspend fun insertSession(session: Session)

    @Query("SELECT * FROM sessions ORDER BY timestamp DESC")
    fun getAllSessions(): LiveData<List<Session>>

    @Query("SELECT * FROM sessions WHERE timestamp >= :startOfDay AND timestamp < :endOfDay ORDER BY timestamp DESC")
    fun getSessionsForDay(startOfDay: Long, endOfDay: Long): LiveData<List<Session>>

    @Query("SELECT COUNT(*) FROM sessions WHERE type = 'WORK' AND timestamp >= :startOfDay")
    suspend fun getCompletedPomodorosToday(startOfDay: Long): Int

    @Query("SELECT SUM(durationMinutes) FROM sessions WHERE type = 'WORK' AND timestamp >= :startOfDay")
    suspend fun getTotalFocusMinutesToday(startOfDay: Long): Int?

    @Query("SELECT COUNT(*) FROM sessions WHERE type = 'WORK' AND timestamp >= :weekStart")
    suspend fun getCompletedPomodorosThisWeek(weekStart: Long): Int

    @Query("""
        SELECT (timestamp / 86400000) * 86400000 AS dayTimestamp, COUNT(*) AS count
        FROM sessions
        WHERE type = 'WORK' AND timestamp >= :startOf7DaysAgo
        GROUP BY timestamp / 86400000
        ORDER BY timestamp / 86400000 ASC
    """)
    suspend fun getPomodorosLast7Days(startOf7DaysAgo: Long): List<DayCount>

    @Query("""
        SELECT type, COUNT(*) AS count
        FROM sessions
        WHERE timestamp >= :weekStart
        GROUP BY type
    """)
    suspend fun getSessionTypeDistribution(weekStart: Long): List<TypeCount>

    @Query("""
        SELECT (timestamp / 3600000) % 24 AS hour, COUNT(*) AS count
        FROM sessions
        WHERE type = 'WORK' AND timestamp >= :startTime
        GROUP BY hour
        ORDER BY hour ASC
    """)
    suspend fun getSessionsByHourOfDay(startTime: Long): List<HourCount>

    @Query("SELECT COUNT(*) FROM sessions WHERE type = 'WORK'")
    suspend fun getTotalPomodoros(): Int

    @Query("SELECT SUM(durationMinutes) FROM sessions WHERE type = 'WORK'")
    suspend fun getTotalFocusMinutes(): Int?

    @Query("""
        SELECT (timestamp / 86400000) * 86400000 AS dayTimestamp, COUNT(*) AS count
        FROM sessions
        WHERE type = 'WORK'
        GROUP BY timestamp / 86400000
        ORDER BY count DESC
        LIMIT 1
    """)
    suspend fun getBestDay(): BestDay?

    @Query("DELETE FROM sessions")
    suspend fun clearAll()
}

@Database(entities = [Task::class, Session::class], version = 2)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS sessions (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "timestamp INTEGER NOT NULL, " +
                            "durationMinutes INTEGER NOT NULL, " +
                            "type TEXT NOT NULL, " +
                            "taskId INTEGER)"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "task_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
