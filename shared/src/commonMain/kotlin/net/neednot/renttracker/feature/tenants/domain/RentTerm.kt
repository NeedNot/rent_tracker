package net.neednot.renttracker.feature.tenants.domain

import kotlinx.datetime.YearMonth

data class RentTerm(
    val rentTermId: String,
    val startMonth: YearMonth,
    val amountCents: Long,
    val dueDay: Int,
    val version: Long,
)
