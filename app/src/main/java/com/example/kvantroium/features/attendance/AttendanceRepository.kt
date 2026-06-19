package com.example.kvantroium.features.attendance

import com.example.kvantroium.api.ApiClient
import com.example.kvantroium.api.ApiPaths
import com.example.kvantroium.features.students.loadStudentsByGroup
import org.json.JSONArray
import org.json.JSONObject

data class AttendanceRecord(
    val name: String,
    val dateOfLesson: String,
    val presence: Int
)

data class AttendanceByDateRecord(
    val studentId: Int,
    val name: String,
    val dateOfLesson: String,
    val presence: Int
)

data class AttendanceStudentRow(
    val name: String,
    val byDate: Map<String, String>,
    val percent: Int
)

data class AttendancePivot(
    val dates: List<String>,
    val students: List<AttendanceStudentRow>
)

data class AttendanceEditRow(
    val studentId: Int,
    val name: String,
    val present: Boolean
)

fun pivotAttendance(records: List<AttendanceRecord>): AttendancePivot {
    val dateSet = linkedSetOf<String>()
    val studentMap = linkedMapOf<String, MutableMap<String, String>>()
    records.forEach { row ->
        val name = row.name.trim().ifBlank { "—" }
        val date = row.dateOfLesson.trim()
        if (date.isEmpty()) return@forEach
        dateSet.add(date)
        val byDate = studentMap.getOrPut(name) { mutableMapOf() }
        byDate[date] = if (row.presence == 1) "✓" else "—"
    }
    val dates = dateSet.sorted()
    val students = studentMap.keys
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
        .map { name ->
            val byDate = studentMap[name].orEmpty()
            val presentCount = dates.count { byDate[it] == "✓" }
            val percent = if (dates.isEmpty()) 0 else presentCount * 100 / dates.size
            AttendanceStudentRow(name = name, byDate = byDate, percent = percent)
        }
    return AttendancePivot(dates = dates, students = students)
}

suspend fun loadAttendanceByGroup(apiClient: ApiClient, groupId: Int): List<AttendanceRecord> {
    if (groupId <= 0) return emptyList()
    val response = apiClient.apiRequest("GET", ApiPaths.Attendance.byGroup(groupId))
    val array = response.optJSONArray("data") ?: JSONArray()
    return parseAttendanceArray(array)
}

suspend fun loadAttendanceByGroupDateNew(
    apiClient: ApiClient,
    groupId: Int,
    dateIso: String
): List<AttendanceByDateRecord> {
    if (groupId <= 0 || dateIso.isBlank()) return emptyList()
    val response = apiClient.apiRequest(
        method = "PUT",
        path = ApiPaths.Attendance.BY_GROUP_DATE_NEW,
        body = JSONObject()
            .put("group_id", groupId)
            .put("date", dateIso)
    )
    val array = response.optJSONArray("data") ?: JSONArray()
    return buildList {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val studentId = item.optInt(
                "id_student",
                item.optInt("idStudent", item.optInt("student_id", item.optInt("id", 0)))
            )
            if (studentId <= 0) continue
            add(
                AttendanceByDateRecord(
                    studentId = studentId,
                    name = item.optString("name", "—"),
                    dateOfLesson = item.optString("date_of_lesson", dateIso),
                    presence = item.optInt("presence", 0)
                )
            )
        }
    }
}

suspend fun loadAttendanceEditRows(
    apiClient: ApiClient,
    groupId: Int,
    dateIso: String
): List<AttendanceEditRow> {
    val students = loadStudentsByGroup(apiClient, groupId)
    val existing = loadAttendanceByGroupDateNew(apiClient, groupId, dateIso)
    val presenceById = existing.associate { it.studentId to (it.presence == 1) }
    return students.map { student ->
        AttendanceEditRow(
            studentId = student.id,
            name = student.fullName.ifBlank { "—" },
            present = presenceById[student.id] ?: false
        )
    }
}

suspend fun saveAttendance(
    apiClient: ApiClient,
    groupId: Int,
    dateIso: String,
    rows: List<AttendanceEditRow>
) {
    rows.forEach { row ->
        apiClient.apiRequest(
            method = "POST",
            path = ApiPaths.Attendance.ROOT,
            body = JSONObject()
                .put("student_id", row.studentId)
                .put("group_id", groupId)
                .put("date_of_lesson", dateIso)
                .put("presence", if (row.present) 1 else 0)
        )
    }
}

private fun parseAttendanceArray(array: JSONArray): List<AttendanceRecord> {
    return buildList {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val name = item.optString("name", "—").trim().ifBlank { "—" }
            val date = item.optString("date_of_lesson", "").trim()
            if (date.isEmpty()) continue
            add(
                AttendanceRecord(
                    name = name,
                    dateOfLesson = date,
                    presence = item.optInt("presence", 0)
                )
            )
        }
    }
}
