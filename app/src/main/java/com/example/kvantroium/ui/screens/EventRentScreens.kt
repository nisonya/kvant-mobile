package com.example.kvantroium.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kvantroium.R
import com.example.kvantroium.api.ApiClient
import com.example.kvantroium.features.events.EventRentDetail
import com.example.kvantroium.features.events.EventTypeOption
import com.example.kvantroium.features.events.deleteEventRent
import com.example.kvantroium.features.events.loadEventRentsDetailed
import com.example.kvantroium.features.events.loadRoomOptions
import com.example.kvantroium.features.events.parseUiDateToIso
import com.example.kvantroium.features.events.saveEventRent
import com.example.kvantroium.ui.components.KvantCard
import com.example.kvantroium.ui.components.KvantFormField
import com.example.kvantroium.ui.components.KvantFormSectionTitle
import com.example.kvantroium.ui.components.KvantFormSelect
import com.example.kvantroium.ui.components.KvantInnerCard
import com.example.kvantroium.ui.components.KvantScreenScaffold
import com.example.kvantroium.ui.components.KvantTimePickerField
import com.example.kvantroium.ui.components.kvantBottomScreenInset
import com.example.kvantroium.ui.theme.Montserrat
import com.example.kvantroium.ui.theme.kvantColors
import com.example.kvantroium.ui.theme.kvantContentColor
import com.example.kvantroium.ui.util.userMessage

