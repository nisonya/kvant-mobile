package com.example.kvantroium.features.schedule

import com.example.kvantroium.api.ApiClient
import com.example.kvantroium.api.ApiPaths
import com.example.kvantroium.features.events.formatTimeForDisplay
import org.json.JSONArray
import org.json.JSONObject

enum class ScheduleTab(val label: String, val placeholder: String) {
    TEACHERS("Наставники", "Выберите наставника"),
    ROOMS("Кабинеты", "Выберите кабинет"),
    GROUPS("Группы", "Выберите группу")
}

data class ScheduleOption(val id: Int, val name: String)

data class ScheduleLesson(
    val id: Int,
    val room: String,
    val group: String,
    val teacherName: String,
    val day: String,
    val dayNum: Int,
    val startTime: String,
    val endTime: String,
    val roomId: Int = 0,
    val groupId: Int = 0,
    val employeeId: Int = 0
) {
    val timeRange: String
        get() {
            val parts = listOf(startTime, endTime).filter { it.isNotBlank() }
            return if (parts.size == 2) "${parts[0]} - ${parts[1]}" else parts.firstOrNull().orEmpty()
        }
}

private val dayOrder = listOf(
    "понедельник", "вторник", "среда", "четверг", "пятница", "суббота", "воскресенье"
)

private val dayToNum = mapOf(
    "понедельник" to 1,
    "вторник" to 2,
    "среда" to 3,
    "четверг" to 4,
    "пятница" to 5,
    "суббота" to 6,
    "воскресенье" to 7
)

private val numToDay = dayToNum.entries.associate { (k, v) -> v to k }

/** idDay в БД (weekday.idDay) → ключ дня */
private val dbDayIdToName = mapOf(
    1 to "воскресенье",
    2 to "понедельник",
    3 to "вторник",
    4 to "среда",
    5 to "четверг",
    6 to "пятница",
    7 to "суббота"
)

private val dbNameToDay = mapOf(
    "ВОСКРЕСЕНЬЕ" to "воскресенье",
    "ПОНЕДЕЛЬНИК" to "понедельник",
    "ВТОРНИК" to "вторник",
    "СРЕДА" to "среда",
    "ЧЕТВЕРГ" to "четверг",
    "ПЯТНИЦА" to "пятница",
    "СУББОТА" to "суббота"
)

fun formatDayTitle(dayKey: String): String {
    val normalized = normalizeDayKey(dayKey, 0)
    if (normalized == "Без дня") return normalized
    return normalized.uppercase(java.util.Locale("ru"))
}

fun normalizeDayKey(rawDay: String, dayNum: Int): String {
    val trimmed = rawDay.trim()
    if (trimmed.isEmpty()) {
        return dbDayIdToName[dayNum]?.takeIf { dayNum > 0 } ?: "Без дня"
    }
    val lower = trimmed.lowercase(java.util.Locale("ru"))
    if (lower in dayToNum) return lower
    dbNameToDay[trimmed.uppercase(java.util.Locale("ru"))]?.let { return it }
    trimmed.toIntOrNull()?.let { id ->
        dbDayIdToName[id]?.let { return it }
    }
    return lower.ifBlank { "Без дня" }
}

fun groupLessonsByDay(lessons: List<ScheduleLesson>): List<Pair<String, List<ScheduleLesson>>> {
    return lessons
        .map { lesson ->
            val dayKey = normalizeDayKey(lesson.day, lesson.dayNum)
            lesson.copy(day = dayKey, dayNum = dayToNum[dayKey] ?: lesson.dayNum)
        }
        .sortedWith(
            compareBy<ScheduleLesson> { lesson ->
                dayOrder.indexOf(lesson.day).takeIf { it >= 0 } ?: 999
            }.thenBy { it.startTime }
        )
        .groupBy { it.day.ifBlank { "Без дня" } }
        .entries
        .sortedBy { (day, _) ->
            dayOrder.indexOf(day).takeIf { it >= 0 } ?: 999
        }
        .map { it.key to it.value }
}

suspend fun loadScheduleOptions(apiClient: ApiClient, tab: ScheduleTab): List<ScheduleOption> {
    return when (tab) {
        ScheduleTab.TEACHERS -> loadTeacherOptions(apiClient)
        ScheduleTab.ROOMS -> parseOptions(
            apiClient.apiRequest("GET", ApiPaths.Reference.ROOMS).optJSONArray("data")
        )
        ScheduleTab.GROUPS -> parseOptions(
            apiClient.apiRequest("GET", ApiPaths.Schedule.GROUPS).optJSONArray("data")
        )
    }.sortedBy { it.name.lowercase() }
}

