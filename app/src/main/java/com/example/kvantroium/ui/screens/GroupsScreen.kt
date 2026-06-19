package com.example.kvantroium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kvantroium.api.ApiClient
import com.example.kvantroium.features.access.canManageReferenceData
import com.example.kvantroium.features.groups.GroupItem
import com.example.kvantroium.features.groups.createGroup
import com.example.kvantroium.features.groups.deleteGroup
import com.example.kvantroium.features.groups.loadGroups
import com.example.kvantroium.features.groups.renameGroup
import com.example.kvantroium.features.students.StudentProfile
import com.example.kvantroium.features.students.loadStudentsByGroup
import com.example.kvantroium.storage.UserSession
import com.example.kvantroium.ui.components.KvantFormSelect
import com.example.kvantroium.ui.components.KvantScreenScaffold
import com.example.kvantroium.ui.components.kvantBottomScreenInset
import com.example.kvantroium.ui.components.kvantTextFieldColors
import com.example.kvantroium.ui.theme.Montserrat
import com.example.kvantroium.ui.theme.kvantColors
import com.example.kvantroium.ui.theme.kvantContentColor
import com.example.kvantroium.ui.util.userMessage
import kotlinx.coroutines.launch

private const val NO_GROUP_ID = 0

private data class StudentTableColumn(
    val title: String,
    val width: Int,
    val value: (StudentProfile) -> String
)

private val studentTableColumns = listOf(
    StudentTableColumn("Фамилия", 120) { it.surname },
    StudentTableColumn("Имя", 110) { it.name },
    StudentTableColumn("Отчество", 120) { it.patronymic },
    StudentTableColumn("Дата рожд.", 110) { it.birthDayDisplay },
    StudentTableColumn("Навигатор", 90) { it.navigatorLabel },
    StudentTableColumn("Родитель", 180) { it.parentFullName },
    StudentTableColumn("Email", 180) { it.email },
    StudentTableColumn("Телефон", 140) { it.phone }
)

@Composable
fun GroupsScreen(
    session: UserSession,
    apiClient: ApiClient,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val canEdit = canManageReferenceData(session.user)
    var groups by remember { mutableStateOf<List<GroupItem>>(emptyList()) }
    var selectedGroupId by remember { mutableIntStateOf(NO_GROUP_ID) }
    var students by remember { mutableStateOf<List<StudentProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingStudents by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadNonce by remember { mutableIntStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    LaunchedEffect(reloadNonce) {
        isLoading = true
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
        }
        isLoading = false
    }

    LaunchedEffect(selectedGroupId) {
        if (selectedGroupId <= 0) {
            students = emptyList()
            return@LaunchedEffect
        }
        isLoadingStudents = true
        runCatching {
            students = loadStudentsByGroup(apiClient, selectedGroupId)
        }.onFailure {
            error = it.userMessage()
            students = emptyList()
        }
        isLoadingStudents = false
    }

    val selectedGroup = groups.firstOrNull { it.id == selectedGroupId }
    val groupOptions = groups.map { it.id to it.name }

    KvantScreenScaffold(onBack = onBack, title = "ГРУППЫ") {
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

            error != null && groups.isEmpty() -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, fontFamily = Montserrat)
                    Button(onClick = { reloadNonce++ }) {
                        Text("Повторить", fontFamily = Montserrat)
                    }
                }
            }

            else -> {
                GroupsContent(
                    groupOptions = groupOptions,
                    selectedGroupId = selectedGroupId,
                    onGroupSelected = { selectedGroupId = it },
                    students = students,
                    isLoadingStudents = isLoadingStudents,
                    canEdit = canEdit,
                    onCreate = { showCreateDialog = true },
                    onRename = { showRenameDialog = true },
                    onDelete = {
                        val groupId = selectedGroupId
                        if (groupId <= 0) return@GroupsContent
                        scope.launch {
                            runCatching {
                                deleteGroup(apiClient, groupId)
                                reloadNonce++
                            }.onFailure {
                                error = it.userMessage()
                            }
                        }
                    }
                )
            }
        }
    }

    if (showCreateDialog) {
        GroupNameDialog(
            title = "Новая группа",
            initial = "",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                scope.launch {
                    runCatching {
                        createGroup(apiClient, name)
                        reloadNonce++
                        showCreateDialog = false
                    }.onFailure {
                        error = it.userMessage()
                    }
                }
            }
        )
    }

    if (showRenameDialog && selectedGroup != null) {
        val group = selectedGroup
        GroupNameDialog(
            title = "Переименовать группу",
            initial = group.name,
            onDismiss = { showRenameDialog = false },
            onConfirm = { name ->
                scope.launch {
                    runCatching {
                        renameGroup(apiClient, group.id, name)
                        reloadNonce++
                        showRenameDialog = false
                    }.onFailure {
                        error = it.userMessage()
                    }
                }
            }
        )
    }
}

