package com.example.kvantroium.api

import android.util.Base64
import com.example.kvantroium.storage.SessionStorage
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class ApiException(message: String, val status: Int? = null) : Exception(message)

data class MultipartFile(
    val fieldName: String,
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray
)

class ApiClient(private val storage: SessionStorage) {
    suspend fun login(serverUrl: String, login: String, password: String): JSONObject {
        return withContext(Dispatchers.IO) {
            storage.saveServerUrl(serverUrl)
            val response = executeJson(
                method = "POST",
                path = ApiPaths.Auth.LOGIN,
                body = JSONObject()
                    .put("login", login)
                    .put("password", password),
                useAuth = false
            ).ensureOk()
            val payload = response.json.optJSONObject("data") ?: response.json
            val user = payload.optJSONObject("user")
            val accessToken = response.extractToken("access_token")
                ?: payload.optString("accessToken", "")
            val refreshToken = response.extractToken("refresh_token")
                ?: payload.optString("refreshToken", "")

            if (accessToken.isBlank() || user == null) {
                throw ApiException("Сервер не вернул данные сессии")
            }

            storage.saveSession(
                serverUrl = serverUrl,
                accessToken = accessToken,
                refreshToken = refreshToken,
                userJson = user
            )
            response.json
        }
    }

    suspend fun logout() {
        withContext(Dispatchers.IO) {
            runCatching {
                apiRequest("POST", ApiPaths.Auth.LOGOUT, null)
            }
            storage.clearSession()
        }
    }

    suspend fun apiRequest(method: String, path: String, body: JSONObject? = null): JSONObject {
        return withContext(Dispatchers.IO) {
            ensureFreshAccessToken()
            val response = requestWithRefresh {
                executeJson(method = method, path = path, body = body, useAuth = true)
            }
            response.json
        }
    }

    suspend fun publicRequest(method: String, path: String, body: JSONObject? = null): JSONObject {
        return withContext(Dispatchers.IO) {
            executeJson(method = method, path = path, body = body, useAuth = false)
                .ensureOk()
                .json
        }
    }

    suspend fun apiFetchBlob(path: String): ByteArray {
        return withContext(Dispatchers.IO) {
            ensureFreshAccessToken()
            val response = requestWithRefresh {
                executeBytes(method = "GET", path = path)
            }
            response.bytes
        }
    }

    suspend fun apiUploadMultipart(
        path: String,
        fields: Map<String, String>,
        files: List<MultipartFile>
    ): JSONObject {
        return withContext(Dispatchers.IO) {
            ensureFreshAccessToken()
            val response = requestWithRefresh {
                executeMultipart(path = path, fields = fields, files = files)
            }
            response.json
        }
    }

    /** Обновляет access-токен и профиль с сервера (актуальный accessLevel и т.д.). */
    suspend fun refreshSessionFromServer(): Boolean {
        return withContext(Dispatchers.IO) {
            refreshAccessToken()
        }
    }

    private suspend fun <T : ApiCallResponse> requestWithRefresh(block: suspend () -> T): T {
        val first = block()
        if (first.status != HttpURLConnection.HTTP_UNAUTHORIZED) return first.ensureOk()

        if (!refreshAccessToken()) {
            storage.clearSession()
            throw ApiException("Сессия истекла", first.status)
        }

        return block().ensureOk()
    }

    private suspend fun ensureFreshAccessToken() {
        val accessToken = storage.getSession().accessToken
        if (accessToken.isBlank()) return

        val expiresAt = jwtExpiresAtMillis(accessToken) ?: return
        val shouldRefresh = expiresAt <= System.currentTimeMillis() + ACCESS_REFRESH_SKEW_MS
        if (!shouldRefresh) return

        if (!refreshAccessToken()) {
            storage.clearSession()
            throw ApiException("Сессия истекла")
        }
    }

    private suspend fun refreshAccessToken(): Boolean {
        val refreshToken = storage.getSession().refreshToken
        if (refreshToken.isBlank()) return false

        val response = executeJson(
            method = "POST",
            path = ApiPaths.Auth.REFRESH,
            body = JSONObject().put("refreshToken", refreshToken),
            useAuth = false
        )
        if (!response.isOk) return false

        val payload = response.json.optJSONObject("data") ?: response.json
        val accessToken = response.extractToken("access_token")
            ?: payload.optString("accessToken", "")
        val nextRefreshToken = response.extractToken("refresh_token")
            ?: payload.optString("refreshToken", "")

        if (accessToken.isBlank()) return false
        val userJson = payload.optJSONObject("user")
        storage.updateTokens(accessToken, nextRefreshToken, userJson)
        return true
    }

    private fun jwtExpiresAtMillis(token: String): Long? {
        val payload = token.split('.').getOrNull(1) ?: return null
        return runCatching {
            val decoded = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            val json = JSONObject(String(decoded, Charsets.UTF_8))
            json.optLong("exp", 0L).takeIf { it > 0L }?.let { TimeUnit.SECONDS.toMillis(it) }
        }.getOrNull()
    }

