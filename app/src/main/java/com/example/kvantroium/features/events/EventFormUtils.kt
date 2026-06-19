package com.example.kvantroium.features.events

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
private val uiDateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
private val uiDateFormatShort = SimpleDateFormat("dd.MM.yy", Locale.getDefault())

private val orgWeekdaysRu = listOf(
    "ВОСКРЕСЕНЬЕ", "ПОНЕДЕЛЬНИК", "ВТОРНИК", "СРЕДА",
    "ЧЕТВЕРГ", "ПЯТНИЦА", "СУББОТА"
)

object EventFieldLimits {
    const val RESP_COMMENT_MAX = 250
    const val UINT_MAX = 4294967295L

    val orgStringMax = mapOf(
        "name" to 110,
        "form_of_holding" to 60,
        "day_of_the_week" to 23,
        "result" to 110,
        "annotation" to 16383,
        "link" to 16383
    )

    val partStringMax = mapOf(
        "name" to 110,
        "participants_and_works" to 210,
        "result" to 180,
        "dates_of_event" to 110,
        "annotation" to 16383,
        "link" to 16383
    )
}

fun parseIsoDate(value: String?): String? {
    if (value.isNullOrBlank()) return null
    val trimmed = value.trim()
    if (trimmed.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) return trimmed
    return parseUiDateToIso(trimmed)
}

fun parseUiDateToIso(value: String?): String? {
    if (value.isNullOrBlank()) return null
    val trimmed = value.trim()
    listOf(uiDateFormat, uiDateFormatShort).forEach { format ->
        runCatching {
            val parsed = format.parse(trimmed) ?: return@forEach
            return isoDateFormat.format(parsed)
        }
    }
    return null
}

fun formatEventDateInput(value: String?): String {
    if (value.isNullOrBlank()) return ""
    runCatching {
        val parsed = isoDateFormat.parse(value.trim())
        if (parsed != null) return uiDateFormat.format(parsed)
    }
    return value
}

fun uiDateToPickerMillis(value: String?): Long? {
    val iso = parseUiDateToIso(value) ?: return null
    val parts = iso.split("-")
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null
    return localDateToUtcMillis(year, month, day)
}

fun pickerMillisToUiDate(millis: Long): String {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }
    return formatDateFromParts(
        year = utc.get(Calendar.YEAR),
        month = utc.get(Calendar.MONTH) + 1,
        day = utc.get(Calendar.DAY_OF_MONTH)
    )
}

fun formatDateFromParts(year: Int, month: Int, day: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return uiDateFormat.format(calendar.time)
}

private fun localDateToUtcMillis(year: Int, month: Int, day: Int): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    utc.clear()
    utc.set(year, month - 1, day, 0, 0, 0)
    utc.set(Calendar.MILLISECOND, 0)
    return utc.timeInMillis
}

fun weekdayFromIsoDate(isoDate: String?): String {
    if (isoDate.isNullOrBlank()) return ""
    return runCatching {
        val parsed = isoDateFormat.parse(isoDate.trim()) ?: return ""
        val calendar = Calendar.getInstance().apply { time = parsed }
        orgWeekdaysRu[calendar.get(Calendar.DAY_OF_WEEK) - 1]
    }.getOrDefault("")
}

fun formatTimeForDisplay(time: String?): String {
    if (time.isNullOrBlank()) return ""
    return time.trim().take(5)
}

fun formatTimeForApi(time: String?): String {
    if (time.isNullOrBlank()) return ""
    val trimmed = time.trim()
    return if (trimmed.length == 5 && trimmed.contains(':')) "$trimmed:00" else trimmed
}

fun parseTimeToHourMinute(time: String?): Pair<Int, Int>? {
    if (time.isNullOrBlank()) return null
    val parts = time.trim().split(':')
    if (parts.size < 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour to minute
}

fun formatTimeFromHourMinute(hour: Int, minute: Int): String =
    "%02d:%02d".format(Locale.getDefault(), hour, minute)

fun validateUintField(value: String, label: String): String? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return null
    if (!trimmed.matches(Regex("""\d+"""))) return "$label: только целое число ≥ 0"
    val number = trimmed.toLongOrNull() ?: return "$label: некорректное число"
    if (number > EventFieldLimits.UINT_MAX) return "$label: слишком большое значение"
    return null
}

fun validateStringField(value: String, label: String, maxLen: Int?): String? {
    if (maxLen != null && value.length > maxLen) {
        return "$label: не более $maxLen символов"
    }
    return null
}

fun validateOrgForm(
    name: String,
    formOfHolding: String,
    dayOfWeek: String,
    result: String,
    annotation: String,
    link: String,
    actualParticipants: String,
    plannedParticipants: String
): String? {
    if (name.trim().isEmpty()) return "Укажите название мероприятия"
    validateStringField(name, "Название", EventFieldLimits.orgStringMax["name"])?.let { return it }
    validateStringField(formOfHolding, "Место", EventFieldLimits.orgStringMax["form_of_holding"])?.let { return it }
    validateStringField(dayOfWeek, "День недели", EventFieldLimits.orgStringMax["day_of_the_week"])?.let { return it }
    validateStringField(result, "Результат", EventFieldLimits.orgStringMax["result"])?.let { return it }
    validateStringField(annotation, "Примечания", EventFieldLimits.orgStringMax["annotation"])?.let { return it }
    validateStringField(link, "Ссылка", EventFieldLimits.orgStringMax["link"])?.let { return it }
    validateUintField(actualParticipants, "Фактическое кол-во")?.let { return it }
    validateUintField(plannedParticipants, "Планируемое кол-во")?.let { return it }
    return null
}

fun validatePartForm(
    name: String,
    registrationDeadline: String,
    participantsAndWorks: String,
    datesOfEvent: String,
    result: String,
    annotation: String,
    link: String,
    typeId: Int?
): String? {
    if (name.trim().isEmpty()) return "Укажите название мероприятия"
    if (registrationDeadline.trim().isEmpty()) return "Укажите дату регистрации"
    if (parseUiDateToIso(registrationDeadline) == null && !registrationDeadline.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) {
        return "Некорректная дата регистрации (дд.мм.гггг)"
    }
    if (typeId == null || typeId <= 0) return "Выберите уровень мероприятия"
    validateStringField(name, "Название", EventFieldLimits.partStringMax["name"])?.let { return it }
    validateStringField(participantsAndWorks, "Участники и работы", EventFieldLimits.partStringMax["participants_and_works"])?.let { return it }
    validateStringField(datesOfEvent, "Даты", EventFieldLimits.partStringMax["dates_of_event"])?.let { return it }
    validateStringField(result, "Результат", EventFieldLimits.partStringMax["result"])?.let { return it }
    validateStringField(annotation, "Примечания", EventFieldLimits.partStringMax["annotation"])?.let { return it }
    validateStringField(link, "Ссылка", EventFieldLimits.partStringMax["link"])?.let { return it }
    return null
}

fun validateResponsibleComment(comment: String): String? {
    if (comment.length > EventFieldLimits.RESP_COMMENT_MAX) {
        return "Комментарий: не более ${EventFieldLimits.RESP_COMMENT_MAX} символов"
    }
    return null
}
