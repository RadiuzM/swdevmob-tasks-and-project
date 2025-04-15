package com.example.myapplication

import Habit
import android.content.Context
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class StatsActivity : AppCompatActivity() {
    private lateinit var habits: List<Habit>
    private val gson = Gson()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)
        val habitsCompletedText = findViewById<TextView>(R.id.textHabitsCompleted)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val progressText = findViewById<TextView>(R.id.textProgress)
        val sharedPrefs = getSharedPreferences("habit_prefs", Context.MODE_PRIVATE)
        val habitsJson = sharedPrefs.getString("habit_list", null)
        val type = object : TypeToken<MutableList<Habit>>() {}.type
        habits = if (habitsJson != null) gson.fromJson(habitsJson, type) else listOf()

        val completedCount = habits.count { it.isChecked }
        val totalCount = habits.size

        habitsCompletedText.text = "Habits Completed Today: $completedCount"

        val percent = if (totalCount == 0) 0 else (completedCount * 100 / totalCount)
        progressBar.progress = percent
        progressText.text = "Progress: $percent%"
    }
}
