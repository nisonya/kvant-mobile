package com.example.kvantroium.ui.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.kvantroium.features.events.EventKind

class AppNavigator(initialRoute: AppRoute) {
    private val backStack: SnapshotStateList<AppRoute> = mutableStateListOf(initialRoute)

    val currentRoute: AppRoute
        get() = backStack.last()

    val canPop: Boolean
        get() = backStack.size > 1

    fun navigate(route: AppRoute) {
        backStack.add(route)
    }

    fun replace(route: AppRoute) {
        backStack.clear()
        backStack.add(route)
    }

    fun popBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    fun contains(route: AppRoute): Boolean = route in backStack
}

data class EventFlowContext(
    var eventId: Int = 0,
    var kind: EventKind = EventKind.ORG,
    var isCreateMode: Boolean = false,
    var detailReloadNonce: Int = 0
)
