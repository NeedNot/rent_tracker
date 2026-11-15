package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOr
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.ui.MessageDispatcher
import net.neednot.renttracker.core.ui.UiMessage
import net.neednot.renttracker.core.ui.UiMessageVariant
import net.neednot.renttracker.core.ui.WhileScreenIsVisible
import net.neednot.renttracker.feature.tenants.domain.MoveTarget
import net.neednot.renttracker.feature.tenants.domain.MoveTenantRepository
import net.neednot.renttracker.feature.tenants.domain.TenantError
import net.neednot.renttracker.feature.tenants.domain.TenantOverview
import net.neednot.renttracker.feature.tenants.domain.TenantRepository
import net.neednot.renttracker.feature.tenants.presentation.uistate.MoveTenantUiState

class MoveTenantViewModel(
    private val tenantId: String,
    tenantRepository: TenantRepository,
    private val moveRepository: MoveTenantRepository,
    private val messageDispatcher: MessageDispatcher,
) : ViewModel() {
    private val pickedGroupId = MutableStateFlow<String?>(null)
    private val isConfirming = MutableStateFlow(false)
    private val isMoving = MutableStateFlow(false)
    private var shownTenant: TenantOverview? = null
    private var shownTargets: List<MoveTarget> = emptyList()

    private val eventChannel = Channel<MoveTenantEvent>(Channel.BUFFERED)
    val events: Flow<MoveTenantEvent> = eventChannel.receiveAsFlow()

    private val moveTargets: Flow<Result<List<MoveTarget>, TenantError>?> =
        moveRepository
            .observeTargets(tenantId)
            .onEach { shownTargets = it.getOr(emptyList()) }
            .onStart<Result<List<MoveTarget>, TenantError>?> { emit(null) }

    val uiState: StateFlow<MoveTenantUiState> =
        combine(
            tenantRepository.observeTenant(tenantId).onEach { shownTenant = it.value },
            moveTargets,
            pickedGroupId,
            isConfirming,
            isMoving,
        ) { synced, targets, picked, confirming, moving ->
            val overview = synced.value
            val failure = (synced.status as? SyncStatus.Failed)?.error
            when {
                overview == null && failure != null -> MoveTenantUiState.Error(failure.text())
                overview == null || targets == null -> MoveTenantUiState.Loading
                else -> targets.toUiState(overview, picked, confirming, moving)
            }
        }.stateIn(viewModelScope, WhileScreenIsVisible, MoveTenantUiState.Loading)

    fun onTargetClicked(groupId: String) {
        pickedGroupId.value = groupId
    }

    fun onNextClicked() {
        if (selectedTarget() != null) isConfirming.value = true
    }

    fun onConfirmationDismissed() {
        isConfirming.value = false
    }

    fun onMoveConfirmed() {
        val overview = shownTenant
        val target = selectedTarget()
        if (overview == null || target == null || isMoving.value) return
        isMoving.value = true
        viewModelScope.launch {
            moveRepository
                .move(tenantId, target.groupId, overview.tenant.version)
                .onOk { eventChannel.send(MoveTenantEvent.TenantMoved) }
                .onErr { error -> messageDispatcher.show(UiMessage(error.text(), UiMessageVariant.Error)) }
            isConfirming.value = false
            isMoving.value = false
        }
    }

    private fun Result<List<MoveTarget>, TenantError>.toUiState(
        overview: TenantOverview,
        picked: String?,
        confirming: Boolean,
        moving: Boolean,
    ): MoveTenantUiState =
        mapBoth(
            success = { targets ->
                val selected = targets.selected(picked)
                when {
                    selected == null -> {
                        MoveTenantUiState.Empty(overview.tenant.details.name)
                    }

                    else -> {
                        MoveTenantUiState.Content(
                            tenantName = overview.tenant.details.name,
                            targets = targets.map { it.toUiState(isSelected = it == selected) },
                            confirmation = overview.moveConfirmation(selected).takeIf { confirming },
                            isMoving = moving,
                        )
                    }
                }
            },
            failure = { error -> MoveTenantUiState.Error(error.text()) },
        )

    private fun selectedTarget(): MoveTarget? = shownTargets.selected(pickedGroupId.value)

    private fun List<MoveTarget>.selected(picked: String?): MoveTarget? =
        firstOrNull { it.groupId == picked } ?: firstOrNull()
}
