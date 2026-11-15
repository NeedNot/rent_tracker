package net.neednot.renttracker.app

sealed interface AppEvent {
    data object ReturnHome : AppEvent

    data class OpenPendingPayment(
        val tenantId: String,
        val clientId: String,
    ) : AppEvent
}
