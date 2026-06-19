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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kvantroium.api.ApiClient
import com.example.kvantroium.features.events.loadRoomOptions
import com.example.kvantroium.features.events.parseUiDateToIso
import com.example.kvantroium.features.rent.RentByRoomItem
import com.example.kvantroium.features.rent.loadRentByDateRoom
import com.example.kvantroium.features.rent.loadScheduleLessonsForRoomOnDate
import com.example.kvantroium.features.schedule.ScheduleLesson
import com.example.kvantroium.ui.components.KvantCard
import com.example.kvantroium.ui.components.KvantDatePickerField
import com.example.kvantroium.ui.components.KvantFormSelect
import com.example.kvantroium.ui.components.KvantScreenScaffold
import com.example.kvantroium.ui.components.kvantBottomScreenInset
import com.example.kvantroium.ui.theme.Montserrat
import com.example.kvantroium.ui.theme.kvantContentColor
import com.example.kvantroium.ui.util.todayUiDate
import com.example.kvantroium.ui.util.userMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Composable
fun RentScreen(
    apiClient: ApiClient,
    onBack: () -> Unit
) {
    var dateUi by remember { mutableStateOf(todayUiDate()) }
    var roomId by remember { mutableIntStateOf(0) }
    var rooms by remember { mutableStateOf(emptyList<com.example.kvantroium.features.events.EventTypeOption>()) }
    var rents by remember { mutableStateOf<List<RentByRoomItem>>(emptyList()) }
    var scheduleLessons by remember { mutableStateOf<List<ScheduleLesson>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadNonce by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        runCatching { loadRoomOptions(apiClient) }
            .onSuccess { loaded ->
                rooms = loaded
                if (roomId == 0) roomId = loaded.firstOrNull()?.id ?: 0
            }
    }

    LaunchedEffect(dateUi, roomId, reloadNonce) {
        val dateIso = parseUiDateToIso(dateUi)
        if (dateIso.isNullOrBlank() || roomId <= 0) {
            rents = emptyList()
            scheduleLessons = emptyList()
            return@LaunchedEffect
        }
        isLoading = true
        error = null
        runCatching {
            coroutineScope {
                val rentsDeferred = async { loadRentByDateRoom(apiClient, dateIso, roomId) }
                val lessonsDeferred = async { loadScheduleLessonsForRoomOnDate(apiClient, dateIso, roomId) }
                rents = rentsDeferred.await()
                scheduleLessons = lessonsDeferred.await()
            }
        }.onFailure {
            error = it.userMessage()
            rents = emptyList()
            scheduleLessons = emptyList()
        }
        isLoading = false
    }

    val contentColor = kvantContentColor()

    KvantScreenScaffold(onBack = onBack, title = "БРОНЬ") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .kvantBottomScreenInset(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KvantDatePickerField(
                label = "Дата",
                value = dateUi,
                onValueChange = { dateUi = it }
            )
            if (rooms.isNotEmpty()) {
                KvantFormSelect(
                    label = "Кабинет",
                    selectedLabel = rooms.firstOrNull { it.id == roomId }?.name ?: "Выберите кабинет",
                    options = rooms.map { it.id to it.name },
                    selectedValue = roomId,
                    onSelected = { roomId = it }
                )
            }
            when {
                roomId <= 0 -> {
                    Text(
                        text = "Нет доступных кабинетов",
                        color = contentColor,
                        fontFamily = Montserrat
                    )
                }
                parseUiDateToIso(dateUi) == null -> {
                    Text(
                        text = "Укажите корректную дату",
                        color = MaterialTheme.colorScheme.error,
                        fontFamily = Montserrat
                    )
                }
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = contentColor)
                    }
                }
                error != null -> {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, fontFamily = Montserrat)
                    Button(onClick = { reloadNonce++ }) {
                        Text("Повторить", fontFamily = Montserrat)
                    }
                }
                rents.isEmpty() && scheduleLessons.isEmpty() -> {
                    Text(
                        text = "На выбранную дату бронирований и занятий нет",
                        color = contentColor,
                        fontFamily = Montserrat,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                else -> {
                    if (rents.isNotEmpty()) {
                        Text(
                            text = "Бронирования на дату",
                            color = contentColor,
                            fontFamily = Montserrat,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        rents.forEach { rent ->
                            KvantCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp)
                                ) {
                                    Text(
                                        text = rent.title,
                                        color = contentColor,
                                        fontFamily = Montserrat,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )
                                    Text(
                                        text = "${rent.startTime} — ${rent.endTime}",
                                        color = contentColor,
                                        fontFamily = Montserrat,
                                        fontSize = 15.sp,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                    if (scheduleLessons.isNotEmpty()) {
                        Text(
                            text = "Занятия по расписанию",
                            color = contentColor,
                            fontFamily = Montserrat,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(top = if (rents.isNotEmpty()) 8.dp else 0.dp)
                        )
                        scheduleLessons.forEach { lesson ->
                            KvantCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp)
                                ) {
                                    Text(
                                        text = lesson.group.ifBlank { lesson.room }.ifBlank { "—" },
                                        color = contentColor,
                                        fontFamily = Montserrat,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )
                                    Text(
                                        text = lesson.timeRange.ifBlank { "—" },
                                        color = contentColor,
                                        fontFamily = Montserrat,
                                        fontSize = 15.sp,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                    Text(
                                        text = "Педагог: ${lesson.teacherName.ifBlank { "—" }}",
                                        color = contentColor,
                                        fontFamily = Montserrat,
                                        fontSize = 15.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
