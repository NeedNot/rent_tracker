package net.neednot.renttracker.feature.tenants.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

sealed interface LedgerEntry {
    val key: String
    val date: LocalDate
    val amountCents: Long
    val balance: LedgerBalance?

    data class Rent(
        override val key: String,
        override val date: LocalDate,
        override val amountCents: Long,
        override val balance: LedgerBalance?,
        val month: YearMonth,
    ) : LedgerEntry

    data class Payment(
        override val key: String,
        override val date: LocalDate,
        override val amountCents: Long,
        override val balance: LedgerBalance?,
        val payment: PaymentDetails,
    ) : LedgerEntry

    data class Charge(
        override val key: String,
        override val date: LocalDate,
        override val amountCents: Long,
        override val balance: LedgerBalance?,
        val charge: ChargeDetails,
    ) : LedgerEntry
}
