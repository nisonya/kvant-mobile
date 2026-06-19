package com.example.kvantroium.api

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class ApkUpdateManager(private val context: Context) {

    suspend fun downloadApk(url: String, fileName: String): File = withContext(Dispatchers.IO) {
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val target = File(updatesDir, fileName)
        if (target.exists()) target.delete()

        val connection = URL(url).openConnection() as HttpURLConnection
        if (connection is HttpsURLConnection) {
            connection.sslSocketFactory = SelfSignedSsl.socketFactory
            connection.hostnameVerifier = SelfSignedSsl.hostnameVerifier
        }
        connection.connectTimeout = 20_000
        connection.readTimeout = 20_000
        connection.requestMethod = "GET"

        val status = connection.responseCode
        if (status !in 200..299) {
            throw ApiException("Не удалось скачать APK (ошибка $status)", status)
        }

        connection.inputStream.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        target
    }

    fun canInstallPackages(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        return context.packageManager.canRequestPackageInstalls()
    }

    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun installApk(apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }
}