@Composable
private fun GroupsContent(
    groupOptions: List<Pair<Int, String>>,
    selectedGroupId: Int,
    onGroupSelected: (Int) -> Unit,
    students: List<StudentProfile>,
    isLoadingStudents: Boolean,
    canEdit: Boolean,
    onCreate: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val contentColor = kvantContentColor()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .kvantBottomScreenInset(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        KvantFormSelect(
            label = "Группа",
            selectedLabel = groupOptions.firstOrNull { it.first == selectedGroupId }?.second ?: "—",
            options = groupOptions,
            selectedValue = selectedGroupId,
            onSelected = onGroupSelected,
            enabled = groupOptions.isNotEmpty()
        )

        if (canEdit) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCreate, modifier = Modifier.weight(1f)) {
                    Text("Создать", fontFamily = Montserrat)
                }
                Button(
                    onClick = onRename,
                    modifier = Modifier.weight(1f),
                    enabled = selectedGroupId > 0
                ) {
                    Text("Переименовать", fontFamily = Montserrat)
                }
                Button(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    enabled = selectedGroupId > 0
                ) {
                    Text("Удалить", fontFamily = Montserrat)
                }
            }
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

            isLoadingStudents -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = contentColor)
                }
            }

            students.isEmpty() -> {
                Text(
                    text = "В группе нет учеников",
                    color = contentColor,
                    fontFamily = Montserrat,
                    fontSize = 16.sp
                )
            }

            else -> {
                Text(
                    text = "Учеников: ${students.size}",
                    color = contentColor.copy(alpha = 0.75f),
                    fontFamily = Montserrat,
                    fontSize = 14.sp
                )
                GroupStudentsTable(
                    students = students,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}

@Composable
private fun GroupStudentsTable(
    students: List<StudentProfile>,
    modifier: Modifier = Modifier
) {
    val contentColor = kvantContentColor()
    val headerBackground = kvantColors().innerCard
    val horizontalScroll = rememberScrollState()
    val verticalScroll = rememberScrollState()

    SelectionContainer(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScroll)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(horizontalScroll)
                    .background(headerBackground)
                    .padding(vertical = 10.dp)
            ) {
                studentTableColumns.forEach { column ->
                    StudentTableCell(
                        text = column.title,
                        width = column.width,
                        bold = true,
                        color = contentColor
                    )
                }
            }

            students.forEach { student ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(horizontalScroll)
                        .padding(vertical = 8.dp)
                ) {
                    studentTableColumns.forEach { column ->
                        StudentTableCell(
                            text = column.value(student).ifBlank { "—" },
                            width = column.width,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentTableCell(
    text: String,
    width: Int,
    bold: Boolean = false,
    color: androidx.compose.ui.graphics.Color
) {
    Text(
        text = text,
        modifier = Modifier
            .width(width.dp)
            .padding(horizontal = 8.dp),
        color = color,
        fontFamily = Montserrat,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        fontSize = if (bold) 14.sp else 13.sp
    )
}

@Composable
private fun GroupNameDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontFamily = Montserrat) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название", fontFamily = Montserrat) },
                singleLine = true,
                colors = kvantTextFieldColors()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.trim().isNotEmpty()
            ) {
                Text("OK", fontFamily = Montserrat)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", fontFamily = Montserrat)
            }
        }
    )
}
