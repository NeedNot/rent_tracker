package net.neednot.renttracker.feature.debts.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

sealed interface ChargeDraft {
    val amountCents: Long
    val date: LocalDate

    data class LateFee(
        override val amountCents: Long,
        override val date: LocalDate,
        val forMonth: YearMonth,
        val note: String?,
    ) : ChargeDraft

    data class Custom(
        override val amountCents: Long,
        override val date: LocalDate,
        val note: String,
    ) : ChargeDraft
}
