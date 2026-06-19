package com.example.kvantroium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kvantroium.R
import com.example.kvantroium.api.ApiClient
import com.example.kvantroium.features.access.canManageReferenceData
import com.example.kvantroium.features.schedule.ScheduleLesson
import com.example.kvantroium.features.schedule.ScheduleLessonDraft
import com.example.kvantroium.features.schedule.ScheduleOption
import com.example.kvantroium.features.schedule.ScheduleReferences
import com.example.kvantroium.features.schedule.ScheduleTab
import com.example.kvantroium.features.schedule.createScheduleLesson
import com.example.kvantroium.features.schedule.dayOptions
import com.example.kvantroium.features.schedule.deleteScheduleLesson
import com.example.kvantroium.features.schedule.formatDayTitle
import com.example.kvantroium.features.schedule.groupLessonsByDay
import com.example.kvantroium.features.schedule.loadScheduleLessons
import com.example.kvantroium.features.schedule.loadScheduleOptions
import com.example.kvantroium.features.schedule.loadScheduleReferenceOptions
import com.example.kvantroium.features.schedule.updateScheduleLesson
import com.example.kvantroium.storage.UserSession
import com.example.kvantroium.ui.components.KvantFormSelect
import com.example.kvantroium.ui.components.KvantInnerCard
import com.example.kvantroium.ui.components.KvantPullRefreshBox
import com.example.kvantroium.ui.components.KvantScreenScaffold
import com.example.kvantroium.ui.components.KvantTimePickerField
import com.example.kvantroium.ui.components.kvantBottomScreenInset
import com.example.kvantroium.ui.theme.DarkBlue
import com.example.kvantroium.ui.theme.Light
import com.example.kvantroium.ui.theme.LightBlue
import com.example.kvantroium.ui.theme.Montserrat
import com.example.kvantroium.ui.theme.kvantColors
import com.example.kvantroium.ui.theme.kvantContentColor
import com.example.kvantroium.ui.util.userMessage
import kotlinx.coroutines.launch
@Composable
fun ScheduleScreen(
    session: UserSession,
    apiClient: ApiClient,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val canEdit = canManageReferenceData(session.user)
    val contentColor = kvantContentColor()
    val kvant = kvantColors()

    var activeTab by remember { mutableStateOf(ScheduleTab.TEACHERS) }
    var options by remember { mutableStateOf<List<ScheduleOption>>(emptyList()) }
    var selectedId by remember { mutableIntStateOf(0) }
    var lessons by remember { mutableStateOf<List<ScheduleLesson>>(emptyList()) }
    var references by remember { mutableStateOf<ScheduleReferences?>(null) }

    var isLoadingOptions by remember { mutableStateOf(true) }
    var isLoadingLessons by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadNonce by remember { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }

    var editDraft by remember { mutableStateOf<ScheduleLessonDraft?>(null) }
    var deleteTarget by remember { mutableStateOf<ScheduleLesson?>(null) }

    LaunchedEffect(activeTab, reloadNonce) {
        isLoadingOptions = true
        error = null
        runCatching {
            if (canEdit && references == null) {
                references = loadScheduleReferenceOptions(apiClient)
            }
            val loaded = loadScheduleOptions(apiClient, activeTab)
            options = loaded
            selectedId = when {
                loaded.any { it.id == selectedId } -> selectedId
                loaded.isNotEmpty() -> loaded.first().id
                else -> 0
            }
        }.onFailure {
            error = it.userMessage()
            options = emptyList()
            selectedId = 0
        }
        isLoadingOptions = false
        isRefreshing = false
    }

    LaunchedEffect(activeTab, selectedId, reloadNonce) {
        if (selectedId <= 0) {
            lessons = emptyList()
            return@LaunchedEffect
        }
        isLoadingLessons = true
        runCatching {
            lessons = loadScheduleLessons(apiClient, activeTab, selectedId)
        }.onFailure {
            error = it.userMessage()
            lessons = emptyList()
        }
        isLoadingLessons = false
    }

    val groupedLessons = remember(lessons) { groupLessonsByDay(lessons) }

    KvantScreenScaffold(onBack = onBack, title = "РАСПИСАНИЕ") {
        Column(modifier = Modifier.fillMaxSize()) {
            KvantPullRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    reloadNonce++
                },
                modifier = Modifier.weight(1f)
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .kvantBottomScreenInset(extra = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when {
                    isLoadingOptions -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = contentColor)
                        }
                    }

                    error != null && options.isEmpty() -> {
                        Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, fontFamily = Montserrat)
                        Button(onClick = { reloadNonce++ }) {
                            Text("Повторить", fontFamily = Montserrat)
                        }
                    }

                    else -> {
                        KvantFormSelect(
                            label = activeTab.label,
                            selectedLabel = options.firstOrNull { it.id == selectedId }?.name
                                ?: activeTab.placeholder,
                            options = listOf(0 to activeTab.placeholder) + options.map { it.id to it.name },
                            selectedValue = selectedId,
                            onSelected = { selectedId = it },
                            enabled = options.isNotEmpty()
                        )

                        if (canEdit) {
                            Button(
                                onClick = {
                                    editDraft = ScheduleLessonDraft(
                                        employeeId = if (activeTab == ScheduleTab.TEACHERS) selectedId else 0,
                                        roomId = if (activeTab == ScheduleTab.ROOMS) selectedId else 0,
                                        groupId = if (activeTab == ScheduleTab.GROUPS) selectedId else 0
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = selectedId > 0
                            ) {
                                Text("Добавить занятие", fontFamily = Montserrat, fontWeight = FontWeight.Bold)
                            }
                        }

                        when {
                            selectedId <= 0 -> {
                                Text(
                                    text = activeTab.placeholder,
                                    color = contentColor.copy(alpha = 0.75f),
                                    fontFamily = Montserrat,
                                    fontSize = 16.sp
                                )
                            }

                            isLoadingLessons -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = contentColor)
                                }
                            }

                            groupedLessons.isEmpty() -> {
                                Text(
                                    text = if (canEdit) {
                                        "Расписание не найдено. Нажмите «Добавить занятие»."
                                    } else {
                                        "Расписание не найдено"
                                    },
                                    color = contentColor,
                                    fontFamily = Montserrat,
                                    fontSize = 16.sp
                                )
                            }

                            else -> {
                                groupedLessons.forEachIndexed { index, (day, dayLessons) ->
                                    ScheduleDaySection(
                                        dayTitle = formatDayTitle(day),
                                        lessons = dayLessons,
                                        canEdit = canEdit,
                                        showTopSpacing = index > 0,
                                        onEdit = { lesson ->
                                            editDraft = ScheduleLessonDraft(
                                                id = lesson.id,
                                                roomId = lesson.roomId,
                                                groupId = lesson.groupId,
                                                employeeId = lesson.employeeId,
                                                dayNum = lesson.dayNum,
                                                startTime = lesson.startTime,
                                                endTime = lesson.endTime
                                            )
                                        },
                                        onDelete = { deleteTarget = it }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }

            ScheduleBottomBar(
                selectedTab = activeTab,
                onTabSelected = { tab ->
                    if (tab != activeTab) {
                        activeTab = tab
                        selectedId = 0
                        lessons = emptyList()
                    }
                },
                contentColor = contentColor,
                selectedContainerColor = kvant.segmentSelected,
                unselectedContainerColor = kvant.segmentUnselected
            )
        }
    }

    references?.let { refs ->
        editDraft?.let { draft ->
            ScheduleLessonEditDialog(
                draft = draft,
                references = refs,
                isCreate = draft.id <= 0,
                onDismiss = { editDraft = null },
                onSave = { updated ->
                    scope.launch {
                        runCatching {
                            if (updated.id > 0) {
                                updateScheduleLesson(apiClient, updated)
                            } else {
                                createScheduleLesson(apiClient, updated)
                            }
                            editDraft = null
                            reloadNonce++
                        }.onFailure {
                            error = it.userMessage()
                        }
                    }
                }
            )
        }
    }

    deleteTarget?.let { lesson ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Удалить занятие?", fontFamily = Montserrat) },
            text = {
                Text(
                    "${lesson.room} · ${lesson.timeRange}",
                    fontFamily = Montserrat
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = lesson.id
                        deleteTarget = null
                        scope.launch {
                            runCatching {
                                deleteScheduleLesson(apiClient, id)
                                reloadNonce++
                            }.onFailure {
                                error = it.userMessage()
                            }
                        }
                    }
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error, fontFamily = Montserrat)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Отмена", fontFamily = Montserrat)
                }
            }
        )
    }
}

