package com.example.kvantroium.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kvantroium.BuildConfig
import com.example.kvantroium.R
import com.example.kvantroium.api.ApiClient
import com.example.kvantroium.features.events.EventKind
import com.example.kvantroium.features.notifications.countUnseenReminders
import com.example.kvantroium.features.notifications.loadEventReminders
import com.example.kvantroium.storage.NotificationStorage
import com.example.kvantroium.storage.UserSession
import com.example.kvantroium.ui.components.KvantCard
import com.example.kvantroium.ui.components.KvantScreenContainer
import com.example.kvantroium.ui.components.TopEndIcon
import com.example.kvantroium.ui.navigation.AppRoute
import com.example.kvantroium.ui.theme.Gothic
import com.example.kvantroium.ui.theme.Montserrat
import com.example.kvantroium.ui.theme.kvantColors
import com.example.kvantroium.ui.theme.kvantContentColor

private val HomeCardHeight = 130.dp
private val HomeRowGap = 10.dp

@Composable
fun HomeScreen(
    session: UserSession,
    apiClient: ApiClient,
    onOpen: (AppRoute) -> Unit,
    onOpenKpi: () -> Unit,
    onCreateEvent: (EventKind) -> Unit
) {
    val context = LocalContext.current
    val showNewEventFab = session.user?.accessLevel != 1
    val contentColor = kvantContentColor()
    var showEventKindDialog by remember { mutableStateOf(false) }
    var reminderCount by remember { mutableIntStateOf(0) }
    val employeeId = session.user?.employeeId ?: session.user?.id ?: 0

    LaunchedEffect(employeeId) {
        if (employeeId <= 0) {
            reminderCount = 0
            return@LaunchedEffect
        }
        runCatching {
            val storage = NotificationStorage(context)
            val reminders = loadEventReminders(
                apiClient = apiClient,
                employeeId = employeeId,
                includeNewAssignments = true,
                notificationStorage = storage
            )
            reminderCount = countUnseenReminders(reminders, storage)
        }.onFailure {
            reminderCount = 0
        }
    }

    KvantScreenContainer {
        TopEndIcon(
            painter = painterResource(R.drawable.baseline_person_outline_24),
            contentDescription = "Профиль",
            onClick = { onOpen(AppRoute.Profile) }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            HomeWeightedRow {
                HomeIconCard(
                    iconRes = R.drawable.baseline_calendar_month_24,
                    title = "БРОНЬ",
                    weight = 2f,
                    titleSize = 20.sp,
                    onClick = { onOpen(AppRoute.Rent) }
                )
                HomeIconCard(
                    iconRes = R.drawable.baseline_notifications_none_24,
                    title = null,
                    weight = 3f,
                    badgeCount = reminderCount,
                    onClick = { onOpen(AppRoute.Notifications) }
                )
            }

            EventsCard(
                showFab = showNewEventFab,
                onOpenEvents = { onOpen(AppRoute.Events) },
                onNewEvent = { showEventKindDialog = true }
            )

            HomeWeightedRow {
                HomeIconCard(
                    iconRes = R.drawable.baseline_insert_drive_file_24,
                    title = "ДОКУМЕНТЫ",
                    weight = 3f,
                    titleSize = 20.sp,
                    onClick = { onOpen(AppRoute.Documents) }
                )
                HomeIconCard(
                    iconRes = R.drawable.baseline_groups_24,
                    title = "ГРУППЫ",
                    weight = 2f,
                    titleSize = 17.sp,
                    onClick = { onOpen(AppRoute.Groups) }
                )
            }

            FullWidthCard(onClick = { onOpen(AppRoute.Schedule) }) {
                CenteredTitle("РАСПИСАНИЕ", 26.sp)
            }

            HomeWeightedRow {
                TextCard(
                    title = "ПИКСЕЛИ",
                    titleSize = 22.sp,
                    weight = 3f,
                    onClick = { onOpen(AppRoute.Pixels) }
                )
                HomeIconCard(
                    iconRes = R.drawable.baseline_groups_24,
                    title = "УЧЕНИКИ",
                    weight = 2f,
                    titleSize = 17.sp,
                    onClick = { onOpen(AppRoute.Students) }
                )
            }

            FullWidthCard(onClick = { onOpen(AppRoute.Attendance) }) {
                CenteredTitle("ПОСЕЩАЕМОСТЬ", 22.sp)
            }

            KpiCard(onClick = onOpenKpi)

            Text(
                text = "Версия ${BuildConfig.VERSION_NAME}",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 50.dp, bottom = 50.dp),
                textAlign = TextAlign.Center,
                color = contentColor.copy(alpha = 0.6f),
                fontFamily = Montserrat,
                fontSize = 14.sp
            )
        }
    }

    if (showEventKindDialog) {
        EventKindPickerDialog(
            onDismiss = { showEventKindDialog = false },
            onSelect = { kind ->
                showEventKindDialog = false
                onCreateEvent(kind)
            }
        )
    }
}

