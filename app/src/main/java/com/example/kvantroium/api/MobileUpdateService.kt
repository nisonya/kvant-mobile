package com.example.kvantroium.api

import com.example.kvantroium.ui.util.normalizeExternalUrl
import java.net.HttpURLConnection

data class MobileUpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val apkUrl: String,
    val apkName: String,
    val required: Boolean,
    val notes: String
) {
    fun isNewerThan(currentVersionCode: Int): Boolean = versionCode > currentVersionCode
}

class MobileUpdateService(private val apiClient: ApiClient) {
    suspend fun checkLatest(): MobileUpdateInfo? {
        return runCatching {
            val response = apiClient.publicRequest("GET", ApiPaths.MobileUpdates.ANDROID_LATEST)
            parseUpdateInfo(response)
        }.getOrElse { error ->
            if (error is ApiException && error.status == HttpURLConnection.HTTP_NOT_FOUND) {
                throw ApiException("Обновления на сервере не настроены (нет latest.json)", error.status)
            }
            throw error
        }
    }

    fun resolveApkDownloadUrl(serverUrl: String, info: MobileUpdateInfo): String {
        val normalizedServer = serverUrl.trim().trimEnd('/')
        val rawUrl = info.apkUrl.trim()
        if (rawUrl.startsWith("http://", ignoreCase = true) ||
            rawUrl.startsWith("https://", ignoreCase = true)
        ) {
            return normalizeExternalUrl(rawUrl).orEmpty().ifBlank { buildFallbackApkUrl(normalizedServer, info) }
        }
        if (rawUrl.startsWith("/")) return normalizedServer + rawUrl
        return buildFallbackApkUrl(normalizedServer, info)
    }

    private fun buildFallbackApkUrl(serverUrl: String, info: MobileUpdateInfo): String {
        val apkName = info.apkName.trim().removeSuffix(".apk")
        return "$serverUrl/mobile-updates/android/${apkName}.apk"
    }

    private fun parseUpdateInfo(response: org.json.JSONObject): MobileUpdateInfo? {
        val data = response.optJSONObject("data") ?: response
        val versionCode = data.optInt("versionCode", 0)
        if (versionCode <= 0) return null
        val apkName = data.optString("apkName", "")
            .ifBlank { data.optString("fileName", "") }
            .ifBlank { data.optString("apk", "") }
        return MobileUpdateInfo(
            versionName = data.optString("versionName", ""),
            versionCode = versionCode,
            apkUrl = data.optString("apkUrl", ""),
            apkName = apkName,
            required = data.optBoolean("required", false),
            notes = data.optString("notes", "")
        )
    }
}
