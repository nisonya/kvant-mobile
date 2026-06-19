package com.example.kvantroium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kvantroium.BuildConfig
import com.example.kvantroium.R
import com.example.kvantroium.api.ApiClient
import com.example.kvantroium.api.AppUpdateCoordinator
import com.example.kvantroium.api.MobileUpdateInfo
import com.example.kvantroium.api.MobileUpdateService
import com.example.kvantroium.features.profile.EmployeeDetails
import com.example.kvantroium.features.profile.ProfileEditDraft
import com.example.kvantroium.features.profile.ProfileFieldLimits
import com.example.kvantroium.features.profile.changePassword
import com.example.kvantroium.features.profile.loadEmployeeDetails
import com.example.kvantroium.features.profile.updateEmployeeProfile
import com.example.kvantroium.storage.SessionStorage
import com.example.kvantroium.storage.UserSession
import com.example.kvantroium.ui.components.KvantDatePickerField
import com.example.kvantroium.ui.components.KvantFormField
import com.example.kvantroium.ui.components.KvantFormSelect
import com.example.kvantroium.ui.components.KvantInnerCard
import com.example.kvantroium.ui.components.KvantScreenScaffold
import com.example.kvantroium.ui.components.kvantBottomScreenInset
import com.example.kvantroium.ui.theme.GenderAccent
import com.example.kvantroium.ui.theme.Montserrat
import com.example.kvantroium.ui.theme.kvantColors
import com.example.kvantroium.ui.theme.kvantContentColor
import com.example.kvantroium.ui.util.accessLevelName
import com.example.kvantroium.ui.util.genderLabel
import com.example.kvantroium.ui.util.userMessage
import kotlinx.coroutines.launch

class ProfileUpdateCache {
    var done: Boolean = false
    var pendingUpdate: MobileUpdateInfo? = null
    var updateError: String? = null
}

@Composable
fun rememberProfileUpdateCache(): ProfileUpdateCache = remember { ProfileUpdateCache() }

