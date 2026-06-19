package com.example.kvantroium.features.events

import com.example.kvantroium.api.ApiClient
import com.example.kvantroium.api.ApiPaths
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

enum class EventKind { ORG, PART }

enum class EventTimeFilter(val label: String, val period: String) {
    ALL("Все время", "all"),
    THIS_WEEK("Эта неделя", "this_week"),
    NEXT_WEEK("Следующая неделя", "next_week"),
    THIS_MONTH("Этот месяц", "this_month"),
    THREE_MONTHS("Три месяца", "three_months")
}

data class EmployeeOption(val id: Int, val name: String)

data class EventTypeOption(val id: Int, val name: String)

data class EventSortOption(
    val field: String,
    val order: String,
    val label: String
) {
    companion object {
        fun defaultFor(kind: EventKind): EventSortOption =
            if (kind == EventKind.ORG) {
                EventSortOption("dates_of_event", "desc", "По дате: позже")
            } else {
                EventSortOption("registration_deadline", "desc", "По дедлайну: позже")
            }

        fun optionsFor(kind: EventKind): List<EventSortOption> =
            if (kind == EventKind.ORG) {
                listOf(
                    EventSortOption("dates_of_event", "desc", "По дате: позже"),
                    EventSortOption("dates_of_event", "asc", "По дате: раньше"),
                    EventSortOption("name", "asc", "По названию: А–Я"),
                    EventSortOption("name", "desc", "По названию: Я–А"),
                    EventSortOption("day_of_the_week", "asc", "По дню недели: А–Я"),
                    EventSortOption("day_of_the_week", "desc", "По дню недели: Я–А")
                )
            } else {
                listOf(
                    EventSortOption("registration_deadline", "desc", "По дедлайну: позже"),
                    EventSortOption("registration_deadline", "asc", "По дедлайну: раньше"),
                    EventSortOption("name", "asc", "По названию: А–Я"),
                    EventSortOption("name", "desc", "По названию: Я–А"),
                    EventSortOption("dates_of_event", "desc", "По дате проведения: позже"),
                    EventSortOption("dates_of_event", "asc", "По дате проведения: раньше"),
                    EventSortOption("participants_amount", "asc", "По числу участников: ↑"),
                    EventSortOption("participants_amount", "desc", "По числу участников: ↓")
                )
            }
    }
}

data class EventResponsible(val name: String)

data class EventListItem(
    val id: Int,
    val name: String,
    val subtitle: String,
    val detail: String,
    val responsibles: List<EventResponsible> = emptyList()
)

data class EventRentItem(
    val roomName: String,
    val startTime: String,
    val endTime: String
)

data class EventOrgDetail(
    val id: Int,
    val name: String,
    val datesOfEvent: String,
    val dayOfWeek: String,
    val place: String,
    val plannedParticipants: Int,
    val actualParticipants: Int,
    val result: String,
    val annotation: String,
    val responsibles: List<EventResponsible> = emptyList(),
    val rents: List<EventRentItem> = emptyList()
)

data class EventPartDetail(
    val id: Int,
    val name: String,
    val formOfHolding: String,
    val level: String,
    val registrationDeadline: String,
    val link: String,
    val participantsAndWorks: String,
    val datesOfEvent: String,
    val annotation: String,
    val participantsAmount: Int,
    val winnerAmount: Int,
    val runnerUpAmount: Int,
    val responsibles: List<EventResponsible> = emptyList()
)

private const val PAGE_SIZE = 10

private val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
private val uiDateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())

fun formatEventDate(value: String?): String {
    if (value.isNullOrBlank()) return "—"
    runCatching {
        val parsed = dbDateFormat.parse(value.trim())
        if (parsed != null) return uiDateFormat.format(parsed)
    }
    return value
}

suspend fun loadEmployeeOptions(apiClient: ApiClient): List<EmployeeOption> {
    val response = apiClient.apiRequest("GET", ApiPaths.Employees.LIST)
    val array = response.optJSONArray("data") ?: JSONArray()
    return buildList {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val id = item.optInt("id_employees", item.optInt("id", 0))
            if (id <= 0) continue
            val name = employeeDisplayName(item)
            if (name.isNotBlank()) add(EmployeeOption(id = id, name = name))
        }
    }.sortedBy { it.name }
}

suspend fun loadEventTypeOptions(apiClient: ApiClient, kind: EventKind): List<EventTypeOption> {
    val paths = if (kind == EventKind.ORG) {
        ApiPaths.Reference.TYPES_OF_ORGANIZATION_TRY
    } else {
        ApiPaths.Reference.LEVELS_TRY
    }
    for (path in paths) {
        val options = runCatching {
            parseReferenceOptions(apiClient.apiRequest("GET", path))
        }.getOrDefault(emptyList())
        if (options.isNotEmpty()) return options
    }
    return emptyList()
}

