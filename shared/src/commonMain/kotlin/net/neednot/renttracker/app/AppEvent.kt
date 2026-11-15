package net.neednot.renttracker.app

sealed interface AppEvent {
    data object ReturnHome : AppEvent
}
