package com.example.student_pomodoro.ui.timer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.student_pomodoro.databinding.FragmentTimeBinding

class TimeFragment : Fragment() {

    private var _binding: FragmentTimeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TimeViewModel by viewModels()

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
        setupButton()
        observeViewModel()
    }

    private fun setupButton() {
        binding.startButton.setOnClickListener {
            if (viewModel.timeState.value == TimeState.RUNNING) {
                viewModel.pauseTimer()
            } else {
                viewModel.startTimer()
            }
        }
        binding.resetButton.setOnClickListener {
            viewModel.resetTimer()
        }
    }

    private fun observeViewModel() {
        viewModel.timeLeft.observe(viewLifecycleOwner) { seconds ->
            val mins = seconds / 60
            val secs = seconds % 60
            binding.timeText.text = String.format("%02d:%02d", mins, secs)
            
            // Update progress indicator
            val totalSeconds = if (viewModel.isWorkingSession.value == true) {
                viewModel.workDurationMin * 60L
            } else {
                viewModel.breakDurationMin * 60L
            }
            val progress = ((seconds.toDouble() / totalSeconds) * 100).toInt()
            binding.timerProgress.progress = progress
        }

        viewModel.timeState.observe(viewLifecycleOwner) { state ->
            binding.startButton.text = when (state) {
                TimeState.RUNNING -> "Pause"
                else -> "Start"
            }
        }

        viewModel.currentSession.observe(viewLifecycleOwner) { session ->
            binding.sessionText.text = "Session $session of 4"
        }

        viewModel.isWorkingSession.observe(viewLifecycleOwner) { isWork ->
            binding.sessionTypeText.text = if (isWork) "Focus Time" else "Break Time"
        }

        // Observe streak data
        viewModel.longestStreak.observe(viewLifecycleOwner) { streak ->
            binding.streakText.text = "Longest Streak: $streak"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}