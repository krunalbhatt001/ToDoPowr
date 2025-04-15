package com.todoreminder.ui.viewmodel

import androidx.lifecycle.*
import com.todoreminder.data.local.ReminderEntity
import com.todoreminder.data.model.Reminder
import com.todoreminder.data.remote.response.ResultState
import com.todoreminder.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel that manages the data for the Reminder UI.
 *
 * It handles combining local and API reminders, transforming them,
 * and exposes the final result in a unified LiveData stream.
 *
 * @property repository The data source for both local and remote reminders.
 */
@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val repository: ReminderRepository
) : ViewModel() {

    /** LiveData of reminders stored locally (Room DB). */
    val localReminders: LiveData<List<Reminder>> = repository.localReminders

    /** LiveData of reminders fetched from the remote API. */
    private val _apiReminders = MutableLiveData<List<Reminder>>()
    val apiReminders: LiveData<List<Reminder>> = _apiReminders

    /** Holds the reminder converted from API to local entity after successful insertion. */
    private val _convertedReminder = MutableLiveData<ReminderEntity?>()
    val convertedReminder: LiveData<ReminderEntity?> = _convertedReminder

    /** LiveData containing the combined list of local and API reminders with loading and error state handling. */
    private val _allReminders = MediatorLiveData<ResultState<List<Reminder>>>()
    val allReminders: LiveData<ResultState<List<Reminder>>> = _allReminders

    init {
        _allReminders.value = ResultState.Loading
        fetchApiReminders()
        _allReminders.addSource(localReminders) { updateCombined() }
        _allReminders.addSource(apiReminders) { updateCombined() }
    }

    /**
     * Combines local and API reminders into a single list and posts it wrapped in a [ResultState.Success].
     */
    private fun updateCombined() {
        val local = localReminders.value
        val api = apiReminders.value

        if (local == null && api == null) return

        val combined = (local ?: emptyList()) + (api ?: emptyList())
        _allReminders.value = ResultState.Success(combined)
    }

    /**
     * Fetches reminders from the API and updates state accordingly.
     */
    fun fetchApiReminders() {
        viewModelScope.launch {
            _allReminders.value = ResultState.Loading

            when (val result = repository.getApiReminders()) {
                is ResultState.Success -> {
                    _apiReminders.postValue(result.data)
                    // UI will be updated via MediatorLiveData
                }
                is ResultState.Error -> {
                    _apiReminders.postValue(emptyList())
                    _allReminders.value = ResultState.Error(result.message)
                }
                ResultState.Loading -> {
                    _allReminders.value = ResultState.Loading
                }
                else -> {}
            }
        }
    }

    /**
     * Inserts a local reminder and schedules an alarm.
     *
     * @param reminder The reminder entity to insert.
     */
    fun insertReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.insertReminder(reminder)
        }
    }

    /**
     * Updates a reminder in the local database.
     *
     * @param reminder The reminder entity to update.
     */
    fun updateReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.updateReminder(reminder)
        }
    }

    /**
     * Deletes a reminder from the local database.
     *
     * @param reminder The reminder entity to delete.
     */
    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
        }
    }

    /**
     * Converts an API reminder to a local reminder and inserts it into the database.
     *
     * @param apiReminder The remote reminder object to convert and store locally.
     */
    fun convertApiReminderToLocal(apiReminder: Reminder) {
        viewModelScope.launch {
            val entity = ReminderEntity(
                title = apiReminder.title,
                description = apiReminder.description,
                dateTime = apiReminder.dateTime,
                isRecurring = apiReminder.isRecurring,
                recurrenceInterval = apiReminder.recurrenceInterval
            )
            val id = repository.insertAndReturnReminder(entity)
            val inserted = entity.copy(id = id.toInt())
            _convertedReminder.postValue(inserted)
        }
    }
}
