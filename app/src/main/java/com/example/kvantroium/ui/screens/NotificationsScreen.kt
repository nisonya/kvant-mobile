package com.example.kvantroium.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kvantroium.api.ApiClient
import com.example.kvantroium.features.events.EventKind
import com.example.kvantroium.features.notifications.EventReminders
import com.example.kvantroium.features.notifications.ReminderEvent
import com.example.kvantroium.features.notifications.loadEventReminders
import com.example.kvantroium.features.notifications.markRemindersViewed
import com.example.kvantroium.storage.NotificationStorage
import com.example.kvantroium.storage.UserSession
import com.example.kvantroium.ui.components.KvantCard
import com.example.kvantroium.ui.components.KvantPullRefreshBox
import com.example.kvantroium.ui.components.KvantScreenScaffold
import com.example.kvantroium.ui.components.kvantBottomScreenInset
import com.example.kvantroium.ui.theme.Montserrat
import com.example.kvantroium.ui.theme.kvantContentColor
import com.example.kvantroium.ui.util.userMessage

@Composable
fun NotificationsScreen(
    session: UserSession,
    apiClient: ApiClient,
    onBack: () -> Unit,
    onOpenEvent: (eventId: Int, kind: EventKind) -> Unit
) {
    val context = LocalContext.current
    var reminders by remember { mutableStateOf<EventReminders?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadNonce by remember { mutableIntStateOf(0) }
    val employeeId = session.user?.employeeId ?: session.user?.id ?: 0

    LaunchedEffect(employeeId, reloadNonce) {
        if (employeeId <= 0) {
            error = "Не удалось определить сотрудника"
            isLoading = false
            isRefreshing = false
            return@LaunchedEffect
        }
        if (reloadNonce == 0) {
            isLoading = true
        }
        error = null
        runCatching {
            val storage = NotificationStorage(context)
            val loaded = loadEventReminders(
                apiClient = apiClient,
                employeeId = employeeId,
                includeNewAssignments = true,
                notificationStorage = storage
            )
            markRemindersViewed(loaded, storage)
            reminders = loaded
        }.onFailure {
            error = it.userMessage()
        }
        isLoading = false
        isRefreshing = false
    }

    KvantScreenScaffold(onBack = onBack, title = "НАПОМИНАНИЯ") {
        KvantPullRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                reloadNonce++
            },
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .kvantBottomScreenInset()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = kvantContentColor())
                        }
                    }
                    error != null -> {
                        Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, fontFamily = Montserrat)
                        Button(onClick = { reloadNonce++ }) {
                            Text("Повторить", fontFamily = Montserrat)
                        }
                    }
                    reminders != null -> {
                        val data = reminders!!
                        if (data.isEmpty()) {
                            Text(
                                text = "На сегодня и завтра напоминаний нет",
                                color = kvantContentColor(),
                                fontFamily = Montserrat,
                                fontSize = 16.sp
                            )
                        } else {
                            ReminderSection(
                                title = "Новые назначения",
                                items = data.newAssignments,
                                onOpenEvent = onOpenEvent
                            )
                            ReminderSection(
                                title = "Организация · сегодня",
                                items = data.orgToday,
                                onOpenEvent = onOpenEvent
                            )
                            ReminderSection(
                                title = "Организация · завтра",
                                items = data.orgTomorrow,
                                onOpenEvent = onOpenEvent
                            )
                            ReminderSection(
                                title = "Участие · сегодня (без отметки «Участвовал»)",
                                items = data.partToday,
                                onOpenEvent = onOpenEvent
                            )
                            ReminderSection(
                                title = "Участие · завтра (без отметки «Участвовал»)",
                                items = data.partTomorrow,
                                onOpenEvent = onOpenEvent
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderSection(
    title: String,
    items: List<ReminderEvent>,
    onOpenEvent: (eventId: Int, kind: EventKind) -> Unit
) {
    if (items.isEmpty()) return
    val contentColor = kvantContentColor()

    Text(
        text = title.uppercase(),
        color = contentColor,
        fontFamily = Montserrat,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier = Modifier.padding(bottom = 4.dp)
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { item ->
            KvantCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onOpenEvent(item.id, item.kind) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = item.name,
                        color = contentColor,
                        fontFamily = Montserrat,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (item.meta.isNotBlank()) {
                        Text(
                            text = item.meta,
                            color = contentColor.copy(alpha = 0.85f),
                            fontFamily = Montserrat,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