@Composable
fun OrgRentInlineSection(
    apiClient: ApiClient,
    eventId: Int,
    eventDateUi: String,
    reloadNonce: Int,
    onReloadHandled: () -> Unit,
    draftRents: List<EventRentDetail> = emptyList(),
    onDraftRentsChange: (List<EventRentDetail>) -> Unit = {}
) {
    val isDraftMode = eventId <= 0
    var rooms by remember { mutableStateOf<List<EventTypeOption>>(emptyList()) }
    var rents by remember { mutableStateOf<List<EventRentDetail>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    var editingRentId by remember { mutableIntStateOf(0) }
    var roomId by remember { mutableIntStateOf(0) }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var actionError by remember { mutableStateOf<String?>(null) }
    var isBusy by remember { mutableStateOf(false) }
    var saveNonce by remember { mutableIntStateOf(0) }
    var deleteNonce by remember { mutableIntStateOf(0) }

    val contentColor = kvantContentColor()
    val kvant = kvantColors()
    val dateIso = remember(eventDateUi) { parseUiDateToIso(eventDateUi).orEmpty() }

    LaunchedEffect(eventId, reloadNonce, isDraftMode) {
        isLoading = true
        loadError = null
        runCatching {
            rooms = loadRoomOptions(apiClient)
            if (!isDraftMode) {
                rents = loadEventRentsDetailed(apiClient, eventId)
            }
        }.onFailure {
            loadError = it.userMessage()
        }
        isLoading = false
        if (reloadNonce > 0) onReloadHandled()
    }

    val visibleRents = if (isDraftMode) draftRents else rents

    fun clearRentForm() {
        editingRentId = 0
        roomId = 0
        startTime = ""
        endTime = ""
        actionError = null
    }

    fun selectRent(rent: EventRentDetail) {
        editingRentId = rent.id
        roomId = rent.roomId
        startTime = rent.startTime
        endTime = rent.endTime
        actionError = null
    }

    LaunchedEffect(saveNonce) {
        if (saveNonce == 0) return@LaunchedEffect
        if (roomId <= 0) {
            actionError = "Выберите кабинет"
            return@LaunchedEffect
        }
        if (startTime.isBlank() || endTime.isBlank()) {
            actionError = "Укажите время начала и окончания"
            return@LaunchedEffect
        }
        if (dateIso.isBlank()) {
            actionError = "Сначала укажите дату мероприятия"
            return@LaunchedEffect
        }
        isBusy = true
        actionError = null
        runCatching {
            if (isDraftMode) {
                val roomName = rooms.firstOrNull { it.id == roomId }?.name.orEmpty()
                val rentEntry = EventRentDetail(
                    id = editingRentId,
                    roomId = roomId,
                    roomName = roomName,
                    date = dateIso,
                    startTime = startTime,
                    endTime = endTime
                )
                val updated = if (editingRentId != 0) {
                    draftRents.map { if (it.id == editingRentId) rentEntry else it }
                } else {
                    draftRents + rentEntry.copy(id = -(draftRents.size + 1))
                }
                onDraftRentsChange(updated)
                clearRentForm()
            } else {
                saveEventRent(
                    apiClient = apiClient,
                    rentId = editingRentId.takeIf { it > 0 },
                    eventId = eventId,
                    roomId = roomId,
                    dateIso = dateIso,
                    startTime = startTime,
                    endTime = endTime
                )
                rents = loadEventRentsDetailed(apiClient, eventId)
                clearRentForm()
            }
        }.onFailure {
            actionError = it.userMessage()
        }
        isBusy = false
    }

    LaunchedEffect(deleteNonce) {
        if (deleteNonce == 0 || editingRentId == 0) return@LaunchedEffect
        isBusy = true
        actionError = null
        runCatching {
            if (isDraftMode) {
                onDraftRentsChange(draftRents.filter { it.id != editingRentId })
                clearRentForm()
            } else {
                deleteEventRent(apiClient, editingRentId)
                rents = loadEventRentsDetailed(apiClient, eventId)
                clearRentForm()
            }
        }.onFailure {
            actionError = it.userMessage()
        }
        isBusy = false
    }

    KvantFormSectionTitle("БРОНИРОВАНИЕ")

    if (isDraftMode) {
        Text(
            text = "Бронь можно добавить сразу — она сохранится вместе с мероприятием",
            color = contentColor.copy(alpha = 0.75f),
            fontFamily = Montserrat,
            fontSize = 13.sp
        )
    }

    when {
        isLoading -> {
            CircularProgressIndicator(
                modifier = Modifier.padding(vertical = 12.dp),
                color = contentColor
            )
        }

        loadError != null -> {
            Text(loadError.orEmpty(), color = MaterialTheme.colorScheme.error, fontFamily = Montserrat)
        }

        else -> {
            if (visibleRents.isEmpty()) {
                Text(
                    text = "Нет бронирований",
                    color = contentColor.copy(alpha = 0.7f),
                    fontFamily = Montserrat,
                    fontSize = 15.sp
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    visibleRents.forEach { rent ->
                        val selected = rent.id == editingRentId
                        KvantCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { selectRent(rent) },
                            cornerRadius = 12.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = rent.roomName,
                                        color = contentColor,
                                        fontFamily = Montserrat,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "${rent.startTime} – ${rent.endTime}",
                                        color = contentColor,
                                        fontFamily = Montserrat,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                if (selected) {
                                    Icon(
                                        painter = painterResource(R.drawable.outline_edit_24),
                                        contentDescription = null,
                                        tint = contentColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            KvantInnerCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = when {
                            editingRentId != 0 -> "Редактирование брони"
                            isDraftMode -> "Новая бронь"
                            else -> "Новая бронь"
                        },
                        color = contentColor,
                        fontFamily = Montserrat,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )

                    if (rooms.isNotEmpty()) {
                        KvantFormSelect(
                            label = "Кабинет",
                            selectedLabel = rooms.firstOrNull { it.id == roomId }?.name ?: "— Кабинет —",
                            options = listOf(0 to "— Кабинет —") + rooms.map { it.id to it.name },
                            selectedValue = roomId,
                            onSelected = { roomId = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KvantTimePickerField(
                            label = "Начало",
                            value = startTime,
                            onValueChange = { startTime = it },
                            modifier = Modifier.weight(1f),
                            enabled = !isBusy
                        )
                        KvantTimePickerField(
                            label = "Конец",
                            value = endTime,
                            onValueChange = { endTime = it },
                            modifier = Modifier.weight(1f),
                            enabled = !isBusy
                        )
                    }

                    if (dateIso.isBlank()) {
                        Text(
                            text = "Укажите дату мероприятия выше",
                            color = MaterialTheme.colorScheme.error,
                            fontFamily = Montserrat,
                            fontSize = 13.sp
                        )
                    }

                    actionError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontFamily = Montserrat, fontSize = 13.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { saveNonce++ },
                            enabled = !isBusy,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                if (isDraftMode && editingRentId == 0) "Добавить" else "Сохранить",
                                fontFamily = Montserrat
                            )
                        }
                        if (editingRentId != 0) {
                            OutlinedButton(
                                onClick = { deleteNonce++ },
                                enabled = !isBusy,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    "Удалить",
                                    color = MaterialTheme.colorScheme.error,
                                    fontFamily = Montserrat
                                )
                            }
                        }
                    }

                    if (editingRentId != 0) {
                        Text(
                            text = "Новая бронь",
                            color = kvant.link,
                            fontFamily = Montserrat,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .clickable(enabled = !isBusy) { clearRentForm() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EventRentListScreen(
    apiClient: ApiClient,
    eventId: Int,
    eventName: String,
    eventDate: String,
    onBack: () -> Unit,
    onEditRent: (rentId: Int?) -> Unit
) {
    var rents by remember { mutableStateOf<List<EventRentDetail>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadNonce by remember { mutableIntStateOf(0) }
    val contentColor = kvantContentColor()

    LaunchedEffect(eventId, reloadNonce) {
        isLoading = true
        error = null
        runCatching {
            rents = loadEventRentsDetailed(apiClient, eventId)
        }.onFailure {
            error = it.userMessage()
        }
        isLoading = false
    }

    KvantScreenScaffold(
        modifier = Modifier.fillMaxSize(),
        onBack = onBack,
        title = "БРОНЬ"
    ) {
        Text(
            text = eventName,
            color = contentColor,
            fontFamily = Montserrat,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (eventDate.isNotBlank()) {
            Text(
                text = eventDate,
                color = contentColor,
                fontFamily = Montserrat,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = contentColor)
                }
            }

            error != null -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, fontFamily = Montserrat)
                    Button(onClick = { reloadNonce++ }, modifier = Modifier.padding(top = 12.dp)) {
                        Text("Повторить", fontFamily = Montserrat)
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rents.forEach { rent ->
                        KvantCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onEditRent(rent.id) },
                            cornerRadius = 15.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = rent.roomName,
                                        color = contentColor,
                                        fontFamily = Montserrat,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = "${rent.startTime}-${rent.endTime}",
                                        color = contentColor,
                                        fontFamily = Montserrat,
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                Icon(
                                    painter = painterResource(R.drawable.outline_edit_24),
                                    contentDescription = "Редактировать",
                                    tint = contentColor
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = { onEditRent(null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("ДОБАВИТЬ", fontFamily = Montserrat, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EventRentEditScreen(
    apiClient: ApiClient,
    eventId: Int,
    rentId: Int?,
    eventDate: String,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    var rooms by remember { mutableStateOf<List<EventTypeOption>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var saveNonce by remember { mutableIntStateOf(0) }

    var roomId by remember { mutableIntStateOf(0) }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var dateIso by remember { mutableStateOf("") }

    LaunchedEffect(eventId, rentId) {
        isLoading = true
        error = null
        runCatching {
            rooms = loadRoomOptions(apiClient)
            dateIso = parseUiDateToIso(eventDate).orEmpty()
            if (rentId != null && rentId > 0) {
                val rents = loadEventRentsDetailed(apiClient, eventId)
                val rent = rents.firstOrNull { it.id == rentId }
                if (rent != null) {
                    roomId = rent.roomId
                    startTime = rent.startTime
                    endTime = rent.endTime
                    if (rent.date.isNotBlank()) dateIso = rent.date
                }
            }
        }.onFailure {
            error = it.userMessage()
        }
        isLoading = false
    }

    LaunchedEffect(saveNonce) {
        if (saveNonce == 0) return@LaunchedEffect
        if (roomId <= 0) {
            saveError = "Выберите аудиторию"
            return@LaunchedEffect
        }
        if (startTime.isBlank() || endTime.isBlank()) {
            saveError = "Укажите время начала и окончания"
            return@LaunchedEffect
        }
        if (dateIso.isBlank()) {
            saveError = "Нет даты мероприятия"
            return@LaunchedEffect
        }
        isSaving = true
        saveError = null
        runCatching {
            saveEventRent(
                apiClient = apiClient,
                rentId = rentId,
                eventId = eventId,
                roomId = roomId,
                dateIso = dateIso,
                startTime = startTime,
                endTime = endTime
            )
        }.onSuccess {
            onSaved()
        }.onFailure {
            saveError = it.userMessage()
        }
        isSaving = false
    }

    KvantScreenScaffold(
        modifier = Modifier.fillMaxSize(),
        onBack = onBack,
        title = if (rentId != null) "РЕДАКТИРОВАНИЕ" else "НОВАЯ БРОНЬ"
    ) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = kvantContentColor())
                }
            }

            error != null -> {
                Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, fontFamily = Montserrat)
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KvantTimePickerField(
                        label = "Время начала",
                        value = startTime,
                        onValueChange = { startTime = it },
                        enabled = !isSaving
                    )
                    KvantTimePickerField(
                        label = "Время окончания",
                        value = endTime,
                        onValueChange = { endTime = it },
                        enabled = !isSaving
                    )
                    if (rooms.isNotEmpty()) {
                        KvantFormSelect(
                            label = "Аудитория",
                            selectedLabel = rooms.firstOrNull { it.id == roomId }?.name ?: "Выберите аудиторию",
                            options = rooms.map { it.id to it.name },
                            selectedValue = roomId,
                            onSelected = { roomId = it }
                        )
                    }
                    if (dateIso.isBlank()) {
                        Text(
                            text = "Укажите дату мероприятия в карточке события",
                            color = MaterialTheme.colorScheme.error,
                            fontFamily = Montserrat
                        )
                    }
                    saveError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontFamily = Montserrat)
                    }
                    Button(
                        onClick = { saveNonce++ },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("СОХРАНИТЬ", fontFamily = Montserrat, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
