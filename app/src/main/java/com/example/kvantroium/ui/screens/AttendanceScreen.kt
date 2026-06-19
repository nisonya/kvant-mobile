package com.example.kvantroium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kvantroium.api.ApiClient
import com.example.kvantroium.features.attendance.AttendanceEditRow
import com.example.kvantroium.features.attendance.AttendancePivot
import com.example.kvantroium.features.attendance.loadAttendanceByGroup
import com.example.kvantroium.features.attendance.loadAttendanceEditRows
import com.example.kvantroium.features.attendance.pivotAttendance
import com.example.kvantroium.features.attendance.saveAttendance
import com.example.kvantroium.features.events.formatEventDateInput
import com.example.kvantroium.features.events.parseUiDateToIso
import com.example.kvantroium.features.groups.GroupItem
import com.example.kvantroium.features.groups.loadGroups
import com.example.kvantroium.features.groups.loadGroupsByTeacher
import com.example.kvantroium.storage.UserSession
import com.example.kvantroium.ui.components.KvantDatePickerField
import com.example.kvantroium.ui.components.KvantFormSelect
import com.example.kvantroium.ui.components.KvantPullRefreshBox
import com.example.kvantroium.ui.components.KvantScreenScaffold
import com.example.kvantroium.ui.components.kvantBottomScreenInset
import com.example.kvantroium.ui.theme.Montserrat
import com.example.kvantroium.ui.theme.kvantColors
import com.example.kvantroium.ui.theme.kvantContentColor
import com.example.kvantroium.ui.util.todayUiDate
import com.example.kvantroium.ui.util.userMessage
import kotlinx.coroutines.launch

private const val NO_GROUP_ID = 0
private val NameColumnWidth = 168.dp
private val DateColumnWidth = 92.dp
private val PercentColumnWidth = 52.dp
private val TableHeaderHeight = 44.dp
private val TableRowHeight = 44.dp

@Composable
fun AttendanceScreen(
    session: UserSession,
    apiClient: ApiClient,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val employeeId = session.user?.employeeId ?: session.user?.id ?: 0

    var groups by remember { mutableStateOf<List<GroupItem>>(emptyList()) }
    var teacherGroups by remember { mutableStateOf<List<GroupItem>>(emptyList()) }
    var selectedGroupId by remember { mutableIntStateOf(NO_GROUP_ID) }
    var pivot by remember { mutableStateOf<AttendancePivot?>(null) }

    var isLoadingGroups by remember { mutableStateOf(true) }
    var isLoadingAttendance by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadNonce by remember { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }

    var showEditDialog by remember { mutableStateOf(false) }

    LaunchedEffect(reloadNonce, employeeId) {
        isLoadingGroups = true
        error = null
        runCatching {
            val loadedGroups = loadGroups(apiClient)
            groups = loadedGroups
            teacherGroups = loadGroupsByTeacher(apiClient, employeeId)
            if (loadedGroups.isEmpty()) {
                selectedGroupId = NO_GROUP_ID
            } else if (selectedGroupId <= 0 || loadedGroups.none { it.id == selectedGroupId }) {
                selectedGroupId = loadedGroups.first().id
            }
        }.onFailure {
            error = it.userMessage()
            groups = emptyList()
            teacherGroups = emptyList()
            selectedGroupId = NO_GROUP_ID
        }
        isLoadingGroups = false
        isRefreshing = false
    }

    LaunchedEffect(selectedGroupId, reloadNonce) {
        if (selectedGroupId <= 0) {
            pivot = null
            return@LaunchedEffect
        }
        isLoadingAttendance = true
        runCatching {
            pivot = pivotAttendance(loadAttendanceByGroup(apiClient, selectedGroupId))
        }.onFailure {
            error = it.userMessage()
            pivot = null
        }
        isLoadingAttendance = false
    }

    val groupOptions = groups.map { it.id to it.name }
    val contentColor = kvantContentColor()

    KvantScreenScaffold(onBack = onBack, title = "ПОСЕЩАЕМОСТЬ") {
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
                .kvantBottomScreenInset(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                isLoadingGroups -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = contentColor)
                    }
                }

                error != null && groups.isEmpty() -> {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, fontFamily = Montserrat)
                    Button(onClick = { reloadNonce++ }) {
                        Text("Повторить", fontFamily = Montserrat)
                    }
                }

                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        KvantFormSelect(
                            label = "Группа",
                            selectedLabel = groupOptions.firstOrNull { it.first == selectedGroupId }?.second ?: "—",
                            options = groupOptions,
                            selectedValue = selectedGroupId,
                            onSelected = { selectedGroupId = it },
                            enabled = groupOptions.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { showEditDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            enabled = teacherGroups.isNotEmpty()
                        ) {
                            Text(
                                text = "Редактировать посещаемость",
                                fontFamily = Montserrat,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (teacherGroups.isEmpty()) {
                        Text(
                            text = "Нет групп для редактирования (только группы, которые вы ведёте).",
                            color = contentColor.copy(alpha = 0.75f),
                            fontFamily = Montserrat,
                            fontSize = 14.sp
                        )
                    }

                    when {
                        groupOptions.isEmpty() -> {
                            Text(
                                text = "Группы не найдены",
                                color = contentColor,
                                fontFamily = Montserrat,
                                fontSize = 16.sp
                            )
                        }

                        isLoadingAttendance -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = contentColor)
                            }
                        }

                        pivot == null || pivot!!.dates.isEmpty() || pivot!!.students.isEmpty() -> {
                            Text(
                                text = "По выбранной группе нет данных посещаемости.",
                                color = contentColor,
                                fontFamily = Montserrat,
                                fontSize = 16.sp
                            )
                        }

                        else -> {
                            AttendanceTable(
                                pivot = pivot!!,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                        }
                    }
                }
            }
        }
        }
    }

    if (showEditDialog) {
        AttendanceEditDialog(
            apiClient = apiClient,
            teacherGroups = teacherGroups,
            initialGroupId = when {
                selectedGroupId > 0 && teacherGroups.any { it.id == selectedGroupId } -> selectedGroupId
                else -> teacherGroups.firstOrNull()?.id ?: NO_GROUP_ID
            },
            onDismiss = { showEditDialog = false },
            onSaved = { savedGroupId ->
                showEditDialog = false
                if (savedGroupId > 0) selectedGroupId = savedGroupId
                reloadNonce++
            }
        )
    }
}