@Composable
private fun ScheduleBottomBar(
    selectedTab: ScheduleTab,
    onTabSelected: (ScheduleTab) -> Unit,
    contentColor: androidx.compose.ui.graphics.Color,
    selectedContainerColor: androidx.compose.ui.graphics.Color,
    unselectedContainerColor: androidx.compose.ui.graphics.Color
) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        containerColor = unselectedContainerColor
    ) {
        ScheduleTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        painter = painterResource(
                            when (tab) {
                                ScheduleTab.TEACHERS -> R.drawable.baseline_person_24
                                ScheduleTab.ROOMS -> R.drawable.baseline_meeting_room_24
                                ScheduleTab.GROUPS -> R.drawable.baseline_groups_24
                            }
                        ),
                        contentDescription = tab.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        fontFamily = Montserrat,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = DarkBlue,
                    selectedTextColor = DarkBlue,
                    unselectedIconColor = contentColor.copy(alpha = 0.7f),
                    unselectedTextColor = contentColor.copy(alpha = 0.7f),
                    indicatorColor = selectedContainerColor
                )
            )
        }
    }
}

@Composable
private fun ScheduleDaySection(
    dayTitle: String,
    lessons: List<ScheduleLesson>,
    canEdit: Boolean,
    showTopSpacing: Boolean = false,
    onEdit: (ScheduleLesson) -> Unit,
    onDelete: (ScheduleLesson) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (showTopSpacing) Modifier.padding(top = 16.dp) else Modifier),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ScheduleDayHeader(title = dayTitle)

        lessons.forEachIndexed { index, lesson ->
            ScheduleLessonCard(
                index = index + 1,
                lesson = lesson,
                canEdit = canEdit,
                onEdit = { onEdit(lesson) },
                onDelete = { onDelete(lesson) }
            )
        }
    }
}

