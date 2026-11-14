package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.designsystem.format.formatMoney
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.WhileScreenIsVisible
import net.neednot.renttracker.feature.tenants.domain.TenantCalendarRepository
import net.neednot.renttracker.feature.tenants.domain.TenantOverview
import net.neednot.renttracker.feature.tenants.domain.TenantRepository
import net.neednot.renttracker.feature.tenants.presentation.uistate.CalendarUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.MonthDetailUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantConfirmationUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantStatusAction
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantStatusChangeUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.tenant_archive_message
import net.neednot.renttracker.resources.tenant_archive_owes
import net.neednot.renttracker.resources.tenant_restore_message

@OptIn(ExperimentalCoroutinesApi::class)
class TenantViewModel(
    private val tenantId: String,
    private val tenantRepository: TenantRepository,
    private val calendarRepository: TenantCalendarRepository,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {
    private val statusChange = MutableStateFlow(TenantStatusChangeUiState())
    private val yearOffset = MutableStateFlow(0)
    private val openedMonth = MutableStateFlow<YearMonth?>(null)
    private var shownTenant: TenantOverview? = null

    private val calendar: Flow<CalendarUiState?> =
        yearOffset
            .flatMapLatest { offset -> calendarRepository.observeCalendar(tenantId, offset) }
            .map { it.toCalendarUiState() }
            .onStart<CalendarUiState?> { emit(null) }

    private val monthDetail: Flow<MonthDetailUiState?> =
        openedMonth.flatMapLatest { month ->
            if (month ==
                null
            ) {
                flowOf(null)
            } else {
                calendarRepository.observeMonthDetail(tenantId, month).map { it?.toUiState() }
            }
        }

    val uiState: StateFlow<TenantUiState> =
        combine(
            tenantRepository.observeTenant(tenantId).onEach { shownTenant = it.value },
            statusChange,
            calendar,
            monthDetail,
        ) { synced, change, calendarState, detail ->
            synced.toTenantUiState(change, timeZone, calendarState, detail)
        }.stateIn(viewModelScope, WhileScreenIsVisible, TenantUiState.Loading)

    fun onPreviousYearClicked() {
        yearOffset.update { it - 1 }
    }

    fun onNextYearClicked() {
        yearOffset.update { it + 1 }
    }

    fun onMonthClicked(month: String) {
        openedMonth.value = YearMonth.parse(month)
    }

    fun onMonthDetailDismissed() {
        openedMonth.value = null
    }

    fun onArchiveClicked() {
        val overview = shownTenant ?: return
        statusChange.value =
            TenantStatusChangeUiState(TenantConfirmationUiState(TenantStatusAction.Archive, overview.archiveMessage()))
    }

    fun onRestoreClicked() {
        val name = shownTenant?.tenant?.details?.name ?: return
        val message = UiText.Resource(Res.string.tenant_restore_message, listOf(name))
        statusChange.value = TenantStatusChangeUiState(TenantConfirmationUiState(TenantStatusAction.Restore, message))
    }

    fun onLateFeeDismissed() {
        val suggestion = shownTenant?.lateFeeSuggestion ?: return
        viewModelScope.launch { tenantRepository.dismissLateFeeSuggestion(tenantId, suggestion.month) }
    }

    fun onStatusChangeDismissed() {
        statusChange.update { it.copy(confirmation = null) }
    }

    fun onStatusChangeConfirmed() {
        val action = statusChange.value.confirmation?.action ?: return
        val expectedVersion = shownTenant?.tenant?.version ?: return
        statusChange.value = TenantStatusChangeUiState(isSaving = true)
        viewModelScope.launch {
            val result =
                when (action) {
                    TenantStatusAction.Archive -> tenantRepository.archiveTenant(tenantId, expectedVersion)
                    TenantStatusAction.Restore -> tenantRepository.restoreTenant(tenantId, expectedVersion)
                }
            result
                .onOk { statusChange.value = TenantStatusChangeUiState() }
                .onErr { error -> statusChange.value = TenantStatusChangeUiState(error = error.text()) }
        }
    }

    private fun TenantOverview.archiveMessage(): UiText {
        val name = tenant.details.name
        val owedCents = balance?.total?.owedCents ?: 0
        return if (owedCents > 0) {
            UiText.Resource(Res.string.tenant_archive_owes, listOf(name, formatMoney(owedCents)))
        } else {
            UiText.Resource(Res.string.tenant_archive_message, listOf(name))
        }
    }
}
