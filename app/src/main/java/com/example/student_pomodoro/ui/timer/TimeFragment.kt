package com.example.student_pomodoro.ui.timer

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.animation.doOnCancel
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.student_pomodoro.R
import com.example.student_pomodoro.databinding.FragmentTimeBinding

class TimeFragment : Fragment() {

    private var _binding: FragmentTimeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TimeViewModel by activityViewModels()
    private var pulseAnimator: ObjectAnimator? = null
    private var lastState: TimeState? = null
    private var previousTodayCount = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupButtons()
        setupLongPress()
        observeViewModel()
    }

    private fun setupButtons() {
        binding.startButton.setOnClickListener {
            animateButtonPress(it)
            when (viewModel.timeState.value) {
                TimeState.RUNNING -> viewModel.pauseTimer()
                TimeState.INFINITE_FOCUS -> viewModel.startTimer()
                else -> viewModel.startTimer()
            }
        }
        binding.resetButton.setOnClickListener {
            animateButtonPress(it)
            viewModel.resetTimer()
        }
        binding.skipButton.setOnClickListener {
            animateButtonPress(it)
            viewModel.skipSession()
        }
        binding.clearTaskButton.setOnClickListener {
            animateButtonPress(it)
            viewModel.setActiveTask(null)
        }
    }

    private fun animateButtonPress(view: View) {
        view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
        view.animate()
            .scaleX(0.96f)
            .scaleY(0.96f)
            .setDuration(80)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .start()
            }
            .start()
    }

    private fun setupLongPress() {
        binding.timerCard.setOnLongClickListener {
            if (viewModel.timeState.value == TimeState.IDLE || viewModel.timeState.value == TimeState.PAUSED) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Infinite Focus")
                    .setMessage("Start an open-ended focus session? The timer will count up until you stop it.")
                    .setPositiveButton("Start") { _, _ ->
                        viewModel.startInfiniteFocus()
                        Toast.makeText(requireContext(), "Infinite Focus started!", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            true
        }
    }

    private fun observeViewModel() {
        viewModel.timeLeft.observe(viewLifecycleOwner) { seconds ->
            val mins = seconds / 60
            val secs = seconds % 60
            binding.timeText.text = String.format("%02d:%02d", mins, secs)

            if (viewModel.timeState.value == TimeState.INFINITE_FOCUS) {
                binding.timerProgress.isIndeterminate = true
            } else {
                binding.timerProgress.isIndeterminate = false
                val totalSeconds = when {
                    viewModel.isWorkingSession.value == true -> viewModel.workDurationMin.value?.times(60) ?: 1500
                    viewModel.isLongBreak.value == true -> viewModel.longBreakDurationMin.value?.times(60) ?: 900
                    else -> viewModel.breakDurationMin.value?.times(60) ?: 300
                }
                val progress = ((seconds.toDouble() / totalSeconds) * 100).toInt()
                binding.timerProgress.progress = progress
            }
        }

        viewModel.timeState.observe(viewLifecycleOwner) { state ->
            binding.startButton.text = when (state) {
                TimeState.RUNNING -> "Pause"
                TimeState.INFINITE_FOCUS -> "Stop"
                else -> "Start"
            }

            when (state) {
                TimeState.RUNNING -> {
                    keepScreenOn(true)
                    startPulseAnimation()
                }
                TimeState.INFINITE_FOCUS -> {
                    keepScreenOn(true)
                    startPulseAnimation()
                    binding.sessionTypeText.text = "Infinite Focus"
                    binding.sessionText.text = "Flow state timer"
                }
                TimeState.PAUSED -> {
                    keepScreenOn(false)
                    stopPulseAnimation()
                }
                TimeState.IDLE -> {
                    keepScreenOn(false)
                    stopPulseAnimation()
                    if (lastState == TimeState.RUNNING) {
                        val sessionLabel = when {
                            viewModel.isWorkingSession.value == false -> "Focus session complete! Great work!"
                            else -> "Break over! Ready to focus?"
                        }
                        Toast.makeText(requireContext(), sessionLabel, Toast.LENGTH_SHORT).show()
                    }
                    if (lastState == TimeState.INFINITE_FOCUS) {
                        Toast.makeText(requireContext(), "Infinite Focus complete! 🎉", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            lastState = state
        }

        viewModel.currentSession.observe(viewLifecycleOwner) { session ->
            if (viewModel.timeState.value != TimeState.INFINITE_FOCUS) {
                val max = viewModel.sessionsBeforeLongBreak.value ?: 4
                binding.sessionText.text = "Session $session of $max"
            }
        }

        viewModel.isWorkingSession.observe(viewLifecycleOwner) { isWork ->
            if (viewModel.timeState.value == TimeState.INFINITE_FOCUS) return@observe
            val isLongBreak = viewModel.isLongBreak.value == true
            binding.sessionTypeText.text = when {
                isWork -> "Focus Time"
                isLongBreak -> "Long Break"
                else -> "Short Break"
            }
            updateProgressTint(isWork, isLongBreak)
        }

        viewModel.isLongBreak.observe(viewLifecycleOwner) { isLong ->
            if (viewModel.timeState.value == TimeState.INFINITE_FOCUS) return@observe
            val isWork = viewModel.isWorkingSession.value == true
            binding.sessionTypeText.text = when {
                isWork -> "Focus Time"
                isLong -> "Long Break"
                else -> "Short Break"
            }
            updateProgressTint(isWork, isLong)
        }

        viewModel.longestStreak.observe(viewLifecycleOwner) { streak ->
            animateCounter(binding.streakText, "Longest Streak: ", streak)
        }

        viewModel.activeTask.observe(viewLifecycleOwner) { task ->
            if (task != null) {
                binding.activeTaskText.text = task.title
                binding.taskRow.visibility = View.VISIBLE
            } else {
                binding.taskRow.visibility = View.GONE
            }
        }

        viewModel.totalSessionsToday.observe(viewLifecycleOwner) { count ->
            animateCounter(binding.todayStatsText, "Today: ", count, " pomodoros")
            updateDailyGoal(count)
            val goal = viewModel.dailyGoal.value ?: 8
            if (count > previousTodayCount && count >= goal) {
                binding.confettiView.burst()
            }
            previousTodayCount = count
        }

        viewModel.dailyGoal.observe(viewLifecycleOwner) { goal ->
            updateDailyGoal(viewModel.totalSessionsToday.value ?: 0)
        }
    }

    private fun updateDailyGoal(todayCount: Int) {
        val goal = viewModel.dailyGoal.value ?: 8
        binding.dailyGoalText.text = "Goal: $todayCount / $goal"
        val progress = if (goal > 0) ((todayCount.toFloat() / goal) * 100).toInt() else 0
        binding.dailyGoalProgress.progress = progress.coerceIn(0, 100)
        binding.dailyGoalText.visibility = View.VISIBLE
        binding.dailyGoalProgress.visibility = View.VISIBLE
    }

    private fun updateProgressTint(isWork: Boolean, isLongBreak: Boolean) {
        val colorRes = when {
            isWork -> R.color.focus_tint
            isLongBreak -> R.color.long_break_tint
            else -> R.color.break_tint
        }
        val color = ContextCompat.getColor(requireContext(), colorRes)
        binding.timerProgress.setIndicatorColor(color)
        binding.timerProgress.trackColor = color
    }

    private fun animateCounter(textView: android.widget.TextView, prefix: String, target: Int, suffix: String = "") {
        val currentText = textView.text?.toString() ?: ""
        val currentNumber = currentText.filter { it.isDigit() }.toIntOrNull() ?: 0
        if (currentNumber == target) {
            textView.text = "$prefix$target$suffix"
            return
        }
        val animator = android.animation.ValueAnimator.ofInt(currentNumber, target)
        animator.duration = 600
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            textView.text = "$prefix$value$suffix"
        }
        animator.start()
    }

    private fun keepScreenOn(enabled: Boolean) {
        if (enabled) {
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun startPulseAnimation() {
        if (pulseAnimator?.isRunning == true) return
        pulseAnimator = ObjectAnimator.ofFloat(binding.timerCard, "scaleX", 1f, 0.98f, 1f).apply {
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            doOnCancel { binding.timerCard.scaleX = 1f }
            start()
        }
        ObjectAnimator.ofFloat(binding.timerCard, "scaleY", 1f, 0.98f, 1f).apply {
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            doOnCancel { binding.timerCard.scaleY = 1f }
            start()
        }
    }

    private fun stopPulseAnimation() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        binding.timerCard.scaleX = 1f
        binding.timerCard.scaleY = 1f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopPulseAnimation()
        keepScreenOn(false)
        _binding = null
    }
}