    private fun executeJson(
        method: String,
        path: String,
        body: JSONObject?,
        useAuth: Boolean
    ): JsonResponse {
        val connection = openConnection(path, method, useAuth)
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        if (body != null) {
            connection.doOutput = true
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use {
                it.write(body.toString())
            }
        }

        val status = connection.responseCode
        val text = readResponseText(connection)
        val json = if (text.isBlank()) JSONObject() else runCatching { JSONObject(text) }.getOrDefault(JSONObject())
        return JsonResponse(status, json, connection.headerFields)
    }

    private fun executeBytes(method: String, path: String): BytesResponse {
        val connection = openConnection(path, method, useAuth = true)
        val status = connection.responseCode
        val bytes = readResponseBytes(connection)
        return BytesResponse(status, bytes, connection.headerFields)
    }

    private fun executeMultipart(
        path: String,
        fields: Map<String, String>,
        files: List<MultipartFile>
    ): JsonResponse {
        val boundary = "Kvantorium-${UUID.randomUUID()}"
        val connection = openConnection(path, "POST", useAuth = true)
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

        connection.outputStream.use { output ->
            fields.forEach { (name, value) ->
                output.write("--$boundary\r\n".toByteArray())
                output.write("Content-Disposition: form-data; name=\"$name\"\r\n\r\n".toByteArray())
                output.write(value.toByteArray(Charsets.UTF_8))
                output.write("\r\n".toByteArray())
            }
            files.forEach { file ->
                output.write("--$boundary\r\n".toByteArray())
                output.write(
                    "Content-Disposition: form-data; name=\"${file.fieldName}\"; filename=\"${file.fileName}\"\r\n"
                        .toByteArray()
                )
                output.write("Content-Type: ${file.mimeType}\r\n\r\n".toByteArray())
                output.write(file.bytes)
                output.write("\r\n".toByteArray())
            }
            output.write("--$boundary--\r\n".toByteArray())
        }

        val status = connection.responseCode
        val text = readResponseText(connection)
        val json = if (text.isBlank()) JSONObject() else runCatching { JSONObject(text) }.getOrDefault(JSONObject())
        return JsonResponse(status, json, connection.headerFields)
    }

    private fun openConnection(path: String, method: String, useAuth: Boolean): HttpURLConnection {
        val serverUrl = storage.getServerUrl()
        if (serverUrl.isBlank()) throw ApiException("Адрес сервера не настроен")

        val connection = URL(serverUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
        if (connection is HttpsURLConnection) {
            connection.sslSocketFactory = SelfSignedSsl.socketFactory
            connection.hostnameVerifier = SelfSignedSsl.hostnameVerifier
        }
        connection.requestMethod = method.uppercase()
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.setRequestProperty("Accept", "application/json")

        if (useAuth) {
            val token = storage.getSession().accessToken
            if (token.isNotBlank()) connection.setRequestProperty("Authorization", "Bearer $token")
        }

        return connection
    }

    private fun readResponseText(connection: HttpURLConnection): String {
        return readResponseBytes(connection).toString(Charsets.UTF_8)
    }

    private fun readResponseBytes(connection: HttpURLConnection): ByteArray {
        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream ?: connection.inputStream
        }

        return stream.use { input ->
            val output = ByteArrayOutputStream()
            input.copyTo(output)
            output.toByteArray()
        }
    }

    private interface ApiCallResponse {
        val status: Int
        val headers: Map<String, List<String>>
    }

    private data class JsonResponse(
        override val status: Int,
        val json: JSONObject,
        override val headers: Map<String, List<String>>
    ) : ApiCallResponse

    private data class BytesResponse(
        override val status: Int,
        val bytes: ByteArray,
        override val headers: Map<String, List<String>>
    ) : ApiCallResponse

    private val ApiCallResponse.isOk: Boolean
        get() = status in 200..299

    private fun <T : ApiCallResponse> T.ensureOk(): T {
        if (isOk) return this
        val message = if (this is JsonResponse) {
            json.optString("error", "Ошибка $status")
        } else {
            documentError(status)
        }
        throw ApiException(message, status)
    }

    private fun ApiCallResponse.extractToken(name: String): String? {
        val cookies = headers.entries
            .filter { it.key.equals("Set-Cookie", ignoreCase = true) }
            .flatMap { it.value }
        return cookies.firstNotNullOfOrNull { cookie ->
            val prefix = "$name="
            cookie.split(';')
                .firstOrNull { it.trim().startsWith(prefix) }
                ?.trim()
                ?.removePrefix(prefix)
                ?.takeIf { it.isNotBlank() }
        }
    }

    private fun documentError(status: Int): String {
        return when (status) {
            HttpURLConnection.HTTP_UNAVAILABLE -> "Сервис документов недоступен (проверьте настройки сервера)."
            HttpURLConnection.HTTP_NOT_FOUND -> "Файл или документ не найдены."
            HttpURLConnection.HTTP_BAD_REQUEST -> "Некорректный запрос к серверу."
            else -> "Ошибка $status"
        }
    }

    private companion object {
        const val TIMEOUT_MS = 20_000
        val ACCESS_REFRESH_SKEW_MS: Long = TimeUnit.MINUTES.toMillis(2)
    }
}
