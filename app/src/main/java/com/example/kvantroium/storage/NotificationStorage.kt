package com.example.kvantroium.storage

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar
import java.util.Locale

class NotificationStorage(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun wasPushDelivered(dedupKey: String): Boolean =
        prefs.getStringSet(KEY_SENT_PUSH, emptySet()).orEmpty().contains(dedupKey)

    fun markPushDelivered(dedupKey: String) {
        val sent = prefs.getStringSet(KEY_SENT_PUSH, emptySet()).orEmpty().toMutableSet()
        sent.add(dedupKey)
        prefs.edit().putStringSet(KEY_SENT_PUSH, sent).apply()
    }

    /**
     * Удаляет ключи утренних/вечерних push старше нескольких дней.
     * Ключи новых назначений хранятся бессрочно.
     */
    fun pruneStalePushKeys(currentDayKey: String) {
        val sent = prefs.getStringSet(KEY_SENT_PUSH, emptySet()).orEmpty()
        if (sent.isEmpty()) return
        val cutoffDayKey = subtractDaysFromDayKey(currentDayKey, PUSH_KEY_RETENTION_DAYS)
        val pruned = sent.filter { key -> keepPushKey(key, cutoffDayKey) }.toSet()
        if (pruned.size != sent.size) {
            prefs.edit().putStringSet(KEY_SENT_PUSH, pruned).apply()
        }
    }

    fun getKnownResponsibleKeys(): Set<String> =
        prefs.getStringSet(KEY_KNOWN_RESPONSIBLE, emptySet()).orEmpty()

    fun setKnownResponsibleKeys(keys: Set<String>) {
        prefs.edit()
            .putStringSet(KEY_KNOWN_RESPONSIBLE, keys)
            .putBoolean(KEY_RESPONSIBLE_BASELINE, true)
            .apply()
    }

    fun isResponsibleBaselineInitialized(): Boolean =
        prefs.getBoolean(KEY_RESPONSIBLE_BASELINE, false)

    fun isReminderSeen(key: String): Boolean =
        prefs.getStringSet(KEY_SEEN_REMINDERS, emptySet()).orEmpty().contains(key)

    fun markRemindersSeen(keys: Collection<String>) {
        if (keys.isEmpty()) return
        val seen = prefs.getStringSet(KEY_SEEN_REMINDERS, emptySet()).orEmpty().toMutableSet()
        seen.addAll(keys)
        prefs.edit().putStringSet(KEY_SEEN_REMINDERS, seen).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun keepPushKey(key: String, cutoffDayKey: String): Boolean {
        if (!key.startsWith(SCHEDULED_PUSH_PREFIX)) return true
        val datePart = key.removePrefix(SCHEDULED_PUSH_PREFIX).substringBefore(':')
        return datePart >= cutoffDayKey
    }

    private fun subtractDaysFromDayKey(dayKey: String, days: Long): String {
        val parts = dayKey.split("-")
        if (parts.size != 3) return dayKey
        val calendar = Calendar.getInstance()
        calendar.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.DAY_OF_MONTH, -days.toInt())
        return formatDayKey(calendar)
    }

    private fun formatDayKey(calendar: Calendar): String {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    }

    private companion object {
        const val PREFS_NAME = "kvantroium_notifications"
        const val KEY_SENT_PUSH = "sent_push_keys"
        const val KEY_KNOWN_RESPONSIBLE = "known_responsible_keys"
        const val KEY_RESPONSIBLE_BASELINE = "responsible_baseline_initialized"
        const val KEY_SEEN_REMINDERS = "seen_reminder_keys"
        const val SCHEDULED_PUSH_PREFIX = "scheduled:"
        const val PUSH_KEY_RETENTION_DAYS = 3L
    }
}

fun reminderStorageKey(kind: com.example.kvantroium.features.events.EventKind, eventId: Int): String =
    "${kind.name.lowercase()}:$eventId"

fun newAssignmentStorageKey(kind: com.example.kvantroium.features.events.EventKind, eventId: Int): String =
    "new:${kind.name.lowercase()}:$eventId"