suspend fun countEvents(
    apiClient: ApiClient,
    kind: EventKind,
    timeFilter: EventTimeFilter,
    employeeId: Int?,
    typeId: Int?,
    search: String?
): Int {
    val path = if (kind == EventKind.ORG) ApiPaths.Events.orgCount() else ApiPaths.Events.partCount()
    val response = apiClient.apiRequest(
        "POST",
        path,
        JSONObject().put("filters", buildFilters(kind, timeFilter, employeeId, typeId, search))
    )
    return response.optInt("total", 0)
}

suspend fun loadEventsPage(
    apiClient: ApiClient,
    kind: EventKind,
    timeFilter: EventTimeFilter,
    employeeId: Int?,
    typeId: Int?,
    search: String?,
    sort: EventSortOption,
    page: Int
): List<EventListItem> {
    val path = if (kind == EventKind.ORG) ApiPaths.Events.orgList() else ApiPaths.Events.partList()
    val body = JSONObject()
        .put("filters", buildFilters(kind, timeFilter, employeeId, typeId, search))
        .put(
            "sort",
            JSONArray().put(
                JSONObject()
                    .put("field", sort.field)
                    .put("order", sort.order)
            )
        )
        .put("page", page)
        .put("limit", PAGE_SIZE)

    val response = apiClient.apiRequest("POST", path, body)
    val array = response.optJSONArray("data") ?: JSONArray()
    val items = buildList {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val id = item.optInt("id", item.optInt("id_events", item.optInt("id_event", 0)))
            if (id <= 0) continue
            add(parseEventListItem(kind, item))
        }
    }.distinctBy { it.id }

    return items.map { item ->
        val responsibles = loadEventResponsibles(apiClient, kind, item.id)
        item.copy(responsibles = responsibles)
    }
}

private suspend fun loadEventResponsibles(
    apiClient: ApiClient,
    kind: EventKind,
    eventId: Int
): List<EventResponsible> {
    val path = if (kind == EventKind.ORG) {
        ApiPaths.Events.orgResponsible(eventId)
    } else {
        ApiPaths.Events.partResponsible(eventId)
    }
    return runCatching {
        val response = apiClient.apiRequest("GET", path)
        val array = response.optJSONArray("data") ?: JSONArray()
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val name = responsibleDisplayName(item)
                if (name.isNotBlank()) add(EventResponsible(name))
            }
        }
    }.getOrDefault(emptyList())
}

internal fun parseReferenceOptions(response: JSONObject): List<EventTypeOption> {
    val array = response.optJSONArray("data") ?: JSONArray()
    return buildList {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val id = item.optInt("id", item.optInt("id_type", 0))
            val name = listOf("name", "type", "form_of_holding", "title")
                .asSequence()
                .map { item.optString(it, "") }
                .firstOrNull { it.isNotBlank() }
                .orEmpty()
            if (id > 0 && name.isNotBlank()) add(EventTypeOption(id = id, name = name))
        }
    }.sortedBy { it.name }
}

fun resolveReferenceOptionName(id: Int?, options: List<EventTypeOption>): String? {
    if (id == null || id <= 0) return null
    return options.firstOrNull { it.id == id }?.name
}

fun resolveFormOfHoldingDisplay(raw: Any?, options: List<EventTypeOption>): String {
    val text = when (raw) {
        null -> ""
        is Number -> raw.toInt().toString()
        else -> raw.toString().trim()
    }
    if (text.isBlank()) return ""
    val id = text.toIntOrNull()
    if (id != null && id > 0) {
        return resolveReferenceOptionName(id, options) ?: text
    }
    return text
}

private fun buildFilters(
    kind: EventKind,
    timeFilter: EventTimeFilter,
    employeeId: Int?,
    typeId: Int?,
    search: String?
): JSONObject {
    val filters = JSONObject().put("period", timeFilter.period)
    if (employeeId != null && employeeId > 0) {
        filters.put("employee_id", employeeId)
    }
    if (typeId != null && typeId > 0) {
        if (kind == EventKind.ORG) {
            filters.put("type", typeId)
        } else {
            filters.put("id_type", typeId)
        }
    }
    val query = search?.trim().orEmpty()
    if (query.isNotEmpty()) {
        filters.put("search", query)
    }
    return filters
}

