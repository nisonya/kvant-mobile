package com.example.kvantroium.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kvantroium.R
import com.example.kvantroium.api.ApiClient
import com.example.kvantroium.features.events.EmployeeOption
import com.example.kvantroium.features.events.EventKind
import com.example.kvantroium.features.events.EventListItem
import com.example.kvantroium.features.events.EventSortOption
import com.example.kvantroium.features.events.EventTimeFilter
import com.example.kvantroium.features.events.EventTypeOption
import com.example.kvantroium.features.events.countEvents
import com.example.kvantroium.features.events.loadEmployeeOptions
import com.example.kvantroium.features.events.loadEventTypeOptions
import com.example.kvantroium.features.events.loadEventsPage
import com.example.kvantroium.ui.components.KvantCard
import com.example.kvantroium.ui.components.KvantFilterChipMenu
import com.example.kvantroium.ui.components.KvantPullRefreshBox
import com.example.kvantroium.ui.components.KvantScreenContainer
import com.example.kvantroium.ui.components.KvantSegmentedRow
import com.example.kvantroium.ui.components.PersonChipsRow
import com.example.kvantroium.ui.components.kvantTextFieldColors
import com.example.kvantroium.ui.theme.Gothic
import com.example.kvantroium.ui.theme.Montserrat
import com.example.kvantroium.ui.theme.kvantContentColor
import com.example.kvantroium.ui.util.userMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val LOAD_MORE_THRESHOLD = 2

private fun eventListItemKey(kind: EventKind, eventId: Int): String = "${kind.name}_$eventId"

private data class EventsQuery(
    val kind: EventKind,
    val timeFilter: EventTimeFilter,
    val employeeId: Int,
    val typeId: Int,
    val sort: EventSortOption,
    val search: String
)

private suspend fun loadEventsFirstPage(
    apiClient: ApiClient,
    query: EventsQuery
): Pair<Int, List<EventListItem>> {
    val employeeId = query.employeeId.takeIf { it > 0 }
    val typeId = query.typeId.takeIf { it > 0 }
    val search = query.search.takeIf { it.isNotBlank() }
    val total = countEvents(
        apiClient = apiClient,
        kind = query.kind,
        timeFilter = query.timeFilter,
        employeeId = employeeId,
        typeId = typeId,
        search = search
    )
    val items = if (total == 0) {
        emptyList()
    } else {
        loadEventsPage(
            apiClient = apiClient,
            kind = query.kind,
            timeFilter = query.timeFilter,
            employeeId = employeeId,
            typeId = typeId,
            search = search,
            sort = query.sort,
            page = 0
        )
    }
    return total to items
}

