package net.neednot.renttracker.feature.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onErr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.neednot.renttracker.core.ui.MessageDispatcher
import net.neednot.renttracker.core.ui.UiMessage
import net.neednot.renttracker.core.ui.UiMessageVariant
import net.neednot.renttracker.core.ui.WhileScreenIsVisible
import net.neednot.renttracker.feature.home.domain.HomeError
import net.neednot.renttracker.feature.home.domain.HomeRepository
import net.neednot.renttracker.feature.home.presentation.uistate.HomeUiState

class HomeViewModel(
    private val homeRepository: HomeRepository,
    private val messageDispatcher: MessageDispatcher,
) : ViewModel() {
    private val decliningInviteId = MutableStateFlow<String?>(null)
    private val workingInviteId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> =
        combine(homeRepository.observeHome(), decliningInviteId, workingInviteId) { home, declining, working ->
            home.toHomeUiState(declineInviteId = declining, workingInviteId = working)
        }.stateIn(viewModelScope, WhileScreenIsVisible, HomeUiState.Loading)

    fun onAcceptClicked(inviteId: String) {
        answer(inviteId) { homeRepository.acceptInvitation(inviteId) }
    }

    fun onDeclineClicked(inviteId: String) {
        if (workingInviteId.value == null) decliningInviteId.value = inviteId
    }

    fun onDeclineDismissed() {
        decliningInviteId.value = null
    }

    fun onDeclineConfirmed() {
        val inviteId = decliningInviteId.value ?: return
        decliningInviteId.value = null
        answer(inviteId) { homeRepository.declineInvitation(inviteId) }
    }

    private fun answer(
        inviteId: String,
        request: suspend () -> Result<Unit, HomeError>,
    ) {
        if (workingInviteId.value != null) return
        workingInviteId.value = inviteId
        viewModelScope.launch {
            val result = request()
            workingInviteId.value = null
            result.onErr { error -> messageDispatcher.show(UiMessage(error.text(), UiMessageVariant.Error)) }
        }
    }
}
