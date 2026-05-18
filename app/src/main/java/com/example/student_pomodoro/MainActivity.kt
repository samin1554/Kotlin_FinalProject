package com.example.student_pomodoro

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.student_pomodoro.databinding.ActivityMainBinding
import com.example.student_pomodoro.ui.timer.TimeViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    companion object {
        const val EXTRA_WIDGET_ACTION = "widget_action"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationHelper.createNotificationChannel(this)
            TimerNotificationHelper.createTimerChannel(this)
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("AUTO_START_TIMER", false) == true) {
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navHostFragment.navController.navigate(R.id.timeFragment)
            return
        }

        val widgetAction = intent?.getStringExtra(EXTRA_WIDGET_ACTION)
        if (widgetAction != null) {
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navHostFragment.navController.navigate(R.id.timeFragment)

            val viewModel = ViewModelProvider(this)[TimeViewModel::class.java]
            when (widgetAction) {
                TimerWidgetProvider.ACTION_WIDGET_START_PAUSE -> {
                    when (viewModel.timeState.value) {
                        com.example.student_pomodoro.ui.timer.TimeState.RUNNING -> viewModel.pauseTimer()
                        com.example.student_pomodoro.ui.timer.TimeState.INFINITE_FOCUS -> viewModel.pauseTimer()
                        else -> viewModel.startTimer()
                    }
                }
                TimerWidgetProvider.ACTION_WIDGET_SKIP -> {
                    viewModel.skipSession()
                }
            }
        }
    }
}
