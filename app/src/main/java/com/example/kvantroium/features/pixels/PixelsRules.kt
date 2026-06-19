package com.example.kvantroium.features.pixels

data class PixelCriterion(
    val title: String,
    val points: Int,
    val isPenalty: Boolean = false
)

fun attendancePercentToPixels(percent: Double): Int {
    return when {
        percent >= 94.0 -> 100
        percent >= 85.0 -> 80
        percent >= 70.0 -> 60
        percent >= 40.0 -> 30
        else -> 0
    }
}

fun calculatePixels(criteria: List<PixelCriterion>, attendancePercent: Double? = null): Int {
    val criteriaScore = criteria.sumOf { criterion ->
        if (criterion.isPenalty) -criterion.points else criterion.points
    }
    val attendanceScore = attendancePercent?.let(::attendancePercentToPixels) ?: 0
    return criteriaScore + attendanceScore
}
