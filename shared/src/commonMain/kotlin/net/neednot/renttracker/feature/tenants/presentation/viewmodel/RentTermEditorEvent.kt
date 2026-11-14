package net.neednot.renttracker.feature.tenants.presentation.viewmodel

sealed interface RentTermEditorEvent {
    data object RentTermSaved : RentTermEditorEvent

    data object RentTermDeleted : RentTermEditorEvent
}
