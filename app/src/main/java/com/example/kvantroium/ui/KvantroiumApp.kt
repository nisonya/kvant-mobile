package com.example.kvantroium.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.activity.compose.BackHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import com.example.kvantroium.api.ApiClient
import com.example.kvantroium.api.MobileUpdateService
import com.example.kvantroium.features.events.EventKind
import com.example.kvantroium.features.notifications.EventNotificationChecker
import com.example.kvantroium.features.notifications.EventNotificationScheduler
import com.example.kvantroium.features.profile.loadEmployeeKpiUrl
import com.example.kvantroium.features.profile.syncGenderAccentFromEmployee
import com.example.kvantroium.storage.NotificationStorage
import com.example.kvantroium.storage.SessionStorage
import com.example.kvantroium.ui.theme.KvantroiumTheme
import com.example.kvantroium.ui.navigation.AppNavigator
import com.example.kvantroium.ui.navigation.AppRoute
import com.example.kvantroium.ui.navigation.EventFlowContext
import com.example.kvantroium.ui.screens.AttendanceScreen
import com.example.kvantroium.ui.screens.DocumentsScreen
import com.example.kvantroium.ui.screens.EventDetailScreen
import com.example.kvantroium.ui.screens.EventEditScreen
import com.example.kvantroium.ui.screens.EventsScreen
import com.example.kvantroium.ui.screens.GroupsScreen
import com.example.kvantroium.ui.screens.StudentsScreen
import com.example.kvantroium.ui.screens.HomeScreen
import com.example.kvantroium.ui.screens.LoginScreen
import com.example.kvantroium.ui.screens.NotificationsScreen
import com.example.kvantroium.ui.screens.PixelsScreen
import com.example.kvantroium.ui.screens.PlaceholderScreen
import com.example.kvantroium.ui.screens.ProfileScreen
import com.example.kvantroium.ui.screens.rememberProfileUpdateCache
import com.example.kvantroium.ui.screens.RentScreen
import com.example.kvantroium.ui.screens.ScheduleScreen
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.kvantroium.ui.components.NetworkStatusBanner
import com.example.kvantroium.ui.util.normalizeExternalUrl
import com.example.kvantroium.ui.util.userMessage

