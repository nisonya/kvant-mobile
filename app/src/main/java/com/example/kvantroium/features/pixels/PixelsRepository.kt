package com.example.kvantroium.features.pixels

import com.example.kvantroium.api.ApiClient
import com.example.kvantroium.api.ApiPaths
import com.example.kvantroium.features.attendance.loadAttendanceByGroup
import com.example.kvantroium.features.groups.GroupItem
import org.json.JSONArray

data class PixelAttendanceIndex(
    val byStudentId: Map<Int, Int> = emptyMap(),
    val byStudentName: Map<String, Int> = emptyMap()
)

suspend fun loadPixelsByGroup(apiClient: ApiClient, groupId: Int): List<PixelStudentRow> {
    if (groupId <= 0) return emptyList()
    val response = apiClient.apiRequest("GET", ApiPaths.Groups.pixelsByGroup(groupId))
    val array = response.optJSONArray("data") ?: JSONArray()
    return buildList {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            parsePixelStudentRow(item)?.let(::add)
        }
    }
}

suspend fun updatePixels(apiClient: ApiClient, row: PixelStudentRow) {
    apiClient.apiRequest(
        method = "PUT",
        path = ApiPaths.Groups.PIXELS_UPDATE,
        body = row.toUpdateJson()
    )
}

suspend fun clearAllPixels(apiClient: ApiClient) {
    apiClient.apiRequest("POST", ApiPaths.Groups.PIXELS_CLEAR_ALL)
}

suspend fun buildPixelAttendanceIndex(
    apiClient: ApiClient,
    groups: List<GroupItem>
): PixelAttendanceIndex {
    val byStudentId = mutableMapOf<Int, Int>()
    val byStudentName = mutableMapOf<String, Int>()
    groups.forEach { group ->
        runCatching {
            val records = loadAttendanceByGroup(apiClient, group.id)
            applyGroupAttendancePercents(records, byStudentId, byStudentName)
        }
    }
    return PixelAttendanceIndex(byStudentId = byStudentId, byStudentName = byStudentName)
}

fun applyAttendanceIndex(
    rows: List<PixelStudentRow>,
    index: PixelAttendanceIndex
): List<PixelStudentRow> {
    return rows.map { row ->
        val byId = index.byStudentId[row.studentId] ?: 0
        val byName = index.byStudentName[normalizeStudentNameKey(row.name)] ?: 0
        row.withAttendancePercent(maxOf(byId, byName))
    }
}

private fun applyGroupAttendancePercents(
    records: List<com.example.kvantroium.features.attendance.AttendanceRecord>,
    byStudentId: MutableMap<Int, Int>,
    byStudentName: MutableMap<String, Int>
) {
    val dates = linkedSetOf<String>()
    val presentCount = mutableMapOf<String, Int>()
    val names = mutableMapOf<String, String>()
    records.forEach { record ->
        val date = record.dateOfLesson.trim()
        if (date.isEmpty()) return@forEach
        dates.add(date)
        val name = record.name.trim().ifBlank { "—" }
        val key = "name:${normalizeStudentNameKey(name)}"
        names[key] = name
        if (record.presence == 1) {
            presentCount[key] = (presentCount[key] ?: 0) + 1
        } else if (!presentCount.containsKey(key)) {
            presentCount[key] = 0
        }
    }
    val totalDates = dates.size
    if (totalDates <= 0) return
    presentCount.forEach { (key, present) ->
        val percent = (present * 100 / totalDates).coerceIn(0, 100)
        val name = names[key].orEmpty()
        rememberBestAttendancePercent(studentId = null, name = name, percent = percent, byStudentId, byStudentName)
    }
}

private fun rememberBestAttendancePercent(
    studentId: Int?,
    name: String,
    percent: Int,
    byStudentId: MutableMap<Int, Int>,
    byStudentName: MutableMap<String, Int>
) {
    val bounded = percent.coerceIn(0, 100)
    if (studentId != null && studentId > 0) {
        byStudentId[studentId] = maxOf(byStudentId[studentId] ?: 0, bounded)
    }
    val nameKey = normalizeStudentNameKey(name)
    if (nameKey.isNotEmpty()) {
        byStudentName[nameKey] = maxOf(byStudentName[nameKey] ?: 0, bounded)
    }
}

fun normalizeStudentNameKey(name: String): String =
    name.trim().replace(Regex("\\s+"), " ").lowercase()
