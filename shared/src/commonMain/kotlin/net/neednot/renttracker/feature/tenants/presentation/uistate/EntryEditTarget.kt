package net.neednot.renttracker.feature.tenants.presentation.uistate

sealed interface EntryEditTarget {
    data class Payment(
        val paymentId: String,
    ) : EntryEditTarget

    data class Charge(
        val debtId: String,
    ) : EntryEditTarget
}
