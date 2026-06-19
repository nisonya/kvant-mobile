package com.example.kvantroium.features.rent

import com.example.kvantroium.api.ApiClient
import com.example.kvantroium.api.ApiPaths
import com.example.kvantroium.features.events.formatTimeForDisplay
import com.example.kvantroium.features.events.weekdayFromIsoDate
import com.example.kvantroium.features.schedule.ScheduleLesson
import com.example.kvantroium.features.schedule.ScheduleTab
import com.example.kvantroium.features.schedule.loadScheduleLessons
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class RentByRoomItem(
    val id: Int,
    val eventId: Int,
    val roomId: Int,
    val title: String,
    val startTime: String,
    val endTime: String,
    val date: String
)

suspend fun loadRentByDateRoom(
    apiClient: ApiClient,
    dateIso: String,
    roomId: Int
): List<RentByRoomItem> {
    val response = apiClient.apiRequest(
        method = "POST",
        path = ApiPaths.Rent.BY_DATE_ROOM,
        body = JSONObject()
            .put("date", dateIso)
            .put("room_id", roomId)
    )
    val array = response.optJSONArray("data") ?: JSONArray()
    return buildList {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val id = item.optInt("id_rent", item.optInt("id", 0))
            if (id <= 0) continue
            add(
                RentByRoomItem(
                    id = id,
                    eventId = item.optInt("id_event", 0),
                    roomId = item.optInt("id_room", roomId),
                    title = item.optString("name", "—"),
                    startTime = formatTimeForDisplay(item.optString("start_time", "")),
                    endTime = formatTimeForDisplay(item.optString("end_time", "")),
                    date = item.optString("date", dateIso)
                )
            )
        }
    }.sortedBy { it.startTime }
}

suspend fun loadScheduleLessonsForRoomOnDate(
    apiClient: ApiClient,
    dateIso: String,
    roomId: Int
): List<ScheduleLesson> {
    if (roomId <= 0 || dateIso.isBlank()) return emptyList()
    val targetDay = weekdayFromIsoDate(dateIso).lowercase(Locale("ru"))
    if (targetDay.isBlank()) return emptyList()
    return loadScheduleLessons(apiClient, ScheduleTab.ROOMS, roomId)
        .filter { it.day.equals(targetDay, ignoreCase = true) }
        .sortedBy { it.startTime }
}