@Composable
private fun HomeWeightedRow(
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 15.dp),
        horizontalArrangement = Arrangement.spacedBy(HomeRowGap),
        content = content
    )
}

@Composable
private fun RowScope.HomeIconCard(
    iconRes: Int,
    title: String?,
    modifier: Modifier = Modifier,
    titleSize: TextUnit = 20.sp,
    weight: Float = 1f,
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    val contentColor = kvantContentColor()

    Box(
        modifier = modifier
            .weight(weight)
            .height(HomeCardHeight)
    ) {
        KvantCard(
            modifier = Modifier.fillMaxSize(),
            onClick = onClick
        ) {
            if (title != null) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = title,
                            modifier = Modifier.size(52.dp),
                            tint = contentColor
                        )
                    }
                    Text(
                        text = title,
                        color = contentColor,
                        fontFamily = Gothic,
                        fontSize = titleSize,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 6.dp, end = 6.dp, bottom = 10.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = contentColor
                    )
                }
            }
        }
        if (badgeCount > 0) {
            ReminderBadge(
                count = badgeCount,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
            )
        }
    }
}

@Composable
private fun ReminderBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.error, CircleShape)
            .padding(horizontal = 7.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            color = MaterialTheme.colorScheme.onError,
            fontFamily = Montserrat,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RowScope.TextCard(
    title: String,
    modifier: Modifier = Modifier,
    titleSize: TextUnit = 22.sp,
    weight: Float = 3f,
    onClick: () -> Unit
) {
    val contentColor = kvantContentColor()

    KvantCard(
        modifier = modifier
            .weight(weight)
            .height(HomeCardHeight),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                color = contentColor,
                fontFamily = Gothic,
                fontSize = titleSize,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
private fun FullWidthCard(
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    KvantCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(HomeCardHeight)
            .padding(top = 15.dp),
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize(), content = content)
    }
}

@Composable
private fun BoxScope.CenteredTitle(title: String, fontSize: TextUnit) {
    val contentColor = kvantContentColor()
    Text(
        text = title,
        modifier = Modifier
            .align(Alignment.Center)
            .padding(horizontal = 16.dp),
        color = contentColor,
        fontFamily = Gothic,
        fontSize = fontSize,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        lineHeight = fontSize * 1.1f
    )
}

@Composable
private fun EventsCard(
    showFab: Boolean,
    onOpenEvents: () -> Unit,
    onNewEvent: () -> Unit
) {
    val contentColor = kvantContentColor()
    val kvant = kvantColors()
    val watermarkTint = if (isSystemInDarkTheme()) kvant.watermark else MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 15.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HomeCardHeight)
                .background(kvant.card, RoundedCornerShape(15.dp))
                .clickable(onClick = onOpenEvents)
        ) {
            Image(
                painter = painterResource(R.drawable.logo_monoton),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 20.dp, y = 58.dp)
                    .size(205.dp),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(watermarkTint)
            )
            Text(
                text = "МЕРОПРИЯТИЯ",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 14.dp, top = 14.dp, end = 64.dp),
                color = contentColor,
                fontFamily = Gothic,
                fontSize = 22.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showFab) {
                HomeAddButton(
                    onClick = onNewEvent,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeAddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val kvant = kvantColors()

    Surface(
        onClick = onClick,
        modifier = modifier.size(44.dp),
        shape = RoundedCornerShape(8.dp),
        color = kvant.homeEditBackground,
        shadowElevation = 6.dp,
        tonalElevation = 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.round_add_24),
                contentDescription = "Новое мероприятие",
                modifier = Modifier.size(24.dp),
                tint = kvant.homeEditIcon
            )
        }
    }
}

@Composable
private fun EventKindPickerDialog(
    onDismiss: () -> Unit,
    onSelect: (EventKind) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Новое мероприятие", fontFamily = Montserrat)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Выберите тип мероприятия",
                    fontFamily = Montserrat,
                    color = kvantContentColor()
                )
                Button(
                    onClick = { onSelect(EventKind.ORG) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Организация", fontFamily = Montserrat)
                }
                Button(
                    onClick = { onSelect(EventKind.PART) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Участие", fontFamily = Montserrat)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", fontFamily = Montserrat)
            }
        }
    )
}

@Composable
private fun KpiCard(onClick: () -> Unit) {
    val contentColor = kvantContentColor()
    val kvant = kvantColors()

    FullWidthCard(onClick = onClick) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.baseline_trending_up_24),
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    colorFilter = ColorFilter.tint(kvant.watermark)
                )
            }
            Text(
                text = "KPI",
                color = contentColor,
                fontFamily = Gothic,
                fontSize = 28.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}
