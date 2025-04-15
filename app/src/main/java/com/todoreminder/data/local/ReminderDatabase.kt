package com.todoreminder.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The Room database for storing reminders locally.
 *
 * This class defines the database configuration and serves as the app's main access point
 * to the persisted data. It uses the singleton pattern to ensure only one instance is
 * created throughout the app's lifecycle.
 *
 * @property reminderDao Provides access to the DAO for reminder-related database operations.
 */
@Database(entities = [ReminderEntity::class], version = 1, exportSchema = false)
abstract class ReminderDatabase : RoomDatabase() {

    /**
     * Returns the DAO used to access reminder data in the database.
     */
    abstract fun reminderDao(): ReminderDao

    companion object {
        // Ensures visibility of changes to INSTANCE across threads
        @Volatile
        private var INSTANCE: ReminderDatabase? = null

        /**
         * Returns a singleton instance of [ReminderDatabase].
         * If the database has already been created, the existing instance is returned.
         * Otherwise, a new database is created.
         *
         * @param context The application context.
         * @return The singleton instance of [ReminderDatabase].
         */
        fun getDatabase(context: Context): ReminderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ReminderDatabase::class.java,
                    "reminder_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