@Composable
private fun ScheduleDayHeader(title: String) {
    val isDark = isSystemInDarkTheme()
    val headerBackground = if (isDark) LightBlue else DarkBlue
    val headerText = Light

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(headerBackground)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = headerText,
            fontFamily = Montserrat,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            letterSpacing = 0.8.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ScheduleLessonCard(
    index: Int,
    lesson: ScheduleLesson,
    canEdit: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val contentColor = kvantContentColor()

    KvantInnerCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = index.toString(),
                modifier = Modifier
                    .width(36.dp)
                    .padding(top = 2.dp),
                color = DarkBlue,
                fontFamily = Montserrat,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                textAlign = TextAlign.Center
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lesson.room.ifBlank { "—" },
                    color = DarkBlue,
                    fontFamily = Montserrat,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Text(
                    text = lesson.timeRange.ifBlank { "—" },
                    color = contentColor,
                    fontFamily = Montserrat,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = lesson.teacherName.ifBlank { "—" },
                    color = contentColor,
                    fontFamily = Montserrat,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = lesson.group.ifBlank { "—" },
                    color = contentColor,
                    fontFamily = Montserrat,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
                if (canEdit) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                            Text("Изменить", fontFamily = Montserrat, fontSize = 13.sp)
                        }
                        OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                            Text(
                                "Удалить",
                                color = MaterialTheme.colorScheme.error,
                                fontFamily = Montserrat,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleLessonEditDialog(
    draft: ScheduleLessonDraft,
    references: ScheduleReferences,
    isCreate: Boolean,
    onDismiss: () -> Unit,
    onSave: (ScheduleLessonDraft) -> Unit
) {
    var roomId by remember(draft) { mutableIntStateOf(draft.roomId) }
    var groupId by remember(draft) { mutableIntStateOf(draft.groupId) }
    var employeeId by remember(draft) { mutableIntStateOf(draft.employeeId) }
    var dayNum by remember(draft) { mutableIntStateOf(draft.dayNum) }
    var startTime by remember(draft) { mutableStateOf(draft.startTime) }
    var endTime by remember(draft) { mutableStateOf(draft.endTime) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isCreate) "Новое занятие" else "Редактирование",
                fontFamily = Montserrat
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isCreate) {
                    KvantFormSelect(
                        label = "День",
                        selectedLabel = dayOptions().firstOrNull { it.first == dayNum }?.second ?: "Выберите день",
                        options = listOf(0 to "Выберите день") + dayOptions(),
                        selectedValue = dayNum,
                        onSelected = { dayNum = it }
                    )
                }
                KvantFormSelect(
                    label = "Кабинет",
                    selectedLabel = references.rooms.firstOrNull { it.id == roomId }?.name ?: "— Кабинет —",
                    options = listOf(0 to "— Кабинет —") + references.rooms.map { it.id to it.name },
                    selectedValue = roomId,
                    onSelected = { roomId = it }
                )
                KvantFormSelect(
                    label = "Группа",
                    selectedLabel = references.groups.firstOrNull { it.id == groupId }?.name ?: "— Группа —",
                    options = listOf(0 to "— Группа —") + references.groups.map { it.id to it.name },
                    selectedValue = groupId,
                    onSelected = { groupId = it }
                )
                KvantFormSelect(
                    label = "Наставник",
                    selectedLabel = references.teachers.firstOrNull { it.id == employeeId }?.name ?: "— Наставник —",
                    options = listOf(0 to "— Наставник —") + references.teachers.map { it.id to it.name },
                    selectedValue = employeeId,
                    onSelected = { employeeId = it }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KvantTimePickerField(
                        label = "Начало",
                        value = startTime,
                        onValueChange = { startTime = it },
                        modifier = Modifier.weight(1f)
                    )
                    KvantTimePickerField(
                        label = "Конец",
                        value = endTime,
                        onValueChange = { endTime = it },
                        modifier = Modifier.weight(1f)
                    )
                }
                validationError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontFamily = Montserrat, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    validationError = when {
                        roomId <= 0 || groupId <= 0 || employeeId <= 0 ->
                            "Укажите кабинет, группу и наставника"
                        startTime.isBlank() || endTime.isBlank() ->
                            "Укажите время начала и конца"
                        isCreate && dayNum <= 0 -> "Выберите день недели"
                        else -> null
                    }
                    if (validationError == null) {
                        onSave(
                            ScheduleLessonDraft(
                                id = draft.id,
                                roomId = roomId,
                                groupId = groupId,
                                employeeId = employeeId,
                                dayNum = dayNum,
                                startTime = startTime,
                                endTime = endTime
                            )
                        )
                    }
                }
            ) {
                Text("Сохранить", fontFamily = Montserrat)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", fontFamily = Montserrat)
            }
        }
    )
}
