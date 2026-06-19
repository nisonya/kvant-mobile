package com.example.kvantroium.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kvantroium.R
import com.example.kvantroium.api.ApiClient
import com.example.kvantroium.features.access.canManageReferenceData
import com.example.kvantroium.features.documents.DocumentDraft
import com.example.kvantroium.features.documents.DocumentItem
import com.example.kvantroium.features.documents.loadDocuments
import com.example.kvantroium.features.documents.saveDocumentDrafts
import com.example.kvantroium.storage.UserSession
import com.example.kvantroium.ui.components.KvantCard
import com.example.kvantroium.ui.components.KvantFormField
import com.example.kvantroium.ui.components.KvantScreenScaffold
import com.example.kvantroium.ui.components.kvantBottomScreenInset
import com.example.kvantroium.ui.theme.Montserrat
import com.example.kvantroium.ui.theme.kvantContentColor
import com.example.kvantroium.ui.util.normalizeExternalUrl
import com.example.kvantroium.ui.util.userMessage

@Composable
fun DocumentsScreen(
    session: UserSession,
    apiClient: ApiClient,
    onBack: () -> Unit,
    openUrl: (String) -> Unit
) {
    val context = LocalContext.current
    val canEdit = canManageReferenceData(session.user)
    var documents by remember { mutableStateOf<List<DocumentItem>>(emptyList()) }
    var drafts by remember { mutableStateOf<List<DocumentDraft>>(emptyList()) }
    var editMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var reloadNonce by remember { mutableIntStateOf(0) }
    var saveNonce by remember { mutableIntStateOf(0) }

    LaunchedEffect(reloadNonce) {
        isLoading = true
        error = null
        runCatching {
            documents = loadDocuments(apiClient)
        }.onFailure {
            error = it.userMessage()
        }
        isLoading = false
    }

    LaunchedEffect(saveNonce) {
        if (saveNonce == 0) return@LaunchedEffect
        isSaving = true
        saveError = null
        runCatching {
            saveDocumentDrafts(apiClient, documents, drafts)
            editMode = false
            reloadNonce++
        }.onFailure {
            saveError = it.userMessage()
        }
        isSaving = false
    }

    KvantScreenScaffold(onBack = onBack, title = "ДОКУМЕНТЫ") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .kvantBottomScreenInset(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (canEdit) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            if (editMode) {
                                editMode = false
                                saveError = null
                            } else {
                                drafts = documents.map { DocumentDraft(it.id, it.name, it.link) }
                                editMode = true
                            }
                        },
                        enabled = !isSaving
                    ) {
                        Text(
                            text = if (editMode) "Отмена" else "Редактировать",
                            fontFamily = Montserrat
                        )
                    }
                }
            }

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
                error != null -> {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, fontFamily = Montserrat)
                    Button(onClick = { reloadNonce++ }) {
                        Text("Повторить", fontFamily = Montserrat)
                    }
                }
                editMode -> {
                    drafts.forEachIndexed { index, draft ->
                        DocumentEditRow(
                            draft = draft,
                            onChange = { updated ->
                                drafts = drafts.toMutableList().also { it[index] = updated }
                            },
                            onRemove = {
                                drafts = drafts.toMutableList().also { it.removeAt(index) }
                            }
                        )
                    }
                    Button(
                        onClick = {
                            drafts = drafts + DocumentDraft(name = "", link = "")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Добавить ссылку", fontFamily = Montserrat)
                    }
                    saveError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontFamily = Montserrat)
                    }
                    Button(
                        onClick = { saveNonce++ },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Сохранить", fontFamily = Montserrat, fontWeight = FontWeight.Bold)
                    }
                }
                documents.isEmpty() -> {
                    Text(
                        text = "Документы не добавлены",
                        color = kvantContentColor(),
                        fontFamily = Montserrat,
                        fontSize = 16.sp
                    )
                }
                else -> {
                    documents.forEach { document ->
                        DocumentReadRow(
                            document = document,
                            onOpen = {
                                normalizeExternalUrl(document.link)?.let(openUrl)
                            },
                            onCopy = {
                                copyLinkToClipboard(context, document.link)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentReadRow(
    document: DocumentItem,
    onOpen: () -> Unit,
    onCopy: () -> Unit
) {
    val contentColor = kvantContentColor()
    KvantCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = document.name.ifBlank { "Без названия" },
                color = contentColor,
                fontFamily = Montserrat,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onCopy) {
                Icon(
                    painter = painterResource(R.drawable.baseline_content_copy_24),
                    contentDescription = "Копировать ссылку",
                    tint = contentColor
                )
            }
        }
    }
}

@Composable
private fun DocumentEditRow(
    draft: DocumentDraft,
    onChange: (DocumentDraft) -> Unit,
    onRemove: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        KvantFormField(
            label = "Название",
            value = draft.name,
            onValueChange = { onChange(draft.copy(name = it)) }
        )
        KvantFormField(
            label = "Ссылка",
            value = draft.link,
            onValueChange = { onChange(draft.copy(link = it)) }
        )
        TextButton(onClick = onRemove) {
            Text("Удалить", fontFamily = Montserrat, color = MaterialTheme.colorScheme.error)
        }
    }
}

private fun copyLinkToClipboard(context: Context, link: String) {
    val normalized = normalizeExternalUrl(link)
    if (normalized.isNullOrBlank()) {
        Toast.makeText(context, "Ссылка пустая", Toast.LENGTH_SHORT).show()
        return
    }
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("document_link", normalized))
    Toast.makeText(context, "Ссылка скопирована", Toast.LENGTH_SHORT).show()
}
