package com.example.kvantroium.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.kvantroium.ui.theme.GenderAccent
import org.json.JSONObject

data class UserProfile(
    val id: Int? = null,
    val profileId: Int? = null,
    val employeeId: Int? = null,
    val login: String = "",
    val accessLevel: Int? = null,
    val rawJson: String = ""
)

data class UserSession(
    val serverUrl: String = "",
    val accessToken: String = "",
    val refreshToken: String = "",
    val user: UserProfile? = null
) {
    val isAuthorized: Boolean
        get() = serverUrl.isNotBlank() && accessToken.isNotBlank() && user != null
}

class SessionStorage(context: Context) {
    private val prefs: SharedPreferences = createEncryptedPrefs(context.applicationContext)
    private val _sessionRevision = MutableStateFlow(0)
    val sessionRevision: StateFlow<Int> = _sessionRevision.asStateFlow()

    fun getSession(): UserSession {
        val userJson = prefs.getString(KEY_USER, null).orEmpty()
        return UserSession(
            serverUrl = prefs.getString(KEY_SERVER_URL, null).orEmpty(),
            accessToken = prefs.getString(KEY_ACCESS_TOKEN, null).orEmpty(),
            refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null).orEmpty(),
            user = parseUser(userJson)
        )
    }

    fun getServerUrl(): String = prefs.getString(KEY_SERVER_URL, null).orEmpty()

    fun saveServerUrl(serverUrl: String) {
        prefs.edit {
            putString(KEY_SERVER_URL, normalizeServerUrl(serverUrl))
        }
    }

    fun saveSession(
        serverUrl: String,
        accessToken: String,
        refreshToken: String,
        userJson: JSONObject?
    ) {
        prefs.edit {
            putString(KEY_SERVER_URL, normalizeServerUrl(serverUrl))
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putString(KEY_USER, userJson?.toString().orEmpty())
        }
        notifySessionChanged()
    }

    fun updateTokens(
        accessToken: String?,
        refreshToken: String?,
        userJson: JSONObject? = null
    ) {
        prefs.edit {
            if (!accessToken.isNullOrBlank()) putString(KEY_ACCESS_TOKEN, accessToken)
            if (!refreshToken.isNullOrBlank()) putString(KEY_REFRESH_TOKEN, refreshToken)
            if (userJson != null) putString(KEY_USER, userJson.toString())
        }
        notifySessionChanged()
    }

    fun getGenderAccent(): GenderAccent {
        val raw = prefs.getString(KEY_GENDER_ACCENT, null).orEmpty()
        return runCatching { GenderAccent.valueOf(raw) }.getOrDefault(GenderAccent.Unknown)
    }

    fun saveGenderAccent(accent: GenderAccent) {
        prefs.edit { putString(KEY_GENDER_ACCENT, accent.name) }
        notifySessionChanged()
    }

    fun clearSession(keepServerUrl: Boolean = true) {
        val serverUrl = getServerUrl()
        prefs.edit {
            clear()
            if (keepServerUrl && serverUrl.isNotBlank()) putString(KEY_SERVER_URL, serverUrl)
        }
        notifySessionChanged()
    }

    private fun notifySessionChanged() {
        _sessionRevision.value++
    }

    private fun normalizeServerUrl(raw: String): String {
        return raw.trim().trimEnd('/')
    }

    private fun parseUser(userJson: String): UserProfile? {
        if (userJson.isBlank()) return null
        return runCatching {
            val json = JSONObject(userJson)
            val profileId = json.optNullableInt("profile_id")
            val employeeId = resolveEmployeeId(json)
            UserProfile(
                id = profileId ?: json.optNullableInt("id"),
                profileId = profileId,
                employeeId = employeeId,
                login = json.optString("login", ""),
                accessLevel = json.optNullableInt("accessLevel") ?: json.optNullableInt("access_level_id"),
                rawJson = userJson
            )
        }.getOrNull()
    }

    private fun resolveEmployeeId(json: JSONObject): Int? {
        listOf("employee_id", "employeeId", "id_employees").forEach { key ->
            json.optNullableInt(key)?.takeIf { it > 0 }?.let { return it }
        }
        val profileId = json.optNullableInt("profile_id")
        val rawId = json.optNullableInt("id")?.takeIf { it > 0 } ?: return null
        if (profileId != null && rawId == profileId) return null
        return rawId
    }

    private fun JSONObject.optNullableInt(name: String): Int? {
        if (!has(name) || isNull(name)) return null
        return optInt(name)
    }

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private companion object {
        const val PREFS_NAME = "kvantroium_session"
        const val KEY_SERVER_URL = "server_url"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_USER = "user"
        const val KEY_GENDER_ACCENT = "gender_accent"
    }
}