@Composable
private fun AttendanceTable(
    pivot: AttendancePivot,
    modifier: Modifier = Modifier
) {
    val contentColor = kvantContentColor()
    val headerBackground = kvantColors().innerCard
    val nameBackground = MaterialTheme.colorScheme.background
    val horizontalScroll = rememberScrollState()
    val verticalScroll = rememberScrollState()

    Column(
        modifier = modifier.verticalScroll(verticalScroll)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            AttendanceHeaderCell(
                text = "Фамилия Имя",
                modifier = Modifier
                    .width(NameColumnWidth)
                    .height(TableHeaderHeight)
                    .background(headerBackground),
                bold = true,
                color = contentColor
            )
            Row(modifier = Modifier.horizontalScroll(horizontalScroll)) {
                pivot.dates.forEach { date ->
                    AttendanceHeaderCell(
                        text = formatEventDateInput(date).ifBlank { date },
                        modifier = Modifier
                            .width(DateColumnWidth)
                            .height(TableHeaderHeight)
                            .background(headerBackground),
                        bold = true,
                        color = contentColor
                    )
                }
                AttendanceHeaderCell(
                    text = "%",
                    modifier = Modifier
                        .width(PercentColumnWidth)
                        .height(TableHeaderHeight)
                        .background(headerBackground),
                    bold = true,
                    color = contentColor,
                    centered = true
                )
            }
        }

        pivot.students.forEach { student ->
            Row(modifier = Modifier.fillMaxWidth()) {
                AttendanceBodyCell(
                    text = student.name,
                    modifier = Modifier
                        .width(NameColumnWidth)
                        .height(TableRowHeight)
                        .background(nameBackground),
                    color = contentColor,
                    maxLines = 2
                )
                Row(modifier = Modifier.horizontalScroll(horizontalScroll)) {
                    pivot.dates.forEach { date ->
                        AttendanceBodyCell(
                            text = student.byDate[date] ?: "—",
                            modifier = Modifier
                                .width(DateColumnWidth)
                                .height(TableRowHeight),
                            color = contentColor,
                            centered = true
                        )
                    }
                    AttendanceBodyCell(
                        text = "${student.percent}%",
                        modifier = Modifier
                            .width(PercentColumnWidth)
                            .height(TableRowHeight),
                        color = contentColor,
                        centered = true
                    )
                }
            }
        }
    }
}