suspend fun loadTeacherOptions(apiClient: ApiClient): List<ScheduleOption> {
    val array = runCatching {
        apiClient.apiRequest("GET", ApiPaths.Employees.WITH_INACTIVE).optJSONArray("data")
    }.getOrElse {
        apiClient.apiRequest("GET", ApiPaths.Employees.ALL).optJSONArray("data")
    } ?: JSONArray()

    return buildList {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            if (!isActiveEmployee(item)) continue
            if (!isTeacherEmployee(item)) continue
            parseEmployeeOption(item)?.let(::add)
        }
    }
}

suspend fun loadScheduleLessons(apiClient: ApiClient, tab: ScheduleTab, entityId: Int): List<ScheduleLesson> {
    if (entityId <= 0) return emptyList()
    val path = when (tab) {
        ScheduleTab.TEACHERS -> ApiPaths.Schedule.byTeacher(entityId)
        ScheduleTab.ROOMS -> ApiPaths.Schedule.byRoom(entityId)
        ScheduleTab.GROUPS -> ApiPaths.Schedule.byGroup(entityId)
    }
    val array = apiClient.apiRequest("GET", path).optJSONArray("data") ?: JSONArray()
    return parseLessons(array)
}

suspend fun loadScheduleReferenceOptions(apiClient: ApiClient): ScheduleReferences {
    val rooms = parseOptions(apiClient.apiRequest("GET", ApiPaths.Reference.ROOMS).optJSONArray("data"))
    val groups = parseOptions(apiClient.apiRequest("GET", ApiPaths.Schedule.GROUPS).optJSONArray("data"))
    val teachers = loadTeacherOptions(apiClient)
    return ScheduleReferences(rooms = rooms, groups = groups, teachers = teachers)
}

data class ScheduleReferences(
    val rooms: List<ScheduleOption>,
    val groups: List<ScheduleOption>,
    val teachers: List<ScheduleOption>
)

data class ScheduleLessonDraft(
    val id: Int = 0,
    val roomId: Int = 0,
    val groupId: Int = 0,
    val employeeId: Int = 0,
    val dayNum: Int = 0,
    val startTime: String = "",
    val endTime: String = ""
)

suspend fun createScheduleLesson(apiClient: ApiClient, draft: ScheduleLessonDraft) {
    apiClient.apiRequest(
        method = "POST",
        path = ApiPaths.Schedule.ROOT,
        body = JSONObject()
            .put("room_id", draft.roomId)
            .put("group_id", draft.groupId)
            .put("employee_id", draft.employeeId)
            .put("day", draft.dayNum)
            .put("start_time", formatTimeForApi(draft.startTime))
            .put("end_time", formatTimeForApi(draft.endTime))
    )
}

suspend fun updateScheduleLesson(apiClient: ApiClient, draft: ScheduleLessonDraft) {
    require(draft.id > 0) { "Некорректный id занятия" }
    apiClient.apiRequest(
        method = "PUT",
        path = ApiPaths.Schedule.ROOT,
        body = JSONObject()
            .put("id", draft.id)
            .put("room_id", draft.roomId)
            .put("group_id", draft.groupId)
            .put("start_time", formatTimeForApi(draft.startTime))
            .put("end_time", formatTimeForApi(draft.endTime))
    )
}

suspend fun deleteScheduleLesson(apiClient: ApiClient, lessonId: Int) {
    apiClient.apiRequest("DELETE", ApiPaths.Schedule.byId(lessonId))
}

private fun parseOptions(array: JSONArray?): List<ScheduleOption> {
    if (array == null) return emptyList()
    return buildList {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val id = item.optInt("id", item.optInt("idGroups", item.optInt("id_employees", 0)))
            val name = item.optString("name", "").trim()
            if (id > 0 && name.isNotBlank()) add(ScheduleOption(id = id, name = name))
        }
    }
}

private fun parseLessons(array: JSONArray): List<ScheduleLesson> {
    return buildList {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            parseLesson(item)?.let(::add)
        }
    }
}

