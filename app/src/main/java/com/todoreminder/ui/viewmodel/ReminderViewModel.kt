package com.todoreminder.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoreminder.data.local.ReminderEntity
import com.todoreminder.data.model.Reminder
import com.todoreminder.repository.ReminderRepository
import com.todoreminder.utils.AlarmUtils
import kotlinx.coroutines.launch

class ReminderViewModel(private val repository: ReminderRepository) : ViewModel() {

    val localReminders: LiveData<List<Reminder>> = repository.localReminders

    private val _apiReminders = MutableLiveData<List<Reminder>>()
    val apiReminders: LiveData<List<Reminder>> = _apiReminders

    val allReminders: LiveData<List<Reminder>> = MediatorLiveData<List<Reminder>>().apply {
        addSource(localReminders) { updateCombined() }
        addSource(apiReminders) { updateCombined() }
    }

    init {
        fetchApiReminders()
    }

    private fun MediatorLiveData<List<Reminder>>.updateCombined() {
        val local = localReminders.value ?: emptyList()
        val api = apiReminders.value ?: emptyList()
        value = local + api
    }

    fun fetchApiReminders() {
        viewModelScope.launch {
            val data = repository.getApiReminders()
            _apiReminders.postValue(data)
        }
    }

    fun insertReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.insertReminder(reminder)
        }
    }

    fun updateReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.updateReminder(reminder)
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
        }
    }
}