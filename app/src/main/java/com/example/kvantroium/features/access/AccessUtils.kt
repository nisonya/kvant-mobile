package com.example.kvantroium.features.access

import com.example.kvantroium.storage.UserProfile

private val ADMIN_ACCESS_LEVELS = setOf(1, 4, 6)

fun canManageReferenceData(user: UserProfile?): Boolean {
    val level = user?.accessLevel ?: return false
    return level in ADMIN_ACCESS_LEVELS
}
