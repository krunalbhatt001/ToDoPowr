package com.todoreminder.receivers

import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.todoreminder.data.local.ReminderDao
import com.todoreminder.data.local.ReminderDatabase
import com.todoreminder.data.local.ReminderEntity
import com.todoreminder.utils.AlarmUtils
import com.todoreminder.utils.NotificationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("id", -1)
        val title = intent.getStringExtra("title") ?: "No Title"
        val desc = intent.getStringExtra("desc") ?: "No Description"
        val recurrence = intent.getLongExtra("recurrenceInterval", 0)

        Log.d("ReminderReceiver", "Reminder Received - Title: $title, id: $id")

        // ✅ Show the notification
        NotificationUtils.showReminderNotification(context, title, desc)

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
            Log.d("ReminderReceiver", "Recurring reminder rescheduled in $recurrence minutes")
        }
    }
}
