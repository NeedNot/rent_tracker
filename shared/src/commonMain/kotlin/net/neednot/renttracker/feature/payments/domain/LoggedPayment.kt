package net.neednot.renttracker.feature.payments.domain

sealed interface LoggedPayment {
    data class Saved(
        val paymentId: String,
    ) : LoggedPayment

    data class SavedOnPhone(
        val clientId: String,
    ) : LoggedPayment
}
