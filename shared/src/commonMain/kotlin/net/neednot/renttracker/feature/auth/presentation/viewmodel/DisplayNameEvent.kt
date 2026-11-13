package net.neednot.renttracker.feature.auth.presentation.viewmodel

sealed interface DisplayNameEvent {
    data object Done : DisplayNameEvent
}
