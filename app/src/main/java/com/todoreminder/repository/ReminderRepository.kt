package com.todoreminder.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.todoreminder.data.local.ReminderDao
import com.todoreminder.data.local.ReminderEntity
import com.todoreminder.data.model.Reminder
import com.todoreminder.data.remote.ReminderApiService
import com.todoreminder.data.remote.response.ResultState
import com.todoreminder.utils.AlarmUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Repository class for managing reminder data from both local and remote sources.
 *
 * @property context The application context used for scheduling alarms.
 * @property reminderDao DAO for accessing local reminder data.
 * @property apiService API service for fetching reminders from the server.
 */
class ReminderRepository @Inject constructor(
    private val context: Context,
    private val reminderDao: ReminderDao,
    private val apiService: ReminderApiService
) {

    /**
     * Observes all reminders stored in the local database,
     * converting them from [ReminderEntity] to [Reminder].
     */
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

    /**
     * Inserts a reminder into the local database and schedules an alarm.
     *
     * @param reminder The reminder entity to be inserted.
     * @return The generated row ID.
     */
    suspend fun insertAndReturnReminder(reminder: ReminderEntity): Long {
        val id = reminderDao.insert(reminder)
        AlarmUtils.scheduleAlarm(
            context,
            id = id.toInt(),
            title = reminder.title,
            desc = reminder.description,
            timeInMillis = reminder.dateTime,
            recurrenceInterval = reminder.recurrenceInterval
        )
        return id
    }

    /**
     * Fetches reminders from the API and maps them to [Reminder].
     *
     * @return A [ResultState] containing either a success with list of reminders or an error message.
     */
    suspend fun getApiReminders(): ResultState<List<Reminder>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = apiService.getApiReminders()
            val reminders = response.map {
                Reminder(
                    id = it.id,
                    title = it.title,
                    description = "From API - Completed: ${it.completed}",
                    dateTime = System.currentTimeMillis(),
                    isFromApi = true
                )
            }
            ResultState.Success(reminders)
        } catch (e: Exception) {
            e.printStackTrace()
            ResultState.Error("Failed to fetch API reminders: ${e.localizedMessage}")
        }
    }

    /**
     * Inserts a reminder into the database and schedules an alarm.
     *
     * @param reminder The reminder to insert.
     */
    suspend fun insertReminder(reminder: ReminderEntity) {
        val id = reminderDao.insert(reminder)
        AlarmUtils.scheduleAlarm(
            context,
            id = id.toInt(),
            title = reminder.title,
            desc = reminder.description,
            timeInMillis = reminder.dateTime,
            recurrenceInterval = reminder.recurrenceInterval
        )
    }

    /**
     * Updates an existing reminder in the database.
     *
     * @param reminder The reminder to update.
     */
    suspend fun updateReminder(reminder: ReminderEntity) {
        reminderDao.update(reminder)
    }

    /**
     * Deletes a reminder from the local database.
     *
     * @param reminder The reminder to delete.
     */
    suspend fun deleteReminder(reminder: ReminderEntity) {
        reminderDao.delete(reminder)
    }
}
