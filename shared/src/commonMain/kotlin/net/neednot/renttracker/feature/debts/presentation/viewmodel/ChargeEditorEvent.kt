package net.neednot.renttracker.feature.debts.presentation.viewmodel

sealed interface ChargeEditorEvent {
    data object ChargeSaved : ChargeEditorEvent
}
