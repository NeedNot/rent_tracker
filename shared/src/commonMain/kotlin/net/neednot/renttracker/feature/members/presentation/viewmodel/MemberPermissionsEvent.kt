package net.neednot.renttracker.feature.members.presentation.viewmodel

sealed interface MemberPermissionsEvent {
    data object PermissionsSaved : MemberPermissionsEvent

    data object MemberRemoved : MemberPermissionsEvent
}
