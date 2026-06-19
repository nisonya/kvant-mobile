package com.example.kvantroium.features.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.kvantroium.MainActivity
import com.example.kvantroium.R
import com.example.kvantroium.features.events.EventKind

object EventNotificationHelper {
    const val CHANNEL_ID = "event_reminders"
    private const val GROUP_KEY = "kvantroium_event_reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_events),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_events_desc)
        }
        manager.createNotificationChannel(channel)
    }

    fun showReminder(context: Context, event: ReminderEvent, dedupKey: String): Boolean {
        if (!canPostNotifications(context)) return false
        ensureChannel(context)
        val notificationManager = NotificationManagerCompat.from(context)
        val notificationId = NotificationDedup.stableNotificationId(dedupKey)
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_EVENT
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_EVENT_ID, event.id)
            putExtra(EXTRA_EVENT_KIND, event.kind.name)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            eventPendingIntentRequestCode(event),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val largeIcon = BitmapFactory.decodeResource(context.resources, R.drawable.logo)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo)
            .setLargeIcon(largeIcon)
            .setContentTitle(reminderEntryTitle(event))
            .setContentText(reminderEntryBody(event))
            .setStyle(NotificationCompat.BigTextStyle().bigText(reminderEntryBody(event)))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY)
            .build()
        notificationManager.notify(notificationId, notification)
        return true
    }

    fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    const val ACTION_OPEN_EVENT = "com.example.kvantroium.action.OPEN_EVENT"
    const val EXTRA_EVENT_ID = "event_id"
    const val EXTRA_EVENT_KIND = "event_kind"

    fun eventPendingIntentRequestCode(event: ReminderEvent): Int =
        event.kind.ordinal * 100_000 + event.id

    fun parseEventOpenIntent(intent: Intent?): Pair<Int, EventKind>? {
        if (intent == null) return null
        val eventId = intent.getIntExtra(EXTRA_EVENT_ID, 0)
        if (eventId <= 0) return null
        val kind = runCatching {
            EventKind.valueOf(intent.getStringExtra(EXTRA_EVENT_KIND).orEmpty())
        }.getOrDefault(EventKind.ORG)
        return eventId to kind
    }
}
