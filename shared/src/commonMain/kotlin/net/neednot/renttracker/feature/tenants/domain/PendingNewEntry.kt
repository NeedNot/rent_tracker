package net.neednot.renttracker.feature.tenants.domain

import kotlinx.datetime.LocalDate

sealed interface PendingNewEntry {
    val date: LocalDate
    val amountCents: Long
    val by: Person

    data class Payment(
        override val date: LocalDate,
        override val amountCents: Long,
        override val by: Person,
    ) : PendingNewEntry

    data class Charge(
        override val date: LocalDate,
        override val amountCents: Long,
        override val by: Person,
        val kind: ChargeKind,
    ) : PendingNewEntry
}
