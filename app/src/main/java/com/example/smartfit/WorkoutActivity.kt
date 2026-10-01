package com.example.smartfit

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.smartfit.databinding.ActivityWorkoutBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip

class WorkoutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkoutBinding

    private var duration = 45
    private var intensityLevel = 4

    private val intensityHeadlines = intArrayOf(
        R.string.intensity_1, R.string.intensity_2, R.string.intensity_3,
        R.string.intensity_4, R.string.intensity_5
    )
    private val intensityDescriptions = intArrayOf(
        R.string.intensity_desc_1, R.string.intensity_desc_2, R.string.intensity_desc_3,
        R.string.intensity_desc_4, R.string.intensity_desc_5
    )
    private val segmentLabels = arrayOf("1 · LOW", "2 · MOD", "3 · VIG", "4 · HIGH", "5 · MAX")
    private lateinit var segmentBars: List<View>
    private lateinit var segmentLabelViews: List<TextView>

    private lateinit var presetButtons: List<Pair<MaterialButton, Int>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupActivityTypeChips()
        setupDurationStepper()
        setupIntensitySegments()
        setupCompletedSwitch()
        setupNotesCharCounter()

        binding.btnSaveWorkout.setOnClickListener { onSaveClicked() }
        binding.btnCancel.setOnClickListener { finish() }

        setupBottomNav(binding.bottomNav, R.id.nav_workout) { /* already here */ }
    }

    private fun setupActivityTypeChips() {
        val defaultType = intent.getStringExtra(EXTRA_DEFAULT_WORKOUT_TYPE)
        chipForType(defaultType)?.isChecked = true
        updateSelectedTypeLabel()

        binding.chipGroupType.setOnCheckedStateChangeListener { _, _ -> updateSelectedTypeLabel() }
    }

    private fun chipForType(type: String?): Chip? = when (type) {
        "Running" -> binding.chipRunning
        "Walking" -> binding.chipWalking
        "Cycling" -> binding.chipCycling
        "Strength Training" -> binding.chipStrength
        "Yoga" -> binding.chipYoga
        "Other" -> binding.chipOther
        else -> null
    }

    private fun updateSelectedTypeLabel() {
        val type = selectedWorkoutType()
        binding.textSelectedType.text = getString(R.string.selected_format, type)
    }

    private fun selectedWorkoutType(): String {
        val checkedId = binding.chipGroupType.checkedChipId
        return findViewById<Chip>(checkedId)?.text?.toString() ?: WorkoutActivity.DEFAULT_WORKOUT_TYPE
    }

    private fun setupDurationStepper() {
        presetButtons = listOf(
            binding.preset15 to 15, binding.preset30 to 30, binding.preset45 to 45,
            binding.preset60 to 60, binding.preset90 to 90
        )
        presetButtons.forEach { (button, value) ->
            button.setOnClickListener {
                duration = value
                updateDurationUi()
            }
        }
        binding.btnDec.setOnClickListener {
            duration = (duration - 5).coerceAtLeast(5)
            updateDurationUi()
        }
        binding.btnInc.setOnClickListener {
            duration = (duration + 5).coerceAtMost(240)
            updateDurationUi()
        }
        updateDurationUi()
    }

    private fun updateDurationUi() {
        binding.textDurationValue.text = getString(R.string.value_minutes_compact, duration)
        binding.textDurationTarget.text = getString(R.string.duration_target_format, duration)
        binding.progressDuration.progress = duration

        val selectedColor = ContextCompat.getColor(this, R.color.on_primary_container)
        val unselectedColor = ContextCompat.getColor(this, R.color.on_surface)
        val selectedBg = ContextCompat.getColorStateList(this, R.color.primary_container)
        val unselectedBg = ContextCompat.getColorStateList(this, R.color.surface_container_high)

        presetButtons.forEach { (button, value) ->
            val isSelected = value == duration
            button.setTextColor(if (isSelected) selectedColor else unselectedColor)
            button.backgroundTintList = if (isSelected) selectedBg else unselectedBg
        }
    }

    private fun setupIntensitySegments() {
        val inflater = LayoutInflater.from(this)
        val bars = mutableListOf<View>()
        val labels = mutableListOf<TextView>()

        for (i in 0..4) {
            val cell = inflater.inflate(R.layout.item_intensity_segment, binding.segmentRow, false)
            val bar = cell.findViewById<View>(R.id.bar_segment)
            val label = cell.findViewById<TextView>(R.id.text_segment_label)
            label.text = segmentLabels[i]

            val params = cell.layoutParams as LinearLayout.LayoutParams
            params.width = 0
            params.weight = 1f
            params.marginEnd = if (i < 4) resources.getDimensionPixelSize(R.dimen.space_2xs) else 0
            cell.layoutParams = params

            val level = i + 1
            cell.setOnClickListener {
                intensityLevel = level
                updateIntensityUi()
            }

            binding.segmentRow.addView(cell)
            bars.add(bar)
            labels.add(label)
        }
        segmentBars = bars
        segmentLabelViews = labels
        updateIntensityUi()
    }

    private fun updateIntensityUi() {
        binding.textIntensityLevel.text =
            getString(R.string.level_format, intensityLevel) + ": " + getString(intensityHeadlines[intensityLevel - 1])
        binding.textIntensityDesc.text = getString(intensityDescriptions[intensityLevel - 1])

        val activeColor = ContextCompat.getColorStateList(this, R.color.primary_container)
        val inactiveColor = ContextCompat.getColorStateList(this, R.color.surface_container_highest)
        val activeText = ContextCompat.getColor(this, R.color.primary_container)
        val inactiveText = ContextCompat.getColor(this, R.color.on_surface_variant)

        segmentBars.forEachIndexed { index, bar ->
            bar.backgroundTintList = if (index < intensityLevel) activeColor else inactiveColor
        }
        segmentLabelViews.forEachIndexed { index, label ->
            label.setTextColor(if (index == intensityLevel - 1) activeText else inactiveText)
        }
    }

    private fun setupCompletedSwitch() {
        binding.textCompletedDesc.text =
            getString(R.string.label_mark_completed_desc_format, MainActivity.WEEKLY_GOAL_MINUTES)
    }

    private fun setupNotesCharCounter() {
        val maxChars = 300
        binding.textCharCount.text = getString(R.string.chars_count_format, 0, maxChars)
        binding.editNotes.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.textCharCount.text =
                    getString(R.string.chars_count_format, s?.length ?: 0, maxChars)
            }
        })
    }

    private fun onSaveClicked() {
        val resultIntent = Intent().apply {
            putExtra(EXTRA_EXERCISE_TYPE, selectedWorkoutType())
            putExtra(EXTRA_DURATION, duration)
            putExtra(EXTRA_RATING, intensityLevel.toFloat())
            putExtra(EXTRA_COMPLETED, binding.switchCompleted.isChecked)
            putExtra(EXTRA_NOTES, binding.editNotes.text?.toString()?.trim().orEmpty())
            putExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    companion object {
        const val EXTRA_DEFAULT_WORKOUT_TYPE = "com.example.smartfit.EXTRA_DEFAULT_WORKOUT_TYPE"
        const val EXTRA_EXERCISE_TYPE = "com.example.smartfit.EXTRA_EXERCISE_TYPE"
        const val EXTRA_DURATION = "com.example.smartfit.EXTRA_DURATION"
        const val EXTRA_RATING = "com.example.smartfit.EXTRA_RATING"
        const val EXTRA_COMPLETED = "com.example.smartfit.EXTRA_COMPLETED"
        const val EXTRA_NOTES = "com.example.smartfit.EXTRA_NOTES"
        const val EXTRA_TIMESTAMP = "com.example.smartfit.EXTRA_TIMESTAMP"

        const val DEFAULT_WORKOUT_TYPE = "Running"
    }
}
