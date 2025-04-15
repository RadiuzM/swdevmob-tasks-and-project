package com.example.myapplication

import Habit
import HabitAdapter
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*
import androidx.core.graphics.drawable.toDrawable
import androidx.core.content.edit

class MainActivity : AppCompatActivity() {

    private lateinit var habitAdapter: HabitAdapter
    private val habits = mutableListOf<Habit>()
    private val PREFS_NAME = "habit_prefs"
    private val HABITS_KEY = "habit_list"
    private val gson = Gson()

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        findViewById<Button>(R.id.testResetButton).setOnClickListener {
            val intent = Intent(this, AlarmReceiver::class.java).apply {
                action = "com.example.myapplication.RESET_HABITS"
            }
            sendBroadcast(intent)
            Toast.makeText(this, "Day changed", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.button_open_stats).setOnClickListener {
            startActivity(Intent(this, StatsActivity::class.java))
        }

        loadHabitsFromPrefs()
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val fab = findViewById<FloatingActionButton>(R.id.fab)

        habitAdapter = HabitAdapter(
            habits,
            onItemClick = { habit -> editHabit(habit) },
            onItemLongClick = { habit -> deleteHabit(habit) },
            saveHabitsToPrefs = { saveHabitsToPrefs() }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = habitAdapter
        habitAdapter.notifyDataSetChanged()

        val divider = DividerItemDecoration(this, LinearLayoutManager.VERTICAL)
        ContextCompat.getDrawable(this, R.drawable.custom_divider)?.let {
            divider.setDrawable(it)
        }
        recyclerView.addItemDecoration(divider)
        fab.setOnClickListener {
            showAddHabitDialog()
        }

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            val deleteIcon = ContextCompat.getDrawable(this@MainActivity, R.drawable.baseline_delete_forever_24)!!
            val background = Color.RED.toDrawable()
            val iconSize = 94

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                deleteHabit(habits[position])
            }
            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val iconMargin = (itemView.height - iconSize) / 2

                background.setBounds(
                    itemView.right + dX.toInt(),
                    itemView.top,
                    itemView.right,
                    itemView.bottom
                )
                background.draw(c)

                val iconTop = itemView.top + iconMargin
                val iconBottom = iconTop + iconSize
                val iconRight = itemView.right - iconMargin
                val iconLeft = iconRight - iconSize
                deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                deleteIcon.draw(c)
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)

        // Schedule reset at midnight and reminders for existing habits
        scheduleMidnightReset()
        habits.forEach { scheduleHabitReminder(it) }
    }

    @SuppressLint("DefaultLocale")
    private fun showAddHabitDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_habit, null)
        val habitInput = dialogView.findViewById<EditText>(R.id.editHabitName)
        val timeInput = dialogView.findViewById<EditText>(R.id.editHabitTime)

        timeInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            TimePickerDialog(this, { _: TimePicker, h: Int, m: Int ->
                val time = String.format("%02d:%02d", h, m)
                timeInput.setText(time)
            }, hour, minute, true).show()
        }

        AlertDialog.Builder(this)
            .setTitle("Add Habit")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = habitInput.text.toString()
                val time = timeInput.text.toString()
                if (name.isNotBlank() && time.isNotBlank()) {
                    val newHabit = Habit(name, time)
                    habits.add(newHabit)
                    habitAdapter.notifyItemInserted(habits.size - 1)
                    saveHabitsToPrefs()
                    scheduleHabitReminder(newHabit)
                } else {
                    Toast.makeText(this, "Please enter habit and time", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    @SuppressLint("DefaultLocale", "NotifyDataSetChanged")
    private fun editHabit(habit: Habit) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_habit, null)
        val habitInput = dialogView.findViewById<EditText>(R.id.editHabitName)
        val timeInput = dialogView.findViewById<EditText>(R.id.editHabitTime)

        habitInput.setText(habit.name)
        timeInput.setText(habit.time)

        timeInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            TimePickerDialog(this, { _: TimePicker, h: Int, m: Int ->
                val time = String.format("%02d:%02d", h, m)
                timeInput.setText(time)
            }, hour, minute, true).show()
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Habit")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newName = habitInput.text.toString()
                val newTime = timeInput.text.toString()
                if (newName.isNotBlank() && newTime.isNotBlank()) {
                    habit.name = newName
                    habit.time = newTime
                    habitAdapter.notifyDataSetChanged()
                    saveHabitsToPrefs()
                    scheduleHabitReminder(habit)
                } else {
                    Toast.makeText(this, "Please enter a valid habit and time", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteHabit(habit: Habit) {
        val index = habits.indexOf(habit)
        if (index != -1) {
            habits.removeAt(index)
            habitAdapter.notifyItemRemoved(index)
            saveHabitsToPrefs()
        }
    }
    private fun saveHabitsToPrefs() {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(habits)
        sharedPrefs.edit { putString(HABITS_KEY, json) }
    }
    private fun loadHabitsFromPrefs() {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = sharedPrefs.getString(HABITS_KEY, null)
        if (json != null) {
            val type = object : TypeToken<MutableList<Habit>>() {}.type
            val savedHabits: MutableList<Habit> = gson.fromJson(json, type)
            habits.clear()
            habits.addAll(savedHabits)
        }
    }
    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleHabitReminder(habit: Habit) {
        val timeParts = habit.time.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DATE, 1) // schedule for next day if time passed
        }
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("habit_name", habit.name)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            habit.name.hashCode(), // unique per habit
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }
    private fun scheduleMidnightReset() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            add(Calendar.DATE, 1)
        }
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            action = "com.example.myapplication.RESET_HABITS"  // Add the reset action
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this, 9999, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
}
