package com.example.kvantroium.features.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.kvantroium.api.ApiClient
import com.example.kvantroium.storage.SessionStorage
import java.util.concurrent.TimeUnit

class EventNotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val storage = SessionStorage(applicationContext)
        if (!storage.getSession().isAuthorized) {
            return Result.success()
        }
        return runCatching {
            val apiClient = ApiClient(storage)
            EventNotificationChecker.runCheck(applicationContext, apiClient, storage)
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
    }
}

object EventNotificationScheduler {
    private const val PERIODIC_WORK_NAME = "event_notification_periodic"
    private const val IMMEDIATE_WORK_NAME = "event_notification_immediate"
    private const val PERIODIC_INTERVAL_MINUTES = 15L

    fun schedule(context: Context) {
        val appContext = context.applicationContext
        val constraints = networkConstraints()
        val periodic = PeriodicWorkRequestBuilder<EventNotificationWorker>(
            PERIODIC_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(PERIODIC_WORK_NAME)
            .build()
        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodic
        )
        runImmediate(appContext)
    }

    fun runImmediate(context: Context) {
        val request = OneTimeWorkRequestBuilder<EventNotificationWorker>()
            .setConstraints(networkConstraints())
            .addTag(IMMEDIATE_WORK_NAME)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context) {
        val wm = WorkManager.getInstance(context.applicationContext)
        wm.cancelAllWorkByTag(IMMEDIATE_WORK_NAME)
        wm.cancelAllWorkByTag(PERIODIC_WORK_NAME)
    }

    private fun networkConstraints(): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
}
