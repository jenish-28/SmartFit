package com.example.smartfit

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Wires the bottom navigation bar shared by all four Activities. Dashboard/History/Sensors
 * are simple peer destinations; Workout is special-cased because only the Activity that
 * launches [WorkoutActivity] can register the [androidx.activity.result.ActivityResultLauncher]
 * that receives its result.
 */
fun AppCompatActivity.setupBottomNav(
    bottomNav: BottomNavigationView,
    selectedItemId: Int,
    onWorkoutSelected: () -> Unit
) {
    bottomNav.selectedItemId = selectedItemId
    bottomNav.setOnItemSelectedListener { item ->
        when (item.itemId) {
            R.id.nav_dashboard -> {
                if (this !is MainActivity) {
                    startActivity(
                        Intent(this, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    )
                }
                true
            }
            R.id.nav_workout -> {
                onWorkoutSelected()
                true
            }
            R.id.nav_history -> {
                if (this !is HistoryActivity) {
                    startActivity(
                        Intent(this, HistoryActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    )
                }
                true
            }
            R.id.nav_sensors -> {
                if (this !is SensorActivity) {
                    startActivity(
                        Intent(this, SensorActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    )
                }
                true
            }
            else -> false
        }
    }
}
