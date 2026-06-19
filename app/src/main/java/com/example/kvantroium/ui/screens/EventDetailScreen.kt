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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.example.kvantroium.features.events.EventKind
import com.example.kvantroium.features.events.EventOrgDetail
import com.example.kvantroium.features.events.EventPartDetail
import com.example.kvantroium.features.events.EventRentItem
import com.example.kvantroium.features.events.EventResponsibleDetail
import com.example.kvantroium.features.events.loadEventOrgDetail
import com.example.kvantroium.features.events.loadEventPartDetail
import com.example.kvantroium.features.events.loadPartResponsiblesDetailed
import com.example.kvantroium.ui.components.EventDocumentsSection
import com.example.kvantroium.ui.components.KvantCard
import com.example.kvantroium.ui.components.KvantDetailSection
import com.example.kvantroium.ui.components.KvantInnerCard
import com.example.kvantroium.ui.components.KvantScreenScaffold
import com.example.kvantroium.ui.components.PersonChipsRow
import com.example.kvantroium.ui.components.kvantBottomScreenInset
import com.example.kvantroium.ui.theme.Montserrat
import com.example.kvantroium.ui.theme.kvantColors
import com.example.kvantroium.ui.theme.kvantContentColor
import com.example.kvantroium.ui.util.userMessage

private val DetailBottomPadding = 96.dp
private val DetailSectionSpacing = 12.dp

@Composable
fun EventDetailScreen(
    apiClient: ApiClient,
    eventId: Int,
    kind: EventKind,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    openUrl: (String) -> Unit,
    reloadNonce: Int = 0
) {
    var orgDetail by remember { mutableStateOf<EventOrgDetail?>(null) }
    var partDetail by remember { mutableStateOf<EventPartDetail?>(null) }
    var partResponsibles by remember { mutableStateOf<List<EventResponsibleDetail>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadNonce by remember { mutableIntStateOf(0) }
    val kvant = kvantColors()

    LaunchedEffect(eventId, kind, reloadNonce) {
        isLoading = true
        error = null
        orgDetail = null
        partDetail = null
        partResponsibles = emptyList()
        runCatching {
            if (kind == EventKind.ORG) {
                orgDetail = loadEventOrgDetail(apiClient, eventId)
            } else {
                partDetail = loadEventPartDetail(apiClient, eventId)
                partResponsibles = loadPartResponsiblesDetailed(apiClient, eventId)
            }
        }.onFailure {
            error = it.userMessage()
        }
        isLoading = false
    }

    val title = when (kind) {
        EventKind.ORG -> orgDetail?.name?.uppercase().orEmpty()
        EventKind.PART -> partDetail?.name?.uppercase().orEmpty()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (!isLoading && error == null) {
                FloatingActionButton(
                    onClick = onEdit,
                    containerColor = kvant.fabBackground,
                    contentColor = kvant.fabIcon
                ) {
                    Icon(
                        painter = painterResource(R.drawable.outline_edit_24),
                        contentDescription = "Редактировать"
                    )
                }
            }
        }
    ) { padding ->
        KvantScreenScaffold(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            onBack = onBack,
            title = title.takeIf { it.isNotBlank() }
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, fontFamily = Montserrat)
                        Button(
                            onClick = { reloadNonce++ },
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            Text("Повторить", fontFamily = Montserrat)
                        }
                    }
                }

                kind == EventKind.ORG && orgDetail != null -> {
                    OrgEventDetailContent(
                        detail = orgDetail!!,
                        apiClient = apiClient,
                        eventId = eventId,
                        kind = kind
                    )
                }

                kind == EventKind.PART && partDetail != null -> {
                    PartEventDetailContent(
                        detail = partDetail!!,
                        responsibles = partResponsibles,
                        apiClient = apiClient,
                        eventId = eventId,
                        kind = kind,
                        openUrl = openUrl
                    )
                }
            }
        }
    }
}

