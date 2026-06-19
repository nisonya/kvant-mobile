package com.example.kvantroium.storage

import android.content.Context
import android.content.SharedPreferences

class UpdatePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun shouldCheckNow(minIntervalMs: Long = DEFAULT_INTERVAL_MS): Boolean {
        val lastCheck = prefs.getLong(KEY_LAST_CHECK_MS, 0L)
        if (lastCheck <= 0L) return true
        return System.currentTimeMillis() - lastCheck >= minIntervalMs
    }

    fun markChecked() {
        prefs.edit().putLong(KEY_LAST_CHECK_MS, System.currentTimeMillis()).apply()
    }

    fun lastNotifiedVersionCode(): Int = prefs.getInt(KEY_LAST_NOTIFIED_CODE, 0)

    fun markNotified(versionCode: Int) {
        prefs.edit().putInt(KEY_LAST_NOTIFIED_CODE, versionCode).apply()
    }

    private companion object {
        const val PREFS_NAME = "kvantroium_updates"
        const val KEY_LAST_CHECK_MS = "last_update_check_ms"
        const val KEY_LAST_NOTIFIED_CODE = "last_notified_version_code"
        const val DEFAULT_INTERVAL_MS = 60L * 60L * 1000L
    }
}
