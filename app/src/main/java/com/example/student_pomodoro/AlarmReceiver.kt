package com.example.student_pomodoro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TIMER_FINISHED = "com.example.student_pomodoro.TIMER_FINISHED"
        const val EXTRA_SESSION_TYPE = "extra_session_type"
        const val EXTRA_IS_WORKING = "extra_is_working"
        const val EXTRA_TASK_ID = "extra_task_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TIMER_FINISHED) return

        val sessionType = intent.getStringExtra(EXTRA_SESSION_TYPE) ?: "Session"
        val isWorking = intent.getBooleanExtra(EXTRA_IS_WORKING, false)
        val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)

        val settings = SettingsDataStore(context)
        val timerPrefs = TimerPreferences(context)
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            val soundEnabled = settings.soundEnabled.first()
            val vibrateEnabled = settings.vibrationEnabled.first()

            // Show notification on main thread
            CoroutineScope(Dispatchers.Main).launch {
                NotificationHelper.showCompletionNotification(context, sessionType)
            }

            if (soundEnabled) {
                try {
                    val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val ringtone = RingtoneManager.getRingtone(context, uri)
                    ringtone.play()
                } catch (_: Exception) { }
            }

            if (vibrateEnabled) {
                vibrate(context)
            }

            if (isWorking) {
                val db = AppDatabase.getDatabase(context)
                val duration = settings.workDuration.first()
                db.sessionDao().insertSession(
                    Session(
                        timestamp = System.currentTimeMillis(),
                        durationMinutes = duration,
                        type = SessionType.WORK,
                        taskId = if (taskId >= 0) taskId else null
                    )
                )
                if (taskId >= 0) {
                    db.taskDao().incrementPomodoro(taskId)
                }

                val todayCount = db.sessionDao().getCompletedPomodorosToday(
                    getStartOfDayMillis()
                )
                CoroutineScope(Dispatchers.Main).launch {
                    NotificationHelper.updateBadgeNotification(context, todayCount)
                }
            }

            timerPrefs.clearState()
            TimerWidgetProvider.updateWidgets(context)
        }
    }

    private fun getStartOfDayMillis(): Long {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun vibrate(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500), -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 500, 200, 500), -1)
                }
            }
        } catch (_: Exception) { }
    }
}
