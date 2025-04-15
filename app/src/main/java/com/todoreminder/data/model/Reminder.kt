package com.todoreminder.data.model

data class Reminder(
    val id: Int = 0,
    val title: String,
    val description: String,
    val dateTime: Long,
    val isRecurring: Boolean = false,
    val recurrenceInterval: Long = 0L,
    val isFromApi: Boolean = false
)
