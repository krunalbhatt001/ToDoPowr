package com.todoreminder.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.todoreminder.R
import kotlin.random.Random

/**
 * Utility object for handling notification-related operations such as creating channels
 * and showing reminder notifications.
 */
object NotificationUtils {

    /**
     * Creates a notification channel for reminders.
     *
     * This is required for Android O (API 26) and above. The channel is identified by
     * the ID `"reminder_channel"` and is used to group and configure reminder notifications.
     *
     * @param context The application context used to access system services.
     */
    fun createNotificationChannel(context: Context) {
        val name = "Reminders"
        val desc = "Reminder notifications for your tasks"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("reminder_channel", name, importance).apply {
            description = desc
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    /**
     * Displays a high-priority reminder notification to the user.
     *
     * If the required notification permission is not granted (Android 13+), this function
     * opens the app settings screen to allow the user to manually grant permission.
     *
     * @param context The application context used to build and display the notification.
     * @param title The title of the notification (typically the reminder title).
     * @param desc The content text of the notification (typically the reminder description).
     */
    fun showReminderNotification(context: Context, title: String, desc: String) {
        val builder = NotificationCompat.Builder(context, "reminder_channel")
            .setSmallIcon(R.drawable.baseline_access_alarm_24) // Replace with your app icon
            .setContentTitle(title)
            .setContentText(desc)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val notificationManager = NotificationManagerCompat.from(context)
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Open settings screen to let user enable notification permission
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            })
            return
        }
        notificationManager.notify(Random.nextInt(), builder.build())
    }
}

