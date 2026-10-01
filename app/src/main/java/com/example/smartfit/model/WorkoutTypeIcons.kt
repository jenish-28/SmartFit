package com.example.smartfit.model

import androidx.annotation.DrawableRes
import com.example.smartfit.R

/** Maps a workout's exercise type to the icon used across the dashboard and history list. */
object WorkoutTypeIcons {

    @DrawableRes
    fun iconFor(exerciseType: String): Int = when (exerciseType) {
        "Running" -> R.drawable.ic_directions_run
        "Walking" -> R.drawable.ic_directions_walk
        "Cycling" -> R.drawable.ic_directions_bike
        "Strength Training" -> R.drawable.ic_fitness_center
        "Yoga" -> R.drawable.ic_self_improvement
        else -> R.drawable.ic_more_horiz
    }
}
