package com.example.student_pomodoro

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object TimerNotificationHelper {

    const val TIMER_CHANNEL_ID = "pomodoro_timer_channel"
    const val TIMER_NOTIFICATION_ID = 2001

    fun createTimerChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Timer"
            val descriptionText = "Shows timer progress and controls"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(TIMER_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
                enableVibration(false)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showTimerNotification(
        context: Context,
        sessionType: String,
        timeLeftFormatted: String,
        progressPercent: Int
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, TIMER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(sessionType)
            .setContentText(timeLeftFormatted)
            .setProgress(100, progressPercent, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(TIMER_NOTIFICATION_ID, builder.build())
            } catch (_: SecurityException) {
            }
        }
    }

    fun updateTimerNotification(
        context: Context,
        sessionType: String,
        timeLeftFormatted: String,
        progressPercent: Int
    ) {
        showTimerNotification(context, sessionType, timeLeftFormatted, progressPercent)
    }

    fun dismissTimerNotification(context: Context) {
        with(NotificationManagerCompat.from(context)) {
            cancel(TIMER_NOTIFICATION_ID)
        }
    }
}
