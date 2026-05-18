package com.example.student_pomodoro.ui.stats

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.student_pomodoro.DayCount
import com.example.student_pomodoro.R
import com.example.student_pomodoro.SessionType
import com.example.student_pomodoro.SlideInItemAnimator
import com.example.student_pomodoro.TypeCount
import com.example.student_pomodoro.databinding.FragmentStatsBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StatsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionAdapter = SessionAdapter()
        binding.sessionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.sessionsRecyclerView.adapter = sessionAdapter
        binding.sessionsRecyclerView.itemAnimator = SlideInItemAnimator()

        setupBarChart()
        setupPieChart()
        setupHourlyChart()

        viewModel.todayPomodoros.observe(viewLifecycleOwner) { count ->
            binding.todayPomodorosText.text = count.toString()
        }

        viewModel.todayFocusMinutes.observe(viewLifecycleOwner) { minutes ->
            val hours = minutes / 60
            val mins = minutes % 60
            binding.todayFocusTimeText.text = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
        }

        viewModel.weekPomodoros.observe(viewLifecycleOwner) { count ->
            binding.weekPomodorosText.text = count.toString()
        }

        viewModel.currentStreak.observe(viewLifecycleOwner) { streak ->
            binding.currentStreakText.text = streak.toString()
        }

        viewModel.longestStreak.observe(viewLifecycleOwner) { streak ->
            binding.longestStreakText.text = streak.toString()
        }

        viewModel.last7Days.observe(viewLifecycleOwner) { days ->
            updateBarChart(days)
        }

        viewModel.typeDistribution.observe(viewLifecycleOwner) { types ->
            updatePieChart(types)
        }

        viewModel.hourlyProductivity.observe(viewLifecycleOwner) { hours ->
            updateHourlyChart(hours)
        }

        viewModel.totalPomodoros.observe(viewLifecycleOwner) { total ->
            binding.totalPomodorosText.text = total.toString()
        }

        viewModel.totalFocusHours.observe(viewLifecycleOwner) { hours ->
            binding.totalFocusHoursText.text = hours.toString()
        }

        viewModel.bestDay.observe(viewLifecycleOwner) { bestDay ->
            binding.bestDayText.text = if (bestDay != null) {
                val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                "${dateFormat.format(Date(bestDay.dayTimestamp))}\n(${bestDay.count})"
            } else "-"
        }

        viewModel.allSessions.observe(viewLifecycleOwner) { sessions ->
            sessionAdapter.submitList(sessions.take(20))
            if (sessions.isEmpty()) {
                binding.statsEmptyState.visibility = View.VISIBLE
                binding.sessionsRecyclerView.visibility = View.GONE
                binding.barChartCard.visibility = View.GONE
                binding.pieChartCard.visibility = View.GONE
                binding.hourlyChartCard.visibility = View.GONE
                binding.lifetimeCard.visibility = View.GONE
            } else {
                binding.statsEmptyState.visibility = View.GONE
                binding.sessionsRecyclerView.visibility = View.VISIBLE
                binding.barChartCard.visibility = View.VISIBLE
                binding.pieChartCard.visibility = View.VISIBLE
                binding.hourlyChartCard.visibility = View.VISIBLE
                binding.lifetimeCard.visibility = View.VISIBLE
                animateCardEntrance()
            }
        }
    }

    private fun animateCardEntrance() {
        val cards = listOf(
            binding.todayCard,
            binding.weekCard,
            binding.streakCard,
            binding.barChartCard,
            binding.pieChartCard,
            binding.hourlyChartCard,
            binding.lifetimeCard,
            binding.recentSessionsLabel,
            binding.sessionsRecyclerView
        )
        cards.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 40f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setStartDelay(index * 60L)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
    }

    private fun setupBarChart() {
        val chart = binding.weeklyBarChart
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.setDrawValueAboveBar(true)
        chart.setScaleEnabled(false)
        chart.setPinchZoom(false)

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(false)
        xAxis.textColor = ContextCompat.getColor(requireContext(), R.color.pomodoro_on_surface_variant)
        xAxis.textSize = 10f

        val leftAxis = chart.axisLeft
        leftAxis.setDrawGridLines(false)
        leftAxis.setDrawAxisLine(false)
        leftAxis.setDrawLabels(false)

        chart.axisRight.isEnabled = false
    }

    private fun updateBarChart(days: List<DayCount>) {
        val chart = binding.weeklyBarChart
        val goal = viewModel.dailyGoal.value ?: 8
        val primaryColor = ContextCompat.getColor(requireContext(), R.color.pomodoro_primary)
        val mutedColor = ContextCompat.getColor(requireContext(), R.color.pomodoro_tertiary)
        val outlineColor = ContextCompat.getColor(requireContext(), R.color.pomodoro_outline)

        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        val barColors = ArrayList<Int>()

        val cal = Calendar.getInstance()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

        for (i in 0..6) {
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(Calendar.DAY_OF_MONTH, -6 + i)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val dayStart = cal.timeInMillis

            val count = days.find { it.dayTimestamp == dayStart }?.count ?: 0
            entries.add(BarEntry(i.toFloat(), count.toFloat()))
            labels.add(dayFormat.format(Date(dayStart)))

            val color = when {
                count >= goal -> primaryColor
                count > 0 -> mutedColor
                else -> outlineColor
            }
            barColors.add(color)
        }

        val dataSet = BarDataSet(entries, "Pomodoros").apply {
            colors = barColors
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.pomodoro_on_surface_variant)
            valueTextSize = 12f
            setDrawValues(true)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value == 0f) "" else value.toInt().toString()
                }
            }
        }

        chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        chart.data = BarData(dataSet).apply {
            barWidth = 0.5f
        }
        chart.invalidate()
    }

    private fun setupPieChart() {
        val chart = binding.sessionPieChart
        chart.description.isEnabled = false
        chart.legend.isEnabled = true
        chart.legend.textColor = ContextCompat.getColor(requireContext(), R.color.pomodoro_on_surface_variant)
        chart.legend.textSize = 12f
        chart.setDrawEntryLabels(false)
        chart.setUsePercentValues(true)
        chart.setDrawCenterText(false)
        chart.setHoleColor(Color.TRANSPARENT)
        chart.holeRadius = 45f
        chart.transparentCircleRadius = 50f
    }

    private fun updatePieChart(types: List<TypeCount>) {
        val chart = binding.sessionPieChart

        val focusColor = ContextCompat.getColor(requireContext(), R.color.focus_tint)
        val breakColor = ContextCompat.getColor(requireContext(), R.color.break_tint)
        val longBreakColor = ContextCompat.getColor(requireContext(), R.color.long_break_tint)

        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        types.forEach { typeCount ->
            val label = when (typeCount.type) {
                SessionType.WORK -> "Focus"
                SessionType.BREAK -> "Break"
                SessionType.LONG_BREAK -> "Long Break"
            }
            entries.add(PieEntry(typeCount.count.toFloat(), label))
            colors.add(
                when (typeCount.type) {
                    SessionType.WORK -> focusColor
                    SessionType.BREAK -> breakColor
                    SessionType.LONG_BREAK -> longBreakColor
                }
            )
        }

        if (entries.isEmpty()) {
            entries.add(PieEntry(1f, "No data"))
            colors.add(ContextCompat.getColor(requireContext(), R.color.pomodoro_outline))
        }

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.pomodoro_on_surface_variant)
            valueTextSize = 12f
        }

        chart.data = PieData(dataSet)
        chart.invalidate()
    }

    private fun setupHourlyChart() {
        val chart = binding.hourlyBarChart
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.setScaleEnabled(false)
        chart.setPinchZoom(false)

        val xAxis = chart.xAxis
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(false)
        xAxis.textColor = ContextCompat.getColor(requireContext(), R.color.pomodoro_on_surface_variant)
        xAxis.textSize = 10f
        xAxis.position = XAxis.XAxisPosition.BOTTOM

        val leftAxis = chart.axisLeft
        leftAxis.setDrawGridLines(false)
        leftAxis.setDrawAxisLine(false)
        leftAxis.textColor = ContextCompat.getColor(requireContext(), R.color.pomodoro_on_surface_variant)
        leftAxis.textSize = 10f

        chart.axisRight.isEnabled = false
    }

    private fun updateHourlyChart(hours: List<com.example.student_pomodoro.HourCount>) {
        val chart = binding.hourlyBarChart
        val primaryColor = ContextCompat.getColor(requireContext(), R.color.pomodoro_primary)

        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        val hourFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        hours.filter { it.count > 0 }.forEachIndexed { index, hourCount ->
            entries.add(BarEntry(index.toFloat(), hourCount.count.toFloat()))
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hourCount.hour)
                set(Calendar.MINUTE, 0)
            }
            labels.add(hourFormat.format(cal.time))
        }

        if (entries.isEmpty()) {
            entries.add(BarEntry(0f, 0f))
            labels.add("No data")
        }

        val dataSet = BarDataSet(entries, "Pomodoros").apply {
            color = primaryColor
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.pomodoro_on_surface_variant)
            valueTextSize = 11f
            setDrawValues(true)
        }

        chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        chart.data = BarData(dataSet).apply {
            barWidth = 0.6f
        }
        chart.invalidate()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