private fun parseLesson(item: JSONObject): ScheduleLesson? {
    val id = item.optInt("id", item.optInt("idlesson", item.optInt("id_lesson", 0)))
    if (id <= 0) return null
    val dayRaw = item.opt("day") ?: item.opt("day_name")
    val (dayName, dayNum) = resolveDayFromRaw(dayRaw)
    return ScheduleLesson(
        id = id,
        room = item.optString("room", item.optString("room_name", "")),
        group = item.optString("group", item.optString("group_name", "")),
        teacherName = item.optString("name", item.optString("teacher_name", item.optString("teacher", ""))),
        day = dayName,
        dayNum = dayNum,
        startTime = formatTimeForDisplay(item.optString("startTime", item.optString("start_time", ""))),
        endTime = formatTimeForDisplay(item.optString("endTime", item.optString("end_time", ""))),
        roomId = item.optInt("room_id", item.optInt("id_room", 0)),
        groupId = item.optInt("group_id", item.optInt("id_group", 0)),
        employeeId = item.optInt("employee_id", item.optInt("id_employees", item.optInt("id_employee", 0)))
    )
}

private fun resolveDayFromRaw(raw: Any?): Pair<String, Int> {
    if (raw == null) return "Без дня" to 0
    when (raw) {
        is Number -> {
            val dayId = raw.toInt()
            numToDay[dayId]?.let { return it to dayId }
            dbDayIdToName[dayId]?.let { name -> return name to (dayToNum[name] ?: dayId) }
            return "Без дня" to 0
        }
    }
    val text = raw.toString().trim()
    if (text.isEmpty()) return "Без дня" to 0
    text.toIntOrNull()?.let { dayId ->
        numToDay[dayId]?.let { return it to dayId }
        dbDayIdToName[dayId]?.let { name -> return name to (dayToNum[name] ?: dayId) }
    }
    val key = normalizeDayKey(text, 0)
    return key to (dayToNum[key] ?: 0)
}

private fun isActiveEmployee(item: JSONObject): Boolean {
    if (!item.has("is_active") || item.isNull("is_active")) return true
    val raw = item.opt("is_active")
    return when (raw) {
        is Number -> raw.toInt() != 0
        is Boolean -> raw
        else -> item.optString("is_active", "1") != "0"
    }
}

private fun isTeacherEmployee(item: JSONObject): Boolean {
    val positionId = readNumericPositionId(item)
    if (positionId == 2) return true
    val positionText = listOf(
        item.optString("position_name", ""),
        item.optString("position", ""),
        item.optString("post", ""),
        item.optString("job_title", ""),
        item.optString("role_name", "")
    ).firstOrNull { it.isNotBlank() }?.lowercase(java.util.Locale("ru")).orEmpty()
    return positionText.contains("настав")
}

private fun readNumericPositionId(item: JSONObject): Int? {
    val raw = when {
        item.has("position_id") && !item.isNull("position_id") -> item.opt("position_id")
        item.has("id_position") && !item.isNull("id_position") -> item.opt("id_position")
        item.has("position") && !item.isNull("position") -> item.opt("position")
        item.has("id_posts") && !item.isNull("id_posts") -> item.opt("id_posts")
        item.has("post") && !item.isNull("post") -> item.opt("post")
        else -> null
    } ?: return null
    return when (raw) {
        is Number -> raw.toInt().takeIf { it > 0 }
        else -> raw.toString().trim().toIntOrNull()?.takeIf { it > 0 }
    }
}

private fun parseEmployeeOption(item: JSONObject): ScheduleOption? {
    val id = item.optInt("id_employees", item.optInt("id", item.optInt("employee_id", 0)))
    if (id <= 0) return null
    val name = employeeDisplayName(item)
    if (name.isBlank() || name == "—") return null
    return ScheduleOption(id = id, name = name)
}

private fun employeeDisplayName(item: JSONObject): String {
    val fromParts = listOf(
        item.optString("second_name", ""),
        item.optString("first_name", ""),
        item.optString("patronymic", "")
    ).filter { it.isNotBlank() }.joinToString(" ")
    if (fromParts.isNotBlank()) return fromParts
    return item.optString("name", item.optString("full_name", item.optString("fio", ""))).trim()
}

private fun formatTimeForApi(time: String): String {
    val trimmed = time.trim()
    if (trimmed.isEmpty()) return ""
    return if (Regex("""^\d{2}:\d{2}$""").matches(trimmed)) "$trimmed:00" else trimmed
}

fun dayOptions(): List<Pair<Int, String>> {
    return dayOrder.mapIndexed { index, name -> (index + 1) to name.replaceFirstChar { it.titlecase() } }
}
