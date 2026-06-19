package com.example.kvantroium.features.notifications

import java.util.Calendar
import java.util.Locale

/**
 * Правила push-уведомлений:
 * - 10:00–16:59 (утро): организация и участие на сегодня и завтра (участие — без отметки «Участвовал»).
 * - 17:00–23:59 (вечер): только завтрашние организация и участие.
 * - Круглосуточно: новые мероприятия, где пользователь стал ответственным.
 * Каждый push отправляется не более одного раза (ключ дедупликации).
 */
object NotificationDedup {
    const val SLOT_MORNING = 10
    const val SLOT_EVENING = 17
    private const val NEW_ASSIGNMENT_PUSH_PREFIX = "new-assignment:"

    fun notificationDayKey(calendar: Calendar = Calendar.getInstance()): String {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    }

    fun scheduledPushKey(dayKey: String, slotHour: Int, event: ReminderEvent): String =
        "scheduled:$dayKey:$slotHour:${reminderEntryKey(event)}"

    fun newAssignmentPushKey(event: ReminderEvent): String =
        NEW_ASSIGNMENT_PUSH_PREFIX + reminderEntryKey(event)

    fun stableNotificationId(dedupKey: String): Int {
        val id = dedupKey.hashCode() and 0x7FFFFFFF
        return if (id == 0) 1 else id
    }
}
