package net.neednot.renttracker.feature.tenants.domain

import kotlinx.datetime.LocalDate

data class MonthLateFee(
    val amountCents: Long,
    val isUnpaid: Boolean,
    val addedOn: LocalDate,
    val addedBy: Person,
    val mayNotApply: Boolean,
)
