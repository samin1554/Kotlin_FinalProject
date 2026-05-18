package com.example.student_pomodoro

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class TimerWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_WIDGET_UPDATE = "com.example.student_pomodoro.ACTION_WIDGET_UPDATE"
        const val ACTION_WIDGET_START_PAUSE = "com.example.student_pomodoro.ACTION_WIDGET_START_PAUSE"
        const val ACTION_WIDGET_SKIP = "com.example.student_pomodoro.ACTION_WIDGET_SKIP"

        fun updateWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, TimerWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, TimerWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_UPDATE
                }
                context.sendBroadcast(intent)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_WIDGET_START_PAUSE, ACTION_WIDGET_SKIP -> {
                val activityIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(MainActivity.EXTRA_WIDGET_ACTION, intent.action)
                }
                context.startActivity(activityIntent)
            }

            ACTION_WIDGET_UPDATE -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, TimerWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                onUpdate(context, appWidgetManager, appWidgetIds)
            }

            else -> super.onReceive(context, intent)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_timer)
        val prefs = TimerPreferences(context)

        val isRunning = runBlocking { prefs.isRunning.first() }
        val timeLeft = runBlocking { prefs.timeLeftSecs.first() }
        val isWorking = runBlocking { prefs.isWorkingSession.first() }
        val isLongBreak = runBlocking { prefs.isLongBreak.first() }
        val taskId = runBlocking { prefs.activeTaskId.first() }

        val mins = timeLeft / 60
        val secs = timeLeft % 60
        views.setTextViewText(R.id.widget_time_text, String.format("%02d:%02d", mins, secs))

        val sessionLabel = when {
            isWorking -> "Focus Time"
            isLongBreak -> "Long Break"
            else -> "Short Break"
        }
        views.setTextViewText(R.id.widget_session_type, sessionLabel)

        if (taskId >= 0) {
            views.setViewVisibility(R.id.widget_task_name, View.VISIBLE)
            views.setTextViewText(R.id.widget_task_name, "Active task")
        } else {
            views.setViewVisibility(R.id.widget_task_name, View.GONE)
        }

        val buttonIcon = if (isRunning) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
        views.setImageViewResource(R.id.widget_start_pause_button, buttonIcon)

        val startPauseIntent = Intent(context, TimerWidgetProvider::class.java).apply {
            action = ACTION_WIDGET_START_PAUSE
        }
        val startPausePendingIntent = PendingIntent.getBroadcast(
            context, 0, startPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_start_pause_button, startPausePendingIntent)

        val skipIntent = Intent(context, TimerWidgetProvider::class.java).apply {
            action = ACTION_WIDGET_SKIP
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context, 1, skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_skip_button, skipPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
