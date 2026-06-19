package com.example.kvantroium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kvantroium.api.ApiClient
import com.example.kvantroium.features.access.canManageReferenceData
import com.example.kvantroium.features.groups.GroupItem
import com.example.kvantroium.features.groups.loadGroups
import com.example.kvantroium.features.pixels.PIXEL_CRITERIA_COLUMNS
import com.example.kvantroium.features.pixels.PixelColumnDef
import com.example.kvantroium.features.pixels.PixelColumnMode
import com.example.kvantroium.features.pixels.PixelStudentRow
import com.example.kvantroium.features.pixels.applyAttendanceIndex
import com.example.kvantroium.features.pixels.buildPixelAttendanceIndex
import com.example.kvantroium.features.pixels.calcPixelDelta
import com.example.kvantroium.features.pixels.clearAllPixels
import com.example.kvantroium.features.pixels.loadPixelsByGroup
import com.example.kvantroium.features.pixels.pixelColumnDisplayValue
import com.example.kvantroium.features.pixels.updatePixels
import com.example.kvantroium.storage.UserSession
import com.example.kvantroium.ui.components.KvantFormSelect
import com.example.kvantroium.ui.components.KvantPullRefreshBox
import com.example.kvantroium.ui.components.KvantScreenScaffold
import com.example.kvantroium.ui.components.kvantBottomScreenInset
import com.example.kvantroium.ui.components.kvantTextFieldColors
import com.example.kvantroium.ui.theme.Montserrat
import com.example.kvantroium.ui.theme.kvantColors
import com.example.kvantroium.ui.theme.kvantContentColor
import com.example.kvantroium.ui.util.userMessage
import kotlinx.coroutines.launch

private const val NO_GROUP_ID = 0
private val NameColumnWidth = 168.dp
private val CriteriaColumnWidth = 104.dp
private val TableHeaderHeight = 56.dp
private val TableRowHeight = 48.dp

private data class PixelActionTarget(
    val rowIndex: Int,
    val columnKey: String
)