private fun parseEventListItem(kind: EventKind, item: JSONObject): EventListItem {
    val id = item.optInt("id", item.optInt("id_events", item.optInt("id_event", 0)))
    val name = item.optString("name", "—")
    return if (kind == EventKind.ORG) {
        EventListItem(
            id = id,
            name = name,
            subtitle = formatEventDate(item.optString("dates_of_event", "")),
            detail = item.optString("day_of_the_week", "").uppercase(Locale.getDefault())
        )
    } else {
        val deadline = formatEventDate(
            item.optString("registration_deadline", item.optString("dates_of_event", ""))
        )
        val level = item.optString("level", item.optString("type_name", ""))
        EventListItem(
            id = id,
            name = name,
            subtitle = "регистрация до: $deadline",
            detail = level.ifBlank { "—" }
        )
    }
}

private fun employeeDisplayName(item: JSONObject): String {
    val direct = item.optString("name", "")
    if (direct.isNotBlank()) return direct
    return listOf(
        item.optString("second_name", ""),
        item.optString("first_name", ""),
        item.optString("patronymic", "")
    ).filter { it.isNotBlank() }.joinToString(" ")
}

internal fun responsibleDisplayName(item: JSONObject): String {
    val direct = item.optString("name", "")
    if (direct.isNotBlank()) return direct
    return listOf(
        item.optString("second_name", ""),
        item.optString("first_name", "")
    ).filter { it.isNotBlank() }.joinToString(" ")
}

suspend fun loadEventOrgDetail(apiClient: ApiClient, eventId: Int): EventOrgDetail {
    val response = apiClient.apiRequest("GET", ApiPaths.Events.orgFullInfo(eventId))
    val item = response.optJSONObject("data") ?: response
    val responsibles = loadEventResponsibles(apiClient, EventKind.ORG, eventId)
    val rents = loadEventRents(apiClient, eventId)
    return EventOrgDetail(
        id = eventId,
        name = item.optString("name", "—"),
        datesOfEvent = formatEventDate(item.optString("dates_of_event", "")),
        dayOfWeek = item.optString("day_of_the_week", "").uppercase(Locale.getDefault()),
        place = item.optString("form_of_holding", item.optString("place", "")),
        plannedParticipants = item.optInt("amount_of_planning_application", 0),
        actualParticipants = item.optInt("amount_of_applications", 0),
        result = item.optString("result", ""),
        annotation = item.optString("annotation", ""),
        responsibles = responsibles,
        rents = rents
    )
}

suspend fun loadEventPartDetail(apiClient: ApiClient, eventId: Int): EventPartDetail {
    val response = apiClient.apiRequest("GET", ApiPaths.Events.partFullInfo(eventId))
    val item = response.optJSONObject("data") ?: response
    val responsibles = loadEventResponsibles(apiClient, EventKind.PART, eventId)
    val holdingOptions = loadFormOfHoldingOptions(apiClient)
    val formOfHolding = resolveFormOfHoldingDisplay(item.opt("form_of_holding"), holdingOptions)
    val levelOptions = loadEventTypeOptions(apiClient, EventKind.PART)
    val levelId = item.optInt("id_type", 0).takeIf { it > 0 }
    val level = resolveReferenceOptionName(levelId, levelOptions)
        ?: item.optString("level", item.optString("type_name", ""))
    return EventPartDetail(
        id = eventId,
        name = item.optString("name", "—"),
        formOfHolding = formOfHolding,
        level = level,
        registrationDeadline = formatEventDate(
            item.optString("registration_deadline", item.optString("dates_of_event", ""))
        ),
        link = item.optString("link", ""),
        participantsAndWorks = item.optString("participants_and_works", ""),
        datesOfEvent = formatEventDate(item.optString("dates_of_event", "")),
        annotation = item.optString("annotation", ""),
        participantsAmount = item.optInt("participants_amount", 0),
        winnerAmount = item.optInt("winner_amount", 0),
        runnerUpAmount = item.optInt("runner_up_amount", 0),
        responsibles = responsibles
    )
}

private suspend fun loadEventRents(apiClient: ApiClient, eventId: Int): List<EventRentItem> {
    return runCatching {
        val response = apiClient.apiRequest("GET", ApiPaths.Rent.byEvent(eventId))
        val array = response.optJSONArray("data") ?: JSONArray()
        buildList {
            for (index in 0 until array.length()) {
                val rent = array.optJSONObject(index) ?: continue
                val roomName = rent.optString("name", rent.optString("room_name", ""))
                val startTime = rent.optString("start_time", "")
                val endTime = rent.optString("end_time", "")
                if (roomName.isNotBlank() || startTime.isNotBlank()) {
                    add(EventRentItem(roomName = roomName, startTime = startTime, endTime = endTime))
                }
            }
        }
    }.getOrDefault(emptyList())
}
