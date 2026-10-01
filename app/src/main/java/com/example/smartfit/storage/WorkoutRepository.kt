package com.example.smartfit.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.smartfit.model.Workout
import org.json.JSONArray

/**
 * Persists workouts locally as a JSON array inside SharedPreferences.
 * No database or network dependency is required for this coursework app.
 */
class WorkoutRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveWorkout(workout: Workout) {
        val workouts = getWorkouts().toMutableList()
        workouts.add(workout)
        persist(workouts)
    }

    fun getWorkouts(): List<Workout> {
        val raw = prefs.getString(KEY_WORKOUTS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { index ->
                Workout.fromJson(array.getJSONObject(index))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearWorkouts() {
        prefs.edit().remove(KEY_WORKOUTS).apply()
    }

    fun generateNextId(): Long = System.currentTimeMillis()

    private fun persist(workouts: List<Workout>) {
        val array = JSONArray()
        workouts.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_WORKOUTS, array.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "smartfit_prefs"
        private const val KEY_WORKOUTS = "workouts"
    }
}
