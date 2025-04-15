package com.example.myapplication

import Habit
import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*
import androidx.core.content.edit
import java.text.SimpleDateFormat


class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        if (action == "com.example.myapplication.RESET_HABITS") {
            resetHabits(context)
        } else {
            sendHabitNotification(context, intent)
        }
    }

    private fun sendHabitNotification(context: Context, intent: Intent) {
        val habitName = intent.getStringExtra("habit_name") ?: "Habit Reminder"

        val notificationIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("habit_name", habitName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val stackBuilder = TaskStackBuilder.create(context).apply {
            addParentStack(MainActivity::class.java)
            addNextIntent(notificationIntent)
        }

        val pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "habit_reminder_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(channelId, "Habit Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Notifications for habit reminders"
                }
                notificationManager.createNotificationChannel(channel)
            }
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Habit Reminder")
            .setContentText("Time to do: $habitName")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(habitName.hashCode(), notification)
        rescheduleHabitAlarm(context, habitName)
    }

    private fun resetHabits(context: Context) {
        val sharedPrefs = context.getSharedPreferences("habit_prefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPrefs.getString("habit_list", null)

        if (json != null) {
            val type = object : TypeToken<MutableList<Habit>>() {}.type
            val habits: MutableList<Habit> = gson.fromJson(json, type)

            // Reset all habits
            habits.forEach { it.isChecked = false }

            // Save updated habits
            sharedPrefs.edit {
                putString("habit_list", gson.toJson(habits))
            }

            // Notify UI to refresh
            val uiIntent = Intent("com.example.myapplication.UPDATE_UI")
            context.sendBroadcast(uiIntent)
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun rescheduleHabitAlarm(context: Context, habitName: String) {
        val sharedPrefs = context.getSharedPreferences("habit_prefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPrefs.getString("habit_list", null) ?: return

        val type = object : TypeToken<MutableList<Habit>>() {}.type
        val habits: MutableList<Habit> = gson.fromJson(json, type)
        val habit = habits.find { it.name == habitName } ?: return

        val timeParts = habit.time.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            add(Calendar.DATE, 1) // Next day
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("habit_name", habit.name)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habit.name.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }
}
