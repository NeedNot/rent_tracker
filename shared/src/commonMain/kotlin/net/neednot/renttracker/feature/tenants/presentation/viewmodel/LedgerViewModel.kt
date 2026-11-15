package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.TimeZone
import net.neednot.renttracker.core.ui.WhileScreenIsVisible
import net.neednot.renttracker.feature.tenants.domain.PendingPaymentRepository
import net.neednot.renttracker.feature.tenants.domain.TenantFreshnessRepository
import net.neednot.renttracker.feature.tenants.domain.TenantRepository
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerUiState

class LedgerViewModel(
    tenantId: String,
    tenantRepository: TenantRepository,
    freshnessRepository: TenantFreshnessRepository,
    pendingPaymentRepository: PendingPaymentRepository,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {
    val uiState: StateFlow<LedgerUiState> =
        combine(
            tenantRepository.observeTenant(tenantId),
            pendingPaymentRepository.observePendingPayments(tenantId),
            freshnessRepository.observeLedgerFreshness(tenantId),
        ) { synced, pending, freshness ->
            synced.toLedgerUiState(OfflineSources(pending, freshness), timeZone)
        }.stateIn(viewModelScope, WhileScreenIsVisible, LedgerUiState.Loading)
}
