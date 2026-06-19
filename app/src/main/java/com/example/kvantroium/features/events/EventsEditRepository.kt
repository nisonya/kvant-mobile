package com.example.kvantroium.features.events

import com.example.kvantroium.api.ApiClient
import com.example.kvantroium.api.ApiPaths
import org.json.JSONArray
import org.json.JSONObject

// --- Write / extended read operations for event editing ---

data class EventResponsibleDetail(
    val employeeId: Int,
    val name: String,
    val markSent: Boolean = false,
    val participants: Int = 0,
    val winners: Int = 0,
    val runnerUp: Int = 0,
    val comment: String = ""
)

data class EventRentDetail(
    val id: Int,
    val roomId: Int,
    val roomName: String,
    val date: String,
    val startTime: String,
    val endTime: String
)

data class OrgEventFormState(
    val id: Int,
    val name: String,
    val typeId: Int?,
    val formOfHolding: String,
    val datesIso: String,
    val dayOfWeek: String,
    val actualParticipants: Int,
    val plannedParticipants: Int,
    val annotation: String,
    val result: String,
    val link: String,
    val responsibles: List<EventResponsibleDetail>,
    val rents: List<EventRentDetail>
)

data class PartEventFormState(
    val id: Int,
    val name: String,
    val formOfHoldingId: Int?,
    val formOfHoldingName: String,
    val typeId: Int?,
    val registrationDeadlineIso: String,
    val participantsAndWorks: String,
    val annotation: String,
    val datesOfEvent: String,
    val link: String,
    val participantsAmount: Int,
    val winnerAmount: Int,
    val runnerUpAmount: Int,
    val result: String,
    val responsibles: List<EventResponsibleDetail>
)

suspend fun loadRoomOptions(apiClient: ApiClient): List<EventTypeOption> {
    return runCatching {
        parseReferenceOptions(apiClient.apiRequest("GET", ApiPaths.Reference.ROOMS))
    }.getOrDefault(emptyList())
}

suspend fun loadFormOfHoldingOptions(apiClient: ApiClient): List<EventTypeOption> {
    for (path in ApiPaths.Reference.TYPES_OF_HOLDING_TRY) {
        val options = runCatching {
            parseReferenceOptions(apiClient.apiRequest("GET", path))
        }.getOrDefault(emptyList())
        if (options.isNotEmpty()) return options
    }
    return emptyList()
}

suspend fun loadOrgEventForEdit(apiClient: ApiClient, eventId: Int): OrgEventFormState {
    val response = apiClient.apiRequest("GET", ApiPaths.Events.orgFullInfo(eventId))
    val item = response.optJSONObject("data") ?: response
    val responsibles = loadOrgResponsiblesDetailed(apiClient, eventId)
    val rents = loadEventRentsDetailed(apiClient, eventId)
    val typeRaw = item.opt("type")
    val typeId = when (typeRaw) {
        is Number -> typeRaw.toInt().takeIf { it > 0 }
        else -> item.optString("type", "").toIntOrNull()?.takeIf { it > 0 }
    }
    return OrgEventFormState(
        id = eventId,
        name = item.optString("name", ""),
        typeId = typeId,
        formOfHolding = item.optString("form_of_holding", ""),
        datesIso = item.optString("dates_of_event", ""),
        dayOfWeek = item.optString("day_of_the_week", "").uppercase(),
        actualParticipants = item.optInt("amount_of_applications", 0),
        plannedParticipants = item.optInt("amount_of_planning_application", 0),
        annotation = item.optString("annotation", ""),
        result = item.optString("result", ""),
        link = item.optString("link", ""),
        responsibles = responsibles,
        rents = rents
    )
}

