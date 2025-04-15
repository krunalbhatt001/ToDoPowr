package com.todoreminder.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.todoreminder.data.local.ReminderDatabase
import com.todoreminder.data.model.Reminder
import com.todoreminder.databinding.ActivityMainBinding
import com.todoreminder.databinding.ItemReminderBinding
import com.todoreminder.repository.ReminderRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import com.todoreminder.data.local.ReminderEntity
import com.todoreminder.ui.viewmodel.ReminderViewModel
import com.todoreminder.ui.viewmodel.factory.ReminderViewModelFactory
import com.todoreminder.utils.AlarmUtils
import com.todoreminder.utils.NotificationUtils

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ReminderAdapter
    private lateinit var textToSpeech: TextToSpeech


    private val viewModel: ReminderViewModel by viewModels {
        ReminderViewModelFactory(
            ReminderRepository(
                this,ReminderDatabase.getDatabase(this).reminderDao()
            )
        )
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                requestNotificationPermission()
            } else {
                NotificationUtils.createNotificationChannel(this)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "TTS Language not supported", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "TTS Initialization failed", Toast.LENGTH_SHORT).show()
            }
        }

        adapter = ReminderAdapter(
            emptyList(),
            onClick = { selected ->
                speakReminder(selected.toReminderEntity())
            },
            onEdit = { selected ->
                if (!selected.isFromApi) {
                    AddEditReminderDialog(
                        existingReminder = selected.toReminderEntity(),
                        onSave = { viewModel.updateReminder(it)
                        AlarmUtils.cancelAlarm(this@MainActivity,id=it.id)
                            AlarmUtils.scheduleAlarm(
                                this@MainActivity,
                                id = it.id,
                                title = it.title,
                                desc = it.description,
                                timeInMillis = it.dateTime,
                                recurrenceInterval = it.recurrenceInterval
                            )
                        }
                    ).show(supportFragmentManager, "EditReminder")
                } else {
                    Toast.makeText(this, "Cannot edit API reminders", Toast.LENGTH_SHORT).show()
                }
            },
            onDelete = { reminder ->
                viewModel.deleteReminder(reminder.toReminderEntity()).also {
                    AlarmUtils.cancelAlarm(this@MainActivity,reminder.id)
                }
            }
        )

        with(binding) {
            reminderRecyclerView.adapter = adapter

            viewModel.allReminders.observe(this@MainActivity) { adapter.updateList(it)
                reminderRecyclerView.scrollToPosition(0)
            }

            fab.setOnClickListener {
                AddEditReminderDialog(
                    onSave = {
                       viewModel.insertReminder(it)
                    }
                ).show(supportFragmentManager, "AddReminder")
            }
        }
    }

    override fun onStart() {
        super.onStart()
        requestNotificationPermission()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                AlertDialog.Builder(this)
                    .setTitle("Enable Notifications")
                    .setMessage("We need permission to show reminders at the right time. Tap Allow to get timely alerts.")
                    .setPositiveButton("Allow") { _, _ ->
                        if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", packageName, null)
                            })
                        } else {
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    .setNegativeButton("Deny", null)
                    .show()
            }
        }
    }


    inner class ReminderAdapter(
        private var reminders: List<Reminder>,
        private val onClick: (Reminder) -> Unit,
        private val onEdit: (Reminder) -> Unit,
        private val onDelete: (Reminder) -> Unit
    ) : RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder>() {

        inner class ReminderViewHolder(val binding: ItemReminderBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
            val binding = ItemReminderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ReminderViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
            val item = reminders[position]
            holder.binding.titleText.text = item.title
            holder.binding.descriptionText.text = item.description

            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            holder.binding.timeText.text = sdf.format(Date(item.dateTime))

            if (item.isFromApi) {
                holder.binding.llControls.visibility = GONE
                holder.binding.deleteButton.alpha = 0.5f
            } else {
                holder.binding.llControls.visibility = VISIBLE
                holder.binding.deleteButton.alpha = 1f
            }

            holder.binding.root.setOnClickListener { onClick(item) }
            holder.binding.editButton.setOnClickListener { onEdit(item) }
            holder.binding.deleteButton.setOnClickListener {
                if (!item.isFromApi) {
                    onDelete(item)
                }
            }
        }

        override fun getItemCount() = reminders.size

        fun updateList(newList: List<Reminder>) {
            val diffCallback = ReminderDiffCallback(reminders, newList)
            val diffResult = DiffUtil.calculateDiff(diffCallback)

            reminders = newList
            diffResult.dispatchUpdatesTo(this)
        }

        inner class ReminderDiffCallback(
            private val oldList: List<Reminder>,
            private val newList: List<Reminder>
        ) : DiffUtil.Callback() {

            override fun getOldListSize() = oldList.size

            override fun getNewListSize() = newList.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                // Check using unique identifier
                return oldList[oldItemPosition].id == newList[newItemPosition].id
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldList[oldItemPosition] == newList[newItemPosition]
            }
        }
    }


    fun Reminder.toReminderEntity(): ReminderEntity {
        return ReminderEntity(
            id = id,
            title = title,
            description = description,
            dateTime = dateTime,
            isRecurring = isRecurring,
            recurrenceInterval = recurrenceInterval
        )
    }

    fun speakReminder(reminder: ReminderEntity) {
        val text = "${reminder.title}. ${reminder.description}"
        if (::textToSpeech.isInitialized) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    override fun onDestroy() {
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        super.onDestroy()
    }
}