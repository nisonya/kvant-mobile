package com.example.kvantroium.features.profile

import com.example.kvantroium.api.ApiClient
import com.example.kvantroium.api.ApiPaths
import com.example.kvantroium.features.events.formatEventDateInput
import com.example.kvantroium.features.events.parseUiDateToIso
import com.example.kvantroium.storage.SessionStorage
import com.example.kvantroium.ui.theme.GenderAccent
import org.json.JSONObject

data class EmployeeDetails(
    val employeeId: Int,
    val firstName: String,
    val secondName: String,
    val patronymic: String,
    val fullName: String,
    val position: String,
    val positionId: Int,
    val dateOfBirth: String,
    val contact: String,
    val size: String,
    val education: String,
    val schedule: String,
    val gender: String
) {
    fun toEditDraft(): ProfileEditDraft = ProfileEditDraft(
        firstName = firstName,
        secondName = secondName,
        patronymic = patronymic,
        dateOfBirthUi = formatEventDateInput(dateOfBirth).ifBlank { dateOfBirth },
        contact = contact,
        size = size,
        education = education,
        gender = genderLabelForForm(gender),
        positionId = positionId
    )
}

data class ProfileEditDraft(
    val firstName: String = "",
    val secondName: String = "",
    val patronymic: String = "",
    val dateOfBirthUi: String = "",
    val contact: String = "",
    val size: String = "",
    val education: String = "",
    val gender: String = "",
    val positionId: Int = 0
) {
    val dateOfBirthIso: String
        get() = parseUiDateToIso(dateOfBirthUi).orEmpty()
}

suspend fun loadEmployeeDetails(apiClient: ApiClient, employeeId: Int?): EmployeeDetails? {
    if (employeeId == null || employeeId <= 0) return null
    return runCatching {
        val response = apiClient.apiRequest("GET", ApiPaths.Employees.byId(employeeId))
        val data = response.optJSONObject("data") ?: response
        parseEmployeeDetails(data)
    }.getOrElse { error ->
        if (error is com.example.kvantroium.api.ApiException &&
            error.status == java.net.HttpURLConnection.HTTP_NOT_FOUND
        ) {
            null
        } else {
            throw error
        }
    }
}

suspend fun updateEmployeeProfile(
    apiClient: ApiClient,
    employeeId: Int,
    draft: ProfileEditDraft
) {
    ProfileFieldLimits.validateProfileDraft(draft)?.let { error(it) }
    val body = JSONObject()
        .put("first_name", draft.firstName.trim())
        .put("second_name", draft.secondName.trim())
        .put("patronymic", draft.patronymic.trim().ifBlank { JSONObject.NULL })
        .put("date_of_birth", draft.dateOfBirthIso)
        .put("contact", draft.contact.trim().ifBlank { JSONObject.NULL })
        .put("size", draft.size.trim().ifBlank { JSONObject.NULL })
        .put("education", draft.education.trim().ifBlank { JSONObject.NULL })
        .put("gender", draft.gender.trim().ifBlank { JSONObject.NULL })
    if (draft.positionId > 0) {
        body.put("position", draft.positionId)
    }
    apiClient.apiRequest("PUT", ApiPaths.Employees.byId(employeeId), body)
}

suspend fun changePassword(
    apiClient: ApiClient,
    oldPassword: String,
    newPassword: String
) {
    apiClient.apiRequest(
        method = "POST",
        path = ApiPaths.Auth.CHANGE_PASSWORD,
        body = JSONObject()
            .put("old_password", oldPassword)
            .put("new_password", newPassword)
    )
}

suspend fun syncGenderAccentFromEmployee(apiClient: ApiClient, storage: SessionStorage) {
    val employeeId = storage.getSession().user?.employeeId ?: return
    val details = loadEmployeeDetails(apiClient, employeeId) ?: return
    storage.saveGenderAccent(GenderAccent.fromRaw(details.gender))
}

suspend fun loadEmployeeKpiUrl(apiClient: ApiClient, employeeId: Int?): String? {
    if (employeeId == null || employeeId <= 0) return null
    val response = apiClient.apiRequest("GET", ApiPaths.Employees.kpiById(employeeId))
    val data = response.optJSONObject("data") ?: response
    return parseKpiValue(data)
}

private fun parseEmployeeDetails(data: JSONObject): EmployeeDetails? {
    val employeeId = data.optInt("id_employees", data.optInt("id", 0))
    if (employeeId <= 0) return null
    val first = data.optString("first_name", "")
    val second = data.optString("second_name", "")
    val patronymic = data.optString("patronymic", "")
    val fullName = listOf(second, first, patronymic).filter { it.isNotBlank() }.joinToString(" ")
    return EmployeeDetails(
        employeeId = employeeId,
        firstName = first,
        secondName = second,
        patronymic = patronymic,
        fullName = fullName.ifBlank { "—" },
        position = data.optString("position_name", ""),
        positionId = data.optInt("position", data.optInt("id_position", 0)),
        dateOfBirth = data.optString("date_of_birth", ""),
        contact = data.optString("contact", ""),
        size = data.optString("size", ""),
        education = data.optString("education", ""),
        schedule = data.optString("schedule", ""),
        gender = data.optString("gender", "")
    )
}

private fun parseKpiValue(data: JSONObject): String? {
    val raw = when {
        data.has("KPI") && !data.isNull("KPI") -> data.opt("KPI")
        data.has("kpi") && !data.isNull("kpi") -> data.opt("kpi")
        else -> null
    } ?: return null
    val text = when (raw) {
        is Number -> if (raw.toDouble() == 0.0) return null else raw.toString()
        else -> raw.toString().trim()
    }
    if (text.isBlank() || text.equals("null", ignoreCase = true) || text == "0") return null
    return text
}