@Composable
fun PixelsScreen(
    session: UserSession,
    apiClient: ApiClient,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val contentColor = kvantContentColor()
    val canClearAll = canManageReferenceData(session.user)

    var groups by remember { mutableStateOf<List<GroupItem>>(emptyList()) }
    var selectedGroupId by remember { mutableIntStateOf(NO_GROUP_ID) }
    val rows = remember { mutableStateListOf<PixelStudentRow>() }

    var isLoadingGroups by remember { mutableStateOf(true) }
    var isLoadingPixels by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var reloadNonce by remember { mutableIntStateOf(0) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var isClearingAll by remember { mutableStateOf(false) }

    var actionTarget by remember { mutableStateOf<PixelActionTarget?>(null) }
    var selectedOptionId by remember { mutableStateOf("") }
    var customValue by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(reloadNonce) {
        isLoadingGroups = true
        error = null
        runCatching {
            val loaded = loadGroups(apiClient)
            groups = loaded
            if (loaded.isEmpty()) {
                selectedGroupId = NO_GROUP_ID
            } else if (selectedGroupId <= 0 || loaded.none { it.id == selectedGroupId }) {
                selectedGroupId = loaded.first().id
            }
        }.onFailure {
            error = it.userMessage()
            groups = emptyList()
            selectedGroupId = NO_GROUP_ID
        }
        isLoadingGroups = false
        isRefreshing = false
    }

    LaunchedEffect(selectedGroupId, reloadNonce) {
        if (selectedGroupId <= 0) {
            rows.clear()
            return@LaunchedEffect
        }
        isLoadingPixels = true
        statusMessage = null
        runCatching {
            val attendanceIndex = buildPixelAttendanceIndex(apiClient, groups)
            val loadedRows = applyAttendanceIndex(
                loadPixelsByGroup(apiClient, selectedGroupId),
                attendanceIndex
            )
            rows.clear()
            rows.addAll(loadedRows)
        }.onFailure {
            error = it.userMessage()
            rows.clear()
        }
        isLoadingPixels = false
    }

    val groupOptions = groups.map { it.id to it.name }
    val actionColumn = actionTarget?.columnKey?.let { key ->
        PIXEL_CRITERIA_COLUMNS.firstOrNull { it.key == key }
    }
    val actionRow = actionTarget?.rowIndex?.let { index ->
        rows.getOrNull(index)
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { if (!isClearingAll) showClearAllDialog = false },
            title = { Text("Очистить все пиксели?", fontFamily = Montserrat) },
            text = {
                Text(
                    "Баллы всех учеников во всех группах будут сброшены. Действие необратимо.",
                    fontFamily = Montserrat
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            isClearingAll = true
                            runCatching {
                                clearAllPixels(apiClient)
                                statusMessage = "Все пиксели очищены"
                                reloadNonce++
                            }.onFailure {
                                error = it.userMessage()
                            }
                            isClearingAll = false
                            showClearAllDialog = false
                        }
                    },
                    enabled = !isClearingAll
                ) {
                    Text("Очистить", fontFamily = Montserrat)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearAllDialog = false },
                    enabled = !isClearingAll
                ) {
                    Text("Отмена", fontFamily = Montserrat)
                }
            }
        )
    }

    if (actionTarget != null && actionColumn != null && actionRow != null) {
        PixelActionDialog(
            studentName = actionRow.name,
            column = actionColumn,
            selectedOptionId = selectedOptionId,
            customValue = customValue,
            isSaving = isSaving,
            error = actionError,
            onOptionSelected = { selectedOptionId = it },
            onCustomValueChange = { customValue = it },
            onDismiss = {
                if (!isSaving) {
                    actionTarget = null
                    actionError = null
                }
            },
            onConfirm = {
                val delta = calcPixelDelta(actionColumn, selectedOptionId, customValue)
                if (delta == null) {
                    actionError = when (actionColumn.mode) {
                        PixelColumnMode.Number -> "Введите корректное число баллов."
                        PixelColumnMode.Penalty -> "Введите корректный размер штрафа."
                        PixelColumnMode.Select -> "Выберите вариант."
                        else -> "Не удалось определить количество баллов."
                    }
                    return@PixelActionDialog
                }
                val target = actionTarget ?: return@PixelActionDialog
                val current = rows.getOrNull(target.rowIndex) ?: return@PixelActionDialog
                val updated = current.withFieldValue(
                    key = actionColumn.key,
                    value = current.fieldValue(actionColumn.key) + delta
                )
                scope.launch {
                    isSaving = true
                    actionError = null
                    runCatching {
                        updatePixels(apiClient, updated)
                        rows[target.rowIndex] = updated
                        statusMessage = "Начислено: ${updated.name} / ${actionColumn.label}"
                        actionTarget = null
                    }.onFailure {
                        actionError = it.userMessage()
                    }
                    isSaving = false
                }
            }
        )
    }

    KvantScreenScaffold(onBack = onBack, title = "ПИКСЕЛИ") {
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
            if (canClearAll) {
                Button(
                    onClick = { showClearAllDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isClearingAll
                ) {
                    Text("Очистить все пиксели", fontFamily = Montserrat)
                }
            }

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
                    KvantFormSelect(
                        label = "Группа",
                        selectedLabel = groupOptions.firstOrNull { it.first == selectedGroupId }?.second ?: "—",
                        options = groupOptions,
                        selectedValue = selectedGroupId,
                        onSelected = { selectedGroupId = it },
                        enabled = groupOptions.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!statusMessage.isNullOrBlank()) {
                        Text(
                            text = statusMessage.orEmpty(),
                            color = contentColor.copy(alpha = 0.85f),
                            fontFamily = Montserrat,
                            fontSize = 13.sp
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

                        isLoadingPixels -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = contentColor)
                            }
                        }

                        rows.isEmpty() -> {
                            Text(
                                text = "В выбранной группе пока нет данных по пикселям.",
                                color = contentColor,
                                fontFamily = Montserrat,
                                fontSize = 16.sp
                            )
                        }

                        else -> {
                            PixelsTable(
                                rows = rows,
                                onCellClick = { rowIndex, column ->
                                    actionTarget = PixelActionTarget(rowIndex, column.key)
                                    selectedOptionId = column.options.firstOrNull()?.id.orEmpty()
                                    customValue = ""
                                    actionError = null
                                },
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
}

@Composable
private fun PixelsTable(
    rows: List<PixelStudentRow>,
    onCellClick: (rowIndex: Int, column: PixelColumnDef) -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = kvantContentColor()
    val kvant = kvantColors()
    val headerBackground = kvant.innerCard
    val nameBackground = MaterialTheme.colorScheme.background
    val horizontalScroll = rememberScrollState()
    val verticalScroll = rememberScrollState()

    Column(modifier = modifier.verticalScroll(verticalScroll)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            PixelHeaderCell(
                text = "Фамилия Имя",
                modifier = Modifier
                    .width(NameColumnWidth)
                    .height(TableHeaderHeight)
                    .background(headerBackground),
                color = contentColor
            )
            Row(modifier = Modifier.horizontalScroll(horizontalScroll)) {
                PIXEL_CRITERIA_COLUMNS.forEach { column ->
                    PixelHeaderCell(
                        text = column.label,
                        modifier = Modifier
                            .width(CriteriaColumnWidth)
                            .height(TableHeaderHeight)
                            .background(headerBackground),
                        color = contentColor
                    )
                }
            }
        }

        rows.forEachIndexed { rowIndex, row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                PixelBodyCell(
                    text = row.name,
                    modifier = Modifier
                        .width(NameColumnWidth)
                        .height(TableRowHeight)
                        .background(nameBackground),
                    color = contentColor,
                    maxLines = 2
                )
                Row(modifier = Modifier.horizontalScroll(horizontalScroll)) {
                    PIXEL_CRITERIA_COLUMNS.forEach { column ->
                        val editable = column.mode != PixelColumnMode.Readonly &&
                            column.mode != PixelColumnMode.ReadonlyTotal
                        val cellModifier = Modifier
                            .width(CriteriaColumnWidth)
                            .height(TableRowHeight)
                            .then(
                                if (editable) {
                                    Modifier
                                        .background(kvant.chipBackground.copy(alpha = 0.35f))
                                        .clickable { onCellClick(rowIndex, column) }
                                } else {
                                    Modifier
                                }
                            )
                        PixelBodyCell(
                            text = pixelColumnDisplayValue(row, column),
                            modifier = cellModifier,
                            color = if (column.key == "__total__") contentColor else contentColor,
                            bold = column.key == "__total__",
                            centered = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PixelHeaderCell(
    text: String,
    modifier: Modifier,
    color: androidx.compose.ui.graphics.Color
) {
    Box(
        modifier = modifier.padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontFamily = Montserrat,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PixelBodyCell(
    text: String,
    modifier: Modifier,
    color: androidx.compose.ui.graphics.Color,
    bold: Boolean = false,
    centered: Boolean = true,
    maxLines: Int = 1
) {
    Box(
        modifier = modifier.padding(horizontal = 6.dp),
        contentAlignment = if (centered) Alignment.Center else Alignment.CenterStart
    ) {
        Text(
            text = text,
            color = color,
            fontFamily = Montserrat,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PixelActionDialog(
    studentName: String,
    column: PixelColumnDef,
    selectedOptionId: String,
    customValue: String,
    isSaving: Boolean,
    error: String?,
    onOptionSelected: (String) -> Unit,
    onCustomValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Начисление баллов", fontFamily = Montserrat, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Начислить ученику $studentName баллы за «${column.label}»?",
                    fontFamily = Montserrat,
                    fontSize = 14.sp
                )
                when (column.mode) {
                    PixelColumnMode.Fixed -> {
                        Text(
                            text = "+${column.fixedPoints} баллов",
                            fontFamily = Montserrat,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    PixelColumnMode.Select -> {
                        column.options.forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOptionSelected(option.id) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedOptionId == option.id,
                                    onClick = { onOptionSelected(option.id) }
                                )
                                Text(option.label, fontFamily = Montserrat, fontSize = 14.sp)
                            }
                        }
                    }

                    PixelColumnMode.Number -> {
                        OutlinedTextField(
                            value = customValue,
                            onValueChange = onCustomValueChange,
                            label = { Text("Количество баллов", fontFamily = Montserrat) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = kvantTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    PixelColumnMode.Penalty -> {
                        OutlinedTextField(
                            value = customValue,
                            onValueChange = onCustomValueChange,
                            label = { Text("Размер штрафа", fontFamily = Montserrat) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = kvantTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Штраф будет вычтен из итоговой суммы.",
                            fontFamily = Montserrat,
                            fontSize = 12.sp,
                            color = kvantContentColor().copy(alpha = 0.75f)
                        )
                    }

                    else -> Unit
                }
                if (!error.isNullOrBlank()) {
                    Text(error, color = MaterialTheme.colorScheme.error, fontFamily = Montserrat)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isSaving) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.heightIn(max = 18.dp))
                } else {
                    Text("Начислить", fontFamily = Montserrat)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Отмена", fontFamily = Montserrat)
            }
        }
    )
}
