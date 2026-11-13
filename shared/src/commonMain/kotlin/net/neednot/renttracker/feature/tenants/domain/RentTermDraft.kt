package net.neednot.renttracker.feature.tenants.domain

import kotlinx.datetime.YearMonth

data class RentTermDraft(
    val startMonth: YearMonth,
    val amountCents: Long,
    val dueDay: Int,
)
