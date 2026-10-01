package com.example.smartfit

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.smartfit.databinding.ActivityMainBinding
import com.example.smartfit.model.Workout
import com.example.smartfit.model.WorkoutTypeIcons
import com.example.smartfit.storage.WorkoutRepository
import com.example.smartfit.storage.between
import com.example.smartfit.storage.currentStreakDays
import com.example.smartfit.storage.endOfWeek
import com.example.smartfit.storage.localDate
import com.example.smartfit.storage.startOfWeek
import com.example.smartfit.storage.totalDurationMinutes
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: WorkoutRepository

    private val addWorkoutLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.let { saveWorkoutFromResult(repository, it) }
                refreshDashboard()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = WorkoutRepository(this)

        binding.gaugeWeekly.trackColor = ContextCompat.getColor(this, R.color.surface_container_highest)
        binding.gaugeWeekly.gradientStartColor = ContextCompat.getColor(this, R.color.secondary_container)
        binding.gaugeWeekly.gradientEndColor = ContextCompat.getColor(this, R.color.primary_container)
        binding.gaugeWeekly.startAngle = 180f
        binding.gaugeWeekly.sweepAngle = 180f

        binding.btnStartWorkout.setOnClickListener { launchWorkoutActivity() }
        binding.cardRecentWorkout.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        binding.cardMotionSensors.setOnClickListener {
            startActivity(Intent(this, SensorActivity::class.java))
        }

        setupBottomNav(binding.bottomNav, R.id.nav_dashboard) { launchWorkoutActivity() }
    }

    override fun onResume() {
        super.onResume()
        refreshDashboard()
    }

    private fun launchWorkoutActivity() {
        val intent = Intent(this, WorkoutActivity::class.java).apply {
            putExtra(WorkoutActivity.EXTRA_DEFAULT_WORKOUT_TYPE, WorkoutActivity.DEFAULT_WORKOUT_TYPE)
        }
        addWorkoutLauncher.launch(intent)
    }

    private fun refreshDashboard() {
        val workouts = repository.getWorkouts()
        val today = LocalDate.now()

        bindSubtitle(workouts, today)
        bindWeeklyGoal(workouts, today)
        bindStreakStrip(workouts, today)
        bindWeeklyPerformance(workouts, today)
        bindRecentWorkout(workouts)
        bindSensorStatus()
    }

    private fun bindSubtitle(workouts: List<Workout>, today: LocalDate) {
        val streak = workouts.currentStreakDays(today)
        binding.textDashboardSubtitle.text = when {
            workouts.isEmpty() -> getString(R.string.dashboard_subtitle_empty)
            streak > 0 -> getString(R.string.dashboard_subtitle_streak_format, streak)
            else -> getString(R.string.dashboard_subtitle_empty)
        }
    }

    private fun bindWeeklyGoal(workouts: List<Workout>, today: LocalDate) {
        val weekStart = today.startOfWeek()
        val weekEnd = today.endOfWeek()
        val weeklyMinutes = workouts.between(weekStart, weekEnd).totalDurationMinutes()
        val progress = (weeklyMinutes.toFloat() / WEEKLY_GOAL_MINUTES).coerceIn(0f, 1f)

        binding.gaugeWeekly.progress = progress
        binding.textGoalValue.text = weeklyMinutes.toString()
        binding.textGoalTarget.text = getString(R.string.goal_progress_target_format, WEEKLY_GOAL_MINUTES)
        binding.textGoalPercent.text =
            getString(R.string.goal_progress_percent_format, (progress * 100).toInt())

        val minsRemaining = WEEKLY_GOAL_MINUTES - weeklyMinutes
        binding.textMinsRemaining.text = if (minsRemaining <= 0) {
            getString(R.string.goal_reached)
        } else {
            getString(R.string.mins_remaining_format, minsRemaining)
        }

        val daysLeft = ChronoUnit.DAYS.between(today, weekEnd)
        binding.textDaysLeft.text = if (daysLeft <= 1L) {
            getString(R.string.day_left_one)
        } else {
            getString(R.string.days_left_format, daysLeft.toInt())
        }
    }

    private fun bindStreakStrip(workouts: List<Workout>, today: LocalDate) {
        val streak = workouts.currentStreakDays(today)
        binding.textStreakLabel.text = if (streak > 0) {
            getString(R.string.label_current_streak_format, streak)
        } else {
            getString(R.string.label_no_streak)
        }
        binding.textStreakBadge.text = when {
            streak >= 3 -> getString(R.string.label_on_fire)
            else -> ""
        }

        val weekStart = today.startOfWeek()
        val workoutDates = workouts.mapTo(HashSet()) { it.localDate() }

        binding.streakRow.removeAllViews()
        val inflater = LayoutInflater.from(this)
        for (i in 0..6) {
            val day = weekStart.plusDays(i.toLong())
            val cell = inflater.inflate(R.layout.item_streak_day, binding.streakRow, false)
            val label = cell.findViewById<TextView>(R.id.text_day_letter)
            val dot = cell.findViewById<ImageView>(R.id.dot_day)

            label.text = day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())

            val isDone = day in workoutDates
            val isToday = day == today
            val isFuture = day.isAfter(today)

            when {
                isDone -> {
                    dot.setImageResource(R.drawable.ic_check)
                    dot.setBackgroundResource(R.drawable.shape_dot)
                    dot.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary_container)
                    dot.imageTintList = ContextCompat.getColorStateList(this, R.color.on_primary_container)
                    label.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant))
                }
                isToday -> {
                    dot.setImageResource(0)
                    dot.setBackgroundResource(R.drawable.shape_dot)
                    dot.backgroundTintList = ContextCompat.getColorStateList(this, R.color.surface_container_highest)
                    label.setTextColor(ContextCompat.getColor(this, R.color.primary_container))
                }
                isFuture -> {
                    dot.setImageResource(0)
                    dot.setBackgroundResource(R.drawable.shape_dot)
                    dot.backgroundTintList = ContextCompat.getColorStateList(this, R.color.surface_container_lowest)
                    label.setTextColor(ContextCompat.getColor(this, R.color.outline))
                }
                else -> {
                    dot.setImageResource(0)
                    dot.setBackgroundResource(R.drawable.shape_dot)
                    dot.backgroundTintList = ContextCompat.getColorStateList(this, R.color.surface_container_lowest)
                    label.setTextColor(ContextCompat.getColor(this, R.color.outline))
                }
            }

            val params = cell.layoutParams as LinearLayout.LayoutParams
            params.width = 0
            params.weight = 1f
            cell.layoutParams = params

            binding.streakRow.addView(cell)
        }
    }

    private fun bindWeeklyPerformance(workouts: List<Workout>, today: LocalDate) {
        val weekStart = today.startOfWeek()
        val weekEnd = today.endOfWeek()
        val thisWeek = workouts.between(weekStart, weekEnd)
        val lastWeek = workouts.between(weekStart.minusWeeks(1), weekEnd.minusWeeks(1))

        val sessions = thisWeek.size
        val totalMins = thisWeek.totalDurationMinutes()
        val avgMins = if (sessions > 0) totalMins / sessions else 0

        val lastSessions = lastWeek.size
        val lastTotalMins = lastWeek.totalDurationMinutes()
        val lastAvgMins = if (lastSessions > 0) lastTotalMins / lastSessions else 0

        binding.textSessionsCount.text = sessions.toString()
        binding.textTotalMinsCount.text = totalMins.toString()
        binding.textAvgMinsCount.text = avgMins.toString()

        binding.textSessionsDelta.text = formatDelta(sessions - lastSessions)
        binding.textTotalMinsDelta.text = formatDelta(totalMins - lastTotalMins, "m")
        binding.textAvgMinsDelta.text = formatDelta(avgMins - lastAvgMins, "m")
    }

    private fun formatDelta(delta: Int, unit: String = ""): String = when {
        delta > 0 -> getString(R.string.delta_vs_last_week_format, "+$delta$unit")
        delta < 0 -> getString(R.string.delta_vs_last_week_format, "$delta$unit")
        else -> getString(R.string.label_steady)
    }

    private fun bindRecentWorkout(workouts: List<Workout>) {
        val recent = workouts.maxByOrNull { it.timestamp }
        if (recent == null) {
            binding.iconRecentType.setImageResource(R.drawable.ic_directions_run)
            binding.textRecentType.text = getString(R.string.label_no_workouts_yet)
            binding.textRecentMeta.text = getString(R.string.label_no_workouts_yet_desc)
            return
        }

        binding.iconRecentType.setImageResource(WorkoutTypeIcons.iconFor(recent.exerciseType))
        binding.textRecentType.text = recent.exerciseType
        val dateLabel = relativeDateLabel(recent.localDate())
        binding.textRecentMeta.text = getString(
            R.string.item_duration_format, recent.duration
        ).let { durationText ->
            "$dateLabel · $durationText · ${getString(R.string.item_rating_format, recent.rating)}"
        }
    }

    private fun relativeDateLabel(date: LocalDate): String {
        val today = LocalDate.now()
        return when {
            date == today -> "Today"
            date == today.minusDays(1) -> "Yesterday"
            else -> date.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
        }
    }

    private fun bindSensorStatus() {
        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val available = listOf(Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE)
            .count { sensorManager.getDefaultSensor(it) != null }
        binding.textSensorsStatus.text = getString(R.string.sensors_ready_format, available, 2)
    }

    companion object {
        const val WEEKLY_GOAL_MINUTES = 150
    }
}

/** Builds and saves a [Workout] from the Intent extras returned by [WorkoutActivity]. */
fun saveWorkoutFromResult(repository: WorkoutRepository, data: Intent) {
    val workout = Workout(
        id = repository.generateNextId(),
        exerciseType = data.getStringExtra(WorkoutActivity.EXTRA_EXERCISE_TYPE)
            ?: WorkoutActivity.DEFAULT_WORKOUT_TYPE,
        duration = data.getIntExtra(WorkoutActivity.EXTRA_DURATION, 0),
        rating = data.getFloatExtra(WorkoutActivity.EXTRA_RATING, 0f),
        completed = data.getBooleanExtra(WorkoutActivity.EXTRA_COMPLETED, false),
        notes = data.getStringExtra(WorkoutActivity.EXTRA_NOTES) ?: "",
        timestamp = data.getLongExtra(WorkoutActivity.EXTRA_TIMESTAMP, System.currentTimeMillis())
    )
    repository.saveWorkout(workout)
}