@Composable
fun EventsScreen(
    apiClient: ApiClient,
    visible: Boolean = true,
    onBack: () -> Unit,
    onOpenEvent: (eventId: Int, kind: EventKind) -> Unit
) {
    var kind by remember { mutableStateOf(EventKind.ORG) }
    var timeFilter by remember { mutableStateOf(EventTimeFilter.ALL) }
    var selectedEmployeeId by remember { mutableIntStateOf(0) }
    var selectedTypeId by remember { mutableIntStateOf(0) }
    var sortOption by remember { mutableStateOf(EventSortOption.defaultFor(EventKind.ORG)) }
    var searchQuery by remember { mutableStateOf("") }
    var submittedSearch by remember { mutableStateOf("") }
    var reloadNonce by remember { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }

    var employees by remember { mutableStateOf<List<EmployeeOption>>(emptyList()) }
    var typeOptions by remember { mutableStateOf<List<EventTypeOption>>(emptyList()) }
    var events by remember { mutableStateOf<List<EventListItem>>(emptyList()) }
    var totalCount by remember { mutableIntStateOf(0) }
    var currentPage by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()
    val loadMoreMutex = remember { Mutex() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val contentColor = kvantContentColor()

    val query = remember(kind, timeFilter, selectedEmployeeId, selectedTypeId, sortOption, submittedSearch) {
        EventsQuery(
            kind = kind,
            timeFilter = timeFilter,
            employeeId = selectedEmployeeId,
            typeId = selectedTypeId,
            sort = sortOption,
            search = submittedSearch
        )
    }

    LaunchedEffect(Unit) {
        runCatching { employees = loadEmployeeOptions(apiClient) }
    }

    LaunchedEffect(kind) {
        runCatching { typeOptions = loadEventTypeOptions(apiClient, kind) }
    }

    LaunchedEffect(kind) {
        listState.scrollToItem(0)
    }

    LaunchedEffect(query, reloadNonce) {
        isLoading = true
        isLoadingMore = false
        error = null
        currentPage = 0
        events = emptyList()
        try {
            val (total, items) = loadEventsFirstPage(apiClient, query)
            totalCount = total
            events = items
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.userMessage()
        } finally {
            isLoading = false
            isRefreshing = false
        }
    }

    LaunchedEffect(listState, query, reloadNonce) {
        snapshotFlow {
            val loadedCount = events.size
            if (loadedCount == 0 || loadedCount >= totalCount || isLoading || isLoadingMore) {
                return@snapshotFlow false
            }
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible >= loadedCount - LOAD_MORE_THRESHOLD
        }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                if (isLoading || events.isEmpty() || events.size >= totalCount) return@collect
                isLoadingMore = true
                try {
                    loadMoreMutex.withLock {
                        if (events.size >= totalCount) return@withLock
                        val nextPage = currentPage + 1
                        val pageItems = loadEventsPage(
                            apiClient = apiClient,
                            kind = query.kind,
                            timeFilter = query.timeFilter,
                            employeeId = query.employeeId.takeIf { it > 0 },
                            typeId = query.typeId.takeIf { it > 0 },
                            search = query.search.takeIf { it.isNotBlank() },
                            sort = query.sort,
                            page = nextPage
                        )
                        if (pageItems.isNotEmpty()) {
                            events = events + pageItems
                            currentPage = nextPage
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    error = e.userMessage()
                } finally {
                    isLoadingMore = false
                }
            }
    }

    val hasMoreEvents = events.isNotEmpty() && events.size < totalCount
    val showLoadMoreProgress = isLoadingMore && hasMoreEvents

    val typeChipLabel = if (kind == EventKind.ORG) "Тип" else "Уровень"
    val typeAllLabel = if (kind == EventKind.ORG) "Все типы" else "Все уровни"
    val selectedTypeLabel = typeOptions.firstOrNull { it.id == selectedTypeId }?.name ?: typeAllLabel

    KvantPullRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            reloadNonce++
        },
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = if (visible) 1f else 0f }
    ) {
    KvantScreenContainer(
        modifier = Modifier.fillMaxSize()
    ) {
        Icon(
            painter = painterResource(R.drawable.round_arrow_back_24),
            contentDescription = "Назад",
            modifier = Modifier
                .size(40.dp)
                .clickable(onClick = onBack),
            tint = contentColor
        )

        KvantSegmentedRow(
            options = listOf("Организация", "Участие"),
            selectedIndex = if (kind == EventKind.ORG) 0 else 1,
            onSelected = { index ->
                val newKind = if (index == 0) EventKind.ORG else EventKind.PART
                if (newKind != kind) {
                    events = emptyList()
                    totalCount = 0
                    currentPage = 0
                    isLoading = true
                    isLoadingMore = false
                    error = null
                    scope.launch { listState.scrollToItem(0) }
                    kind = newKind
                    sortOption = EventSortOption.defaultFor(newKind)
                    selectedTypeId = 0
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            item {
                KvantFilterChipMenu(
                    chipLabel = "Период",
                    selectedLabel = timeFilter.label,
                    isActive = timeFilter != EventTimeFilter.ALL,
                    options = EventTimeFilter.entries.map { it to it.label },
                    selectedValue = timeFilter,
                    onSelected = { timeFilter = it }
                )
            }
            item {
                KvantFilterChipMenu(
                    chipLabel = "Сотрудник",
                    selectedLabel = employees.firstOrNull { it.id == selectedEmployeeId }?.name
                        ?: "Все сотрудники",
                    isActive = selectedEmployeeId > 0,
                    options = listOf(0 to "Все сотрудники") + employees.map { it.id to it.name },
                    selectedValue = selectedEmployeeId,
                    onSelected = { selectedEmployeeId = it }
                )
            }
            if (typeOptions.isNotEmpty()) {
                item {
                    KvantFilterChipMenu(
                        chipLabel = typeChipLabel,
                        selectedLabel = selectedTypeLabel,
                        isActive = selectedTypeId > 0,
                        options = listOf(0 to typeAllLabel) + typeOptions.map { it.id to it.name },
                        selectedValue = selectedTypeId,
                        onSelected = { selectedTypeId = it }
                    )
                }
            }
            item {
                KvantFilterChipMenu(
                    chipLabel = "Сортировка",
                    selectedLabel = sortOption.label,
                    isActive = sortOption != EventSortOption.defaultFor(kind),
                    options = EventSortOption.optionsFor(kind).map { it to it.label },
                    selectedValue = sortOption,
                    onSelected = { sortOption = it },
                    valueEquals = { a, b -> a.field == b.field && a.order == b.order }
                )
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            placeholder = {
                Text("Поиск", fontFamily = Montserrat)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    submittedSearch = searchQuery.trim()
                    focusManager.clearFocus()
                }
            ),
            colors = kvantTextFieldColors()
        )

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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, fontFamily = Montserrat)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { reloadNonce++ }) {
                        Text("Повторить", fontFamily = Montserrat)
                    }
                }
            }

            events.isEmpty() -> {
                Text(
                    text = "Нет мероприятий",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 30.dp),
                    textAlign = TextAlign.Center,
                    color = contentColor,
                    fontFamily = Montserrat,
                    fontSize = 18.sp
                )
            }

            else -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = events,
                            key = { eventListItemKey(kind, it.id) }
                        ) { event ->
                            EventCard(
                                event = event,
                                onClick = { onOpenEvent(event.id, kind) }
                            )
                        }
                        if (showLoadMoreProgress) {
                            item(key = "load_more_progress_${kind.name}") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(36.dp),
                                        color = contentColor,
                                        strokeWidth = 3.dp
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
}

@Composable
private fun EventCard(event: EventListItem, onClick: () -> Unit) {
    val contentColor = kvantContentColor()

    KvantCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = event.name,
                color = contentColor,
                fontFamily = Gothic,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (event.subtitle.isNotBlank()) {
                Text(
                    text = event.subtitle,
                    modifier = Modifier.padding(top = 8.dp),
                    color = contentColor,
                    fontFamily = Montserrat,
                    fontSize = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (event.detail.isNotBlank() && event.detail != "—") {
                Text(
                    text = event.detail,
                    modifier = Modifier.padding(top = 2.dp),
                    color = contentColor,
                    fontFamily = Montserrat,
                    fontSize = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (event.responsibles.isNotEmpty()) {
                PersonChipsRow(
                    names = event.responsibles.map { it.name },
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}
