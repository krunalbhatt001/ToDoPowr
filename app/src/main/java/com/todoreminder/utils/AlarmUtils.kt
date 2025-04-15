package com.todoreminder.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.todoreminder.receivers.ReminderReceiver

/**
 * Utility object for handling alarm-related operations such as scheduling and canceling alarms.
 * This object provides methods for scheduling alarms to trigger reminders at specific times,
 * as well as canceling those alarms when needed.
 */
object AlarmUtils {

    /**
     * Schedules an alarm to trigger a reminder at a specific time.
     *
     * This method uses `AlarmManager` to schedule the alarm with the provided time and
     * recurrence interval. The alarm triggers a broadcast that can be handled by a
     * `BroadcastReceiver` (e.g., `ReminderReceiver`).
     *
     * @param context The application context used to access system services.
     * @param id The unique identifier for the alarm. This ID is used to distinguish this alarm
     *           from other alarms and is necessary for canceling or updating the alarm.
     * @param title The title of the reminder, which will be included in the alarm's intent.
     * @param desc The description of the reminder, which will be included in the alarm's intent.
     * @param timeInMillis The time (in milliseconds) when the alarm should go off.
     * @param recurrenceInterval The recurrence interval in minutes. Default is 0, indicating no recurrence.
     *                           A non-zero value can be used to repeat the alarm at a fixed interval (e.g., every 1 hour).
     */
    fun scheduleAlarm(
        context: Context,
        id: Int,
        title: String,
        desc: String,
        timeInMillis: Long,
        recurrenceInterval: Long = 0
    ) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("id", id)
            putExtra("title", title)
            putExtra("desc", desc)
            putExtra("recurrenceInterval", recurrenceInterval)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
    }

    /**
     * Cancels an existing alarm.
     *
     * This method cancels an alarm that was previously scheduled with the specified `id`.
     * It uses the same `PendingIntent` that was used to schedule the alarm.
     *
     * @param context The application context used to access system services.
     * @param id The unique identifier of the alarm to be canceled.
     */
    fun cancelAlarm(context: Context, id: Int) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }
}