@Composable
fun KvantroiumApp(
    storage: SessionStorage,
    apiClient: ApiClient,
    updateService: MobileUpdateService,
    eventOpenRequest: Int = 0,
    eventOpenTarget: Pair<Int, EventKind>? = null,
    onEventOpenHandled: () -> Unit = {},
    openUrl: (String) -> Unit
) {
    var session by remember { mutableStateOf(storage.getSession()) }
    val navigator = remember(session.isAuthorized) {
        AppNavigator(if (session.isAuthorized) AppRoute.Home else AppRoute.Login)
    }
    val eventFlow = remember { EventFlowContext() }
    val profileUpdateCache = rememberProfileUpdateCache()
    var kpiRequestNonce by remember { mutableIntStateOf(0) }
    var logoutNonce by remember { mutableIntStateOf(0) }
    var resumeSyncNonce by remember { mutableIntStateOf(0) }
    val sessionRevision by storage.sessionRevision.collectAsState()
    val genderAccent = remember(sessionRevision) { storage.getGenderAccent() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val route = navigator.currentRoute

    BackHandler(enabled = navigator.canPop) {
        navigator.popBack()
    }

    LaunchedEffect(sessionRevision) {
        val updated = storage.getSession()
        session = updated
        if (updated.isAuthorized) {
            runCatching { syncGenderAccentFromEmployee(apiClient, storage) }
            EventNotificationScheduler.schedule(context)
            runCatching { EventNotificationChecker.runCheck(context, apiClient, storage) }
        } else {
            EventNotificationScheduler.cancel(context)
        }
        if (!updated.isAuthorized && route != AppRoute.Login) {
            navigator.replace(AppRoute.Login)
        }
    }

    LaunchedEffect(eventOpenRequest, session.isAuthorized) {
        if (eventOpenRequest == 0) return@LaunchedEffect
        val target = eventOpenTarget ?: return@LaunchedEffect
        if (!session.isAuthorized) return@LaunchedEffect
        eventFlow.eventId = target.first
        eventFlow.kind = target.second
        eventFlow.detailReloadNonce++
        navigator.replace(AppRoute.Home)
        navigator.navigate(AppRoute.EventDetail)
        onEventOpenHandled()
    }

    LaunchedEffect(Unit) {
        if (storage.getSession().isAuthorized) {
            runCatching { apiClient.refreshSessionFromServer() }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && storage.getSession().isAuthorized) {
                resumeSyncNonce++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(resumeSyncNonce) {
        if (resumeSyncNonce == 0) return@LaunchedEffect
        runCatching { apiClient.refreshSessionFromServer() }
        runCatching { EventNotificationChecker.runCheck(context, apiClient, storage) }
    }

    LaunchedEffect(kpiRequestNonce) {
        if (kpiRequestNonce == 0) return@LaunchedEffect
        runCatching {
            loadEmployeeKpiUrl(apiClient, session.user?.employeeId)
        }.onSuccess { url ->
            val normalized = normalizeExternalUrl(url)
            if (normalized.isNullOrBlank()) {
                Toast.makeText(context, "К вам не прикреплен KPI", Toast.LENGTH_SHORT).show()
            } else {
                openUrl(normalized)
            }
        }.onFailure {
            Toast.makeText(context, it.userMessage(), Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(logoutNonce) {
        if (logoutNonce == 0) return@LaunchedEffect
        apiClient.logout()
        NotificationStorage(context).clear()
        EventNotificationScheduler.cancel(context)
        profileUpdateCache.done = false
        profileUpdateCache.pendingUpdate = null
        profileUpdateCache.updateError = null
        session = storage.getSession()
        navigator.replace(AppRoute.Login)
    }

    KvantroiumTheme(genderAccent = genderAccent) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                NetworkStatusBanner()
                Box(modifier = Modifier.weight(1f)) {
            if (navigator.contains(AppRoute.Events)) {
                EventsScreen(
                    apiClient = apiClient,
                    visible = route == AppRoute.Events,
                    onBack = { navigator.popBack() },
                    onOpenEvent = { eventId, kind ->
                        eventFlow.eventId = eventId
                        eventFlow.kind = kind
                        navigator.navigate(AppRoute.EventDetail)
                    }
                )
            }

            when (route) {
            AppRoute.Login -> LoginScreen(
                initialServerUrl = storage.getServerUrl(),
                apiClient = apiClient,
                onLoggedIn = {
                    session = storage.getSession()
                    navigator.replace(AppRoute.Home)
                }
            )

            AppRoute.Home -> HomeScreen(
                session = session,
                apiClient = apiClient,
                onOpen = { navigator.navigate(it) },
                onOpenKpi = { kpiRequestNonce++ },
                onCreateEvent = { kind ->
                    eventFlow.eventId = 0
                    eventFlow.kind = kind
                    eventFlow.isCreateMode = true
                    navigator.navigate(AppRoute.EventEdit)
                }
            )

            AppRoute.Profile -> ProfileScreen(
                session = session,
                apiClient = apiClient,
                storage = storage,
                updateService = updateService,
                updateCache = profileUpdateCache,
                onBack = { navigator.popBack() },
                onLogout = { logoutNonce++ },
                openUrl = openUrl
            )

            AppRoute.Schedule -> ScheduleScreen(
                session = session,
                apiClient = apiClient,
                onBack = { navigator.popBack() }
            )

            AppRoute.Events -> Unit

            AppRoute.EventDetail -> EventDetailScreen(
                apiClient = apiClient,
                eventId = eventFlow.eventId,
                kind = eventFlow.kind,
                onBack = { navigator.popBack() },
                onEdit = {
                    eventFlow.isCreateMode = false
                    navigator.navigate(AppRoute.EventEdit)
                },
                openUrl = openUrl,
                reloadNonce = eventFlow.detailReloadNonce
            )

            AppRoute.EventEdit -> EventEditScreen(
                apiClient = apiClient,
                eventId = eventFlow.eventId,
                kind = eventFlow.kind,
                isCreateMode = eventFlow.isCreateMode,
                onBack = { navigator.popBack() },
                onSaved = { savedId ->
                    val wasCreate = eventFlow.isCreateMode
                    eventFlow.eventId = savedId
                    eventFlow.isCreateMode = false
                    eventFlow.detailReloadNonce++
                    navigator.popBack()
                    if (wasCreate) {
                        navigator.navigate(AppRoute.EventDetail)
                    }
                }
            )

            AppRoute.Notifications -> NotificationsScreen(
                session = session,
                apiClient = apiClient,
                onBack = { navigator.popBack() },
                onOpenEvent = { eventId, kind ->
                    eventFlow.eventId = eventId
                    eventFlow.kind = kind
                    navigator.navigate(AppRoute.EventDetail)
                }
            )

            AppRoute.Groups -> GroupsScreen(
                session = session,
                apiClient = apiClient,
                onBack = { navigator.popBack() }
            )

            AppRoute.Attendance -> AttendanceScreen(
                session = session,
                apiClient = apiClient,
                onBack = { navigator.popBack() }
            )

            AppRoute.Students -> StudentsScreen(
                apiClient = apiClient,
                onBack = { navigator.popBack() }
            )

            AppRoute.Pixels -> PixelsScreen(
                session = session,
                apiClient = apiClient,
                onBack = { navigator.popBack() }
            )

            AppRoute.Rent -> RentScreen(
                apiClient = apiClient,
                onBack = { navigator.popBack() }
            )

            AppRoute.Documents -> DocumentsScreen(
                session = session,
                apiClient = apiClient,
                onBack = { navigator.popBack() },
                openUrl = openUrl
            )

            AppRoute.Employees -> PlaceholderScreen(
                title = "Сотрудники",
                message = "Раздел доступен только root, руководителю и администратору.",
                onBack = { navigator.popBack() }
            )
            }
                }
            }
        }
    }
}
