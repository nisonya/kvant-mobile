package com.example.kvantroium.api

import android.content.Context
import com.example.kvantroium.storage.SessionStorage
import com.example.kvantroium.storage.UpdatePreferences
import java.net.HttpURLConnection

data class AppUpdateState(
    val info: MobileUpdateInfo?,
    val message: String,
    val isError: Boolean = false,
    val downloadedApk: java.io.File? = null,
    val isDownloading: Boolean = false
)

class AppUpdateCoordinator(
    private val context: Context,
    private val apiClient: ApiClient,
    private val updateService: MobileUpdateService,
    private val apkUpdateManager: ApkUpdateManager = ApkUpdateManager(context)
) {
    private val preferences = UpdatePreferences(context)

    suspend fun checkForUpdates(force: Boolean = false): AppUpdateState {
        if (!force && !preferences.shouldCheckNow()) {
            return AppUpdateState(
                info = null,
                message = "Нет обновлений"
            )
        }

        return runCatching {
            val update = updateService.checkLatest()
            preferences.markChecked()
            when {
                update == null || !update.isNewerThan(com.example.kvantroium.BuildConfig.VERSION_CODE) ->
                    AppUpdateState(
                        info = null,
                        message = "Нет обновлений"
                    )
                else -> AppUpdateState(
                    info = update,
                    message = ""
                )
            }
        }.getOrElse { error ->
            preferences.markChecked()
            AppUpdateState(
                info = null,
                message = error.userUpdateMessage(),
                isError = true
            )
        }
    }

    suspend fun downloadUpdate(info: MobileUpdateInfo): AppUpdateState {
        val serverUrl = SessionStorage(context).getServerUrl()
        val apkUrl = updateService.resolveApkDownloadUrl(serverUrl, info)
        val fileName = info.apkFileName()
        return runCatching {
            val file = apkUpdateManager.downloadApk(apkUrl, fileName)
            AppUpdateState(
                info = info,
                message = "",
                downloadedApk = file
            )
        }.getOrElse { error ->
            AppUpdateState(
                info = info,
                message = error.userUpdateMessage(),
                isError = true
            )
        }
    }

    fun installDownloaded(apkFile: java.io.File): String? {
        if (!apkUpdateManager.canInstallPackages()) {
            apkUpdateManager.openInstallPermissionSettings()
            return "Разрешите установку из неизвестных источников для этого приложения"
        }
        apkUpdateManager.installApk(apkFile)
        return null
    }
}

private fun Throwable.userUpdateMessage(): String {
    return when (this) {
        is ApiException -> when (status) {
            HttpURLConnection.HTTP_NOT_FOUND ->
                message?.takeIf { it.isNotBlank() } ?: "Обновления на сервере не настроены"
            else -> message ?: "Ошибка проверки обновлений"
        }
        else -> message ?: "Ошибка проверки обновлений"
    }
}

private fun MobileUpdateInfo.apkFileName(): String {
    val fromUrl = apkUrl.substringAfterLast('/').substringBefore('?').trim()
    if (fromUrl.endsWith(".apk", ignoreCase = true)) return fromUrl
    val fromName = apkName.trim()
    if (fromName.endsWith(".apk", ignoreCase = true)) return fromName
    if (fromName.isNotBlank()) return "$fromName.apk"
    return "kvantroium-update.apk"
}
