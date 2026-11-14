package net.neednot.renttracker.feature.tenants.domain

import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.data.TenantBalanceState

data class TenantBalance(
    val state: TenantBalanceState,
    val total: BalanceTotal,
    val owedBreakdown: OwedBreakdown,
    val nextDueDate: LocalDate?,
    val partialMoveOutPeriod: ClosedRange<LocalDate>?,
)
