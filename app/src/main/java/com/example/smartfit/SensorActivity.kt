package com.example.smartfit

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.smartfit.databinding.ActivitySensorBinding
import com.example.smartfit.storage.WorkoutRepository
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

class SensorActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivitySensorBinding
    private lateinit var sensorManager: SensorManager
    private lateinit var repository: WorkoutRepository

    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null

    private var isStreaming = true
    private var baseIdleG: Float? = null
    private var peakSpikeG: Float? = null

    private var lastAccelTimestampNs: Long = 0L
    private var smoothedHz: Double = 0.0

    private val addWorkoutLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.let { saveWorkoutFromResult(repository, it) }
                Toast.makeText(this, R.string.toast_workout_saved, Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySensorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = WorkoutRepository(this)

        binding.gaugeMagnitude.trackColor = ContextCompat.getColor(this, R.color.surface_container_highest)
        binding.gaugeMagnitude.gradientStartColor = ContextCompat.getColor(this, R.color.secondary_container)
        binding.gaugeMagnitude.gradientEndColor = ContextCompat.getColor(this, R.color.primary_container)
        binding.gaugeMagnitude.startAngle = 150f
        binding.gaugeMagnitude.sweepAngle = 240f

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        bindAvailability()
        binding.textStreamState.text = getString(R.string.label_live)
        binding.textSensorPrecision.text = getString(R.string.label_sensors_desc)
        binding.textMagnitudeValue.text = "0.00"
        binding.textBaseIdle.text = "—"
        binding.textPeakSpike.text = "—"
        binding.textMotionStatus.text = getString(R.string.status_idle)

        binding.btnPauseStream.setOnClickListener { toggleStreaming() }
        binding.btnRecalibrate.setOnClickListener { recalibrate() }

        setupBottomNav(binding.bottomNav, R.id.nav_sensors) {
            val intent = Intent(this, WorkoutActivity::class.java).apply {
                putExtra(WorkoutActivity.EXTRA_DEFAULT_WORKOUT_TYPE, WorkoutActivity.DEFAULT_WORKOUT_TYPE)
            }
            addWorkoutLauncher.launch(intent)
        }
    }

    private fun bindAvailability() {
        val bothAvailable = accelerometer != null && gyroscope != null
        binding.textSensorManagerStatus.text = getString(
            if (bothAvailable) R.string.label_sensor_manager_active else R.string.label_sensor_manager_unavailable
        )
        binding.dotSensorStatus.backgroundTintList = ContextCompat.getColorStateList(
            this, if (bothAvailable) R.color.primary_container else R.color.error
        )

        if (accelerometer == null) {
            binding.groupAccelAxes.visibility = View.GONE
            binding.textAccelUnavailable.visibility = View.VISIBLE
        }
        if (gyroscope == null) {
            binding.groupGyroAxes.visibility = View.GONE
            binding.textGyroUnavailable.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        if (isStreaming) registerListeners()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    private fun registerListeners() {
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        gyroscope?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    private fun toggleStreaming() {
        isStreaming = !isStreaming
        if (isStreaming) {
            registerListeners()
            binding.btnPauseStream.text = getString(R.string.btn_pause_stream)
            binding.btnPauseStream.setIconResource(R.drawable.ic_pause_circle)
            binding.textStreamState.text = getString(R.string.label_live)
        } else {
            sensorManager.unregisterListener(this)
            binding.btnPauseStream.text = getString(R.string.btn_resume_stream)
            binding.btnPauseStream.setIconResource(R.drawable.ic_play_circle)
            binding.textStreamState.text = getString(R.string.label_paused)
        }
        binding.dotSensorStatus.backgroundTintList = ContextCompat.getColorStateList(
            this, if (isStreaming) R.color.primary_container else R.color.outline
        )
    }

    private fun recalibrate() {
        baseIdleG = null
        peakSpikeG = null
        binding.movementGraphView.clear()
        binding.textBaseIdle.text = "—"
        binding.textPeakSpike.text = "—"
        Toast.makeText(this, R.string.btn_recalibrate, Toast.LENGTH_SHORT).show()
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> handleAccelerometer(event)
            Sensor.TYPE_GYROSCOPE -> handleGyroscope(event.values)
        }
    }

    private fun handleAccelerometer(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        if (lastAccelTimestampNs != 0L) {
            val deltaNs = event.timestamp - lastAccelTimestampNs
            if (deltaNs > 0) {
                val instantHz = 1_000_000_000.0 / deltaNs
                smoothedHz = if (smoothedHz == 0.0) instantHz else (smoothedHz * 0.9 + instantHz * 0.1)
            }
        }
        lastAccelTimestampNs = event.timestamp
        if (smoothedHz > 0) {
            binding.textSensorPrecision.text =
                getString(R.string.label_hz_live_format, smoothedHz.roundToInt())
        }

        binding.textAccelX.text = getString(R.string.axis_value_format, x) + " " + getString(R.string.unit_m_s2)
        binding.textAccelY.text = getString(R.string.axis_value_format, y) + " " + getString(R.string.unit_m_s2)
        binding.textAccelZ.text = getString(R.string.axis_value_format, z) + " " + getString(R.string.unit_m_s2)
        binding.progressAccelX.progress = axisToProgress(x, MAX_ACCEL_MS2)
        binding.progressAccelY.progress = axisToProgress(y, MAX_ACCEL_MS2)
        binding.progressAccelZ.progress = axisToProgress(z, MAX_ACCEL_MS2)

        val magnitudeMs2 = sqrt(x * x + y * y + z * z)
        val magnitudeG = magnitudeMs2 / SensorManager.GRAVITY_EARTH

        baseIdleG = baseIdleG?.let { minOf(it, magnitudeG) } ?: magnitudeG
        peakSpikeG = peakSpikeG?.let { maxOf(it, magnitudeG) } ?: magnitudeG

        binding.gaugeMagnitude.progress = (magnitudeG / GAUGE_MAX_G).coerceIn(0f, 1f)
        binding.textMagnitudeValue.text = String.format("%.2f", magnitudeG)
        binding.textBaseIdle.text = String.format("%.2f g", baseIdleG)
        binding.textPeakSpike.text = String.format("%.2f g", peakSpikeG)

        val overIdle = (magnitudeG - (baseIdleG ?: magnitudeG)).coerceAtLeast(0f)
        binding.textOverIdle.text = getString(R.string.over_idle_format, overIdle)

        val restDelta = magnitudeG - 1f
        binding.textMotionStatus.text = when {
            restDelta > 0.5f -> getString(R.string.status_vigorous_motion)
            restDelta > 0.15f -> getString(R.string.status_light_motion)
            else -> getString(R.string.status_idle)
        }

        binding.movementGraphView.addValue(magnitudeG)
    }

    private fun handleGyroscope(values: FloatArray) {
        binding.textGyroX.text = String.format("%.2f", values[0])
        binding.textGyroY.text = String.format("%.2f", values[1])
        binding.textGyroZ.text = String.format("%.2f", values[2])
        binding.progressGyroX.progress = axisToProgress(values[0], MAX_GYRO_RAD_S)
        binding.progressGyroY.progress = axisToProgress(values[1], MAX_GYRO_RAD_S)
        binding.progressGyroZ.progress = axisToProgress(values[2], MAX_GYRO_RAD_S)
    }

    private fun axisToProgress(value: Float, max: Float): Int =
        ((abs(value) / max) * 100).roundToInt().coerceIn(0, 100)

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No action needed for this coursework app.
    }

    companion object {
        private const val GAUGE_MAX_G = 5f
        private const val MAX_ACCEL_MS2 = 20f
        private const val MAX_GYRO_RAD_S = 6f
    }
}
