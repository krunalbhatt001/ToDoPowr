package com.todoreminder.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.todoreminder.utils.AlarmUtils
import com.todoreminder.utils.NotificationUtils

/**
 * [ReminderReceiver] is a [BroadcastReceiver] that listens for alarm broadcasts
 * and shows a notification when a reminder is triggered.
 *
 * If the reminder is recurring, it also schedules the next alarm.
 */
class ReminderReceiver : BroadcastReceiver() {

    /**
     * Called when the broadcast is received.
     *
     * @param context The [Context] in which the receiver is running.
     * @param intent The [Intent] being received, containing reminder details.
     */
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("id", -1)
        val title = intent.getStringExtra("title") ?: "No Title"
        val desc = intent.getStringExtra("desc") ?: "No Description"
        val recurrence = intent.getLongExtra("recurrenceInterval", 0)

        // ✅ Show the reminder notification
        NotificationUtils.showReminderNotification(context, title, desc)

        // ✅ If it's a recurring reminder, schedule the next alarm
        if (recurrence > 0) {
            val nextTriggerTime = System.currentTimeMillis() + (recurrence * 60 * 1000)
            AlarmUtils.scheduleAlarm(
                context = context,
                id = id,
                title = title,
                desc = desc,
                timeInMillis = nextTriggerTime,
                recurrenceInterval = recurrence
            )
        }
    }
}

