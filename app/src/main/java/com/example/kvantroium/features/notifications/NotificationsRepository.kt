package com.example.kvantroium.features.notifications

import com.example.kvantroium.api.ApiClient
import com.example.kvantroium.api.ApiPaths
import com.example.kvantroium.features.events.EventKind
import com.example.kvantroium.features.events.EventSortOption
import com.example.kvantroium.features.events.EventTimeFilter
import com.example.kvantroium.features.events.formatEventDateInput
import com.example.kvantroium.features.events.loadEventsPage
import com.example.kvantroium.features.events.loadFormOfHoldingOptions
import com.example.kvantroium.features.events.resolveFormOfHoldingDisplay
import com.example.kvantroium.storage.NotificationStorage
import com.example.kvantroium.storage.newAssignmentStorageKey
import com.example.kvantroium.storage.reminderStorageKey
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale

enum class ReminderCategory {
    ORG,
    PART,
    NEW_ASSIGNMENT
}

data class ReminderEvent(
    val id: Int,
    val name: String,
    val meta: String,
    val kind: EventKind,
    val category: ReminderCategory = ReminderCategory.ORG,
    val whenLabel: String = ""
)

data class EventReminders(
    val orgToday: List<ReminderEvent>,
    val orgTomorrow: List<ReminderEvent>,
    val partToday: List<ReminderEvent>,
    val partTomorrow: List<ReminderEvent>,
    val newAssignments: List<ReminderEvent> = emptyList()
) {
    fun isEmpty(): Boolean =
        orgToday.isEmpty() && orgTomorrow.isEmpty() &&
            partToday.isEmpty() && partTomorrow.isEmpty() && newAssignments.isEmpty()

    fun totalCount(): Int =
        orgToday.size + orgTomorrow.size + partToday.size + partTomorrow.size + newAssignments.size
}

fun eventReminderViewKeys(reminders: EventReminders): List<String> = buildList {
    reminders.newAssignments.forEach { add(reminderEntryKey(it)) }
    buildScheduledReminderEntries(reminders).forEach { add(reminderEntryKey(it)) }
}

fun countUnseenReminders(reminders: EventReminders, storage: NotificationStorage): Int =
    eventReminderViewKeys(reminders).count { !storage.isReminderSeen(it) }

fun markRemindersViewed(reminders: EventReminders, storage: NotificationStorage) {
    storage.markRemindersSeen(eventReminderViewKeys(reminders))
}

fun resolveCurrentNotificationSlotHour(calendar: Calendar = Calendar.getInstance()): Int? {
    return when (calendar.get(Calendar.HOUR_OF_DAY)) {
        in 0..9 -> null
        in 10..16 -> 10
        else -> 17
    }
}

fun notificationSlotKey(calendar: Calendar, slotHour: Int): String {
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH) + 1
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    return String.format(Locale.US, "%04d-%02d-%02d:%d", year, month, day, slotHour)
}

fun filterRemindersForSlot(reminders: EventReminders, slotHour: Int): EventReminders {
    if (slotHour != 17) return reminders.copy(newAssignments = emptyList())
    return EventReminders(
        orgToday = emptyList(),
        orgTomorrow = reminders.orgTomorrow,
        partToday = emptyList(),
        partTomorrow = reminders.partTomorrow,
        newAssignments = emptyList()
    )
}

suspend fun loadEventReminders(
    apiClient: ApiClient,
    employeeId: Int,
    includeNewAssignments: Boolean = false,
    notificationStorage: NotificationStorage? = null
): EventReminders {
    val holdingOptions = loadFormOfHoldingOptions(apiClient)
    val orgToday = loadOrgReminders(apiClient, ApiPaths.Events.orgNotificationsToday(employeeId), EventKind.ORG)
    val orgTomorrow = loadOrgReminders(apiClient, ApiPaths.Events.orgNotificationsTomorrow(employeeId), EventKind.ORG)
    val partTodayRaw = loadPartReminders(
        apiClient,
        ApiPaths.Events.partNotificationsToday(employeeId),
        EventKind.PART,
        holdingOptions
    )
    val partTomorrowRaw = loadPartReminders(
        apiClient,
        ApiPaths.Events.partNotificationsTomorrow(employeeId),
        EventKind.PART,
        holdingOptions
    )
    val partToday = filterPartEventsUnmarked(apiClient, employeeId, partTodayRaw)
    val partTomorrow = filterPartEventsUnmarked(apiClient, employeeId, partTomorrowRaw)
    val newAssignments = if (includeNewAssignments && notificationStorage != null) {
        detectNewResponsibleAssignments(apiClient, employeeId, notificationStorage)
    } else {
        emptyList()
    }
    return EventReminders(
        orgToday = orgToday,
        orgTomorrow = orgTomorrow,
        partToday = partToday,
        partTomorrow = partTomorrow,
        newAssignments = newAssignments
    )
}

