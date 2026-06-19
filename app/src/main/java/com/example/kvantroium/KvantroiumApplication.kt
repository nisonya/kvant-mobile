package com.example.kvantroium

import android.app.Application
import com.example.kvantroium.features.notifications.EventNotificationHelper
import com.example.kvantroium.features.notifications.EventNotificationScheduler
import com.example.kvantroium.storage.SessionStorage

class KvantroiumApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        EventNotificationHelper.ensureChannel(this)
        if (SessionStorage(this).getSession().isAuthorized) {
            EventNotificationScheduler.schedule(this)
        }
    }
}
