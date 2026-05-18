package com.example.student_pomodoro.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.student_pomodoro.SettingsDataStore
import com.example.student_pomodoro.databinding.FragmentSettingsBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var settingsDataStore: SettingsDataStore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settingsDataStore = SettingsDataStore(requireContext())

        lifecycleScope.launch {
            val workDuration = settingsDataStore.workDuration.first()
            val breakDuration = settingsDataStore.breakDuration.first()
            val longBreakDuration = settingsDataStore.longBreakDuration.first()
            val sessionsBeforeLongBreak = settingsDataStore.sessionsBeforeLongBreak.first()
            val soundEnabled = settingsDataStore.soundEnabled.first()
            val vibrationEnabled = settingsDataStore.vibrationEnabled.first()
            val autoStart = settingsDataStore.autoStartEnabled.first()
            val dailyGoal = settingsDataStore.dailyGoal.first()

            binding.workSeekBar.value = workDuration.toFloat()
            binding.workValueText.text = "$workDuration min"

            binding.breakSeekBar.value = breakDuration.toFloat()
            binding.breakValueText.text = "$breakDuration min"

            binding.longBreakSeekBar.value = longBreakDuration.toFloat()
            binding.longBreakValueText.text = "$longBreakDuration min"

            binding.sessionsSeekBar.value = sessionsBeforeLongBreak.toFloat()
            binding.sessionsValueText.text = "$sessionsBeforeLongBreak"

            binding.soundSwitch.isChecked = soundEnabled
            binding.vibrationSwitch.isChecked = vibrationEnabled
            binding.autoStartSwitch.isChecked = autoStart
            binding.dailyGoalSeekBar.value = dailyGoal.toFloat()
            binding.dailyGoalValueText.text = "$dailyGoal"
        }

        binding.workSeekBar.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val min = value.toInt().coerceIn(1, 60)
                binding.workValueText.text = "$min min"
                lifecycleScope.launch { settingsDataStore.setWorkDuration(min) }
            }
        }

        binding.breakSeekBar.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val min = value.toInt().coerceIn(1, 30)
                binding.breakValueText.text = "$min min"
                lifecycleScope.launch { settingsDataStore.setBreakDuration(min) }
            }
        }

        binding.longBreakSeekBar.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val min = value.toInt().coerceIn(5, 45)
                binding.longBreakValueText.text = "$min min"
                lifecycleScope.launch { settingsDataStore.setLongBreakDuration(min) }
            }
        }

        binding.sessionsSeekBar.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val count = value.toInt().coerceIn(2, 8)
                binding.sessionsValueText.text = "$count"
                lifecycleScope.launch { settingsDataStore.setSessionsBeforeLongBreak(count) }
            }
        }

        binding.soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch { settingsDataStore.setSoundEnabled(isChecked) }
        }

        binding.vibrationSwitch.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch { settingsDataStore.setVibrationEnabled(isChecked) }
        }

        binding.autoStartSwitch.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch { settingsDataStore.setAutoStartEnabled(isChecked) }
        }

        binding.dailyGoalSeekBar.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val goal = value.toInt().coerceIn(1, 20)
                binding.dailyGoalValueText.text = "$goal"
                lifecycleScope.launch { settingsDataStore.setDailyGoal(goal) }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
