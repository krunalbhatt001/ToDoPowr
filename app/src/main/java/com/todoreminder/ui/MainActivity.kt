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
import android.view.View.*
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.todoreminder.data.local.ReminderEntity
import com.todoreminder.data.model.Reminder
import com.todoreminder.data.remote.response.ResultState
import com.todoreminder.databinding.ActivityMainBinding
import com.todoreminder.databinding.ItemReminderBinding
import com.todoreminder.ui.viewmodel.ReminderViewModel
import com.todoreminder.utils.AlarmUtils
import com.todoreminder.utils.NotificationUtils
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

/**
 * MainActivity is the launcher activity of the To-Do Reminder App.
 * It displays the list of reminders and allows users to create, edit, delete,
 * or speak reminders using Text-to-Speech.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ReminderAdapter
    private lateinit var textToSpeech: TextToSpeech

    private val viewModel: ReminderViewModel by viewModels()

    /**
     * Launcher to request POST_NOTIFICATIONS permission.
     */
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

        initTextToSpeech()
        setupRecyclerView()
        observeViewModel()
        setupFabClick()
    }

    /**
     * Initializes the Text-to-Speech engine.
     */
    private fun initTextToSpeech() {
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
    }

    /**
     * Sets up the RecyclerView with the ReminderAdapter.
     */
    private fun setupRecyclerView() {
        adapter = ReminderAdapter(
            emptyList(),
            onClick = { selected -> speakReminder(selected.toReminderEntity()) },
            onEdit = { selected -> handleEditReminder(selected) },
            onDelete = { reminder ->
                viewModel.deleteReminder(reminder.toReminderEntity())
                AlarmUtils.cancelAlarm(this, reminder.id)
            }
        )

        binding.reminderRecyclerView.adapter = adapter
    }

    /**
     * Observes LiveData from the ViewModel for reminder data.
     */
    private fun observeViewModel() {
        viewModel.convertedReminder.observe(this) { reminderEntity ->
            reminderEntity?.let {
                AddEditReminderDialog(
                    existingReminder = it,
                    onSave = { updated ->
                        viewModel.updateReminder(updated)
                        AlarmUtils.cancelAlarm(this, updated.id)
                        AlarmUtils.scheduleAlarm(
                            this,
                            updated.id,
                            updated.title,
                            updated.description,
                            updated.dateTime,
                            updated.recurrenceInterval
                        )
                    }
                ).show(supportFragmentManager, "EditConvertedReminder")
            }
        }

        viewModel.allReminders.observe(this) { result ->
            when (result) {
                is ResultState.Loading -> binding.progressbar.visibility = VISIBLE
                is ResultState.Success -> {
                    binding.progressbar.visibility = GONE
                    adapter.updateList(result.data)
                    binding.reminderRecyclerView.scrollToPosition(0)
                }
                is ResultState.Error -> {
                    binding.progressbar.visibility = GONE
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Handles FAB click to open dialog for adding new reminder.
     */
    private fun setupFabClick() {
        binding.fab.setOnClickListener {
            AddEditReminderDialog(
                onSave = { viewModel.insertReminder(it) }
            ).show(supportFragmentManager, "AddReminder")
        }
    }

    override fun onStart() {
        super.onStart()
        requestNotificationPermission()
    }

    /**
     * Requests POST_NOTIFICATIONS permission if needed.
     */
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

    /**
     * Converts a Reminder to ReminderEntity for local database use.
     */
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

    /**
     * Speaks the reminder title and description using TTS.
     */
    fun speakReminder(reminder: ReminderEntity) {
        val text = "${reminder.title}. ${reminder.description}"
        if (::textToSpeech.isInitialized) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    /**
     * Handles editing reminders (local or API).
     */
    private fun handleEditReminder(selected: Reminder) {
        if (!selected.isFromApi) {
            AddEditReminderDialog(
                existingReminder = selected.toReminderEntity(),
                onSave = {
                    viewModel.updateReminder(it)
                    AlarmUtils.cancelAlarm(this, it.id)
                    AlarmUtils.scheduleAlarm(
                        this,
                        it.id,
                        it.title,
                        it.description,
                        it.dateTime,
                        it.recurrenceInterval
                    )
                }
            ).show(supportFragmentManager, "EditReminder")
        } else {
            AlertDialog.Builder(this)
                .setTitle("Make Editable?")
                .setMessage("This reminder is from an external source. Do you want to make it editable?")
                .setPositiveButton("Yes") { _, _ ->
                    viewModel.convertApiReminderToLocal(selected)
                    Toast.makeText(this, "Reminder made editable. You can now edit it.", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    override fun onDestroy() {
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        super.onDestroy()
    }

    /**
     * RecyclerView Adapter for showing the list of reminders.
     */
    inner class ReminderAdapter(
        private var reminders: List<Reminder>,
        private val onClick: (Reminder) -> Unit,
        private val onEdit: (Reminder) -> Unit,
        private val onDelete: (Reminder) -> Unit
    ) : RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder>() {

        /**
         * ViewHolder class for Reminder item.
         */
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

            holder.binding.deleteButton.visibility = if (item.isFromApi) GONE else VISIBLE

            holder.binding.root.setOnClickListener { onClick(item) }
            holder.binding.editButton.setOnClickListener { onEdit(item) }
            holder.binding.deleteButton.setOnClickListener {
                if (!item.isFromApi) {
                    onDelete(item)
                }
            }
        }

        override fun getItemCount() = reminders.size

        /**
         * Updates the list of reminders using DiffUtil for efficient UI updates.
         */
        fun updateList(newList: List<Reminder>) {
            val diffCallback = ReminderDiffCallback(reminders, newList)
            val diffResult = DiffUtil.calculateDiff(diffCallback)

            reminders = newList
            diffResult.dispatchUpdatesTo(this)
        }

        /**
         * DiffUtil callback for optimizing RecyclerView updates.
         */
        inner class ReminderDiffCallback(
            private val oldList: List<Reminder>,
            private val newList: List<Reminder>
        ) : DiffUtil.Callback() {

            override fun getOldListSize() = oldList.size
            override fun getNewListSize() = newList.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldList[oldItemPosition].id == newList[newItemPosition].id
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldList[oldItemPosition] == newList[newItemPosition]
            }
        }
    }
}

