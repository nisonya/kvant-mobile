package com.example.kvantroium.features.documents

import com.example.kvantroium.api.ApiClient
import com.example.kvantroium.api.ApiPaths
import org.json.JSONArray
import org.json.JSONObject

data class DocumentItem(
    val id: Int,
    val name: String,
    val link: String
)

data class DocumentDraft(
    val id: Int? = null,
    val name: String,
    val link: String
)

suspend fun loadDocuments(apiClient: ApiClient): List<DocumentItem> {
    val response = apiClient.apiRequest("GET", ApiPaths.Reference.DOCS)
    val array = response.optJSONArray("data") ?: JSONArray()
    return buildList {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val id = item.optInt("id", 0)
            if (id <= 0) continue
            add(
                DocumentItem(
                    id = id,
                    name = item.optString("name", ""),
                    link = item.optString("link", "")
                )
            )
        }
    }
}

suspend fun createDocument(apiClient: ApiClient, name: String, link: String): Int {
    val response = apiClient.apiRequest(
        method = "POST",
        path = ApiPaths.Reference.DOCS,
        body = JSONObject()
            .put("name", name)
            .put("link", link)
    )
    val data = response.optJSONObject("data") ?: response
    return data.optInt("id", 0)
}

suspend fun updateDocument(apiClient: ApiClient, id: Int, name: String, link: String) {
    apiClient.apiRequest(
        method = "PUT",
        path = ApiPaths.Reference.docById(id),
        body = JSONObject()
            .put("name", name)
            .put("link", link)
    )
}

suspend fun deleteDocument(apiClient: ApiClient, id: Int) {
    apiClient.apiRequest("DELETE", ApiPaths.Reference.docById(id))
}

suspend fun saveDocumentDrafts(
    apiClient: ApiClient,
    original: List<DocumentItem>,
    drafts: List<DocumentDraft>
) {
    val originalById = original.associateBy { it.id }
    val draftIds = drafts.mapNotNull { it.id }.toSet()

    original.filter { it.id !in draftIds }.forEach { deleteDocument(apiClient, it.id) }

    drafts.forEach { draft ->
        val trimmedName = draft.name.trim()
        val trimmedLink = draft.link.trim()
        if (trimmedName.isEmpty() && trimmedLink.isEmpty()) return@forEach

        if (draft.id == null) {
            if (trimmedName.isNotEmpty() && trimmedLink.isNotEmpty()) {
                createDocument(apiClient, trimmedName, trimmedLink)
            }
        } else {
            val previous = originalById[draft.id]
            if (previous == null ||
                previous.name != trimmedName ||
                previous.link != trimmedLink
            ) {
                updateDocument(apiClient, draft.id, trimmedName, trimmedLink)
            }
        }
    }
}
