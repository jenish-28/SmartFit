package com.example.smartfit.model

import org.json.JSONObject

data class Workout(
    val id: Long,
    val exerciseType: String,
    val duration: Int,
    val rating: Float,
    val completed: Boolean,
    val notes: String,
    val timestamp: Long
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("exerciseType", exerciseType)
        put("duration", duration)
        put("rating", rating.toDouble())
        put("completed", completed)
        put("notes", notes)
        put("timestamp", timestamp)
    }

    companion object {
        fun fromJson(json: JSONObject): Workout = Workout(
            id = json.getLong("id"),
            exerciseType = json.getString("exerciseType"),
            duration = json.getInt("duration"),
            rating = json.getDouble("rating").toFloat(),
            completed = json.getBoolean("completed"),
            notes = json.optString("notes", ""),
            timestamp = json.getLong("timestamp")
        )
    }
}
