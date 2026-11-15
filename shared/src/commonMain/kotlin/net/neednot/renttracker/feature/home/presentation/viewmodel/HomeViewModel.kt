package net.neednot.renttracker.feature.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onErr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.neednot.renttracker.core.ui.MessageDispatcher
import net.neednot.renttracker.core.ui.UiMessage
import net.neednot.renttracker.core.ui.UiMessageVariant
import net.neednot.renttracker.core.ui.WhileScreenIsVisible
import net.neednot.renttracker.core.ui.initialsOf
import net.neednot.renttracker.feature.home.domain.HomeAccountRepository
import net.neednot.renttracker.feature.home.domain.HomeError
import net.neednot.renttracker.feature.home.domain.HomeOfflineRepository
import net.neednot.renttracker.feature.home.domain.HomeRepository
import net.neednot.renttracker.feature.home.presentation.uistate.HomeUiState

class HomeViewModel(
    private val homeRepository: HomeRepository,
    private val homeOfflineRepository: HomeOfflineRepository,
    private val homeAccountRepository: HomeAccountRepository,
    private val messageDispatcher: MessageDispatcher,
) : ViewModel() {
    private val overlay = MutableStateFlow(HomeOverlay())

    private val chrome =
        combine(
            homeAccountRepository.observeAccountName(),
            homeOfflineRepository.observeFreshness(),
            homeOfflineRepository.observeRejectedPayments(),
        ) { accountName, freshness, rejectedPayments ->
            HomeChrome(accountName?.let(::initialsOf)?.take(1), freshness, rejectedPayments)
        }

    val uiState: StateFlow<HomeUiState> =
        combine(homeRepository.observeHome(), chrome, overlay) { home, chrome, overlay ->
            home.toHomeUiState(overlay, chrome)
        }.stateIn(viewModelScope, WhileScreenIsVisible, HomeUiState.Loading())

    fun onAcceptClicked(inviteId: String) {
        answer(inviteId) { homeRepository.acceptInvitation(inviteId) }
    }

    fun onDeclineClicked(inviteId: String) {
        if (overlay.value.workingInviteId == null) overlay.update { it.copy(decliningInviteId = inviteId) }
    }

    fun onDeclineDismissed() {
        overlay.update { it.copy(decliningInviteId = null) }
    }

    fun onDeclineConfirmed() {
        val inviteId = overlay.value.decliningInviteId ?: return
        overlay.update { it.copy(decliningInviteId = null) }
        answer(inviteId) { homeRepository.declineInvitation(inviteId) }
    }

    fun onRejectedPaymentsClicked() {
        overlay.update { it.copy(isRejectedSheetOpen = true) }
    }

    fun onRejectedPaymentsDismissed() {
        overlay.update { it.copy(isRejectedSheetOpen = false) }
    }

    fun onRejectedPaymentDismissed(clientId: String) {
        viewModelScope.launch { homeOfflineRepository.dismissRejectedPayment(clientId) }
    }

    private fun answer(
        inviteId: String,
        request: suspend () -> Result<Unit, HomeError>,
    ) {
        if (overlay.value.workingInviteId != null) return
        overlay.update { it.copy(workingInviteId = inviteId) }
        viewModelScope.launch {
            val result = request()
            overlay.update { it.copy(workingInviteId = null) }
            result.onErr { error -> messageDispatcher.show(UiMessage(error.text(), UiMessageVariant.Error)) }
        }
    }
}