fun buildScheduledReminderEntries(reminders: EventReminders): List<ReminderEvent> {
    return buildList {
        reminders.orgToday.forEach {
            add(it.copy(category = ReminderCategory.ORG, whenLabel = "Сегодня"))
        }
        reminders.orgTomorrow.forEach {
            add(it.copy(category = ReminderCategory.ORG, whenLabel = "Завтра"))
        }
        reminders.partToday.forEach {
            add(it.copy(category = ReminderCategory.PART, whenLabel = "Сегодня"))
        }
        reminders.partTomorrow.forEach {
            add(it.copy(category = ReminderCategory.PART, whenLabel = "Завтра"))
        }
    }
}

fun reminderEntryKey(event: ReminderEvent): String = when (event.category) {
    ReminderCategory.NEW_ASSIGNMENT -> newAssignmentStorageKey(event.kind, event.id)
    else -> reminderStorageKey(event.kind, event.id) + ":" + event.whenLabel.lowercase(Locale.getDefault())
}

fun reminderEntryTitle(event: ReminderEvent): String = when (event.category) {
    ReminderCategory.NEW_ASSIGNMENT -> "Новое мероприятие"
    ReminderCategory.ORG -> "Организация: ${event.name}"
    ReminderCategory.PART -> "Участие: ${event.name}"
}

fun reminderEntryBody(event: ReminderEvent): String {
    if (event.category == ReminderCategory.NEW_ASSIGNMENT) {
        val kindLabel = if (event.kind == EventKind.ORG) "Организация" else "Участие"
        return "$kindLabel · ${event.name}" + if (event.meta.isNotBlank()) " · ${event.meta}" else ""
    }
    return buildList {
        if (event.whenLabel.isNotBlank()) add(event.whenLabel)
        if (event.meta.isNotBlank()) add(event.meta)
    }.joinToString(" · ")
}

private suspend fun filterPartEventsUnmarked(
    apiClient: ApiClient,
    employeeId: Int,
    events: List<ReminderEvent>
): List<ReminderEvent> = coroutineScope {
    if (events.isEmpty()) return@coroutineScope emptyList()
    events.map { event ->
        async {
            if (isPartParticipationUnmarked(apiClient, employeeId, event.id)) event else null
        }
    }.awaitAll().filterNotNull()
}

private suspend fun isPartParticipationUnmarked(
    apiClient: ApiClient,
    employeeId: Int,
    eventId: Int
): Boolean {
    return runCatching {
        val response = apiClient.apiRequest("GET", ApiPaths.Events.partResponsibleNew(eventId))
        val array = response.optJSONArray("data") ?: JSONArray()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val rowEmployeeId = item.optInt("id_employees", item.optInt("id_employee", 0))
            if (rowEmployeeId != employeeId) continue
            return item.optInt("mark_of_sending_an_application", 0) != 1
        }
        false
    }.getOrDefault(false)
}

private suspend fun detectNewResponsibleAssignments(
    apiClient: ApiClient,
    employeeId: Int,
    storage: NotificationStorage
): List<ReminderEvent> {
    val currentKeys = loadAllResponsibleEventKeys(apiClient, employeeId)
    if (!storage.isResponsibleBaselineInitialized()) {
        storage.setKnownResponsibleKeys(currentKeys)
        return emptyList()
    }
    val knownKeys = storage.getKnownResponsibleKeys()
    val addedKeys = currentKeys - knownKeys
    if (addedKeys.isEmpty()) {
        storage.setKnownResponsibleKeys(currentKeys)
        return emptyList()
    }
    val newEvents = loadResponsibleEventsByKeys(apiClient, employeeId, addedKeys)
    storage.setKnownResponsibleKeys(currentKeys)
    return newEvents
}

private suspend fun loadAllResponsibleEventKeys(
    apiClient: ApiClient,
    employeeId: Int
): Set<String> {
    val keys = linkedSetOf<String>()
    for (kind in listOf(EventKind.ORG, EventKind.PART)) {
        var page = 1
        while (true) {
            val items = loadEventsPage(
                apiClient = apiClient,
                kind = kind,
                timeFilter = EventTimeFilter.ALL,
                employeeId = employeeId,
                typeId = null,
                search = null,
                sort = EventSortOption.defaultFor(kind),
                page = page
            )
            if (items.isEmpty()) break
            items.forEach { keys.add(responsibleEventKey(kind, it.id)) }
            page++
            if (items.size < 10) break
        }
    }
    return keys
}

