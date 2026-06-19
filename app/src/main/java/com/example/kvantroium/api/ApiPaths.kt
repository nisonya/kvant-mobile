package com.example.kvantroium.api

object ApiPaths {
    object Auth {
        const val LOGIN = "/api/auth/login"
        const val REFRESH = "/api/auth/refresh"
        const val LOGOUT = "/api/auth/logout"
        const val CHANGE_PASSWORD = "/api/auth/change-password"
    }

    object Employees {
        const val ALL = "/api/employees/all"
        const val WITH_INACTIVE = "/api/employees/with-inactive"
        const val LIST = "/api/employees"
        const val ADD = "/api/employees/add"
        const val SIZES = "/api/employees/sizes"

        fun byId(id: Int) = "/api/employees/$id"
        fun kpiById(id: Int) = "/api/employees/kpi/$id"
    }

    object Students {
        const val ROOT = "/api/students"
        const val SEARCH = "/api/students/search"
        const val EXIST = "/api/students/exist"
        const val SEARCH_NEW = "/api/students/search-new"
        const val ADD_TO_GROUP = "/api/students/add-to-group"
        const val DELETE_FROM_GROUP = "/api/students/from-group"
        const val MOVE_TO_GROUP = "/api/students/update-to-group"

        fun byId(id: Int) = "/api/students/$id"
        fun fullByGroup(id: Int) = "/api/students/full-by-group/$id"
        fun groupsByStudent(id: Int) = "/api/students/groups-by-student/$id"
    }

    object Groups {
        const val LIST = "/api/groups/list"
        const val PIXELS_UPDATE = "/api/groups/pixels"
        const val PIXELS_CLEAR_ALL = "/api/groups/pixels/clear-all"

        fun byId(id: Int) = "/api/groups/list/$id"
        fun byTeacher(id: Int) = "/api/groups/by-teacher/$id"
        fun pixelsByGroup(id: Int) = "/api/groups/pixels/$id"
    }

    object Schedule {
        const val ROOT = "/api/schedule"
        const val TEACHERS = "/api/schedule/teachers"
        const val GROUPS = "/api/schedule/groups"

        fun byTeacher(id: Int) = "/api/schedule/by-teacher/$id"
        fun byGroup(id: Int) = "/api/schedule/by-group/$id"
        fun byRoom(id: Int) = "/api/schedule/by-room/$id"
        fun byId(id: Int) = "/api/schedule/$id"
    }

    object Attendance {
        const val ROOT = "/api/attendance"
        const val BY_GROUP_DATE = "/api/attendance/by-group-date"
        const val BY_GROUP_DATE_NEW = "/api/attendance/by-group-date-new"
        const val CLEAR_ALL = "/api/attendance/clear-all"

        fun byGroup(id: Int) = "/api/attendance/by-group/$id"
    }

    object Reference {
        const val POSITIONS = "/api/reference/positions"
        const val ACCESS = "/api/reference/access"
        const val ROOMS = "/api/reference/rooms"
        const val DOCS = "/api/reference/docs"
        val LEVELS_TRY = listOf(
            "/api/reference/levels",
            "/api/reference/type-of-part-event",
            "/api/reference/types-of-part-event",
            "/api/reference/type_of_part_event"
        )
        val TYPES_OF_HOLDING_TRY = listOf(
            "/api/reference/types-of-holding",
            "/api/reference/form-of-holding",
            "/api/reference/forms-of-holding",
            "/api/reference/form_of_holding",
            "/api/reference/types_of_holding"
        )
        val TYPES_OF_ORGANIZATION_TRY = listOf(
            "/api/reference/types-of-organization",
            "/api/reference/types-of-organizations",
            "/api/reference/types_of_organization",
            "/api/reference/types_of_organizations"
        )

        fun positionById(id: Int) = "/api/reference/positions/$id"
        fun accessById(id: Int) = "/api/reference/access/$id"
        fun roomById(id: Int) = "/api/reference/rooms/$id"
        fun docById(id: Int) = "/api/reference/docs/$id"
    }

    object Events {
        const val ORG = "/api/events/org"
        const val PART = "/api/events/part"
        const val ORGANIZATION_LEGACY = "/api/events/organization"

        fun orgList() = "$ORG/list"
        fun orgCount() = "$ORG/count"
        fun orgFullInfo(eventId: Int) = "$ORG/full-inf/$eventId"
        fun orgResponsible(eventId: Int) = "$ORG/responsible/$eventId"
        const val ORG_RESPONSIBLE = "$ORG/responsible"

        fun partList() = "$PART/list"
        fun partCount() = "$PART/count"
        fun partFullInfo(eventId: Int) = "$PART/full-inf/$eventId"
        fun partResponsible(eventId: Int) = "$PART/responsible/$eventId"
        fun partResponsibleNew(eventId: Int) = "$PART/responsible-new/$eventId"
        const val PART_RESPONSIBLE = "$PART/responsible"
        const val PART_MARK = "$PART/mark"
        const val PART_RESULT = "$PART/result"

        fun orgNotificationsToday(employeeId: Int) = "$ORG/notifications-today/$employeeId"
        fun orgNotificationsTomorrow(employeeId: Int) = "$ORG/notifications-tomorrow/$employeeId"
        fun partNotificationsToday(employeeId: Int) = "$PART/notifications-today/$employeeId"
        fun partNotificationsTomorrow(employeeId: Int) = "$PART/notifications-tomorrow/$employeeId"

        fun eventDocuments(isOrg: Boolean, eventId: Int): String =
            "${if (isOrg) ORG else PART}/$eventId/documents"

        fun eventDocumentDownload(isOrg: Boolean, documentId: Int): String =
            "${if (isOrg) ORG else PART}/documents/$documentId/download"

        fun eventDocumentDelete(isOrg: Boolean, documentId: Int): String =
            "${if (isOrg) ORG else PART}/documents/$documentId"
    }

    object Rent {
        const val ROOT = "/api/rent"
        const val BY_DATE_ROOM = "/api/rent/by-date-room"

        fun byEvent(eventId: Int) = "/api/rent/by-event/$eventId"
        fun byId(id: Int) = "/api/rent/$id"
    }

    object MobileUpdates {
        const val ANDROID_LATEST = "/mobile-updates/android/latest"
    }
}
