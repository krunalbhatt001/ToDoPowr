# ToDoPowr

APK Link : https://github.com/krunalbhatt001/ToDoPowr/blob/main/Assignment.apk

> **Tech Stack**

**Kotlin:** The primary programming language used for Android development.

**Room Database:** For local storage of reminders.

**Retrofit:** For handling API communication.

**MVVM Architecture:** To separate concerns and manage UI-related data in a lifecycle-conscious way.

**Text-to-Speech (TTS):** For reading out reminder notifications.

**AlarmManager:** For scheduling reminders at specific times.

**Notifications:** For displaying reminder notifications to the user.

**RecyclerView:** For displaying the list of reminders.


> **Permissions**

The app requires the following permissions:

**Notifications**: To show reminders.

**Internet**: To fetch data from the API (if applicable).

**Storage**: For saving and managing reminders locally.

**Alarm** : to get reminder at exact time.

> App Overview

**1. Managing Reminders**
Users can add, edit, and delete reminders, specifying details like the title, description, date, time, and recurrence interval. The reminders are saved in a Room Database to ensure persistence across app launches.

**2. Alarm Management**

The app uses AlarmManager to schedule and cancel alarms for reminders:
Recurring alarms can be set based on the user's preferences.The app handles alarm cancellations when reminders are deleted or updated.

**3. Notifications**

The app sends notifications at the time of the reminder:
Notifications are created using NotificationManager and Notification Channels for managing reminder alerts.
The app ensures that users receive the notifications even if the app is in the background.

**4. Text-to-Speech (TTS)**

Text-to-Speech functionality reads out the reminder details to the user when tap on reminder.


> **Architecture**

The app follows MVVM (Model-View-ViewModel) architecture to maintain a clean separation of concerns:

**Model:** Manages data (Room Database, API calls).

**View:** Displays UI (MainActivity and Dialogs).

**ViewModel:** Handles the app’s data and lifecycle, updating the UI accordingly with LiveData.


> **Error Handling**

The app includes error handling for:

**Network Errors:** Handling no internet connectivity or failed network requests (if applicable).

**Permissions:** Requesting necessary permissions at runtime (e.g., notifications and storage access).