@Composable
private fun AttendanceHeaderCell(
    text: String,
    modifier: Modifier,
    bold: Boolean,
    color: androidx.compose.ui.graphics.Color,
    centered: Boolean = false
) {
    Box(
        modifier = modifier.padding(horizontal = 8.dp),
        contentAlignment = if (centered) Alignment.Center else Alignment.CenterStart
    ) {
        Text(
            text = text,
            color = color,
            fontFamily = Montserrat,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AttendanceBodyCell(
    text: String,
    modifier: Modifier,
    color: androidx.compose.ui.graphics.Color,
    centered: Boolean = false,
    maxLines: Int = 1
) {
    Box(
        modifier = modifier.padding(horizontal = 8.dp),
        contentAlignment = if (centered) Alignment.Center else Alignment.CenterStart
    ) {
        Text(
            text = text,
            color = color,
            fontFamily = Montserrat,
            fontSize = 13.sp,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AttendanceEditDialog(
    apiClient: ApiClient,
    teacherGroups: List<GroupItem>,
    initialGroupId: Int,
    onDismiss: () -> Unit,
    onSaved: (Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    val contentColor = kvantContentColor()

    var groupId by remember { mutableIntStateOf(initialGroupId) }
    var dateUi by remember { mutableStateOf(todayUiDate()) }
    val editRows = remember { mutableStateListOf<AttendanceEditRow>() }

    var isLoading by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loadNonce by remember { mutableIntStateOf(0) }

    val groupOptions = teacherGroups.map { it.id to it.name }
    val dateIso = parseUiDateToIso(dateUi)

    LaunchedEffect(groupId, dateUi, loadNonce) {
        if (groupId <= 0 || dateIso.isNullOrBlank()) {
            editRows.clear()
            return@LaunchedEffect
        }
        isLoading = true
        error = null
        runCatching {
            editRows.clear()
            editRows.addAll(loadAttendanceEditRows(apiClient, groupId, dateIso))
        }.onFailure {
            error = it.userMessage()
            editRows.clear()
        }
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = {
            Text("Редактировать посещаемость", fontFamily = Montserrat, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (groupOptions.isNotEmpty()) {
                    KvantFormSelect(
                        label = "Группа",
                        selectedLabel = groupOptions.firstOrNull { it.first == groupId }?.second ?: "—",
                        options = groupOptions,
                        selectedValue = groupId,
                        onSelected = { groupId = it },
                        enabled = !isSaving && !isLoading
                    )
                }

                KvantDatePickerField(
                    label = "Дата",
                    value = dateUi,
                    onValueChange = { dateUi = it },
                    enabled = !isSaving && !isLoading
                )

                if (error != null) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, fontFamily = Montserrat)
                }

                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = contentColor)
                        }
                    }

                    groupId <= 0 || dateIso.isNullOrBlank() -> {
                        Text(
                            text = "Выберите группу и дату.",
                            color = contentColor,
                            fontFamily = Montserrat
                        )
                    }

                    editRows.isEmpty() -> {
                        Text(
                            text = "В выбранной группе нет учеников.",
                            color = contentColor,
                            fontFamily = Montserrat
                        )
                    }

                    else -> {
                        editRows.forEachIndexed { index, row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = row.present,
                                    onCheckedChange = { checked ->
                                        editRows[index] = row.copy(present = checked)
                                    },
                                    enabled = !isSaving
                                )
                                Text(
                                    text = row.name,
                                    color = contentColor,
                                    fontFamily = Montserrat,
                                    fontSize = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val iso = parseUiDateToIso(dateUi)
                    if (groupId <= 0 || iso.isNullOrBlank() || editRows.isEmpty()) {
                        error = "Выберите группу, дату и отметьте учеников."
                        return@TextButton
                    }
                    scope.launch {
                        isSaving = true
                        error = null
                        runCatching {
                            saveAttendance(apiClient, groupId, iso, editRows.toList())
                            onSaved(groupId)
                        }.onFailure {
                            error = it.userMessage()
                        }
                        isSaving = false
                    }
                },
                enabled = !isSaving && !isLoading && editRows.isNotEmpty()
            ) {
                Text(if (isSaving) "Сохранение..." else "Сохранить", fontFamily = Montserrat)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Отмена", fontFamily = Montserrat)
            }
        }
    )
}
