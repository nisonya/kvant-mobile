package com.example.kvantroium.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.kvantroium.api.ApiClient
import com.example.kvantroium.features.events.EventDocPreviewKind
import com.example.kvantroium.features.events.EventDocumentItem
import com.example.kvantroium.features.events.EventKind
import com.example.kvantroium.features.events.deleteEventDocument
import com.example.kvantroium.features.events.downloadEventDocument
import com.example.kvantroium.features.events.eventDocPreviewKind
import com.example.kvantroium.features.events.formatEventDocumentSize
import com.example.kvantroium.features.events.loadEventDocuments
import com.example.kvantroium.features.events.uploadEventDocument
import com.example.kvantroium.ui.theme.Montserrat
import com.example.kvantroium.ui.theme.kvantColors
import com.example.kvantroium.ui.theme.kvantContentColor
import com.example.kvantroium.ui.util.EventDocumentFiles
import com.example.kvantroium.ui.util.userMessage
import kotlinx.coroutines.launch

@Composable
fun EventDocumentsSection(
    apiClient: ApiClient,
    eventId: Int,
    kind: EventKind,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val contentColor = kvantContentColor()
    val kvant = kvantColors()

    var documents by remember { mutableStateOf<List<EventDocumentItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isUploading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var reloadNonce by remember { mutableIntStateOf(0) }
    var deleteCandidate by remember { mutableStateOf<EventDocumentItem?>(null) }
    var previewDocument by remember { mutableStateOf<EventDocumentItem?>(null) }

    val previewBitmaps = remember { mutableStateMapOf<Int, Bitmap?>() }
    val previewLoading = remember { mutableStateMapOf<Int, Boolean>() }

    fun loadDocuments() {
        scope.launch {
            isLoading = true
            message = null
            isError = false
            runCatching { loadEventDocuments(apiClient, kind, eventId) }
                .onSuccess { documents = it }
                .onFailure {
                    isError = true
                    message = it.userMessage()
                }
            isLoading = false
        }
    }

    LaunchedEffect(eventId, kind, reloadNonce) {
        loadDocuments()
    }

    LaunchedEffect(documents) {
        documents.forEach { doc ->
            if (previewBitmaps.containsKey(doc.id) || previewLoading[doc.id] == true) return@forEach
            val previewKind = eventDocPreviewKind(doc.mimeType, doc.fileName)
            if (previewKind == EventDocPreviewKind.OTHER) {
                previewBitmaps[doc.id] = null
                return@forEach
            }
            previewLoading[doc.id] = true
            runCatching {
                val bytes = downloadEventDocument(apiClient, kind, doc.id)
                EventDocumentFiles.decodePreviewBitmap(bytes, doc.mimeType, doc.fileName)
            }.onSuccess { previewBitmaps[doc.id] = it }
                .onFailure { previewBitmaps[doc.id] = null }
            previewLoading[doc.id] = false
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            isUploading = true
            isError = false
            message = "Загрузка..."
            val errors = mutableListOf<String>()
            uris.forEachIndexed { index, uri ->
                if (uris.size > 1) message = "Загрузка ${index + 1} из ${uris.size}..."
                runCatching {
                    val fileName = EventDocumentFiles.guessFileName(context, uri)
                    val mimeType = EventDocumentFiles.guessMimeType(context, uri, fileName)
                    val bytes = EventDocumentFiles.readBytes(context, uri)
                    uploadEventDocument(apiClient, kind, eventId, fileName, mimeType, bytes)
                }.onFailure { errors.add(it.userMessage()) }
            }
            if (errors.isNotEmpty()) {
                isError = true
                message = errors.joinToString("; ")
            } else {
                message = "Документы загружены"
                isError = false
            }
            previewBitmaps.clear()
            previewLoading.clear()
            reloadNonce++
            isUploading = false
        }
    }

    if (deleteCandidate != null) {
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Удалить документ?", fontFamily = Montserrat) },
            text = { Text(deleteCandidate!!.fileName, fontFamily = Montserrat) },
            confirmButton = {
                TextButton(onClick = {
                    val doc = deleteCandidate!!
                    deleteCandidate = null
                    scope.launch {
                        runCatching { deleteEventDocument(apiClient, kind, doc.id) }
                            .onSuccess {
                                previewBitmaps.remove(doc.id)
                                reloadNonce++
                                message = "Документ удалён"
                                isError = false
                            }
                            .onFailure {
                                message = it.userMessage()
                                isError = true
                            }
                    }
                }) {
                    Text("Удалить", fontFamily = Montserrat)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text("Отмена", fontFamily = Montserrat)
                }
            }
        )
    }

    previewDocument?.let { doc ->
        EventDocumentPreviewDialog(
            document = doc,
            apiClient = apiClient,
            kind = kind,
            onDismiss = { previewDocument = null }
        )
    }

    KvantInnerCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ДОКУМЕНТЫ",
                    color = contentColor,
                    fontFamily = Montserrat,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Button(
                    onClick = { filePicker.launch(arrayOf("*/*")) },
                    enabled = !isUploading
                ) {
                    Text("Добавить", fontFamily = Montserrat, fontSize = 13.sp)
                }
            }

            if (isUploading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(message.orEmpty(), fontFamily = Montserrat, fontSize = 13.sp, color = contentColor)
                }
            } else if (!message.isNullOrBlank()) {
                Text(
                    text = message.orEmpty(),
                    fontFamily = Montserrat,
                    fontSize = 13.sp,
                    color = if (isError) MaterialTheme.colorScheme.error else contentColor
                )
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = contentColor)
                    }
                }

                documents.isEmpty() -> {
                    Text(
                        text = "Нет документов. Нажмите «Добавить».",
                        color = contentColor.copy(alpha = 0.75f),
                        fontFamily = Montserrat,
                        fontSize = 14.sp
                    )
                }

                else -> {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(documents, key = { it.id }) { doc ->
                            EventDocumentCard(
                                document = doc,
                                previewBitmap = previewBitmaps[doc.id],
                                isPreviewLoading = previewLoading[doc.id] == true,
                                onPreviewClick = { previewDocument = doc },
                                onDownload = {
                                    scope.launch {
                                        runCatching {
                                            val bytes = downloadEventDocument(apiClient, kind, doc.id)
                                            val file = EventDocumentFiles.saveToCache(
                                                context,
                                                bytes,
                                                doc.fileName
                                            )
                                            EventDocumentFiles.openFile(context, file, doc.mimeType)
                                        }.onFailure {
                                            message = it.userMessage()
                                            isError = true
                                        }
                                    }
                                },
                                onDelete = { deleteCandidate = doc }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventDocumentCard(
    document: EventDocumentItem,
    previewBitmap: Bitmap?,
    isPreviewLoading: Boolean,
    onPreviewClick: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    val contentColor = kvantContentColor()
    val previewKind = eventDocPreviewKind(document.mimeType, document.fileName)
    val ext = document.fileName.substringAfterLast('.', "FILE").uppercase()

    Column(
        modifier = Modifier
            .width(220.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(kvantColors().chipBackground)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onPreviewClick),
            contentAlignment = Alignment.Center
        ) {
            when {
                isPreviewLoading -> CircularProgressIndicator(modifier = Modifier.size(28.dp))
                previewBitmap != null -> {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = document.fileName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                previewKind == EventDocPreviewKind.OTHER -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(ext, fontFamily = Montserrat, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text(
                            "Предпросмотр недоступен",
                            fontFamily = Montserrat,
                            fontSize = 12.sp,
                            color = contentColor.copy(alpha = 0.7f)
                        )
                    }
                }
                else -> Text(
                    "Не удалось загрузить превью",
                    fontFamily = Montserrat,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Text(
            text = document.fileName,
            color = contentColor,
            fontFamily = Montserrat,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = formatEventDocumentSize(document.sizeBytes),
            color = contentColor.copy(alpha = 0.7f),
            fontFamily = Montserrat,
            fontSize = 12.sp
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onDownload, modifier = Modifier.weight(1f)) {
                Text("Скачать", fontFamily = Montserrat, fontSize = 12.sp)
            }
            OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                Text("Удалить", fontFamily = Montserrat, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun EventDocumentPreviewDialog(
    document: EventDocumentItem,
    apiClient: ApiClient,
    kind: EventKind,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap by remember(document.id) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(document.id) { mutableStateOf(true) }
    var error by remember(document.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(document.id) {
        isLoading = true
        val previewKind = eventDocPreviewKind(document.mimeType, document.fileName)
        runCatching {
            val bytes = downloadEventDocument(apiClient, kind, document.id)
            if (previewKind == EventDocPreviewKind.OTHER) {
                val file = EventDocumentFiles.saveToCache(context, bytes, document.fileName)
                EventDocumentFiles.openFile(context, file, document.mimeType)
                onDismiss()
                return@LaunchedEffect
            }
            EventDocumentFiles.decodePreviewBitmap(bytes, document.mimeType, document.fileName)
        }.onSuccess { if (previewKind != EventDocPreviewKind.OTHER) bitmap = it }
            .onFailure { error = it.userMessage() }
        isLoading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = document.fileName,
                fontFamily = Montserrat,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> CircularProgressIndicator()
                    bitmap != null -> {
                        Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            contentScale = ContentScale.Fit
                        )
                    }
                    else -> Text(
                        text = error ?: "Предпросмотр недоступен",
                        fontFamily = Montserrat,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            runCatching {
                                val bytes = downloadEventDocument(apiClient, kind, document.id)
                                val file = EventDocumentFiles.saveToCache(context, bytes, document.fileName)
                                EventDocumentFiles.openFile(context, file, document.mimeType)
                            }.onFailure { error = it.userMessage() }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Открыть", fontFamily = Montserrat)
                }
                Button(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Закрыть", fontFamily = Montserrat)
                }
            }
        }
    }
}
