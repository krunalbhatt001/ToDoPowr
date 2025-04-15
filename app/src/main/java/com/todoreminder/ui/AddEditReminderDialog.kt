package com.todoreminder.ui

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.todoreminder.data.local.ReminderEntity
import com.todoreminder.databinding.DialogAddEditReminderBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * A DialogFragment for adding or editing a reminder.
 *
 * This dialog allows the user to enter a title, description, date/time, and recurrence interval
 * for a reminder. If an existing reminder is passed, it populates the UI with its data.
 *
 * @param onSave Callback invoked when the Save button is pressed with the [ReminderEntity] as a result.
 * @param existingReminder Optional reminder to be edited. If null, a new reminder is created.
 */
class AddEditReminderDialog(
    private val onSave: (ReminderEntity) -> Unit,
    private val existingReminder: ReminderEntity? = null
) : DialogFragment() {

    private var _binding: DialogAddEditReminderBinding? = null
    private val binding get() = _binding!!

    private var selectedDateTime: Calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    /**
     * Creates and returns the dialog UI.
     */
    @SuppressLint("UseGetLayoutInflater")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddEditReminderBinding.inflate(LayoutInflater.from(context))
        val dialog = Dialog(requireContext()).apply {
            setContentView(binding.root)
            setCancelable(true)
        }

        setupRecurrenceSpinner()
        setupDateTimePicker()

        // Pre-fill data if editing existing reminder
        existingReminder?.let {
            binding.titleInput.setText(it.title)
            binding.descInput.setText(it.description)
            selectedDateTime.timeInMillis = it.dateTime
            binding.selectedDateTimeText.text = dateFormat.format(selectedDateTime.time)

            val position = when (it.recurrenceInterval) {
                1L -> 1  // Every 1 Minute
                60L -> 2 // Hourly
                1440L -> 3 // Daily
                else -> 0 // None
            }
            binding.recurrenceSpinner.setSelection(position)
        }

        // Save button action
        binding.saveButton.setOnClickListener {
            val title = binding.titleInput.text.toString().trim()
            val desc = binding.descInput.text.toString().trim()
            val recurrenceOption = binding.recurrenceSpinner.selectedItem.toString()
            val interval = calculateRecurrenceInterval(recurrenceOption)

            if (title.isEmpty()) {
                Toast.makeText(context, "Title cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val reminder = existingReminder?.copy(
                title = title,
                description = desc,
                dateTime = selectedDateTime.timeInMillis,
                isRecurring = interval > 0,
                recurrenceInterval = interval
            ) ?: ReminderEntity(
                title = title,
                description = desc,
                dateTime = selectedDateTime.timeInMillis,
                isRecurring = interval > 0,
                recurrenceInterval = interval
            )

            onSave(reminder)
            dismiss()
        }

        binding.cancelButton.setOnClickListener { dismiss() }

        return dialog
    }

    /**
     * Sets up the date and time picker dialog.
     * Opens a DatePickerDialog first and then a TimePickerDialog.
     */
    private fun setupDateTimePicker() {
        binding.dateTimeButton.setOnClickListener {
            val now = selectedDateTime
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    TimePickerDialog(requireContext(), { _, hour, minute ->
                        selectedDateTime.set(year, month, day, hour, minute)
                        binding.selectedDateTimeText.text = dateFormat.format(selectedDateTime.time)
                    }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false).show()
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    /**
     * Initializes the recurrence interval dropdown spinner with options like None, Hourly, etc.
     */
    private fun setupRecurrenceSpinner() {
        val options = listOf("None", "Every 1 Minute", "Hourly", "Daily")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.recurrenceSpinner.adapter = adapter

        // Select existing recurrence if editing
        val selected = when (existingReminder?.recurrenceInterval) {
            1L -> 1
            60L -> 2
            1440L -> 3
            else -> 0
        }
        binding.recurrenceSpinner.setSelection(selected)
    }

    /**
     * Converts the selected spinner option to a recurrence interval in minutes.
     *
     * @param option The selected recurrence option from the spinner.
     * @return The corresponding interval in minutes.
     */
    private fun calculateRecurrenceInterval(option: String): Long {
        return when (option) {
            "Every 1 Minute" -> 1L
            "Hourly" -> 60L
            "Daily" -> 1440L
            else -> 0L
        }
    }

    /**
     * Cleans up view binding when the view is destroyed.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}