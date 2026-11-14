package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.TimeZone
import net.neednot.renttracker.core.ui.WhileScreenIsVisible
import net.neednot.renttracker.feature.tenants.domain.TenantRepository
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerUiState

class LedgerViewModel(
    tenantId: String,
    tenantRepository: TenantRepository,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {
    val uiState: StateFlow<LedgerUiState> =
        tenantRepository
            .observeTenant(tenantId)
            .map { it.toLedgerUiState(timeZone) }
            .stateIn(viewModelScope, WhileScreenIsVisible, LedgerUiState.Loading)
}
