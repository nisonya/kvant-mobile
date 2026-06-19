package com.example.kvantroium.features.students

import com.example.kvantroium.api.ApiClient
import com.example.kvantroium.api.ApiPaths
import com.example.kvantroium.features.events.formatEventDateInput
import com.example.kvantroium.features.groups.GroupItem
import org.json.JSONArray
import org.json.JSONObject

data class StudentProfile(
    val id: Int,
    val surname: String,
    val name: String,
    val patronymic: String,
    val birthDay: String,
    val navigator: Boolean,
    val parentSurname: String,
    val parentName: String,
    val parentPatronymic: String,
    val email: String,
    val phone: String,
    val activeGroupCount: Int = 0
) {
    val fullName: String
        get() = listOf(surname, name, patronymic).filter { it.isNotBlank() }.joinToString(" ")

    val parentFullName: String
        get() = listOf(parentSurname, parentName, parentPatronymic).filter { it.isNotBlank() }.joinToString(" ")

    val birthDayDisplay: String
        get() = formatEventDateInput(birthDay).ifBlank { birthDay }

    val navigatorLabel: String
        get() = if (navigator) "Да" else "Нет"
}

fun filterStudents(students: List<StudentProfile>, query: String): List<StudentProfile> {
    val trimmed = query.trim().lowercase()
    val filtered = if (trimmed.isEmpty()) {
        students
    } else {
        students.filter { student ->
            student.fullName.lowercase().contains(trimmed) ||
                student.phone.lowercase().contains(trimmed) ||
                student.email.lowercase().contains(trimmed) ||
                student.parentFullName.lowercase().contains(trimmed) ||
                student.surname.lowercase().contains(trimmed)
        }
    }
    return if (trimmed.isEmpty()) {
        filtered.sortedBy { it.fullName.lowercase() }
    } else {
        filtered.sortedWith(
            compareByDescending<StudentProfile> { it.activeGroupCount > 0 }
                .thenBy { it.fullName.lowercase() }
        )
    }
}

suspend fun loadAllStudents(apiClient: ApiClient): List<StudentProfile> {
    val response = apiClient.apiRequest("GET", ApiPaths.Students.SEARCH_NEW)
    val array = response.optJSONArray("data") ?: JSONArray()
    return parseStudentArray(array, includeActiveCount = true)
        .sortedBy { it.fullName.lowercase() }
}

suspend fun loadStudentsByGroup(apiClient: ApiClient, groupId: Int): List<StudentProfile> {
    val response = apiClient.apiRequest("GET", ApiPaths.Students.fullByGroup(groupId))
    val array = response.optJSONArray("data") ?: JSONArray()
    return parseStudentArray(array, includeActiveCount = false)
        .sortedBy { it.fullName.lowercase() }
}

suspend fun loadStudentGroups(apiClient: ApiClient, studentId: Int): List<GroupItem> {
    val response = apiClient.apiRequest("GET", ApiPaths.Students.groupsByStudent(studentId))
    val array = response.optJSONArray("data") ?: JSONArray()
    return buildList {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val id = item.optInt("id", item.optInt("idGroups", 0))
            val name = item.optString("name", "")
            if (id > 0 && name.isNotBlank()) add(GroupItem(id = id, name = name))
        }
    }.sortedBy { it.name }
}

private fun parseStudentArray(array: JSONArray, includeActiveCount: Boolean): List<StudentProfile> {
    return buildList {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            parseStudentProfile(item, includeActiveCount)?.let(::add)
        }
    }
}

private fun parseStudentProfile(item: JSONObject, includeActiveCount: Boolean): StudentProfile? {
    val id = item.optInt("id", item.optInt("idStudent", 0))
    if (id <= 0) return null
    val navigatorRaw = item.opt("navigator")
    val navigator = when (navigatorRaw) {
        is Number -> navigatorRaw.toInt() == 1
        is Boolean -> navigatorRaw
        else -> item.optString("navigator", "") == "1"
    }
    return StudentProfile(
        id = id,
        surname = item.optString("surname", ""),
        name = item.optString("name", ""),
        patronymic = item.optString("patronymic", ""),
        birthDay = item.optString("birthDay", item.optString("birthday", "")),
        navigator = navigator,
        parentSurname = item.optString("parentSurname", ""),
        parentName = item.optString("parentName", ""),
        parentPatronymic = item.optString("parentPatronymic", ""),
        email = item.optString("email", item.optString("E-mail", "")),
        phone = item.optString("phone", ""),
        activeGroupCount = if (includeActiveCount) item.optInt("isActive", 0) else 0
    )
}
