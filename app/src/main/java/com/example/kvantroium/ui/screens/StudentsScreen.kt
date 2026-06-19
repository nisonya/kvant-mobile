package com.example.kvantroium.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.example.kvantroium.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kvantroium.api.ApiClient
import com.example.kvantroium.features.groups.GroupItem
import com.example.kvantroium.features.students.StudentProfile
import com.example.kvantroium.features.students.filterStudents
import com.example.kvantroium.features.students.loadAllStudents
import com.example.kvantroium.features.students.loadStudentGroups
import com.example.kvantroium.ui.components.CopyableInfoRow
import com.example.kvantroium.ui.components.KvantCard
import com.example.kvantroium.ui.components.KvantScreenScaffold
import com.example.kvantroium.ui.components.kvantBottomScreenInset
import com.example.kvantroium.ui.components.kvantTextFieldColors
import com.example.kvantroium.ui.theme.Montserrat
import com.example.kvantroium.ui.theme.kvantContentColor
import com.example.kvantroium.ui.util.userMessage

@Composable
fun StudentsScreen(
    apiClient: ApiClient,
    onBack: () -> Unit
) {
    var students by remember { mutableStateOf<List<StudentProfile>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var expandedStudentId by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadNonce by remember { mutableIntStateOf(0) }

    LaunchedEffect(reloadNonce) {
        isLoading = true
        error = null
        runCatching {
            students = loadAllStudents(apiClient)
        }.onFailure {
            error = it.userMessage()
        }
        isLoading = false
    }

    val filtered = remember(students, searchQuery) {
        filterStudents(students, searchQuery)
    }

    KvantScreenScaffold(onBack = onBack, title = "УЧЕНИКИ") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .kvantBottomScreenInset(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Поиск по ФИО, телефону, email", fontFamily = Montserrat) },
                singleLine = true,
                colors = kvantTextFieldColors()
            )

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

                error != null && students.isEmpty() -> {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, fontFamily = Montserrat)
                    Button(onClick = { reloadNonce++ }) {
                        Text("Повторить", fontFamily = Montserrat)
                    }
                }

                filtered.isEmpty() -> {
                    Text(
                        text = if (searchQuery.isBlank()) "Ученики не найдены" else "Ничего не найдено",
                        color = kvantContentColor(),
                        fontFamily = Montserrat,
                        fontSize = 16.sp
                    )
                }

                else -> {
                    if (searchQuery.isNotBlank()) {
                        Text(
                            text = "Найдено: ${filtered.size}",
                            color = kvantContentColor().copy(alpha = 0.75f),
                            fontFamily = Montserrat,
                            fontSize = 14.sp
                        )
                    }
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filtered, key = { it.id }) { student ->
                            StudentExpandableCard(
                                student = student,
                                expanded = expandedStudentId == student.id,
                                onToggle = {
                                    expandedStudentId = if (expandedStudentId == student.id) 0 else student.id
                                },
                                apiClient = apiClient
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentExpandableCard(
    student: StudentProfile,
    expanded: Boolean,
    onToggle: () -> Unit,
    apiClient: ApiClient
) {
    val contentColor = kvantContentColor()
    var groups by remember(student.id) { mutableStateOf<List<GroupItem>?>(null) }
    var groupsLoading by remember { mutableStateOf(false) }

    LaunchedEffect(expanded, student.id) {
        if (expanded && groups == null) {
            groupsLoading = true
            runCatching {
                groups = loadStudentGroups(apiClient, student.id)
            }.onFailure {
                groups = emptyList()
            }
            groupsLoading = false
        }
    }

    KvantCard(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = student.fullName.ifBlank { "—" },
                        color = contentColor,
                        fontFamily = Montserrat,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    if (!expanded) {
                        val meta = buildList {
                            if (student.birthDayDisplay.isNotBlank()) add(student.birthDayDisplay)
                            if (student.phone.isNotBlank()) add(student.phone)
                        }.joinToString(" · ")
                        if (meta.isNotBlank()) {
                            Text(
                                text = meta,
                                color = contentColor.copy(alpha = 0.75f),
                                fontFamily = Montserrat,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
                Icon(
                    painter = painterResource(R.drawable.round_arrow_drop_down_24),
                    contentDescription = if (expanded) "Свернуть" else "Развернуть",
                    tint = contentColor,
                    modifier = Modifier.rotate(if (expanded) 180f else 0f)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    CopyableInfoRow(label = "Дата рождения", value = student.birthDayDisplay)
                    CopyableInfoRow(label = "Навигатор", value = student.navigatorLabel)
                    CopyableInfoRow(label = "Родитель", value = student.parentFullName)
                    CopyableInfoRow(label = "Email", value = student.email)
                    CopyableInfoRow(label = "Телефон", value = student.phone)

                    Text(
                        text = "Группы",
                        color = contentColor.copy(alpha = 0.7f),
                        fontFamily = Montserrat,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    when {
                        groupsLoading -> {
                            Text(
                                text = "Загрузка…",
                                color = contentColor,
                                fontFamily = Montserrat,
                                fontSize = 14.sp
                            )
                        }

                        groups.isNullOrEmpty() -> {
                            Text(
                                text = "Без группы",
                                color = contentColor,
                                fontFamily = Montserrat,
                                fontSize = 15.sp
                            )
                        }

                        else -> {
                            groups.orEmpty().forEach { group ->
                                CopyableInfoRow(label = "Группа", value = group.name)
                            }
                        }
                    }
                }
            }
        }
    }
}
