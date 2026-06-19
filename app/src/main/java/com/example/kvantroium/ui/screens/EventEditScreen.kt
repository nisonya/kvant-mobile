package com.example.kvantroium.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kvantroium.R
import com.example.kvantroium.api.ApiClient
import com.example.kvantroium.features.events.EmployeeOption
import com.example.kvantroium.features.events.EventKind
import com.example.kvantroium.features.events.EventRentDetail
import com.example.kvantroium.features.events.EventResponsibleDetail
import com.example.kvantroium.features.events.EventTypeOption
import com.example.kvantroium.features.events.OrgEventFormState
import com.example.kvantroium.features.events.PartEventFormState
import com.example.kvantroium.features.events.buildOrgEventBody
import com.example.kvantroium.features.events.buildPartEventBody
import com.example.kvantroium.features.events.formatEventDateInput
import com.example.kvantroium.features.events.loadEmployeeOptions
import com.example.kvantroium.features.events.loadEventTypeOptions
import com.example.kvantroium.features.events.loadFormOfHoldingOptions
import com.example.kvantroium.features.events.loadOrgEventForEdit
import com.example.kvantroium.features.events.loadPartEventForEdit
import com.example.kvantroium.features.events.parseUiDateToIso
import com.example.kvantroium.features.events.createOrgEvent
import com.example.kvantroium.features.events.createPartEvent
import com.example.kvantroium.features.events.saveOrgEventEdit
import com.example.kvantroium.features.events.savePartEventEdit
import com.example.kvantroium.features.events.validateOrgForm
import com.example.kvantroium.features.events.validatePartForm
import com.example.kvantroium.features.events.validateResponsibleComment
import com.example.kvantroium.features.events.validateUintField
import com.example.kvantroium.features.events.weekdayFromIsoDate
import com.example.kvantroium.ui.components.KvantDatePickerField
import com.example.kvantroium.ui.components.KvantFormField
import com.example.kvantroium.ui.components.KvantFormSelect
import com.example.kvantroium.ui.components.KvantInnerCard
import com.example.kvantroium.ui.components.KvantScreenScaffold
import com.example.kvantroium.ui.components.ResponsiblePickerDialog
import com.example.kvantroium.ui.components.kvantBottomScreenInset
import com.example.kvantroium.ui.theme.Montserrat
import com.example.kvantroium.ui.theme.kvantContentColor
import com.example.kvantroium.ui.util.userMessage

