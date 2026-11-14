package net.neednot.renttracker.feature.payments.presentation.viewmodel

sealed interface PaymentEditorEvent {
    data object PaymentSaved : PaymentEditorEvent
}
