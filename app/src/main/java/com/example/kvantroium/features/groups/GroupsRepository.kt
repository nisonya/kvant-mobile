package com.example.kvantroium.features.groups

import com.example.kvantroium.api.ApiClient
import com.example.kvantroium.api.ApiPaths
import org.json.JSONArray
import org.json.JSONObject

data class GroupItem(
    val id: Int,
    val name: String
)

suspend fun loadGroups(apiClient: ApiClient): List<GroupItem> {
    val response = apiClient.apiRequest("GET", ApiPaths.Groups.LIST)
    val array = response.optJSONArray("data") ?: JSONArray()
    return parseGroupArray(array)
}

suspend fun loadGroupsByTeacher(apiClient: ApiClient, teacherId: Int): List<GroupItem> {
    if (teacherId <= 0) return emptyList()
    val response = apiClient.apiRequest("GET", ApiPaths.Groups.byTeacher(teacherId))
    val array = response.optJSONArray("data") ?: JSONArray()
    return parseGroupArray(array)
}

private fun parseGroupArray(array: JSONArray): List<GroupItem> {
    return buildList {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val id = item.optInt("id", item.optInt("idGroups", 0))
            val name = item.optString("name", "")
            if (id > 0 && name.isNotBlank()) add(GroupItem(id = id, name = name))
        }
    }.sortedBy { it.name }
}

suspend fun createGroup(apiClient: ApiClient, name: String): Int {
    val response = apiClient.apiRequest(
        method = "POST",
        path = ApiPaths.Groups.LIST,
        body = JSONObject().put("name", name)
    )
    val data = response.optJSONObject("data") ?: response
    return data.optInt("id", 0)
}

suspend fun renameGroup(apiClient: ApiClient, groupId: Int, name: String) {
    apiClient.apiRequest(
        method = "PUT",
        path = ApiPaths.Groups.byId(groupId),
        body = JSONObject().put("name", name)
    )
}

suspend fun deleteGroup(apiClient: ApiClient, groupId: Int) {
    apiClient.apiRequest("DELETE", ApiPaths.Groups.byId(groupId))
}
