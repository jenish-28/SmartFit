package com.example.smartfit

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartfit.adapter.WorkoutAdapter
import com.example.smartfit.databinding.ActivityHistoryBinding
import com.example.smartfit.model.Workout
import com.example.smartfit.storage.WorkoutRepository
import com.example.smartfit.storage.inMonth
import com.example.smartfit.storage.totalDurationMinutes
import com.google.android.material.chip.Chip
import java.time.YearMonth

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var repository: WorkoutRepository
    private lateinit var adapter: WorkoutAdapter

    private var allWorkouts: List<Workout> = emptyList()
    private var selectedFilterType: String? = null

    private val addWorkoutLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.let { saveWorkoutFromResult(repository, it) }
                loadWorkouts()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = WorkoutRepository(this)

        adapter = WorkoutAdapter()
        binding.recyclerWorkouts.layoutManager = LinearLayoutManager(this)
        binding.recyclerWorkouts.adapter = adapter

        setupBottomNav(binding.bottomNav, R.id.nav_history) {
            val intent = android.content.Intent(this, WorkoutActivity::class.java).apply {
                putExtra(WorkoutActivity.EXTRA_DEFAULT_WORKOUT_TYPE, WorkoutActivity.DEFAULT_WORKOUT_TYPE)
            }
            addWorkoutLauncher.launch(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadWorkouts()
    }

    private fun loadWorkouts() {
        allWorkouts = repository.getWorkouts()
        bindMonthlyAggregate()
        buildFilterChips()
        applyFilter()
    }

    private fun bindMonthlyAggregate() {
        val month = YearMonth.now()
        val monthWorkouts = allWorkouts.inMonth(month)
        val totalMins = monthWorkouts.totalDurationMinutes()

        binding.textMonthDuration.text =
            getString(R.string.hours_minutes_format, totalMins / 60, totalMins % 60)

        val completedCount = monthWorkouts.count { it.completed }
        binding.textMonthSessions.text = "$completedCount ${getString(R.string.label_sessions_unit)}"

        val monthlyGoalMinutes = MainActivity.WEEKLY_GOAL_MINUTES * 4
        binding.textMonthGoal.text = getString(R.string.month_goal_format, monthlyGoalMinutes / 60)

        val pacingPercent = if (monthlyGoalMinutes > 0) {
            ((totalMins.toFloat() / monthlyGoalMinutes) * 100).toInt()
        } else 0
        binding.progressMonthPacing.progress = pacingPercent.coerceIn(0, 100)
        binding.textMonthPacing.text = getString(R.string.pacing_format, pacingPercent)
    }

    private fun buildFilterChips() {
        binding.chipGroupFilter.removeAllViews()

        val allChip = layoutInflater.inflate(R.layout.item_filter_chip, binding.chipGroupFilter, false) as Chip
        allChip.text = getString(R.string.filter_all_format, allWorkouts.size)
        allChip.isChecked = selectedFilterType == null
        allChip.setOnClickListener {
            selectedFilterType = null
            applyFilter()
        }
        binding.chipGroupFilter.addView(allChip)

        val orderedTypes = resources.getStringArray(R.array.workout_types)
        orderedTypes.forEach { type ->
            val count = allWorkouts.count { it.exerciseType == type }
            if (count == 0) return@forEach
            val chip = layoutInflater.inflate(R.layout.item_filter_chip, binding.chipGroupFilter, false) as Chip
            chip.text = "$type ($count)"
            chip.isChecked = selectedFilterType == type
            chip.setOnClickListener {
                selectedFilterType = type
                applyFilter()
            }
            binding.chipGroupFilter.addView(chip)
        }
    }

    private fun applyFilter() {
        val filtered = allWorkouts
            .filter { selectedFilterType == null || it.exerciseType == selectedFilterType }
            .sortedByDescending { it.timestamp }

        adapter.submitList(filtered)

        val isEmpty = filtered.isEmpty()
        binding.layoutEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerWorkouts.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
}