@Composable
fun ProfileScreen(
    session: UserSession,
    apiClient: ApiClient,
    storage: SessionStorage,
    updateService: MobileUpdateService,
    updateCache: ProfileUpdateCache,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    openUrl: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val updateCoordinator = remember(context, apiClient, updateService) {
        AppUpdateCoordinator(context, apiClient, updateService)
    }
    val contentColor = kvantContentColor()
    val kvant = kvantColors()
    val employeeId = session.user?.employeeId

    var employee by remember { mutableStateOf<EmployeeDetails?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isEditing by remember { mutableStateOf(false) }
    var editDraft by remember { mutableStateOf(ProfileEditDraft()) }
    var editError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordMessage by remember { mutableStateOf<String?>(null) }
    var isChangingPassword by remember { mutableStateOf(false) }

    var updateError by remember { mutableStateOf(updateCache.updateError) }
    var pendingUpdate by remember { mutableStateOf(updateCache.pendingUpdate) }
    var isCheckingUpdate by remember { mutableStateOf(!updateCache.done) }
    var isUpdating by remember { mutableStateOf(false) }
    var updateChecked by remember { mutableStateOf(updateCache.done) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var reloadNonce by remember { mutableIntStateOf(0) }

    LaunchedEffect(employeeId, reloadNonce) {
        isLoading = true
        loadError = null
        if (employeeId == null || employeeId <= 0) {
            employee = null
            loadError = "К учётной записи не привязан сотрудник"
        } else {
            runCatching { loadEmployeeDetails(apiClient, employeeId) }
                .onSuccess { details ->
                    employee = details
                    if (details == null) {
                        loadError = "Данные сотрудника не найдены на сервере"
                    } else {
                        editDraft = details.toEditDraft()
                    }
                }
                .onFailure { loadError = it.userMessage() }
        }
        isLoading = false
    }

    LaunchedEffect(Unit) {
        if (updateCache.done) {
            pendingUpdate = updateCache.pendingUpdate
            updateError = updateCache.updateError
            updateChecked = true
            return@LaunchedEffect
        }
        isCheckingUpdate = true
        updateError = null
        pendingUpdate = null
        updateChecked = false
        val state = updateCoordinator.checkForUpdates(force = false)
        if (state.isError) {
            updateCache.updateError = state.message
            updateCache.pendingUpdate = null
        } else {
            updateCache.pendingUpdate = state.info
            updateCache.updateError = null
        }
        updateCache.done = true
        pendingUpdate = updateCache.pendingUpdate
        updateError = updateCache.updateError
        updateChecked = true
        isCheckingUpdate = false
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Выйти из аккаунта?", fontFamily = Montserrat) },
            text = { Text("Текущая сессия будет завершена.", fontFamily = Montserrat) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    onLogout()
                }) {
                    Text("Выйти", fontFamily = Montserrat)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Отмена", fontFamily = Montserrat)
                }
            }
        )
    }

    KvantScreenScaffold(onBack = onBack, title = "ПРОФИЛЬ") {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .kvantBottomScreenInset(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(kvant.chipBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_person_24),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = kvant.chipContent
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = employee?.fullName ?: session.user?.login.orEmpty().ifBlank { "Пользователь" },
                        color = contentColor,
                        fontFamily = Montserrat,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = session.user?.login.orEmpty(),
                        color = contentColor.copy(alpha = 0.7f),
                        fontFamily = Montserrat,
                        fontSize = 14.sp
                    )
                    Text(
                        text = accessLevelName(session.user?.accessLevel),
                        color = contentColor.copy(alpha = 0.7f),
                        fontFamily = Montserrat,
                        fontSize = 14.sp
                    )
                }
            }

            if (!isEditing) {
                Button(
                    onClick = {
                        employee?.let { editDraft = it.toEditDraft() }
                        isEditing = true
                        editError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = employee != null
                ) {
                    Text("Редактировать", fontFamily = Montserrat, fontSize = 13.sp)
                }
            }

            when {
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

                isEditing -> {
                    ProfileEditForm(
                        draft = editDraft,
                        onDraftChange = { editDraft = it },
                        error = editError,
                        isSaving = isSaving,
                        oldPassword = oldPassword,
                        newPassword = newPassword,
                        confirmPassword = confirmPassword,
                        passwordMessage = passwordMessage,
                        isChangingPassword = isChangingPassword,
                        onOldPasswordChange = { oldPassword = it },
                        onNewPasswordChange = { newPassword = it },
                        onConfirmPasswordChange = { confirmPassword = it },
                        onCancel = {
                            isEditing = false
                            editError = null
                            passwordMessage = null
                            employee?.let { editDraft = it.toEditDraft() }
                        },
                        onSave = {
                            val validationError = ProfileFieldLimits.validateProfileDraft(editDraft)
                            if (validationError != null) {
                                editError = validationError
                                return@ProfileEditForm
                            }
                            val id = employeeId ?: return@ProfileEditForm
                            scope.launch {
                                isSaving = true
                                editError = null
                                runCatching {
                                    updateEmployeeProfile(apiClient, id, editDraft)
                                    storage.saveGenderAccent(GenderAccent.fromRaw(editDraft.gender))
                                    reloadNonce++
                                    isEditing = false
                                }.onFailure {
                                    editError = it.userMessage()
                                }
                                isSaving = false
                            }
                        },
                        onChangePassword = {
                            val validationError = ProfileFieldLimits.validatePasswordChange(
                                oldPassword, newPassword, confirmPassword
                            )
                            if (validationError != null) {
                                passwordMessage = validationError
                                return@ProfileEditForm
                            }
                            scope.launch {
                                isChangingPassword = true
                                passwordMessage = null
                                runCatching {
                                    changePassword(apiClient, oldPassword, newPassword)
                                    oldPassword = ""
                                    newPassword = ""
                                    confirmPassword = ""
                                    passwordMessage = "Пароль изменён"
                                }.onFailure {
                                    passwordMessage = it.userMessage()
                                }
                                isChangingPassword = false
                            }
                        }
                    )
                }

                employee != null -> {
                    KvantInnerCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Личная информация",
                                color = contentColor,
                                fontFamily = Montserrat,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            ProfileInfoRow("Должность", employee!!.position)
                            ProfileInfoRow("Дата рождения", formatEventDateDisplay(employee!!.dateOfBirth))
                            ProfileInfoRow("Контактный телефон", employee!!.contact)
                            ProfileInfoRow("Размер одежды", employee!!.size)
                            ProfileInfoRow("Образование", employee!!.education)
                            ProfileInfoRow("График", employee!!.schedule)
                            ProfileInfoRow("Пол", genderLabel(employee!!.gender))
                        }
                    }
                }

                loadError != null && !isLoading -> {
                    Text(
                        text = loadError.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        fontFamily = Montserrat,
                        fontSize = 14.sp
                    )
                }
            }

            Text(
                text = "Сервер: ${session.serverUrl}",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = contentColor.copy(alpha = 0.65f),
                fontFamily = Montserrat,
                fontSize = 13.sp
            )

            Text(
                text = "Версия ${BuildConfig.VERSION_NAME}",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = contentColor.copy(alpha = 0.65f),
                fontFamily = Montserrat,
                fontSize = 13.sp
            )

            when {
                isCheckingUpdate || isUpdating -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = contentColor
                    )
                }

                updateError != null -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = updateError.orEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error,
                            fontFamily = Montserrat,
                            fontSize = 14.sp
                        )
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isCheckingUpdate = true
                                    updateError = null
                                    updateCache.done = false
                                    val state = updateCoordinator.checkForUpdates(force = true)
                                    if (state.isError) {
                                        updateCache.updateError = state.message
                                        updateCache.pendingUpdate = null
                                    } else {
                                        updateCache.pendingUpdate = state.info
                                        updateCache.updateError = null
                                    }
                                    updateCache.done = true
                                    pendingUpdate = updateCache.pendingUpdate
                                    updateError = updateCache.updateError
                                    updateChecked = true
                                    isCheckingUpdate = false
                                }
                            },
                            enabled = !isCheckingUpdate && !isUpdating
                        ) {
                            Text("Проверить снова", fontFamily = Montserrat)
                        }
                    }
                }

                pendingUpdate != null -> {
                    Button(
                        onClick = {
                            val update = pendingUpdate ?: return@Button
                            scope.launch {
                                isUpdating = true
                                updateError = null
                                val downloadState = updateCoordinator.downloadUpdate(update)
                                if (downloadState.isError || downloadState.downloadedApk == null) {
                                    updateError = downloadState.message.ifBlank {
                                        "Не удалось загрузить обновление"
                                    }
                                    isUpdating = false
                                    return@launch
                                }
                                val installMessage = updateCoordinator.installDownloaded(
                                    downloadState.downloadedApk
                                )
                                if (installMessage != null) {
                                    updateError = installMessage
                                }
                                isUpdating = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isUpdating
                    ) {
                        Text("Обновить", fontFamily = Montserrat, fontWeight = FontWeight.Bold)
                    }
                }

                updateChecked -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Нет обновлений",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = contentColor,
                            fontFamily = Montserrat,
                            fontSize = 14.sp
                        )
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isCheckingUpdate = true
                                    updateError = null
                                    pendingUpdate = null
                                    updateChecked = false
                                    updateCache.done = false
                                    val state = updateCoordinator.checkForUpdates(force = true)
                                    if (state.isError) {
                                        updateCache.updateError = state.message
                                        updateCache.pendingUpdate = null
                                    } else {
                                        updateCache.pendingUpdate = state.info
                                        updateCache.updateError = null
                                    }
                                    updateCache.done = true
                                    pendingUpdate = updateCache.pendingUpdate
                                    updateError = updateCache.updateError
                                    updateChecked = true
                                    isCheckingUpdate = false
                                }
                            },
                            enabled = !isCheckingUpdate && !isUpdating
                        ) {
                            Text("Проверить снова", fontFamily = Montserrat)
                        }
                    }
                }
            }
            }

            if (!isEditing) {
                OutlinedButton(
                    onClick = { showLogoutConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .kvantBottomScreenInset()
                        .padding(top = 8.dp)
                ) {
                    Text("Выйти из аккаунта", fontFamily = Montserrat, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    val contentColor = kvantContentColor()
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = contentColor.copy(alpha = 0.75f),
            fontFamily = Montserrat,
            fontSize = 13.sp
        )
        Text(
            text = value.ifBlank { "—" },
            color = contentColor,
            fontFamily = Montserrat,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun ProfileEditForm(
    draft: ProfileEditDraft,
    onDraftChange: (ProfileEditDraft) -> Unit,
    error: String?,
    isSaving: Boolean,
    oldPassword: String,
    newPassword: String,
    confirmPassword: String,
    passwordMessage: String?,
    isChangingPassword: Boolean,
    onOldPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onChangePassword: () -> Unit
) {
    val contentColor = kvantContentColor()
    val genderOptions = ProfileFieldLimits.GENDER_OPTIONS.map { it to it }
    val educationOptions = ProfileFieldLimits.EDUCATION_OPTIONS.map { it to it }

    KvantInnerCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Редактирование профиля",
                color = contentColor,
                fontFamily = Montserrat,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )

            KvantFormField(
                label = "Фамилия",
                value = draft.secondName,
                onValueChange = { onDraftChange(draft.copy(secondName = it.take(ProfileFieldLimits.NAME_MAX))) }
            )
            KvantFormField(
                label = "Имя",
                value = draft.firstName,
                onValueChange = { onDraftChange(draft.copy(firstName = it.take(ProfileFieldLimits.NAME_MAX))) }
            )
            KvantFormField(
                label = "Отчество",
                value = draft.patronymic,
                onValueChange = { onDraftChange(draft.copy(patronymic = it.take(ProfileFieldLimits.PATRONYMIC_MAX))) }
            )
            KvantDatePickerField(
                label = "Дата рождения",
                value = draft.dateOfBirthUi,
                onValueChange = { onDraftChange(draft.copy(dateOfBirthUi = it)) }
            )
            KvantFormField(
                label = "Контактный номер",
                value = draft.contact,
                onValueChange = { onDraftChange(draft.copy(contact = it.take(ProfileFieldLimits.CONTACT_MAX))) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            KvantFormSelect(
                label = "Пол",
                selectedLabel = draft.gender.ifBlank { "—" },
                options = listOf("" to "—") + genderOptions,
                selectedValue = draft.gender,
                onSelected = { onDraftChange(draft.copy(gender = it)) }
            )
            KvantFormSelect(
                label = "Образование",
                selectedLabel = draft.education.ifBlank { "—" },
                options = listOf("" to "—") + educationOptions,
                selectedValue = draft.education,
                onSelected = { onDraftChange(draft.copy(education = it)) }
            )
            KvantFormField(
                label = "Размер одежды",
                value = draft.size,
                onValueChange = { onDraftChange(draft.copy(size = it.take(ProfileFieldLimits.SIZE_MAX))) }
            )

            if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error, fontFamily = Montserrat)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f), enabled = !isSaving) {
                    Text("Отмена", fontFamily = Montserrat)
                }
                Button(onClick = onSave, modifier = Modifier.weight(1f), enabled = !isSaving) {
                    Text(if (isSaving) "Сохранение..." else "Сохранить", fontFamily = Montserrat)
                }
            }
        }
    }

    KvantInnerCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Смена пароля",
                color = contentColor,
                fontFamily = Montserrat,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            KvantFormField(
                label = "Текущий пароль",
                value = oldPassword,
                onValueChange = onOldPasswordChange,
                visualTransformation = PasswordVisualTransformation()
            )
            KvantFormField(
                label = "Новый пароль",
                value = newPassword,
                onValueChange = onNewPasswordChange,
                visualTransformation = PasswordVisualTransformation()
            )
            KvantFormField(
                label = "Повторите новый пароль",
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                visualTransformation = PasswordVisualTransformation()
            )
            if (passwordMessage != null) {
                Text(
                    text = passwordMessage.orEmpty(),
                    color = if (passwordMessage == "Пароль изменён") contentColor else MaterialTheme.colorScheme.error,
                    fontFamily = Montserrat
                )
            }
            Button(onClick = onChangePassword, enabled = !isChangingPassword) {
                Text(if (isChangingPassword) "Смена..." else "Сменить пароль", fontFamily = Montserrat)
            }
        }
    }
}

private fun formatEventDateDisplay(isoDate: String): String {
    return com.example.kvantroium.features.events.formatEventDateInput(isoDate).ifBlank { isoDate }
}
