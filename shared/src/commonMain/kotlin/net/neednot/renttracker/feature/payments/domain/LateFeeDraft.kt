package net.neednot.renttracker.feature.payments.domain

import kotlinx.datetime.YearMonth

data class LateFeeDraft(
    val amountCents: Long,
    val forMonth: YearMonth,
)
