package net.neednot.renttracker.feature.home.presentation.viewmodel

sealed interface CreateGroupEvent {
    data class GroupCreated(
        val groupId: String,
    ) : CreateGroupEvent
}
