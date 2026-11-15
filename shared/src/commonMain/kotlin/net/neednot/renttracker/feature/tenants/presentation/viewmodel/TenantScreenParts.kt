package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import net.neednot.renttracker.core.sync.Freshness
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.feature.tenants.domain.PendingPayment
import net.neednot.renttracker.feature.tenants.domain.TenantCalendar
import net.neednot.renttracker.feature.tenants.domain.TenantError
import net.neednot.renttracker.feature.tenants.presentation.uistate.MonthDetailUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantStatusChangeUiState

internal data class TenantScreenParts(
    val statusChange: TenantStatusChangeUiState,
    val calendar: Synced<TenantCalendar, TenantError>?,
    val monthDetail: MonthDetailUiState?,
    val pendingPayments: List<PendingPayment>,
    val freshness: Freshness,
)
