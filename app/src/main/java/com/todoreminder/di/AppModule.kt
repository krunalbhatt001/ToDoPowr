package com.todoreminder.di

import android.content.Context
import com.todoreminder.data.local.ReminderDao
import com.todoreminder.data.local.ReminderDatabase
import com.todoreminder.data.remote.ReminderApiService
import com.todoreminder.repository.ReminderRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger-Hilt module that provides application-level dependencies.
 * Installed in the SingletonComponent, which means all provided dependencies
 * will follow the singleton lifecycle of the application.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides a singleton instance of [ReminderDatabase].
     *
     * @param context Application context injected by Hilt.
     * @return Instance of [ReminderDatabase].
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ReminderDatabase {
        return ReminderDatabase.getDatabase(context)
    }

    /**
     * Provides an instance of [ReminderDao] from the [ReminderDatabase].
     *
     * @param database Instance of [ReminderDatabase].
     * @return Instance of [ReminderDao].
     */
    @Provides
    fun provideDao(database: ReminderDatabase): ReminderDao {
        return database.reminderDao()
    }

    /**
     * Provides a singleton instance of [ReminderRepository], which acts as the
     * single source of truth for data access from both local database and API.
     *
     * @param context Application context injected by Hilt.
     * @param dao Local database DAO used for CRUD operations.
     * @param apiService Retrofit service for remote data access.
     * @return Instance of [ReminderRepository].
     */
    @Provides
    @Singleton
    fun provideRepository(
        @ApplicationContext context: Context,
        dao: ReminderDao,
        apiService: ReminderApiService
    ): ReminderRepository {
        return ReminderRepository(context, dao, apiService)
    }
}