@Composable
fun EventEditScreen(
    apiClient: ApiClient,
    eventId: Int,
    kind: EventKind,
    isCreateMode: Boolean,
    onBack: () -> Unit,
    onSaved: (Int) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var reloadNonce by remember { mutableIntStateOf(0) }
    var saveNonce by remember { mutableIntStateOf(0) }
    var rentReloadNonce by remember { mutableIntStateOf(0) }

    var employees by remember { mutableStateOf<List<EmployeeOption>>(emptyList()) }
    var orgTypes by remember { mutableStateOf<List<EventTypeOption>>(emptyList()) }
    var partLevels by remember { mutableStateOf<List<EventTypeOption>>(emptyList()) }
    var holdingTypes by remember { mutableStateOf<List<EventTypeOption>>(emptyList()) }

    var orgState by remember { mutableStateOf<OrgEventFormState?>(null) }
    var partState by remember { mutableStateOf<PartEventFormState?>(null) }
    var originalResponsibleIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var originalResponsibles by remember { mutableStateOf<List<EventResponsibleDetail>>(emptyList()) }
    var responsibles by remember { mutableStateOf<List<EventResponsibleDetail>>(emptyList()) }
    var showResponsiblePicker by remember { mutableStateOf(false) }
    var draftRents by remember { mutableStateOf<List<EventRentDetail>>(emptyList()) }
    var baselineSnapshot by remember { mutableStateOf<String?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    var orgName by remember { mutableStateOf("") }
    var orgTypeId by remember { mutableIntStateOf(0) }
    var orgPlace by remember { mutableStateOf("") }
    var orgDate by remember { mutableStateOf("") }
    var orgDayOfWeek by remember { mutableStateOf("") }
    var orgActual by remember { mutableStateOf("0") }
    var orgPlanned by remember { mutableStateOf("0") }
    var orgAnnotation by remember { mutableStateOf("") }
    var orgResult by remember { mutableStateOf("") }
    var orgLink by remember { mutableStateOf("") }

    var partName by remember { mutableStateOf("") }
    var partHoldingId by remember { mutableIntStateOf(0) }
    var partHoldingName by remember { mutableStateOf("") }
    var partLevelId by remember { mutableIntStateOf(0) }
    var partDeadline by remember { mutableStateOf("") }
    var partParticipantsWorks by remember { mutableStateOf("") }
    var partAnnotation by remember { mutableStateOf("") }
    var partDates by remember { mutableStateOf("") }
    var partLink by remember { mutableStateOf("") }
    var partResult by remember { mutableStateOf("") }

    fun buildFormSnapshot(): String {
        val responsiblesPart = responsibles.joinToString(";") { responsible ->
            listOf(
                responsible.employeeId,
                responsible.markSent,
                responsible.participants,
                responsible.winners,
                responsible.runnerUp,
                responsible.comment
            ).joinToString(",")
        }
        val rentsPart = draftRents.joinToString(";") { rent ->
            listOf(rent.id, rent.roomId, rent.date, rent.startTime, rent.endTime).joinToString(",")
        }
        val formPart = if (kind == EventKind.ORG) {
            listOf(
                orgName, orgTypeId, orgPlace, orgDate, orgDayOfWeek, orgActual, orgPlanned,
                orgAnnotation, orgResult, orgLink
            ).joinToString("|")
        } else {
            listOf(
                partName, partHoldingId, partLevelId, partDeadline, partParticipantsWorks,
                partAnnotation, partDates, partLink, partResult
            ).joinToString("|")
        }
        return "$formPart#$responsiblesPart#$rentsPart"
    }

    fun isFormDirty(): Boolean {
        val baseline = baselineSnapshot ?: return false
        return buildFormSnapshot() != baseline
    }

    fun requestBack() {
        if (!isSaving && !isLoading && error == null && isFormDirty()) {
            showDiscardDialog = true
        } else {
            onBack()
        }
    }

    BackHandler { requestBack() }

    LaunchedEffect(eventId, kind, isCreateMode, reloadNonce) {
        isLoading = true
        error = null
        baselineSnapshot = null
        runCatching {
            employees = loadEmployeeOptions(apiClient)
            if (kind == EventKind.ORG) {
                orgTypes = loadEventTypeOptions(apiClient, EventKind.ORG)
                if (isCreateMode) {
                    orgState = null
                    orgName = ""
                    orgTypeId = orgTypes.firstOrNull()?.id ?: 0
                    orgPlace = ""
                    orgDate = ""
                    orgDayOfWeek = ""
                    orgActual = "0"
                    orgPlanned = "0"
                    orgAnnotation = ""
                    orgResult = ""
                    orgLink = ""
                    responsibles = emptyList()
                    originalResponsibleIds = emptySet()
                    originalResponsibles = emptyList()
                    draftRents = emptyList()
                } else {
                    val loaded = loadOrgEventForEdit(apiClient, eventId)
                    orgState = loaded
                    orgName = loaded.name
                    orgTypeId = loaded.typeId ?: 0
                    orgPlace = loaded.formOfHolding
                    orgDate = formatEventDateInput(loaded.datesIso)
                    orgDayOfWeek = loaded.dayOfWeek
                    orgActual = loaded.actualParticipants.toString()
                    orgPlanned = loaded.plannedParticipants.toString()
                    orgAnnotation = loaded.annotation
                    orgResult = loaded.result
                    orgLink = loaded.link
                    responsibles = loaded.responsibles
                    originalResponsibleIds = loaded.responsibles.map { it.employeeId }.toSet()
                    originalResponsibles = loaded.responsibles
                }
            } else {
                partLevels = loadEventTypeOptions(apiClient, EventKind.PART)
                holdingTypes = loadFormOfHoldingOptions(apiClient)
                if (isCreateMode) {
                    partState = null
                    partName = ""
                    partHoldingId = holdingTypes.firstOrNull()?.id ?: 0
                    partHoldingName = holdingTypes.firstOrNull()?.name.orEmpty()
                    partLevelId = partLevels.firstOrNull()?.id ?: 0
                    partDeadline = ""
                    partParticipantsWorks = ""
                    partAnnotation = ""
                    partDates = ""
                    partLink = ""
                    partResult = ""
                    responsibles = emptyList()
                    originalResponsibleIds = emptySet()
                    originalResponsibles = emptyList()
                } else {
                    val loaded = loadPartEventForEdit(apiClient, eventId)
                    partState = loaded
                    partName = loaded.name
                    partHoldingId = loaded.formOfHoldingId ?: 0
                    partHoldingName = loaded.formOfHoldingName
                    partLevelId = loaded.typeId ?: 0
                    partDeadline = formatEventDateInput(loaded.registrationDeadlineIso)
                    partParticipantsWorks = loaded.participantsAndWorks
                    partAnnotation = loaded.annotation
                    partDates = loaded.datesOfEvent
                    partLink = loaded.link
                    partResult = loaded.result
                    responsibles = loaded.responsibles
                    originalResponsibleIds = loaded.responsibles.map { it.employeeId }.toSet()
                    originalResponsibles = loaded.responsibles
                }
            }
        }.onFailure {
            error = it.userMessage()
        }
        if (error == null) {
            baselineSnapshot = buildFormSnapshot()
        }
        isLoading = false
    }

    LaunchedEffect(saveNonce) {
        if (saveNonce == 0) return@LaunchedEffect
        isSaving = true
        saveError = null
        runCatching {
            if (kind == EventKind.ORG) {
                val validation = validateOrgForm(
                    orgName, orgPlace, orgDayOfWeek, orgResult, orgAnnotation, orgLink,
                    orgActual, orgPlanned
                )
                if (validation != null) throw IllegalArgumentException(validation)
                val datesIso = parseUiDateToIso(orgDate).orEmpty()
                val body = buildOrgEventBody(
                    name = orgName,
                    typeId = orgTypeId.takeIf { it > 0 },
                    formOfHolding = orgPlace,
                    datesIso = datesIso,
                    dayOfWeek = orgDayOfWeek,
                    actualParticipants = orgActual.toIntOrNull() ?: 0,
                    plannedParticipants = orgPlanned.toIntOrNull() ?: 0,
                    annotation = orgAnnotation,
                    result = orgResult,
                    link = orgLink
                )
                if (isCreateMode) {
                    createOrgEvent(
                        apiClient = apiClient,
                        body = body,
                        responsibles = responsibles,
                        pendingRents = draftRents,
                        rentDateIso = datesIso
                    )
                } else {
                    saveOrgEventEdit(
                        apiClient = apiClient,
                        eventId = eventId,
                        body = body,
                        originalResponsibleIds = originalResponsibleIds,
                        newResponsibles = responsibles
                    )
                    eventId
                }
            } else {
                responsibles.forEach { responsible ->
                    validateResponsibleComment(responsible.comment)?.let {
                        throw IllegalArgumentException(it)
                    }
                    listOf(
                        responsible.participants.toString() to "Участвовавшие",
                        responsible.winners.toString() to "Победители",
                        responsible.runnerUp.toString() to "Призёры"
                    ).forEach { (value, label) ->
                        validateUintField(value, label)?.let { throw IllegalArgumentException(it) }
                    }
                }
                val validation = validatePartForm(
                    partName, partDeadline, partParticipantsWorks, partDates,
                    partResult, partAnnotation, partLink, partLevelId.takeIf { it > 0 }
                )
                if (validation != null) throw IllegalArgumentException(validation)
                val body = buildPartEventBody(
                    name = partName,
                    formOfHoldingId = partHoldingId.takeIf { it > 0 },
                    typeId = partLevelId,
                    registrationDeadlineIso = parseUiDateToIso(partDeadline),
                    participantsAndWorks = partParticipantsWorks,
                    annotation = partAnnotation,
                    datesOfEvent = partDates,
                    link = partLink,
                    participantsAmount = partState?.participantsAmount ?: 0,
                    winnerAmount = partState?.winnerAmount ?: 0,
                    runnerUpAmount = partState?.runnerUpAmount ?: 0,
                    result = partResult
                )
                if (isCreateMode) {
                    createPartEvent(apiClient, body, responsibles)
                } else {
                    savePartEventEdit(
                        apiClient = apiClient,
                        eventId = eventId,
                        body = body,
                        originalResponsibles = originalResponsibles,
                        newResponsibles = responsibles
                    )
                    eventId
                }
            }
        }.onSuccess { savedId ->
            onSaved(savedId)
        }.onFailure {
            saveError = it.userMessage()
        }
        isSaving = false
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Отменить изменения?", fontFamily = Montserrat) },
            text = { Text("Несохранённые данные будут потеряны.", fontFamily = Montserrat) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onBack()
                }) {
                    Text("Выйти", fontFamily = Montserrat)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Остаться", fontFamily = Montserrat)
                }
            }
        )
    }

    if (showResponsiblePicker) {
        ResponsiblePickerDialog(
            employees = employees,
            selectedIds = responsibles.map { it.employeeId }.toSet(),
            onDismiss = { showResponsiblePicker = false },
            onConfirm = { selected ->
                val existing = responsibles.associateBy { it.employeeId }
                responsibles = selected.map { id ->
                    existing[id] ?: EmployeeOption(id, employees.firstOrNull { it.id == id }?.name.orEmpty())
                        .let { EventResponsibleDetail(employeeId = id, name = it.name) }
                }
                showResponsiblePicker = false
            }
        )
    }

    KvantScreenScaffold(
        modifier = Modifier.fillMaxSize(),
        onBack = { requestBack() },
        title = if (isCreateMode) "СОЗДАНИЕ" else "РЕДАКТИРОВАНИЕ"
    ) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = kvantContentColor())
                }
            }

            error != null -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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
                        .kvantBottomScreenInset(extra = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (kind == EventKind.ORG) {
                        OrgEditFields(
                            orgTypes = orgTypes,
                            orgTypeId = orgTypeId,
                            onTypeSelected = { orgTypeId = it },
                            orgName = orgName,
                            onNameChange = { orgName = it },
                            orgPlace = orgPlace,
                            onPlaceChange = { orgPlace = it },
                            orgDate = orgDate,
                            onDateChange = { value ->
                                orgDate = value
                                parseUiDateToIso(value)?.let { iso ->
                                    orgDayOfWeek = weekdayFromIsoDate(iso)
                                }
                            },
                            orgDayOfWeek = orgDayOfWeek,
                            orgActual = orgActual,
                            onActualChange = { orgActual = it.filter { ch -> ch.isDigit() } },
                            orgPlanned = orgPlanned,
                            onPlannedChange = { orgPlanned = it.filter { ch -> ch.isDigit() } },
                            orgAnnotation = orgAnnotation,
                            onAnnotationChange = { orgAnnotation = it },
                            orgResult = orgResult,
                            onResultChange = { orgResult = it },
                            orgLink = orgLink,
                            onLinkChange = { orgLink = it }
                        )
                        OrgRentInlineSection(
                            apiClient = apiClient,
                            eventId = if (isCreateMode) 0 else eventId,
                            eventDateUi = orgDate,
                            reloadNonce = rentReloadNonce,
                            onReloadHandled = { rentReloadNonce = 0 },
                            draftRents = draftRents,
                            onDraftRentsChange = { draftRents = it }
                        )
                    } else {
                        PartEditFields(
                            holdingTypes = holdingTypes,
                            partLevels = partLevels,
                            partHoldingId = partHoldingId,
                            partHoldingName = partHoldingName,
                            onHoldingSelected = { id ->
                                partHoldingId = id
                                partHoldingName = holdingTypes.firstOrNull { it.id == id }?.name.orEmpty()
                            },
                            partLevelId = partLevelId,
                            onLevelSelected = { partLevelId = it },
                            partName = partName,
                            onNameChange = { partName = it },
                            partDeadline = partDeadline,
                            onDeadlineChange = { partDeadline = it },
                            partParticipantsWorks = partParticipantsWorks,
                            onParticipantsWorksChange = { partParticipantsWorks = it },
                            partDates = partDates,
                            onDatesChange = { partDates = it },
                            partAnnotation = partAnnotation,
                            onAnnotationChange = { partAnnotation = it },
                            partLink = partLink,
                            onLinkChange = { partLink = it },
                            partResult = partResult,
                            onResultChange = { partResult = it },
                            participantsAmount = partState?.participantsAmount ?: 0,
                            winnerAmount = partState?.winnerAmount ?: 0,
                            runnerUpAmount = partState?.runnerUpAmount ?: 0
                        )
                    }

                    ResponsiblesSection(
                        kind = kind,
                        responsibles = responsibles,
                        onAddRemove = { showResponsiblePicker = true },
                        onResponsibleChange = { updated ->
                            responsibles = responsibles.map {
                                if (it.employeeId == updated.employeeId) updated else it
                            }
                        }
                    )

                    saveError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontFamily = Montserrat)
                    }

                    Button(
                        onClick = { saveNonce++ },
                        enabled = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                        } else {
                            Text(
                                text = if (isCreateMode) "СОЗДАТЬ" else "СОХРАНИТЬ",
                                fontFamily = Montserrat,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrgEditFields(
    orgTypes: List<EventTypeOption>,
    orgTypeId: Int,
    onTypeSelected: (Int) -> Unit,
    orgName: String,
    onNameChange: (String) -> Unit,
    orgPlace: String,
    onPlaceChange: (String) -> Unit,
    orgDate: String,
    onDateChange: (String) -> Unit,
    orgDayOfWeek: String,
    orgActual: String,
    onActualChange: (String) -> Unit,
    orgPlanned: String,
    onPlannedChange: (String) -> Unit,
    orgAnnotation: String,
    onAnnotationChange: (String) -> Unit,
    orgResult: String,
    onResultChange: (String) -> Unit,
    orgLink: String,
    onLinkChange: (String) -> Unit
) {
    KvantFormField("Название", orgName, onNameChange)
    if (orgTypes.isNotEmpty()) {
        KvantFormSelect(
            label = "Тип",
            selectedLabel = orgTypes.firstOrNull { it.id == orgTypeId }?.name ?: "Выберите тип",
            options = listOf(0 to "Не выбран") + orgTypes.map { it.id to it.name },
            selectedValue = orgTypeId,
            onSelected = onTypeSelected
        )
    }
    KvantFormField("Место", orgPlace, onPlaceChange)
    KvantDatePickerField(
        label = "Дата проведения",
        value = orgDate,
        onValueChange = onDateChange
    )
    KvantFormField("День недели", orgDayOfWeek, {}, readOnly = true)
    KvantFormField(
        "Фактическое кол-во",
        orgActual,
        onActualChange,
        singleLine = true
    )
    KvantFormField("Планируемое кол-во", orgPlanned, onPlannedChange)
    KvantFormField("Примечания", orgAnnotation, onAnnotationChange, singleLine = false, maxLines = 4)
    KvantFormField("Результат", orgResult, onResultChange)
    KvantFormField("Ссылка", orgLink, onLinkChange)
}

@Composable
private fun PartEditFields(
    holdingTypes: List<EventTypeOption>,
    partLevels: List<EventTypeOption>,
    partHoldingId: Int,
    partHoldingName: String,
    onHoldingSelected: (Int) -> Unit,
    partLevelId: Int,
    onLevelSelected: (Int) -> Unit,
    partName: String,
    onNameChange: (String) -> Unit,
    partDeadline: String,
    onDeadlineChange: (String) -> Unit,
    partParticipantsWorks: String,
    onParticipantsWorksChange: (String) -> Unit,
    partDates: String,
    onDatesChange: (String) -> Unit,
    partAnnotation: String,
    onAnnotationChange: (String) -> Unit,
    partLink: String,
    onLinkChange: (String) -> Unit,
    partResult: String,
    onResultChange: (String) -> Unit,
    participantsAmount: Int,
    winnerAmount: Int,
    runnerUpAmount: Int
) {
    KvantFormField("Название", partName, onNameChange)
    if (holdingTypes.isNotEmpty()) {
        KvantFormSelect(
            label = "Форма проведения",
            selectedLabel = holdingTypes.firstOrNull { it.id == partHoldingId }?.name
                ?: partHoldingName.ifBlank { "Выберите форму" },
            options = listOf(0 to "Не выбрана") + holdingTypes.map { it.id to it.name },
            selectedValue = partHoldingId,
            onSelected = onHoldingSelected
        )
    } else if (partHoldingName.isNotBlank()) {
        KvantFormField("Форма проведения", partHoldingName, {}, readOnly = true)
    }
    if (partLevels.isNotEmpty()) {
        KvantFormSelect(
            label = "Уровень",
            selectedLabel = partLevels.firstOrNull { it.id == partLevelId }?.name ?: "Выберите уровень",
            options = partLevels.map { it.id to it.name },
            selectedValue = partLevelId,
            onSelected = onLevelSelected
        )
    }
    KvantDatePickerField(
        label = "Регистрация до",
        value = partDeadline,
        onValueChange = onDeadlineChange
    )
    KvantFormField("Участники и работы", partParticipantsWorks, onParticipantsWorksChange, singleLine = false, maxLines = 3)
    KvantFormField("Даты проведения", partDates, onDatesChange)
    KvantFormField("Примечания", partAnnotation, onAnnotationChange, singleLine = false, maxLines = 4)
    KvantFormField("Ссылка", partLink, onLinkChange)
    KvantFormField("Результат", partResult, onResultChange)
    KvantFormField("Кол-во участников", participantsAmount.toString(), {}, readOnly = true)
    KvantFormField("Победители", winnerAmount.toString(), {}, readOnly = true)
    KvantFormField("Призёры", runnerUpAmount.toString(), {}, readOnly = true)
}

@Composable
private fun ResponsiblesSection(
    kind: EventKind,
    responsibles: List<EventResponsibleDetail>,
    onAddRemove: () -> Unit,
    onResponsibleChange: (EventResponsibleDetail) -> Unit
) {
    val contentColor = kvantContentColor()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "ОТВЕТСТВЕННЫЕ",
            color = contentColor,
            fontFamily = Montserrat,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onAddRemove) {
            Icon(
                painter = painterResource(R.drawable.outline_edit_24),
                contentDescription = "Добавить или удалить ответственных",
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
    responsibles.forEach { responsible ->
        KvantInnerCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = responsible.name,
                    color = contentColor,
                    fontFamily = Montserrat,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (kind == EventKind.PART) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Участвовал", color = contentColor, fontFamily = Montserrat)
                        Switch(
                            checked = responsible.markSent,
                            onCheckedChange = { checked ->
                                onResponsibleChange(responsible.copy(markSent = checked))
                            }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KvantFormField(
                            label = "Участв.",
                            value = responsible.participants.toString(),
                            onValueChange = { value ->
                                onResponsibleChange(
                                    responsible.copy(participants = value.filter { it.isDigit() }.toIntOrNull() ?: 0)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                        KvantFormField(
                            label = "Призёры",
                            value = responsible.runnerUp.toString(),
                            onValueChange = { value ->
                                onResponsibleChange(
                                    responsible.copy(runnerUp = value.filter { it.isDigit() }.toIntOrNull() ?: 0)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                        KvantFormField(
                            label = "Побед.",
                            value = responsible.winners.toString(),
                            onValueChange = { value ->
                                onResponsibleChange(
                                    responsible.copy(winners = value.filter { it.isDigit() }.toIntOrNull() ?: 0)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    KvantFormField(
                        label = "Комментарий (до 250 симв.)",
                        value = responsible.comment,
                        onValueChange = { onResponsibleChange(responsible.copy(comment = it.take(250))) },
                        singleLine = false,
                        maxLines = 3
                    )
                }
            }
        }
    }
}
