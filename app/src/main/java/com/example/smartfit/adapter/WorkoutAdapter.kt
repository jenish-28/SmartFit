package com.example.smartfit.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.smartfit.R
import com.example.smartfit.databinding.ItemWorkoutBinding
import com.example.smartfit.model.Workout
import com.example.smartfit.model.WorkoutTypeIcons
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WorkoutAdapter(
    private var workouts: List<Workout> = emptyList()
) : RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder>() {

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

    fun submitList(newWorkouts: List<Workout>) {
        workouts = newWorkouts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        val binding = ItemWorkoutBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return WorkoutViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        holder.bind(workouts[position])
    }

    override fun getItemCount(): Int = workouts.size

    inner class WorkoutViewHolder(
        private val binding: ItemWorkoutBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(workout: Workout) {
            val context = binding.root.context
            val today = Date()
            val isToday = dateFormat.format(today) == dateFormat.format(Date(workout.timestamp))

            binding.iconType.setImageResource(WorkoutTypeIcons.iconFor(workout.exerciseType))
            binding.textWorkoutType.text = workout.exerciseType

            val dayLabel = if (isToday) "Today" else dateFormat.format(Date(workout.timestamp))
            binding.textDate.text = "$dayLabel · ${timeFormat.format(Date(workout.timestamp))}"

            binding.textDuration.text = context.getString(R.string.item_duration_format, workout.duration)
            binding.textRating.text = context.getString(R.string.item_rating_format, workout.rating)
            binding.ratingBarItem.rating = workout.rating

            if (workout.completed) {
                binding.textStatus.text = context.getString(R.string.status_completed)
                binding.textStatus.setTextColor(ContextCompat.getColor(context, R.color.success))
            } else {
                binding.textStatus.text = context.getString(R.string.status_incomplete)
                binding.textStatus.setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant))
            }

            if (workout.notes.isNotBlank()) {
                binding.textNotesPreview.visibility = View.VISIBLE
                binding.textNotesPreview.text = workout.notes
            } else {
                binding.textNotesPreview.visibility = View.GONE
            }
        }
    }
}