suspend fun loadPartEventForEdit(apiClient: ApiClient, eventId: Int): PartEventFormState {
    val response = apiClient.apiRequest("GET", ApiPaths.Events.partFullInfo(eventId))
    val item = response.optJSONObject("data") ?: response
    val responsibles = loadPartResponsiblesDetailed(apiClient, eventId)
    val formId = item.opt("form_of_holding")
    val formOfHoldingId = when (formId) {
        is Number -> formId.toInt().takeIf { it > 0 }
        else -> item.optString("form_of_holding", "").toIntOrNull()?.takeIf { it > 0 }
    }
    val holdingOptions = loadFormOfHoldingOptions(apiClient)
    return PartEventFormState(
        id = eventId,
        name = item.optString("name", ""),
        formOfHoldingId = formOfHoldingId,
        formOfHoldingName = resolveFormOfHoldingDisplay(formId, holdingOptions),
        typeId = item.optInt("id_type", 0).takeIf { it > 0 },
        registrationDeadlineIso = item.optString("registration_deadline", ""),
        participantsAndWorks = item.optString("participants_and_works", ""),
        annotation = item.optString("annotation", ""),
        datesOfEvent = item.optString("dates_of_event", ""),
        link = item.optString("link", ""),
        participantsAmount = item.optInt("participants_amount", 0),
        winnerAmount = item.optInt("winner_amount", 0),
        runnerUpAmount = item.optInt("runner_up_amount", 0),
        result = item.optString("result", ""),
        responsibles = responsibles
    )
}

suspend fun loadOrgResponsiblesDetailed(apiClient: ApiClient, eventId: Int): List<EventResponsibleDetail> {
    return runCatching {
        val response = apiClient.apiRequest("GET", ApiPaths.Events.orgResponsible(eventId))
        parseResponsibleArray(response.optJSONArray("data") ?: JSONArray(), includePartFields = false)
    }.getOrDefault(emptyList())
}

suspend fun loadPartResponsiblesDetailed(apiClient: ApiClient, eventId: Int): List<EventResponsibleDetail> {
    val path = ApiPaths.Events.partResponsibleNew(eventId)
    val response = runCatching { apiClient.apiRequest("GET", path) }.getOrElse {
        apiClient.apiRequest("GET", ApiPaths.Events.partResponsible(eventId))
    }
    return parseResponsibleArray(response.optJSONArray("data") ?: JSONArray(), includePartFields = true)
}

private fun parseResponsibleArray(array: JSONArray, includePartFields: Boolean): List<EventResponsibleDetail> {
    return buildList {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val employeeId = item.optInt("id_employees", item.optInt("id_employee", 0))
            if (employeeId <= 0) continue
            val name = responsibleDisplayName(item)
            add(
                EventResponsibleDetail(
                    employeeId = employeeId,
                    name = name,
                    markSent = includePartFields && item.optInt("mark_of_sending_an_application", 0) == 1,
                    participants = if (includePartFields) item.optInt("responsible_participants", 0) else 0,
                    winners = if (includePartFields) item.optInt("responsible_winners", 0) else 0,
                    runnerUp = if (includePartFields) item.optInt("responsible_runner_up", 0) else 0,
                    comment = if (includePartFields) {
                        item.optString("result_of_responsible", item.optString("comment", ""))
                    } else {
                        ""
                    }
                )
            )
        }
    }
}

