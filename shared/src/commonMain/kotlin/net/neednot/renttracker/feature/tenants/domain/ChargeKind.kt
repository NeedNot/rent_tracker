package net.neednot.renttracker.feature.tenants.domain

import kotlinx.datetime.YearMonth

sealed interface ChargeKind {
    data class LateFee(
        val forMonth: YearMonth,
    ) : ChargeKind

    data class Custom(
        val note: String,
    ) : ChargeKind
}
