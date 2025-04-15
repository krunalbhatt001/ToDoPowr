package com.todoreminder.data.remote

import com.todoreminder.data.remote.response.ResultState
import retrofit2.http.GET

interface ReminderApiService {
    @GET("todos")
    suspend fun getApiReminders(): List<ApiReminder>
}