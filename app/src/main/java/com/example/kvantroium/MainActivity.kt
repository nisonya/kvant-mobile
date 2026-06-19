package com.example.kvantroium

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.example.kvantroium.api.ApiClient
import com.example.kvantroium.api.MobileUpdateService
import com.example.kvantroium.features.events.EventKind
import com.example.kvantroium.features.notifications.EventNotificationHelper
import com.example.kvantroium.features.notifications.EventNotificationScheduler
import com.example.kvantroium.storage.SessionStorage
import com.example.kvantroium.ui.KvantroiumApp
import com.example.kvantroium.ui.util.normalizeExternalUrl

class MainActivity : ComponentActivity() {
    private var eventOpenRequest by mutableIntStateOf(0)
    private var eventOpenTarget by mutableStateOf<Pair<Int, EventKind>?>(null)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            EventNotificationScheduler.schedule(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        deliverEventOpenIntent(intent)
        val storage = SessionStorage(applicationContext)
        val apiClient = ApiClient(storage)
        val updateService = MobileUpdateService(apiClient)
        ensureNotificationAccess(storage)
        setContent {
            KvantroiumApp(
                storage = storage,
                apiClient = apiClient,
                updateService = updateService,
                eventOpenRequest = eventOpenRequest,
                eventOpenTarget = eventOpenTarget,
                onEventOpenHandled = {
                    eventOpenRequest = 0
                    eventOpenTarget = null
                },
                openUrl = { url ->
                    val normalized = normalizeExternalUrl(url)
                    if (normalized.isNullOrBlank()) {
                        Toast.makeText(this, "Некорректная ссылка", Toast.LENGTH_SHORT).show()
                        return@KvantroiumApp
                    }
                    runCatching {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalized)))
                    }.onFailure {
                        Toast.makeText(this, "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deliverEventOpenIntent(intent)
    }

    private fun deliverEventOpenIntent(intent: Intent?) {
        val target = EventNotificationHelper.parseEventOpenIntent(intent) ?: return
        eventOpenTarget = target
        eventOpenRequest++
        intent?.removeExtra(EventNotificationHelper.EXTRA_EVENT_ID)
        intent?.removeExtra(EventNotificationHelper.EXTRA_EVENT_KIND)
    }

    private fun ensureNotificationAccess(storage: SessionStorage) {
        if (!storage.getSession().isAuthorized) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            EventNotificationScheduler.schedule(this)
            return
        }
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> {
                EventNotificationScheduler.schedule(this)
            }
            else -> notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
