package com.example.kvantroium.features.events

import com.example.kvantroium.api.ApiClient
import com.example.kvantroium.api.ApiPaths
import com.example.kvantroium.api.MultipartFile
import org.json.JSONArray
import java.util.Locale

enum class EventDocPreviewKind {
    IMAGE,
    PDF,
    OTHER
}

data class EventDocumentItem(
    val id: Int,
    val eventId: Int,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val sortOrder: Int
)

fun eventDocPreviewKind(mimeType: String, fileName: String): EventDocPreviewKind {
    val mime = mimeType.trim().lowercase(Locale.getDefault())
    if (mime.startsWith("image/")) return EventDocPreviewKind.IMAGE
    if (mime == "application/pdf" || fileName.lowercase(Locale.getDefault()).endsWith(".pdf")) {
        return EventDocPreviewKind.PDF
    }
    return EventDocPreviewKind.OTHER
}

fun formatEventDocumentSize(bytes: Long?): String {
    if (bytes == null || bytes <= 0L) return "—"
    if (bytes < 1024) return "$bytes Б"
    if (bytes < 1024 * 1024) {
        return String.format(Locale.getDefault(), "%.1f КБ", bytes / 1024.0)
    }
    return String.format(Locale.getDefault(), "%.1f МБ", bytes / 1024.0 / 1024.0)
}

suspend fun loadEventDocuments(
    apiClient: ApiClient,
    kind: EventKind,
    eventId: Int
): List<EventDocumentItem> {
    val response = apiClient.apiRequest("GET", ApiPaths.Events.eventDocuments(kind == EventKind.ORG, eventId))
    val array = response.optJSONArray("data") ?: JSONArray()
    return buildList {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val id = item.optInt("id", 0)
            if (id <= 0) continue
            add(
                EventDocumentItem(
                    id = id,
                    eventId = item.optInt("id_event", eventId),
                    fileName = item.optString("original_filename", "файл"),
                    mimeType = item.optString("mime_type", ""),
                    sizeBytes = item.optLong("size_bytes", 0L),
                    sortOrder = item.optInt("sort_order", 0)
                )
            )
        }
    }.sortedWith(compareBy({ it.sortOrder }, { it.id }))
}

suspend fun downloadEventDocument(
    apiClient: ApiClient,
    kind: EventKind,
    documentId: Int
): ByteArray {
    return apiClient.apiFetchBlob(ApiPaths.Events.eventDocumentDownload(kind == EventKind.ORG, documentId))
}

suspend fun uploadEventDocument(
    apiClient: ApiClient,
    kind: EventKind,
    eventId: Int,
    fileName: String,
    mimeType: String,
    bytes: ByteArray
) {
    apiClient.apiUploadMultipart(
        path = ApiPaths.Events.eventDocuments(kind == EventKind.ORG, eventId),
        fields = emptyMap(),
        files = listOf(
            MultipartFile(
                fieldName = "file",
                fileName = fileName,
                mimeType = mimeType.ifBlank { "application/octet-stream" },
                bytes = bytes
            )
        )
    )
}

suspend fun deleteEventDocument(
    apiClient: ApiClient,
    kind: EventKind,
    documentId: Int
) {
    apiClient.apiRequest("DELETE", ApiPaths.Events.eventDocumentDelete(kind == EventKind.ORG, documentId))
}
