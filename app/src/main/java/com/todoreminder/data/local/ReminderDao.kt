package com.todoreminder.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * Data Access Object (DAO) interface for performing CRUD operations on ReminderEntity.
 */
@Dao
interface ReminderDao {

    /**
     * Inserts a new reminder into the database.
     * If a reminder with the same ID already exists, it will be replaced.
     *
     * @param reminder The reminder to be inserted.
     * @return The newly inserted reminder's row ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity): Long

    /**
     * Updates an existing reminder in the database.
     *
     * @param reminder The reminder with updated values.
     */
    @Update
    suspend fun update(reminder: ReminderEntity)

    /**
     * Deletes a reminder from the database.
     *
     * @param reminder The reminder to be deleted.
     */
    @Delete
    suspend fun delete(reminder: ReminderEntity)

    /**
     * Retrieves all reminders sorted by their date and time in ascending order.
     *
     * @return A LiveData list of all reminders.
     */
    @Query("SELECT * FROM reminders ORDER BY dateTime ASC")
    fun getAllReminders(): LiveData<List<ReminderEntity>>

    /**
     * Deletes a reminder by its unique ID.
     *
     * @param reminderId The ID of the reminder to be deleted.
     */
    @Query("DELETE FROM reminders WHERE id = :reminderId")
    suspend fun deleteReminderById(reminderId: Int)

    /**
     * Retrieves a single reminder by its ID.
     *
     * @param reminderId The ID of the reminder to retrieve.
     * @return The ReminderEntity if found, or null otherwise.
     */
    @Query("SELECT * FROM reminders WHERE id = :reminderId")
    suspend fun getReminderById(reminderId: Int): ReminderEntity?
}
