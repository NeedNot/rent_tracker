package net.neednot.renttracker.feature.tenants.domain

import kotlinx.datetime.YearMonth

sealed interface AllocationTarget {
    data class Rent(
        val month: YearMonth,
    ) : AllocationTarget

    data class Charge(
        val kind: ChargeKind,
    ) : AllocationTarget
}
