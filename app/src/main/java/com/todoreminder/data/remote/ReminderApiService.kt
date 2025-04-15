package com.todoreminder.data.remote

import retrofit2.http.GET

interface ReminderApiService {
    @GET("todos")
    suspend fun getApiReminders(): List<ApiReminder>
}