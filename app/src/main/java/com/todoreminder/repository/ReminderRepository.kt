package com.todoreminder.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.todoreminder.data.local.ReminderDao
import com.todoreminder.data.local.ReminderEntity
import com.todoreminder.data.model.Reminder
import com.todoreminder.data.remote.RetrofitClient
import com.todoreminder.utils.AlarmUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class ReminderRepository(private val context: Context, val reminderDao: ReminderDao) {

    val localReminders: LiveData<List<Reminder>> =
        reminderDao.getAllReminders().map { entities ->
            entities.map {
                Reminder(
                    id = it.id,
                    title = it.title,
                    description = it.description,
                    dateTime = it.dateTime,
                    isRecurring = it.isRecurring,
                    recurrenceInterval = it.recurrenceInterval,
                    isFromApi = false
                )
            }
        }

    suspend fun getApiReminders(): List<Reminder> = withContext(Dispatchers.IO) {
        try {
            RetrofitClient.apiService.getApiReminders().map {
                Reminder(
                    id = it.id,
                    title = it.title,
                    description = "From API - Completed: ${it.completed}",
                    dateTime = System.currentTimeMillis(), // No date in API, so set current
                    isFromApi = true
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun insertReminder(reminder: ReminderEntity) {
       val id= reminderDao.insert(reminder)
        AlarmUtils.scheduleAlarm(
            context,
            id = id.toInt(),
            title = reminder.title,
            desc = reminder.description,
            timeInMillis = reminder.dateTime,
            recurrenceInterval = reminder.recurrenceInterval
        )
    }

    suspend fun updateReminder(reminder: ReminderEntity) {
        reminderDao.update(reminder)
    }

    suspend fun deleteReminder(reminder: ReminderEntity) {
        reminderDao.delete(reminder)
    }
}