suspend fun loadEventRentsDetailed(apiClient: ApiClient, eventId: Int): List<EventRentDetail> {
    return runCatching {
        val response = apiClient.apiRequest("GET", ApiPaths.Rent.byEvent(eventId))
        val array = response.optJSONArray("data") ?: JSONArray()
        buildList {
            for (index in 0 until array.length()) {
                val rent = array.optJSONObject(index) ?: continue
                val id = rent.optInt("id_rent", rent.optInt("id", 0))
                if (id <= 0) continue
                add(
                    EventRentDetail(
                        id = id,
                        roomId = rent.optInt("id_room", 0),
                        roomName = rent.optString("name", rent.optString("room_name", "")),
                        date = rent.optString("date", ""),
                        startTime = formatTimeForDisplay(rent.optString("start_time", "")),
                        endTime = formatTimeForDisplay(rent.optString("end_time", ""))
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
}

private fun extractNewEventId(response: org.json.JSONObject): Int {
    val data = response.optJSONObject("data") ?: response
    return data.optInt("id", 0).takeIf { it > 0 }
        ?: throw IllegalArgumentException("Сервер не вернул id мероприятия")
}

suspend fun syncPendingEventRents(
    apiClient: ApiClient,
    eventId: Int,
    dateIso: String,
    rents: List<EventRentDetail>
) {
    if (rents.isEmpty()) return
    if (dateIso.isBlank()) {
        throw IllegalArgumentException("Укажите дату мероприятия для сохранения брони")
    }
    rents.forEach { rent ->
        saveEventRent(
            apiClient = apiClient,
            rentId = null,
            eventId = eventId,
            roomId = rent.roomId,
            dateIso = dateIso,
            startTime = rent.startTime,
            endTime = rent.endTime
        )
    }
}

suspend fun createOrgEvent(
    apiClient: ApiClient,
    body: JSONObject,
    responsibles: List<EventResponsibleDetail>,
    pendingRents: List<EventRentDetail> = emptyList(),
    rentDateIso: String = ""
): Int {
    val newId = extractNewEventId(apiClient.apiRequest("POST", ApiPaths.Events.ORG, body))
    saveOrgEventEdit(
        apiClient = apiClient,
        eventId = newId,
        body = body,
        originalResponsibleIds = emptySet(),
        newResponsibles = responsibles
    )
    syncPendingEventRents(apiClient, newId, rentDateIso, pendingRents)
    return newId
}

suspend fun createPartEvent(
    apiClient: ApiClient,
    body: JSONObject,
    responsibles: List<EventResponsibleDetail>
): Int {
    val newId = extractNewEventId(apiClient.apiRequest("POST", ApiPaths.Events.PART, body))
    savePartEventEdit(
        apiClient = apiClient,
        eventId = newId,
        body = body,
        originalResponsibles = emptyList(),
        newResponsibles = responsibles
    )
    return newId
}

suspend fun saveOrgEventEdit(
    apiClient: ApiClient,
    eventId: Int,
    body: JSONObject,
    originalResponsibleIds: Set<Int>,
    newResponsibles: List<EventResponsibleDetail>
) {
    apiClient.apiRequest("PUT", ApiPaths.Events.ORG, body.put("id", eventId))
    syncResponsibleIds(
        apiClient = apiClient,
        kind = EventKind.ORG,
        eventId = eventId,
        originalIds = originalResponsibleIds,
        newIds = newResponsibles.map { it.employeeId }.toSet()
    )
}

suspend fun savePartEventEdit(
    apiClient: ApiClient,
    eventId: Int,
    body: JSONObject,
    originalResponsibles: List<EventResponsibleDetail>,
    newResponsibles: List<EventResponsibleDetail>
) {
    syncResponsibleIds(
        apiClient = apiClient,
        kind = EventKind.PART,
        eventId = eventId,
        originalIds = originalResponsibles.map { it.employeeId }.toSet(),
        newIds = newResponsibles.map { it.employeeId }.toSet()
    )
    val originalById = originalResponsibles.associateBy { it.employeeId }
    newResponsibles.forEach { responsible ->
        val previous = originalById[responsible.employeeId]
        if (previous == null || previous.markSent != responsible.markSent) {
            apiClient.apiRequest(
                "PUT",
                ApiPaths.Events.PART_MARK,
                JSONObject()
                    .put("id_event", eventId)
                    .put("id_employee", responsible.employeeId)
                    .put("mark_of_sending_an_application", if (responsible.markSent) 1 else 0)
            )
        }
        if (previous == null ||
            previous.participants != responsible.participants ||
            previous.winners != responsible.winners ||
            previous.runnerUp != responsible.runnerUp ||
            previous.comment != responsible.comment
        ) {
            apiClient.apiRequest(
                "PUT",
                ApiPaths.Events.PART_RESULT,
                JSONObject()
                    .put("id_event", eventId)
                    .put("id_employee", responsible.employeeId)
                    .put("result_of_responsible", responsible.comment)
                    .put("responsible_participants", responsible.participants)
                    .put("responsible_winners", responsible.winners)
                    .put("responsible_runner_up", responsible.runnerUp)
            )
        }
    }
    apiClient.apiRequest("PUT", ApiPaths.Events.PART, body.put("id", eventId))
}

private suspend fun syncResponsibleIds(
    apiClient: ApiClient,
    kind: EventKind,
    eventId: Int,
    originalIds: Set<Int>,
    newIds: Set<Int>
) {
    val path = if (kind == EventKind.ORG) ApiPaths.Events.ORG_RESPONSIBLE else ApiPaths.Events.PART_RESPONSIBLE
    (originalIds - newIds).forEach { employeeId ->
        apiClient.apiRequest(
            "DELETE",
            path,
            JSONObject()
                .put("id_event", eventId)
                .put("id_employee", employeeId)
        )
    }
    (newIds - originalIds).forEach { employeeId ->
        apiClient.apiRequest(
            "POST",
            path,
            JSONObject()
                .put("id_event", eventId)
                .put("id_employee", employeeId)
        )
    }
}

suspend fun saveEventRent(
    apiClient: ApiClient,
    rentId: Int?,
    eventId: Int,
    roomId: Int,
    dateIso: String,
    startTime: String,
    endTime: String
) {
    val body = JSONObject()
        .put("event_id", eventId)
        .put("room_id", roomId)
        .put("date", dateIso)
        .put("start_time", formatTimeForApi(startTime))
        .put("end_time", formatTimeForApi(endTime))
    if (rentId != null && rentId > 0) {
        apiClient.apiRequest("PUT", ApiPaths.Rent.ROOT, body.put("id", rentId))
    } else {
        apiClient.apiRequest("POST", ApiPaths.Rent.ROOT, body)
    }
}

suspend fun deleteEventRent(apiClient: ApiClient, rentId: Int) {
    apiClient.apiRequest("DELETE", ApiPaths.Rent.byId(rentId), null)
}

fun buildOrgEventBody(
    name: String,
    typeId: Int?,
    formOfHolding: String,
    datesIso: String?,
    dayOfWeek: String,
    actualParticipants: Int,
    plannedParticipants: Int,
    annotation: String,
    result: String,
    link: String
): JSONObject {
    return JSONObject()
        .put("name", name.trim())
        .put("type", typeId ?: 0)
        .put("form_of_holding", formOfHolding)
        .put("dates_of_event", datesIso.orEmpty())
        .put("day_of_the_week", dayOfWeek)
        .put("amount_of_applications", actualParticipants)
        .put("amount_of_planning_application", plannedParticipants)
        .put("annotation", annotation)
        .put("result", result)
        .put("link", link)
}

fun buildPartEventBody(
    name: String,
    formOfHoldingId: Int?,
    typeId: Int,
    registrationDeadlineIso: String?,
    participantsAndWorks: String,
    annotation: String,
    datesOfEvent: String,
    link: String,
    participantsAmount: Int,
    winnerAmount: Int,
    runnerUpAmount: Int,
    result: String
): JSONObject {
    return JSONObject()
        .put("name", name.trim())
        .put("form_of_holding", formOfHoldingId?.toString().orEmpty())
        .put("id_type", typeId)
        .put("registration_deadline", registrationDeadlineIso.orEmpty())
        .put("participants_and_works", participantsAndWorks)
        .put("annotation", annotation)
        .put("dates_of_event", datesOfEvent)
        .put("link", link)
        .put("participants_amount", participantsAmount)
        .put("winner_amount", winnerAmount)
        .put("runner_up_amount", runnerUpAmount)
        .put("result", result)
}
