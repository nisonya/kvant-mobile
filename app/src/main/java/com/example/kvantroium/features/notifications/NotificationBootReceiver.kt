package com.example.kvantroium.features.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.kvantroium.storage.SessionStorage

class NotificationBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!SessionStorage(context).getSession().isAuthorized) return
        EventNotificationScheduler.schedule(context)
    }
}
