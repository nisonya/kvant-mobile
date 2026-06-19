package com.example.kvantroium.features.pixels

import org.json.JSONObject
import kotlin.math.abs

data class PixelStudentRow(
    val studentId: Int,
    val name: String,
    val partOfComp: Int = 0,
    val makeContent: Int = 0,
    val inviteFriend: Int = 0,
    val cleanKvantum: Int = 0,
    val filledProjectCardOnTime: Int = 0,
    val finishedProjectWithProduct: Int = 0,
    val regionalCompetition: Int = 0,
    val interregionalCompetition: Int = 0,
    val allRussianCompetition: Int = 0,
    val internationalCompetition: Int = 0,
    val nto: Int = 0,
    val becomeAnEngineeringVolunteer: Int = 0,
    val helpWithEvent: Int = 0,
    val makeOwnEvent: Int = 0,
    val specialAchievements: Int = 0,
    val fine: Int = 0,
    val attendancePercent: Int = 0
) {
    fun fieldValue(key: String): Int = when (key) {
        "part_of_comp" -> partOfComp
        "make_content" -> makeContent
        "invite_friend" -> inviteFriend
        "clean_kvantum" -> cleanKvantum
        "filled_project_card_on_time" -> filledProjectCardOnTime
        "finished_project_with_product" -> finishedProjectWithProduct
        "regional_competition" -> regionalCompetition
        "interregional_competition" -> interregionalCompetition
        "all_russian_competition" -> allRussianCompetition
        "international_competition" -> internationalCompetition
        "nto" -> nto
        "become_an_engineering_volunteer" -> becomeAnEngineeringVolunteer
        "help_with_event" -> helpWithEvent
        "make_own_event" -> makeOwnEvent
        "special_achievements" -> specialAchievements
        "fine" -> abs(fine)
        else -> 0
    }

    fun withFieldValue(key: String, value: Int): PixelStudentRow = when (key) {
        "part_of_comp" -> copy(partOfComp = value)
        "make_content" -> copy(makeContent = value)
        "invite_friend" -> copy(inviteFriend = value)
        "clean_kvantum" -> copy(cleanKvantum = value)
        "filled_project_card_on_time" -> copy(filledProjectCardOnTime = value)
        "finished_project_with_product" -> copy(finishedProjectWithProduct = value)
        "regional_competition" -> copy(regionalCompetition = value)
        "interregional_competition" -> copy(interregionalCompetition = value)
        "all_russian_competition" -> copy(allRussianCompetition = value)
        "international_competition" -> copy(internationalCompetition = value)
        "nto" -> copy(nto = value)
        "become_an_engineering_volunteer" -> copy(becomeAnEngineeringVolunteer = value)
        "help_with_event" -> copy(helpWithEvent = value)
        "make_own_event" -> copy(makeOwnEvent = value)
        "special_achievements" -> copy(specialAchievements = value)
        "fine" -> copy(fine = abs(value))
        else -> this
    }

    fun withAttendancePercent(percent: Int): PixelStudentRow =
        copy(attendancePercent = percent.coerceIn(0, 100))

    fun attendancePoints(): Int = attendancePercentToPixels(attendancePercent.toDouble())

    fun total(): Int = computePixelTotal(this)

    fun toUpdateJson(): JSONObject {
        val body = JSONObject()
            .put("id_student", studentId)
            .put("id", studentId)
        EDITABLE_PIXEL_KEYS.forEach { key ->
            body.put(key, fieldValue(key))
        }
        return body
    }
}

fun computePixelTotal(row: PixelStudentRow): Int {
    var sum = 0
    EDITABLE_PIXEL_KEYS.forEach { key ->
        val value = row.fieldValue(key)
        sum += if (key == "fine") -value else value
    }
    return sum + row.attendancePoints()
}

fun parsePixelStudentRow(item: JSONObject): PixelStudentRow? {
    val studentId = item.optInt("id_student", item.optInt("id", 0))
    if (studentId <= 0) return null
    return PixelStudentRow(
        studentId = studentId,
        name = item.optString("name", "—").ifBlank { "—" },
        partOfComp = item.optInt("part_of_comp", 0),
        makeContent = item.optInt("make_content", 0),
        inviteFriend = item.optInt("invite_friend", 0),
        cleanKvantum = item.optInt("clean_kvantum", 0),
        filledProjectCardOnTime = item.optInt("filled_project_card_on_time", 0),
        finishedProjectWithProduct = item.optInt("finished_project_with_product", 0),
        regionalCompetition = item.optInt("regional_competition", 0),
        interregionalCompetition = item.optInt("interregional_competition", 0),
        allRussianCompetition = item.optInt("all_russian_competition", 0),
        internationalCompetition = item.optInt("international_competition", 0),
        nto = item.optInt("nto", 0),
        becomeAnEngineeringVolunteer = item.optInt("become_an_engineering_volunteer", 0),
        helpWithEvent = item.optInt("help_with_event", 0),
        makeOwnEvent = item.optInt("make_own_event", 0),
        specialAchievements = item.optInt("special_achievements", 0),
        fine = abs(item.optInt("fine", 0))
    )
}

fun pixelColumnDisplayValue(row: PixelStudentRow, column: PixelColumnDef): String = when (column.key) {
    "__attendance_percent__" -> "${row.attendancePercent}%"
    "__attendance__" -> row.attendancePoints().toString()
    "__total__" -> row.total().toString()
    else -> row.fieldValue(column.key).toString()
}

fun calcPixelDelta(column: PixelColumnDef, selectedOptionId: String, customValue: String): Int? {
    return when (column.mode) {
        PixelColumnMode.Fixed -> column.fixedPoints
        PixelColumnMode.Select -> column.options
            .firstOrNull { it.id == selectedOptionId }
            ?.points
        PixelColumnMode.Number -> customValue.trim().toIntOrNull()
        PixelColumnMode.Penalty -> customValue.trim().toIntOrNull()?.let { abs(it) }
        else -> null
    }
}