@Composable
private fun OrgEventDetailContent(
    detail: EventOrgDetail,
    apiClient: ApiClient,
    eventId: Int,
    kind: EventKind
) {
    val contentColor = kvantContentColor()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .kvantBottomScreenInset(extra = DetailBottomPadding),
        verticalArrangement = Arrangement.spacedBy(DetailSectionSpacing)
    ) {
        if (detail.datesOfEvent.isNotBlank()) {
            Text(
                text = detail.datesOfEvent,
                color = contentColor,
                fontFamily = Montserrat,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (detail.dayOfWeek.isNotBlank()) {
            Text(
                text = detail.dayOfWeek,
                color = contentColor,
                fontFamily = Montserrat,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (detail.responsibles.isNotEmpty()) {
            PersonChipsRow(names = detail.responsibles.map { it.name })
        }

        KvantDetailSection(label = "Место:", value = detail.place)
        ParticipantsBlock(
            planned = detail.plannedParticipants,
            actual = detail.actualParticipants
        )

        if (detail.rents.isNotEmpty()) {
            BookingCard(rents = detail.rents)
        }

        KvantDetailSection(label = "Результат:", value = detail.result)
        KvantDetailSection(label = "Примечания:", value = detail.annotation)

        EventDocumentsSection(
            apiClient = apiClient,
            eventId = eventId,
            kind = kind
        )
    }
}

@Composable
private fun PartEventDetailContent(
    detail: EventPartDetail,
    responsibles: List<EventResponsibleDetail>,
    apiClient: ApiClient,
    eventId: Int,
    kind: EventKind,
    openUrl: (String) -> Unit
) {
    val contentColor = kvantContentColor()
    val kvant = kvantColors()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .kvantBottomScreenInset(extra = DetailBottomPadding),
        verticalArrangement = Arrangement.spacedBy(DetailSectionSpacing)
    ) {
        if (detail.formOfHolding.isNotBlank()) {
            Text(
                text = detail.formOfHolding,
                color = contentColor,
                fontFamily = Montserrat,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (detail.level.isNotBlank()) {
            Text(
                text = detail.level,
                color = contentColor,
                fontFamily = Montserrat,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (detail.registrationDeadline.isNotBlank()) {
            Text(
                text = "регистрация до: ${detail.registrationDeadline}",
                color = contentColor,
                fontFamily = Montserrat,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (detail.link.isNotBlank()) {
            Text(
                text = "Перейти на сайт",
                color = kvant.link,
                fontFamily = Montserrat,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { openUrl(detail.link) }
            )
        }

        if (responsibles.isNotEmpty()) {
            Text(
                text = "ОТВЕТСТВЕННЫЕ",
                color = contentColor,
                fontFamily = Montserrat,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            responsibles.forEach { responsible ->
                PartResponsibleViewCard(responsible = responsible)
            }
        } else if (detail.responsibles.isNotEmpty()) {
            PersonChipsRow(names = detail.responsibles.map { it.name })
        }

        KvantDetailSection(label = "Участники и работы:", value = detail.participantsAndWorks)
        KvantDetailSection(label = "Даты:", value = detail.datesOfEvent)
        KvantDetailSection(label = "Количество участников:", value = detail.participantsAmount.toString())
        KvantDetailSection(label = "Количество победителей:", value = detail.winnerAmount.toString())
        KvantDetailSection(label = "Количество призёров:", value = detail.runnerUpAmount.toString())
        KvantDetailSection(label = "Примечания:", value = detail.annotation)

        EventDocumentsSection(
            apiClient = apiClient,
            eventId = eventId,
            kind = kind
        )
    }
}

@Composable
private fun PartResponsibleViewCard(responsible: EventResponsibleDetail) {
    val contentColor = kvantContentColor()
    KvantInnerCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = responsible.name,
                    color = contentColor,
                    fontFamily = Montserrat,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                ParticipationMarkIcon(participated = responsible.markSent)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ResponsibleStat(label = "Участв.", value = responsible.participants)
                ResponsibleStat(label = "Призёры", value = responsible.runnerUp)
                ResponsibleStat(label = "Побед.", value = responsible.winners)
            }
            if (responsible.comment.isNotBlank()) {
                Text(
                    text = responsible.comment,
                    color = contentColor,
                    fontFamily = Montserrat,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun ParticipationMarkIcon(participated: Boolean) {
    val contentColor = kvantContentColor()
    Icon(
        painter = painterResource(
            if (participated) R.drawable.outline_check_24 else R.drawable.round_close_24
        ),
        contentDescription = if (participated) "Участвовал" else "Не участвовал",
        tint = contentColor,
        modifier = Modifier.size(24.dp)
    )
}

@Composable
private fun ResponsibleStat(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = kvantContentColor(),
            fontFamily = Montserrat,
            fontSize = 13.sp
        )
        Text(
            text = value.toString(),
            color = kvantContentColor(),
            fontFamily = Montserrat,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun ParticipantsBlock(planned: Int, actual: Int) {
    val contentColor = kvantContentColor()
    Text(
        text = "Количество участников:",
        color = contentColor,
        fontSize = 20.sp,
        fontFamily = Montserrat,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "запланированное: $planned",
        color = contentColor,
        fontFamily = Montserrat,
        fontSize = 18.sp,
        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
    )
    Text(
        text = "фактическое: $actual",
        color = contentColor,
        fontFamily = Montserrat,
        fontSize = 18.sp,
        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
    )
}

@Composable
private fun BookingCard(rents: List<EventRentItem>) {
    val contentColor = kvantContentColor()

    KvantInnerCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "БРОНИРОВАНИЕ",
                color = contentColor,
                fontFamily = Montserrat,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            rents.forEach { rent ->
                KvantCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 12.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = rent.roomName,
                            color = contentColor,
                            fontFamily = Montserrat,
                            fontSize = 17.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${rent.startTime}-${rent.endTime}",
                            color = contentColor,
                            fontFamily = Montserrat,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
