package net.neednot.renttracker.feature.tenants.domain

sealed interface PendingPaymentReview {
    val payment: PendingPayment

    data class Waiting(
        override val payment: PendingPayment,
    ) : PendingPaymentReview

    data class PossibleDuplicate(
        override val payment: PendingPayment,
        val matches: List<PendingDuplicateMatch>,
    ) : PendingPaymentReview

    data class TenantChanged(
        override val payment: PendingPayment,
        val balanceChange: PendingBalanceChange?,
        val newEntries: List<PendingNewEntry>,
    ) : PendingPaymentReview

    data class Rejected(
        override val payment: PendingPayment,
        val reason: PendingRejection,
    ) : PendingPaymentReview
}