private suspend fun loadResponsibleEventsByKeys(
    apiClient: ApiClient,
    employeeId: Int,
    keys: Set<String>
): List<ReminderEvent> {
    if (keys.isEmpty()) return emptyList()
    val holdingOptions = loadFormOfHoldingOptions(apiClient)
    val byKind = keys.groupBy { key -> key.substringBefore(":") }
    return buildList {
        byKind["org"]?.forEach { key ->
            val eventId = key.substringAfter(":").toIntOrNull() ?: return@forEach
            loadOrgEventReminder(apiClient, eventId)?.let { add(it) }
        }
        byKind["part"]?.forEach { key ->
            val eventId = key.substringAfter(":").toIntOrNull() ?: return@forEach
            loadPartEventReminder(apiClient, eventId, holdingOptions)?.let { add(it) }
        }
    }
}

private suspend fun loadOrgEventReminder(apiClient: ApiClient, eventId: Int): ReminderEvent? {
    return runCatching {
        val response = apiClient.apiRequest("GET", ApiPaths.Events.orgFullInfo(eventId))
        val data = response.optJSONObject("data") ?: response
        val id = data.optInt("id", data.optInt("id_events", eventId))
        if (id <= 0) return@runCatching null
        ReminderEvent(
            id = id,
            name = data.optString("name", "—"),
            meta = formatEventDateInput(data.optString("dates_of_event", "")),
            kind = EventKind.ORG,
            category = ReminderCategory.NEW_ASSIGNMENT
        )
    }.getOrNull()
}

private suspend fun loadPartEventReminder(
    apiClient: ApiClient,
    eventId: Int,
    holdingOptions: List<com.example.kvantroium.features.events.EventTypeOption>
): ReminderEvent? {
    return runCatching {
        val response = apiClient.apiRequest("GET", ApiPaths.Events.partFullInfo(eventId))
        val data = response.optJSONObject("data") ?: response
        val id = data.optInt("id", data.optInt("id_events", eventId))
        if (id <= 0) return@runCatching null
        val meta = buildList {
            formatEventDateInput(data.optString("registration_deadline", ""))
                .takeIf { it.isNotBlank() }
                ?.let { add("рег. до $it") }
            resolveFormOfHoldingDisplay(data.opt("form_of_holding"), holdingOptions)
                .takeIf { it.isNotBlank() }
                ?.let { add(it) }
        }.joinToString(" · ")
        ReminderEvent(
            id = id,
            name = data.optString("name", "—"),
            meta = meta,
            kind = EventKind.PART,
            category = ReminderCategory.NEW_ASSIGNMENT
        )
    }.getOrNull()
}

private fun responsibleEventKey(kind: EventKind, eventId: Int): String =
    "${kind.name.lowercase()}:$eventId"

private suspend fun loadOrgReminders(
    apiClient: ApiClient,
    path: String,
    kind: EventKind
): List<ReminderEvent> {
    val response = apiClient.apiRequest("GET", path)
    val array = response.optJSONArray("data") ?: JSONArray()
    return parseReminderArray(array, kind) { item ->
        buildList {
            formatEventDateInput(item.optString("dates_of_event", ""))
                .takeIf { it.isNotBlank() }
                ?.let { add(it) }
            item.optString("day_of_the_week", "")
                .takeIf { it.isNotBlank() }
                ?.let { add(it.uppercase()) }
        }.joinToString(" · ")
    }
}

private suspend fun loadPartReminders(
    apiClient: ApiClient,
    path: String,
    kind: EventKind,
    holdingOptions: List<com.example.kvantroium.features.events.EventTypeOption>
): List<ReminderEvent> {
    val response = apiClient.apiRequest("GET", path)
    val array = response.optJSONArray("data") ?: JSONArray()
    return parseReminderArray(array, kind) { item ->
        buildList {
            formatEventDateInput(item.optString("registration_deadline", ""))
                .takeIf { it.isNotBlank() }
                ?.let { add("рег. до $it") }
            resolveFormOfHoldingDisplay(item.opt("form_of_holding"), holdingOptions)
                .takeIf { it.isNotBlank() }
                ?.let { add(it) }
        }.joinToString(" · ")
    }
}

private fun parseReminderArray(
    array: JSONArray,
    kind: EventKind,
    metaBuilder: (JSONObject) -> String
): List<ReminderEvent> {
    return buildList {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val id = item.optInt("id", 0)
            if (id <= 0) continue
            add(
                ReminderEvent(
                    id = id,
                    name = item.optString("name", "—"),
                    meta = metaBuilder(item),
                    kind = kind
                )
            )
        }
    }
}
