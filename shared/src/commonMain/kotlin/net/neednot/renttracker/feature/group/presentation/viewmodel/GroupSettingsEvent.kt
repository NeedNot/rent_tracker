package net.neednot.renttracker.feature.group.presentation.viewmodel

sealed interface GroupSettingsEvent {
    data object Saved : GroupSettingsEvent
}
