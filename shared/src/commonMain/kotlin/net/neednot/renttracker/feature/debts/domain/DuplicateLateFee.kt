package net.neednot.renttracker.feature.debts.domain

import kotlinx.datetime.LocalDate

data class DuplicateLateFee(
    val date: LocalDate,
    val amountCents: Long,
    val byName: String,
    val byAccountId: String,
)
