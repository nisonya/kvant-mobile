package com.example.kvantroium.features.notifications

import android.content.Context
import com.example.kvantroium.api.ApiClient
import com.example.kvantroium.storage.NotificationStorage
import com.example.kvantroium.storage.SessionStorage
import java.util.Calendar

object EventNotificationChecker {

    suspend fun runCheck(context: Context, apiClient: ApiClient, sessionStorage: SessionStorage) {
        val session = sessionStorage.getSession()
        if (!session.isAuthorized) return
        val employeeId = session.user?.employeeId ?: return
        if (employeeId <= 0) return

        val storage = NotificationStorage(context)
        val calendar = Calendar.getInstance()
        val dayKey = NotificationDedup.notificationDayKey(calendar)
        storage.pruneStalePushKeys(dayKey)

        val reminders = loadEventReminders(
            apiClient = apiClient,
            employeeId = employeeId,
            includeNewAssignments = true,
            notificationStorage = storage
        )

        deliverNewAssignmentPushes(context, storage, reminders.newAssignments)
        deliverScheduledPushes(context, storage, calendar, dayKey, reminders)
    }

    /** Новые назначения — проверяются при каждом фоновом опросе. */
    private fun deliverNewAssignmentPushes(
        context: Context,
        storage: NotificationStorage,
        assignments: List<ReminderEvent>
    ) {
        assignments.forEach { event ->
            val dedupKey = NotificationDedup.newAssignmentPushKey(event)
            if (storage.wasPushDelivered(dedupKey)) return@forEach
            if (EventNotificationHelper.showReminder(context, event, dedupKey)) {
                storage.markPushDelivered(dedupKey)
            }
        }
    }

    /**
     * Утренний/вечерний слот — только в окне 10:00–16:59 или 17:00–23:59.
     * До 10:00 scheduled push не отправляются.
     */
    private fun deliverScheduledPushes(
        context: Context,
        storage: NotificationStorage,
        calendar: Calendar,
        dayKey: String,
        reminders: EventReminders
    ) {
        val slotHour = resolveCurrentNotificationSlotHour(calendar) ?: return
        val filtered = filterRemindersForSlot(reminders, slotHour)
        val entries = buildScheduledReminderEntries(filtered)
        entries.forEach { event ->
            val dedupKey = NotificationDedup.scheduledPushKey(dayKey, slotHour, event)
            if (storage.wasPushDelivered(dedupKey)) return@forEach
            if (EventNotificationHelper.showReminder(context, event, dedupKey)) {
                storage.markPushDelivered(dedupKey)
            }
        }
    }
}
