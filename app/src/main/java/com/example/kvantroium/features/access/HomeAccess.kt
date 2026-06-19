package com.example.kvantroium.features.access

import com.example.kvantroium.storage.UserProfile
import com.example.kvantroium.ui.navigation.AppRoute

enum class HomeSection {
    Rent,
    Notifications,
    Documents,
    Groups,
    Schedule,
    Pixels,
    Students,
    Attendance,
    Kpi
}

fun isGuestUser(accessLevel: Int?): Boolean = accessLevel == 3

fun canOpenHomeSection(section: HomeSection, accessLevel: Int?): Boolean {
    if (isGuestUser(accessLevel)) {
        return section == HomeSection.Notifications || section == HomeSection.Documents
    }
    return true
}

fun canOpenAppRoute(route: AppRoute, user: UserProfile?): Boolean = when (route) {
    AppRoute.Employees -> canManageReferenceData(user)
    AppRoute.Groups,
    AppRoute.Students,
    AppRoute.Pixels,
    AppRoute.Attendance,
    AppRoute.Schedule,
    AppRoute.Rent -> !isGuestUser(user?.accessLevel)
    else -> true
}
