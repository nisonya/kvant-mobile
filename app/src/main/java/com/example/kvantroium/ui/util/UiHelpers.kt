package com.example.kvantroium.ui.util

import com.example.kvantroium.api.ApiException

fun Throwable.userMessage(): String {
    return when (this) {
        is ApiException -> message ?: "Ошибка API"
        else -> message ?: "Ошибка сети"
    }
}

fun validateLogin(serverUrl: String, login: String, password: String): String? {
    if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
        return "Адрес сервера должен начинаться с http:// или https://"
    }
    if (login.isBlank()) return "Введите логин"
    if (password.isBlank()) return "Введите пароль"
    return null
}

fun accessLevelName(level: Int?): String {
    return when (level) {
        1 -> "root"
        2 -> "педагог"
        3 -> "гость"
        4 -> "руководитель"
        5 -> "педагог-организатор"
        6 -> "администратор"
        null -> "—"
        else -> level.toString()
    }
}

fun genderLabel(value: String?): String {
    return when (value?.trim()?.uppercase()) {
        "M", "М" -> "Мужской"
        "F", "Ж" -> "Женский"
        null, "" -> "—"
        else -> value
    }
}

fun normalizeExternalUrl(value: String?): String? {
    val trimmed = value?.trim().orEmpty()
    if (trimmed.isEmpty()) return null
    if (trimmed.startsWith("http://", ignoreCase = true) ||
        trimmed.startsWith("https://", ignoreCase = true)
    ) {
        return trimmed
    }
    if (trimmed.startsWith("//")) return "https:$trimmed"
    return "https://$trimmed"
}

fun todayUiDate(): String =
    java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
        .format(java.util.Date())
