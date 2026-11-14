package net.neednot.renttracker.feature.group.presentation.viewmodel

sealed interface GroupEvent {
    data object GroupGone : GroupEvent
}
